package com.dating.owoke.identity.account.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dating.owoke.identity.account.dto.AccountProfileResponse;
import com.dating.owoke.identity.account.dto.UpdateProfileRequest;
import com.dating.owoke.identity.account.service.AccountService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users/me")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public AccountProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        return AccountProfileResponse.from(accountService.get(UUID.fromString(jwt.getSubject())));
    }

    @PatchMapping
    public AccountProfileResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        return AccountProfileResponse.from(accountService.updateDisplayName(
                UUID.fromString(jwt.getSubject()), request.displayName()));
    }
}
