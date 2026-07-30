package com.dating.owoke.dating.dateproposal.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDateProposalRequest(
        @NotNull @Future Instant scheduledAt,
        @NotNull UUID placeId,
        @Size(max = 1000) String description
) {
}
