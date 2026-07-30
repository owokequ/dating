package com.dating.owoke.dating.shared.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record CoupleActivatedV1(UUID coupleId, UUID ownerId, UUID partnerId, Instant activatedAt) {
}
