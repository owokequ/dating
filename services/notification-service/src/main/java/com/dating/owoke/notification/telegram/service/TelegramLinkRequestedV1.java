package com.dating.owoke.notification.telegram.service;

public record TelegramLinkRequestedV1(
        String linkToken,
        long telegramUserId,
        long telegramChatId,
        String username) {
}
