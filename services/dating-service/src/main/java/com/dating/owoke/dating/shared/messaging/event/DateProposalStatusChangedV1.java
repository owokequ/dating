package com.dating.owoke.dating.shared.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record DateProposalStatusChangedV1(
        UUID proposalId,
        UUID coupleId,
        UUID proposerId,
        UUID responderId,
        String status,
        UUID changedBy,
        Instant changedAt,
        Instant scheduledAt,
        String timezone,
        String placeName,
        String placeAddress,
        String description
) {
}
