package com.dating.owoke.dating.shared.messaging.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dating.owoke.dating.shared.messaging.domain.OutboxEvent;

import jakarta.persistence.LockModeType;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select event from OutboxEvent event
            where event.publishedAt is null and event.nextAttemptAt <= :now
            order by event.occurredAt
            """)
    List<OutboxEvent> lockPending(@Param("now") Instant now, Pageable pageable);
}
