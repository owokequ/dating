package com.dating.owoke.notification.shared.messaging.event;

import java.util.UUID;

public record UserRegisteredV1(UUID userId, String displayName, String email) {
}
