package com.dating.owoke.identity.authentication.service;

import java.util.UUID;

public record RefreshGrant(UUID userId, String refreshToken) {
}
