package com.dating.owoke.dating.eventprojection.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_occurrence_projections")
public class EventOccurrenceProjection {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false) private EventProjection event;
    @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @Column(name = "ends_at") private Instant endsAt;
    @Column(nullable = false) private boolean continuous;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private OccurrenceProjectionStatus status;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected EventOccurrenceProjection() {}
    EventOccurrenceProjection(EventProjection event, UUID id, Instant startsAt, Instant endsAt,
            boolean continuous, OccurrenceProjectionStatus status, Instant updatedAt) {
        this.event = event; this.id = id; this.startsAt = startsAt; this.endsAt = endsAt;
        this.continuous = continuous; this.status = status; this.updatedAt = updatedAt;
    }
    public UUID getId() { return id; }
    public EventProjection getEvent() { return event; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public boolean isContinuous() { return continuous; }
    public boolean isActive() { return status == OccurrenceProjectionStatus.ACTIVE; }
}
