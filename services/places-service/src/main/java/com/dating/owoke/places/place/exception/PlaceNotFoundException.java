package com.dating.owoke.places.place.exception;

public class PlaceNotFoundException extends RuntimeException {

    public PlaceNotFoundException() {
        super("Place not found");
    }
}
