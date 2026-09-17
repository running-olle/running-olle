package com.runningolle.domain.meetup.dto;

import com.runningolle.domain.meetup.enums.JoinMethod;
import com.runningolle.domain.user.enums.ThemeCode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MeetupCreateRequest(
        UUID courseId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotNull @Future LocalDateTime meetupDate,
        @NotNull @Min(2) @Max(100) Integer maxParticipants,
        @DecimalMin("0.00") @DecimalMax("99.99") BigDecimal targetPace,
        @NotBlank @Size(max = 200) String meetingPlace,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @NotNull JoinMethod joinMethod,
        ThemeCode themeCode
) {
}
