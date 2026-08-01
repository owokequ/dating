package com.dating.owoke.notification.shared.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record DateProposalCreatedV3(
        UUID proposalId, UUID coupleId, UUID proposerId, UUID responderId,
        Instant scheduledAt, String timezone, String selectionType,
        UUID placeId, String placeName, String placeAddress, UUID coverMediaId,
        UUID eventId, UUID eventOccurrenceId, String eventTitle, String eventSourceUrl, String eventPrice,
        String description) {
}
