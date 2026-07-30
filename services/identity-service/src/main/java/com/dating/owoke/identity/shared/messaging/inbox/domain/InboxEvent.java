package com.dating.owoke.identity.shared.messaging.inbox.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inbox_events")
public class InboxEvent {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 128)
    private String eventType;

    @Column(nullable = false, updatable = false, length = 128)
    private String topic;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    protected InboxEvent() {
    }

    public InboxEvent(UUID eventId, String eventType, String topic, Instant receivedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.topic = topic;
        this.receivedAt = receivedAt;
    }
}
