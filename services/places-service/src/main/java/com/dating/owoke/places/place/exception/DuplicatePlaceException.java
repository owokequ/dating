package com.dating.owoke.places.place.exception;

public class DuplicatePlaceException extends RuntimeException {

    public DuplicatePlaceException() {
        super("A place with the same normalized name and address already exists");
    }
}
