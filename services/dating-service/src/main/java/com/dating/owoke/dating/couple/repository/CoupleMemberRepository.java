package com.dating.owoke.dating.couple.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dating.owoke.dating.couple.domain.CoupleMember;

import jakarta.persistence.LockModeType;

public interface CoupleMemberRepository extends JpaRepository<CoupleMember, UUID> {

    Optional<CoupleMember> findByUserIdAndLeftAtIsNull(UUID userId);

    List<CoupleMember> findByCoupleIdAndLeftAtIsNullOrderByJoinedAt(UUID coupleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select member from CoupleMember member where member.userId = :userId and member.leftAt is null")
    Optional<CoupleMember> findActiveByUserIdForUpdate(@Param("userId") UUID userId);
}
