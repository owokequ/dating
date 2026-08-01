package com.dating.owoke.places.media.service;

import java.util.List;
import java.util.UUID;

import com.dating.owoke.places.place.dto.PlaceImageResponse;

public record PlaceMediaView(UUID coverMediaId, List<PlaceImageResponse> images) {

    public static PlaceMediaView empty() {
        return new PlaceMediaView(null, List.of());
    }
}
