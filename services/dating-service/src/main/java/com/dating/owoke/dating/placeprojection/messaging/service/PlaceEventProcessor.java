package com.dating.owoke.dating.placeprojection.messaging.service;

import java.time.Clock;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.dating.placeprojection.domain.PlaceProjectionStatus;
import com.dating.owoke.dating.placeprojection.messaging.domain.PlaceChangedV1;
import com.dating.owoke.dating.placeprojection.service.PlaceProjectionService;
import com.dating.owoke.dating.shared.messaging.domain.InboxEvent;
import com.dating.owoke.dating.shared.messaging.domain.IncomingEventEnvelope;
import com.dating.owoke.dating.shared.messaging.repository.InboxEventRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class PlaceEventProcessor {

    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "PlaceDraftedV1", "PlacePublishedV1", "PlaceUpdatedV1", "PlaceArchivedV1");

    private final InboxEventRepository inboxRepository;
    private final PlaceProjectionService projectionService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PlaceEventProcessor(
            InboxEventRepository inboxRepository,
            PlaceProjectionService projectionService,
            ObjectMapper objectMapper,
            Clock clock) {
        this.inboxRepository = inboxRepository;
        this.projectionService = projectionService;
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

        PlaceChangedV1 payload = objectMapper.treeToValue(envelope.payload(), PlaceChangedV1.class);
        projectionService.upsert(
                payload.placeId(),
                payload.name(),
                payload.address(),
                PlaceProjectionStatus.valueOf(payload.status()),
                envelope.occurredAt());
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
        if (envelope.eventVersion() != 1 || !SUPPORTED_EVENTS.contains(envelope.eventType())) {
            throw new IllegalArgumentException("Unsupported places event type or version");
        }
    }
}
