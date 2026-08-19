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
import com.dating.owoke.notification.push.service.ExpoPushClient;
import com.dating.owoke.notification.push.service.MobileDeviceService;
import com.dating.owoke.notification.push.configuration.ExpoPushProperties;
import com.dating.owoke.notification.push.service.PushMetrics;
import com.dating.owoke.notification.push.service.ExpoPushException;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "owoke.delivery", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeliveryDispatcher {

    private final DeliveryService deliveryService;
    private final TelegramBotClient telegramClient;
    private final TelegramMediaService telegramMediaService;
    private final TelegramCardFormatter telegramCardFormatter;
    private final TelegramDateCardService telegramDateCardService;
    private final EmailClient emailClient;
    private final ExpoPushClient expoPushClient;
    private final MobileDeviceService mobileDeviceService;
    private final ExpoPushProperties expoPushProperties;
    private final PushMetrics pushMetrics;

    public DeliveryDispatcher(
            DeliveryService deliveryService,
            TelegramBotClient telegramClient,
            TelegramMediaService telegramMediaService,
            TelegramCardFormatter telegramCardFormatter,
            TelegramDateCardService telegramDateCardService,
            EmailClient emailClient, ExpoPushClient expoPushClient, MobileDeviceService mobileDeviceService,
            ExpoPushProperties expoPushProperties, PushMetrics pushMetrics) {
        this.deliveryService = deliveryService;
        this.telegramClient = telegramClient;
        this.telegramMediaService = telegramMediaService;
        this.telegramCardFormatter = telegramCardFormatter;
        this.telegramDateCardService = telegramDateCardService;
        this.emailClient = emailClient;
        this.expoPushClient = expoPushClient; this.mobileDeviceService = mobileDeviceService;
        this.expoPushProperties = expoPushProperties;
        this.pushMetrics = pushMetrics;
    }

    @Scheduled(fixedDelayString = "${owoke.notification.delivery-fixed-delay:1000}")
    public void dispatch() {
        List<DeliveryTask> tasks = deliveryService.claim();
        for (DeliveryTask task : tasks.stream().filter(task -> task.channel() != com.dating.owoke.notification.delivery.domain.DeliveryChannel.PUSH).toList()) {
            deliver(task);
        }
        deliverPush(tasks.stream().filter(task -> task.channel() == com.dating.owoke.notification.delivery.domain.DeliveryChannel.PUSH).toList());
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
                case PUSH -> throw new IllegalStateException("Push deliveries are batched");
            };
            deliveryService.markSent(task, providerMessageId);
        } catch (Exception exception) {
            deliveryService.markFailed(task, exception);
        }
    }

    private void deliverPush(List<DeliveryTask> tasks) {
        if (tasks.isEmpty()) return;
        if (!expoPushProperties.enabled()) {
            tasks.forEach(task -> deliveryService.markFailed(task, new IllegalStateException("Expo push is disabled")));
            return;
        }
        try {
            List<ExpoPushClient.ExpoTicket> tickets = expoPushClient.send(tasks.stream().map(this::message).toList());
            if (tickets.size() != tasks.size()) throw new IllegalStateException("Expo returned an incomplete ticket batch");
            for (int i = 0; i < tasks.size(); i++) {
                DeliveryTask task = tasks.get(i); ExpoPushClient.ExpoTicket ticket = tickets.get(i);
                if ("ok".equals(ticket.status()) && ticket.id() != null) { deliveryService.markPushAccepted(task, ticket.id()); pushMetrics.sent(); }
                else if ("DeviceNotRegistered".equals(ticket.error())) { mobileDeviceService.deactivate(task.destination()); deliveryService.markPushDeviceDisabled(task, ticket.error()); pushMetrics.deviceDisabled(); }
                else { deliveryService.markFailed(task, new IllegalStateException(ticket.error() == null ? "Expo rejected push" : ticket.error())); pushMetrics.retry(); }
            }
        } catch (ExpoPushException exception) {
            tasks.forEach(task -> {
                if (exception.retryable()) { deliveryService.markFailed(task, exception); pushMetrics.retry(); }
                else { deliveryService.markPushPermanentlyFailed(task, exception.getMessage()); pushMetrics.permanentFailure(); }
            });
        } catch (Exception exception) { tasks.forEach(task -> { deliveryService.markFailed(task, exception); pushMetrics.retry(); }); }
    }

    private ExpoPushClient.ExpoMessage message(DeliveryTask task) {
        String route = allowedRoute(task.notificationType());
        Map<String, String> data = new java.util.LinkedHashMap<>(); data.put("route", route);
        if (task.referenceId() != null) data.put("referenceId", task.referenceId().toString());
        if (task.contextId() != null) data.put("contextId", task.contextId().toString());
        String channel = route.equals("date") ? "dates" : route.equals("reminder") ? "reminders" : "general";
        return new ExpoPushClient.ExpoMessage(task.destination(), "For my L", shortText(route), "default", channel, data);
    }
    private static String allowedRoute(String type) {
        if (type.startsWith("DATE_PROPOSAL_")) return "date";
        if (type.startsWith("REMINDER_")) return "reminder";
        return "notifications";
    }
    private static String shortText(String route) { return switch (route) { case "date" -> "Новое обновление свидания"; case "reminder" -> "Напоминание"; default -> "Новое уведомление"; }; }

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
