package com.dating.owoke.notification.reminder.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.notification.notification.service.NotificationService;
import com.dating.owoke.notification.reminder.domain.DateReminderContext;
import com.dating.owoke.notification.reminder.domain.DateReminderContextId;
import com.dating.owoke.notification.reminder.domain.ReminderPayload;
import com.dating.owoke.notification.reminder.domain.ReminderType;
import com.dating.owoke.notification.reminder.domain.ScheduledNotification;
import com.dating.owoke.notification.reminder.repository.DateReminderContextRepository;
import com.dating.owoke.notification.reminder.repository.ScheduledNotificationRepository;
import com.dating.owoke.notification.shared.configuration.NotificationProperties;
import com.dating.owoke.notification.shared.messaging.domain.EventEnvelope;
import com.dating.owoke.notification.shared.messaging.event.DateProposalStatusChangedV1;
import com.dating.owoke.notification.shared.messaging.event.DateProposalStatusChangedV2;
import com.dating.owoke.notification.shared.messaging.event.DateProposalStatusChangedV3;

@Service
public class ReminderService {

    private static final int MINUTES_MIN = 5;
    private static final int MINUTES_MAX = 7 * 24 * 60;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Moscow"));

    private final ScheduledNotificationRepository repository;
    private final DateReminderContextRepository contextRepository;
    private final NotificationService notificationService;
    private final NotificationProperties properties;
    private final Clock clock;

    public ReminderService(ScheduledNotificationRepository repository,
            DateReminderContextRepository contextRepository,
            NotificationService notificationService,
            NotificationProperties properties,
            Clock clock) {
        this.repository = repository;
        this.contextRepository = contextRepository;
        this.notificationService = notificationService;
        this.properties = properties;
        this.clock = clock;
    }

    public void schedule(EventEnvelope envelope, DateProposalStatusChangedV1 event) {
        requestPersonalChoice(envelope, event.proposalId(), event.coupleId(), event.proposerId(), event.responderId(),
                event.scheduledAt(), event.placeName(), event.placeAddress(), null);
    }

    public void schedule(EventEnvelope envelope, DateProposalStatusChangedV2 event) {
        requestPersonalChoice(envelope, event.proposalId(), event.coupleId(), event.proposerId(), event.responderId(),
                event.scheduledAt(), event.placeName(), event.placeAddress(), event.placeCoverMediaId());
    }

    public void schedule(EventEnvelope envelope, DateProposalStatusChangedV3 event) {
        String subject = "EVENT".equals(event.selectionType()) && event.eventTitle() != null
                ? event.eventTitle() : event.placeName();
        requestPersonalChoice(envelope, event.proposalId(), event.coupleId(), event.proposerId(), event.responderId(),
                event.scheduledAt(), subject, event.placeAddress(), event.coverMediaId());
    }

    private void requestPersonalChoice(EventEnvelope envelope, UUID proposalId, UUID coupleId, UUID proposerId,
            UUID responderId, Instant scheduledAt, String placeName, String placeAddress, UUID mediaId) {
        String body = DATE_FORMAT.format(scheduledAt) + " — " + placeName
                + (placeAddress == null || placeAddress.isBlank() ? "" : ", " + placeAddress);
        String actionUrl = properties.webAppUrl() + "/dates/" + proposalId;
        for (UUID userId : new UUID[] { proposerId, responderId }) {
            contextRepository.save(new DateReminderContext(proposalId, userId, coupleId, scheduledAt, body, actionUrl, mediaId));
            notificationService.create(envelope.eventId(), userId, "DATE_REMINDER_SELECTION",
                    "Когда напомнить о свидании?", body, actionUrl, proposalId, coupleId, mediaId);
        }
    }

    @Transactional
    public String configure(UUID proposalId, UUID userId, int minutes) {
        if (minutes < MINUTES_MIN || minutes > MINUTES_MAX) {
            throw new IllegalArgumentException("Введите число от 5 до 10080 минут");
        }
        DateReminderContext context = contextRepository.findById(new DateReminderContextId(proposalId, userId))
                .orElseThrow(() -> new IllegalArgumentException("Настройка напоминания недоступна"));
        Instant scheduledFor = context.getScheduledAt().minusSeconds(minutes * 60L);
        if (!scheduledFor.isAfter(clock.instant())) {
            throw new IllegalArgumentException("Это время уже прошло — выберите более короткий интервал");
        }
        ReminderPayload payload = new ReminderPayload(context.getBody(), context.getActionUrl(), context.getMediaId());
        ScheduledNotification reminder = repository.findByProposalIdAndUserIdAndReminderType(
                        proposalId, userId, ReminderType.PERSONAL)
                .orElseGet(() -> new ScheduledNotification(UUID.randomUUID(), proposalId, userId,
                        ReminderType.PERSONAL, scheduledFor, payload, clock.instant()));
        reminder.reschedule(scheduledFor, payload, clock.instant());
        repository.save(reminder);
        return "Напомню за " + readableMinutes(minutes) + ".";
    }

    @Transactional
    public String disable(UUID proposalId, UUID userId) {
        repository.findByProposalIdAndUserIdAndReminderType(proposalId, userId, ReminderType.PERSONAL)
                .ifPresent(reminder -> reminder.cancel(clock.instant()));
        return "Личное напоминание отключено.";
    }

    @Transactional
    public void cancel(UUID proposalId) {
        repository.cancelPending(proposalId, clock.instant());
        contextRepository.deleteByProposalId(proposalId);
    }

    @Scheduled(fixedDelayString = "${owoke.notification.reminder-fixed-delay:30000}")
    @Transactional
    public void createDueNotifications() {
        for (ScheduledNotification reminder : repository.lockDue(clock.instant(), PageRequest.of(0, 50))) {
            ReminderPayload payload = reminder.getPayload();
            notificationService.create(reminder.getSourceEventId(), reminder.getUserId(),
                    "DATE_REMINDER_PERSONAL", "Скоро свидание", payload.body(), payload.actionUrl(),
                    reminder.getProposalId(), null, payload.mediaId());
            reminder.markCreated(clock.instant());
        }
    }

    private static String readableMinutes(int minutes) {
        if (minutes % 60 == 0) return minutes / 60 + " ч.";
        return minutes + " мин.";
    }
}
