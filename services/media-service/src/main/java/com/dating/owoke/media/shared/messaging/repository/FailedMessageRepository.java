package com.dating.owoke.media.shared.messaging.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.media.shared.messaging.domain.FailedMessage;

public interface FailedMessageRepository extends JpaRepository<FailedMessage, UUID> {
}
