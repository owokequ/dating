package com.dating.owoke.media.asset.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.dating.owoke.media.shared.exception.ResourceNotFoundException;

@Component
public class RemoteImageFetcher {

    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final KudaGoUrlPolicy urlPolicy;

    public RemoteImageFetcher(KudaGoUrlPolicy urlPolicy) {
        this.urlPolicy = urlPolicy;
    }

    public FetchedRemoteImage fetch(String remoteUrl) {
        String allowedUrl = urlPolicy.imageUrl(remoteUrl);
        HttpRequest request = HttpRequest.newBuilder(URI.create(allowedUrl))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "image/jpeg,image/png,image/webp")
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() != 200) {
                    throw unavailable();
                }
                String contentType = response.headers().firstValue("Content-Type")
                        .map(value -> value.split(";", 2)[0].trim().toLowerCase(java.util.Locale.ROOT))
                        .orElse("");
                if (!ALLOWED_TYPES.contains(contentType)) {
                    throw unavailable();
                }
                long declaredLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
                if (declaredLength > MAX_BYTES) {
                    throw unavailable();
                }
                byte[] content = body.readNBytes(MAX_BYTES + 1);
                if (content.length > MAX_BYTES) {
                    throw unavailable();
                }
                return new FetchedRemoteImage(content, contentType);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (IOException exception) {
            throw unavailable();
        }
    }

    private ResourceNotFoundException unavailable() {
        return new ResourceNotFoundException("Remote media asset is temporarily unavailable");
    }

    public record FetchedRemoteImage(byte[] content, String contentType) {
    }
}
