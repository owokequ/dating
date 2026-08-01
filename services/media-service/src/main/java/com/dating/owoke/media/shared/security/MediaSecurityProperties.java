package com.dating.owoke.media.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owoke.security")
public record MediaSecurityProperties(String issuer, String audience, String jwkSetUri) {
}
