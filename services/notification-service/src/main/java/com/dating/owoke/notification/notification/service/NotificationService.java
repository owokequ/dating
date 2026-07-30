package com.dating.owoke.notification.notification.service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.notification.contact.domain.ContactProjection;
import com.dating.owoke.notification.contact.service.ContactProjectionService;
import com.dating.owoke.notification.delivery.domain.DeliveryAttempt;
import com.dating.owoke.notification.delivery.domain.DeliveryChannel;
import com.dating.owoke.notification.delivery.repository.DeliveryAttemptRepository;
import com.dating.owoke.notification.notification.domain.Notification;
import com.dating.owoke.notification.notification.exception.NotificationNotFoundException;
import com.dating.owoke.notification.notification.repository.NotificationRepository;
import com.dating.owoke.notification.preference.domain.NotificationPreference;
import com.dating.owoke.notification.preference.repository.NotificationPreferenceRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeliveryAttemptRepository deliveryRepository;
    private final ContactProjectionService contactService;
    private final NotificationPreferenceRepository preferenceRepository;
    private final Clock clock;

    public NotificationService(
            NotificationRepository notificationRepository,
            DeliveryAttemptRepository deliveryRepository,
            ContactProjectionService contactService,
            NotificationPreferenceRepository preferenceRepository,
            Clock clock) {
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.contactService = contactService;
        this.preferenceRepository = preferenceRepository;
        this.clock = clock;
    }

    public Notification create(
            UUID sourceEventId,
            UUID userId,
            String type,
            String title,
            String body,
            String actionUrl) {
        ContactProjection contact = contactService.required(userId);
        NotificationPreference preference = preferenceRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Notification preferences are missing for " + userId));
        Notification notification = notificationRepository.save(
                new Notification(sourceEventId, userId, type, title, body, actionUrl, clock.instant()));

        DeliveryChannel channel = selectChannel(contact, preference);
        if (channel != null) {
            deliveryRepository.save(new DeliveryAttempt(notification.getId(), channel, clock.instant()));
        }
        return notification;
    }

    @Transactional(readOnly = true)
    public List<Notification> list(UUID userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, safeLimit));
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(NotificationNotFoundException::new);
        notification.markRead(clock.instant());
    }

    private DeliveryChannel selectChannel(ContactProjection contact, NotificationPreference preference) {
        if (preference.isTelegramEnabled() && contact.hasBotAccess() && contact.getTelegramChatId() != null) {
            return DeliveryChannel.TELEGRAM;
        }
        if (preference.isEmailEnabled() && contact.getEmail() != null) {
            return DeliveryChannel.EMAIL;
        }
        return null;
    }
}
