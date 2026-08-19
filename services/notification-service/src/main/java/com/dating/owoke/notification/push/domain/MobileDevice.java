package com.dating.owoke.notification.push.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "mobile_devices")
public class MobileDevice {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "expo_push_token", nullable = false, unique = true, length = 512) private String expoPushToken;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private MobilePlatform platform;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "last_seen_at", nullable = false) private Instant lastSeenAt;
    @Version @Column(nullable = false) private long version;
    protected MobileDevice() { }
    public MobileDevice(UUID userId, String token, MobilePlatform platform, Instant now) {
        this.id = UUID.randomUUID(); this.userId = userId; this.expoPushToken = token; this.platform = platform;
        this.active = true; this.createdAt = now; this.updatedAt = now; this.lastSeenAt = now;
    }
    public void refresh(UUID userId, MobilePlatform platform, Instant now) { this.userId = userId; this.platform = platform; active = true; updatedAt = now; lastSeenAt = now; }
    public void deactivate(Instant now) { active = false; updatedAt = now; }
    public UUID getUserId() { return userId; }
    public String getExpoPushToken() { return expoPushToken; }
}
