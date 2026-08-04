package com.dating.owoke.notification.availability.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dating.owoke.notification.availability.dto.SiteAvailabilityWebhookRequest;
import com.dating.owoke.notification.availability.service.SiteAvailabilityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/site-availability/recoveries")
@ConditionalOnProperty(prefix = "owoke.site-availability", name = "enabled", havingValue = "true")
public class SiteAvailabilityWebhookController {

    private final SiteAvailabilityService service;

    public SiteAvailabilityWebhookController(SiteAvailabilityService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(
            @RequestHeader(name = "X-Site-Availability-Secret", required = false) String secret,
            @Valid @RequestBody SiteAvailabilityWebhookRequest request) {
        service.accept(secret, request);
    }
}
