package com.dating.owoke.identity.authentication.service;

import java.time.Instant;

public record IssuedSession(
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt) {
}
