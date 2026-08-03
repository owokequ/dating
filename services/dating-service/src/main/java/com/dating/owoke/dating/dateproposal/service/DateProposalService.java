package com.dating.owoke.dating.dateproposal.service;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import com.dating.owoke.dating.couple.domain.Couple;
import com.dating.owoke.dating.couple.domain.CoupleMember;
import com.dating.owoke.dating.couple.domain.CoupleStatus;
import com.dating.owoke.dating.couple.repository.CoupleMemberRepository;
import com.dating.owoke.dating.couple.repository.CoupleRepository;
import com.dating.owoke.dating.dateproposal.domain.DateProposal;
import com.dating.owoke.dating.dateproposal.domain.DateProposalStatus;
import com.dating.owoke.dating.dateproposal.repository.DateProposalRepository;
import com.dating.owoke.dating.eventprojection.domain.EventOccurrenceProjection;
import com.dating.owoke.dating.eventprojection.repository.EventOccurrenceProjectionRepository;
import com.dating.owoke.dating.placeprojection.domain.PlaceProjection;
import com.dating.owoke.dating.placeprojection.repository.PlaceProjectionRepository;
import com.dating.owoke.dating.shared.exception.BusinessConflictException;
import com.dating.owoke.dating.shared.exception.ResourceNotFoundException;
import com.dating.owoke.dating.shared.idempotency.service.IdempotencyService;
import com.dating.owoke.dating.shared.messaging.event.DateProposalCreatedV3;
import com.dating.owoke.dating.shared.messaging.event.DateProposalStatusChangedV3;
import com.dating.owoke.dating.shared.messaging.event.PrivateDateDraftCreatedV1;
import com.dating.owoke.dating.shared.messaging.service.OutboxService;

@Service
public class DateProposalService {

    private static final String DATING_EVENTS_TOPIC = "dating.events.v1";
    private static final String IDENTITY_COMMANDS_TOPIC = "identity.commands.v1";
    private static final Duration PRIVATE_DRAFT_TTL = Duration.ofHours(24);

    private final DateProposalRepository proposalRepository;
    private final CoupleMemberRepository memberRepository;
    private final CoupleRepository coupleRepository;
    private final PlaceProjectionRepository placeRepository;
    private final EventOccurrenceProjectionRepository eventOccurrenceRepository;
    private final IdempotencyService idempotencyService;
    private final OutboxService outboxService;
    private final Clock clock;

    public DateProposalService(
            DateProposalRepository proposalRepository,
            CoupleMemberRepository memberRepository,
            CoupleRepository coupleRepository,
            PlaceProjectionRepository placeRepository,
            EventOccurrenceProjectionRepository eventOccurrenceRepository,
            IdempotencyService idempotencyService,
            OutboxService outboxService,
            Clock clock
    ) {
        this.proposalRepository = proposalRepository;
        this.memberRepository = memberRepository;
        this.coupleRepository = coupleRepository;
        this.placeRepository = placeRepository;
        this.eventOccurrenceRepository = eventOccurrenceRepository;
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
                "DateProposalCreatedV3", 3, createdEvent(proposal));
        completeOnboarding(proposal);
        idempotencyService.remember(
                userId, "CREATE_DATE_PROPOSAL", idempotencyKey, requestMaterial, proposal.getId());
        return details(proposal);
    }

    @Transactional
    public DateProposalDetails createFromEvent(
            UUID userId, UUID eventOccurrenceId, Instant visitAt, String description, String idempotencyKey) {
        String requestMaterial = eventOccurrenceId + "|" + visitAt + "|" + normalize(description);
        UUID existingId = idempotencyService.find(
                userId, "CREATE_EVENT_DATE_PROPOSAL", idempotencyKey, requestMaterial).orElse(null);
        if (existingId != null) return visibleProposal(existingId, userId);

        ActiveCouple activeCouple = requireActiveCouple(userId);
        Instant now = clock.instant();
        EventOccurrenceProjection occurrence = requireUsableOccurrence(eventOccurrenceId);
        Instant scheduledAt = resolveVisitTime(occurrence, visitAt, now);
        var event = occurrence.getEvent();
        UUID responderId = activeCouple.members().stream().map(CoupleMember::getUserId)
                .filter(memberId -> !memberId.equals(userId)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Active couple has no responder"));
        DateProposal proposal = proposalRepository.save(DateProposal.forEvent(
                activeCouple.couple().getId(), userId, responderId, scheduledAt, event.getLocalPlaceId(),
                event.getVenueName(), event.getVenueAddress(), event.getCoverMediaId(), event.getId(),
                occurrence.getId(), event.getTitle(), event.getSourcePageUrl(), event.getPriceText(), description, now));
        outboxService.enqueue(DATING_EVENTS_TOPIC, proposal.getCoupleId().toString(),
                "DateProposalCreatedV3", 3, createdEvent(proposal));
        completeOnboarding(proposal);
        idempotencyService.remember(userId, "CREATE_EVENT_DATE_PROPOSAL", idempotencyKey,
                requestMaterial, proposal.getId());
        return details(proposal);
    }

    @Transactional
    public DateProposalDetails createPrivateDraft(
            UUID userId, Instant scheduledAt, String placeName, String placeAddress,
            String description, String idempotencyKey) {
        String requestMaterial = scheduledAt + "|" + normalize(placeName) + "|" + normalize(placeAddress)
                + "|" + normalize(description);
        UUID existingId = idempotencyService.find(
                userId, "CREATE_PRIVATE_DATE_DRAFT", idempotencyKey, requestMaterial).orElse(null);
        if (existingId != null) return visibleProposal(existingId, userId);
        ActiveCouple activeCouple = requireActiveCouple(userId);
        Instant now = clock.instant();
        if (scheduledAt == null || !scheduledAt.isAfter(now)) {
            throw new IllegalArgumentException("scheduledAt must be in the future");
        }
        UUID responderId = responder(activeCouple, userId);
        DateProposal proposal = proposalRepository.save(DateProposal.privateDraft(
                activeCouple.couple().getId(), userId, responderId, scheduledAt, placeName, placeAddress,
                description, now.plus(PRIVATE_DRAFT_TTL), now));
        outboxService.enqueue(DATING_EVENTS_TOPIC, proposal.getCoupleId().toString(),
                "PrivateDateDraftCreatedV1", 1, new PrivateDateDraftCreatedV1(
                        proposal.getId(), proposal.getCoupleId(), proposal.getProposerId(), proposal.getResponderId(),
                        proposal.getDraftExpiresAt()));
        idempotencyService.remember(userId, "CREATE_PRIVATE_DATE_DRAFT", idempotencyKey,
                requestMaterial, proposal.getId());
        return details(proposal);
    }

    @Transactional
    public DateProposalDetails sendPrivateDraft(UUID proposalId, UUID userId, String idempotencyKey) {
        UUID existingId = idempotencyService.find(
                userId, "SEND_PRIVATE_DATE_DRAFT", idempotencyKey, proposalId.toString()).orElse(null);
        if (existingId != null) return visibleProposal(existingId, userId);
        DateProposal proposal = ownedDraft(proposalId, userId);
        Instant now = clock.instant();
        proposal.send(userId, now);
        outboxService.enqueue(DATING_EVENTS_TOPIC, proposal.getCoupleId().toString(),
                "DateProposalCreatedV3", 3, createdEvent(proposal));
        completeOnboarding(proposal);
        idempotencyService.remember(userId, "SEND_PRIVATE_DATE_DRAFT", idempotencyKey,
                proposalId.toString(), proposalId);
        return details(proposal);
    }

    @Transactional
    public void updatePrivateDraftCover(UUID proposalId, UUID coverMediaId) {
        proposalRepository.findById(proposalId)
                .filter(proposal -> proposal.getSelectionType() == com.dating.owoke.dating.dateproposal.domain.DateSelectionType.PRIVATE_PLACE)
                .ifPresent(proposal -> proposal.updateDraftCover(coverMediaId));
    }

    @Scheduled(fixedDelayString = "${owoke.dating.private-draft-cleanup-delay:PT15M}")
    @Transactional
    public void expirePrivateDrafts() {
        Instant now = clock.instant();
        proposalRepository.findByStatusAndDraftExpiresAtBefore(DateProposalStatus.DRAFT, now)
                .forEach(proposal -> proposal.expireDraft(now));
    }

    @Transactional(readOnly = true)
    public List<DateProposalDetails> list(UUID userId) {
        ActiveCouple activeCouple = requireActiveCouple(userId);
        return proposalRepository.findByCoupleIdOrderByScheduledAtDesc(activeCouple.couple().getId())
                .stream().filter(proposal -> proposal.getStatus() != DateProposalStatus.DRAFT
                        || proposal.getProposerId().equals(userId))
                .map(DateProposalService::details).toList();
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

        if (action == Action.ACCEPT && proposal.getEventOccurrenceId() != null) {
            EventOccurrenceProjection occurrence = requireUsableOccurrence(proposal.getEventOccurrenceId());
            resolveVisitTime(occurrence, occurrence.isContinuous() ? proposal.getScheduledAt() : null, clock.instant());
        }

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
                3,
                statusEvent(proposal, userId, now));
        idempotencyService.remember(
                userId, action.operation, idempotencyKey, requestMaterial, proposal.getId());
        return details(proposal);
    }

    private DateProposalDetails visibleProposal(UUID proposalId, UUID userId) {
        ActiveCouple activeCouple = requireActiveCouple(userId);
        return proposalRepository.findById(proposalId)
                .filter(proposal -> proposal.getCoupleId().equals(activeCouple.couple().getId()))
                .filter(proposal -> proposal.getStatus() != DateProposalStatus.DRAFT
                        || proposal.getProposerId().equals(userId))
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

    private DateProposal ownedDraft(UUID proposalId, UUID userId) {
        ActiveCouple activeCouple = requireActiveCouple(userId);
        return proposalRepository.findById(proposalId)
                .filter(proposal -> proposal.getCoupleId().equals(activeCouple.couple().getId()))
                .filter(proposal -> proposal.getProposerId().equals(userId))
                .filter(proposal -> proposal.getStatus() == DateProposalStatus.DRAFT)
                .orElseThrow(() -> new ResourceNotFoundException("Private date draft was not found"));
    }

    private static UUID responder(ActiveCouple activeCouple, UUID userId) {
        return activeCouple.members().stream()
                .map(CoupleMember::getUserId)
                .filter(memberId -> !memberId.equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Active couple has no responder"));
    }

    private static DateProposalStatusChangedV3 statusEvent(DateProposal proposal, UUID userId, Instant now) {
        return new DateProposalStatusChangedV3(
                proposal.getId(),
                proposal.getCoupleId(),
                proposal.getProposerId(),
                proposal.getResponderId(),
                proposal.getStatus().name(),
                userId,
                now,
                proposal.getScheduledAt(),
                proposal.getTimezone(),
                proposal.getSelectionType().name(),
                proposal.getPlaceId(),
                proposal.getPlaceNameSnapshot(),
                proposal.getPlaceAddressSnapshot(),
                proposal.getPlaceCoverMediaIdSnapshot(),
                proposal.getEventId(), proposal.getEventOccurrenceId(), proposal.getEventTitleSnapshot(),
                proposal.getEventSourceUrlSnapshot(), proposal.getEventPriceSnapshot(),
                proposal.getDescription());
    }

    private static DateProposalCreatedV3 createdEvent(DateProposal proposal) {
        return new DateProposalCreatedV3(proposal.getId(), proposal.getCoupleId(), proposal.getProposerId(),
                proposal.getResponderId(), proposal.getScheduledAt(), proposal.getTimezone(),
                proposal.getSelectionType().name(), proposal.getPlaceId(), proposal.getPlaceNameSnapshot(),
                proposal.getPlaceAddressSnapshot(), proposal.getPlaceCoverMediaIdSnapshot(), proposal.getEventId(),
                proposal.getEventOccurrenceId(), proposal.getEventTitleSnapshot(), proposal.getEventSourceUrlSnapshot(),
                proposal.getEventPriceSnapshot(), proposal.getDescription());
    }

    private void completeOnboarding(DateProposal proposal) {
        for (UUID userId : List.of(proposal.getProposerId(), proposal.getResponderId())) {
            outboxService.enqueue(IDENTITY_COMMANDS_TOPIC, userId.toString(), "OnboardingCompletedV1",
                    new com.dating.owoke.dating.shared.messaging.event.OnboardingCompletedV1(userId));
        }
    }

    private static DateProposalDetails details(DateProposal proposal) {
        return new DateProposalDetails(
                proposal.getId(), proposal.getCoupleId(), proposal.getProposerId(), proposal.getResponderId(),
                proposal.getScheduledAt(), proposal.getTimezone(), proposal.getSelectionType(), proposal.getPlaceId(),
                proposal.getPlaceNameSnapshot(), proposal.getPlaceAddressSnapshot(),
                proposal.getPlaceCoverMediaIdSnapshot(), proposal.getEventId(), proposal.getEventOccurrenceId(),
                proposal.getEventTitleSnapshot(), proposal.getEventSourceUrlSnapshot(), proposal.getEventPriceSnapshot(), proposal.getDescription(),
                proposal.getStatus(), proposal.getCreatedAt(), proposal.getDecidedAt(), proposal.getCancelledAt(),
                proposal.getVersion());
    }

    private EventOccurrenceProjection requireUsableOccurrence(UUID occurrenceId) {
        return eventOccurrenceRepository.findDetailedById(occurrenceId)
                .filter(EventOccurrenceProjection::isActive)
                .filter(item -> item.getEvent().isActive())
                .orElseThrow(() -> new BusinessConflictException("Selected event occurrence is unavailable"));
    }

    private static Instant resolveVisitTime(EventOccurrenceProjection occurrence, Instant visitAt, Instant now) {
        if (!occurrence.isContinuous()) {
            if (visitAt != null) throw new IllegalArgumentException("visitAt must be omitted for a fixed event session");
            if (!occurrence.getStartsAt().isAfter(now)) throw new BusinessConflictException("Event session has already started");
            return occurrence.getStartsAt();
        }
        if (visitAt == null) throw new IllegalArgumentException("visitAt is required for a continuous event");
        if (!visitAt.isAfter(now) || visitAt.isBefore(occurrence.getStartsAt())
                || (occurrence.getEndsAt() != null && visitAt.isAfter(occurrence.getEndsAt()))) {
            throw new IllegalArgumentException("visitAt must be in the future and inside the event period");
        }
        return visitAt;
    }

    private static String normalize(String description) {
        return description == null ? "" : description.trim();
    }

    private record ActiveCouple(Couple couple, List<CoupleMember> members) {
    }

    private enum Action {
        ACCEPT("ACCEPT_DATE_PROPOSAL", "DateProposalAcceptedV3"),
        DECLINE("DECLINE_DATE_PROPOSAL", "DateProposalDeclinedV3"),
        CANCEL("CANCEL_DATE_PROPOSAL", "DateProposalCancelledV3");

        private final String operation;
        private final String eventType;

        Action(String operation, String eventType) {
            this.operation = operation;
            this.eventType = eventType;
        }
    }
}
