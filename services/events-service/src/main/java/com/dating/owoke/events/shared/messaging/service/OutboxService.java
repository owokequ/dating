package com.dating.owoke.events.shared.messaging.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dating.owoke.events.shared.messaging.domain.EventEnvelope;
import com.dating.owoke.events.shared.messaging.domain.OutboxEvent;
import com.dating.owoke.events.shared.messaging.repository.OutboxEventRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class OutboxService {
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    public OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository; this.objectMapper = objectMapper; this.clock = clock;
    }
    public void enqueue(String topic, UUID aggregateId, String eventType, Object payload) {
        UUID eventId = UUID.randomUUID(); Instant now = clock.instant();
        EventEnvelope envelope = new EventEnvelope(eventId, eventType, 1, aggregateId.toString(), now,
                UUID.randomUUID(), payload);
        try {
            repository.save(new OutboxEvent(eventId, topic, aggregateId.toString(), eventType,
                    objectMapper.writeValueAsString(envelope), now));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Cannot serialize " + eventType, exception);
        }
    }
}
