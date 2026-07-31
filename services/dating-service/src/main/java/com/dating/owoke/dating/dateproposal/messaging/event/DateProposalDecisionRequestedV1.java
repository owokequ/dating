package com.dating.owoke.dating.dateproposal.messaging.event;

import java.util.UUID;

public record DateProposalDecisionRequestedV1(
        UUID proposalId,
        UUID coupleId,
        UUID actorId,
        String decision) {
}
