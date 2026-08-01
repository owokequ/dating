package com.dating.owoke.places.shared.messaging.event;

import java.util.List;
import java.util.UUID;

public record PlaceChangedV2(
        UUID placeId,
        String cityCode,
        String name,
        String address,
        String category,
        double latitude,
        double longitude,
        Integer priceLevel,
        String status,
        String source,
        String externalId,
        String sourcePageUrl,
        List<ExternalImageV2> images) {
}
