package com.dating.owoke.dating.placeprojection.messaging.domain;

import java.util.UUID;

public record PlaceChangedV1(
        UUID placeId,
        String cityCode,
        String name,
        String address,
        String category,
        double latitude,
        double longitude,
        Integer priceLevel,
        String status) {
}
