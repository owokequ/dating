package com.dating.owoke.identity.shared.security;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.dating.owoke.identity.authentication.configuration.IdentitySecurityProperties;
import com.dating.owoke.identity.telegram.configuration.TelegramOidcProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({IdentitySecurityProperties.class, TelegramOidcProperties.class})
public class IdentitySecurityConfiguration {

    private static final Logger log = LoggerFactory.getLogger(IdentitySecurityConfiguration.class);

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorWriter errorWriter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/.well-known/jwks.json").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(errorWriter::unauthorized))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errorWriter::unauthorized)
                        .accessDeniedHandler(errorWriter::forbidden))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    RSAKey owokeRsaKey(IdentitySecurityProperties properties, Environment environment) {
        boolean privateKeyConfigured = hasText(properties.privateKeyBase64());
        boolean publicKeyConfigured = hasText(properties.publicKeyBase64());
        if (privateKeyConfigured != publicKeyConfigured) {
            throw new IllegalStateException("Both OWOKE JWT public and private keys must be configured together");
        }
        if (!privateKeyConfigured && environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("Persistent OWOKE JWT signing keys are required in the prod profile");
        }

        return privateKeyConfigured ? loadKeyPair(properties) : generateLocalKeyPair(properties);
    }

    private static RSAKey loadKeyPair(IdentitySecurityProperties properties) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(
                    decodeKey(properties.privateKeyBase64())));
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(
                    decodeKey(properties.publicKeyBase64())));
            if (!privateKey.getModulus().equals(publicKey.getModulus())) {
                throw new IllegalStateException("Configured JWT public and private keys do not form a pair");
            }
            return rsaKey(properties, publicKey, privateKey);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Configured JWT RSA keys are invalid", exception);
        }
    }

    private static RSAKey generateLocalKeyPair(IdentitySecurityProperties properties) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            log.warn("JWT signing keys are not configured; generated an ephemeral local key pair");
            return rsaKey(properties, (RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("RSA must be available", exception);
        }
    }

    private static RSAKey rsaKey(
            IdentitySecurityProperties properties,
            RSAPublicKey publicKey,
            RSAPrivateKey privateKey
    ) {
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(properties.keyId())
                .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                .build();
    }

    private static byte[] decodeKey(String value) {
        String normalized = value
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey rsaKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey)));
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey rsaKey, IdentitySecurityProperties properties) throws JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(properties.issuer());
        OAuth2TokenValidator<Jwt> audience = token -> token.getAudience().contains(properties.audience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, audience));
        return decoder;
    }

    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
