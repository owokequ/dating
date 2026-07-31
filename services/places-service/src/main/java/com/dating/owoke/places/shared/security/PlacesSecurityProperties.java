package com.dating.owoke.places.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owoke.security")
public record PlacesSecurityProperties(String issuer, String audience, String jwkSetUri) {
}
