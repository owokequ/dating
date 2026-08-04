package com.dating.owoke.notification.availability.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "site_availability_states")
public class SiteAvailabilityState {

    public static final short SINGLETON_ID = 1;

    @Id
    private short id;

    @Enumerated(EnumType.STRING)
    @Column(name = "frontend_status", nullable = false, length = 16)
    private MonitorStatus frontendStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "api_status", nullable = false, length = 16)
    private MonitorStatus apiStatus;

    @Column(name = "recovery_pending", nullable = false)
    private boolean recoveryPending;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected SiteAvailabilityState() {
    }

    public boolean apply(MonitorKind monitor, MonitorStatus status, Instant occurredAt) {
        Objects.requireNonNull(monitor, "monitor must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (status == MonitorStatus.UNKNOWN) {
            throw new IllegalArgumentException("External monitor status must be UP or DOWN");
        }

        if (monitor == MonitorKind.FRONTEND) {
            frontendStatus = status;
        } else {
            apiStatus = status;
        }
        if (status == MonitorStatus.DOWN) {
            recoveryPending = true;
        }
        updatedAt = occurredAt;

        if (recoveryPending && frontendStatus == MonitorStatus.UP && apiStatus == MonitorStatus.UP) {
            recoveryPending = false;
            return true;
        }
        return false;
    }
}
