package com.dating.owoke.notification.delivery.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.dating.owoke.notification.delivery.domain.DeliveryAttempt;

import jakarta.persistence.LockModeType;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select attempt from DeliveryAttempt attempt
            where attempt.status in (
                com.dating.owoke.notification.delivery.domain.DeliveryStatus.PENDING,
                com.dating.owoke.notification.delivery.domain.DeliveryStatus.PROCESSING)
              and attempt.nextAttemptAt <= :now
            order by attempt.nextAttemptAt
            """)
    List<DeliveryAttempt> lockPending(Instant now, Pageable pageable);

    boolean existsByNotificationIdAndChannel(UUID notificationId,
            com.dating.owoke.notification.delivery.domain.DeliveryChannel channel);

    boolean existsByNotificationIdAndChannelAndDestination(UUID notificationId,
            com.dating.owoke.notification.delivery.domain.DeliveryChannel channel, String destination);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select attempt from DeliveryAttempt attempt where attempt.status = "
            + "com.dating.owoke.notification.delivery.domain.DeliveryStatus.WAITING_RECEIPT "
            + "and attempt.nextAttemptAt <= :now order by attempt.nextAttemptAt")
    List<DeliveryAttempt> lockReceipts(Instant now, Pageable pageable);
}
