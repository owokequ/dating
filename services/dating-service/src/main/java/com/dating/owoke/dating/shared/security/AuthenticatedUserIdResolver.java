package com.dating.owoke.dating.shared.security;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserIdResolver {

    public UUID resolve(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new IllegalArgumentException("JWT subject is missing");
        }
        return UUID.fromString(jwt.getSubject());
    }
}
