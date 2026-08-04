package com.dating.owoke.notification.availability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SiteAvailabilityWebhookRequest(
        @NotBlank String monitorId,
        @NotBlank String status,
        @NotNull Long occurredAt) {
}
