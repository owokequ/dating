package com.dating.owoke.dating.dateproposal.repository;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.dating.dateproposal.domain.DateProposal;
import com.dating.owoke.dating.dateproposal.domain.DateProposalStatus;

public interface DateProposalRepository extends JpaRepository<DateProposal, UUID> {

    List<DateProposal> findByCoupleIdOrderByScheduledAtDesc(UUID coupleId);

    List<DateProposal> findByStatusAndDraftExpiresAtBefore(DateProposalStatus status, Instant now);
}
