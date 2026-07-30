package com.dating.owoke.identity.telegram.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.identity.telegram.domain.ExternalIdentity;
import com.dating.owoke.identity.telegram.domain.ExternalProvider;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentity, UUID> {

    Optional<ExternalIdentity> findByProviderAndSubject(ExternalProvider provider, String subject);

    Optional<ExternalIdentity> findByTelegramUserId(long telegramUserId);

    Optional<ExternalIdentity> findByUserIdAndProvider(UUID userId, ExternalProvider provider);
}
