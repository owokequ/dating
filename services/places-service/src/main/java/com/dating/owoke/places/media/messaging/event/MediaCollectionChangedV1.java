package com.dating.owoke.places.media.messaging.event;

import java.util.List;
import java.util.UUID;

public record MediaCollectionChangedV1(
        String ownerType,
        UUID ownerId,
        UUID coverMediaId,
        List<UUID> orderedMediaIds,
        long collectionVersion) {
}
