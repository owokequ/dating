package com.dating.owoke.notification.telegram.service;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.notification.contact.domain.ContactProjection;
import com.dating.owoke.notification.contact.repository.ContactProjectionRepository;
import com.dating.owoke.notification.preference.domain.NotificationPreference;
import com.dating.owoke.notification.preference.repository.NotificationPreferenceRepository;
import com.dating.owoke.notification.shared.messaging.service.OutboxService;
import com.dating.owoke.notification.shared.configuration.NotificationProperties;
import com.dating.owoke.notification.telegram.domain.TelegramUpdate;
import com.dating.owoke.notification.telegram.domain.DateProposalCallback;
import com.dating.owoke.notification.telegram.domain.ReminderCallback;
import com.dating.owoke.notification.reminder.service.ReminderService;
import com.dating.owoke.notification.telegram.messaging.event.DateProposalDecisionRequestedV1;
import com.dating.owoke.notification.telegram.repository.TelegramUpdateRepository;

@Service
public class BotCommandService {

    private static final String IDENTITY_COMMANDS_TOPIC = "identity.commands.v1";
    private static final String DATING_COMMANDS_TOPIC = "dating.commands.v1";

    private final OutboxService outboxService;
    private final ContactProjectionRepository contactRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final TelegramUpdateRepository updateRepository;
    private final TelegramDecisionService decisionService;
    private final ReminderService reminderService;
    private final StringRedisTemplate redisTemplate;
    private final TelegramDateCardService dateCardService;
    private final NotificationProperties notificationProperties;
    private final Clock clock;

    public BotCommandService(
            OutboxService outboxService,
            ContactProjectionRepository contactRepository,
            NotificationPreferenceRepository preferenceRepository,
            TelegramUpdateRepository updateRepository,
            TelegramDecisionService decisionService,
            ReminderService reminderService,
            StringRedisTemplate redisTemplate,
            TelegramDateCardService dateCardService,
            NotificationProperties notificationProperties,
            Clock clock) {
        this.outboxService = outboxService;
        this.contactRepository = contactRepository;
        this.preferenceRepository = preferenceRepository;
        this.updateRepository = updateRepository;
        this.decisionService = decisionService;
        this.reminderService = reminderService;
        this.redisTemplate = redisTemplate;
        this.dateCardService = dateCardService;
        this.notificationProperties = notificationProperties;
        this.clock = clock;
    }

    @Transactional
    public BotReply handle(long updateId, long telegramUserId, long chatId, String username, String text) {
        TelegramUpdate processed = updateRepository.findById(updateId).orElse(null);
        if (processed != null) {
            return new BotReply(processed.getChatId(), processed.getReplyText());
        }
        String command = text == null ? "" : text.trim();
        BotReply reply;
        UUID pendingProposal = pendingReminder(telegramUserId);
        if (pendingProposal != null) {
            reply = handleReminderMinutes(telegramUserId, chatId, pendingProposal, command);
        } else if (command.startsWith("/start link_")) {
            String linkToken = command.substring("/start link_".length()).trim();
            if (linkToken.isEmpty()) {
                reply = new BotReply(chatId, "Ссылка привязки повреждена. Создайте новую на сайте For my L.");
            } else {
                outboxService.enqueue(
                        IDENTITY_COMMANDS_TOPIC,
                        Long.toString(telegramUserId),
                        "TelegramLinkRequestedV1",
                        new TelegramLinkRequestedV1(linkToken, telegramUserId, chatId, username));
                reply = new BotReply(chatId, "Запрос на привязку принят. Подтверждение скоро придёт сюда.");
            }
        } else {
            reply = switch (firstToken(command)) {
                case "/start", "/help" -> new BotReply(chatId,
                        "For my L присылает уведомления о свиданиях. Привяжите бота в настройках сайта.");
                case "/settings" -> new BotReply(chatId,
                        "Настройки каналов доступны на сайте For my L. /stop отключит Telegram-уведомления.");
                case "/stop" -> disableTelegram(telegramUserId, chatId);
                default -> new BotReply(chatId, "Неизвестная команда. Используйте /help.");
            };
        }
        updateRepository.save(new TelegramUpdate(updateId, reply.chatId(), reply.text(), clock.instant()));
        return reply;
    }

    @Transactional
    public BotReply handleDateProposalDecision(
            long updateId,
            long telegramUserId,
            long chatId,
            long messageId,
            String callbackData) {
        TelegramUpdate processed = updateRepository.findById(updateId).orElse(null);
        if (processed != null) {
            return new BotReply(processed.getChatId(), processed.getReplyText());
        }

        ContactProjection contact = contactRepository.findByTelegramUserId(telegramUserId).orElse(null);
        BotReply reply;
        if (contact == null || contact.getTelegramChatId() == null
                || contact.getTelegramChatId().longValue() != chatId) {
            reply = new BotReply(chatId, "Сначала привяжите этот Telegram-аккаунт к For my L.");
        } else if (ReminderCallback.isReminder(callbackData)) {
            reply = handleReminderCallback(contact, telegramUserId, chatId, ReminderCallback.decode(callbackData));
        } else {
            DateProposalCallback decision = DateProposalCallback.decode(callbackData);
            java.util.UUID requestId = outboxService.enqueue(
                    DATING_COMMANDS_TOPIC,
                    decision.coupleId().toString(),
                    "DateProposalDecisionRequestedV1",
                    new DateProposalDecisionRequestedV1(
                            decision.proposalId(), decision.coupleId(), contact.getUserId(), decision.decision()));
            decisionService.remember(
                    requestId, updateId, decision.proposalId(), contact.getUserId(), chatId, messageId,
                    notificationProperties.webAppUrl() + "/dates/" + decision.proposalId());
            String actionText = "ACCEPT".equals(decision.decision()) ? "принятие" : "отклонение";
            reply = new BotReply(chatId, "Запрос на " + actionText + " отправлен.");
        }
        updateRepository.save(new TelegramUpdate(updateId, reply.chatId(), reply.text(), clock.instant()));
        return reply;
    }

    private BotReply handleReminderCallback(ContactProjection contact, long telegramUserId, long chatId, ReminderCallback callback) {
        try {
            return switch (callback.action()) {
                case "3h" -> confirmReminder(chatId, callback.proposalId(), contact.getUserId(), 180);
                case "1h" -> confirmReminder(chatId, callback.proposalId(), contact.getUserId(), 60);
                case "30m" -> confirmReminder(chatId, callback.proposalId(), contact.getUserId(), 30);
                case "15m" -> confirmReminder(chatId, callback.proposalId(), contact.getUserId(), 15);
                case "45m" -> confirmReminder(chatId, callback.proposalId(), contact.getUserId(), 45);
                case "90m" -> confirmReminder(chatId, callback.proposalId(), contact.getUserId(), 90);
                case "pick" -> {
                    dateCardService.showReminderOptions(callback.proposalId(), contact.getUserId(), true);
                    yield new BotReply(chatId, "Выберите свой интервал на карточке свидания.");
                }
                case "text" -> {
                    redisTemplate.opsForValue().set(reminderKey(telegramUserId), callback.proposalId().toString(), Duration.ofMinutes(10));
                    yield new BotReply(chatId, "Отправьте одним сообщением число минут: от 5 до 10080.");
                }
                case "edit" -> {
                    dateCardService.showReminderOptions(callback.proposalId(), contact.getUserId(), false);
                    yield new BotReply(chatId, "Выберите новый интервал.");
                }
                case "off" -> {
                    String result = reminderService.disable(callback.proposalId(), contact.getUserId());
                    dateCardService.setReminder(callback.proposalId(), contact.getUserId(), "отключено", false);
                    yield new BotReply(chatId, result);
                }
                default -> throw new IllegalArgumentException("Unsupported reminder action");
            };
        } catch (IllegalArgumentException exception) {
            return new BotReply(chatId, exception.getMessage());
        }
    }

    private BotReply handleReminderMinutes(long telegramUserId, long chatId, UUID proposalId, String value) {
        ContactProjection contact = contactRepository.findByTelegramUserId(telegramUserId).orElse(null);
        if (contact == null || contact.getTelegramChatId() == null || contact.getTelegramChatId().longValue() != chatId) {
            return new BotReply(chatId, "Сначала привяжите этот Telegram-аккаунт к For my L.");
        }
        try {
            int minutes = Integer.parseInt(value);
            BotReply reply = confirmReminder(chatId, proposalId, contact.getUserId(), minutes);
            redisTemplate.delete(reminderKey(telegramUserId));
            return reply;
        } catch (NumberFormatException exception) {
            return new BotReply(chatId, "Нужно целое число минут: от 5 до 10080.");
        } catch (IllegalArgumentException exception) {
            return new BotReply(chatId, exception.getMessage());
        }
    }

    private BotReply confirmReminder(long chatId, UUID proposalId, UUID userId, int minutes) {
        String text = reminderService.configure(proposalId, userId, minutes);
        dateCardService.setReminder(proposalId, userId, readableInterval(text), true);
        return new BotReply(chatId, text);
    }

    private UUID pendingReminder(long telegramUserId) {
        String value = redisTemplate.opsForValue().get(reminderKey(telegramUserId));
        try { return value == null ? null : UUID.fromString(value); }
        catch (IllegalArgumentException exception) { redisTemplate.delete(reminderKey(telegramUserId)); return null; }
    }

    private static String reminderKey(long telegramUserId) { return "notification:reminder-input:" + telegramUserId; }

    private static String readableInterval(String message) {
        return message.replaceFirst("^Напомню за ", "").replaceFirst("\\.$", "");
    }

    private BotReply disableTelegram(long telegramUserId, long chatId) {
        ContactProjection contact = contactRepository.findByTelegramUserId(telegramUserId).orElse(null);
        if (contact == null) {
            return new BotReply(chatId, "Telegram ещё не привязан к For my L.");
        }
        NotificationPreference preference = preferenceRepository.findById(contact.getUserId())
                .orElseThrow(() -> new IllegalStateException("Notification preferences are missing"));
        preference.disableTelegram(clock.instant());
        return new BotReply(chatId, "Telegram-уведомления отключены. Их можно включить на сайте.");
    }

    private String firstToken(String command) {
        int separator = command.indexOf(' ');
        String token = separator < 0 ? command : command.substring(0, separator);
        int botSuffix = token.indexOf('@');
        return botSuffix < 0 ? token : token.substring(0, botSuffix);
    }

}
