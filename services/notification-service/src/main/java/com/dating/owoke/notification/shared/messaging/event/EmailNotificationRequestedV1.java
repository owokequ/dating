package com.dating.owoke.notification.shared.messaging.event;

import java.util.UUID;

public record EmailNotificationRequestedV1(UUID userId, String email, String template, String actionUrl) {
}
