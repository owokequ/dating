package com.dating.owoke.notification.delivery.domain;

import java.time.Duration;
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
@Table(name = "delivery_attempts")
public class DeliveryAttempt {

    private static final int MAX_ATTEMPTS = 3;

    @Id
    private UUID id;

    @Column(name = "notification_id", nullable = false, updatable = false)
    private UUID notificationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 16)
    private DeliveryChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "provider_message_id", length = 128)
    private String providerMessageId;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected DeliveryAttempt() {
    }

    public DeliveryAttempt(UUID notificationId, DeliveryChannel channel, Instant now) {
        this.id = UUID.randomUUID();
        this.notificationId = Objects.requireNonNull(notificationId, "notificationId must not be null");
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.status = DeliveryStatus.PENDING;
        this.nextAttemptAt = Objects.requireNonNull(now, "now must not be null");
        this.createdAt = now;
    }

    public void markSent(String providerMessageId, Instant now) {
        attemptCount++;
        status = DeliveryStatus.SENT;
        this.providerMessageId = providerMessageId;
        completedAt = now;
        lastError = null;
    }

    public void markFailed(Exception exception, Instant now) {
        attemptCount++;
        lastError = truncate(exception.getMessage());
        if (attemptCount >= MAX_ATTEMPTS) {
            status = DeliveryStatus.FAILED;
            completedAt = now;
        } else {
            status = DeliveryStatus.PENDING;
            nextAttemptAt = now.plus(Duration.ofSeconds(1L << attemptCount));
        }
    }

    public void markProcessing(Instant now) {
        if (status != DeliveryStatus.PENDING && status != DeliveryStatus.PROCESSING) {
            throw new IllegalStateException("Only pending delivery can be claimed");
        }
        status = DeliveryStatus.PROCESSING;
        nextAttemptAt = now.plus(Duration.ofMinutes(1));
    }

    public UUID getId() {
        return id;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public DeliveryChannel getChannel() {
        return channel;
    }

    public boolean isFailedPermanently() {
        return status == DeliveryStatus.FAILED;
    }

    private static String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown delivery error";
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
