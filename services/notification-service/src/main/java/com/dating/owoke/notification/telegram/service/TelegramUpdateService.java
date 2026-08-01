package com.dating.owoke.notification.telegram.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;

@Service
public class TelegramUpdateService {

    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateService.class);

    private final BotCommandService commandService;
    private final TelegramBotClient botClient;

    public TelegramUpdateService(BotCommandService commandService, TelegramBotClient botClient) {
        this.commandService = commandService;
        this.botClient = botClient;
    }

    public void process(JsonNode update) {
        JsonNode callback = update.path("callback_query");
        if (!callback.isMissingNode()) {
            processCallback(update, callback);
            return;
        }
        JsonNode message = update.path("message");
        if (message.isMissingNode() || message.path("text").isMissingNode()) {
            return;
        }
        long updateId = update.path("update_id").asLong();
        long chatId = message.path("chat").path("id").asLong();
        long userId = message.path("from").path("id").asLong();
        if (updateId <= 0 || chatId == 0 || userId <= 0) {
            throw new IllegalArgumentException("Invalid Telegram update");
        }
        BotReply reply = commandService.handle(
                updateId,
                userId,
                chatId,
                nullableString(message.path("from").path("username")),
                message.path("text").asString());
        botClient.send(reply.chatId(), reply.text(), null);
    }

    private void processCallback(JsonNode update, JsonNode callback) {
        long updateId = update.path("update_id").asLong();
        String callbackQueryId = callback.path("id").asString();
        long chatId = callback.path("message").path("chat").path("id").asLong();
        long messageId = callback.path("message").path("message_id").asLong();
        long userId = callback.path("from").path("id").asLong();
        String callbackData = callback.path("data").asString();
        if (updateId <= 0 || callbackQueryId.isBlank() || chatId == 0 || messageId <= 0
                || userId <= 0 || callbackData.isBlank()) {
            throw new IllegalArgumentException("Invalid Telegram callback update");
        }
        BotReply reply = commandService.handleDateProposalDecision(
                updateId, userId, chatId, messageId, callbackData);
        acknowledgeCallback(callbackQueryId, reply.text());
    }

    private void acknowledgeCallback(String callbackQueryId, String replyText) {
        try {
            botClient.answerCallbackQuery(callbackQueryId, replyText);
        } catch (RuntimeException exception) {
            // The decision command is already committed and idempotently recorded by updateId.
            // Telegram callback acknowledgements expire quickly and must not poison the polling offset.
            log.warn("Cannot acknowledge processed Telegram callback: {}", exception.getMessage());
        }
    }

    private String nullableString(JsonNode node) {
        return node.isMissingNode() || node.isNull() || node.asString().isBlank() ? null : node.asString();
    }
}
