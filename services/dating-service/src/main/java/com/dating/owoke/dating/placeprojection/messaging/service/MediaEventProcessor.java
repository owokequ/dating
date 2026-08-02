package com.dating.owoke.dating.placeprojection.messaging.service;

import java.time.Clock;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.dating.placeprojection.messaging.domain.MediaCollectionChangedV1;
import com.dating.owoke.dating.placeprojection.messaging.domain.MediaCollectionChangedV2;
import com.dating.owoke.dating.placeprojection.service.PlaceProjectionService;
import com.dating.owoke.dating.eventprojection.service.EventProjectionService;
import com.dating.owoke.dating.dateproposal.service.DateProposalService;
import com.dating.owoke.dating.shared.messaging.domain.InboxEvent;
import com.dating.owoke.dating.shared.messaging.domain.IncomingEventEnvelope;
import com.dating.owoke.dating.shared.messaging.repository.InboxEventRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class MediaEventProcessor {

    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "MediaAssetReadyV1", "MediaCollectionChangedV1", "MediaAssetDeletedV1", "MediaCollectionChangedV2");

    private final InboxEventRepository inboxRepository;
    private final PlaceProjectionService projectionService;
    private final EventProjectionService eventProjectionService;
    private final DateProposalService dateProposalService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public MediaEventProcessor(
            InboxEventRepository inboxRepository,
            PlaceProjectionService projectionService,
            EventProjectionService eventProjectionService,
            DateProposalService dateProposalService,
            ObjectMapper objectMapper,
            Clock clock) {
        this.inboxRepository = inboxRepository;
        this.projectionService = projectionService;
        this.eventProjectionService = eventProjectionService;
        this.dateProposalService = dateProposalService;
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
        if ("MediaCollectionChangedV1".equals(envelope.eventType())) {
            MediaCollectionChangedV1 payload = objectMapper.treeToValue(
                    envelope.payload(), MediaCollectionChangedV1.class);
            if ("PLACE".equals(payload.ownerType())) {
                projectionService.updateMedia(
                        payload.ownerId(), payload.coverMediaId(), payload.collectionVersion());
            }
        } else if ("MediaCollectionChangedV2".equals(envelope.eventType())) {
            MediaCollectionChangedV2 payload = objectMapper.treeToValue(
                    envelope.payload(), MediaCollectionChangedV2.class);
            if ("PLACE".equals(payload.ownerType())) {
                projectionService.updateMedia(payload.ownerId(), payload.coverMediaId(), payload.collectionVersion());
            } else if ("EVENT".equals(payload.ownerType())) {
                eventProjectionService.updateMedia(payload.ownerId(), payload.coverMediaId(), payload.collectionVersion());
            } else if ("DATE_PROPOSAL".equals(payload.ownerType())) {
                dateProposalService.updatePrivateDraftCover(payload.ownerId(), payload.coverMediaId());
            }
        }
        inboxRepository.saveAndFlush(new InboxEvent(
                envelope.eventId(), envelope.eventType(), topic, clock.instant()));
    }

    private IncomingEventEnvelope readEnvelope(String message) {
        try {
            return objectMapper.readValue(message, IncomingEventEnvelope.class);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid media event envelope", exception);
        }
    }

    private static void validate(IncomingEventEnvelope envelope) {
        if (envelope.eventId() == null || envelope.payload() == null || envelope.occurredAt() == null) {
            throw new IllegalArgumentException("Media event is missing required envelope fields");
        }
        boolean supportedVersion = envelope.eventVersion() == 1
                || (envelope.eventVersion() == 2 && "MediaCollectionChangedV2".equals(envelope.eventType()));
        if (!supportedVersion || !SUPPORTED_EVENTS.contains(envelope.eventType())) {
            throw new IllegalArgumentException("Unsupported media event type or version");
        }
    }
}
