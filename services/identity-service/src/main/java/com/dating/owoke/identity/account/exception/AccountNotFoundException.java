package com.dating.owoke.identity.account.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException() {
        super("Account was not found");
    }
}
