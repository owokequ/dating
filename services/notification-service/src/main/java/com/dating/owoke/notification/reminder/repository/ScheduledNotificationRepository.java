package com.dating.owoke.notification.reminder.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.dating.owoke.notification.reminder.domain.ScheduledNotification;

import jakarta.persistence.LockModeType;

public interface ScheduledNotificationRepository extends JpaRepository<ScheduledNotification, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select reminder from ScheduledNotification reminder
            where reminder.status = com.dating.owoke.notification.reminder.domain.ScheduledNotificationStatus.PENDING
              and reminder.scheduledFor <= :now
            order by reminder.scheduledFor
            """)
    List<ScheduledNotification> lockDue(Instant now, Pageable pageable);

    @Modifying
    @Query("""
            update ScheduledNotification reminder
            set reminder.status = com.dating.owoke.notification.reminder.domain.ScheduledNotificationStatus.CANCELLED,
                reminder.completedAt = :now
            where reminder.proposalId = :proposalId
              and reminder.status = com.dating.owoke.notification.reminder.domain.ScheduledNotificationStatus.PENDING
            """)
    int cancelPending(UUID proposalId, Instant now);
}
