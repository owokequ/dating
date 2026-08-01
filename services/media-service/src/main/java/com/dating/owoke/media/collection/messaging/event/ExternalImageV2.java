package com.dating.owoke.media.collection.messaging.event;

public record ExternalImageV2(
        String providerAssetKey,
        String remoteUrl,
        String sourceName,
        String sourceLink) {
}
