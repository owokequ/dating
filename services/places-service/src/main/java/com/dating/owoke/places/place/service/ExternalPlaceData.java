package com.dating.owoke.places.place.service;

public record ExternalPlaceData(
        String externalId,
        String name,
        String category,
        String address,
        double latitude,
        double longitude) {
}
