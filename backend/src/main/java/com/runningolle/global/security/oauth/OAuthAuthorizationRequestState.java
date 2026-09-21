package com.runningolle.global.security.oauth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "oauth_authorization_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class OAuthAuthorizationRequestState {

    @Id
    @Column(length = 128, nullable = false)
    private String state;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Instant expiresAt;

    OAuthAuthorizationRequestState(String state, String payload, Instant expiresAt) {
        this.state = state;
        this.payload = payload;
        this.expiresAt = expiresAt;
    }

    boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
