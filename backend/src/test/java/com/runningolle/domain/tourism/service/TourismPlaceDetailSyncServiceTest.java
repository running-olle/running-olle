package com.runningolle.domain.tourism.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runningolle.domain.tourism.client.TourApiClient;
import com.runningolle.domain.tourism.client.TourApiClient.TourDetail;
import com.runningolle.domain.tourism.config.TourismSyncProperties;
import com.runningolle.domain.tourism.entity.TourismPlace;
import com.runningolle.domain.tourism.entity.TourismPlace.TourismPlaceSnapshot;
import com.runningolle.domain.tourism.enums.TourismDetailSyncStatus;
import com.runningolle.domain.tourism.repository.TourismPlaceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TourismPlaceDetailSyncServiceTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Mock
    private TourApiClient tourApiClient;

    @Mock
    private TourismPlaceRepository tourismPlaceRepository;

    private TourismPlaceDetailSyncService detailSyncService;

    @BeforeEach
    void setUp() {
        TourismSyncProperties properties = new TourismSyncProperties();
        properties.setDetailBatchSize(300);
        properties.setDetailMaxRetries(5);
        properties.setDetailRetryDelayHours(24);
        detailSyncService = new TourismPlaceDetailSyncService(
                tourApiClient,
                tourismPlaceRepository,
                properties,
                new ObjectMapper()
        );
    }

    @Test
    void enrichesPendingInventoryPlaceAndMarksItComplete() {
        TourismPlace place = TourismPlace.createFromInventory(snapshot("1", null, null));
        TourDetail detail = new TourDetail(
                "1", "12", "한라산 상세", "제주특별자치도 제주시", "1100로",
                "39", "4", "A01", "A0101", "A01010100",
                33.362, 126.53, "제주의 대표 산", "https://example.com/detail.jpg",
                "09:00 - 18:00", Map.of("detailCommon2", Map.of("contentid", "1"))
        );
        given(tourismPlaceRepository.findDetailSyncCandidates(any(), eq(5), eq(300)))
                .willReturn(List.of(place));
        given(tourApiClient.getDetail("1", "12")).willReturn(Optional.of(detail));

        var response = detailSyncService.syncPendingDetails();

        assertThat(response.completedCount()).isEqualTo(1);
        assertThat(response.failedCount()).isZero();
        assertThat(place.getDetailSyncStatus()).isEqualTo(TourismDetailSyncStatus.COMPLETE);
        assertThat(place.getOverview()).isEqualTo("제주의 대표 산");
        assertThat(place.getUseTime()).isEqualTo("09:00 - 18:00");
        assertThat(place.getRawData().has("areaBasedList2")).isTrue();
        assertThat(place.getRawData().has("detail")).isTrue();
        verify(tourismPlaceRepository).save(place);
    }

    @Test
    void keepsExistingDetailWhenRefreshFails() {
        TourismPlace place = TourismPlace.create(snapshot("2", "기존 설명", "기존 이용 시간"));
        given(tourismPlaceRepository.findDetailSyncCandidates(any(), eq(5), eq(300)))
                .willReturn(List.of(place));
        given(tourApiClient.getDetail("2", "12")).willThrow(new RuntimeException("quota exceeded"));

        var response = detailSyncService.syncPendingDetails();

        assertThat(response.completedCount()).isZero();
        assertThat(response.failedCount()).isEqualTo(1);
        assertThat(place.getDetailSyncStatus()).isEqualTo(TourismDetailSyncStatus.FAILED);
        assertThat(place.getDetailRetryCount()).isEqualTo(1);
        assertThat(place.getOverview()).isEqualTo("기존 설명");
        assertThat(place.getUseTime()).isEqualTo("기존 이용 시간");
        assertThat(place.getDetailLastError()).contains("quota exceeded");
        verify(tourismPlaceRepository).save(place);
    }

    @Test
    void retriesLaterWhenTourApiReturnsNoDetail() {
        TourismPlace place = TourismPlace.createFromInventory(snapshot("3", null, null));
        given(tourismPlaceRepository.findDetailSyncCandidates(any(), eq(5), eq(300)))
                .willReturn(List.of(place));
        given(tourApiClient.getDetail("3", "12")).willReturn(Optional.empty());

        var response = detailSyncService.syncPendingDetails();

        assertThat(response.completedCount()).isZero();
        assertThat(response.failedCount()).isEqualTo(1);
        assertThat(place.getDetailSyncStatus()).isEqualTo(TourismDetailSyncStatus.FAILED);
        assertThat(place.getDetailRetryCount()).isEqualTo(1);
        assertThat(place.getDetailLastError()).contains("상세정보 응답이 비어 있습니다");
        verify(tourismPlaceRepository).save(place);
    }

    @Test
    void skipsBatchWhenDailyAttemptLimitIsExhausted() {
        given(tourismPlaceRepository.countByDetailLastAttemptedAtGreaterThanEqual(any()))
                .willReturn(300L);

        var response = detailSyncService.syncPendingDetails();

        assertThat(response.selectedCount()).isZero();
        assertThat(response.processedCount()).isZero();
        verify(tourismPlaceRepository, never()).findDetailSyncCandidates(any(), any(Integer.class), any(Integer.class));
    }

    private static TourismPlaceSnapshot snapshot(String contentId, String overview, String useTime) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(126.529167, 33.361667));
        point.setSRID(4326);
        var rawData = new ObjectMapper().createObjectNode();
        rawData.set("areaBasedList2", new ObjectMapper().valueToTree(Map.of("contentid", contentId)));
        return new TourismPlaceSnapshot(
                contentId, "12", "한라산", "제주특별자치도 제주시", null, null,
                "A01", "A0101", "A01010100", "39", "4", point,
                "https://example.com/list.jpg", null, overview, useTime,
                "20240101000000", "20240102000000", rawData, LocalDateTime.now()
        );
    }
}
