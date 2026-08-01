package com.dating.owoke.places.sync.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owoke.kudago")
public record KudaGoProperties(
        boolean enabled,
        String baseUrl,
        String location,
        int pageSize,
        int foodLimit,
        int leisureLimit) {

    public boolean isConfigured() {
        return enabled && baseUrl != null && !baseUrl.isBlank() && location != null && !location.isBlank();
    }

    public int safePageSize() {
        int requiredForSelection = Math.max(foodLimit, leisureLimit);
        return Math.max(1, Math.min(Math.max(pageSize, requiredForSelection), 100));
    }
}
