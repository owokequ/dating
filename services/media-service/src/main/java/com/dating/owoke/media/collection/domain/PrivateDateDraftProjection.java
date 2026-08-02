package com.dating.owoke.media.collection.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "private_date_draft_projections")
public class PrivateDateDraftProjection {

    @Id
    @Column(name = "proposal_id")
    private UUID proposalId;
    @Column(name = "proposer_id", nullable = false, updatable = false)
    private UUID proposerId;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected PrivateDateDraftProjection() {
    }

    public PrivateDateDraftProjection(UUID proposalId, UUID proposerId, Instant expiresAt) {
        this.proposalId = proposalId;
        this.proposerId = proposerId;
        this.expiresAt = expiresAt;
    }

    public boolean canUpload(UUID userId, Instant now) {
        return proposerId.equals(userId) && expiresAt.isAfter(now);
    }
}
