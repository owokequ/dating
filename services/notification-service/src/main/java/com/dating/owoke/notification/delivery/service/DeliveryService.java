package com.dating.owoke.notification.delivery.service;

import java.time.Clock;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.notification.contact.domain.ContactProjection;
import com.dating.owoke.notification.contact.service.ContactProjectionService;
import com.dating.owoke.notification.delivery.domain.DeliveryAttempt;
import com.dating.owoke.notification.delivery.domain.DeliveryChannel;
import com.dating.owoke.notification.delivery.repository.DeliveryAttemptRepository;
import com.dating.owoke.notification.notification.domain.Notification;
import com.dating.owoke.notification.notification.repository.NotificationRepository;
import com.dating.owoke.notification.preference.domain.NotificationPreference;
import com.dating.owoke.notification.preference.repository.NotificationPreferenceRepository;

@Service
public class DeliveryService {

    private static final int BATCH_SIZE = 25;

    private final DeliveryAttemptRepository deliveryRepository;
    private final NotificationRepository notificationRepository;
    private final ContactProjectionService contactService;
    private final NotificationPreferenceRepository preferenceRepository;
    private final Clock clock;

    public DeliveryService(
            DeliveryAttemptRepository deliveryRepository,
            NotificationRepository notificationRepository,
            ContactProjectionService contactService,
            NotificationPreferenceRepository preferenceRepository,
            Clock clock) {
        this.deliveryRepository = deliveryRepository;
        this.notificationRepository = notificationRepository;
        this.contactService = contactService;
        this.preferenceRepository = preferenceRepository;
        this.clock = clock;
    }

    @Transactional
    public List<DeliveryTask> claim() {
        return deliveryRepository.lockPending(clock.instant(), PageRequest.of(0, BATCH_SIZE)).stream()
                .map(this::claim)
                .toList();
    }

    @Transactional
    public void markSent(DeliveryTask task, String providerMessageId) {
        DeliveryAttempt attempt = deliveryRepository.findById(task.attemptId())
                .orElseThrow(() -> new IllegalStateException("Delivery attempt disappeared"));
        attempt.markSent(providerMessageId, clock.instant());
    }

    @Transactional
    public void markPushAccepted(DeliveryTask task, String ticketId) {
        deliveryRepository.findById(task.attemptId())
                .orElseThrow(() -> new IllegalStateException("Delivery attempt disappeared"))
                .markWaitingForReceipt(ticketId, clock.instant());
    }

    @Transactional
    public List<DeliveryAttempt> claimPushReceipts() {
        return deliveryRepository.lockReceipts(clock.instant(), PageRequest.of(0, 100));
    }

    @Transactional
    public void markPushDelivered(java.util.UUID attemptId) {
        deliveryRepository.findById(attemptId).orElseThrow().markDelivered(clock.instant());
    }

    @Transactional
    public void markPushReceiptFailed(java.util.UUID attemptId, Exception exception) {
        deliveryRepository.findById(attemptId).orElseThrow().markFailed(exception, clock.instant());
    }

    @Transactional
    public void markPushDeviceDisabled(DeliveryTask task, String error) {
        deliveryRepository.findById(task.attemptId()).orElseThrow().markDisabled(error, clock.instant());
    }

    @Transactional
    public void markPushDeviceDisabled(java.util.UUID attemptId, String error) {
        deliveryRepository.findById(attemptId).orElseThrow().markDisabled(error, clock.instant());
    }

    @Transactional
    public void markPushPermanentlyFailed(DeliveryTask task, String error) {
        deliveryRepository.findById(task.attemptId()).orElseThrow().markPermanentlyFailed(error, clock.instant());
    }

    @Transactional
    public void markFailed(DeliveryTask task, Exception exception) {
        DeliveryAttempt attempt = deliveryRepository.findById(task.attemptId())
                .orElseThrow(() -> new IllegalStateException("Delivery attempt disappeared"));
        attempt.markFailed(exception, clock.instant());
        if (attempt.isFailedPermanently() && attempt.getChannel() == DeliveryChannel.TELEGRAM) {
            createEmailFallback(attempt.getNotificationId());
        }
    }

    private DeliveryTask claim(DeliveryAttempt attempt) {
        Notification notification = notificationRepository.findById(attempt.getNotificationId())
                .orElseThrow(() -> new IllegalStateException("Notification is missing for delivery"));
        ContactProjection contact = contactService.required(notification.getUserId());
        attempt.markProcessing(clock.instant());
        return new DeliveryTask(
                attempt.getId(),
                notification.getUserId(),
                attempt.getChannel(),
                contact.getTelegramChatId(),
                contact.getEmail(),
                notification.getTitle(),
                notification.getBody(),
                notification.getActionUrl(),
                notification.getType(),
                notification.getReferenceId(),
                notification.getContextId(),
                notification.getMediaId(),
                attempt.getDestination());
    }

    private void createEmailFallback(java.util.UUID notificationId) {
        if (deliveryRepository.existsByNotificationIdAndChannel(notificationId, DeliveryChannel.EMAIL)) {
            return;
        }
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalStateException("Notification is missing for fallback"));
        if ("SITE_AVAILABLE".equals(notification.getType())) {
            return;
        }
        ContactProjection contact = contactService.required(notification.getUserId());
        NotificationPreference preference = preferenceRepository.findById(notification.getUserId())
                .orElseThrow(() -> new IllegalStateException("Preferences are missing for fallback"));
        if (preference.isEmailEnabled() && contact.getEmail() != null) {
            deliveryRepository.save(new DeliveryAttempt(notificationId, DeliveryChannel.EMAIL, clock.instant()));
        }
    }
}
