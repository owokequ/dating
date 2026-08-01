package com.dating.owoke.places.media.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "place_media_collections")
public class PlaceMediaCollection {

    @Id
    @Column(name = "place_id")
    private UUID placeId;

    @Column(nullable = false)
    private long revision;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PlaceMediaCollection() {
    }

    public PlaceMediaCollection(UUID placeId, long revision, Instant updatedAt) {
        this.placeId = placeId;
        this.revision = revision;
        this.updatedAt = updatedAt;
    }

    public boolean updateIfNewer(long revision, Instant updatedAt) {
        if (revision <= this.revision) {
            return false;
        }
        this.revision = revision;
        this.updatedAt = updatedAt;
        return true;
    }

    public long getRevision() {
        return revision;
    }
}
