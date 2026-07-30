package com.dating.owoke.identity.authentication.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

@RestController
public class JwkSetController {

    private final RSAKey rsaKey;

    public JwkSetController(RSAKey rsaKey) {
        this.rsaKey = rsaKey;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> keys() {
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }
}
