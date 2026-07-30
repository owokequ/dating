package com.dating.owoke.notification.contact.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.notification.contact.domain.ContactProjection;

public interface ContactProjectionRepository extends JpaRepository<ContactProjection, UUID> {

    Optional<ContactProjection> findByTelegramUserId(long telegramUserId);
}
