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
import com.runningolle.domain.tourism.client.TourApiClient.TourDetail;
import com.runningolle.domain.tourism.client.VisitJejuEventClient;
import com.runningolle.domain.tourism.client.VisitJejuEventClient.VisitJejuEventItem;
import com.runningolle.domain.tourism.client.VisitJejuEventClient.VisitJejuEventPage;
import com.runningolle.domain.tourism.config.TourismEventSyncProperties;
import com.runningolle.domain.tourism.entity.TourismEvent;
import com.runningolle.domain.tourism.repository.TourismEventRepository;
import com.runningolle.global.config.properties.ExternalApiProperties;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TourismEventSyncServiceTest {

    @Mock
    private TourApiClient tourApiClient;

    @Mock
    private VisitJejuEventClient visitJejuEventClient;

    @Mock
    private TourismEventRepository tourismEventRepository;

    private TourismEventSyncService tourismEventSyncService;
    private ExternalApiProperties externalApiProperties;

    @BeforeEach
    void setUp() {
        TourismEventSyncProperties properties = new TourismEventSyncProperties();
        properties.setAreaCode("39");
        properties.setPageSize(100);
        properties.setLookBackDays(1);
        properties.setLookAheadDays(30);
        properties.setVisitJejuEnabled(false);
        externalApiProperties = new ExternalApiProperties();
        externalApiProperties.setTourApiKey("tour-api-key");

        tourismEventSyncService = new TourismEventSyncService(
                tourApiClient,
                visitJejuEventClient,
                tourismEventRepository,
                properties,
                externalApiProperties,
                new ObjectMapper()
        );
    }

    @Test
    void createsRunningRelatedTourismEventWithDetailData() {
        TourAreaItem item = tourAreaItem("10", "제주국제관광마라톤축제", 33.53, 126.84);
        TourDetail detail = new TourDetail(
                "10",
                "15",
                "제주국제관광마라톤축제",
                "제주특별자치도 제주시 구좌읍",
                null,
                "39",
                "4",
                "A02",
                "A0208",
                "A02081300",
                33.531,
                126.841,
                "푸른 바다를 따라 달리는 마라톤 행사입니다.<br>누구나 참여할 수 있어요.",
                "https://example.com/marathon.jpg",
                null,
                Map.of(
                        "detailCommon2", Map.of("contentid", "10"),
                        "detailIntro2", Map.of(
                                "eventstartdate", "20260607",
                                "eventenddate", "20260607",
                                "eventplace", "구좌체육공원",
                                "sponsor1", "제주특별자치도"
                        )
                )
        );
        given(tourApiClient.getAreaBasedList("39", "15", 1, 100))
                .willReturn(new TourAreaPage(List.of(item), 1, 100, 1));
        given(tourApiClient.getDetail("10", "15")).willReturn(Optional.of(detail));
        given(tourismEventRepository.findByContentId("10")).willReturn(Optional.empty());

        var response = tourismEventSyncService.syncJejuTourismEvents();

        assertThat(response.fetchedCount()).isEqualTo(1);
        assertThat(response.createdCount()).isEqualTo(1);
        assertThat(response.updatedCount()).isZero();
        assertThat(response.skippedCount()).isZero();

        ArgumentCaptor<TourismEvent> eventCaptor = ArgumentCaptor.forClass(TourismEvent.class);
        verify(tourismEventRepository).save(eventCaptor.capture());
        TourismEvent savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getContentId()).isEqualTo("10");
        assertThat(savedEvent.getTitle()).isEqualTo("제주국제관광마라톤축제");
        assertThat(savedEvent.getVenueName()).isEqualTo("구좌체육공원");
        assertThat(savedEvent.getOrganizer()).isEqualTo("제주특별자치도");
        assertThat(savedEvent.getEventStartDate()).isEqualTo(LocalDate.of(2026, 6, 7));
        assertThat(savedEvent.getRunningRelated()).isTrue();
        assertThat(savedEvent.getRunningScore()).isGreaterThanOrEqualTo(5);
        assertThat(savedEvent.getOverview()).doesNotContain("<br>");
        assertThat(savedEvent.getLocation().getY()).isEqualTo(33.531);
        assertThat(savedEvent.getLocation().getX()).isEqualTo(126.841);
        assertThat(savedEvent.getRawData().has("areaBasedList2")).isTrue();
        assertThat(savedEvent.getRawData().has("detail")).isTrue();
    }

    @Test
    void skipsEventWithoutDate() {
        TourAreaItem item = new TourAreaItem(
                "11",
                "15",
                "날짜 없는 행사",
                "제주특별자치도 제주시",
                null,
                "39",
                "4",
                "A02",
                "A0208",
                "A02081300",
                null,
                33.53,
                126.84,
                null,
                null,
                null,
                null,
                Map.of("contentid", "11")
        );
        given(tourApiClient.getAreaBasedList("39", "15", 1, 100))
                .willReturn(new TourAreaPage(List.of(item), 1, 100, 1));
        given(tourApiClient.getDetail("11", "15")).willReturn(Optional.empty());

        var response = tourismEventSyncService.syncJejuTourismEvents();

        assertThat(response.fetchedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.createdCount()).isZero();
        verify(tourismEventRepository, never()).save(any());
    }

    @Test
    void createsActiveVisitJejuEvent() {
        LocalDate today = LocalDate.now();
        TourismEventSyncProperties properties = new TourismEventSyncProperties();
        properties.setAreaCode("39");
        properties.setPageSize(100);
        properties.setLookAheadDays(1);
        properties.setVisitJejuEnabled(true);
        externalApiProperties.setTourApiKey("tour-api-key");

        tourismEventSyncService = new TourismEventSyncService(
                tourApiClient,
                visitJejuEventClient,
                tourismEventRepository,
                properties,
                externalApiProperties,
                new ObjectMapper()
        );

        VisitJejuEventItem item = new VisitJejuEventItem(
                "CNTS_1",
                "2026 제주올레걷기축제",
                "제주특별자치도 제주시 조천읍 조천18길 11-1",
                "제주특별자치도 제주시 조천읍 조천리 1176-1",
                "조천체육공원",
                "064-762-2190",
                "(사)제주올레",
                today,
                today.plusDays(2),
                "cate0000001361",
                "축제",
                33.5390118,
                126.6427149,
                "https://example.com/olle.jpg",
                "https://example.com/olle-thumb.jpg",
                "제주올레길을 걷는 축제",
                "제주올레걷기축제, 걷기, 올레길",
                "20260701145053",
                "20260701180257",
                "https://www.visitjeju.net/kr/festival/view?contentsid=CNTS_1",
                new ObjectMapper().createObjectNode()
        );

        given(tourApiClient.getAreaBasedList("39", "15", 1, 100))
                .willReturn(new TourAreaPage(List.of(), 1, 100, 0));
        given(visitJejuEventClient.getFestivalEvents(
                today.getYear(),
                String.format("%02d", today.getMonthValue()),
                1,
                100
        )).willReturn(new VisitJejuEventPage(List.of(item), 1, 100, 1));
        given(tourismEventRepository.findByContentId("VISITJEJU:CNTS_1")).willReturn(Optional.empty());

        var response = tourismEventSyncService.syncJejuTourismEvents();

        assertThat(response.fetchedCount()).isEqualTo(1);
        assertThat(response.createdCount()).isEqualTo(1);

        ArgumentCaptor<TourismEvent> eventCaptor = ArgumentCaptor.forClass(TourismEvent.class);
        verify(tourismEventRepository).save(eventCaptor.capture());
        TourismEvent savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getContentId()).isEqualTo("VISITJEJU:CNTS_1");
        assertThat(savedEvent.getProviderName()).isEqualTo("비짓제주");
        assertThat(savedEvent.getSourceUrl()).isEqualTo("https://www.visitjeju.net/kr/festival/view?contentsid=CNTS_1");
        assertThat(savedEvent.getRunningRelated()).isTrue();
        assertThat(savedEvent.getCategory2()).isEqualTo("A0207");
    }

    private static TourAreaItem tourAreaItem(String contentId, String title, Double lat, Double lng) {
        return new TourAreaItem(
                contentId,
                "15",
                title,
                "제주특별자치도 제주시",
                null,
                "39",
                "4",
                "A02",
                "A0208",
                "A02081300",
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
}
