package com.dating.owoke.identity.account.dto;

import java.util.UUID;

import com.dating.owoke.identity.account.domain.AccountRole;
import com.dating.owoke.identity.account.domain.AccountStatus;
import com.dating.owoke.identity.account.service.AccountProfile;

public record AccountProfileResponse(
        UUID userId,
        String email,
        String displayName,
        AccountStatus status,
        AccountRole role,
        boolean telegramLinked) {

    public static AccountProfileResponse from(AccountProfile profile) {
        return new AccountProfileResponse(
                profile.userId(),
                profile.email(),
                profile.displayName(),
                profile.status(),
                profile.role(),
                profile.telegramLinked());
    }
}
