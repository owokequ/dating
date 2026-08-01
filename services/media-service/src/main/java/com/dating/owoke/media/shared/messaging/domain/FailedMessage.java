package com.dating.owoke.media.shared.messaging.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "failed_messages")
public class FailedMessage {

    @Id
    private UUID id;
    @Column(name = "event_id")
    private UUID eventId;
    @Column(nullable = false, updatable = false, length = 128)
    private String topic;
    @Column(name = "event_type", length = 128)
    private String eventType;
    @Column(nullable = false, updatable = false)
    private String payload;
    @Column(name = "error_message", nullable = false, updatable = false, length = 1000)
    private String errorMessage;
    @Column(name = "failed_at", nullable = false, updatable = false)
    private Instant failedAt;

    protected FailedMessage() {
    }

    public FailedMessage(UUID eventId, String topic, String eventType, String payload, String error, Instant now) {
        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.topic = topic;
        this.eventType = eventType;
        this.payload = payload;
        this.errorMessage = error;
        this.failedAt = now;
    }
}
