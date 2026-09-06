package com.runningolle.domain.running.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveRunningCourseRequest(
        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 1000)
        String description,

        Boolean isPublic
) {
}
