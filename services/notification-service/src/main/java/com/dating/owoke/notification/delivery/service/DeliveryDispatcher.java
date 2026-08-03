package com.dating.owoke.notification.delivery.service;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.dating.owoke.notification.email.service.EmailClient;
import com.dating.owoke.notification.telegram.dto.TelegramInlineButton;
import com.dating.owoke.notification.telegram.domain.DateProposalCallback;
import com.dating.owoke.notification.telegram.service.TelegramBotClient;
import com.dating.owoke.notification.telegram.service.TelegramCardFormatter;
import com.dating.owoke.notification.telegram.service.TelegramDateCardService;
import com.dating.owoke.notification.telegram.service.TelegramMediaService;
import com.dating.owoke.notification.telegram.service.TelegramPhotoResult;

@Component
@ConditionalOnProperty(prefix = "owoke.delivery", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeliveryDispatcher {

    private final DeliveryService deliveryService;
    private final TelegramBotClient telegramClient;
    private final TelegramMediaService telegramMediaService;
    private final TelegramCardFormatter telegramCardFormatter;
    private final TelegramDateCardService telegramDateCardService;
    private final EmailClient emailClient;

    public DeliveryDispatcher(
            DeliveryService deliveryService,
            TelegramBotClient telegramClient,
            TelegramMediaService telegramMediaService,
            TelegramCardFormatter telegramCardFormatter,
            TelegramDateCardService telegramDateCardService,
            EmailClient emailClient) {
        this.deliveryService = deliveryService;
        this.telegramClient = telegramClient;
        this.telegramMediaService = telegramMediaService;
        this.telegramCardFormatter = telegramCardFormatter;
        this.telegramDateCardService = telegramDateCardService;
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
        String caption = telegramCardFormatter.format(task.title(), task.body());
        List<TelegramInlineButton> buttons = telegramButtons(task);
        if (updatesExistingCard(task)) {
            return telegramDateCardService.updateExisting(
                    task.referenceId(), task.userId(), caption, task.actionUrl(), buttons)
                    .orElseGet(() -> sendTelegramCard(task, chatId, caption, buttons));
        }
        return sendTelegramCard(task, chatId, caption, buttons);
    }

    private String sendTelegramCard(
            DeliveryTask task,
            long chatId,
            String caption,
            List<TelegramInlineButton> buttons) {
        TelegramMediaService.PreparedTelegramPhoto prepared = task.mediaId() == null
                ? telegramMediaService.preparePlaceholder()
                : telegramMediaService.prepare(task.mediaId());
        TelegramPhotoResult result = telegramClient.sendPhoto(
                chatId,
                caption,
                task.actionUrl(),
                buttons,
                prepared.photo());
        telegramMediaService.remember(prepared, result);
        if (isDateCard(task)) {
            telegramDateCardService.rememberSent(
                    task.referenceId(), task.userId(), chatId, Long.parseLong(result.messageId()),
                    caption, task.actionUrl(), task.mediaId());
        }
        return result.messageId();
    }

    private static boolean updatesExistingCard(DeliveryTask task) {
        return isDateCard(task) && ("DATE_PROPOSAL_ACCEPTED".equals(task.notificationType())
                || "DATE_PROPOSAL_DECLINED".equals(task.notificationType())
                || "DATE_PROPOSAL_CANCELLED".equals(task.notificationType()));
    }

    private static boolean isDateCard(DeliveryTask task) {
        return task.referenceId() != null && task.userId() != null
                && task.notificationType().startsWith("DATE_PROPOSAL_")
                && !"DATE_PROPOSAL_DECISION_RESULT".equals(task.notificationType());
    }

    private static <T> T required(T value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private static List<TelegramInlineButton> telegramButtons(DeliveryTask task) {
        if ("DATE_PROPOSAL_ACCEPTED".equals(task.notificationType()) && task.referenceId() != null) {
            return TelegramDateCardService.reminderButtons(task.referenceId());
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
