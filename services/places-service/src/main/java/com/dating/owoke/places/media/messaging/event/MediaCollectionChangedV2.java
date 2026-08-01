package com.dating.owoke.places.media.messaging.event;

import java.util.List;
import java.util.UUID;

public record MediaCollectionChangedV2(
        String ownerType,
        UUID ownerId,
        UUID coverMediaId,
        List<UUID> orderedMediaIds,
        List<Item> items,
        long collectionVersion) {

    public record Item(
            UUID mediaId,
            int position,
            String source,
            String providerAssetKey,
            String thumbnailUrl,
            String cardUrl,
            String detailUrl,
            String sourceName,
            String sourceLink) {
    }
}
