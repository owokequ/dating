package com.dating.owoke.dating.placeprojection.messaging.service;

import java.time.Clock;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.dating.placeprojection.domain.PlaceProjectionStatus;
import com.dating.owoke.dating.placeprojection.messaging.domain.PlaceChanged;
import com.dating.owoke.dating.placeprojection.messaging.domain.PlaceChangedV1;
import com.dating.owoke.dating.placeprojection.messaging.domain.PlaceChangedV2;
import com.dating.owoke.dating.placeprojection.service.PlaceProjectionService;
import com.dating.owoke.dating.shared.messaging.domain.InboxEvent;
import com.dating.owoke.dating.shared.messaging.domain.IncomingEventEnvelope;
import com.dating.owoke.dating.shared.messaging.repository.InboxEventRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class PlaceEventProcessor {

    private static final Pattern LOSSY_TEXT = Pattern.compile("[?]{3,}");

    private static final Set<String> V1_EVENTS = Set.of(
            "PlaceDraftedV1", "PlacePublishedV1", "PlaceUpdatedV1", "PlaceArchivedV1");
    private static final Set<String> V2_EVENTS = Set.of(
            "PlaceDraftedV2", "PlacePublishedV2", "PlaceUpdatedV2", "PlaceArchivedV2");

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

        PlaceChanged payload = readPayload(envelope);
        rejectLossyText(payload.name(), "name");
        rejectLossyText(payload.address(), "address");
        projectionService.upsert(
                payload.placeId(),
                payload.name(),
                payload.address(),
                PlaceProjectionStatus.valueOf(payload.status()),
                envelope.occurredAt());
        inboxRepository.saveAndFlush(new InboxEvent(
                envelope.eventId(), envelope.eventType(), topic, clock.instant()));
    }

    private PlaceChanged readPayload(IncomingEventEnvelope envelope) {
        if (envelope.eventVersion() == 1) {
            return objectMapper.treeToValue(envelope.payload(), PlaceChangedV1.class);
        }
        return objectMapper.treeToValue(envelope.payload(), PlaceChangedV2.class);
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
        boolean supported = envelope.eventVersion() == 1 && V1_EVENTS.contains(envelope.eventType())
                || envelope.eventVersion() == 2 && V2_EVENTS.contains(envelope.eventType());
        if (!supported) {
            throw new IllegalArgumentException("Unsupported places event type or version");
        }
    }

    private static void rejectLossyText(String value, String field) {
        if (value != null && LOSSY_TEXT.matcher(value).find()) {
            throw new IllegalArgumentException("Place " + field + " contains lossy character replacement");
        }
    }
}
