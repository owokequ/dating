package com.dating.owoke.notification.availability.dto;

import com.dating.owoke.notification.availability.domain.MonitorStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SiteAvailabilityWebhookRequest(
        @NotBlank String monitorId,
        @NotNull MonitorStatus status,
        @NotNull Long occurredAt) {
}
