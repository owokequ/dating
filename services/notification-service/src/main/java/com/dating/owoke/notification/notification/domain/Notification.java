package com.dating.owoke.notification.notification.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "source_event_id", nullable = false, updatable = false)
    private UUID sourceEventId;

    @Column(nullable = false, updatable = false, length = 64)
    private String type;

    @Column(nullable = false, updatable = false, length = 160)
    private String title;

    @Column(nullable = false, updatable = false, length = 2000)
    private String body;

    @Column(name = "action_url", updatable = false, length = 1000)
    private String actionUrl;

    @Column(name = "reference_id", updatable = false)
    private UUID referenceId;

    @Column(name = "context_id", updatable = false)
    private UUID contextId;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(
            UUID sourceEventId,
            UUID userId,
            String type,
            String title,
            String body,
            String actionUrl,
            Instant now) {
        this(sourceEventId, userId, type, title, body, actionUrl, null, now);
    }

    public Notification(
            UUID sourceEventId,
            UUID userId,
            String type,
            String title,
            String body,
            String actionUrl,
            UUID referenceId,
            Instant now) {
        this(sourceEventId, userId, type, title, body, actionUrl, referenceId, null, now);
    }

    public Notification(
            UUID sourceEventId,
            UUID userId,
            String type,
            String title,
            String body,
            String actionUrl,
            UUID referenceId,
            UUID contextId,
            Instant now) {
        this.id = UUID.randomUUID();
        this.sourceEventId = Objects.requireNonNull(sourceEventId, "sourceEventId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.type = requireText(type, "type");
        this.title = requireText(title, "title");
        this.body = requireText(body, "body");
        this.actionUrl = actionUrl;
        this.referenceId = referenceId;
        this.contextId = contextId;
        this.createdAt = Objects.requireNonNull(now, "now must not be null");
    }

    public void markRead(Instant now) {
        if (readAt == null) {
            readAt = Objects.requireNonNull(now, "now must not be null");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public UUID getContextId() {
        return contextId;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
