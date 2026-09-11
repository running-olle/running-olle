package com.runningolle.domain.tourism.service;

import com.runningolle.domain.tourism.config.TourismEventSyncProperties;
import com.runningolle.domain.tourism.dto.TourismEventSyncResponse;
import com.runningolle.domain.tourism.repository.TourismEventRepository;
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
public class TourismEventBootstrapService {

    private final TourismEventRepository tourismEventRepository;
    private final TourismEventSyncService tourismEventSyncService;
    private final TourismEventSyncProperties tourismEventSyncProperties;
    private final ExternalApiProperties externalApiProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapJejuTourismEvents() {
        if (!tourismEventSyncProperties.isBootstrapEnabled()) {
            log.info("TourAPI Jeju tourism event bootstrap skipped because bootstrap is disabled.");
            return;
        }

        if (!StringUtils.hasText(externalApiProperties.getTourApiKey())) {
            log.warn("TourAPI Jeju tourism event bootstrap skipped because external-api.tour-api-key is empty.");
            return;
        }

        long activeCount = tourismEventRepository.countByIsDeletedFalse();
        if (activeCount > 0) {
            log.info("TourAPI Jeju tourism event bootstrap skipped because tourism_events already has {} rows.", activeCount);
            return;
        }

        try {
            log.info("TourAPI Jeju tourism event bootstrap started.");
            TourismEventSyncResponse response = tourismEventSyncService.syncJejuTourismEvents();
            log.info("TourAPI Jeju tourism event bootstrap finished. response={}", response);
        } catch (RuntimeException exception) {
            log.error("TourAPI Jeju tourism event bootstrap failed.", exception);
        }
    }
}
