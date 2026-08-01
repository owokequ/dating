package com.dating.owoke.events.event.dto;

import java.time.Instant;
import java.util.UUID;

public record EventOccurrenceResponse(
        UUID id,
        Instant startsAt,
        Instant endsAt,
        boolean continuous,
        String status) {
}
