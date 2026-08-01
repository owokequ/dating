package com.dating.owoke.places.place.service;

import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.dating.owoke.places.place.domain.PlaceSource;

public record ExternalPlaceData(
        PlaceSource source,
        String externalId,
        String name,
        String providerDescription,
        String category,
        String address,
        double latitude,
        double longitude,
        String sourcePageUrl,
        List<ExternalPlaceImageData> images) {

    public ExternalPlaceData {
        images = images == null ? List.of() : List.copyOf(images);
    }

    public ExternalPlaceData(
            String externalId,
            String name,
            String category,
            String address,
            double latitude,
            double longitude) {
        this(PlaceSource.TWO_GIS, externalId, name, null, category, address, latitude, longitude, null, List.of());
    }

    public static ExternalPlaceData twoGis(
            String externalId,
            String name,
            String category,
            String address,
            double latitude,
            double longitude) {
        return new ExternalPlaceData(
                PlaceSource.TWO_GIS, externalId, name, null, category, address, latitude, longitude, null, List.of());
    }

    public String imagesFingerprint() {
        String value = images.stream()
                .map(image -> image.providerAssetKey() + "|" + image.remoteUrl())
                .sorted()
                .collect(java.util.stream.Collectors.joining("\n"));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
