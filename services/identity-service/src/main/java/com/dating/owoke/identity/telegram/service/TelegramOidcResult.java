package com.dating.owoke.identity.telegram.service;

import com.dating.owoke.identity.authentication.service.IssuedSession;

public record TelegramOidcResult(IssuedSession session, String continuePath) {
}
