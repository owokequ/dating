package com.dating.owoke.identity.telegram.exception;

public class TelegramOidcException extends RuntimeException {

    public TelegramOidcException(String message) {
        super(message);
    }

    public TelegramOidcException(String message, Throwable cause) {
        super(message, cause);
    }
}
