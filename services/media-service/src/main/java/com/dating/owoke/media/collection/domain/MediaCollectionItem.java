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
@Table(name = "media_collection_items")
public class MediaCollectionItem {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, updatable = false, length = 32)
    private MediaOwnerType ownerType;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "media_asset_id", nullable = false, updatable = false)
    private UUID mediaAssetId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean cover;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected MediaCollectionItem() {
    }

    public MediaCollectionItem(
            MediaOwnerType ownerType,
            UUID ownerId,
            UUID mediaAssetId,
            int position,
            boolean cover,
            Instant now) {
        this.id = UUID.randomUUID();
        this.ownerType = Objects.requireNonNull(ownerType, "ownerType must not be null");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
        this.mediaAssetId = Objects.requireNonNull(mediaAssetId, "mediaAssetId must not be null");
        if (position < 0) {
            throw new IllegalArgumentException("position must not be negative");
        }
        this.position = position;
        this.cover = cover;
        this.createdAt = Objects.requireNonNull(now, "now must not be null");
    }

    public void reorder(int position, boolean cover) {
        if (position < 0) {
            throw new IllegalArgumentException("position must not be negative");
        }
        this.position = position;
        this.cover = cover;
    }

    public void delete(Instant now) {
        deletedAt = Objects.requireNonNull(now, "now must not be null");
        cover = false;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public MediaOwnerType getOwnerType() {
        return ownerType;
    }

    public UUID getMediaAssetId() {
        return mediaAssetId;
    }

    public int getPosition() {
        return position;
    }

    public boolean isCover() {
        return cover;
    }
}
