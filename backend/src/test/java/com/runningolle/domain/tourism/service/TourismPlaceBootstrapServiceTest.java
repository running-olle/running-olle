package com.runningolle.domain.tourism.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.runningolle.domain.tourism.config.TourismSyncProperties;
import com.runningolle.domain.tourism.repository.TourismPlaceRepository;
import com.runningolle.global.config.properties.ExternalApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TourismPlaceBootstrapServiceTest {

    @Mock
    private TourismPlaceRepository tourismPlaceRepository;

    @Mock
    private TourismPlaceSyncService tourismPlaceSyncService;

    private TourismSyncProperties tourismSyncProperties;
    private ExternalApiProperties externalApiProperties;
    private TourismPlaceBootstrapService tourismPlaceBootstrapService;

    @BeforeEach
    void setUp() {
        tourismSyncProperties = new TourismSyncProperties();
        externalApiProperties = new ExternalApiProperties();
        externalApiProperties.setTourApiKey("tour-api-key");
        tourismPlaceBootstrapService = new TourismPlaceBootstrapService(
                tourismPlaceRepository,
                tourismPlaceSyncService,
                tourismSyncProperties,
                externalApiProperties
        );
    }

    @Test
    void backfillsTourismPlacesWhenCacheIsEmpty() {
        given(tourismPlaceRepository.countByIsDeletedFalseAndContentTypeIdIn(tourismSyncProperties.getContentTypeIds()))
                .willReturn(0L);

        tourismPlaceBootstrapService.backfillJejuTourismPlacesIfEmpty();

        verify(tourismPlaceSyncService).syncJejuTourismPlaces();
    }

    @Test
    void skipsBackfillWhenCacheAlreadyExists() {
        given(tourismPlaceRepository.countByIsDeletedFalseAndContentTypeIdIn(tourismSyncProperties.getContentTypeIds()))
                .willReturn(1L);

        tourismPlaceBootstrapService.backfillJejuTourismPlacesIfEmpty();

        verify(tourismPlaceSyncService, never()).syncJejuTourismPlaces();
    }

    @Test
    void skipsBackfillWhenBootstrapIsDisabled() {
        tourismSyncProperties.setBootstrapEnabled(false);

        tourismPlaceBootstrapService.backfillJejuTourismPlacesIfEmpty();

        verify(tourismPlaceRepository, never()).countByIsDeletedFalseAndContentTypeIdIn(tourismSyncProperties.getContentTypeIds());
        verify(tourismPlaceSyncService, never()).syncJejuTourismPlaces();
    }

    @Test
    void skipsBackfillWhenTourApiKeyIsEmpty() {
        externalApiProperties.setTourApiKey("");

        tourismPlaceBootstrapService.backfillJejuTourismPlacesIfEmpty();

        verify(tourismPlaceRepository, never()).countByIsDeletedFalseAndContentTypeIdIn(tourismSyncProperties.getContentTypeIds());
        verify(tourismPlaceSyncService, never()).syncJejuTourismPlaces();
    }
}
