package com.dating.owoke.dating.placeprojection.messaging.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.dating.placeprojection.messaging.domain.InboxEvent;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {
}
