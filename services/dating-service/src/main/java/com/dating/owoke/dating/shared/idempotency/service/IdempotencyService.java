package com.dating.owoke.dating.shared.idempotency.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dating.owoke.dating.shared.exception.BusinessConflictException;
import com.dating.owoke.dating.shared.idempotency.domain.IdempotencyRecord;
import com.dating.owoke.dating.shared.idempotency.repository.IdempotencyRecordRepository;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final Clock clock;

    public IdempotencyService(IdempotencyRecordRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Optional<UUID> find(UUID userId, String operation, String key, String requestMaterial) {
        validateKey(key);
        String requestHash = hash(requestMaterial);
        return repository.findByUserIdAndOperationAndIdempotencyKey(userId, operation, key)
                .map(record -> {
                    if (!record.getRequestHash().equals(requestHash)) {
                        throw new BusinessConflictException("Idempotency-Key was already used for another request");
                    }
                    return record.getResourceId();
                });
    }

    public void remember(UUID userId, String operation, String key, String requestMaterial, UUID resourceId) {
        repository.saveAndFlush(new IdempotencyRecord(
                userId, operation, key, hash(requestMaterial), resourceId, clock.instant()));
    }

    private static void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key must contain 1-128 characters");
        }
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }
}
