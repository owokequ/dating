package com.dating.owoke.identity.authentication.service;

import java.util.UUID;

import com.dating.owoke.identity.account.domain.AccountRole;

public record AuthenticatedAccount(
        UUID userId,
        String displayName,
        String email,
        AccountRole role) {
}
