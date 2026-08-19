package com.dating.owoke.identity.telegram.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owoke.telegram.oidc")
public record TelegramOidcProperties(
        String clientId,
        String clientSecret,
        String botUsername,
        String redirectUri,
        String mobileRedirectUri,
        String authorizationUri,
        String tokenUri,
        String jwkSetUri,
        String issuer,
        Duration stateTtl) {

    public boolean configured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }
}
