package com.dating.owoke.media.asset.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.dating.owoke.media.collection.domain.MediaOwnerType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "remote_media_suppressions")
public class RemoteMediaSuppression {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, updatable = false, length = 32)
    private MediaOwnerType ownerType;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false, updatable = false, length = 32)
    private String provider;

    @Column(name = "provider_asset_key", nullable = false, updatable = false, length = 128)
    private String providerAssetKey;

    @Column(name = "suppressed_at", nullable = false, updatable = false)
    private Instant suppressedAt;

    protected RemoteMediaSuppression() {
    }

    public RemoteMediaSuppression(
            MediaOwnerType ownerType,
            UUID ownerId,
            String provider,
            String providerAssetKey,
            Instant suppressedAt) {
        this.id = UUID.randomUUID();
        this.ownerType = Objects.requireNonNull(ownerType, "ownerType must not be null");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.providerAssetKey = Objects.requireNonNull(providerAssetKey, "providerAssetKey must not be null");
        this.suppressedAt = Objects.requireNonNull(suppressedAt, "suppressedAt must not be null");
    }
}
