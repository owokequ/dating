package com.dating.owoke.notification.shared.messaging.domain;

import java.time.Instant;
import java.util.UUID;

public record OutgoingEventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        String aggregateId,
        Instant occurredAt,
        UUID correlationId,
        Object payload) {
}
