package com.dating.owoke.dating.shared.messaging.event;

import java.util.UUID;

public record DateProposalDecisionResultV1(
        UUID requestId,
        UUID proposalId,
        UUID coupleId,
        UUID actorId,
        String decision,
        boolean successful,
        String errorCode) {
}
