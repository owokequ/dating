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
@Table(name = "couples")
public class Couple {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CoupleStatus status;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Couple() {
    }

    private Couple(UUID ownerId, Instant now) {
        this.id = UUID.randomUUID();
        this.status = CoupleStatus.PENDING;
        this.createdBy = Objects.requireNonNull(ownerId, "ownerId must not be null");
        this.createdAt = Objects.requireNonNull(now, "now must not be null");
    }

    public static Couple pending(UUID ownerId, Instant now) {
        return new Couple(ownerId, now);
    }

    public void activate(Instant now) {
        if (status != CoupleStatus.PENDING) {
            throw new IllegalStateException("Only a pending couple can be activated");
        }
        status = CoupleStatus.ACTIVE;
        activatedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public void close(Instant now) {
        if (status == CoupleStatus.CLOSED) {
            throw new IllegalStateException("Couple is already closed");
        }
        status = CoupleStatus.CLOSED;
        closedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public UUID getId() {
        return id;
    }

    public CoupleStatus getStatus() {
        return status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public long getVersion() {
        return version;
    }
}
