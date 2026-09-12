package com.runningolle.domain.tourism.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.runningolle.domain.tourism.config.TourismEventSyncProperties;
import com.runningolle.domain.tourism.repository.TourismEventRepository;
import com.runningolle.global.config.properties.ExternalApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TourismEventBootstrapServiceTest {

    @Mock
    private TourismEventRepository tourismEventRepository;

    @Mock
    private TourismEventSyncService tourismEventSyncService;

    private TourismEventSyncProperties tourismEventSyncProperties;
    private ExternalApiProperties externalApiProperties;
    private TourismEventBootstrapService tourismEventBootstrapService;

    @BeforeEach
    void setUp() {
        tourismEventSyncProperties = new TourismEventSyncProperties();
        externalApiProperties = new ExternalApiProperties();
        externalApiProperties.setTourApiKey("tour-api-key");
        tourismEventBootstrapService = new TourismEventBootstrapService(
                tourismEventRepository,
                tourismEventSyncService,
                tourismEventSyncProperties,
                externalApiProperties
        );
    }

    @Test
    void backfillsTourismEventsWhenCacheIsEmpty() {
        given(tourismEventRepository.countByIsDeletedFalseAndEventEndDateGreaterThanEqual(any())).willReturn(0L);

        tourismEventBootstrapService.bootstrapJejuTourismEvents();

        verify(tourismEventSyncService).syncJejuTourismEvents();
    }

    @Test
    void skipsBackfillWhenActiveOrUpcomingEventAlreadyExists() {
        given(tourismEventRepository.countByIsDeletedFalseAndEventEndDateGreaterThanEqual(any())).willReturn(1L);

        tourismEventBootstrapService.bootstrapJejuTourismEvents();

        verify(tourismEventSyncService, never()).syncJejuTourismEvents();
    }

    @Test
    void backfillsWhenTourApiKeyIsEmptyButVisitJejuIsEnabled() {
        externalApiProperties.setTourApiKey("");
        given(tourismEventRepository.countByIsDeletedFalseAndEventEndDateGreaterThanEqual(any())).willReturn(0L);

        tourismEventBootstrapService.bootstrapJejuTourismEvents();

        verify(tourismEventSyncService).syncJejuTourismEvents();
    }

    @Test
    void skipsBackfillWhenNoEventProviderIsEnabled() {
        externalApiProperties.setTourApiKey("");
        tourismEventSyncProperties.setVisitJejuEnabled(false);

        tourismEventBootstrapService.bootstrapJejuTourismEvents();

        verify(tourismEventRepository, never()).countByIsDeletedFalseAndEventEndDateGreaterThanEqual(any());
        verify(tourismEventSyncService, never()).syncJejuTourismEvents();
    }
}
