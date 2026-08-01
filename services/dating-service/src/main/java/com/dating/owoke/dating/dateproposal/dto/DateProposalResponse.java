package com.dating.owoke.dating.dateproposal.dto;

import java.time.Instant;
import java.util.UUID;

import com.dating.owoke.dating.dateproposal.domain.DateProposalStatus;
import com.dating.owoke.dating.dateproposal.domain.DateSelectionType;

public record DateProposalResponse(
        UUID id,
        UUID coupleId,
        UUID proposerId,
        UUID responderId,
        Instant scheduledAt,
        String timezone,
        DateSelectionType selectionType,
        UUID placeId,
        String placeName,
        String placeAddress,
        UUID placeCoverMediaId,
        UUID eventId,
        UUID eventOccurrenceId,
        String eventTitle,
        String eventSourceUrl,
        String eventPrice,
        String description,
        DateProposalStatus status,
        Instant createdAt,
        Instant decidedAt,
        Instant cancelledAt,
        long version
) {
}
