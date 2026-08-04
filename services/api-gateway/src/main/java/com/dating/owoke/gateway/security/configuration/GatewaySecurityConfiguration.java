package com.dating.owoke.gateway.security.configuration;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.dating.owoke.gateway.security.service.AuthRateLimitFilter;
import com.dating.owoke.gateway.security.service.CookieBearerTokenResolver;
import com.dating.owoke.gateway.security.service.CsrfCookieFilter;
import com.dating.owoke.gateway.security.service.GatewaySecurityErrorWriter;
import com.dating.owoke.gateway.security.service.JwtForwardingFilter;

@Configuration
@EnableConfigurationProperties({GatewaySecurityProperties.class, RateLimitProperties.class})
public class GatewaySecurityConfiguration {

    @Bean
    SecurityFilterChain gatewaySecurityFilterChain(
            HttpSecurity http,
            CookieBearerTokenResolver tokenResolver,
            JwtForwardingFilter jwtForwardingFilter,
            CsrfCookieFilter csrfCookieFilter,
            AuthRateLimitFilter authRateLimitFilter,
            GatewaySecurityErrorWriter errorWriter,
            GatewaySecurityProperties properties
    ) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");
        csrfRepository.setHeaderName("X-XSRF-TOKEN");
        csrfRepository.setCookieCustomizer(cookie -> cookie
                .secure(properties.cookieSecure())
                .sameSite("Lax"));

        return http
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                "/api/v1/telegram/webhook",
                                "/api/v1/site-availability/**"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/.well-known/jwks.json", "/api/v1/security/csrf").permitAll()
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/telegram/webhook",
                                "/api/v1/site-availability/**",
                                "/api/v1/system/availability")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/places", "/api/v1/places/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events", "/api/v1/events/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/media/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/couple-invitations/*").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                        .bearerTokenResolver(tokenResolver)
                        .authenticationEntryPoint((request, response, exception) ->
                                errorWriter.write(response, 401, "Unauthorized", "Authentication is required")))
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler((request, response, exception) ->
                                errorWriter.write(response, 403, "Forbidden", "Access is denied")))
                .addFilterAfter(csrfCookieFilter, CsrfFilter.class)
                .addFilterAfter(authRateLimitFilter, CsrfFilter.class)
                .addFilterAfter(jwtForwardingFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    JwtDecoder gatewayJwtDecoder(GatewaySecurityProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(properties.issuer());
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>(
                "aud", audiences -> audiences != null && audiences.contains(properties.audience()));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, audience));
        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(GatewaySecurityProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(properties.webAppOrigin()));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "Idempotency-Key"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
