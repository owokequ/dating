package com.dating.owoke.notification.telegram.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.dating.owoke.notification.telegram.domain.TelegramDateCard;
import com.dating.owoke.notification.telegram.domain.TelegramDateCardId;

public interface TelegramDateCardRepository extends JpaRepository<TelegramDateCard, TelegramDateCardId> {
    Optional<TelegramDateCard> findByProposalIdAndUserId(UUID proposalId, UUID userId);
}
