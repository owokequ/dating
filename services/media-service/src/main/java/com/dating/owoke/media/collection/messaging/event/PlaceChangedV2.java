package com.dating.owoke.media.collection.messaging.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PlaceChangedV2(
        UUID placeId,
        String cityCode,
        String name,
        String address,
        String category,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer priceLevel,
        String status,
        String source,
        String externalId,
        String sourcePageUrl,
        List<ExternalImageV2> images) {
}
