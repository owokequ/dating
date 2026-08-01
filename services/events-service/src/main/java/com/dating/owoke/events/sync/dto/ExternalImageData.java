package com.dating.owoke.events.sync.dto;

public record ExternalImageData(
        String providerAssetKey,
        String remoteUrl,
        String thumbnailUrl,
        String sourceName,
        String sourceLink) {
}
