package com.dating.owoke.dating.dateproposal.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.dating.owoke.dating.dateproposal.exception.InvalidDateProposalActionException;

@Entity
@Table(name = "date_proposals")
public class DateProposal {

    public static final String DEFAULT_TIMEZONE = "Europe/Moscow";

    @Id
    private UUID id;
    @Column(name = "couple_id", nullable = false, updatable = false)
    private UUID coupleId;
    @Column(name = "proposer_id", nullable = false, updatable = false)
    private UUID proposerId;
    @Column(name = "responder_id", nullable = false, updatable = false)
    private UUID responderId;
    @Column(name = "scheduled_at", nullable = false, updatable = false)
    private Instant scheduledAt;
    @Column(nullable = false, updatable = false, length = 64)
    private String timezone;
    @Enumerated(EnumType.STRING)
    @Column(name = "selection_type", nullable = false, updatable = false, length = 16)
    private DateSelectionType selectionType;
    @Column(name = "place_id", updatable = false)
    private UUID placeId;
    @Column(name = "place_name_snapshot", nullable = false, updatable = false, length = 300)
    private String placeNameSnapshot;
    @Column(name = "place_address_snapshot", nullable = false, updatable = false, length = 500)
    private String placeAddressSnapshot;
    @Column(name = "place_cover_media_id_snapshot", updatable = false)
    private UUID placeCoverMediaIdSnapshot;
    @Column(name = "event_id", updatable = false)
    private UUID eventId;
    @Column(name = "event_occurrence_id", updatable = false)
    private UUID eventOccurrenceId;
    @Column(name = "event_title_snapshot", updatable = false, length = 300)
    private String eventTitleSnapshot;
    @Column(name = "event_source_url_snapshot", updatable = false, length = 1000)
    private String eventSourceUrlSnapshot;
    @Column(name = "event_price_snapshot", updatable = false, length = 500)
    private String eventPriceSnapshot;
    @Column(updatable = false, length = 1000)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DateProposalStatus status;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "decided_at")
    private Instant decidedAt;
    @Column(name = "cancelled_at")
    private Instant cancelledAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "draft_expires_at")
    private Instant draftExpiresAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected DateProposal() {
    }

    public DateProposal(
            UUID coupleId,
            UUID proposerId,
            UUID responderId,
            Instant scheduledAt,
            UUID placeId,
            String placeNameSnapshot,
            String placeAddressSnapshot,
            UUID placeCoverMediaIdSnapshot,
            String description,
            Instant now
    ) {
        this.id = UUID.randomUUID();
        this.coupleId = Objects.requireNonNull(coupleId);
        this.proposerId = Objects.requireNonNull(proposerId);
        this.responderId = Objects.requireNonNull(responderId);
        if (proposerId.equals(responderId)) {
            throw new IllegalArgumentException("proposer and responder must be different users");
        }
        this.scheduledAt = Objects.requireNonNull(scheduledAt);
        this.timezone = DEFAULT_TIMEZONE;
        this.selectionType = DateSelectionType.PLACE;
        this.placeId = Objects.requireNonNull(placeId);
        this.placeNameSnapshot = requireText(placeNameSnapshot, 300, "placeNameSnapshot");
        this.placeAddressSnapshot = requireText(placeAddressSnapshot, 500, "placeAddressSnapshot");
        this.placeCoverMediaIdSnapshot = placeCoverMediaIdSnapshot;
        this.description = normalizeDescription(description);
        this.status = DateProposalStatus.PENDING_CONFIRMATION;
        this.createdAt = Objects.requireNonNull(now);
    }

    public static DateProposal forEvent(
            UUID coupleId,
            UUID proposerId,
            UUID responderId,
            Instant scheduledAt,
            UUID localPlaceId,
            String venueName,
            String venueAddress,
            UUID coverMediaId,
            UUID eventId,
            UUID eventOccurrenceId,
            String eventTitle,
            String eventSourceUrl,
            String eventPrice,
            String description,
            Instant now) {
        DateProposal proposal = new DateProposal();
        proposal.id = UUID.randomUUID();
        proposal.coupleId = Objects.requireNonNull(coupleId);
        proposal.proposerId = Objects.requireNonNull(proposerId);
        proposal.responderId = Objects.requireNonNull(responderId);
        if (proposerId.equals(responderId)) throw new IllegalArgumentException("proposer and responder must be different users");
        proposal.scheduledAt = Objects.requireNonNull(scheduledAt);
        proposal.timezone = DEFAULT_TIMEZONE;
        proposal.selectionType = DateSelectionType.EVENT;
        proposal.placeId = localPlaceId;
        proposal.placeNameSnapshot = requireText(venueName, 300, "venueName");
        proposal.placeAddressSnapshot = requireText(venueAddress, 500, "venueAddress");
        proposal.placeCoverMediaIdSnapshot = coverMediaId;
        proposal.eventId = Objects.requireNonNull(eventId);
        proposal.eventOccurrenceId = Objects.requireNonNull(eventOccurrenceId);
        proposal.eventTitleSnapshot = requireText(eventTitle, 300, "eventTitle");
        proposal.eventSourceUrlSnapshot = requireText(eventSourceUrl, 1000, "eventSourceUrl");
        proposal.eventPriceSnapshot = eventPrice == null ? null : requireText(eventPrice, 500, "eventPrice");
        proposal.description = normalizeDescription(description);
        proposal.status = DateProposalStatus.PENDING_CONFIRMATION;
        proposal.createdAt = Objects.requireNonNull(now);
        return proposal;
    }

    public static DateProposal privateDraft(
            UUID coupleId,
            UUID proposerId,
            UUID responderId,
            Instant scheduledAt,
            String placeName,
            String placeAddress,
            String description,
            Instant expiresAt,
            Instant now) {
        DateProposal proposal = new DateProposal();
        proposal.id = UUID.randomUUID();
        proposal.coupleId = Objects.requireNonNull(coupleId);
        proposal.proposerId = Objects.requireNonNull(proposerId);
        proposal.responderId = Objects.requireNonNull(responderId);
        if (proposerId.equals(responderId)) {
            throw new IllegalArgumentException("proposer and responder must be different users");
        }
        proposal.scheduledAt = Objects.requireNonNull(scheduledAt);
        proposal.timezone = DEFAULT_TIMEZONE;
        proposal.selectionType = DateSelectionType.PRIVATE_PLACE;
        proposal.placeNameSnapshot = requireText(placeName, 300, "placeName");
        proposal.placeAddressSnapshot = normalizeAddress(placeAddress);
        proposal.description = normalizeDescription(description);
        proposal.status = DateProposalStatus.DRAFT;
        proposal.draftExpiresAt = Objects.requireNonNull(expiresAt);
        proposal.createdAt = Objects.requireNonNull(now);
        return proposal;
    }

    public void send(UUID actorId, Instant now) {
        if (!proposerId.equals(actorId)) {
            throw new InvalidDateProposalActionException("Only proposer can send a draft");
        }
        if (status != DateProposalStatus.DRAFT) {
            throw new InvalidDateProposalActionException("Only a draft can be sent");
        }
        if (!scheduledAt.isAfter(now) || !draftExpiresAt.isAfter(now)) {
            throw new InvalidDateProposalActionException("Draft is expired");
        }
        status = DateProposalStatus.PENDING_CONFIRMATION;
        draftExpiresAt = null;
    }

    public void updateDraftCover(UUID coverMediaId) {
        if (status == DateProposalStatus.DRAFT) {
            placeCoverMediaIdSnapshot = coverMediaId;
        }
    }

    public void expireDraft(Instant now) {
        if (status == DateProposalStatus.DRAFT) {
            status = DateProposalStatus.CANCELLED;
            cancelledAt = now;
        }
    }

    public void accept(UUID actorId, Instant now) {
        requireResponder(actorId);
        requirePending();
        status = DateProposalStatus.ACCEPTED;
        decidedAt = Objects.requireNonNull(now);
    }

    public void decline(UUID actorId, Instant now) {
        requireResponder(actorId);
        requirePending();
        status = DateProposalStatus.DECLINED;
        decidedAt = Objects.requireNonNull(now);
    }

    public void cancel(UUID actorId, Instant now) {
        if (status == DateProposalStatus.PENDING_CONFIRMATION && !proposerId.equals(actorId)) {
            throw new InvalidDateProposalActionException("Only proposer can cancel a pending proposal");
        }
        if (status == DateProposalStatus.ACCEPTED
                && !proposerId.equals(actorId)
                && !responderId.equals(actorId)) {
            throw new InvalidDateProposalActionException("Only a couple member can cancel an accepted proposal");
        }
        if (status != DateProposalStatus.PENDING_CONFIRMATION && status != DateProposalStatus.ACCEPTED) {
            throw new InvalidDateProposalActionException("Proposal cannot be cancelled in its current state");
        }
        status = DateProposalStatus.CANCELLED;
        cancelledAt = Objects.requireNonNull(now);
    }

    private void requireResponder(UUID actorId) {
        if (!responderId.equals(actorId)) {
            throw new InvalidDateProposalActionException("Only responder can decide the proposal");
        }
    }

    private void requirePending() {
        if (status != DateProposalStatus.PENDING_CONFIRMATION) {
            throw new InvalidDateProposalActionException("Proposal is not awaiting confirmation");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getCoupleId() {
        return coupleId;
    }

    public UUID getProposerId() {
        return proposerId;
    }

    public UUID getResponderId() {
        return responderId;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public String getTimezone() {
        return timezone;
    }

    public DateSelectionType getSelectionType() { return selectionType; }

    public UUID getPlaceId() {
        return placeId;
    }

    public String getPlaceNameSnapshot() {
        return placeNameSnapshot;
    }

    public String getPlaceAddressSnapshot() {
        return placeAddressSnapshot;
    }

    public UUID getPlaceCoverMediaIdSnapshot() {
        return placeCoverMediaIdSnapshot;
    }

    public UUID getEventId() { return eventId; }
    public UUID getEventOccurrenceId() { return eventOccurrenceId; }
    public String getEventTitleSnapshot() { return eventTitleSnapshot; }
    public String getEventSourceUrlSnapshot() { return eventSourceUrlSnapshot; }
    public String getEventPriceSnapshot() { return eventPriceSnapshot; }

    public String getDescription() {
        return description;
    }

    public DateProposalStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public long getVersion() {
        return version;
    }

    public Instant getDraftExpiresAt() {
        return draftExpiresAt;
    }

    private static String requireText(String value, int maxLength, String field) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }

    private static String normalizeDescription(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 1000) {
            throw new IllegalArgumentException("description is too long");
        }
        return normalized;
    }

    private static String normalizeAddress(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("placeAddress is too long");
        }
        return normalized;
    }
}
