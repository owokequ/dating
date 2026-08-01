package com.dating.owoke.events.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owoke.security")
public record EventsSecurityProperties(String issuer, String audience, String jwkSetUri) {
}
