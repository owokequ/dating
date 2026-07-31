package com.dating.owoke.dating.dateproposal.messaging.service;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.dating.owoke.dating.dateproposal.exception.InvalidDateProposalActionException;
import com.dating.owoke.dating.dateproposal.messaging.event.DateProposalDecisionRequestedV1;
import com.dating.owoke.dating.dateproposal.service.DateProposalService;
import com.dating.owoke.dating.shared.exception.BusinessConflictException;
import com.dating.owoke.dating.shared.exception.ResourceNotFoundException;
import com.dating.owoke.dating.shared.messaging.domain.IncomingEventEnvelope;
import com.dating.owoke.dating.shared.messaging.event.DateProposalDecisionResultV1;
import com.dating.owoke.dating.shared.messaging.repository.InboxEventRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class DateProposalCommandProcessor {

    private static final Set<String> SUPPORTED_DECISIONS = Set.of("ACCEPT", "DECLINE");

    private final InboxEventRepository inboxRepository;
    private final DateProposalService proposalService;
    private final DateProposalCommandResultService resultService;
    private final ObjectMapper objectMapper;

    public DateProposalCommandProcessor(
            InboxEventRepository inboxRepository,
            DateProposalService proposalService,
            DateProposalCommandResultService resultService,
            ObjectMapper objectMapper) {
        this.inboxRepository = inboxRepository;
        this.proposalService = proposalService;
        this.resultService = resultService;
        this.objectMapper = objectMapper;
    }

    public void process(String topic, String message) {
        IncomingEventEnvelope envelope = readEnvelope(message);
        validateEnvelope(envelope);
        if (inboxRepository.existsById(envelope.eventId())) {
            return;
        }

        DateProposalDecisionRequestedV1 command = readCommand(envelope);
        validateCommand(envelope, command);
        String errorCode = null;
        try {
            if ("ACCEPT".equals(command.decision())) {
                proposalService.accept(command.proposalId(), command.actorId(), envelope.eventId().toString());
            } else {
                proposalService.decline(command.proposalId(), command.actorId(), envelope.eventId().toString());
            }
        } catch (ResourceNotFoundException exception) {
            errorCode = "PROPOSAL_NOT_FOUND";
        } catch (InvalidDateProposalActionException exception) {
            errorCode = "ACTION_NOT_ALLOWED";
        } catch (BusinessConflictException exception) {
            errorCode = "COUPLE_NOT_ACTIVE";
        }

        resultService.record(
                topic,
                envelope,
                new DateProposalDecisionResultV1(
                        envelope.eventId(),
                        command.proposalId(),
                        command.coupleId(),
                        command.actorId(),
                        command.decision(),
                        errorCode == null,
                        errorCode));
    }

    private IncomingEventEnvelope readEnvelope(String message) {
        try {
            return objectMapper.readValue(message, IncomingEventEnvelope.class);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid dating command envelope", exception);
        }
    }

    private DateProposalDecisionRequestedV1 readCommand(IncomingEventEnvelope envelope) {
        try {
            return objectMapper.treeToValue(envelope.payload(), DateProposalDecisionRequestedV1.class);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid date proposal decision command", exception);
        }
    }

    private void validateEnvelope(IncomingEventEnvelope envelope) {
        if (envelope.eventId() == null || envelope.occurredAt() == null || envelope.payload() == null) {
            throw new IllegalArgumentException("Dating command is missing required envelope fields");
        }
        if (envelope.eventVersion() != 1
                || !"DateProposalDecisionRequestedV1".equals(envelope.eventType())) {
            throw new IllegalArgumentException("Unsupported dating command type or version");
        }
    }

    private void validateCommand(
            IncomingEventEnvelope envelope,
            DateProposalDecisionRequestedV1 command) {
        if (command.proposalId() == null || command.coupleId() == null || command.actorId() == null
                || !SUPPORTED_DECISIONS.contains(command.decision())) {
            throw new IllegalArgumentException("Invalid date proposal decision command");
        }
        if (!command.coupleId().toString().equals(envelope.aggregateId())) {
            throw new IllegalArgumentException("Dating command key does not match coupleId");
        }
    }
}
