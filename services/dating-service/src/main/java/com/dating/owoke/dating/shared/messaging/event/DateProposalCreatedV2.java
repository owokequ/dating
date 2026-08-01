package com.dating.owoke.dating.shared.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record DateProposalCreatedV2(
        UUID proposalId,
        UUID coupleId,
        UUID proposerId,
        UUID responderId,
        Instant scheduledAt,
        String timezone,
        UUID placeId,
        String placeName,
        String placeAddress,
        UUID placeCoverMediaId,
        String description) {
}
