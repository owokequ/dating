package com.dating.owoke.notification.telegram.service;

import java.util.UUID;

public record TelegramDecisionTask(
        UUID requestId, long chatId, long messageId, String caption, String actionUrl) {
}
