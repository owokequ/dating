package com.dating.owoke.dating.dateproposal.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEventDateProposalRequest(
        @NotNull UUID eventOccurrenceId,
        Instant visitAt,
        @Size(max = 1000) String description) {
}
