package com.dating.owoke.events.sync.dto;

import java.time.Instant;

public record ExternalOccurrenceData(
        String providerOccurrenceKey,
        Instant startsAt,
        Instant endsAt,
        boolean continuous) {
}
