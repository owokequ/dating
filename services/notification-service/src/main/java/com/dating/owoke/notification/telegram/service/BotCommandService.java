package com.dating.owoke.notification.telegram.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.notification.contact.domain.ContactProjection;
import com.dating.owoke.notification.contact.repository.ContactProjectionRepository;
import com.dating.owoke.notification.preference.domain.NotificationPreference;
import com.dating.owoke.notification.preference.repository.NotificationPreferenceRepository;
import com.dating.owoke.notification.shared.messaging.service.OutboxService;
import com.dating.owoke.notification.telegram.domain.TelegramUpdate;
import com.dating.owoke.notification.telegram.repository.TelegramUpdateRepository;

@Service
public class BotCommandService {

    private static final String IDENTITY_COMMANDS_TOPIC = "identity.commands.v1";

    private final OutboxService outboxService;
    private final ContactProjectionRepository contactRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final TelegramUpdateRepository updateRepository;
    private final Clock clock;

    public BotCommandService(
            OutboxService outboxService,
            ContactProjectionRepository contactRepository,
            NotificationPreferenceRepository preferenceRepository,
            TelegramUpdateRepository updateRepository,
            Clock clock) {
        this.outboxService = outboxService;
        this.contactRepository = contactRepository;
        this.preferenceRepository = preferenceRepository;
        this.updateRepository = updateRepository;
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
        if (command.startsWith("/start link_")) {
            String linkToken = command.substring("/start link_".length()).trim();
            if (linkToken.isEmpty()) {
                reply = new BotReply(chatId, "Ссылка привязки повреждена. Создайте новую на сайте Owoke.");
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
                        "Owoke присылает уведомления о свиданиях. Привяжите бота в настройках обычного сайта.");
                case "/settings" -> new BotReply(chatId,
                        "Настройки каналов доступны на сайте Owoke. /stop отключит Telegram-уведомления.");
                case "/stop" -> disableTelegram(telegramUserId, chatId);
                default -> new BotReply(chatId, "Неизвестная команда. Используйте /help.");
            };
        }
        updateRepository.save(new TelegramUpdate(updateId, reply.chatId(), reply.text(), clock.instant()));
        return reply;
    }

    private BotReply disableTelegram(long telegramUserId, long chatId) {
        ContactProjection contact = contactRepository.findByTelegramUserId(telegramUserId).orElse(null);
        if (contact == null) {
            return new BotReply(chatId, "Telegram ещё не привязан к Owoke.");
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
