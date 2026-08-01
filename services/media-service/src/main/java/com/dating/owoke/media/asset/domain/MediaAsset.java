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
import jakarta.persistence.Version;

@Entity
@Table(name = "media_assets")
public class MediaAsset {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 32)
    private MediaAssetSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MediaAssetStatus status;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "detected_content_type", length = 100)
    private String detectedContentType;

    @Column(name = "original_size", nullable = false, updatable = false)
    private long originalSize;

    private Integer width;
    private Integer height;

    @Column(length = 64)
    private String sha256;

    @Column(name = "uploaded_by", nullable = false, updatable = false)
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "purge_after")
    private Instant purgeAfter;

    @Column(name = "purged_at")
    private Instant purgedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected MediaAsset() {
    }

    public MediaAsset(
            UUID id,
            MediaAssetSource source,
            String originalFilename,
            long originalSize,
            UUID uploadedBy,
            Instant now) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.originalFilename = truncate(originalFilename, 255);
        if (originalSize < 1) {
            throw new IllegalArgumentException("originalSize must be positive");
        }
        this.originalSize = originalSize;
        this.uploadedBy = Objects.requireNonNull(uploadedBy, "uploadedBy must not be null");
        this.createdAt = Objects.requireNonNull(now, "now must not be null");
        this.status = MediaAssetStatus.UPLOADED;
    }

    public void markProcessing() {
        if (status != MediaAssetStatus.UPLOADED) {
            throw new IllegalStateException("Only uploaded media can be processed");
        }
        status = MediaAssetStatus.PROCESSING;
    }

    public void markReady(String contentType, int width, int height, String sha256, Instant now) {
        if (status != MediaAssetStatus.PROCESSING) {
            throw new IllegalStateException("Only processing media can become ready");
        }
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Image dimensions must be positive");
        }
        this.detectedContentType = Objects.requireNonNull(contentType, "contentType must not be null");
        this.width = width;
        this.height = height;
        this.sha256 = Objects.requireNonNull(sha256, "sha256 must not be null");
        this.readyAt = Objects.requireNonNull(now, "now must not be null");
        status = MediaAssetStatus.READY;
    }

    public void markFailed() {
        if (status != MediaAssetStatus.PROCESSING) {
            throw new IllegalStateException("Only processing media can fail");
        }
        status = MediaAssetStatus.FAILED;
    }

    public void softDelete(Instant now, Instant purgeAfter) {
        if (status == MediaAssetStatus.DELETED) {
            return;
        }
        this.deletedAt = Objects.requireNonNull(now, "now must not be null");
        this.purgeAfter = Objects.requireNonNull(purgeAfter, "purgeAfter must not be null");
        status = MediaAssetStatus.DELETED;
    }

    public void markPurged(Instant now) {
        if (status != MediaAssetStatus.DELETED) {
            throw new IllegalStateException("Only deleted media can be purged");
        }
        purgedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public UUID getId() {
        return id;
    }

    public MediaAssetStatus getStatus() {
        return status;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public long getOriginalSize() {
        return originalSize;
    }

    public String getDetectedContentType() {
        return detectedContentType;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public String getSha256() {
        return sha256;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPurgeAfter() {
        return purgeAfter;
    }

    public Instant getPurgedAt() {
        return purgedAt;
    }

    private static String truncate(String value, int limit) {
        if (value == null) {
            return null;
        }
        return value.length() <= limit ? value : value.substring(0, limit);
    }
}
