package com.dating.owoke.notification.media.service;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.dating.owoke.notification.shared.configuration.NotificationProperties;

@Component
public class MediaContentClient {

    private final RestClient restClient;

    public MediaContentClient(RestClient.Builder builder, NotificationProperties properties) {
        this.restClient = builder.baseUrl(properties.mediaServiceUrl()).build();
    }

    public MediaBinary getTelegramVariant(UUID mediaId) {
        try {
            ResponseEntity<byte[]> response = restClient.get()
                    .uri("/api/v1/media/assets/{mediaId}/content?variant=TELEGRAM", mediaId)
                    .retrieve()
                    .toEntity(byte[].class);
            byte[] content = response.getBody();
            String etag = response.getHeaders().getFirst(HttpHeaders.ETAG);
            if (content == null || content.length == 0 || etag == null || etag.isBlank()) {
                throw new IllegalStateException("Media Service returned incomplete Telegram image");
            }
            String contentType = response.getHeaders().getContentType() == null
                    ? "image/jpeg" : response.getHeaders().getContentType().toString();
            return new MediaBinary(content, contentType, etag.replace("\"", ""));
        } catch (RestClientException exception) {
            throw new IllegalStateException("Cannot load Telegram image from Media Service", exception);
        }
    }
}
