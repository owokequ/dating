package com.dating.owoke.identity.shared.messaging.event;

import java.util.UUID;

public record EmailNotificationRequestedV1(
        UUID userId,
        String email,
        String template,
        String actionUrl) {
}
