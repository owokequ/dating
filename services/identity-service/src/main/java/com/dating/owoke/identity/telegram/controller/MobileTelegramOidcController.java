package com.dating.owoke.identity.telegram.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dating.owoke.identity.authentication.dto.MobileSessionResponse;
import com.dating.owoke.identity.telegram.dto.TelegramMobileExchangeRequest;
import com.dating.owoke.identity.telegram.service.TelegramMobileSessionCodeService;
import com.dating.owoke.identity.telegram.service.TelegramOidcService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth/mobile/telegram")
public class MobileTelegramOidcController {

    private final TelegramOidcService oidcService;
    private final TelegramMobileSessionCodeService mobileSessionCodeService;

    public MobileTelegramOidcController(
            TelegramOidcService oidcService,
            TelegramMobileSessionCodeService mobileSessionCodeService) {
        this.oidcService = oidcService;
        this.mobileSessionCodeService = mobileSessionCodeService;
    }

    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize() {
        URI authorizationUri = oidcService.mobileAuthorizationUri();
        return ResponseEntity.status(302).location(authorizationUri).build();
    }

    @PostMapping("/exchange")
    public MobileSessionResponse exchange(@Valid @RequestBody TelegramMobileExchangeRequest request) {
        return MobileSessionResponse.from(mobileSessionCodeService.consume(request.code()));
    }
}
