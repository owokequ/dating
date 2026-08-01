package com.dating.owoke.dating.placeprojection.domain;

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
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PlaceProjectionStatus status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "cover_media_id")
    private UUID coverMediaId;

    @Column(name = "media_revision", nullable = false)
    private long mediaRevision;

    @Version
    @Column(nullable = false)
    private long version;

    protected PlaceProjection() {
    }

    public PlaceProjection(UUID id, String name, String address, PlaceProjectionStatus status, Instant now) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        update(name, address, status, now);
    }

    public void update(String name, String address, PlaceProjectionStatus status, Instant now) {
        this.name = requireText(name, "name", 200);
        this.address = requireText(address, "address", 500);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.updatedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public boolean updateIfNewer(String name, String address, PlaceProjectionStatus status, Instant eventOccurredAt) {
        Objects.requireNonNull(eventOccurredAt, "eventOccurredAt must not be null");
        if (!eventOccurredAt.isAfter(updatedAt)) {
            return false;
        }
        update(name, address, status, eventOccurredAt);
        return true;
    }

    public boolean isActive() {
        return status == PlaceProjectionStatus.ACTIVE;
    }

    public boolean updateMediaIfNewer(UUID coverMediaId, long mediaRevision) {
        if (mediaRevision <= this.mediaRevision) {
            return false;
        }
        this.coverMediaId = coverMediaId;
        this.mediaRevision = mediaRevision;
        return true;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public PlaceProjectionStatus getStatus() {
        return status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public UUID getCoverMediaId() {
        return coverMediaId;
    }

    public long getMediaRevision() {
        return mediaRevision;
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank() || value.trim().length() > maxLength) {
            throw new IllegalArgumentException(field + " must contain 1-" + maxLength + " characters");
        }
        return value.trim();
    }
}
