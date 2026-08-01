package com.dating.owoke.places.sync.exception;

public class SyncAlreadyRunningException extends RuntimeException {

    public SyncAlreadyRunningException() {
        super("2GIS synchronization is already running");
    }
}
