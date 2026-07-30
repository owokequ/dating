package com.dating.owoke.gateway.security.service;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtForwardingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authorization = authentication instanceof JwtAuthenticationToken jwtAuthentication
                ? "Bearer " + jwtAuthentication.getToken().getTokenValue()
                : null;
        filterChain.doFilter(new AuthorizationRequestWrapper(request, authorization), response);
    }

    private static final class AuthorizationRequestWrapper extends HttpServletRequestWrapper {

        private final String authorization;

        private AuthorizationRequestWrapper(HttpServletRequest request, String authorization) {
            super(request);
            this.authorization = authorization;
        }

        @Override
        public String getHeader(String name) {
            if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                return authorization;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                return authorization == null
                        ? Collections.emptyEnumeration()
                        : Collections.enumeration(Collections.singleton(authorization));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            LinkedHashSet<String> names = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
            names.removeIf(HttpHeaders.AUTHORIZATION::equalsIgnoreCase);
            if (authorization != null) {
                names.add(HttpHeaders.AUTHORIZATION);
            }
            return Collections.enumeration(names);
        }
    }
}
