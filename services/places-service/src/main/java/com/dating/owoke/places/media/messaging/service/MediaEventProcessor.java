package com.dating.owoke.places.media.messaging.service;

import java.time.Clock;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.places.media.messaging.event.MediaCollectionChangedV1;
import com.dating.owoke.places.media.messaging.event.MediaCollectionChangedV2;
import com.dating.owoke.places.media.service.PlaceMediaProjectionService;
import com.dating.owoke.places.shared.messaging.domain.InboxEvent;
import com.dating.owoke.places.shared.messaging.domain.IncomingEventEnvelope;
import com.dating.owoke.places.shared.messaging.repository.InboxEventRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class MediaEventProcessor {

    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "MediaAssetReadyV1", "MediaCollectionChangedV1", "MediaAssetDeletedV1");
    private static final Set<String> SUPPORTED_V2_EVENTS = Set.of("MediaCollectionChangedV2");

    private final InboxEventRepository inboxRepository;
    private final PlaceMediaProjectionService projectionService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public MediaEventProcessor(
            InboxEventRepository inboxRepository,
            PlaceMediaProjectionService projectionService,
            ObjectMapper objectMapper,
            Clock clock) {
        this.inboxRepository = inboxRepository;
        this.projectionService = projectionService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void process(String topic, String message) {
        IncomingEventEnvelope envelope = read(message);
        validate(envelope);
        if (inboxRepository.existsById(envelope.eventId())) {
            return;
        }
        if ("MediaCollectionChangedV1".equals(envelope.eventType())) {
            MediaCollectionChangedV1 payload = objectMapper.treeToValue(
                    envelope.payload(), MediaCollectionChangedV1.class);
            if (!"PLACE".equals(payload.ownerType()) || payload.ownerId() == null) {
                throw new IllegalArgumentException("Unsupported media collection owner");
            }
            projectionService.replace(
                    payload.ownerId(),
                    payload.coverMediaId(),
                    payload.orderedMediaIds(),
                    payload.collectionVersion(),
                    envelope.occurredAt());
        } else if ("MediaCollectionChangedV2".equals(envelope.eventType())) {
            MediaCollectionChangedV2 payload = objectMapper.treeToValue(
                    envelope.payload(), MediaCollectionChangedV2.class);
            if (!"PLACE".equals(payload.ownerType()) || payload.ownerId() == null) {
                throw new IllegalArgumentException("Unsupported media collection owner");
            }
            projectionService.replaceV2(
                    payload.ownerId(),
                    payload.coverMediaId(),
                    payload.items(),
                    payload.collectionVersion(),
                    envelope.occurredAt());
        }
        inboxRepository.saveAndFlush(new InboxEvent(
                envelope.eventId(), envelope.eventType(), topic, clock.instant()));
    }

    private IncomingEventEnvelope read(String message) {
        try {
            return objectMapper.readValue(message, IncomingEventEnvelope.class);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid media event envelope", exception);
        }
    }

    private void validate(IncomingEventEnvelope envelope) {
        if (envelope.eventId() == null || envelope.payload() == null || envelope.occurredAt() == null) {
            throw new IllegalArgumentException("Media event is missing required envelope fields");
        }
        boolean supportedV1 = envelope.eventVersion() == 1 && SUPPORTED_EVENTS.contains(envelope.eventType());
        boolean supportedV2 = envelope.eventVersion() == 2 && SUPPORTED_V2_EVENTS.contains(envelope.eventType());
        if (!supportedV1 && !supportedV2) {
            throw new IllegalArgumentException("Unsupported media event type or version");
        }
    }
}
