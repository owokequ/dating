package com.dating.owoke.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import com.dating.owoke.gateway.security.service.CookieBearerTokenResolver;

class CookieBearerTokenResolverTest {

    private final CookieBearerTokenResolver resolver = new CookieBearerTokenResolver();

    @Test
    void resolvesStandardAuthorizationBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.addHeader("Authorization", "Bearer mobile-access-token");

        assertThat(resolver.resolve(request)).isEqualTo("mobile-access-token");
    }

    @Test
    void continuesToResolveBrowserAccessCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.setCookies(new Cookie("OWOKE_ACCESS", "browser-access-token"));

        assertThat(resolver.resolve(request)).isEqualTo("browser-access-token");
    }

    @Test
    void rejectsDifferentCookieAndAuthorizationTokens() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.setCookies(new Cookie("OWOKE_ACCESS", "browser-access-token"));
        request.addHeader("Authorization", "Bearer mobile-access-token");

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }
}
