package com.dating.owoke.notification.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String body,
        String actionUrl,
        Instant readAt,
        Instant createdAt) {
}
