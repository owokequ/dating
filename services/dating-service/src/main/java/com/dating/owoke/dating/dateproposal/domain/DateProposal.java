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
    @Column(name = "place_id", nullable = false, updatable = false)
    private UUID placeId;
    @Column(name = "place_name_snapshot", nullable = false, updatable = false, length = 200)
    private String placeNameSnapshot;
    @Column(name = "place_address_snapshot", nullable = false, updatable = false, length = 500)
    private String placeAddressSnapshot;
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
        this.placeId = Objects.requireNonNull(placeId);
        this.placeNameSnapshot = requireText(placeNameSnapshot, 200, "placeNameSnapshot");
        this.placeAddressSnapshot = requireText(placeAddressSnapshot, 500, "placeAddressSnapshot");
        this.description = normalizeDescription(description);
        this.status = DateProposalStatus.PENDING_CONFIRMATION;
        this.createdAt = Objects.requireNonNull(now);
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

    public UUID getPlaceId() {
        return placeId;
    }

    public String getPlaceNameSnapshot() {
        return placeNameSnapshot;
    }

    public String getPlaceAddressSnapshot() {
        return placeAddressSnapshot;
    }

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
}
