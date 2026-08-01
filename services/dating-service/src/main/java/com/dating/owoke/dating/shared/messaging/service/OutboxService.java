package com.dating.owoke.dating.shared.messaging.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dating.owoke.dating.shared.messaging.domain.EventEnvelope;
import com.dating.owoke.dating.shared.messaging.domain.OutboxEvent;
import com.dating.owoke.dating.shared.messaging.repository.OutboxEventRepository;

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
        return enqueue(topic, eventKey, eventType, 1, payload);
    }

    public UUID enqueue(String topic, String eventKey, String eventType, int eventVersion, Object payload) {
        UUID eventId = UUID.randomUUID();
        Instant now = clock.instant();
        EventEnvelope envelope = new EventEnvelope(
                eventId, eventType, eventVersion, eventKey, now, UUID.randomUUID(), payload);
        try {
            repository.save(new OutboxEvent(
                    eventId, topic, eventKey, eventType, objectMapper.writeValueAsString(envelope), now));
            return eventId;
        } catch (JacksonException exception) {
            throw new IllegalStateException("Cannot serialize outbox event " + eventType, exception);
        }
    }
}
