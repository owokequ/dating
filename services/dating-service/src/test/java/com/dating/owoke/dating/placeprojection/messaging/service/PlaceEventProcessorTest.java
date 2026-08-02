package com.dating.owoke.dating.placeprojection.messaging.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.dating.owoke.dating.placeprojection.domain.PlaceProjectionStatus;
import com.dating.owoke.dating.placeprojection.messaging.domain.PlaceChangedV1;
import com.dating.owoke.dating.placeprojection.messaging.domain.PlaceChangedV2;
import com.dating.owoke.dating.placeprojection.service.PlaceProjectionService;
import com.dating.owoke.dating.shared.messaging.domain.IncomingEventEnvelope;
import com.dating.owoke.dating.shared.messaging.repository.InboxEventRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class PlaceEventProcessorTest {

    @Test
    void publishedV2EventIsStoredAsActiveProjection() {
        UUID eventId = UUID.randomUUID();
        UUID placeId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-01T10:00:00Z");
        JsonNode payloadNode = mock(JsonNode.class);
        IncomingEventEnvelope envelope = new IncomingEventEnvelope(
                eventId, "PlacePublishedV2", 2, placeId.toString(), occurredAt, UUID.randomUUID(), payloadNode);
        PlaceChangedV2 payload = new PlaceChangedV2(
                placeId, "KZN", "KudaGo place", "Kazan", "ENTERTAINMENT",
                55.79, 49.10, null, "ACTIVE", "KUDAGO", "42",
                "https://kzn.kudago.com/place/test/",
                List.of(new PlaceChangedV2.Image(
                        "image-1", "https://media.kudago.com/images/place/test.jpg", "KudaGo", null)));
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        InboxEventRepository inboxRepository = mock(InboxEventRepository.class);
        PlaceProjectionService projectionService = mock(PlaceProjectionService.class);
        when(objectMapper.readValue("message", IncomingEventEnvelope.class)).thenReturn(envelope);
        when(objectMapper.treeToValue(payloadNode, PlaceChangedV2.class)).thenReturn(payload);

        new PlaceEventProcessor(
                inboxRepository,
                projectionService,
                objectMapper,
                Clock.fixed(occurredAt, ZoneOffset.UTC))
                .process("places.events.v1", "message");

        verify(projectionService).upsert(
                placeId, "KudaGo place", "Kazan", PlaceProjectionStatus.ACTIVE, occurredAt);
        verify(inboxRepository).saveAndFlush(any());
    }

    @Test
    void draftedEventIsStoredAsDraftProjection() {
        UUID eventId = UUID.randomUUID();
        UUID placeId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-01T10:00:00Z");
        JsonNode payloadNode = mock(JsonNode.class);
        IncomingEventEnvelope envelope = new IncomingEventEnvelope(
                eventId, "PlaceDraftedV1", 1, placeId.toString(), occurredAt, UUID.randomUUID(), payloadNode);
        PlaceChangedV1 payload = new PlaceChangedV1(
                placeId, "KZN", "Draft", "Kazan", "CAFE",
                55.79, 49.10, null, "DRAFT");
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        InboxEventRepository inboxRepository = mock(InboxEventRepository.class);
        PlaceProjectionService projectionService = mock(PlaceProjectionService.class);
        when(objectMapper.readValue("message", IncomingEventEnvelope.class)).thenReturn(envelope);
        when(objectMapper.treeToValue(payloadNode, PlaceChangedV1.class)).thenReturn(payload);

        new PlaceEventProcessor(
                inboxRepository,
                projectionService,
                objectMapper,
                Clock.fixed(occurredAt, ZoneOffset.UTC))
                .process("places.events.v1", "message");

        verify(projectionService).upsert(
                placeId, "Draft", "Kazan", PlaceProjectionStatus.DRAFT, occurredAt);
        verify(inboxRepository).saveAndFlush(any());
    }

    @Test
    void rejectsReplayWithLossyQuestionMarkReplacement() {
        UUID eventId = UUID.randomUUID();
        UUID placeId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-01T10:00:00Z");
        JsonNode payloadNode = mock(JsonNode.class);
        IncomingEventEnvelope envelope = new IncomingEventEnvelope(
                eventId, "PlacePublishedV2", 2, placeId.toString(), occurredAt, UUID.randomUUID(), payloadNode);
        PlaceChangedV2 payload = new PlaceChangedV2(
                placeId, "KZN", "??????? ?????", "Казань", "ENTERTAINMENT",
                55.79, 49.10, null, "ACTIVE", "KUDAGO", "42",
                "https://kzn.kudago.com/place/test/", List.of());
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        InboxEventRepository inboxRepository = mock(InboxEventRepository.class);
        PlaceProjectionService projectionService = mock(PlaceProjectionService.class);
        when(objectMapper.readValue("message", IncomingEventEnvelope.class)).thenReturn(envelope);
        when(objectMapper.treeToValue(payloadNode, PlaceChangedV2.class)).thenReturn(payload);
        PlaceEventProcessor processor = new PlaceEventProcessor(
                inboxRepository, projectionService, objectMapper, Clock.fixed(occurredAt, ZoneOffset.UTC));

        assertThatThrownBy(() -> processor.process("places.events.v1", "message"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lossy character replacement");
    }
}
