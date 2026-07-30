package com.dating.owoke.identity.telegram.service;

public record TelegramOidcState(String codeVerifier, String nonce, String continuePath) {
}
