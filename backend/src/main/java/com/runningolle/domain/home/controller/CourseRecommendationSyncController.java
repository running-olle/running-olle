package com.runningolle.domain.home.controller;

import com.runningolle.domain.home.dto.CourseRecommendationSyncResponse;
import com.runningolle.domain.home.config.HomeRecommendationProperties;
import com.runningolle.domain.home.service.CourseRecommendationSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/home/recommendations")
public class CourseRecommendationSyncController {

    private final CourseRecommendationSyncService courseRecommendationSyncService;
    private final HomeRecommendationProperties homeRecommendationProperties;

    @PostMapping("/sync/courses")
    public CourseRecommendationSyncResponse syncCourseRecommendations() {
        if (!homeRecommendationProperties.isManualSyncEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Home recommendation manual sync is disabled.");
        }
        return courseRecommendationSyncService.syncPublicCourseRecommendations();
    }
}
