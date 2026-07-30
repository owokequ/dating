package com.dating.owoke.notification.shared.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record DateProposalCreatedV1(
        UUID proposalId,
        UUID coupleId,
        UUID proposerId,
        UUID responderId,
        Instant scheduledAt,
        String timezone,
        UUID placeId,
        String placeName,
        String placeAddress,
        String description) {
}
