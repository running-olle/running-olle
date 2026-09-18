package com.runningolle.domain.course.controller;

import com.runningolle.domain.course.dto.CourseReviewRequest;
import com.runningolle.domain.course.dto.CourseReviewResponse;
import com.runningolle.domain.course.dto.CourseReviewUpdateRequest;
import com.runningolle.domain.course.service.CourseReviewService;
import jakarta.validation.Valid;
import java.util.List;
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
    public List<CourseReviewResponse> getReviews(
            Authentication authentication,
            @PathVariable UUID courseId
    ) {
        return courseReviewService.getReviews(UUID.fromString(authentication.getName()), courseId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseReviewResponse createReview(
            Authentication authentication,
            @PathVariable UUID courseId,
            @Valid @RequestBody CourseReviewRequest request
    ) {
        return courseReviewService.createReview(UUID.fromString(authentication.getName()), courseId, request);
    }

    @PatchMapping("/{reviewId}")
    public CourseReviewResponse updateReview(
            Authentication authentication,
            @PathVariable UUID courseId,
            @PathVariable UUID reviewId,
            @Valid @RequestBody CourseReviewUpdateRequest request
    ) {
        return courseReviewService.updateReview(
                UUID.fromString(authentication.getName()),
                courseId,
                reviewId,
                request
        );
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(
            Authentication authentication,
            @PathVariable UUID courseId,
            @PathVariable UUID reviewId
    ) {
        courseReviewService.deleteReview(UUID.fromString(authentication.getName()), courseId, reviewId);
    }
}
