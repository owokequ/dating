package com.dating.owoke.identity.authentication.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "password_credentials")
public class PasswordCredential {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected PasswordCredential() {
    }

    public PasswordCredential(UUID userId, String passwordHash, Instant now) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.updatedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public void changePassword(String passwordHash, Instant now) {
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.updatedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
