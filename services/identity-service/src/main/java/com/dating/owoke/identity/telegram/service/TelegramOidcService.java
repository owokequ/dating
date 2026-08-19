package com.dating.owoke.identity.telegram.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.dating.owoke.identity.authentication.service.SecureTokenGenerator;
import com.dating.owoke.identity.telegram.configuration.TelegramOidcProperties;
import com.dating.owoke.identity.telegram.exception.TelegramOidcException;
import com.dating.owoke.identity.telegram.exception.TelegramOidcUnavailableException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class TelegramOidcService {

    private static final String STATE_PREFIX = "identity:telegram:oidc-state:";

    private final TelegramOidcProperties properties;
    private final SecureTokenGenerator tokenGenerator;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final NimbusJwtDecoder telegramJwtDecoder;
    private final TelegramIdentityService identityService;

    public TelegramOidcService(
            TelegramOidcProperties properties,
            SecureTokenGenerator tokenGenerator,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RestClient telegramRestClient,
            TelegramIdentityService identityService) {
        this.properties = properties;
        this.tokenGenerator = tokenGenerator;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.restClient = telegramRestClient;
        this.telegramJwtDecoder = createDecoder(properties);
        this.identityService = identityService;
    }

    public URI authorizationUri(String requestedContinuePath) {
        return authorizationUri(requestedContinuePath, false);
    }

    public URI mobileAuthorizationUri() {
        return authorizationUri("/dashboard", true);
    }

    private URI authorizationUri(String requestedContinuePath, boolean mobile) {
        ensureConfigured();
        String state = tokenGenerator.generate();
        String nonce = tokenGenerator.generate();
        String verifier = tokenGenerator.generate();
        TelegramOidcState storedState = new TelegramOidcState(
                verifier,
                nonce,
                sanitizeContinuePath(requestedContinuePath),
                mobile);
        try {
            redisTemplate.opsForValue().set(
                    STATE_PREFIX + tokenGenerator.hash(state),
                    objectMapper.writeValueAsString(storedState),
                    properties.stateTtl());
        } catch (JacksonException exception) {
            throw new IllegalStateException("Cannot serialize Telegram OIDC state", exception);
        }

        return UriComponentsBuilder.fromUriString(properties.authorizationUri())
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "openid profile telegram:bot_access")
                .queryParam("state", state)
                .queryParam("nonce", nonce)
                .queryParam("code_challenge", sha256Base64Url(verifier))
                .queryParam("code_challenge_method", "S256")
                .build()
                .encode()
                .toUri();
    }

    public TelegramOidcResult callback(String code, String state) {
        ensureConfigured();
        TelegramOidcState storedState = consumeState(state);
        Map<String, Object> tokenResponse = exchangeCode(code, storedState.codeVerifier());
        Object idTokenValue = tokenResponse.get("id_token");
        if (!(idTokenValue instanceof String idToken) || idToken.isBlank()) {
            throw new TelegramOidcException("Telegram did not return an ID token");
        }

        Jwt jwt;
        try {
            jwt = telegramJwtDecoder.decode(idToken);
        } catch (JwtException exception) {
            throw new TelegramOidcException("Telegram ID token validation failed", exception);
        }
        if (!storedState.nonce().equals(jwt.getClaimAsString("nonce"))) {
            throw new TelegramOidcException("Telegram OIDC nonce is invalid");
        }

        String subject = jwt.getSubject();
        long telegramUserId = longClaim(jwt.getClaim("id"));
        String displayName = defaultText(jwt.getClaimAsString("name"), "Telegram user");
        String username = jwt.getClaimAsString("preferred_username");
        boolean botAccess = String.valueOf(tokenResponse.getOrDefault("scope", ""))
                .contains("telegram:bot_access");
        return new TelegramOidcResult(
                identityService.authenticate(new TelegramProfile(
                        subject, telegramUserId, displayName, username, botAccess)),
                storedState.continuePath(),
                storedState.mobile());
    }

    private Map<String, Object> exchangeCode(String code, String verifier) {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", properties.redirectUri());
        form.add("client_id", properties.clientId());
        form.add("code_verifier", verifier);
        try {
            Map<String, Object> response = restClient.post()
                    .uri(properties.tokenUri())
                    .headers(headers -> headers.setBasicAuth(properties.clientId(), properties.clientSecret()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null) {
                throw new TelegramOidcException("Telegram token response is empty");
            }
            return response;
        } catch (RestClientException exception) {
            throw new TelegramOidcUnavailableException("Telegram authorization is temporarily unavailable");
        }
    }

    private TelegramOidcState consumeState(String state) {
        if (state == null || state.isBlank()) {
            throw new TelegramOidcException("Telegram OIDC state is missing");
        }
        String json = redisTemplate.opsForValue().getAndDelete(STATE_PREFIX + tokenGenerator.hash(state));
        if (json == null) {
            throw new TelegramOidcException("Telegram OIDC state is invalid or expired");
        }
        try {
            return objectMapper.readValue(json, TelegramOidcState.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Stored Telegram OIDC state is corrupted", exception);
        }
    }

    private void ensureConfigured() {
        if (!properties.configured()) {
            throw new TelegramOidcUnavailableException("Telegram OIDC is not configured");
        }
    }

    private static NimbusJwtDecoder createDecoder(TelegramOidcProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(properties.issuer());
        OAuth2TokenValidator<Jwt> audience = token -> token.getAudience().contains(properties.clientId())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, audience));
        return decoder;
    }

    private static String sanitizeContinuePath(String value) {
        if (value == null || value.isBlank()) {
            return "/dashboard";
        }
        if (!value.startsWith("/") || value.startsWith("//") || value.length() > 512) {
            throw new IllegalArgumentException("continue must be a relative application path");
        }
        return value;
    }

    private static long longClaim(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            return Long.parseLong(text);
        }
        throw new TelegramOidcException("Telegram user id is missing");
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String sha256Base64Url(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }
}
