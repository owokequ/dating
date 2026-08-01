package com.dating.owoke.media.collection.messaging.service;

import java.time.Clock;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.media.collection.domain.PlaceProjectionStatus;
import com.dating.owoke.media.collection.messaging.event.PlaceChangedV1;
import com.dating.owoke.media.collection.messaging.event.PlaceChangedV2;
import com.dating.owoke.media.asset.service.RemoteImageDescriptor;
import com.dating.owoke.media.asset.service.RemoteMediaService;
import com.dating.owoke.media.collection.domain.MediaOwnerType;
import com.dating.owoke.media.collection.service.PlaceProjectionService;
import com.dating.owoke.media.shared.messaging.domain.InboxEvent;
import com.dating.owoke.media.shared.messaging.domain.IncomingEventEnvelope;
import com.dating.owoke.media.shared.messaging.repository.InboxEventRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class PlaceEventProcessor {

    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "PlaceDraftedV1", "PlacePublishedV1", "PlaceUpdatedV1", "PlaceArchivedV1");
    private static final Set<String> SUPPORTED_EVENTS_V2 = Set.of(
            "PlaceDraftedV2", "PlacePublishedV2", "PlaceUpdatedV2", "PlaceArchivedV2");

    private final InboxEventRepository inboxRepository;
    private final PlaceProjectionService projectionService;
    private final RemoteMediaService remoteMediaService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PlaceEventProcessor(
            InboxEventRepository inboxRepository,
            PlaceProjectionService projectionService,
            RemoteMediaService remoteMediaService,
            ObjectMapper objectMapper,
            Clock clock) {
        this.inboxRepository = inboxRepository;
        this.projectionService = projectionService;
        this.remoteMediaService = remoteMediaService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void process(String topic, String message) {
        IncomingEventEnvelope envelope = readEnvelope(message);
        validate(envelope);
        if (inboxRepository.existsById(envelope.eventId())) {
            return;
        }
        if (envelope.eventVersion() == 1) {
            PlaceChangedV1 payload = objectMapper.treeToValue(envelope.payload(), PlaceChangedV1.class);
            projectionService.upsert(
                    payload.placeId(), PlaceProjectionStatus.valueOf(payload.status()), envelope.occurredAt());
        } else {
            PlaceChangedV2 payload = objectMapper.treeToValue(envelope.payload(), PlaceChangedV2.class);
            projectionService.upsert(
                    payload.placeId(), PlaceProjectionStatus.valueOf(payload.status()), envelope.occurredAt());
            if ("KUDAGO".equals(payload.source())) {
                remoteMediaService.synchronize(
                        MediaOwnerType.PLACE,
                        payload.placeId(),
                        payload.images() == null ? java.util.List.of() : payload.images().stream()
                                .map(image -> new RemoteImageDescriptor(
                                        image.providerAssetKey(), image.remoteUrl(), image.sourceName(), image.sourceLink()))
                                .toList());
            }
        }
        inboxRepository.saveAndFlush(new InboxEvent(
                envelope.eventId(), envelope.eventType(), topic, clock.instant()));
    }

    private IncomingEventEnvelope readEnvelope(String message) {
        try {
            return objectMapper.readValue(message, IncomingEventEnvelope.class);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid places event envelope", exception);
        }
    }

    private void validate(IncomingEventEnvelope envelope) {
        if (envelope.eventId() == null || envelope.occurredAt() == null || envelope.payload() == null) {
            throw new IllegalArgumentException("Places event is missing required envelope fields");
        }
        boolean v1 = envelope.eventVersion() == 1 && SUPPORTED_EVENTS.contains(envelope.eventType());
        boolean v2 = envelope.eventVersion() == 2 && SUPPORTED_EVENTS_V2.contains(envelope.eventType());
        if (!v1 && !v2) {
            throw new IllegalArgumentException("Unsupported places event type or version");
        }
    }
}
