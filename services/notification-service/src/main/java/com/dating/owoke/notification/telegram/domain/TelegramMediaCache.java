package com.dating.owoke.notification.telegram.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "telegram_media_cache", uniqueConstraints = @UniqueConstraint(
        name = "uq_telegram_media_cache_asset_bot", columnNames = {"media_id", "content_hash", "bot_id"}))
public class TelegramMediaCache {
    @Id
    private UUID id;
    @Column(name = "media_id", nullable = false, updatable = false)
    private UUID mediaId;
    @Column(name = "content_hash", nullable = false, updatable = false, length = 64)
    private String contentHash;
    @Column(name = "bot_id", nullable = false, updatable = false, length = 32)
    private String botId;
    @Column(name = "file_id", nullable = false, updatable = false, length = 255)
    private String fileId;
    @Column(name = "file_unique_id", updatable = false, length = 255)
    private String fileUniqueId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TelegramMediaCache() {
    }

    public TelegramMediaCache(
            UUID mediaId, String contentHash, String botId,
            String fileId, String fileUniqueId, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.mediaId = mediaId;
        this.contentHash = contentHash;
        this.botId = botId;
        this.fileId = fileId;
        this.fileUniqueId = fileUniqueId;
        this.createdAt = createdAt;
    }

    public String getFileId() {
        return fileId;
    }

    public String getContentHash() {
        return contentHash;
    }
}
