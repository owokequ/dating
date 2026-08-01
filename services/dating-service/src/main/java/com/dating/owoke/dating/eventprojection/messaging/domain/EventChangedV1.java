package com.dating.owoke.dating.eventprojection.messaging.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

public record EventChangedV1(
        UUID eventId,
        String source,
        String externalId,
        String title,
        String description,
        List<String> categories,
        String priceText,
        boolean isFree,
        String ageRestriction,
        String sourcePageUrl,
        UUID localPlaceId,
        Venue venue,
        String status,
        List<Occurrence> occurrences,
        List<JsonNode> images) {
    public record Venue(String name, String address, Double latitude, Double longitude) {}
    public record Occurrence(UUID occurrenceId, Instant startsAt, Instant endsAt, boolean continuous, String status) {}
}
