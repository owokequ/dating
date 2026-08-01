package com.dating.owoke.places.shared.messaging.event;

public record ExternalImageV2(
        String providerAssetKey,
        String remoteUrl,
        String sourceName,
        String sourceLink) {
}
