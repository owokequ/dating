package com.dating.owoke.notification.availability.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class InvalidSiteAvailabilityWebhookException extends RuntimeException {

    public InvalidSiteAvailabilityWebhookException() {
        super("Site availability webhook is not authorized");
    }
}
