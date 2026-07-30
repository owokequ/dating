package com.dating.owoke.identity.telegram.service;

public record TelegramProfile(
        String subject,
        long telegramUserId,
        String displayName,
        String username,
        boolean botAccess) {
}
