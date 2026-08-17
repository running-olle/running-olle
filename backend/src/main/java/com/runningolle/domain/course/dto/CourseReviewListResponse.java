package com.runningolle.domain.course.dto;

import java.math.BigDecimal;
import java.util.List;

public record CourseReviewListResponse(
        BigDecimal ratingAvg,
        long reviewCount,
        List<CourseReviewResponse> reviews
) {
}
