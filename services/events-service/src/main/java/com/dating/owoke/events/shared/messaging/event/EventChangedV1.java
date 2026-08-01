package com.dating.owoke.events.shared.messaging.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
        List<Image> images) {

    public record Venue(String name, String address, Double latitude, Double longitude) {}
    public record Occurrence(UUID occurrenceId, Instant startsAt, Instant endsAt, boolean continuous, String status) {}
    public record Image(String providerAssetKey, String remoteUrl, String thumbnailUrl,
                        String sourceName, String sourceLink) {}
}
