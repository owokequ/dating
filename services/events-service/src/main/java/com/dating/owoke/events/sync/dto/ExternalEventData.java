package com.dating.owoke.events.sync.dto;

import java.util.List;

public record ExternalEventData(
        String externalId,
        String title,
        String description,
        List<String> categories,
        String priceText,
        boolean free,
        String ageRestriction,
        String sourcePageUrl,
        String kudagoPlaceId,
        String venueName,
        String venueAddress,
        Double latitude,
        Double longitude,
        List<ExternalOccurrenceData> occurrences,
        List<ExternalImageData> images) {
}
