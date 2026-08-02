package com.dating.owoke.dating.dateproposal.dto;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePrivatePlaceDraftRequest(
        @NotNull @Future Instant scheduledAt,
        @NotBlank @Size(max = 300) String placeName,
        @Size(max = 500) String placeAddress,
        @Size(max = 1000) String description
) {
}
