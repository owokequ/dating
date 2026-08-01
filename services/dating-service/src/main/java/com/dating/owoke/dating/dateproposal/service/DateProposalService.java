package com.dating.owoke.dating.dateproposal.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.dating.couple.domain.Couple;
import com.dating.owoke.dating.couple.domain.CoupleMember;
import com.dating.owoke.dating.couple.domain.CoupleStatus;
import com.dating.owoke.dating.couple.repository.CoupleMemberRepository;
import com.dating.owoke.dating.couple.repository.CoupleRepository;
import com.dating.owoke.dating.dateproposal.domain.DateProposal;
import com.dating.owoke.dating.dateproposal.repository.DateProposalRepository;
import com.dating.owoke.dating.placeprojection.domain.PlaceProjection;
import com.dating.owoke.dating.placeprojection.repository.PlaceProjectionRepository;
import com.dating.owoke.dating.shared.exception.BusinessConflictException;
import com.dating.owoke.dating.shared.exception.ResourceNotFoundException;
import com.dating.owoke.dating.shared.idempotency.service.IdempotencyService;
import com.dating.owoke.dating.shared.messaging.event.DateProposalCreatedV2;
import com.dating.owoke.dating.shared.messaging.event.DateProposalStatusChangedV2;
import com.dating.owoke.dating.shared.messaging.service.OutboxService;

@Service
public class DateProposalService {

    private static final String DATING_EVENTS_TOPIC = "dating.events.v1";

    private final DateProposalRepository proposalRepository;
    private final CoupleMemberRepository memberRepository;
    private final CoupleRepository coupleRepository;
    private final PlaceProjectionRepository placeRepository;
    private final IdempotencyService idempotencyService;
    private final OutboxService outboxService;
    private final Clock clock;

    public DateProposalService(
            DateProposalRepository proposalRepository,
            CoupleMemberRepository memberRepository,
            CoupleRepository coupleRepository,
            PlaceProjectionRepository placeRepository,
            IdempotencyService idempotencyService,
            OutboxService outboxService,
            Clock clock
    ) {
        this.proposalRepository = proposalRepository;
        this.memberRepository = memberRepository;
        this.coupleRepository = coupleRepository;
        this.placeRepository = placeRepository;
        this.idempotencyService = idempotencyService;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    @Transactional
    public DateProposalDetails create(
            UUID userId,
            Instant scheduledAt,
            UUID placeId,
            String description,
            String idempotencyKey
    ) {
        String requestMaterial = scheduledAt + "|" + placeId + "|" + normalize(description);
        UUID existingId = idempotencyService.find(
                userId, "CREATE_DATE_PROPOSAL", idempotencyKey, requestMaterial).orElse(null);
        if (existingId != null) {
            return visibleProposal(existingId, userId);
        }

        ActiveCouple activeCouple = requireActiveCouple(userId);
        Instant now = clock.instant();
        if (scheduledAt == null || !scheduledAt.isAfter(now)) {
            throw new IllegalArgumentException("scheduledAt must be in the future");
        }
        PlaceProjection place = placeRepository.findById(placeId)
                .filter(PlaceProjection::isActive)
                .orElseThrow(() -> new BusinessConflictException("Selected place is unavailable"));
        UUID responderId = activeCouple.members().stream()
                .map(CoupleMember::getUserId)
                .filter(memberId -> !memberId.equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Active couple has no responder"));

        DateProposal proposal = proposalRepository.save(new DateProposal(
                activeCouple.couple().getId(),
                userId,
                responderId,
                scheduledAt,
                placeId,
                place.getName(),
                place.getAddress(),
                place.getCoverMediaId(),
                description,
                now));
        outboxService.enqueue(
                DATING_EVENTS_TOPIC,
                activeCouple.couple().getId().toString(),
                "DateProposalCreatedV2",
                2,
                new DateProposalCreatedV2(
                        proposal.getId(),
                        proposal.getCoupleId(),
                        proposal.getProposerId(),
                        proposal.getResponderId(),
                        proposal.getScheduledAt(),
                        proposal.getTimezone(),
                        proposal.getPlaceId(),
                        proposal.getPlaceNameSnapshot(),
                        proposal.getPlaceAddressSnapshot(),
                        proposal.getPlaceCoverMediaIdSnapshot(),
                        proposal.getDescription()));
        idempotencyService.remember(
                userId, "CREATE_DATE_PROPOSAL", idempotencyKey, requestMaterial, proposal.getId());
        return details(proposal);
    }

    @Transactional(readOnly = true)
    public List<DateProposalDetails> list(UUID userId) {
        ActiveCouple activeCouple = requireActiveCouple(userId);
        return proposalRepository.findByCoupleIdOrderByScheduledAtDesc(activeCouple.couple().getId())
                .stream().map(DateProposalService::details).toList();
    }

    @Transactional(readOnly = true)
    public DateProposalDetails get(UUID proposalId, UUID userId) {
        return visibleProposal(proposalId, userId);
    }

    @Transactional
    public DateProposalDetails accept(UUID proposalId, UUID userId, String idempotencyKey) {
        return changeStatus(proposalId, userId, idempotencyKey, Action.ACCEPT);
    }

    @Transactional
    public DateProposalDetails decline(UUID proposalId, UUID userId, String idempotencyKey) {
        return changeStatus(proposalId, userId, idempotencyKey, Action.DECLINE);
    }

    @Transactional
    public DateProposalDetails cancel(UUID proposalId, UUID userId, String idempotencyKey) {
        return changeStatus(proposalId, userId, idempotencyKey, Action.CANCEL);
    }

    private DateProposalDetails changeStatus(
            UUID proposalId,
            UUID userId,
            String idempotencyKey,
            Action action
    ) {
        String requestMaterial = proposalId.toString();
        UUID existingId = idempotencyService.find(
                userId, action.operation, idempotencyKey, requestMaterial).orElse(null);
        if (existingId != null) {
            return visibleProposal(existingId, userId);
        }
        ActiveCouple activeCouple = requireActiveCouple(userId);
        DateProposal proposal = proposalRepository.findById(proposalId)
                .filter(value -> value.getCoupleId().equals(activeCouple.couple().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Date proposal was not found"));

        Instant now = clock.instant();
        switch (action) {
            case ACCEPT -> proposal.accept(userId, now);
            case DECLINE -> proposal.decline(userId, now);
            case CANCEL -> proposal.cancel(userId, now);
        }
        outboxService.enqueue(
                DATING_EVENTS_TOPIC,
                proposal.getCoupleId().toString(),
                action.eventType,
                2,
                statusEvent(proposal, userId, now));
        idempotencyService.remember(
                userId, action.operation, idempotencyKey, requestMaterial, proposal.getId());
        return details(proposal);
    }

    private DateProposalDetails visibleProposal(UUID proposalId, UUID userId) {
        ActiveCouple activeCouple = requireActiveCouple(userId);
        return proposalRepository.findById(proposalId)
                .filter(proposal -> proposal.getCoupleId().equals(activeCouple.couple().getId()))
                .map(DateProposalService::details)
                .orElseThrow(() -> new ResourceNotFoundException("Date proposal was not found"));
    }

    private ActiveCouple requireActiveCouple(UUID userId) {
        CoupleMember membership = memberRepository.findByUserIdAndLeftAtIsNull(userId)
                .orElseThrow(() -> new BusinessConflictException("User is not in an active couple"));
        Couple couple = coupleRepository.findById(membership.getCoupleId())
                .filter(value -> value.getStatus() == CoupleStatus.ACTIVE)
                .orElseThrow(() -> new BusinessConflictException("User is not in an active couple"));
        List<CoupleMember> members = memberRepository.findByCoupleIdAndLeftAtIsNullOrderByJoinedAt(couple.getId());
        if (members.size() != 2) {
            throw new IllegalStateException("Active couple must have exactly two members");
        }
        return new ActiveCouple(couple, members);
    }

    private static DateProposalStatusChangedV2 statusEvent(DateProposal proposal, UUID userId, Instant now) {
        return new DateProposalStatusChangedV2(
                proposal.getId(),
                proposal.getCoupleId(),
                proposal.getProposerId(),
                proposal.getResponderId(),
                proposal.getStatus().name(),
                userId,
                now,
                proposal.getScheduledAt(),
                proposal.getTimezone(),
                proposal.getPlaceNameSnapshot(),
                proposal.getPlaceAddressSnapshot(),
                proposal.getPlaceCoverMediaIdSnapshot(),
                proposal.getDescription());
    }

    private static DateProposalDetails details(DateProposal proposal) {
        return new DateProposalDetails(
                proposal.getId(), proposal.getCoupleId(), proposal.getProposerId(), proposal.getResponderId(),
                proposal.getScheduledAt(), proposal.getTimezone(), proposal.getPlaceId(),
                proposal.getPlaceNameSnapshot(), proposal.getPlaceAddressSnapshot(),
                proposal.getPlaceCoverMediaIdSnapshot(), proposal.getDescription(),
                proposal.getStatus(), proposal.getCreatedAt(), proposal.getDecidedAt(), proposal.getCancelledAt(),
                proposal.getVersion());
    }

    private static String normalize(String description) {
        return description == null ? "" : description.trim();
    }

    private record ActiveCouple(Couple couple, List<CoupleMember> members) {
    }

    private enum Action {
        ACCEPT("ACCEPT_DATE_PROPOSAL", "DateProposalAcceptedV2"),
        DECLINE("DECLINE_DATE_PROPOSAL", "DateProposalDeclinedV2"),
        CANCEL("CANCEL_DATE_PROPOSAL", "DateProposalCancelledV2");

        private final String operation;
        private final String eventType;

        Action(String operation, String eventType) {
            this.operation = operation;
            this.eventType = eventType;
        }
    }
}
