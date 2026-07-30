package com.dating.owoke.identity.telegram.service;

public record TelegramLinkRequestedV1(
        String linkToken,
        long telegramUserId,
        long telegramChatId,
        String username) {
}
