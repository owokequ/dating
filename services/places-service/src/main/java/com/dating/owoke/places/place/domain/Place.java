package com.dating.owoke.places.place.domain;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
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
@Table(name = "places")
public class Place {

    public static final String CITY_CODE = "KZN";

    @Id
    private UUID id;

    @Column(name = "city_code", nullable = false, updatable = false, length = 8)
    private String cityCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 200)
    private String normalizedName;

    @Column(name = "provider_description", length = 2000)
    private String providerDescription;

    @Column(name = "description_override", length = 2000)
    private String descriptionOverride;

    @Column(nullable = false, length = 64)
    private String category;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(name = "normalized_address", nullable = false, length = 500)
    private String normalizedAddress;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "price_level")
    private Integer priceLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 16)
    private PlaceSource source;

    @Column(name = "external_id", updatable = false, length = 128)
    private String externalId;

    @Column(name = "source_page_url", length = 1000)
    private String sourcePageUrl;

    @Column(name = "provider_media_fingerprint", length = 64)
    private String providerMediaFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PlaceStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Place() {
    }

    private Place(
            PlaceSource source,
            String externalId,
            String name,
            String providerDescription,
            String descriptionOverride,
            String category,
            String address,
            double latitude,
            double longitude,
            Integer priceLevel,
            String sourcePageUrl,
            String providerMediaFingerprint,
            PlaceStatus status,
            Instant now) {
        this.id = UUID.randomUUID();
        this.cityCode = CITY_CODE;
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.externalId = normalizeExternalId(source, externalId);
        this.providerDescription = optionalText(providerDescription, 2000);
        this.descriptionOverride = optionalText(descriptionOverride, 2000);
        this.sourcePageUrl = normalizeSourcePageUrl(source, sourcePageUrl);
        this.providerMediaFingerprint = providerMediaFingerprint;
        applyProviderDetails(name, category, address, latitude, longitude);
        applyAdminDetails(priceLevel);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(now, "now must not be null");
        this.updatedAt = now;
    }

    public static Place manual(
            String name,
            String description,
            String category,
            String address,
            double latitude,
            double longitude,
            Integer priceLevel,
            Instant now) {
        return new Place(
                PlaceSource.MANUAL, null, name, null, description, category, address, latitude, longitude, priceLevel,
                null, null,
                PlaceStatus.ACTIVE, now);
    }

    public static Place external(
            PlaceSource source,
            String externalId,
            String name,
            String providerDescription,
            String category,
            String address,
            double latitude,
            double longitude,
            String sourcePageUrl,
            String providerMediaFingerprint,
            Instant now) {
        if (!source.isExternal()) {
            throw new IllegalArgumentException("External place must have an external source");
        }
        return new Place(
                source, externalId, name, providerDescription, null, category, address, latitude, longitude, null,
                sourcePageUrl, providerMediaFingerprint,
                PlaceStatus.DRAFT, now);
    }

    public boolean update(
            String name,
            String description,
            String category,
            String address,
            double latitude,
            double longitude,
            Integer priceLevel,
            PlaceStatus status,
            Instant now) {
        String oldFingerprint = fingerprint();
        if (source.isExternal()) {
            rejectExternalFieldChanges(name, category, address, latitude, longitude);
            String requestedDescription = optionalText(description, 2000);
            this.descriptionOverride = descriptionOverride == null
                    && Objects.equals(requestedDescription, providerDescription)
                    ? null
                    : requestedDescription;
            applyAdminDetails(priceLevel);
        } else {
            this.descriptionOverride = optionalText(description, 2000);
            applyProviderDetails(name, category, address, latitude, longitude);
            applyAdminDetails(priceLevel);
        }
        this.status = Objects.requireNonNull(status, "status must not be null");
        boolean changed = !oldFingerprint.equals(fingerprint());
        if (changed) {
            this.updatedAt = Objects.requireNonNull(now, "now must not be null");
        }
        return changed;
    }

    public boolean refreshExternal(
            String name,
            String providerDescription,
            String category,
            String address,
            double latitude,
            double longitude,
            String sourcePageUrl,
            String providerMediaFingerprint,
            Instant now) {
        if (!source.isExternal()) {
            throw new IllegalStateException("Only an external place can be refreshed from the provider");
        }
        String oldFingerprint = fingerprint();
        this.providerDescription = optionalText(providerDescription, 2000);
        this.sourcePageUrl = normalizeSourcePageUrl(source, sourcePageUrl);
        this.providerMediaFingerprint = providerMediaFingerprint;
        applyProviderDetails(name, category, address, latitude, longitude);
        boolean changed = !oldFingerprint.equals(fingerprint());
        if (changed) {
            updatedAt = Objects.requireNonNull(now, "now must not be null");
        }
        return changed;
    }

    public UUID getId() {
        return id;
    }

    public String getCityCode() {
        return cityCode;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return descriptionOverride != null ? descriptionOverride : providerDescription;
    }

    public String getProviderDescription() {
        return providerDescription;
    }

    public String getDescriptionOverride() {
        return descriptionOverride;
    }

    public boolean isDescriptionOverridden() {
        return descriptionOverride != null;
    }

    public String getCategory() {
        return category;
    }

    public String getAddress() {
        return address;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Integer getPriceLevel() {
        return priceLevel;
    }

    public PlaceSource getSource() {
        return source;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getSourcePageUrl() {
        return sourcePageUrl;
    }

    public PlaceStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private void applyProviderDetails(
            String name,
            String category,
            String address,
            double latitude,
            double longitude) {
        this.name = requireText(name, "name", 200);
        this.normalizedName = normalize(this.name);
        this.category = requireText(category, "category", 64).toUpperCase(Locale.ROOT);
        this.address = requireText(address, "address", 500);
        this.normalizedAddress = normalize(this.address);
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Coordinates are outside valid range");
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    private void applyAdminDetails(Integer priceLevel) {
        if (priceLevel != null && (priceLevel < 1 || priceLevel > 4)) {
            throw new IllegalArgumentException("priceLevel must be between 1 and 4");
        }
        this.priceLevel = priceLevel;
    }

    private String fingerprint() {
        return String.join("|",
                name,
                Objects.toString(providerDescription, ""),
                Objects.toString(descriptionOverride, ""),
                category,
                address,
                Double.toString(latitude),
                Double.toString(longitude),
                Objects.toString(priceLevel, ""),
                Objects.toString(sourcePageUrl, ""),
                Objects.toString(providerMediaFingerprint, ""),
                status == null ? "" : status.name());
    }

    private void rejectExternalFieldChanges(
            String name,
            String category,
            String address,
            double latitude,
            double longitude) {
        boolean changed = !Objects.equals(normalize(name), normalize(this.name))
                || !Objects.equals(normalize(address), normalize(this.address))
                || category == null
                || !category.trim().toUpperCase(Locale.ROOT).equals(this.category)
                || Double.compare(latitude, this.latitude) != 0
                || Double.compare(longitude, this.longitude) != 0;
        if (changed) {
            throw new IllegalArgumentException("Provider-owned name, category, address and coordinates cannot be edited");
        }
    }

    private static String normalizeExternalId(PlaceSource source, String externalId) {
        if (source == PlaceSource.MANUAL) {
            if (externalId != null) {
                throw new IllegalArgumentException("Manual place cannot have externalId");
            }
            return null;
        }
        return requireText(externalId, "externalId", 128);
    }

    private static String normalizeSourcePageUrl(PlaceSource source, String sourcePageUrl) {
        String normalized = optionalText(sourcePageUrl, 1000);
        if (source == PlaceSource.KUDAGO && normalized == null) {
            throw new IllegalArgumentException("KudaGo place must have sourcePageUrl");
        }
        if (normalized != null && !normalized.startsWith("https://")) {
            throw new IllegalArgumentException("sourcePageUrl must use HTTPS");
        }
        return normalized;
    }

    private static String requireText(String value, String name, int maxLength) {
        if (value == null || value.isBlank() || value.trim().length() > maxLength) {
            throw new IllegalArgumentException(name + " must contain 1-" + maxLength + " characters");
        }
        return value.trim();
    }

    private static String optionalText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("value is too long");
        }
        return normalized;
    }
}
