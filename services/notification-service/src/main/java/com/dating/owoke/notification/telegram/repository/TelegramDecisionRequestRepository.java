package com.dating.owoke.notification.telegram.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dating.owoke.notification.telegram.domain.TelegramDecisionRequest;

public interface TelegramDecisionRequestRepository extends JpaRepository<TelegramDecisionRequest, UUID> {
    @Query(value = """
            SELECT * FROM telegram_decision_requests
            WHERE (status = 'READY' OR status = 'PROCESSING') AND next_attempt_at <= :now
            ORDER BY next_attempt_at
            LIMIT 25
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<TelegramDecisionRequest> lockReady(@Param("now") Instant now);
}
