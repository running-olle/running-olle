package com.runningolle.domain.home.service;

import com.runningolle.domain.home.config.HomeRecommendationProperties;
import com.runningolle.domain.home.dto.CourseRecommendationSyncResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseRecommendationSyncScheduler {

    private final CourseRecommendationSyncService courseRecommendationSyncService;
    private final HomeRecommendationProperties homeRecommendationProperties;

    @Scheduled(
            cron = "${home.recommendation.sync.cron:0 0 4 * * *}",
            zone = "${home.recommendation.sync.zone:Asia/Seoul}"
    )
    public void syncPublicCourseRecommendations() {
        if (!homeRecommendationProperties.getSync().isSchedulerEnabled()) {
            return;
        }

        CourseRecommendationSyncResponse response = courseRecommendationSyncService.syncPublicCourseRecommendations();
        log.info(
                "Home recommendation sync completed. targetCourseCount={}, documentCreatedCount={}, "
                        + "documentUpdatedCount={}, documentDeletedCount={}, documentSkippedCount={}, "
                        + "embeddingSyncedCount={}, embeddingDeletedCount={}, embeddingSkippedCount={}, "
                        + "embeddingFailedCount={}, syncedAt={}",
                response.targetCourseCount(),
                response.documentCreatedCount(),
                response.documentUpdatedCount(),
                response.documentDeletedCount(),
                response.documentSkippedCount(),
                response.embeddingSyncedCount(),
                response.embeddingDeletedCount(),
                response.embeddingSkippedCount(),
                response.embeddingFailedCount(),
                response.syncedAt()
        );
    }
}
