package com.dating.owoke.dating.couple.service;

import java.time.Instant;
import java.util.UUID;

public record InvitationCreation(UUID invitationId, String inviteUrl, Instant expiresAt) {
}
