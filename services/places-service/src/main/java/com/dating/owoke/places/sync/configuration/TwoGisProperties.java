package com.dating.owoke.places.sync.configuration;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owoke.two-gis")
public record TwoGisProperties(
        boolean enabled,
        String apiKey,
        String baseUrl,
        String location,
        int radiusMeters,
        int pageSize,
        int requestsPerSecond,
        List<String> queries) {

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
