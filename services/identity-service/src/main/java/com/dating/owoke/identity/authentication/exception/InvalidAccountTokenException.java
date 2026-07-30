package com.dating.owoke.identity.authentication.exception;

public class InvalidAccountTokenException extends RuntimeException {

    public InvalidAccountTokenException() {
        super("Account token is invalid, expired or already used");
    }
}
