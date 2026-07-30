package com.dating.owoke.identity.telegram.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "external_identities")
public class ExternalIdentity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 32)
    private ExternalProvider provider;

    @Column(nullable = false, length = 128)
    private String subject;

    @Column(name = "telegram_user_id")
    private Long telegramUserId;

    @Column(length = 64)
    private String username;

    @Column(name = "bot_access", nullable = false)
    private boolean botAccess;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_authenticated_at", nullable = false)
    private Instant lastAuthenticatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected ExternalIdentity() {
    }

    public ExternalIdentity(
            UUID userId,
            String subject,
            long telegramUserId,
            String username,
            boolean botAccess,
            Instant now) {
        this.id = UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.provider = ExternalProvider.TELEGRAM;
        this.subject = requireText(subject, "subject", 128);
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        this.telegramUserId = telegramUserId;
        this.username = normalizeOptional(username, 64);
        this.botAccess = botAccess;
        this.createdAt = Objects.requireNonNull(now, "now must not be null");
        this.lastAuthenticatedAt = now;
    }

    public void recordLogin(String username, boolean botAccess, Instant now) {
        this.username = normalizeOptional(username, 64);
        this.botAccess = this.botAccess || botAccess;
        this.lastAuthenticatedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public void recordOidcLogin(String subject, String username, boolean botAccess, Instant now) {
        String normalizedSubject = requireText(subject, "subject", 128);
        if (!this.subject.equals(normalizedSubject)) {
            if (!this.subject.startsWith("bot:")) {
                throw new IllegalStateException("Telegram OIDC subject does not match linked identity");
            }
            this.subject = normalizedSubject;
        }
        recordLogin(username, botAccess, now);
    }

    public UUID getUserId() {
        return userId;
    }

    public Long getTelegramUserId() {
        return telegramUserId;
    }

    public String getUsername() {
        return username;
    }

    public boolean hasBotAccess() {
        return botAccess;
    }

    private static String requireText(String value, String name, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException(name + " must contain 1-" + maxLength + " characters");
        }
        return value;
    }

    private static String normalizeOptional(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("value is too long");
        }
        return normalized;
    }
}
