package com.dating.owoke.identity.shared.messaging.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dating.owoke.identity.shared.messaging.domain.EventEnvelope;
import com.dating.owoke.identity.shared.messaging.domain.OutboxEvent;
import com.dating.owoke.identity.shared.messaging.repository.OutboxEventRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class OutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public UUID enqueue(String topic, String eventKey, String eventType, Object payload) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = clock.instant();
        EventEnvelope envelope = new EventEnvelope(
                eventId,
                eventType,
                1,
                eventKey,
                occurredAt,
                UUID.randomUUID(),
                payload);
        try {
            String json = objectMapper.writeValueAsString(envelope);
            repository.save(new OutboxEvent(eventId, topic, eventKey, eventType, json, occurredAt));
            return eventId;
        } catch (JacksonException exception) {
            throw new IllegalStateException("Cannot serialize outbox event " + eventType, exception);
        }
    }
}
