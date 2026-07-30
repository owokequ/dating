package com.dating.owoke.gateway.security.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owoke.gateway.rate-limit")
public record RateLimitProperties(
        Duration window,
        int loginAttempts,
        int registrationAttempts,
        int passwordResetAttempts,
        int refreshAttempts
) {
}
