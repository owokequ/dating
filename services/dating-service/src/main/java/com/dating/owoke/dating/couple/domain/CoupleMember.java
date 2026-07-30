package com.dating.owoke.dating.couple.domain;

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
@Table(name = "couple_members")
public class CoupleMember {

    @Id
    private UUID id;

    @Column(name = "couple_id", nullable = false, updatable = false)
    private UUID coupleId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 32)
    private CoupleMemberRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected CoupleMember() {
    }

    public CoupleMember(UUID coupleId, UUID userId, CoupleMemberRole role, Instant now) {
        this.id = UUID.randomUUID();
        this.coupleId = Objects.requireNonNull(coupleId, "coupleId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.joinedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public void leave(Instant now) {
        if (leftAt != null) {
            throw new IllegalStateException("Member has already left the couple");
        }
        leftAt = Objects.requireNonNull(now, "now must not be null");
    }

    public UUID getId() {
        return id;
    }

    public UUID getCoupleId() {
        return coupleId;
    }

    public UUID getUserId() {
        return userId;
    }

    public CoupleMemberRole getRole() {
        return role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public Instant getLeftAt() {
        return leftAt;
    }
}
