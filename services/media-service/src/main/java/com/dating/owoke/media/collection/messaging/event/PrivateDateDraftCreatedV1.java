package com.dating.owoke.media.collection.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record PrivateDateDraftCreatedV1(
        UUID proposalId, UUID coupleId, UUID proposerId, UUID responderId, Instant expiresAt) {
}
