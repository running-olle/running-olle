package com.runningolle.domain.tourism.service;

import com.runningolle.domain.tourism.config.TourismSyncProperties;
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

    private final TourismPlaceSyncService tourismPlaceSyncService;
    private final TourismSyncProperties tourismSyncProperties;
    private final ExternalApiProperties externalApiProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void syncJejuTourismPlaceInventoryOnStartup() {
        if (!tourismSyncProperties.isBootstrapEnabled()) {
            log.info("TourAPI tourism place bootstrap is disabled.");
            return;
        }
        if (!StringUtils.hasText(externalApiProperties.getTourApiKey())) {
            log.warn("TourAPI tourism place bootstrap skipped because external-api.tour-api-key is empty.");
            return;
        }

        try {
            log.info("TourAPI tourism place startup inventory sync started.");
            var response = tourismPlaceSyncService.syncJejuTourismPlaces();
            log.info("TourAPI tourism place startup inventory sync finished. response={}", response);
        } catch (RuntimeException exception) {
            log.error("TourAPI tourism place bootstrap failed.", exception);
        }
    }
}
