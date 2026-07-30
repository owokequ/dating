package com.dating.owoke.notification.delivery.service;

import java.util.UUID;

import com.dating.owoke.notification.delivery.domain.DeliveryChannel;

public record DeliveryTask(
        UUID attemptId,
        DeliveryChannel channel,
        Long telegramChatId,
        String email,
        String title,
        String body,
        String actionUrl) {
}
