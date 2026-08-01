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
@Table(name = "media_collections")
public class MediaCollection {

    @Id
    @Column(name = "owner_id")
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, updatable = false, length = 32)
    private MediaOwnerType ownerType;

    @Column(nullable = false)
    private long revision;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected MediaCollection() {
    }

    public MediaCollection(UUID ownerId, Instant now) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
        this.ownerType = MediaOwnerType.PLACE;
        this.updatedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public long changed(Instant now) {
        revision++;
        updatedAt = Objects.requireNonNull(now, "now must not be null");
        return revision;
    }

    public long getRevision() {
        return revision;
    }
}
