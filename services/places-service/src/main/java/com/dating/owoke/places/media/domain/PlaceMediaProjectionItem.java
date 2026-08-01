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
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PlaceMediaProjectionItem() {
    }

    public PlaceMediaProjectionItem(UUID placeId, UUID mediaId, int position, boolean cover, Instant now) {
        this.id = UUID.randomUUID();
        this.placeId = placeId;
        this.mediaId = mediaId;
        this.position = position;
        this.cover = cover;
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
}
