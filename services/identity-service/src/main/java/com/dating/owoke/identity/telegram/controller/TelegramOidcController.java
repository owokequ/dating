package com.dating.owoke.identity.telegram.controller;

import java.net.URI;
import java.time.Clock;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.dating.owoke.identity.authentication.configuration.IdentitySecurityProperties;
import com.dating.owoke.identity.authentication.service.AuthenticationCookieService;
import com.dating.owoke.identity.telegram.service.TelegramOidcResult;
import com.dating.owoke.identity.telegram.service.TelegramOidcService;
import com.dating.owoke.identity.telegram.service.TelegramMobileSessionCodeService;
import com.dating.owoke.identity.telegram.configuration.TelegramOidcProperties;

@RestController
@RequestMapping("/api/v1/auth/telegram")
public class TelegramOidcController {

    private final TelegramOidcService oidcService;
    private final AuthenticationCookieService cookieService;
    private final IdentitySecurityProperties securityProperties;
    private final Clock clock;
    private final TelegramMobileSessionCodeService mobileSessionCodeService;
    private final TelegramOidcProperties telegramProperties;

    public TelegramOidcController(
            TelegramOidcService oidcService,
            AuthenticationCookieService cookieService,
            IdentitySecurityProperties securityProperties,
            TelegramMobileSessionCodeService mobileSessionCodeService,
            TelegramOidcProperties telegramProperties,
            Clock clock) {
        this.oidcService = oidcService;
        this.cookieService = cookieService;
        this.securityProperties = securityProperties;
        this.mobileSessionCodeService = mobileSessionCodeService;
        this.telegramProperties = telegramProperties;
        this.clock = clock;
    }

    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(
            @RequestParam(name = "continue", required = false) String continuePath) {
        return ResponseEntity.status(302)
                .location(oidcService.authorizationUri(continuePath))
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam String code, @RequestParam String state) {
        TelegramOidcResult result = oidcService.callback(code, state);
        if (result.mobile()) {
            String exchangeCode = mobileSessionCodeService.create(result.session());
            URI redirect = UriComponentsBuilder.fromUriString(telegramProperties.mobileRedirectUri())
                    .queryParam("code", exchangeCode)
                    .build()
                    .encode()
                    .toUri();
            return ResponseEntity.status(302).location(redirect).build();
        }
        ResponseEntity.BodyBuilder response = ResponseEntity.status(302)
                .location(URI.create(securityProperties.webAppUrl() + result.continuePath()));
        cookieService.sessionCookies(result.session(), clock.instant())
                .forEach(cookie -> response.header(HttpHeaders.SET_COOKIE, cookie.toString()));
        return response.build();
    }
}
