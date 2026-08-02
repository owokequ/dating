package com.dating.owoke.notification.reminder.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "scheduled_notifications")
public class ScheduledNotification {

    @Id
    private UUID id;

    @Column(name = "source_event_id", nullable = false, updatable = false)
    private UUID sourceEventId;

    @Column(name = "proposal_id", nullable = false, updatable = false)
    private UUID proposalId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, updatable = false, length = 16)
    private ReminderType reminderType;

    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private ReminderPayload payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ScheduledNotificationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected ScheduledNotification() {
    }

    public ScheduledNotification(
            UUID sourceEventId,
            UUID proposalId,
            UUID userId,
            ReminderType reminderType,
            Instant scheduledFor,
            ReminderPayload payload,
            Instant now) {
        this.id = UUID.randomUUID();
        this.sourceEventId = Objects.requireNonNull(sourceEventId, "sourceEventId must not be null");
        this.proposalId = Objects.requireNonNull(proposalId, "proposalId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.reminderType = Objects.requireNonNull(reminderType, "reminderType must not be null");
        this.scheduledFor = Objects.requireNonNull(scheduledFor, "scheduledFor must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.status = ScheduledNotificationStatus.PENDING;
        this.createdAt = Objects.requireNonNull(now, "now must not be null");
    }

    public void markCreated(Instant now) {
        status = ScheduledNotificationStatus.CREATED;
        completedAt = now;
    }

    public void reschedule(Instant scheduledFor, ReminderPayload payload, Instant now) {
        this.scheduledFor = Objects.requireNonNull(scheduledFor, "scheduledFor must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.status = ScheduledNotificationStatus.PENDING;
        this.completedAt = null;
    }

    public void cancel(Instant now) {
        if (status == ScheduledNotificationStatus.PENDING) {
            status = ScheduledNotificationStatus.CANCELLED;
            completedAt = now;
        }
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public UUID getProposalId() {
        return proposalId;
    }

    public UUID getUserId() {
        return userId;
    }

    public ReminderType getReminderType() {
        return reminderType;
    }

    public ReminderPayload getPayload() {
        return payload;
    }

    public Instant getScheduledFor() {
        return scheduledFor;
    }
}
