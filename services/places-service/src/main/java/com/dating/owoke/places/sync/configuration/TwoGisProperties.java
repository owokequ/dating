package com.dating.owoke.places.sync.configuration;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owoke.two-gis")
public record TwoGisProperties(
        boolean enabled,
        boolean scheduleEnabled,
        String apiKey,
        String baseUrl,
        String point,
        int radiusMeters,
        int pageSize,
        int maxPages,
        int requestsPerSecond,
        List<TwoGisQuery> queries) {

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
