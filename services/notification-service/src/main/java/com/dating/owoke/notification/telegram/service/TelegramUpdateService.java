package com.dating.owoke.notification.telegram.service;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;

@Service
public class TelegramUpdateService {

    private final BotCommandService commandService;
    private final TelegramBotClient botClient;

    public TelegramUpdateService(BotCommandService commandService, TelegramBotClient botClient) {
        this.commandService = commandService;
        this.botClient = botClient;
    }

    public void process(JsonNode update) {
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

    private String nullableString(JsonNode node) {
        return node.isMissingNode() || node.isNull() || node.asString().isBlank() ? null : node.asString();
    }
}
