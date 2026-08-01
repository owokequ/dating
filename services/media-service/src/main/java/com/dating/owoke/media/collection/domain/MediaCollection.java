package com.dating.owoke.media.collection.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "media_collections")
public class MediaCollection {

    @EmbeddedId
    private MediaCollectionId id;

    @Column(nullable = false)
    private long revision;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected MediaCollection() {
    }

    public MediaCollection(MediaOwnerType ownerType, UUID ownerId, Instant now) {
        this.id = new MediaCollectionId(ownerType, ownerId);
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

    public MediaOwnerType getOwnerType() {
        return id.getOwnerType();
    }

    public UUID getOwnerId() {
        return id.getOwnerId();
    }
}
