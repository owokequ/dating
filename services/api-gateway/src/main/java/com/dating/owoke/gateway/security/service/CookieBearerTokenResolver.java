package com.dating.owoke.gateway.security.service;

import java.util.Arrays;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;

@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {

    private static final String ACCESS_COOKIE = "OWOKE_ACCESS";
    private final DefaultBearerTokenResolver headerTokenResolver = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        if (isPublicRequest(request)) {
            return null;
        }

        String headerToken = headerTokenResolver.resolve(request);
        String cookieToken = request.getCookies() == null ? null : Arrays.stream(request.getCookies())
                .filter(cookie -> ACCESS_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
        if (headerToken != null && cookieToken != null && !headerToken.equals(cookieToken)) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "invalid_request", "Cookie and Authorization tokens do not match", null));
        }

        return headerToken != null ? headerToken : cookieToken;
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/auth/")
                || path.equals("/.well-known/jwks.json")
                || path.equals("/api/v1/security/csrf")
                || path.startsWith("/actuator/")
                || path.equals("/api/v1/telegram/webhook")) {
            return true;
        }

        if (HttpMethod.GET.matches(request.getMethod())) {
            return path.startsWith("/api/v1/places/")
                    || path.equals("/api/v1/places")
                    || path.startsWith("/api/v1/media/")
                    || path.startsWith("/api/v1/couple-invitations/");
        }

        return false;
    }
}
