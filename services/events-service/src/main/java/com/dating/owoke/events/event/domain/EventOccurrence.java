package com.dating.owoke.events.event.domain;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.dating.owoke.events.sync.dto.ExternalOccurrenceData;

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
@Table(name = "event_occurrences")
public class EventOccurrence {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private CatalogEvent event;

    @Column(name = "provider_occurrence_key", nullable = false, length = 256)
    private String providerOccurrenceKey;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(nullable = false)
    private boolean continuous;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OccurrenceStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EventOccurrence() {
    }

    EventOccurrence(CatalogEvent event, ExternalOccurrenceData data, Instant now) {
        this.event = Objects.requireNonNull(event);
        this.providerOccurrenceKey = Objects.requireNonNull(data.providerOccurrenceKey());
        this.id = stableId(event.getExternalId(), providerOccurrenceKey);
        this.startsAt = Objects.requireNonNull(data.startsAt());
        this.endsAt = data.endsAt();
        if (endsAt != null && endsAt.isBefore(startsAt)) {
            throw new IllegalArgumentException("Occurrence end cannot precede start");
        }
        this.continuous = data.continuous();
        this.status = isPast(now) ? OccurrenceStatus.EXPIRED : OccurrenceStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    void refresh(Instant now) {
        status = isPast(now) ? OccurrenceStatus.EXPIRED : OccurrenceStatus.ACTIVE;
        updatedAt = now;
    }

    private boolean isPast(Instant now) {
        Instant boundary = endsAt == null ? startsAt : endsAt;
        return !boundary.isAfter(now);
    }

    private static UUID stableId(String eventExternalId, String key) {
        return UUID.nameUUIDFromBytes(("kudago-occurrence:" + eventExternalId + ':' + key)
                .getBytes(StandardCharsets.UTF_8));
    }

    public UUID getId() { return id; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public boolean isContinuous() { return continuous; }
    public OccurrenceStatus getStatus() { return status; }
}
