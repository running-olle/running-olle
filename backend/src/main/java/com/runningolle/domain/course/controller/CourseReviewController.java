package com.runningolle.domain.course.controller;

import com.runningolle.domain.course.dto.CourseReviewCreateRequest;
import com.runningolle.domain.course.dto.CourseReviewListResponse;
import com.runningolle.domain.course.dto.CourseReviewResponse;
import com.runningolle.domain.course.dto.CourseReviewUpdateRequest;
import com.runningolle.domain.course.service.CourseReviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses/{courseId}/reviews")
public class CourseReviewController {

    private final CourseReviewService courseReviewService;

    @GetMapping
    public CourseReviewListResponse getCourseReviews(
            @PathVariable UUID courseId,
            Authentication authentication
    ) {
        return courseReviewService.getCourseReviews(courseId, authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseReviewResponse createReview(
            @PathVariable UUID courseId,
            @Valid @RequestBody CourseReviewCreateRequest request,
            Authentication authentication
    ) {
        return courseReviewService.createReview(courseId, request, authentication.getName());
    }

    @PatchMapping("/{reviewId}")
    public CourseReviewResponse updateReview(
            @PathVariable UUID courseId,
            @PathVariable UUID reviewId,
            @Valid @RequestBody CourseReviewUpdateRequest request,
            Authentication authentication
    ) {
        return courseReviewService.updateReview(courseId, reviewId, request, authentication.getName());
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(
            @PathVariable UUID courseId,
            @PathVariable UUID reviewId,
            Authentication authentication
    ) {
        courseReviewService.deleteReview(courseId, reviewId, authentication.getName());
    }
}
