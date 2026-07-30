package com.dating.owoke.identity.shared.messaging.inbox.event;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

public record IncomingEventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        String aggregateId,
        Instant occurredAt,
        UUID correlationId,
        JsonNode payload) {
}
