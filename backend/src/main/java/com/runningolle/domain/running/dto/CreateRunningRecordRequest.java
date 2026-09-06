package com.runningolle.domain.running.dto;

import com.runningolle.domain.running.enums.RunningMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateRunningRecordRequest(
        @NotNull @Size(min = 1, max = 100_000) List<@Valid RoutePoint> route,
        @PositiveOrZero double totalDistanceMeters,
        @PositiveOrZero int totalDurationSeconds,
        @DecimalMin("0.0") Double averagePace,
        @PositiveOrZero double calories,
        @NotNull OffsetDateTime startedAt,
        @NotNull OffsetDateTime endedAt,
        UUID courseId,
        RunningMode runningMode
) {
    public record RoutePoint(
            @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") double longitude
    ) {
    }
}
