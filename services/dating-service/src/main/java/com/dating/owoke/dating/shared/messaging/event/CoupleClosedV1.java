package com.dating.owoke.dating.shared.messaging.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CoupleClosedV1(UUID coupleId, List<UUID> memberIds, UUID closedBy, Instant closedAt) {
}
