package com.dating.owoke.events.event.dto;

public record EventImageResponse(
        String providerAssetKey,
        String remoteUrl,
        String thumbnailUrl,
        String sourceName,
        String sourceLink) {
}
