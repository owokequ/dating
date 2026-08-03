package com.dating.owoke.notification.telegram.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class TelegramDateCardId implements Serializable {
    private UUID proposalId;
    private UUID userId;
    public TelegramDateCardId() { }
    public TelegramDateCardId(UUID proposalId, UUID userId) { this.proposalId = proposalId; this.userId = userId; }
    @Override public boolean equals(Object value) { return value instanceof TelegramDateCardId id && Objects.equals(proposalId, id.proposalId) && Objects.equals(userId, id.userId); }
    @Override public int hashCode() { return Objects.hash(proposalId, userId); }
}
