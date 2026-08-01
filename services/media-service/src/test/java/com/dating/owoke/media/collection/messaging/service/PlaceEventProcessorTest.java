package com.dating.owoke.media.collection.messaging.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.dating.owoke.media.collection.domain.PlaceProjectionStatus;
import com.dating.owoke.media.collection.messaging.event.PlaceChangedV1;
import com.dating.owoke.media.collection.messaging.event.PlaceChangedV2;
import com.dating.owoke.media.collection.messaging.event.ExternalImageV2;
import com.dating.owoke.media.collection.domain.MediaOwnerType;
import java.util.List;
import com.dating.owoke.media.collection.service.PlaceProjectionService;
import com.dating.owoke.media.asset.service.RemoteMediaService;
import com.dating.owoke.media.shared.messaging.domain.IncomingEventEnvelope;
import com.dating.owoke.media.shared.messaging.repository.InboxEventRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class PlaceEventProcessorTest {

    @Test
    void draftedEventCreatesDraftMediaProjection() {
        UUID eventId = UUID.randomUUID();
        UUID placeId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-01T10:00:00Z");
        JsonNode payloadNode = mock(JsonNode.class);
        IncomingEventEnvelope envelope = new IncomingEventEnvelope(
                eventId, "PlaceDraftedV1", 1, placeId.toString(), occurredAt, UUID.randomUUID(), payloadNode);
        PlaceChangedV1 payload = new PlaceChangedV1(
                placeId, "KZN", "Draft", "Kazan", "CAFE",
                BigDecimal.valueOf(55.79), BigDecimal.valueOf(49.10), null, "DRAFT");
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        InboxEventRepository inboxRepository = mock(InboxEventRepository.class);
        PlaceProjectionService projectionService = mock(PlaceProjectionService.class);
        RemoteMediaService remoteMediaService = mock(RemoteMediaService.class);
        when(objectMapper.readValue("message", IncomingEventEnvelope.class)).thenReturn(envelope);
        when(objectMapper.treeToValue(payloadNode, PlaceChangedV1.class)).thenReturn(payload);

        new PlaceEventProcessor(
                inboxRepository,
                projectionService,
                remoteMediaService,
                objectMapper,
                Clock.fixed(occurredAt, ZoneOffset.UTC))
                .process("places.events.v1", "message");

        verify(projectionService).upsert(placeId, PlaceProjectionStatus.DRAFT, occurredAt);
        verify(inboxRepository).saveAndFlush(any());
    }

    @Test
    void kudagoV2EventSynchronizesRemoteImages() {
        UUID eventId = UUID.randomUUID();
        UUID placeId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-01T10:00:00Z");
        JsonNode payloadNode = mock(JsonNode.class);
        IncomingEventEnvelope envelope = new IncomingEventEnvelope(
                eventId, "PlaceDraftedV2", 2, placeId.toString(), occurredAt, UUID.randomUUID(), payloadNode);
        PlaceChangedV2 payload = new PlaceChangedV2(
                placeId, "KZN", "Draft", "Kazan", "CAFE",
                BigDecimal.valueOf(55.79), BigDecimal.valueOf(49.10), null, "DRAFT",
                "KUDAGO", "77", "https://kudago.com/msk/place/draft/",
                List.of(new ExternalImageV2(
                        "image-1", "https://kudago.com/media/images/place.jpg",
                        "KudaGo", "https://kudago.com/msk/place/draft/")));
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        InboxEventRepository inboxRepository = mock(InboxEventRepository.class);
        PlaceProjectionService projectionService = mock(PlaceProjectionService.class);
        RemoteMediaService remoteMediaService = mock(RemoteMediaService.class);
        when(objectMapper.readValue("message", IncomingEventEnvelope.class)).thenReturn(envelope);
        when(objectMapper.treeToValue(payloadNode, PlaceChangedV2.class)).thenReturn(payload);

        new PlaceEventProcessor(
                inboxRepository, projectionService, remoteMediaService, objectMapper,
                Clock.fixed(occurredAt, ZoneOffset.UTC))
                .process("places.events.v1", "message");

        verify(projectionService).upsert(placeId, PlaceProjectionStatus.DRAFT, occurredAt);
        verify(remoteMediaService).synchronize(
                org.mockito.ArgumentMatchers.eq(MediaOwnerType.PLACE),
                org.mockito.ArgumentMatchers.eq(placeId),
                any());
        verify(inboxRepository).saveAndFlush(any());
    }
}
