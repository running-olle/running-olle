package com.runningolle.domain.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CourseReviewUpdateRequest(
        @Min(1) @Max(5) int rating,
        @Size(max = 1000) String content
) {
}
