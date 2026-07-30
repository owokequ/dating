package com.dating.owoke.dating.couple.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.dating.owoke.dating.couple.domain.CoupleMemberRole;
import com.dating.owoke.dating.couple.domain.CoupleStatus;

public record CoupleDetails(
        UUID id,
        CoupleStatus status,
        List<MemberDetails> members,
        Instant createdAt,
        Instant activatedAt,
        long version
) {
    public record MemberDetails(UUID userId, CoupleMemberRole role, Instant joinedAt) {
    }
}
