package com.dating.owoke.notification.push.service;

import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import com.dating.owoke.notification.push.configuration.ExpoPushProperties;

@Component
public class ExpoPushClient {
    private final RestClient client; private final ExpoPushProperties properties;
    public ExpoPushClient(RestClient.Builder builder, ExpoPushProperties properties) {
        this.properties = properties; this.client = builder.baseUrl(properties.baseUrl()).build();
    }
    public List<ExpoTicket> send(List<ExpoMessage> messages) {
        if (messages.size() > 100) throw new IllegalArgumentException("Expo batches are limited to 100 messages");
        try {
            Map<?, ?> response = client.post().uri("/--/api/v2/push/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> { if (properties.accessToken() != null && !properties.accessToken().isBlank()) h.setBearerAuth(properties.accessToken()); })
                    .body(messages).retrieve().body(Map.class);
            return tickets(response);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            throw new ExpoPushException("Expo push request failed: " + status, status == 429 || status >= 500, exception);
        } catch (RestClientException exception) { throw new ExpoPushException("Expo push is temporarily unavailable", true, exception); }
    }
    public Map<String, ExpoReceipt> receipts(List<String> ids) {
        try {
            Map<?, ?> response = client.post().uri("/--/api/v2/push/getReceipts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> { if (properties.accessToken() != null && !properties.accessToken().isBlank()) h.setBearerAuth(properties.accessToken()); })
                    .body(Map.of("ids", ids)).retrieve().body(Map.class);
            Object data = response == null ? null : response.get("data");
            if (!(data instanceof Map<?, ?> values)) return Map.of();
            return values.entrySet().stream().collect(java.util.stream.Collectors.toMap(
                    entry -> String.valueOf(entry.getKey()), entry -> receipt((Map<?, ?>) entry.getValue())));
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            throw new ExpoPushException("Expo receipt lookup failed: " + status, status == 429 || status >= 500, exception);
        } catch (RestClientException exception) { throw new ExpoPushException("Expo receipt lookup is temporarily unavailable", true, exception); }
    }
    private static List<ExpoTicket> tickets(Map<?, ?> response) {
        Object data = response == null ? null : response.get("data");
        if (!(data instanceof List<?> list)) throw new ExpoPushException("Expo returned malformed tickets", true, null);
        return list.stream().map(value -> ticket((Map<?, ?>) value)).toList();
    }
    private static ExpoTicket ticket(Map<?, ?> value) { return new ExpoTicket(String.valueOf(value.get("status")), string(value.get("id")), error(value)); }
    private static ExpoReceipt receipt(Map<?, ?> value) { return new ExpoReceipt(String.valueOf(value.get("status")), error(value)); }
    private static String error(Map<?, ?> value) { Object details = value.get("details"); return details instanceof Map<?, ?> map && map.get("error") != null ? String.valueOf(map.get("error")) : string(value.get("message")); }
    private static String string(Object value) { return value == null ? null : String.valueOf(value); }
    public record ExpoMessage(String to, String title, String body, String sound, String channelId, Map<String, String> data) { }
    public record ExpoTicket(String status, String id, String error) { }
    public record ExpoReceipt(String status, String error) { }
}
