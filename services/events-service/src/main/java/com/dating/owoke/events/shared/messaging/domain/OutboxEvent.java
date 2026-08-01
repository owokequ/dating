package com.dating.owoke.events.shared.messaging.domain;

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
    @Id private UUID id;
    @Column(nullable = false, updatable = false, length = 128) private String topic;
    @Column(name = "event_key", nullable = false, updatable = false, length = 128) private String eventKey;
    @Column(name = "event_type", nullable = false, updatable = false, length = 128) private String eventType;
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb") private String payload;
    @Column(name = "occurred_at", nullable = false, updatable = false) private Instant occurredAt;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(nullable = false) private int attempts;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "last_error", length = 1000) private String lastError;

    protected OutboxEvent() {}
    public OutboxEvent(UUID id, String topic, String eventKey, String eventType, String payload, Instant now) {
        this.id = Objects.requireNonNull(id); this.topic = Objects.requireNonNull(topic);
        this.eventKey = Objects.requireNonNull(eventKey); this.eventType = Objects.requireNonNull(eventType);
        this.payload = Objects.requireNonNull(payload); this.occurredAt = Objects.requireNonNull(now);
        this.nextAttemptAt = now;
    }
    public void markPublished(Instant now) { publishedAt = now; lastError = null; }
    public void markFailed(Exception exception, Instant now) {
        attempts++;
        nextAttemptAt = now.plus(Math.min(300, 5L << Math.min(attempts - 1, 6)), ChronoUnit.SECONDS);
        String value = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        lastError = value.substring(0, Math.min(1000, value.length()));
    }
    public String getTopic() { return topic; }
    public String getEventKey() { return eventKey; }
    public String getPayload() { return payload; }
}
