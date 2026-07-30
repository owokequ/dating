package com.dating.owoke.identity.authentication.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import com.dating.owoke.identity.authentication.configuration.IdentitySecurityProperties;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final IdentitySecurityProperties properties;
    private final Clock clock;

    public JwtService(JwtEncoder encoder, IdentitySecurityProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public AccessToken issue(AuthenticatedAccount account) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(properties.keyId())
                .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .audience(List.of(properties.audience()))
                .subject(account.userId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("name", account.displayName())
                .claim("roles", List.of(account.role().name()))
                .build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, expiresAt);
    }

    public record AccessToken(String value, Instant expiresAt) {
    }
}
