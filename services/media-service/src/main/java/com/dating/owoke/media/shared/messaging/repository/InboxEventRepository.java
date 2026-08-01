package com.dating.owoke.media.shared.messaging.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.media.shared.messaging.domain.InboxEvent;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {
}
