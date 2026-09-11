package com.runningolle.domain.tourism.service;

import com.runningolle.domain.tourism.config.TourismEventSyncProperties;
import com.runningolle.domain.tourism.dto.TourismEventSyncResponse;
import com.runningolle.global.config.properties.ExternalApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourismEventSyncScheduler {

    private final TourismEventSyncService tourismEventSyncService;
    private final TourismEventSyncProperties tourismEventSyncProperties;
    private final ExternalApiProperties externalApiProperties;

    @Scheduled(cron = "${tourism.event-sync.cron}", zone = "${tourism.event-sync.zone}")
    public void syncJejuTourismEvents() {
        if (!tourismEventSyncProperties.isSchedulerEnabled()) {
            return;
        }

        if (!StringUtils.hasText(externalApiProperties.getTourApiKey())) {
            log.warn("Scheduled TourAPI Jeju tourism event sync skipped because external-api.tour-api-key is empty.");
            return;
        }

        try {
            log.info("Scheduled TourAPI Jeju tourism event sync started.");
            TourismEventSyncResponse response = tourismEventSyncService.syncJejuTourismEvents();
            log.info("Scheduled TourAPI Jeju tourism event sync finished. response={}", response);
        } catch (RuntimeException exception) {
            log.error("Scheduled TourAPI Jeju tourism event sync failed.", exception);
        }
    }
}
