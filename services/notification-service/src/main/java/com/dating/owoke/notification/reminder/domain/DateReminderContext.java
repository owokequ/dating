package com.dating.owoke.notification.reminder.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "date_reminder_contexts")
@IdClass(DateReminderContextId.class)
public class DateReminderContext {

    @Id
    @Column(name = "proposal_id", nullable = false)
    private UUID proposalId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "couple_id", nullable = false)
    private UUID coupleId;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(name = "action_url", nullable = false, length = 1000)
    private String actionUrl;

    @Column(name = "media_id")
    private UUID mediaId;

    protected DateReminderContext() {
    }

    public DateReminderContext(UUID proposalId, UUID userId, UUID coupleId, Instant scheduledAt,
            String body, String actionUrl, UUID mediaId) {
        this.proposalId = Objects.requireNonNull(proposalId);
        this.userId = Objects.requireNonNull(userId);
        this.coupleId = Objects.requireNonNull(coupleId);
        this.scheduledAt = Objects.requireNonNull(scheduledAt);
        this.body = Objects.requireNonNull(body);
        this.actionUrl = Objects.requireNonNull(actionUrl);
        this.mediaId = mediaId;
    }

    public UUID getProposalId() { return proposalId; }
    public UUID getUserId() { return userId; }
    public Instant getScheduledAt() { return scheduledAt; }
    public String getBody() { return body; }
    public String getActionUrl() { return actionUrl; }
    public UUID getMediaId() { return mediaId; }
}
