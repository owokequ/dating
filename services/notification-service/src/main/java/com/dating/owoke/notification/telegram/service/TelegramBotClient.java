package com.dating.owoke.notification.telegram.service;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.dating.owoke.notification.telegram.configuration.TelegramBotProperties;

import tools.jackson.databind.JsonNode;

@Component
public class TelegramBotClient {

    private static final int ERROR_DETAIL_LIMIT = 300;
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "0.0.0.0", "::1", "[::1]");

    private final RestClient restClient;
    private final TelegramBotProperties properties;

    public TelegramBotClient(RestClient.Builder builder, TelegramBotProperties properties) {
        this.restClient = builder.baseUrl("https://api.telegram.org").build();
        this.properties = properties;
    }

    public String send(long chatId, String text, String actionUrl) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("Telegram bot is disabled or not configured");
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("chat_id", chatId);
        request.put("text", text);
        if (isPublicHttpUrl(actionUrl)) {
            request.put("reply_markup", Map.of(
                    "inline_keyboard", List.of(List.of(Map.of("text", "Открыть Owoke", "url", actionUrl)))));
        }
        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/bot{token}/sendMessage", properties.botToken())
                    .body(request)
                    .retrieve()
                    .requiredBody(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw telegramResponseException("sendMessage", exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Telegram sendMessage request failed");
        }
        if (!response.path("ok").asBoolean(false)) {
            throw new IllegalStateException("Telegram rejected sendMessage request");
        }
        return response.path("result").path("message_id").asString();
    }

    public JsonNode getUpdates(long offset) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("Telegram bot is disabled or not configured");
        }
        JsonNode response;
        try {
            response = restClient.get()
                    .uri(uri -> uri.path("/bot{token}/getUpdates")
                            .queryParam("offset", offset)
                            .queryParam("timeout", 30)
                            .build(properties.botToken()))
                    .retrieve()
                    .requiredBody(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw telegramResponseException("getUpdates", exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Telegram getUpdates request failed");
        }
        if (!response.path("ok").asBoolean(false)) {
            throw new IllegalStateException("Telegram rejected getUpdates request");
        }
        return response.path("result");
    }

    private static boolean isPublicHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && host != null
                    && !LOCAL_HOSTS.contains(host.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static IllegalStateException telegramResponseException(
            String operation,
            RestClientResponseException exception) {
        String detail = exception.getResponseBodyAsString().replaceAll("\\s+", " ").trim();
        if (detail.length() > ERROR_DETAIL_LIMIT) {
            detail = detail.substring(0, ERROR_DETAIL_LIMIT) + "…";
        }
        String suffix = detail.isEmpty() ? "" : ": " + detail;
        return new IllegalStateException(
                "Telegram " + operation + " returned " + exception.getStatusCode() + suffix,
                exception);
    }
}
