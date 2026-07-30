package com.dating.owoke.notification.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owoke.security")
public record NotificationSecurityProperties(String issuer, String audience, String jwkSetUri) {
}
