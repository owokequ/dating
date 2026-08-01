package com.dating.owoke.media.asset.domain;

public enum MediaVariantName {
    THUMBNAIL(480, 320, 0.82),
    CARD(1200, 800, 0.86),
    DETAIL(1920, 1280, 0.9),
    TELEGRAM(1280, 853, 0.87);

    private final int width;
    private final int height;
    private final double quality;

    MediaVariantName(int width, int height, double quality) {
        this.width = width;
        this.height = height;
        this.quality = quality;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public double quality() {
        return quality;
    }
}
