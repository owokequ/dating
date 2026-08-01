package com.dating.owoke.media.processing.service;

import java.util.List;

public record ProcessedImage(
        String detectedContentType,
        int originalWidth,
        int originalHeight,
        String sourceSha256,
        List<ProcessedVariant> variants) {
}
