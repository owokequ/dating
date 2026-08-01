package com.dating.owoke.notification.reminder.service;

import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.notification.notification.service.NotificationService;
import com.dating.owoke.notification.reminder.domain.ReminderType;
import com.dating.owoke.notification.reminder.domain.ReminderPayload;
import com.dating.owoke.notification.reminder.domain.ScheduledNotification;
import com.dating.owoke.notification.reminder.repository.ScheduledNotificationRepository;
import com.dating.owoke.notification.shared.configuration.NotificationProperties;
import com.dating.owoke.notification.shared.messaging.domain.EventEnvelope;
import com.dating.owoke.notification.shared.messaging.event.DateProposalStatusChangedV1;
import com.dating.owoke.notification.shared.messaging.event.DateProposalStatusChangedV2;
import com.dating.owoke.notification.shared.messaging.event.DateProposalStatusChangedV3;

@Service
public class ReminderService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Moscow"));

    private final ScheduledNotificationRepository repository;
    private final NotificationService notificationService;
    private final NotificationProperties properties;
    private final Clock clock;

    public ReminderService(
            ScheduledNotificationRepository repository,
            NotificationService notificationService,
            NotificationProperties properties,
            Clock clock) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.properties = properties;
        this.clock = clock;
    }

    public void schedule(EventEnvelope envelope, DateProposalStatusChangedV1 event) {
        schedule(envelope, event.proposalId(), event.proposerId(), event.responderId(),
                event.scheduledAt(), event.placeName(), event.placeAddress(), null);
    }

    public void schedule(EventEnvelope envelope, DateProposalStatusChangedV2 event) {
        schedule(envelope, event.proposalId(), event.proposerId(), event.responderId(),
                event.scheduledAt(), event.placeName(), event.placeAddress(), event.placeCoverMediaId());
    }

    public void schedule(EventEnvelope envelope, DateProposalStatusChangedV3 event) {
        String subject = "EVENT".equals(event.selectionType()) && event.eventTitle() != null
                ? event.eventTitle()
                : event.placeName();
        schedule(envelope, event.proposalId(), event.proposerId(), event.responderId(),
                event.scheduledAt(), subject, event.placeAddress(), event.coverMediaId());
    }

    private void schedule(
            EventEnvelope envelope,
            UUID proposalId,
            UUID proposerId,
            UUID responderId,
            java.time.Instant scheduledAt,
            String placeName,
            String placeAddress,
            UUID mediaId) {
        for (UUID userId : new UUID[] {proposerId, responderId}) {
            for (ReminderType type : ReminderType.values()) {
                java.time.Instant scheduledFor = scheduledAt.minus(type.beforeDate());
                if (scheduledFor.isAfter(clock.instant())) {
                    repository.save(new ScheduledNotification(
                            envelope.eventId(),
                            proposalId,
                            userId,
                            type,
                            scheduledFor,
                            new ReminderPayload(
                                    DATE_FORMAT.format(scheduledAt) + " — " + placeName + ", " + placeAddress,
                                    properties.webAppUrl() + "/dates/" + proposalId,
                                    mediaId),
                            clock.instant()));
                }
            }
        }
    }

    public void cancel(UUID proposalId) {
        repository.cancelPending(proposalId, clock.instant());
    }

    @Scheduled(fixedDelayString = "${owoke.notification.reminder-fixed-delay:30000}")
    @Transactional
    public void createDueNotifications() {
        for (ScheduledNotification reminder : repository.lockDue(clock.instant(), PageRequest.of(0, 50))) {
            ReminderPayload payload = reminder.getPayload();
            notificationService.create(
                    reminder.getSourceEventId(),
                    reminder.getUserId(),
                    "DATE_REMINDER_" + reminder.getReminderType().name(),
                    "Скоро свидание",
                    payload.body(),
                    payload.actionUrl(),
                    null,
                    null,
                    payload.mediaId());
            reminder.markCreated(clock.instant());
        }
    }

}
