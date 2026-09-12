package com.runningolle.domain.tourism.service;

import com.runningolle.domain.tourism.config.TourismEventSyncProperties;
import com.runningolle.domain.tourism.dto.TourismEventSyncResponse;
import com.runningolle.domain.tourism.repository.TourismEventRepository;
import com.runningolle.global.config.properties.ExternalApiProperties;
import java.time.LocalDate;
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
            log.info("Jeju tourism event bootstrap skipped because bootstrap is disabled.");
            return;
        }

        if (!hasEnabledProvider()) {
            log.warn("Jeju tourism event bootstrap skipped because no event provider is enabled.");
            return;
        }

        long displayableCount = tourismEventRepository.countByIsDeletedFalseAndEventEndDateGreaterThanEqual(LocalDate.now());
        if (displayableCount > 0) {
            log.info(
                    "Jeju tourism event bootstrap skipped because tourism_events already has {} active/upcoming rows.",
                    displayableCount
            );
            return;
        }

        try {
            log.info("Jeju tourism event bootstrap started.");
            TourismEventSyncResponse response = tourismEventSyncService.syncJejuTourismEvents();
            log.info("Jeju tourism event bootstrap finished. response={}", response);
        } catch (RuntimeException exception) {
            log.error("Jeju tourism event bootstrap failed.", exception);
        }
    }

    private boolean hasEnabledProvider() {
        return StringUtils.hasText(externalApiProperties.getTourApiKey())
                || tourismEventSyncProperties.isVisitJejuEnabled();
    }
}
