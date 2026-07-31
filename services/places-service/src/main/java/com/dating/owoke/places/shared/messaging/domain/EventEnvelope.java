package com.dating.owoke.places.shared.messaging.domain;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        String aggregateId,
        Instant occurredAt,
        UUID correlationId,
        Object payload) {
}
