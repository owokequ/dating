package com.dating.owoke.media.asset.messaging.event;

public record MediaVariantV1(
        String name,
        String contentType,
        int width,
        int height,
        long size,
        String sha256) {
}
