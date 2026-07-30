package com.dating.owoke.identity.authentication.exception;

public class AuthenticationRejectedException extends RuntimeException {

    public AuthenticationRejectedException(String message) {
        super(message);
    }
}
