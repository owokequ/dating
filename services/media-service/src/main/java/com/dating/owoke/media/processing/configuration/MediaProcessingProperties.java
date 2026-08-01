package com.dating.owoke.media.processing.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owoke.media.processing")
public record MediaProcessingProperties(long maximumSourceBytes, long maximumPixels, Duration purgeDelay) {
}
