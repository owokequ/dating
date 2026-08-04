package com.dating.owoke.notification.availability.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SiteAvailabilityWebhookRequest(
        @NotNull UUID incidentId) {
}
