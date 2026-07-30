package com.dating.owoke.dating.dateproposal.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.dating.dateproposal.domain.DateProposal;

public interface DateProposalRepository extends JpaRepository<DateProposal, UUID> {

    List<DateProposal> findByCoupleIdOrderByScheduledAtDesc(UUID coupleId);
}
