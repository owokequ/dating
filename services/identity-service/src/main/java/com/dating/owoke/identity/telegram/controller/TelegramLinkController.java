package com.dating.owoke.identity.telegram.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dating.owoke.identity.telegram.dto.TelegramLinkResponse;
import com.dating.owoke.identity.telegram.service.TelegramLinkService;

@RestController
@RequestMapping("/api/v1/users/me/telegram-link")
public class TelegramLinkController {

    private final TelegramLinkService telegramLinkService;

    public TelegramLinkController(TelegramLinkService telegramLinkService) {
        this.telegramLinkService = telegramLinkService;
    }

    @PostMapping
    public TelegramLinkResponse create(@AuthenticationPrincipal Jwt jwt) {
        return telegramLinkService.create(UUID.fromString(jwt.getSubject()));
    }
}
