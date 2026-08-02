package com.dating.owoke.places.sync.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.dating.owoke.places.place.domain.PlaceSource;
import com.dating.owoke.places.place.service.ExternalPlaceData;
import com.dating.owoke.places.place.service.ExternalPlaceImageData;
import com.dating.owoke.places.sync.configuration.KudaGoCollection;
import com.dating.owoke.places.sync.configuration.KudaGoProperties;
import com.dating.owoke.places.sync.exception.SyncUnavailableException;

import tools.jackson.databind.JsonNode;

@Component
public class KudaGoClient {

    private static final Pattern COORDINATE_ADDRESS = Pattern.compile(
            "^[+-]?\\d{1,3}(?:[.]\\d+)?\\s*[,;]\\s*[+-]?\\d{1,3}(?:[.]\\d+)?$");
    private static final String KAZAN_ADDRESS = "Казань";
    private static final String FIELDS = String.join(",",
            "id", "title", "address", "coords", "categories", "description", "images", "site_url",
            "is_closed", "favorites_count");

    private final RestClient restClient;
    private final KudaGoProperties properties;

    public KudaGoClient(RestClient.Builder builder, KudaGoProperties properties) {
        this.restClient = builder.baseUrl(URI.create(properties.baseUrl())).build();
        this.properties = properties;
    }

    public List<ExternalPlaceData> search(KudaGoCollection collection) {
        if (!properties.isConfigured()) {
            throw new SyncUnavailableException("KudaGo synchronization is not configured");
        }
        JsonNode response;
        try {
            response = restClient.get()
                    .uri(uri -> uri.path("/public-api/v1.4/places/")
                            .queryParam("location", properties.location())
                            .queryParam("categories", collection.queryValue())
                            .queryParam("fields", FIELDS)
                            .queryParam("text_format", "text")
                            .queryParam("order_by", "-favorites_count,id")
                            .queryParam("page_size", properties.safePageSize())
                            .queryParam("page", 1)
                            .build())
                    .retrieve()
                    .requiredBody(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw new SyncUnavailableException("KudaGo returned HTTP " + exception.getStatusCode());
        } catch (RestClientException exception) {
            throw new SyncUnavailableException("KudaGo request failed");
        }

        List<Candidate> candidates = new ArrayList<>();
        for (JsonNode item : response.path("results")) {
            Candidate candidate = map(item, collection);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }
        return candidates.stream()
                .sorted(Comparator.comparingInt(Candidate::favoritesCount).reversed()
                        .thenComparingLong(Candidate::providerId))
                .limit(Math.max(0, collection.limit(properties)))
                .map(Candidate::place)
                .toList();
    }

    private Candidate map(JsonNode item, KudaGoCollection collection) {
        if (item.path("is_closed").asBoolean(false)) {
            return null;
        }
        long id = item.path("id").asLong(-1);
        String name = item.path("title").asString();
        String address = normalizeAddress(item.path("address").asString());
        JsonNode coords = item.path("coords");
        String sourcePageUrl = normalizeHttps(item.path("site_url").asString());
        if (id < 0 || name.isBlank() || address.isBlank() || sourcePageUrl == null
                || !coords.path("lat").isNumber() || !coords.path("lon").isNumber()) {
            return null;
        }

        List<String> categories = new ArrayList<>();
        item.path("categories").forEach(category -> categories.add(category.asString()));
        String category = mapCategory(collection, categories);
        if (category == null) {
            return null;
        }
        List<ExternalPlaceImageData> images = mapImages(item.path("images"), sourcePageUrl);
        ExternalPlaceData place = new ExternalPlaceData(
                PlaceSource.KUDAGO,
                Long.toString(id),
                name,
                truncate(item.path("description").asString(null), 2000),
                category,
                address,
                coords.path("lat").asDouble(),
                coords.path("lon").asDouble(),
                sourcePageUrl,
                images);
        return new Candidate(id, item.path("favorites_count").asInt(0), place);
    }

    private String mapCategory(KudaGoCollection collection, List<String> categories) {
        if (collection == KudaGoCollection.LEISURE
                && categories.stream().anyMatch(collection.categories()::contains)) {
            return "ENTERTAINMENT";
        }
        if (categories.contains("anticafe")) {
            return "CAFE";
        }
        if (categories.contains("restaurants")) {
            return "RESTAURANT";
        }
        return null;
    }

    private List<ExternalPlaceImageData> mapImages(JsonNode imagesNode, String sourcePageUrl) {
        List<ExternalPlaceImageData> images = new ArrayList<>();
        for (JsonNode image : imagesNode) {
            if (images.size() == 5) {
                break;
            }
            String remoteUrl = normalizeHttps(image.path("image").asString());
            if (remoteUrl == null || !isKudaGoUrl(remoteUrl)) {
                continue;
            }
            images.add(new ExternalPlaceImageData(
                    sha256(remoteUrl), remoteUrl, "KudaGo", sourcePageUrl));
        }
        return List.copyOf(images);
    }

    private static String normalizeHttps(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.startsWith("http://")) {
            normalized = "https://" + normalized.substring("http://".length());
        }
        return normalized.startsWith("https://") ? normalized : null;
    }

    private static String normalizeAddress(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.strip();
        return COORDINATE_ADDRESS.matcher(normalized).matches() ? KAZAN_ADDRESS : normalized;
    }

    private static boolean isKudaGoUrl(String value) {
        try {
            String host = URI.create(value).getHost();
            return host != null && (host.equals("kudago.com") || host.endsWith(".kudago.com"));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record Candidate(long providerId, int favoritesCount, ExternalPlaceData place) {
    }
}
