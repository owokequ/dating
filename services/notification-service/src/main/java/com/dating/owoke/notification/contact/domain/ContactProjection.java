package com.dating.owoke.notification.contact.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "contact_projections")
public class ContactProjection {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(length = 320)
    private String email;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "telegram_user_id")
    private Long telegramUserId;

    @Column(name = "telegram_chat_id")
    private Long telegramChatId;

    @Column(name = "telegram_username", length = 64)
    private String telegramUsername;

    @Column(name = "bot_access", nullable = false)
    private boolean botAccess;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected ContactProjection() {
    }

    public ContactProjection(UUID userId, String displayName, String email, Instant now) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.displayName = requireText(displayName, "displayName");
        this.email = normalize(email);
        this.updatedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public void updateProfile(String displayName, String email, Instant now) {
        this.displayName = requireText(displayName, "displayName");
        if (email != null) {
            this.email = normalize(email);
        }
        this.updatedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public void linkTelegram(long telegramUserId, Long chatId, String username, boolean botAccess, Instant now) {
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        this.telegramUserId = telegramUserId;
        if (chatId != null) {
            this.telegramChatId = chatId;
        }
        this.telegramUsername = normalize(username);
        this.botAccess = botAccess;
        this.updatedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public boolean hasBotAccess() {
        return botAccess;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
