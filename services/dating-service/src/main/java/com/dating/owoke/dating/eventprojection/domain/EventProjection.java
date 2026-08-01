package com.dating.owoke.dating.eventprojection.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "event_projections")
public class EventProjection {
    @Id private UUID id;
    @Column(nullable = false, length = 300) private String title;
    @Column(length = 4000) private String description;
    @Column(name = "price_text", length = 500) private String priceText;
    @Column(name = "source_page_url", nullable = false, length = 1000) private String sourcePageUrl;
    @Column(name = "venue_name", length = 300) private String venueName;
    @Column(name = "venue_address", length = 500) private String venueAddress;
    private Double latitude;
    private Double longitude;
    @Column(name = "local_place_id") private UUID localPlaceId;
    @Column(name = "cover_media_id") private UUID coverMediaId;
    @Column(name = "media_version", nullable = false) private long mediaVersion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private EventProjectionStatus status;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private long version;
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventOccurrenceProjection> occurrences = new ArrayList<>();
    protected EventProjection() {}

    public EventProjection(UUID id) { this.id = id; }
    public void replace(String title, String description, String priceText, String sourcePageUrl,
            String venueName, String venueAddress, Double latitude, Double longitude, UUID localPlaceId,
            EventProjectionStatus status, List<OccurrenceData> items, Instant now) {
        this.title = title; this.description = description; this.priceText = priceText; this.sourcePageUrl = sourcePageUrl;
        this.venueName = venueName; this.venueAddress = venueAddress; this.latitude = latitude; this.longitude = longitude;
        this.localPlaceId = localPlaceId; this.status = status; this.updatedAt = now;
        occurrences.clear();
        items.forEach(item -> occurrences.add(new EventOccurrenceProjection(this, item.id(), item.startsAt(), item.endsAt(),
                item.continuous(), item.status(), now)));
    }
    public void updateMedia(UUID coverMediaId, long collectionVersion) {
        if (collectionVersion < mediaVersion) return;
        this.coverMediaId = coverMediaId; this.mediaVersion = collectionVersion;
    }
    public boolean isActive() { return status == EventProjectionStatus.ACTIVE; }
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getPriceText() { return priceText; }
    public String getSourcePageUrl() { return sourcePageUrl; }
    public String getVenueName() { return venueName; }
    public String getVenueAddress() { return venueAddress; }
    public UUID getLocalPlaceId() { return localPlaceId; }
    public UUID getCoverMediaId() { return coverMediaId; }
    public List<EventOccurrenceProjection> getOccurrences() { return List.copyOf(occurrences); }
    public record OccurrenceData(UUID id, Instant startsAt, Instant endsAt, boolean continuous,
                                 OccurrenceProjectionStatus status) {}
}
