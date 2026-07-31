package com.dating.owoke.dating.dateproposal.messaging.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.dating.shared.messaging.domain.InboxEvent;
import com.dating.owoke.dating.shared.messaging.domain.IncomingEventEnvelope;
import com.dating.owoke.dating.shared.messaging.event.DateProposalDecisionResultV1;
import com.dating.owoke.dating.shared.messaging.repository.InboxEventRepository;
import com.dating.owoke.dating.shared.messaging.service.OutboxService;

@Service
public class DateProposalCommandResultService {

    private static final String DATING_EVENTS_TOPIC = "dating.events.v1";

    private final InboxEventRepository inboxRepository;
    private final OutboxService outboxService;
    private final Clock clock;

    public DateProposalCommandResultService(
            InboxEventRepository inboxRepository,
            OutboxService outboxService,
            Clock clock) {
        this.inboxRepository = inboxRepository;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    @Transactional
    public void record(
            String topic,
            IncomingEventEnvelope envelope,
            DateProposalDecisionResultV1 result) {
        if (inboxRepository.existsById(envelope.eventId())) {
            return;
        }
        outboxService.enqueue(
                DATING_EVENTS_TOPIC,
                result.coupleId().toString(),
                "DateProposalDecisionResultV1",
                result);
        inboxRepository.saveAndFlush(new InboxEvent(
                envelope.eventId(), envelope.eventType(), topic, clock.instant()));
    }
}
