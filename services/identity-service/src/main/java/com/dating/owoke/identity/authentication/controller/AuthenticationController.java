package com.dating.owoke.identity.authentication.controller;

import java.time.Clock;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dating.owoke.identity.authentication.dto.AccountTokenRequest;
import com.dating.owoke.identity.authentication.dto.LoginRequest;
import com.dating.owoke.identity.authentication.dto.PasswordResetConfirmRequest;
import com.dating.owoke.identity.authentication.dto.PasswordResetRequest;
import com.dating.owoke.identity.authentication.dto.RegisterRequest;
import com.dating.owoke.identity.authentication.service.AuthenticationCookieService;
import com.dating.owoke.identity.authentication.service.AuthenticationService;
import com.dating.owoke.identity.authentication.service.IssuedSession;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final AuthenticationCookieService cookieService;
    private final Clock clock;

    public AuthenticationController(
            AuthenticationService authenticationService,
            AuthenticationCookieService cookieService,
            Clock clock) {
        this.authenticationService = authenticationService;
        this.cookieService = cookieService;
        this.clock = clock;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authenticationService.register(request.email(), request.displayName(), request.password());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/email-verifications/confirm")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody AccountTokenRequest request) {
        authenticationService.verifyEmail(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
        IssuedSession session = authenticationService.login(request.email(), request.password());
        return withCookies(HttpStatus.NO_CONTENT, cookieService.sessionCookies(session, clock.instant()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(name = AuthenticationCookieService.REFRESH_COOKIE, required = false) String refreshToken) {
        IssuedSession session = authenticationService.refresh(refreshToken == null ? "" : refreshToken);
        return withCookies(HttpStatus.NO_CONTENT, cookieService.sessionCookies(session, clock.instant()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = AuthenticationCookieService.REFRESH_COOKIE, required = false) String refreshToken) {
        authenticationService.logout(refreshToken);
        return withCookies(HttpStatus.NO_CONTENT, cookieService.clearCookies());
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authenticationService.requestPasswordReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authenticationService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<Void> withCookies(HttpStatus status, List<ResponseCookie> cookies) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
        cookies.forEach(cookie -> response.header(HttpHeaders.SET_COOKIE, cookie.toString()));
        return response.build();
    }
}
