package com.dating.owoke.dating.shared.idempotency.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "idempotency_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_idempotency_user_operation_key",
                columnNames = {"user_id", "operation", "idempotency_key"}))
public class IdempotencyRecord {

    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(nullable = false, updatable = false, length = 64)
    private String operation;
    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 128)
    private String idempotencyKey;
    @Column(name = "request_hash", nullable = false, updatable = false, length = 64)
    private String requestHash;
    @Column(name = "resource_id", nullable = false, updatable = false)
    private UUID resourceId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(
            UUID userId,
            String operation,
            String idempotencyKey,
            String requestHash,
            UUID resourceId,
            Instant createdAt
    ) {
        this.id = UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId);
        this.operation = requireText(operation, 64, "operation");
        this.idempotencyKey = requireText(idempotencyKey, 128, "idempotencyKey");
        this.requestHash = requireText(requestHash, 64, "requestHash");
        this.resourceId = Objects.requireNonNull(resourceId);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public String getRequestHash() {
        return requestHash;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    private static String requireText(String value, int maxLength, String field) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }
}
