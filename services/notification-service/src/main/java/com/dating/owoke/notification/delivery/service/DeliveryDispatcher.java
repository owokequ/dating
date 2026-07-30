package com.dating.owoke.notification.delivery.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.dating.owoke.notification.email.service.EmailClient;
import com.dating.owoke.notification.telegram.service.TelegramBotClient;

@Component
@ConditionalOnProperty(prefix = "owoke.delivery", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeliveryDispatcher {

    private final DeliveryService deliveryService;
    private final TelegramBotClient telegramClient;
    private final EmailClient emailClient;

    public DeliveryDispatcher(
            DeliveryService deliveryService,
            TelegramBotClient telegramClient,
            EmailClient emailClient) {
        this.deliveryService = deliveryService;
        this.telegramClient = telegramClient;
        this.emailClient = emailClient;
    }

    @Scheduled(fixedDelayString = "${owoke.notification.delivery-fixed-delay:1000}")
    public void dispatch() {
        for (DeliveryTask task : deliveryService.claim()) {
            deliver(task);
        }
    }

    private void deliver(DeliveryTask task) {
        try {
            String providerMessageId = switch (task.channel()) {
                case TELEGRAM -> telegramClient.send(
                        required(task.telegramChatId(), "Telegram chat is not linked"),
                        task.title() + "\n\n" + task.body(),
                        task.actionUrl());
                case EMAIL -> emailClient.send(
                        required(task.email(), "Email is not available"),
                        task.title(),
                        task.body(),
                        task.actionUrl());
            };
            deliveryService.markSent(task, providerMessageId);
        } catch (Exception exception) {
            deliveryService.markFailed(task, exception);
        }
    }

    private static <T> T required(T value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
        return value;
    }
}
