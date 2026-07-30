package com.dating.owoke.gateway.security.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owoke.gateway.security")
public record GatewaySecurityProperties(
        String issuer,
        String audience,
        String jwkSetUri,
        String webAppOrigin,
        boolean cookieSecure
) {
}
