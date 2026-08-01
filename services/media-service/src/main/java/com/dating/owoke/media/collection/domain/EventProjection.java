package com.dating.owoke.media.collection.domain;

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
@Table(name = "event_projections")
public class EventProjection {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EventProjectionStatus status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected EventProjection() {
    }

    public EventProjection(UUID eventId, EventProjectionStatus status, Instant updatedAt) {
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public void update(EventProjectionStatus status, Instant occurredAt) {
        if (occurredAt.isBefore(updatedAt)) {
            return;
        }
        this.status = Objects.requireNonNull(status, "status must not be null");
        updatedAt = occurredAt;
    }

    public boolean isActive() {
        return status == EventProjectionStatus.ACTIVE;
    }

    public boolean acceptsUploads() {
        return status == EventProjectionStatus.DRAFT || status == EventProjectionStatus.ACTIVE;
    }
}
