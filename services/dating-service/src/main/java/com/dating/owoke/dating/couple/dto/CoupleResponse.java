package com.dating.owoke.dating.couple.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.dating.owoke.dating.couple.domain.CoupleStatus;

public record CoupleResponse(
        UUID id,
        CoupleStatus status,
        List<CoupleMemberResponse> members,
        Instant createdAt,
        Instant activatedAt,
        long version
) {
}
