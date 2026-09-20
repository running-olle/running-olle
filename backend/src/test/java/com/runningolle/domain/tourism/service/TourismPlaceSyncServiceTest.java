package com.runningolle.domain.tourism.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runningolle.domain.tourism.client.TourApiClient;
import com.runningolle.domain.tourism.client.TourApiClient.TourAreaItem;
import com.runningolle.domain.tourism.client.TourApiClient.TourAreaPage;
import com.runningolle.domain.tourism.config.TourismSyncProperties;
import com.runningolle.domain.tourism.entity.TourismPlace;
import com.runningolle.domain.tourism.entity.TourismPlace.TourismPlaceSnapshot;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TourismPlaceSyncServiceTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Mock
    private TourApiClient tourApiClient;

    @Mock
    private TourismPlaceRepository tourismPlaceRepository;

    private TourismPlaceSyncService tourismPlaceSyncService;

    @BeforeEach
    void setUp() {
        TourismSyncProperties properties = new TourismSyncProperties();
        properties.setAreaCode("39");
        properties.setContentTypeIds(List.of("12"));
        properties.setPageSize(100);

        tourismPlaceSyncService = new TourismPlaceSyncService(
                tourApiClient,
                tourismPlaceRepository,
                properties,
                new ObjectMapper()
        );
    }

    @Test
    void createsPendingTourismPlaceFromInventoryWithoutDetailCalls() {
        TourAreaItem item = tourAreaItem("1", "한라산", 33.361667, 126.529167);
        given(tourApiClient.getAreaBasedList(null, "12", 1, 100))
                .willReturn(new TourAreaPage(List.of(item), 1, 100, 1));
        given(tourismPlaceRepository.findByContentId("1")).willReturn(Optional.empty());

        var response = tourismPlaceSyncService.syncJejuTourismPlaces();

        assertThat(response.fetchedCount()).isEqualTo(1);
        assertThat(response.createdCount()).isEqualTo(1);
        assertThat(response.updatedCount()).isZero();
        assertThat(response.skippedCount()).isZero();
        assertThat(response.failedCount()).isZero();

        ArgumentCaptor<TourismPlace> placeCaptor = ArgumentCaptor.forClass(TourismPlace.class);
        verify(tourismPlaceRepository).save(placeCaptor.capture());
        TourismPlace savedPlace = placeCaptor.getValue();
        assertThat(savedPlace.getContentId()).isEqualTo("1");
        assertThat(savedPlace.getTitle()).isEqualTo("한라산");
        assertThat(savedPlace.getOverview()).isNull();
        assertThat(savedPlace.getUseTime()).isNull();
        assertThat(savedPlace.getDetailSyncStatus().name()).isEqualTo("PENDING");
        assertThat(savedPlace.getRawData().has("areaBasedList2")).isTrue();
        assertThat(savedPlace.getRawData().has("detail")).isFalse();
        verify(tourApiClient, never()).getDetail(any(), any());
    }

    @Test
    void skipsItemsWithoutCoordinates() {
        TourAreaItem item = tourAreaItem("2", "좌표 없는 장소", null, 126.529167);
        given(tourApiClient.getAreaBasedList(null, "12", 1, 100))
                .willReturn(new TourAreaPage(List.of(item), 1, 100, 1));

        var response = tourismPlaceSyncService.syncJejuTourismPlaces();

        assertThat(response.fetchedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.createdCount()).isZero();
        verify(tourApiClient, never()).getDetail(any(), any());
        verify(tourismPlaceRepository, never()).save(any());
    }

    @Test
    void updatesExistingTourismPlace() {
        TourAreaItem item = tourAreaItem("3", "성산일출봉", 33.462147, 126.936424);
        TourismPlace existingPlace = TourismPlace.create(snapshot("3", "성산일출봉 옛 이름"));
        given(tourApiClient.getAreaBasedList(null, "12", 1, 100))
                .willReturn(new TourAreaPage(List.of(item), 1, 100, 1));
        given(tourismPlaceRepository.findByContentId("3")).willReturn(Optional.of(existingPlace));

        var response = tourismPlaceSyncService.syncJejuTourismPlaces();

        assertThat(response.createdCount()).isZero();
        assertThat(response.updatedCount()).isEqualTo(1);
        assertThat(existingPlace.getTitle()).isEqualTo("성산일출봉");
        verify(tourismPlaceRepository).save(existingPlace);
        verify(tourApiClient, never()).getDetail(any(), any());
    }

    @Test
    void includesJejuAddressPlaceEvenWhenTourApiAreaCodeIsMissing() {
        TourAreaItem item = new TourAreaItem(
                "126435", "12", "성산일출봉 [유네스코 세계자연유산]",
                "제주특별자치도 서귀포시 성산읍 일출로 284-12", null,
                null, null, "A01", "A0101", "A01010400", null,
                33.4580801942, 126.9415003865, null, null,
                "20031107000000", "20250312000000", Map.of("contentid", "126435")
        );
        given(tourApiClient.getAreaBasedList(null, "12", 1, 100))
                .willReturn(new TourAreaPage(List.of(item), 1, 100, 1));
        given(tourismPlaceRepository.findByContentId("126435")).willReturn(Optional.empty());

        var response = tourismPlaceSyncService.syncJejuTourismPlaces();

        assertThat(response.createdCount()).isEqualTo(1);
        ArgumentCaptor<TourismPlace> placeCaptor = ArgumentCaptor.forClass(TourismPlace.class);
        verify(tourismPlaceRepository).save(placeCaptor.capture());
        assertThat(placeCaptor.getValue().getTitle()).contains("성산일출봉");
        assertThat(placeCaptor.getValue().getAreaCode()).isNull();
    }

    private static TourAreaItem tourAreaItem(String contentId, String title, Double lat, Double lng) {
        return new TourAreaItem(
                contentId,
                "12",
                title,
                "제주특별자치도 제주시",
                null,
                "39",
                "4",
                "A01",
                "A0101",
                "A01010100",
                "064-000-0000",
                lat,
                lng,
                "https://example.com/image.jpg",
                "https://example.com/thumb.jpg",
                "20240101000000",
                "20240102000000",
                Map.of("contentid", contentId, "title", title)
        );
    }

    private static TourismPlaceSnapshot snapshot(String contentId, String title) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(126.529167, 33.361667));
        point.setSRID(4326);
        return new TourismPlaceSnapshot(
                contentId,
                "12",
                title,
                "제주특별자치도 제주시",
                null,
                null,
                "A01",
                "A0101",
                "A01010100",
                "39",
                "4",
                point,
                null,
                null,
                null,
                null,
                "20240101000000",
                "20240102000000",
                new ObjectMapper().createObjectNode(),
                LocalDateTime.now()
        );
    }
}
