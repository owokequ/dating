package com.dating.owoke.identity.authentication.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.dating.owoke.identity.authentication.configuration.IdentitySecurityProperties;

@Service
public class AuthenticationCookieService {

    public static final String ACCESS_COOKIE = "OWOKE_ACCESS";
    public static final String REFRESH_COOKIE = "OWOKE_REFRESH";

    private final IdentitySecurityProperties properties;

    public AuthenticationCookieService(IdentitySecurityProperties properties) {
        this.properties = properties;
    }

    public List<ResponseCookie> sessionCookies(IssuedSession session, Instant now) {
        Duration accessMaxAge = Duration.between(now, session.accessTokenExpiresAt());
        return List.of(
                cookie(ACCESS_COOKIE, session.accessToken(), "/", accessMaxAge),
                cookie(REFRESH_COOKIE, session.refreshToken(), "/api/v1/auth", properties.refreshTokenTtl()));
    }

    public List<ResponseCookie> clearCookies() {
        return List.of(
                cookie(ACCESS_COOKIE, "", "/", Duration.ZERO),
                cookie(REFRESH_COOKIE, "", "/api/v1/auth", Duration.ZERO));
    }

    private ResponseCookie cookie(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
