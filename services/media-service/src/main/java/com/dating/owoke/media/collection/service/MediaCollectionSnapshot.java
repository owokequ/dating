package com.dating.owoke.media.collection.service;

import java.util.List;
import java.util.UUID;

public record MediaCollectionSnapshot(
        UUID ownerId,
        UUID coverMediaId,
        List<UUID> orderedMediaIds,
        long revision) {
}
