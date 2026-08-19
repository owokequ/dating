package com.dating.owoke.notification.push.service;
public class ExpoPushException extends RuntimeException {
    private final boolean retryable;
    public ExpoPushException(String message, boolean retryable, Throwable cause) { super(message, cause); this.retryable = retryable; }
    public boolean retryable() { return retryable; }
}
