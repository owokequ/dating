package com.dating.owoke.notification.telegram.messaging.event;

import java.util.UUID;

public record DateProposalDecisionRequestedV1(
        UUID proposalId,
        UUID coupleId,
        UUID actorId,
        String decision) {
}
