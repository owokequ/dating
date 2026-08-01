package com.dating.owoke.events.sync.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.dating.owoke.events.sync.configuration.KudaGoProperties;
import com.dating.owoke.events.sync.dto.ExternalEventData;
import com.dating.owoke.events.sync.dto.ExternalImageData;
import com.dating.owoke.events.sync.dto.ExternalOccurrenceData;
import com.dating.owoke.events.sync.dto.KudaGoPage;
import com.dating.owoke.events.sync.exception.SyncUnavailableException;

import tools.jackson.databind.JsonNode;

@Component
public class KudaGoEventClient {
    private final RestClient restClient;
    private final KudaGoProperties properties;

    public KudaGoEventClient(RestClient.Builder builder, KudaGoProperties properties) {
        this.restClient = builder.baseUrl(java.net.URI.create(properties.baseUrl())).build();
        this.properties = properties;
    }

    public KudaGoPage page(int page, Instant from, Instant until) {
        if (!properties.enabled()) throw new SyncUnavailableException("KudaGo event synchronization is disabled");
        JsonNode response;
        try {
            response = restClient.get().uri(builder -> builder
                    .path("/public-api/v1.4/events/")
                    .queryParam("location", properties.location())
                    .queryParam("actual_since", from.getEpochSecond())
                    .queryParam("actual_until", until.getEpochSecond())
                    .queryParam("categories", String.join(",", properties.categories()))
                    .queryParam("page", page)
                    .queryParam("page_size", Math.min(Math.max(properties.pageSize(), 1), 100))
                    .queryParam("order_by", "-rank,-id")
                    .queryParam("text_format", "text")
                    .queryParam("fields", "id,title,description,categories,price,is_free,age_restriction,site_url,dates,place,images")
                    .queryParam("expand", "dates,place,images")
                    .build()).retrieve().requiredBody(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw new SyncUnavailableException("KudaGo returned HTTP " + exception.getStatusCode());
        } catch (RestClientException exception) {
            throw new SyncUnavailableException("KudaGo request failed");
        }
        List<ExternalEventData> events = new ArrayList<>();
        int received = 0; int skipped = 0;
        for (JsonNode item : response.path("results")) {
            received++;
            ExternalEventData mapped = map(item, from);
            if (mapped == null) skipped++; else events.add(mapped);
        }
        return new KudaGoPage(events, !response.path("next").isNull() && !response.path("next").asString().isBlank(),
                received, skipped);
    }

    private ExternalEventData map(JsonNode item, Instant now) {
        String id = item.path("id").asString();
        String title = item.path("title").asString();
        String siteUrl = normalizeKudaGoUrl(item.path("site_url").asString());
        if (id.isBlank() || title.isBlank() || siteUrl == null) return null;
        List<ExternalOccurrenceData> occurrences = occurrences(item.path("dates"), id, now);
        if (occurrences.isEmpty()) return null;
        JsonNode place = item.path("place");
        JsonNode coords = place.path("coords");
        Double latitude = coords.path("lat").isNumber() ? coords.path("lat").asDouble() : null;
        Double longitude = coords.path("lon").isNumber() ? coords.path("lon").asDouble() : null;
        return new ExternalEventData(id, title, item.path("description").asString(null), strings(item.path("categories")),
                item.path("price").asString(null), item.path("is_free").asBoolean(false),
                item.path("age_restriction").asString(null), siteUrl, place.path("id").asString(null),
                place.path("title").asString(null), place.path("address").asString(null), latitude, longitude,
                occurrences, images(item.path("images")));
    }

    private List<ExternalOccurrenceData> occurrences(JsonNode dates, String eventId, Instant now) {
        List<ExternalOccurrenceData> result = new ArrayList<>();
        for (JsonNode date : dates) {
            if (!date.path("start").canConvertToLong()) continue;
            Instant start = Instant.ofEpochSecond(date.path("start").asLong());
            Instant end = date.path("end").canConvertToLong() ? Instant.ofEpochSecond(date.path("end").asLong()) : null;
            Instant boundary = end == null ? start : end;
            if (!boundary.isAfter(now)) continue;
            boolean continuous = date.path("is_continuous").asBoolean(false)
                    || date.path("is_endless").asBoolean(false) || date.path("is_startless").asBoolean(false);
            String key = start.getEpochSecond() + ":" + (end == null ? "" : end.getEpochSecond()) + ":" + continuous;
            result.add(new ExternalOccurrenceData(key, start, end, continuous));
        }
        return result.stream().distinct().limit(200).toList();
    }

    private List<ExternalImageData> images(JsonNode images) {
        List<ExternalImageData> result = new ArrayList<>();
        for (JsonNode image : images) {
            String remote = normalizeKudaGoMediaUrl(image.path("image").asString());
            if (remote == null) continue;
            JsonNode thumbnails = image.path("thumbnails");
            String thumbnail = normalizeKudaGoMediaUrl(thumbnails.path("640x384").asString());
            JsonNode source = image.path("source");
            String key = UUID.nameUUIDFromBytes(remote.getBytes(StandardCharsets.UTF_8)).toString();
            result.add(new ExternalImageData(key, remote, thumbnail, source.path("name").asString(null),
                    normalizeAnyHttpUrl(source.path("link").asString(null))));
            if (result.size() == 5) break;
        }
        return result;
    }

    private List<String> strings(JsonNode node) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (JsonNode item : node) if (!item.asString().isBlank()) result.add(item.asString());
        return List.copyOf(result);
    }

    static String normalizeKudaGoUrl(String value) {
        return normalize(value, false);
    }
    static String normalizeKudaGoMediaUrl(String value) {
        return normalize(value, true);
    }
    private static String normalize(String value, boolean mediaOnly) {
        if (value == null || value.isBlank()) return null;
        try {
            java.net.URI uri = java.net.URI.create(value.replaceFirst("^http://", "https://"));
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !"kudago.com".equalsIgnoreCase(uri.getHost())) return null;
            if (mediaOnly && (uri.getPath() == null || !uri.getPath().startsWith("/media/"))) return null;
            return uri.toString();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
    private static String normalizeAnyHttpUrl(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            java.net.URI uri = java.net.URI.create(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    ? uri.toString() : null;
        } catch (IllegalArgumentException exception) { return null; }
    }
}
