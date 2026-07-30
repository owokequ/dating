package com.dating.owoke.dating.couple.domain;

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

@Entity
@Table(name = "couple_invitations")
public class CoupleInvitation {

    @Id
    private UUID id;

    @Column(name = "couple_id", nullable = false, updatable = false)
    private UUID coupleId;

    @Column(name = "inviter_id", nullable = false, updatable = false)
    private UUID inviterId;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InvitationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected CoupleInvitation() {
    }

    public CoupleInvitation(UUID coupleId, UUID inviterId, String tokenHash, Instant now, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.coupleId = Objects.requireNonNull(coupleId, "coupleId must not be null");
        this.inviterId = Objects.requireNonNull(inviterId, "inviterId must not be null");
        this.tokenHash = requireHash(tokenHash);
        this.status = InvitationStatus.PENDING;
        this.createdAt = Objects.requireNonNull(now, "now must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (!expiresAt.isAfter(now)) {
            throw new IllegalArgumentException("expiresAt must be after createdAt");
        }
    }

    public boolean isUsableAt(Instant now) {
        return status == InvitationStatus.PENDING && expiresAt.isAfter(now);
    }

    public void accept(Instant now) {
        if (!isUsableAt(now)) {
            throw new IllegalStateException("Invitation is unavailable");
        }
        status = InvitationStatus.ACCEPTED;
        acceptedAt = now;
    }

    public void revoke(Instant now) {
        if (status == InvitationStatus.PENDING) {
            status = InvitationStatus.REVOKED;
            revokedAt = Objects.requireNonNull(now, "now must not be null");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getCoupleId() {
        return coupleId;
    }

    public UUID getInviterId() {
        return inviterId;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    private static String requireHash(String value) {
        if (value == null || value.length() != 64) {
            throw new IllegalArgumentException("tokenHash must contain 64 characters");
        }
        return value;
    }
}
