package com.dating.owoke.identity.shared.messaging.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.ColumnTransformer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(nullable = false, length = 128)
    private String topic;

    @Column(name = "event_key", nullable = false, length = 128)
    private String eventKey;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String payload;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    protected OutboxEvent() {
    }

    public OutboxEvent(UUID id, String topic, String eventKey, String eventType, String payload, Instant occurredAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.topic = requireText(topic, "topic");
        this.eventKey = requireText(eventKey, "eventKey");
        this.eventType = requireText(eventType, "eventType");
        this.payload = requireText(payload, "payload");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.nextAttemptAt = occurredAt;
    }

    public void markPublished(Instant now) {
        publishedAt = Objects.requireNonNull(now, "now must not be null");
        lastError = null;
    }

    public void markFailed(Exception exception, Instant now) {
        attempts++;
        long delaySeconds = Math.min(300, 5L << Math.min(attempts - 1, 6));
        nextAttemptAt = now.plus(delaySeconds, ChronoUnit.SECONDS);
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        lastError = message.substring(0, Math.min(message.length(), 1000));
    }

    public UUID getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getEventKey() {
        return eventKey;
    }

    public String getPayload() {
        return payload;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
