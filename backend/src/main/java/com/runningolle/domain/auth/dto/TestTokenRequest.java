package com.runningolle.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TestTokenRequest(
        @NotBlank String kakaoId
) {
}
