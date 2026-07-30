package com.dating.owoke.identity.account.domain;

import java.time.Instant;
import java.util.Locale;
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
@Table(name = "users")
public class UserAccount {

    @Id
    private UUID id;

    @Column(length = 320)
    private String email;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AccountStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AccountRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected UserAccount() {
    }

    private UserAccount(String email, String displayName, AccountStatus status, Instant now) {
        this.id = UUID.randomUUID();
        this.email = normalizeEmail(email);
        this.displayName = normalizeDisplayName(displayName);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.role = AccountRole.USER;
        this.createdAt = Objects.requireNonNull(now, "now must not be null");
        this.updatedAt = now;
    }

    public static UserAccount registerLocal(String email, String displayName, Instant now) {
        return new UserAccount(
                Objects.requireNonNull(email, "email must not be null"),
                displayName,
                AccountStatus.PENDING_VERIFICATION,
                now);
    }

    public static UserAccount registerExternal(String displayName, Instant now) {
        return new UserAccount(null, displayName, AccountStatus.ACTIVE, now);
    }

    public void verifyEmail(Instant now) {
        if (status == AccountStatus.DISABLED) {
            throw new IllegalStateException("disabled account cannot be activated");
        }
        status = AccountStatus.ACTIVE;
        updatedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public void changeDisplayName(String displayName, Instant now) {
        this.displayName = normalizeDisplayName(displayName);
        this.updatedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public AccountRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String normalizeEmail(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > 320) {
            throw new IllegalArgumentException("email length must be between 1 and 320 characters");
        }
        return normalized;
    }

    private static String normalizeDisplayName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("displayName must not be null");
        }
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw new IllegalArgumentException("displayName length must be between 1 and 100 characters");
        }
        return normalized;
    }
}
