package com.dating.owoke.dating.shared.idempotency.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.dating.shared.idempotency.domain.IdempotencyRecord;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByUserIdAndOperationAndIdempotencyKey(
            UUID userId, String operation, String idempotencyKey);
}
