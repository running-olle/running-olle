package com.runningolle.global.security.oauth;

import java.time.Instant;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

interface OAuthAuthorizationRequestStateRepository
        extends JpaRepository<OAuthAuthorizationRequestState, String> {

    long deleteByExpiresAtBefore(Instant cutoff);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from OAuthAuthorizationRequestState request where request.state = :state")
    Optional<OAuthAuthorizationRequestState> findByStateForUpdate(@Param("state") String state);
}
