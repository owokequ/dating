package com.dating.owoke.dating.couple.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dating.owoke.dating.couple.domain.Couple;

import jakarta.persistence.LockModeType;

public interface CoupleRepository extends JpaRepository<Couple, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select couple from Couple couple where couple.id = :id")
    Optional<Couple> findByIdForUpdate(@Param("id") UUID id);
}
