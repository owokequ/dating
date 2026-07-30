package com.dating.owoke.identity.shared.messaging.event;

import java.util.UUID;

public record UserProfileUpdatedV1(UUID userId, String displayName) {
}
