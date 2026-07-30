package com.dating.owoke.identity.authentication.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owoke.security")
public record IdentitySecurityProperties(
        String issuer,
        String audience,
        String keyId,
        String privateKeyBase64,
        String publicKeyBase64,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        boolean cookieSecure,
        String webAppUrl) {
}
