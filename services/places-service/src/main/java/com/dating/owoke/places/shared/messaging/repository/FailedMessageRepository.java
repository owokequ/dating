package com.dating.owoke.places.shared.messaging.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.places.shared.messaging.domain.FailedMessage;

public interface FailedMessageRepository extends JpaRepository<FailedMessage, UUID> {
}
