package com.dating.owoke.dating.couple.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dating.owoke.dating.couple.domain.CoupleInvitation;
import com.dating.owoke.dating.couple.domain.InvitationStatus;

import jakarta.persistence.LockModeType;

public interface CoupleInvitationRepository extends JpaRepository<CoupleInvitation, UUID> {

    List<CoupleInvitation> findByCoupleIdAndStatus(UUID coupleId, InvitationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from CoupleInvitation invitation where invitation.tokenHash = :tokenHash")
    Optional<CoupleInvitation> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    Optional<CoupleInvitation> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from CoupleInvitation invitation where invitation.id = :id")
    Optional<CoupleInvitation> findByIdForUpdate(@Param("id") UUID id);
}
