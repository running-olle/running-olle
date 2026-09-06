package com.runningolle.domain.tourism.service;

import com.runningolle.domain.tourism.config.TourismSyncProperties;
import com.runningolle.domain.tourism.repository.TourismPlaceRepository;
import com.runningolle.global.config.properties.ExternalApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourismPlaceBootstrapService {

    private final TourismPlaceRepository tourismPlaceRepository;
    private final TourismPlaceSyncService tourismPlaceSyncService;
    private final TourismSyncProperties tourismSyncProperties;
    private final ExternalApiProperties externalApiProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void backfillJejuTourismPlacesIfEmpty() {
        if (!tourismSyncProperties.isBootstrapEnabled()) {
            log.info("TourAPI tourism place bootstrap is disabled.");
            return;
        }
        if (!StringUtils.hasText(externalApiProperties.getTourApiKey())) {
            log.warn("TourAPI tourism place bootstrap skipped because external-api.tour-api-key is empty.");
            return;
        }

        long activePlaceCount = tourismPlaceRepository.countByIsDeletedFalseAndContentTypeIdIn(
                tourismSyncProperties.getContentTypeIds()
        );
        if (activePlaceCount > 0) {
            log.info("TourAPI tourism place bootstrap skipped. activePlaceCount={}", activePlaceCount);
            return;
        }

        try {
            log.info("TourAPI tourism place bootstrap started because tourism_places is empty.");
            var response = tourismPlaceSyncService.syncJejuTourismPlaces();
            log.info("TourAPI tourism place bootstrap finished. response={}", response);
        } catch (RuntimeException exception) {
            log.error("TourAPI tourism place bootstrap failed.", exception);
        }
    }
}
