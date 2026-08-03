package com.dating.owoke.dating.couple.dto;

import java.time.Instant;
import java.util.UUID;

import com.dating.owoke.dating.couple.domain.CoupleMemberRole;

public record CoupleMemberResponse(UUID userId, String displayName, CoupleMemberRole role, Instant joinedAt) {
}
