package com.dating.owoke.media.storage.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owoke.media.storage")
public record MediaStorageProperties(
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey,
        boolean pathStyleAccess) {
}
