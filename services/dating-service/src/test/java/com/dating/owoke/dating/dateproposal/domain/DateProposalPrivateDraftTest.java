package com.dating.owoke.dating.dateproposal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.dating.owoke.dating.dateproposal.exception.InvalidDateProposalActionException;

class DateProposalPrivateDraftTest {

    @Test
    void sendsPrivateDraftOnlyByItsProposer() {
        Instant now = Instant.parse("2026-08-03T10:00:00Z");
        UUID proposer = UUID.randomUUID();
        DateProposal draft = DateProposal.privateDraft(
                UUID.randomUUID(), proposer, UUID.randomUUID(), now.plus(3, ChronoUnit.DAYS),
                "Наше место", "Казань", "Возьмём кофе", now.plus(24, ChronoUnit.HOURS), now);

        draft.send(proposer, now.plus(1, ChronoUnit.MINUTES));

        assertThat(draft.getStatus()).isEqualTo(DateProposalStatus.PENDING_CONFIRMATION);
        assertThat(draft.getDraftExpiresAt()).isNull();
        assertThat(draft.getSelectionType()).isEqualTo(DateSelectionType.PRIVATE_PLACE);
        assertThat(draft.getPlaceId()).isNull();
    }

    @Test
    void rejectsSendingExpiredPrivateDraft() {
        Instant now = Instant.parse("2026-08-03T10:00:00Z");
        UUID proposer = UUID.randomUUID();
        DateProposal draft = DateProposal.privateDraft(
                UUID.randomUUID(), proposer, UUID.randomUUID(), now.plus(3, ChronoUnit.DAYS),
                "Наше место", null, null, now.plus(1, ChronoUnit.MINUTES), now);

        assertThatThrownBy(() -> draft.send(proposer, now.plus(2, ChronoUnit.MINUTES)))
                .isInstanceOf(InvalidDateProposalActionException.class);
    }
}
