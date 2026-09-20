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
@ConditionalOnProperty(prefix = "tourism.sync", name = "detail-scheduler-enabled", havingValue = "true")
public class TourismPlaceDetailSyncScheduler {

    private final TourismPlaceDetailSyncService detailSyncService;
    private final ExternalApiProperties externalApiProperties;

    @Scheduled(cron = "${tourism.sync.detail-cron}", zone = "${tourism.sync.zone}")
    public void syncPendingDetails() {
        if (!StringUtils.hasText(externalApiProperties.getTourApiKey())) {
            log.warn("Scheduled TourAPI tourism place detail sync skipped because the API key is empty.");
            return;
        }

        try {
            log.info("Scheduled TourAPI tourism place detail sync started.");
            var response = detailSyncService.syncPendingDetails();
            log.info("Scheduled TourAPI tourism place detail sync finished. response={}", response);
        } catch (RuntimeException exception) {
            log.error("Scheduled TourAPI tourism place detail sync failed.", exception);
        }
    }
}
