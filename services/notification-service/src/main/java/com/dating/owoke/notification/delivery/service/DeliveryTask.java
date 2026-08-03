package com.dating.owoke.notification.delivery.service;

import java.util.UUID;

import com.dating.owoke.notification.delivery.domain.DeliveryChannel;

public record DeliveryTask(
        UUID attemptId,
        UUID userId,
        DeliveryChannel channel,
        Long telegramChatId,
        String email,
        String title,
        String body,
        String actionUrl,
        String notificationType,
        UUID referenceId,
        UUID contextId,
        UUID mediaId) {
}
