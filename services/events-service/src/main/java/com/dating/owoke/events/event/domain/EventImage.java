package com.dating.owoke.events.event.domain;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

import com.dating.owoke.events.sync.dto.ExternalImageData;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_images")
public class EventImage {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private CatalogEvent event;
    @Column(name = "provider_asset_key", nullable = false, length = 128)
    private String providerAssetKey;
    @Column(name = "remote_url", nullable = false, length = 1000)
    private String remoteUrl;
    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;
    @Column(name = "source_name", length = 200)
    private String sourceName;
    @Column(name = "source_link", length = 1000)
    private String sourceLink;
    @Column(nullable = false)
    private int position;

    protected EventImage() {
    }

    EventImage(CatalogEvent event, ExternalImageData data, int position) {
        this.event = Objects.requireNonNull(event);
        this.providerAssetKey = Objects.requireNonNull(data.providerAssetKey());
        this.id = UUID.nameUUIDFromBytes(("kudago-image:" + event.getExternalId() + ':' + providerAssetKey)
                .getBytes(StandardCharsets.UTF_8));
        this.remoteUrl = Objects.requireNonNull(data.remoteUrl());
        this.thumbnailUrl = data.thumbnailUrl();
        this.sourceName = data.sourceName();
        this.sourceLink = data.sourceLink();
        this.position = position;
    }

    public String getProviderAssetKey() { return providerAssetKey; }
    public String getRemoteUrl() { return remoteUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getSourceName() { return sourceName; }
    public String getSourceLink() { return sourceLink; }
}
