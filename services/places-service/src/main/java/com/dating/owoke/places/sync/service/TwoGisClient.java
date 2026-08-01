package com.dating.owoke.places.sync.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.dating.owoke.places.place.service.ExternalPlaceData;
import com.dating.owoke.places.sync.configuration.TwoGisProperties;
import com.dating.owoke.places.sync.configuration.TwoGisQuery;
import com.dating.owoke.places.sync.exception.SyncUnavailableException;

import tools.jackson.databind.JsonNode;

@Component
public class TwoGisClient {

    private final RestClient restClient;
    private final TwoGisProperties properties;
    private final TwoGisRateLimiter rateLimiter;

    public TwoGisClient(
            RestClient.Builder builder,
            TwoGisProperties properties,
            TwoGisRateLimiter rateLimiter) {
        this.restClient = builder.baseUrl(java.net.URI.create(properties.baseUrl())).build();
        this.properties = properties;
        this.rateLimiter = rateLimiter;
    }

    public List<ExternalPlaceData> search(TwoGisQuery query, int page) {
        if (!properties.isConfigured()) {
            throw new SyncUnavailableException("2GIS synchronization is not configured");
        }
        rateLimiter.acquire();
        JsonNode response;
        try {
            response = restClient.get()
                    .uri(uri -> uri.path("/3.0/items")
                            .queryParam("key", properties.apiKey())
                            .queryParam("q", query.query())
                            .queryParam("point", properties.point())
                            .queryParam("radius", properties.radiusMeters())
                            .queryParam("type", "branch")
                            .queryParam("page_size", properties.pageSize())
                            .queryParam("page", page)
                            .queryParam("fields", "items.point")
                            .build())
                    .retrieve()
                    .requiredBody(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw new SyncUnavailableException("2GIS returned HTTP " + exception.getStatusCode());
        } catch (RestClientException exception) {
            throw new SyncUnavailableException("2GIS request failed");
        }
        if (response.path("meta").path("code").asInt() != 200) {
            throw new SyncUnavailableException("2GIS returned an unsuccessful response");
        }

        List<ExternalPlaceData> places = new ArrayList<>();
        for (JsonNode item : response.path("result").path("items")) {
            ExternalPlaceData place = map(item, query.category());
            if (place != null) {
                places.add(place);
            }
        }
        return places;
    }

    private ExternalPlaceData map(JsonNode item, String category) {
        String externalId = item.path("id").asString();
        String name = item.path("name").asString();
        String address = item.path("address_name").asString();
        JsonNode point = item.path("point");
        if (externalId.isBlank() || name.isBlank() || address.isBlank()
                || !point.path("lat").isNumber() || !point.path("lon").isNumber()) {
            return null;
        }
        return ExternalPlaceData.twoGis(
                externalId, name, category, address,
                point.path("lat").asDouble(), point.path("lon").asDouble());
    }
}
