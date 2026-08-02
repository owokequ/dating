package com.dating.owoke.notification.reminder.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class DateReminderContextId implements Serializable {
    private UUID proposalId;
    private UUID userId;

    public DateReminderContextId() {
    }

    public DateReminderContextId(UUID proposalId, UUID userId) {
        this.proposalId = proposalId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DateReminderContextId value
                && Objects.equals(proposalId, value.proposalId)
                && Objects.equals(userId, value.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(proposalId, userId);
    }
}
