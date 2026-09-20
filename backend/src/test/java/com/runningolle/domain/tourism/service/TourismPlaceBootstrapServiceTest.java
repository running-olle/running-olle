package com.runningolle.domain.tourism.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.runningolle.domain.tourism.config.TourismSyncProperties;
import com.runningolle.global.config.properties.ExternalApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TourismPlaceBootstrapServiceTest {

    @Mock
    private TourismPlaceSyncService tourismPlaceSyncService;

    @Mock
    private TourismPlaceDetailSyncService tourismPlaceDetailSyncService;

    private TourismSyncProperties tourismSyncProperties;
    private ExternalApiProperties externalApiProperties;
    private TourismPlaceBootstrapService tourismPlaceBootstrapService;

    @BeforeEach
    void setUp() {
        tourismSyncProperties = new TourismSyncProperties();
        externalApiProperties = new ExternalApiProperties();
        externalApiProperties.setTourApiKey("tour-api-key");
        tourismPlaceBootstrapService = new TourismPlaceBootstrapService(
                tourismPlaceSyncService,
                tourismPlaceDetailSyncService,
                tourismSyncProperties,
                externalApiProperties
        );
    }

    @Test
    void refreshesLightweightInventoryOnEveryStartup() {
        tourismSyncProperties.setDetailSchedulerEnabled(true);

        tourismPlaceBootstrapService.syncJejuTourismPlaceInventoryOnStartup();

        verify(tourismPlaceSyncService).syncJejuTourismPlaces();
        verify(tourismPlaceDetailSyncService).syncPendingDetails();
    }

    @Test
    void stillEnrichesPendingDetailsWhenInventorySyncFails() {
        tourismSyncProperties.setDetailSchedulerEnabled(true);
        given(tourismPlaceSyncService.syncJejuTourismPlaces())
                .willThrow(new RuntimeException("inventory unavailable"));

        tourismPlaceBootstrapService.syncJejuTourismPlaceInventoryOnStartup();

        verify(tourismPlaceDetailSyncService).syncPendingDetails();
    }

    @Test
    void skipsBackfillWhenBootstrapIsDisabled() {
        tourismSyncProperties.setBootstrapEnabled(false);

        tourismPlaceBootstrapService.syncJejuTourismPlaceInventoryOnStartup();

        verify(tourismPlaceSyncService, never()).syncJejuTourismPlaces();
        verify(tourismPlaceDetailSyncService, never()).syncPendingDetails();
    }

    @Test
    void skipsBackfillWhenTourApiKeyIsEmpty() {
        externalApiProperties.setTourApiKey("");

        tourismPlaceBootstrapService.syncJejuTourismPlaceInventoryOnStartup();

        verify(tourismPlaceSyncService, never()).syncJejuTourismPlaces();
        verify(tourismPlaceDetailSyncService, never()).syncPendingDetails();
    }
}
