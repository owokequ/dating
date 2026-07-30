package com.dating.owoke.dating.couple.service;

import java.time.Instant;
import java.util.UUID;

public record InvitationPreview(UUID invitationId, Instant expiresAt) {
}
