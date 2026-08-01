package com.dating.owoke.media.asset.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "media_variants")
public class MediaVariant {

    @Id
    private UUID id;

    @Column(name = "media_asset_id", nullable = false, updatable = false)
    private UUID mediaAssetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 32)
    private MediaVariantName variant;

    @Column(name = "object_key", nullable = false, updatable = false, length = 500)
    private String objectKey;

    @Column(name = "content_type", nullable = false, updatable = false, length = 100)
    private String contentType;

    @Column(nullable = false, updatable = false)
    private int width;

    @Column(nullable = false, updatable = false)
    private int height;

    @Column(nullable = false, updatable = false)
    private long size;

    @Column(nullable = false, updatable = false, length = 64)
    private String sha256;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MediaVariant() {
    }

    public MediaVariant(
            UUID mediaAssetId,
            MediaVariantName variant,
            String objectKey,
            String contentType,
            int width,
            int height,
            long size,
            String sha256,
            Instant now) {
        this.id = UUID.randomUUID();
        this.mediaAssetId = Objects.requireNonNull(mediaAssetId, "mediaAssetId must not be null");
        this.variant = Objects.requireNonNull(variant, "variant must not be null");
        this.objectKey = Objects.requireNonNull(objectKey, "objectKey must not be null");
        this.contentType = Objects.requireNonNull(contentType, "contentType must not be null");
        this.width = width;
        this.height = height;
        this.size = size;
        this.sha256 = Objects.requireNonNull(sha256, "sha256 must not be null");
        this.createdAt = Objects.requireNonNull(now, "now must not be null");
    }

    public UUID getMediaAssetId() {
        return mediaAssetId;
    }

    public MediaVariantName getVariant() {
        return variant;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getContentType() {
        return contentType;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getSize() {
        return size;
    }

    public String getSha256() {
        return sha256;
    }
}
