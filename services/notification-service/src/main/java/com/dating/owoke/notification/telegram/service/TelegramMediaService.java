package com.dating.owoke.notification.telegram.service;

import java.time.Clock;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.notification.media.service.MediaBinary;
import com.dating.owoke.notification.media.service.MediaContentClient;
import com.dating.owoke.notification.telegram.configuration.TelegramBotProperties;
import com.dating.owoke.notification.telegram.domain.TelegramMediaCache;
import com.dating.owoke.notification.telegram.repository.TelegramMediaCacheRepository;

@Service
public class TelegramMediaService {
    private static final UUID PLACEHOLDER_MEDIA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final MediaContentClient mediaClient;
    private final TelegramMediaCacheRepository cacheRepository;
    private final TelegramBotProperties properties;
    private final Clock clock;

    public TelegramMediaService(
            MediaContentClient mediaClient,
            TelegramMediaCacheRepository cacheRepository,
            TelegramBotProperties properties,
            Clock clock) {
        this.mediaClient = mediaClient;
        this.cacheRepository = cacheRepository;
        this.properties = properties;
        this.clock = clock;
    }

    public PreparedTelegramPhoto prepare(UUID mediaId) {
        PreparedTelegramPhoto cached = cached(mediaId);
        if (cached != null) return cached;
        MediaBinary media = mediaClient.getTelegramVariant(mediaId);
        return prepare(mediaId, media);
    }

    public PreparedTelegramPhoto preparePlaceholder() {
        PreparedTelegramPhoto cached = cached(PLACEHOLDER_MEDIA_ID);
        if (cached != null) return cached;
        ClassPathResource resource = new ClassPathResource("telegram/for-my-l-place-placeholder.png");
        try (java.io.InputStream input = resource.getInputStream()) {
            byte[] content = input.readAllBytes();
            return prepare(PLACEHOLDER_MEDIA_ID, new MediaBinary(content, "image/png", sha256(content)));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load Telegram placeholder", exception);
        }
    }

    private PreparedTelegramPhoto prepare(UUID mediaId, MediaBinary media) {
        String botId = botId();
        String fileId = cacheRepository.findByMediaIdAndContentHashAndBotId(mediaId, media.contentHash(), botId)
                .map(TelegramMediaCache::getFileId).orElse(null);
        TelegramPhoto photo = fileId == null
                ? TelegramPhoto.upload(media.content(), media.contentType(), mediaId + ".jpg")
                : TelegramPhoto.cached(fileId);
        return new PreparedTelegramPhoto(mediaId, media.contentHash(), botId, photo);
    }

    private PreparedTelegramPhoto cached(UUID mediaId) {
        String botId = botId();
        TelegramMediaCache cached = cacheRepository
                .findFirstByMediaIdAndBotIdOrderByCreatedAtDesc(mediaId, botId)
                .orElse(null);
        return cached == null ? null : new PreparedTelegramPhoto(
                mediaId, cached.getContentHash(), botId, TelegramPhoto.cached(cached.getFileId()));
    }

    @Transactional
    public void remember(PreparedTelegramPhoto prepared, TelegramPhotoResult result) {
        if (prepared.photo().cached()) return;
        if (cacheRepository.findByMediaIdAndContentHashAndBotId(
                prepared.mediaId(), prepared.contentHash(), prepared.botId()).isEmpty()) {
            cacheRepository.save(new TelegramMediaCache(
                    prepared.mediaId(), prepared.contentHash(), prepared.botId(),
                    result.fileId(), result.fileUniqueId(), clock.instant()));
        }
    }

    private String botId() {
        String token = properties.botToken();
        int separator = token == null ? -1 : token.indexOf(':');
        return separator > 0 ? token.substring(0, separator) : "unknown";
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record PreparedTelegramPhoto(UUID mediaId, String contentHash, String botId, TelegramPhoto photo) {
    }
}
