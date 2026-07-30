package com.dating.owoke.identity.shared.messaging.inbox.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.identity.shared.messaging.inbox.domain.FailedMessage;

public interface FailedMessageRepository extends JpaRepository<FailedMessage, UUID> {
}
