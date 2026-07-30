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
        for (UUID userId : new UUID[] {event.proposerId(), event.responderId()}) {
            for (ReminderType type : ReminderType.values()) {
                java.time.Instant scheduledFor = event.scheduledAt().minus(type.beforeDate());
                if (scheduledFor.isAfter(clock.instant())) {
                    repository.save(new ScheduledNotification(
                            envelope.eventId(),
                            event.proposalId(),
                            userId,
                            type,
                            scheduledFor,
                            new ReminderPayload(
                                    DATE_FORMAT.format(event.scheduledAt()) + " — " + event.placeName()
                                            + ", " + event.placeAddress(),
                                    properties.webAppUrl() + "/dates/" + event.proposalId()),
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
                    payload.actionUrl());
            reminder.markCreated(clock.instant());
        }
    }

}
