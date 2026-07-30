package com.dating.owoke.gateway.security.service;

import java.util.Arrays;
import java.util.Set;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {

    private static final String ACCESS_COOKIE = "OWOKE_ACCESS";
    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/auth/email-verifications/confirm",
            "/api/v1/auth/password-reset/request",
            "/api/v1/auth/password-reset/confirm",
            "/api/v1/auth/telegram/authorize",
            "/api/v1/auth/telegram/callback"
    );

    @Override
    public String resolve(HttpServletRequest request) {
        if (isPublicRequest(request) || request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> ACCESS_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (PUBLIC_AUTH_PATHS.contains(path)
                || path.equals("/.well-known/jwks.json")
                || path.equals("/api/v1/security/csrf")
                || path.startsWith("/actuator/")
                || path.startsWith("/api/v1/webhooks/telegram/")) {
            return true;
        }

        if (HttpMethod.GET.matches(request.getMethod())) {
            return path.startsWith("/api/v1/places/")
                    || path.equals("/api/v1/places")
                    || path.startsWith("/api/v1/couple-invitations/");
        }

        return false;
    }
}
