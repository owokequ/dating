package com.dating.owoke.media.collection.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.dating.owoke.media.collection.domain.EventProjection;

import jakarta.persistence.LockModeType;

public interface EventProjectionRepository extends JpaRepository<EventProjection, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from EventProjection event where event.eventId = :eventId")
    Optional<EventProjection> lockById(UUID eventId);
}
