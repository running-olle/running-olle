package com.runningolle.domain.tourism.service;

import com.runningolle.global.config.properties.ExternalApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tourism.sync", name = "scheduler-enabled", havingValue = "true")
public class TourismPlaceSyncScheduler {

    private final TourismPlaceSyncService tourismPlaceSyncService;
    private final ExternalApiProperties externalApiProperties;

    @Scheduled(cron = "${tourism.sync.cron}", zone = "${tourism.sync.zone}")
    public void syncJejuTourismPlaces() {
        if (!StringUtils.hasText(externalApiProperties.getTourApiKey())) {
            log.warn("Scheduled TourAPI Jeju tourism place sync skipped because external-api.tour-api-key is empty.");
            return;
        }

        try {
            log.info("Scheduled TourAPI Jeju tourism place sync started.");
            var response = tourismPlaceSyncService.syncJejuTourismPlaces();
            log.info("Scheduled TourAPI Jeju tourism place sync finished. response={}", response);
        } catch (RuntimeException exception) {
            log.error("Scheduled TourAPI Jeju tourism place sync failed.", exception);
        }
    }
}
