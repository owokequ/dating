package com.dating.owoke.media.collection.messaging.event;

import java.util.UUID;

public record MediaCollectionItemV2(
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
