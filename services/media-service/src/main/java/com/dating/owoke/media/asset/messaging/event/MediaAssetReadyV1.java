package com.dating.owoke.media.asset.messaging.event;

import java.util.List;
import java.util.UUID;

public record MediaAssetReadyV1(
        UUID mediaId,
        String ownerType,
        UUID ownerId,
        String sha256,
        List<MediaVariantV1> variants) {
}
