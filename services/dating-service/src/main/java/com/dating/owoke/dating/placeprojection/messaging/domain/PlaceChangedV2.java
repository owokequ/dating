package com.dating.owoke.dating.placeprojection.messaging.domain;

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
        List<Image> images) implements PlaceChanged {

    public record Image(
            String providerAssetKey,
            String remoteUrl,
            String sourceName,
            String sourceLink) {
    }
}
