package com.dating.owoke.notification.availability.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owoke.site-availability")
public record SiteAvailabilityProperties(
        boolean enabled,
        String webhookSecret) {

    public boolean isConfigured() {
        return enabled
                && hasText(webhookSecret);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
