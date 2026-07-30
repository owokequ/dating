package com.dating.owoke.notification.preference.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled;

    @Column(name = "telegram_enabled", nullable = false)
    private boolean telegramEnabled;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected NotificationPreference() {
    }

    public NotificationPreference(UUID userId, Instant now) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.inAppEnabled = true;
        this.telegramEnabled = true;
        this.emailEnabled = true;
        this.updatedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public boolean isTelegramEnabled() {
        return telegramEnabled;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void disableTelegram(Instant now) {
        telegramEnabled = false;
        updatedAt = Objects.requireNonNull(now, "now must not be null");
    }
}
