package com.dating.owoke.media.asset.service;

public record RemoteImageDescriptor(
        String providerAssetKey,
        String remoteUrl,
        String sourceName,
        String sourceLink) {
}
