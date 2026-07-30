package com.dating.owoke.identity.telegram.dto;

import java.time.Instant;

public record TelegramLinkResponse(String url, Instant expiresAt) {
}
