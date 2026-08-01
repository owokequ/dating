package com.dating.owoke.media.collection.dto;

import java.util.List;
import java.util.UUID;

import com.dating.owoke.media.asset.dto.MediaItemResponse;
import com.dating.owoke.media.collection.domain.MediaOwnerType;

public record MediaCollectionResponse(
        MediaOwnerType ownerType,
        UUID ownerId,
        UUID coverMediaId,
        long version,
        List<MediaItemResponse> images) {
}
