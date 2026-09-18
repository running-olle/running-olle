package com.runningolle.domain.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CourseReviewRequest(
        @NotNull UUID runningRecordId,
        @Min(1) @Max(5) int rating,
        @Size(max = 1000) String content
) {
}
