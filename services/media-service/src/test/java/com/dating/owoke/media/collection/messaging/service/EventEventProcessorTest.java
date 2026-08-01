package com.dating.owoke.media.collection.messaging.service;

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

import com.dating.owoke.media.asset.service.RemoteMediaService;
import com.dating.owoke.media.collection.domain.EventProjectionStatus;
import com.dating.owoke.media.collection.domain.MediaOwnerType;
import com.dating.owoke.media.collection.messaging.event.EventChangedV1;
import com.dating.owoke.media.collection.service.EventProjectionService;
import com.dating.owoke.media.shared.messaging.domain.IncomingEventEnvelope;
import com.dating.owoke.media.shared.messaging.repository.InboxEventRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class EventEventProcessorTest {

    @Test
    void publishedEventCreatesProjectionAndRemoteCollection() {
        UUID envelopeId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-01T10:00:00Z");
        JsonNode payloadNode = mock(JsonNode.class);
        IncomingEventEnvelope envelope = new IncomingEventEnvelope(
                envelopeId, "EventPublishedV1", 1, eventId.toString(), occurredAt, UUID.randomUUID(), payloadNode);
        EventChangedV1 payload = new EventChangedV1(
                eventId, "KUDAGO", "42", "Concert", null, List.of("concert"), null,
                false, null, "https://kudago.com/events/concert/", null, null, "ACTIVE", List.of(),
                List.of(new EventChangedV1.Image(
                        "image-1", "https://kudago.com/media/images/concert.jpg", null,
                        "KudaGo", "https://kudago.com/events/concert/")));
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        InboxEventRepository inboxRepository = mock(InboxEventRepository.class);
        EventProjectionService projectionService = mock(EventProjectionService.class);
        RemoteMediaService remoteMediaService = mock(RemoteMediaService.class);
        when(objectMapper.readValue("message", IncomingEventEnvelope.class)).thenReturn(envelope);
        when(objectMapper.treeToValue(payloadNode, EventChangedV1.class)).thenReturn(payload);

        new EventEventProcessor(
                inboxRepository, projectionService, remoteMediaService, objectMapper,
                Clock.fixed(occurredAt, ZoneOffset.UTC))
                .process("events.events.v1", "message");

        verify(projectionService).upsert(eventId, EventProjectionStatus.ACTIVE, occurredAt);
        verify(remoteMediaService).synchronize(
                org.mockito.ArgumentMatchers.eq(MediaOwnerType.EVENT),
                org.mockito.ArgumentMatchers.eq(eventId),
                any());
        verify(inboxRepository).saveAndFlush(any());
    }
}
