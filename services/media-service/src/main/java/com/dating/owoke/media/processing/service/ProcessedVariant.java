package com.dating.owoke.media.processing.service;

import com.dating.owoke.media.asset.domain.MediaVariantName;

public record ProcessedVariant(
        MediaVariantName name,
        byte[] content,
        String contentType,
        int width,
        int height,
        String sha256) {
}
