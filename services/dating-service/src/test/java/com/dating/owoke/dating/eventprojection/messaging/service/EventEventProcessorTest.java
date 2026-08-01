package com.dating.owoke.dating.eventprojection.messaging.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.dating.owoke.dating.eventprojection.messaging.domain.EventChangedV1;
import com.dating.owoke.dating.eventprojection.service.EventProjectionService;
import com.dating.owoke.dating.shared.messaging.domain.IncomingEventEnvelope;
import com.dating.owoke.dating.shared.messaging.repository.InboxEventRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class EventEventProcessorTest {

    @Test
    void publishedEventIsStoredAndRecordedInInbox() {
        UUID messageId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-01T10:00:00Z");
        JsonNode payloadNode = mock(JsonNode.class);
        IncomingEventEnvelope envelope = new IncomingEventEnvelope(
                messageId, "EventPublishedV1", 1, eventId.toString(), occurredAt,
                UUID.randomUUID(), payloadNode);
        EventChangedV1 payload = new EventChangedV1(
                eventId, "KUDAGO", "42", "Спектакль", "Описание", List.of("theater"),
                "от 1000 ₽", false, "12+", "https://kudago.com/kzn/event/test/", null,
                new EventChangedV1.Venue("Театр", "Казань", 55.79, 49.10), "ACTIVE",
                List.of(), List.of());
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        InboxEventRepository inboxRepository = mock(InboxEventRepository.class);
        EventProjectionService projectionService = mock(EventProjectionService.class);
        when(objectMapper.readValue("message", IncomingEventEnvelope.class)).thenReturn(envelope);
        when(objectMapper.treeToValue(payloadNode, EventChangedV1.class)).thenReturn(payload);

        new EventEventProcessor(
                inboxRepository,
                projectionService,
                objectMapper,
                Clock.fixed(occurredAt, ZoneOffset.UTC))
                .process("events.events.v1", "message");

        verify(projectionService).upsert(payload, occurredAt);
        verify(inboxRepository).saveAndFlush(any());
    }
}
