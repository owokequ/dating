package com.dating.owoke.places.place.dto;

import java.util.UUID;

public record PlaceImageResponse(
        UUID mediaId,
        int position,
        boolean cover,
        String thumbnailUrl,
        String cardUrl,
        String detailUrl) {
}
