package com.dating.owoke.media.processing.service;

import java.nio.charset.StandardCharsets;

import com.dating.owoke.media.processing.exception.InvalidImageException;

public final class ImageFormatDetector {

    private ImageFormatDetector() {
    }

    public static String detect(byte[] content) {
        if (content.length >= 3
                && (content[0] & 0xff) == 0xff
                && (content[1] & 0xff) == 0xd8
                && (content[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        if (content.length >= 8
                && (content[0] & 0xff) == 0x89
                && content[1] == 'P' && content[2] == 'N' && content[3] == 'G'
                && content[4] == 0x0d && content[5] == 0x0a && content[6] == 0x1a && content[7] == 0x0a) {
            return "image/png";
        }
        if (content.length >= 12
                && "RIFF".equals(new String(content, 0, 4, StandardCharsets.US_ASCII))
                && "WEBP".equals(new String(content, 8, 4, StandardCharsets.US_ASCII))) {
            return "image/webp";
        }
        throw new InvalidImageException("Only JPEG, PNG and WebP images are supported");
    }
}
