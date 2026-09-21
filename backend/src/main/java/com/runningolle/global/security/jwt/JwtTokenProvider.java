package com.runningolle.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenProvider {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final SecretKey key;
    private final long expiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration
    ) {
        this.key = createSigningKey(secret);
        this.expiration = expiration;
    }

    public String createToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiration)))
                .signWith(key)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        List<GrantedAuthority> authorities = extractAuthorities(claims);
        User principal = new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AuthCookieService.COOKIE_NAME.equals(cookie.getName())
                        && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    public String resolveTokenFromCookieHeader(String cookieHeader) {
        if (!StringUtils.hasText(cookieHeader)) {
            return null;
        }

        for (String part : cookieHeader.split(";")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2 && AuthCookieService.COOKIE_NAME.equals(pair[0])
                    && StringUtils.hasText(pair[1])) {
                return pair[1];
            }
        }
        return null;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey createSigningKey(String secret) {
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        } catch (RuntimeException exception) {
            return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }

    private List<GrantedAuthority> extractAuthorities(Claims claims) {
        Object rolesClaim = claims.get("roles");

        if (!(rolesClaim instanceof List<?> roles)) {
            return List.of();
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Object role : roles) {
            authorities.add(new SimpleGrantedAuthority(String.valueOf(role)));
        }
        return authorities;
    }
}
