package com.dating.owoke.notification.telegram.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.dating.owoke.notification.telegram.configuration.TelegramBotProperties;
import com.dating.owoke.notification.telegram.service.TelegramUpdateService;

import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1/telegram/webhook")
public class TelegramWebhookController {

    private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final TelegramBotProperties properties;
    private final TelegramUpdateService updateService;

    public TelegramWebhookController(
            TelegramBotProperties properties,
            TelegramUpdateService updateService) {
        this.properties = properties;
        this.updateService = updateService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void receive(
            @RequestHeader(value = SECRET_HEADER, required = false) String providedSecret,
            @RequestBody JsonNode update) {
        verifySecret(providedSecret);
        try {
            updateService.process(update);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private void verifySecret(String providedSecret) {
        String expected = properties.webhookSecret();
        if (!properties.isConfigured() || expected == null || expected.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Telegram webhook is disabled");
        }
        byte[] provided = providedSecret == null ? new byte[0] : providedSecret.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), provided)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Telegram webhook secret");
        }
    }
}
