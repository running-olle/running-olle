package com.runningolle.domain.home.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.runningolle.domain.home.config.HomeRecommendationProperties;
import com.runningolle.domain.home.dto.CourseRecommendationSyncResponse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseRecommendationSyncSchedulerTest {

    @Mock
    private CourseRecommendationSyncService courseRecommendationSyncService;

    @Test
    void skipsWhenSchedulerIsDisabled() {
        HomeRecommendationProperties properties = new HomeRecommendationProperties();
        CourseRecommendationSyncScheduler scheduler = new CourseRecommendationSyncScheduler(
                courseRecommendationSyncService,
                properties
        );

        scheduler.syncPublicCourseRecommendations();

        then(courseRecommendationSyncService).should(never()).syncPublicCourseRecommendations();
    }

    @Test
    void syncsWhenSchedulerIsEnabled() {
        HomeRecommendationProperties properties = new HomeRecommendationProperties();
        properties.getSync().setSchedulerEnabled(true);
        CourseRecommendationSyncScheduler scheduler = new CourseRecommendationSyncScheduler(
                courseRecommendationSyncService,
                properties
        );
        given(courseRecommendationSyncService.syncPublicCourseRecommendations())
                .willReturn(new CourseRecommendationSyncResponse(
                        1,
                        1,
                        0,
                        0,
                        0,
                        1,
                        0,
                        0,
                        0,
                        LocalDateTime.of(2026, 9, 6, 4, 0)
                ));

        scheduler.syncPublicCourseRecommendations();

        then(courseRecommendationSyncService).should().syncPublicCourseRecommendations();
    }
}
