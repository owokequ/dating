package com.dating.owoke.events.event.domain;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.dating.owoke.events.sync.dto.ExternalEventData;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "events")
public class CatalogEvent {
    @Id
    private UUID id;
    @Column(nullable = false, updatable = false, length = 32)
    private String source;
    @Column(name = "external_id", nullable = false, updatable = false, length = 128)
    private String externalId;
    @Column(nullable = false, length = 300)
    private String title;
    @Column(name = "provider_description", length = 4000)
    private String providerDescription;
    @Column(name = "description_override", length = 4000)
    private String descriptionOverride;
    @Column(nullable = false, length = 1000)
    private String categories;
    @Column(name = "price_text", length = 500)
    private String priceText;
    @Column(name = "is_free", nullable = false)
    private boolean free;
    @Column(name = "age_restriction", length = 32)
    private String ageRestriction;
    @Column(name = "source_page_url", nullable = false, length = 1000)
    private String sourcePageUrl;
    @Column(name = "venue_name", length = 300)
    private String venueName;
    @Column(name = "venue_address", length = 500)
    private String venueAddress;
    private Double latitude;
    private Double longitude;
    @Column(name = "venue_override", nullable = false)
    private boolean venueOverride;
    @Column(name = "kudago_place_id", length = 128)
    private String kudagoPlaceId;
    @Column(name = "local_place_id")
    private UUID localPlaceId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EventStatus status;
    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;
    @Column(name = "missing_since")
    private Instant missingSince;
    @Column(name = "missing_sync_count", nullable = false)
    private int missingSyncCount;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startsAt ASC")
    private List<EventOccurrence> occurrences = new ArrayList<>();
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<EventImage> images = new ArrayList<>();

    protected CatalogEvent() {
    }

    public static CatalogEvent imported(ExternalEventData data, Instant now) {
        CatalogEvent event = new CatalogEvent();
        event.source = "KUDAGO";
        event.externalId = requireText(data.externalId(), "externalId", 128);
        event.id = UUID.nameUUIDFromBytes(("kudago-event:" + event.externalId).getBytes(StandardCharsets.UTF_8));
        event.createdAt = Objects.requireNonNull(now);
        event.status = EventStatus.DRAFT;
        event.applyProvider(data, now);
        return event;
    }

    public boolean refresh(ExternalEventData data, Instant now) {
        String before = fingerprint();
        applyProvider(data, now);
        return !before.equals(fingerprint());
    }

    private void applyProvider(ExternalEventData data, Instant now) {
        title = requireText(data.title(), "title", 300);
        providerDescription = optionalText(data.description(), 4000);
        categories = normalizeCategories(data.categories());
        priceText = optionalText(data.priceText(), 500);
        free = data.free();
        ageRestriction = optionalText(data.ageRestriction(), 32);
        sourcePageUrl = requireKudaGoUrl(data.sourcePageUrl());
        kudagoPlaceId = optionalText(data.kudagoPlaceId(), 128);
        if (!venueOverride) {
            venueName = optionalText(data.venueName(), 300);
            venueAddress = optionalText(data.venueAddress(), 500);
            latitude = validLatitude(data.latitude());
            longitude = validLongitude(data.longitude());
        }
        replaceOccurrences(data, now);
        replaceImages(data);
        lastSeenAt = now;
        missingSince = null;
        missingSyncCount = 0;
        if (status != EventStatus.HIDDEN && status != EventStatus.ARCHIVED) {
            status = hasUsableVenue() && hasActiveOccurrences() ? EventStatus.ACTIVE : EventStatus.DRAFT;
        }
        updatedAt = now;
    }

    private void replaceOccurrences(ExternalEventData data, Instant now) {
        occurrences.clear();
        data.occurrences().stream().limit(200).forEach(item -> occurrences.add(new EventOccurrence(this, item, now)));
    }

    private void replaceImages(ExternalEventData data) {
        images.clear();
        for (int i = 0; i < Math.min(5, data.images().size()); i++) {
            images.add(new EventImage(this, data.images().get(i), i));
        }
    }

    public void updateVenue(String name, String address, Double latitude, Double longitude, Instant now) {
        venueName = requireText(name, "venueName", 300);
        venueAddress = requireText(address, "venueAddress", 500);
        this.latitude = validLatitude(Objects.requireNonNull(latitude, "latitude must not be null"));
        this.longitude = validLongitude(Objects.requireNonNull(longitude, "longitude must not be null"));
        venueOverride = true;
        updatedAt = now;
    }

    public void publish(Instant now) {
        if (!hasUsableVenue() || !hasActiveOccurrences()) {
            throw new IllegalStateException("Event requires a venue and a future occurrence before publication");
        }
        status = EventStatus.ACTIVE;
        updatedAt = now;
    }

    public void hide(Instant now) {
        if (status == EventStatus.ARCHIVED) throw new IllegalStateException("Archived event cannot be hidden");
        status = EventStatus.HIDDEN;
        updatedAt = now;
    }

    public void archive(Instant now) {
        status = EventStatus.ARCHIVED;
        updatedAt = now;
    }

    public boolean markMissing(Instant now) {
        if (status == EventStatus.ARCHIVED) return false;
        missingSyncCount++;
        if (missingSince == null) missingSince = now;
        if (missingSyncCount >= 2 && Duration.between(missingSince, now).compareTo(Duration.ofHours(24)) >= 0) {
            archive(now);
            return true;
        }
        return false;
    }

    public boolean expireOccurrences(Instant now) {
        occurrences.forEach(item -> item.refresh(now));
        if (status != EventStatus.ARCHIVED && !hasActiveOccurrences()) {
            archive(now);
            return true;
        }
        return false;
    }

    private boolean hasActiveOccurrences() {
        return occurrences.stream().anyMatch(item -> item.getStatus() == OccurrenceStatus.ACTIVE);
    }

    private boolean hasUsableVenue() {
        return venueName != null && venueAddress != null && latitude != null && longitude != null;
    }

    private String fingerprint() {
        return String.join("|", title, Objects.toString(providerDescription, ""), categories,
                Objects.toString(priceText, ""), Boolean.toString(free), Objects.toString(ageRestriction, ""),
                sourcePageUrl, Objects.toString(venueName, ""), Objects.toString(venueAddress, ""),
                Objects.toString(latitude, ""), Objects.toString(longitude, ""), status.name());
    }

    private static String normalizeCategories(List<String> input) {
        Set<String> values = input == null ? Set.of() : input.stream()
                .filter(Objects::nonNull).map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank()).collect(Collectors.toCollection(LinkedHashSet::new));
        return String.join(",", values);
    }

    private static String requireKudaGoUrl(String value) {
        String normalized = requireText(value, "sourcePageUrl", 1000).replaceFirst("^http://", "https://");
        java.net.URI uri = java.net.URI.create(normalized);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !(host.equals("kudago.com") || host.endsWith(".kudago.com"))) {
            throw new IllegalArgumentException("sourcePageUrl must point to kudago.com over HTTPS");
        }
        return normalized;
    }

    private static Double validLatitude(Double value) {
        if (value == null) return null;
        if (value < -90 || value > 90) throw new IllegalArgumentException("Invalid latitude");
        return value;
    }
    private static Double validLongitude(Double value) {
        if (value == null) return null;
        if (value < -180 || value > 180) throw new IllegalArgumentException("Invalid longitude");
        return value;
    }
    private static String requireText(String value, String field, int max) {
        String result = optionalText(value, max);
        if (result == null) throw new IllegalArgumentException(field + " must not be blank");
        return result;
    }
    private static String optionalText(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String result = value.trim().replaceAll("\\s+", " ");
        return result.substring(0, Math.min(max, result.length()));
    }

    public UUID getId() { return id; }
    public String getSource() { return source; }
    public String getExternalId() { return externalId; }
    public String getTitle() { return title; }
    public String getDescription() { return descriptionOverride == null ? providerDescription : descriptionOverride; }
    public String getProviderDescription() { return providerDescription; }
    public boolean isDescriptionOverridden() { return descriptionOverride != null; }
    public List<String> getCategories() { return categories.isBlank() ? List.of() : List.of(categories.split(",")); }
    public String getPriceText() { return priceText; }
    public boolean isFree() { return free; }
    public String getAgeRestriction() { return ageRestriction; }
    public String getSourcePageUrl() { return sourcePageUrl; }
    public String getVenueName() { return venueName; }
    public String getVenueAddress() { return venueAddress; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public boolean isVenueOverride() { return venueOverride; }
    public String getKudagoPlaceId() { return kudagoPlaceId; }
    public UUID getLocalPlaceId() { return localPlaceId; }
    public EventStatus getStatus() { return status; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
    public List<EventOccurrence> getOccurrences() { return List.copyOf(occurrences); }
    public List<EventImage> getImages() { return List.copyOf(images); }
}
