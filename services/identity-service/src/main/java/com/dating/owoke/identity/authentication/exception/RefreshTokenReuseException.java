package com.dating.owoke.identity.authentication.exception;

public class RefreshTokenReuseException extends AuthenticationRejectedException {

    public RefreshTokenReuseException() {
        super("Refresh token reuse detected");
    }
}
