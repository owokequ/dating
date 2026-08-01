package com.dating.owoke.places.place.service;

public record ExternalPlaceImageData(
        String providerAssetKey,
        String remoteUrl,
        String sourceName,
        String sourceLink) {
}
