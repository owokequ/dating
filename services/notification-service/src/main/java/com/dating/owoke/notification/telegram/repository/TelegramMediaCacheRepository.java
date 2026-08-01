package com.dating.owoke.notification.telegram.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.notification.telegram.domain.TelegramMediaCache;

public interface TelegramMediaCacheRepository extends JpaRepository<TelegramMediaCache, UUID> {
    Optional<TelegramMediaCache> findByMediaIdAndContentHashAndBotId(
            UUID mediaId, String contentHash, String botId);

    Optional<TelegramMediaCache> findFirstByMediaIdAndBotIdOrderByCreatedAtDesc(UUID mediaId, String botId);
}
