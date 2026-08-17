package com.runningolle.domain.course.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CourseReviewCreateRequest(
        @NotNull UUID runningRecordId,
        @NotNull @DecimalMin("0.5") @DecimalMax("5.0") BigDecimal rating,
        String content
) {
}
