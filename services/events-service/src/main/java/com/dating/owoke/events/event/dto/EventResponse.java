package com.dating.owoke.events.event.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String title,
        String description,
        String providerDescription,
        boolean descriptionOverridden,
        List<String> categories,
        String priceText,
        boolean free,
        String ageRestriction,
        String sourcePageUrl,
        String venueName,
        String venueAddress,
        Double latitude,
        Double longitude,
        boolean venueOverride,
        UUID localPlaceId,
        String status,
        List<EventOccurrenceResponse> occurrences,
        List<EventImageResponse> images,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}
