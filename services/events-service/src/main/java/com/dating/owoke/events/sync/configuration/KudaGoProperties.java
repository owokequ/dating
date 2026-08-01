package com.dating.owoke.events.sync.configuration;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owoke.kudago")
public record KudaGoProperties(
        boolean enabled,
        boolean scheduleEnabled,
        String baseUrl,
        String location,
        Duration horizon,
        Duration syncDelay,
        int pageSize,
        List<String> categories) {
}
