package com.dating.owoke.notification.telegram.service;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.dating.owoke.notification.telegram.configuration.TelegramBotProperties;
import com.dating.owoke.notification.telegram.dto.TelegramInlineButton;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class TelegramBotClient {

    private static final int ERROR_DETAIL_LIMIT = 300;
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "0.0.0.0", "::1", "[::1]");

    private final RestClient restClient;
    private final TelegramBotProperties properties;
    private final ObjectMapper objectMapper;

    public TelegramBotClient(
            RestClient.Builder builder,
            TelegramBotProperties properties,
            ObjectMapper objectMapper) {
        this.restClient = builder.baseUrl("https://api.telegram.org").build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String send(long chatId, String text, String actionUrl) {
        return send(chatId, text, actionUrl, List.of());
    }

    public String send(
            long chatId,
            String text,
            String actionUrl,
            List<TelegramInlineButton> callbackButtons) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("Telegram bot is disabled or not configured");
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("chat_id", chatId);
        request.put("text", text);
        List<List<Map<String, String>>> keyboard = keyboard(actionUrl, callbackButtons);
        if (!keyboard.isEmpty()) {
            request.put("reply_markup", Map.of("inline_keyboard", keyboard));
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

    public TelegramPhotoResult sendPhoto(
            long chatId,
            String caption,
            String actionUrl,
            List<TelegramInlineButton> callbackButtons,
            TelegramPhoto photo) {
        requireConfigured();
        MultiValueMap<String, Object> request = new LinkedMultiValueMap<>();
        request.add("chat_id", Long.toString(chatId));
        request.add("caption", caption);
        request.add("parse_mode", "HTML");
        List<List<Map<String, String>>> keyboard = keyboard(actionUrl, callbackButtons);
        if (!keyboard.isEmpty()) {
            request.add("reply_markup", toJson(Map.of("inline_keyboard", keyboard)));
        }
        if (photo.cached()) {
            request.add("photo", photo.cachedFileId());
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(photo.contentType()));
            ByteArrayResource resource = new ByteArrayResource(photo.content()) {
                @Override
                public String getFilename() {
                    return photo.fileName();
                }
            };
            request.add("photo", new HttpEntity<>(resource, headers));
        }
        JsonNode response = telegramCall("sendPhoto", () -> restClient.post()
                .uri("/bot{token}/sendPhoto", properties.botToken())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(request)
                .retrieve()
                .requiredBody(JsonNode.class));
        JsonNode result = response.path("result");
        JsonNode photos = result.path("photo");
        JsonNode largest = photos.isArray() && !photos.isEmpty() ? photos.get(photos.size() - 1) : null;
        if (largest == null || largest.path("file_id").asString().isBlank()) {
            throw new IllegalStateException("Telegram sendPhoto response has no reusable file_id");
        }
        return new TelegramPhotoResult(
                result.path("message_id").asString(),
                largest.path("file_id").asString(),
                largest.path("file_unique_id").asString());
    }

    public void editPhotoCaption(long chatId, long messageId, String caption, String actionUrl) {
        requireConfigured();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("chat_id", chatId);
        request.put("message_id", messageId);
        request.put("caption", caption);
        request.put("parse_mode", "HTML");
        List<List<Map<String, String>>> keyboard = keyboard(actionUrl, List.of());
        request.put("reply_markup", Map.of("inline_keyboard", keyboard));
        telegramCall("editMessageCaption", () -> restClient.post()
                .uri("/bot{token}/editMessageCaption", properties.botToken())
                .body(request)
                .retrieve()
                .requiredBody(JsonNode.class));
    }

    public void answerCallbackQuery(String callbackQueryId, String text) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("Telegram bot is disabled or not configured");
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("callback_query_id", callbackQueryId);
        request.put("text", text);
        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/bot{token}/answerCallbackQuery", properties.botToken())
                    .body(request)
                    .retrieve()
                    .requiredBody(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw telegramResponseException("answerCallbackQuery", exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Telegram answerCallbackQuery request failed");
        }
        if (!response.path("ok").asBoolean(false)) {
            throw new IllegalStateException("Telegram rejected answerCallbackQuery request");
        }
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

    private List<List<Map<String, String>>> keyboard(
            String actionUrl,
            List<TelegramInlineButton> callbackButtons) {
        List<List<Map<String, String>>> keyboard = new java.util.ArrayList<>();
        if (callbackButtons != null && !callbackButtons.isEmpty()) {
            keyboard.add(callbackButtons.stream()
                    .map(button -> Map.of("text", button.text(), "callback_data", button.callbackData()))
                    .toList());
        }
        if (isPublicHttpUrl(actionUrl)) {
            keyboard.add(List.of(Map.of("text", "Открыть Owoke 💗", "url", actionUrl)));
        }
        return keyboard;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (tools.jackson.core.JacksonException exception) {
            throw new IllegalStateException("Cannot serialize Telegram keyboard", exception);
        }
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("Telegram bot is disabled or not configured");
        }
    }

    private JsonNode telegramCall(String operation, java.util.function.Supplier<JsonNode> call) {
        JsonNode response;
        try {
            response = call.get();
        } catch (RestClientResponseException exception) {
            throw telegramResponseException(operation, exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Telegram " + operation + " request failed", exception);
        }
        if (!response.path("ok").asBoolean(false)) {
            throw new IllegalStateException("Telegram rejected " + operation + " request");
        }
        return response;
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
