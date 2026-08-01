package com.dating.owoke.media.collection.service;

import java.util.List;
import java.util.UUID;

import com.dating.owoke.media.asset.dto.MediaItemResponse;
import com.dating.owoke.media.collection.domain.MediaOwnerType;

public record MediaCollectionSnapshot(
        MediaOwnerType ownerType,
        UUID ownerId,
        UUID coverMediaId,
        List<UUID> orderedMediaIds,
        List<MediaItemResponse> items,
        long revision) {
}
