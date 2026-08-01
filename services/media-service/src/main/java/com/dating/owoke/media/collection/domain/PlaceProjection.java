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
@Table(name = "place_projections")
public class PlaceProjection {

    @Id
    @Column(name = "place_id")
    private UUID placeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PlaceProjectionStatus status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected PlaceProjection() {
    }

    public PlaceProjection(UUID placeId, PlaceProjectionStatus status, Instant updatedAt) {
        this.placeId = Objects.requireNonNull(placeId, "placeId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public void update(PlaceProjectionStatus status, Instant occurredAt) {
        if (occurredAt.isBefore(updatedAt)) {
            return;
        }
        this.status = Objects.requireNonNull(status, "status must not be null");
        updatedAt = occurredAt;
    }

    public boolean isActive() {
        return status == PlaceProjectionStatus.ACTIVE;
    }
}
