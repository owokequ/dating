package com.dating.owoke.media.collection.messaging.service;

import java.time.Clock;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.media.asset.service.RemoteImageDescriptor;
import com.dating.owoke.media.asset.service.RemoteMediaService;
import com.dating.owoke.media.collection.domain.EventProjectionStatus;
import com.dating.owoke.media.collection.domain.MediaOwnerType;
import com.dating.owoke.media.collection.messaging.event.EventChangedV1;
import com.dating.owoke.media.collection.service.EventProjectionService;
import com.dating.owoke.media.shared.messaging.domain.InboxEvent;
import com.dating.owoke.media.shared.messaging.domain.IncomingEventEnvelope;
import com.dating.owoke.media.shared.messaging.repository.InboxEventRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class EventEventProcessor {

    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "EventDraftedV1", "EventPublishedV1", "EventUpdatedV1", "EventHiddenV1", "EventArchivedV1");

    private final InboxEventRepository inboxRepository;
    private final EventProjectionService projectionService;
    private final RemoteMediaService remoteMediaService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public EventEventProcessor(
            InboxEventRepository inboxRepository,
            EventProjectionService projectionService,
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
        EventChangedV1 payload = objectMapper.treeToValue(envelope.payload(), EventChangedV1.class);
        projectionService.upsert(
                payload.eventId(), EventProjectionStatus.valueOf(payload.status()), envelope.occurredAt());
        List<RemoteImageDescriptor> images = payload.images() == null ? List.of() : payload.images().stream()
                .map(image -> new RemoteImageDescriptor(
                        image.providerAssetKey(), image.remoteUrl(), image.sourceName(), image.sourceLink()))
                .toList();
        remoteMediaService.synchronize(MediaOwnerType.EVENT, payload.eventId(), images);
        inboxRepository.saveAndFlush(new InboxEvent(
                envelope.eventId(), envelope.eventType(), topic, clock.instant()));
    }

    private IncomingEventEnvelope readEnvelope(String message) {
        try {
            return objectMapper.readValue(message, IncomingEventEnvelope.class);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid events event envelope", exception);
        }
    }

    private void validate(IncomingEventEnvelope envelope) {
        if (envelope.eventId() == null || envelope.occurredAt() == null || envelope.payload() == null) {
            throw new IllegalArgumentException("Events event is missing required envelope fields");
        }
        if (envelope.eventVersion() != 1 || !SUPPORTED_EVENTS.contains(envelope.eventType())) {
            throw new IllegalArgumentException("Unsupported events event type or version");
        }
    }
}
