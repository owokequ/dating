package com.dating.owoke.dating.dateproposal.dto;

import java.time.Instant;
import java.util.UUID;

import com.dating.owoke.dating.dateproposal.domain.DateProposalStatus;

public record DateProposalResponse(
        UUID id,
        UUID coupleId,
        UUID proposerId,
        UUID responderId,
        Instant scheduledAt,
        String timezone,
        UUID placeId,
        String placeName,
        String placeAddress,
        String description,
        DateProposalStatus status,
        Instant createdAt,
        Instant decidedAt,
        Instant cancelledAt,
        long version
) {
}
