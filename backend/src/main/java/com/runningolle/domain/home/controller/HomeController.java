package com.runningolle.domain.home.controller;

import com.runningolle.domain.home.dto.PopularCoursesResponse;
import com.runningolle.domain.home.dto.RecommendedCoursesResponse;
import com.runningolle.domain.home.service.CourseRecommendationService;
import com.runningolle.domain.home.service.PopularCourseService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
public class HomeController {

    private final CourseRecommendationService courseRecommendationService;
    private final PopularCourseService popularCourseService;

    @GetMapping("/popular-courses")
    public PopularCoursesResponse getPopularCourses() {
        return popularCourseService.getPopularCourses();
    }

    @GetMapping("/recommended-courses")
    public RecommendedCoursesResponse getRecommendedCourses(
            Authentication authentication,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude
    ) {
        return courseRecommendationService.getRecommendedCourses(
                UUID.fromString(authentication.getName()),
                latitude,
                longitude
        );
    }
}
