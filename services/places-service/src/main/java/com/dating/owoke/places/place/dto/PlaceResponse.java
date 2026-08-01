package com.dating.owoke.places.place.dto;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

public record PlaceResponse(
        UUID id,
        String cityCode,
        String name,
        String description,
        String category,
        String address,
        double latitude,
        double longitude,
        Integer priceLevel,
        String source,
        String externalId,
        String status,
        UUID coverMediaId,
        List<PlaceImageResponse> images,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}
