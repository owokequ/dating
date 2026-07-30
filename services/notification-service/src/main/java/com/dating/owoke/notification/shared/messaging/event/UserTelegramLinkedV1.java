package com.dating.owoke.notification.shared.messaging.event;

import java.util.UUID;

public record UserTelegramLinkedV1(UUID userId, long telegramUserId, String username, boolean botAccess) {
}
