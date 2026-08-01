package com.dating.owoke.media.collection.dto;

import java.util.List;
import java.util.UUID;

import com.dating.owoke.media.asset.dto.MediaItemResponse;

public record MediaCollectionResponse(
        UUID placeId,
        UUID coverMediaId,
        long version,
        List<MediaItemResponse> images) {
}
