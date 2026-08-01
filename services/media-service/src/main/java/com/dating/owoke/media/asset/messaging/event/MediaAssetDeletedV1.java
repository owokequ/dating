package com.dating.owoke.media.asset.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record MediaAssetDeletedV1(
        UUID mediaId,
        String ownerType,
        UUID ownerId,
        Instant deletedAt,
        Instant purgeAfter) {
}
