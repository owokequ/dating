package com.dating.owoke.identity.account.service;

import java.util.UUID;

import com.dating.owoke.identity.account.domain.AccountRole;
import com.dating.owoke.identity.account.domain.AccountStatus;

public record AccountProfile(
        UUID userId,
        String email,
        String displayName,
        AccountStatus status,
        AccountRole role,
        boolean telegramLinked,
        boolean onboardingCompleted) {
}
