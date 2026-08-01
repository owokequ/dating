package com.dating.owoke.places.media.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "place_media_projection_items")
public class PlaceMediaProjectionItem {

    @Id
    private UUID id;
    @Column(name = "place_id", nullable = false, updatable = false)
    private UUID placeId;
    @Column(name = "media_id", nullable = false, updatable = false)
    private UUID mediaId;
    @Column(nullable = false, updatable = false)
    private int position;
    @Column(nullable = false, updatable = false)
    private boolean cover;
    @Column(nullable = false, updatable = false, length = 16)
    private String source;
    @Column(name = "provider_asset_key", updatable = false, length = 256)
    private String providerAssetKey;
    @Column(name = "thumbnail_url", nullable = false, updatable = false, length = 2000)
    private String thumbnailUrl;
    @Column(name = "card_url", nullable = false, updatable = false, length = 2000)
    private String cardUrl;
    @Column(name = "detail_url", nullable = false, updatable = false, length = 2000)
    private String detailUrl;
    @Column(name = "source_name", updatable = false, length = 200)
    private String sourceName;
    @Column(name = "source_link", updatable = false, length = 1000)
    private String sourceLink;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PlaceMediaProjectionItem() {
    }

    public PlaceMediaProjectionItem(
            UUID placeId,
            UUID mediaId,
            int position,
            boolean cover,
            String source,
            String providerAssetKey,
            String thumbnailUrl,
            String cardUrl,
            String detailUrl,
            String sourceName,
            String sourceLink,
            Instant now) {
        this.id = UUID.randomUUID();
        this.placeId = placeId;
        this.mediaId = mediaId;
        this.position = position;
        this.cover = cover;
        this.source = source;
        this.providerAssetKey = providerAssetKey;
        this.thumbnailUrl = thumbnailUrl;
        this.cardUrl = cardUrl;
        this.detailUrl = detailUrl;
        this.sourceName = sourceName;
        this.sourceLink = sourceLink;
        this.createdAt = now;
    }

    public UUID getPlaceId() {
        return placeId;
    }

    public UUID getMediaId() {
        return mediaId;
    }

    public int getPosition() {
        return position;
    }

    public boolean isCover() {
        return cover;
    }

    public String getSource() {
        return source;
    }

    public String getProviderAssetKey() {
        return providerAssetKey;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getCardUrl() {
        return cardUrl;
    }

    public String getDetailUrl() {
        return detailUrl;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getSourceLink() {
        return sourceLink;
    }
}
