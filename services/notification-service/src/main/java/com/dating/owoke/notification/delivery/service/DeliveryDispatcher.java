package com.dating.owoke.notification.delivery.service;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.dating.owoke.notification.email.service.EmailClient;
import com.dating.owoke.notification.telegram.dto.TelegramInlineButton;
import com.dating.owoke.notification.telegram.domain.DateProposalCallback;
import com.dating.owoke.notification.telegram.domain.ReminderCallback;
import com.dating.owoke.notification.telegram.service.TelegramBotClient;
import com.dating.owoke.notification.telegram.service.TelegramCardFormatter;
import com.dating.owoke.notification.telegram.service.TelegramMediaService;
import com.dating.owoke.notification.telegram.service.TelegramPhotoResult;

@Component
@ConditionalOnProperty(prefix = "owoke.delivery", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeliveryDispatcher {

    private final DeliveryService deliveryService;
    private final TelegramBotClient telegramClient;
    private final TelegramMediaService telegramMediaService;
    private final TelegramCardFormatter telegramCardFormatter;
    private final EmailClient emailClient;

    public DeliveryDispatcher(
            DeliveryService deliveryService,
            TelegramBotClient telegramClient,
            TelegramMediaService telegramMediaService,
            TelegramCardFormatter telegramCardFormatter,
            EmailClient emailClient) {
        this.deliveryService = deliveryService;
        this.telegramClient = telegramClient;
        this.telegramMediaService = telegramMediaService;
        this.telegramCardFormatter = telegramCardFormatter;
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
                case TELEGRAM -> deliverTelegram(task);
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

    private String deliverTelegram(DeliveryTask task) {
        long chatId = required(task.telegramChatId(), "Telegram chat is not linked");
        TelegramMediaService.PreparedTelegramPhoto prepared = task.mediaId() == null
                ? telegramMediaService.preparePlaceholder()
                : telegramMediaService.prepare(task.mediaId());
        TelegramPhotoResult result = telegramClient.sendPhoto(
                chatId,
                telegramCardFormatter.format(task.title(), task.body()),
                task.actionUrl(),
                telegramButtons(task),
                prepared.photo());
        telegramMediaService.remember(prepared, result);
        return result.messageId();
    }

    private static <T> T required(T value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private static List<TelegramInlineButton> telegramButtons(DeliveryTask task) {
        if ("DATE_REMINDER_SELECTION".equals(task.notificationType()) && task.referenceId() != null) {
            return List.of(
                    new TelegramInlineButton("За 3 часа", new ReminderCallback("3h", task.referenceId()).encode()),
                    new TelegramInlineButton("За 1 час", new ReminderCallback("1h", task.referenceId()).encode()),
                    new TelegramInlineButton("За 30 минут", new ReminderCallback("30m", task.referenceId()).encode()),
                    new TelegramInlineButton("Своё время", new ReminderCallback("pick", task.referenceId()).encode()));
        }
        if (!"DATE_PROPOSAL_CREATED".equals(task.notificationType())
                || task.referenceId() == null
                || task.contextId() == null) {
            return List.of();
        }
        return List.of(
                new TelegramInlineButton("Принять", new DateProposalCallback(
                        task.referenceId(), task.contextId(), "ACCEPT").encode()),
                new TelegramInlineButton("Отклонить", new DateProposalCallback(
                        task.referenceId(), task.contextId(), "DECLINE").encode()));
    }
}
