package com.dating.owoke.notification.telegram.service;

import java.util.UUID;

public record TelegramDecisionTask(
        UUID requestId,
        UUID proposalId,
        UUID actorId,
        long chatId,
        long messageId,
        String title,
        String actionUrl) {
}
