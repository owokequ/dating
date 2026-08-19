package com.dating.owoke.identity.authentication.dto;

import java.time.Instant;

import com.dating.owoke.identity.authentication.service.IssuedSession;

public record MobileSessionResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt) {

    public static MobileSessionResponse from(IssuedSession session) {
        return new MobileSessionResponse(
                "Bearer",
                session.accessToken(),
                session.refreshToken(),
                session.accessTokenExpiresAt());
    }
}
