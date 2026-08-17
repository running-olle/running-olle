package com.runningolle.domain.auth.dto;

public record TestTokenResponse(
        String tokenType,
        String accessToken,
        String provider,
        String providerUserId
) {
}
