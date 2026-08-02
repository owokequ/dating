package com.dating.owoke.media.collection.messaging.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.media.collection.domain.PrivateDateDraftProjection;
import com.dating.owoke.media.collection.messaging.event.PrivateDateDraftCreatedV1;
import com.dating.owoke.media.collection.repository.PrivateDateDraftProjectionRepository;
import com.dating.owoke.media.shared.messaging.domain.InboxEvent;
import com.dating.owoke.media.shared.messaging.domain.IncomingEventEnvelope;
import com.dating.owoke.media.shared.messaging.repository.InboxEventRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class PrivateDateDraftEventProcessor {

    private final InboxEventRepository inboxRepository;
    private final PrivateDateDraftProjectionRepository draftRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PrivateDateDraftEventProcessor(
            InboxEventRepository inboxRepository,
            PrivateDateDraftProjectionRepository draftRepository,
            ObjectMapper objectMapper,
            Clock clock) {
        this.inboxRepository = inboxRepository;
        this.draftRepository = draftRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void process(String topic, String message) {
        IncomingEventEnvelope envelope = read(message);
        if (!"PrivateDateDraftCreatedV1".equals(envelope.eventType()) || envelope.eventVersion() != 1) return;
        if (envelope.eventId() == null || envelope.payload() == null) {
            throw new IllegalArgumentException("Private date draft event is incomplete");
        }
        if (inboxRepository.existsById(envelope.eventId())) return;
        PrivateDateDraftCreatedV1 payload = objectMapper.treeToValue(envelope.payload(), PrivateDateDraftCreatedV1.class);
        if (payload.proposalId() == null || payload.proposerId() == null || payload.expiresAt() == null) {
            throw new IllegalArgumentException("Private date draft event is incomplete");
        }
        draftRepository.save(new PrivateDateDraftProjection(payload.proposalId(), payload.proposerId(), payload.expiresAt()));
        inboxRepository.saveAndFlush(new InboxEvent(envelope.eventId(), envelope.eventType(), topic, clock.instant()));
    }

    private IncomingEventEnvelope read(String message) {
        try {
            return objectMapper.readValue(message, IncomingEventEnvelope.class);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid dating event envelope", exception);
        }
    }
}
