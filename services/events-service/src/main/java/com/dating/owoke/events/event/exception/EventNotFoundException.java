package com.dating.owoke.events.event.exception;

import java.util.UUID;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(UUID id) {
        super("Event " + id + " was not found");
    }
}
