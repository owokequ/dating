package com.dating.owoke.dating.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owoke.security")
public record DatingSecurityProperties(String issuer, String audience, String jwkSetUri) {
}
