package com.dating.owoke.notification.telegram.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owoke.telegram")
public record TelegramBotProperties(boolean enabled, String mode, String botToken, String webhookSecret) {

    public boolean isConfigured() {
        return enabled && botToken != null && !botToken.isBlank();
    }
}
