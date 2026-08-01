package com.dating.owoke.dating.eventprojection.messaging.service;

import java.time.Clock;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.dating.eventprojection.messaging.domain.EventChangedV1;
import com.dating.owoke.dating.eventprojection.service.EventProjectionService;
import com.dating.owoke.dating.shared.messaging.domain.InboxEvent;
import com.dating.owoke.dating.shared.messaging.domain.IncomingEventEnvelope;
import com.dating.owoke.dating.shared.messaging.repository.InboxEventRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class EventEventProcessor {
    private static final Set<String> SUPPORTED = Set.of("EventDraftedV1", "EventPublishedV1", "EventUpdatedV1",
            "EventHiddenV1", "EventArchivedV1");
    private final InboxEventRepository inboxRepository;
    private final EventProjectionService projectionService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public EventEventProcessor(
            InboxEventRepository inboxRepository,
            EventProjectionService projectionService,
            ObjectMapper objectMapper,
            Clock clock) {
        this.inboxRepository = inboxRepository;
        this.projectionService = projectionService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }
    @Transactional
    public void process(String topic, String message) {
        IncomingEventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(message, IncomingEventEnvelope.class);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid event envelope", exception);
        }
        if (envelope.eventId() == null || envelope.payload() == null || envelope.occurredAt() == null
                || envelope.eventVersion() != 1 || !SUPPORTED.contains(envelope.eventType())) {
            throw new IllegalArgumentException("Unsupported or incomplete event envelope");
        }
        if (inboxRepository.existsById(envelope.eventId())) {
            return;
        }
        projectionService.upsert(objectMapper.treeToValue(envelope.payload(), EventChangedV1.class), envelope.occurredAt());
        inboxRepository.saveAndFlush(new InboxEvent(envelope.eventId(), envelope.eventType(), topic, clock.instant()));
    }
}
