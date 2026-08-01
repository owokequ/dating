package com.dating.owoke.places.place.domain;

public enum PlaceSource {
    MANUAL,
    TWO_GIS,
    KUDAGO;

    public boolean isExternal() {
        return this != MANUAL;
    }
}
