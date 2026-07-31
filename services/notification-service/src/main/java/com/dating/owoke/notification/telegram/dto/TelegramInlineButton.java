package com.dating.owoke.notification.telegram.dto;

import java.nio.charset.StandardCharsets;

public record TelegramInlineButton(String text, String callbackData) {

    private static final int MAX_CALLBACK_BYTES = 64;

    public TelegramInlineButton {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Telegram button text must not be blank");
        }
        if (callbackData == null || callbackData.isBlank()) {
            throw new IllegalArgumentException("Telegram callback data must not be blank");
        }
        if (callbackData.getBytes(StandardCharsets.UTF_8).length > MAX_CALLBACK_BYTES) {
            throw new IllegalArgumentException("Telegram callback data must not exceed 64 bytes");
        }
    }
}
