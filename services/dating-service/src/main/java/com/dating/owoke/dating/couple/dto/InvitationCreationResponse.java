package com.dating.owoke.dating.couple.dto;

import java.time.Instant;
import java.util.UUID;

public record InvitationCreationResponse(UUID invitationId, String inviteUrl, Instant expiresAt) {
}
