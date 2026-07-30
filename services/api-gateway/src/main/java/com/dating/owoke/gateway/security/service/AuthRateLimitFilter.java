package com.dating.owoke.gateway.security.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.dating.owoke.gateway.security.configuration.RateLimitProperties;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final AuthRateLimitService rateLimitService;
    private final RateLimitProperties properties;

    public AuthRateLimitFilter(AuthRateLimitService rateLimitService, RateLimitProperties properties) {
        this.rateLimitService = rateLimitService;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Limit limit = resolveLimit(request);
        if (limit == null || rateLimitService.isAllowed(
                limit.operation(), request.getRemoteAddr(), limit.attempts(), properties.window())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {"type":"about:blank","title":"Too Many Requests","status":429,"detail":"Try again later"}
                """);
    }

    private Limit resolveLimit(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return null;
        }

        return switch (request.getRequestURI()) {
            case "/api/v1/auth/login" -> new Limit("login", properties.loginAttempts());
            case "/api/v1/auth/register" -> new Limit("register", properties.registrationAttempts());
            case "/api/v1/auth/password-reset/request" ->
                    new Limit("password-reset", properties.passwordResetAttempts());
            case "/api/v1/auth/refresh" -> new Limit("refresh", properties.refreshAttempts());
            default -> null;
        };
    }

    private record Limit(String operation, int attempts) {
    }
}
