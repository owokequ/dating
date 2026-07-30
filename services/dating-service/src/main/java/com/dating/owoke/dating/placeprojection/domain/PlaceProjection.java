package com.dating.owoke.dating.placeprojection.domain;

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
@Table(name = "place_projections")
public class PlaceProjection {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PlaceProjectionStatus status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected PlaceProjection() {
    }

    public PlaceProjection(UUID id, String name, String address, PlaceProjectionStatus status, Instant now) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        update(name, address, status, now);
    }

    public void update(String name, String address, PlaceProjectionStatus status, Instant now) {
        this.name = requireText(name, "name", 200);
        this.address = requireText(address, "address", 500);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.updatedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public boolean isActive() {
        return status == PlaceProjectionStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank() || value.trim().length() > maxLength) {
            throw new IllegalArgumentException(field + " must contain 1-" + maxLength + " characters");
        }
        return value.trim();
    }
}
