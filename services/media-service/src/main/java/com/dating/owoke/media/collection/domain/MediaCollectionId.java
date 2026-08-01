package com.dating.owoke.media.collection.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class MediaCollectionId implements Serializable {

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, updatable = false, length = 32)
    private MediaOwnerType ownerType;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    protected MediaCollectionId() {
    }

    public MediaCollectionId(MediaOwnerType ownerType, UUID ownerId) {
        this.ownerType = Objects.requireNonNull(ownerType, "ownerType must not be null");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
    }

    public MediaOwnerType getOwnerType() {
        return ownerType;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MediaCollectionId that)) {
            return false;
        }
        return ownerType == that.ownerType && ownerId.equals(that.ownerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerType, ownerId);
    }
}
