package com.dating.owoke.notification.shared.messaging.service;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.notification.contact.repository.ContactProjectionRepository;
import com.dating.owoke.notification.contact.service.ContactProjectionService;
import com.dating.owoke.notification.notification.service.NotificationService;
import com.dating.owoke.notification.reminder.service.ReminderService;
import com.dating.owoke.notification.shared.configuration.NotificationProperties;
import com.dating.owoke.notification.shared.messaging.domain.EventEnvelope;
import com.dating.owoke.notification.shared.messaging.domain.InboxEvent;
import com.dating.owoke.notification.shared.messaging.event.DateProposalCreatedV1;
import com.dating.owoke.notification.shared.messaging.event.DateProposalCreatedV2;
import com.dating.owoke.notification.shared.messaging.event.DateProposalCreatedV3;
import com.dating.owoke.notification.shared.messaging.event.DateProposalDecisionResultV1;
import com.dating.owoke.notification.shared.messaging.event.DateProposalStatusChangedV1;
import com.dating.owoke.notification.shared.messaging.event.DateProposalStatusChangedV2;
import com.dating.owoke.notification.shared.messaging.event.DateProposalStatusChangedV3;
import com.dating.owoke.notification.shared.messaging.event.EmailNotificationRequestedV1;
import com.dating.owoke.notification.shared.messaging.event.UserProfileUpdatedV1;
import com.dating.owoke.notification.shared.messaging.event.UserRegisteredV1;
import com.dating.owoke.notification.shared.messaging.event.UserTelegramLinkedV1;
import com.dating.owoke.notification.shared.messaging.repository.InboxEventRepository;
import com.dating.owoke.notification.telegram.service.TelegramDecisionService;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class IncomingEventProcessor {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final InboxEventRepository inboxRepository;
    private final ContactProjectionRepository contactRepository;
    private final ContactProjectionService contactService;
    private final NotificationService notificationService;
    private final ReminderService reminderService;
    private final TelegramDecisionService telegramDecisionService;
    private final NotificationProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public IncomingEventProcessor(
            InboxEventRepository inboxRepository,
            ContactProjectionRepository contactRepository,
            ContactProjectionService contactService,
            NotificationService notificationService,
            ReminderService reminderService,
            TelegramDecisionService telegramDecisionService,
            NotificationProperties properties,
            ObjectMapper objectMapper,
            Clock clock) {
        this.inboxRepository = inboxRepository;
        this.contactRepository = contactRepository;
        this.contactService = contactService;
        this.notificationService = notificationService;
        this.reminderService = reminderService;
        this.telegramDecisionService = telegramDecisionService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void process(String topic, String message) {
        EventEnvelope envelope = deserialize(message, EventEnvelope.class);
        int expectedVersion = switch (envelope.eventType()) {
            case "DateProposalCreatedV3", "DateProposalAcceptedV3",
                    "DateProposalDeclinedV3", "DateProposalCancelledV3" -> 3;
            case "DateProposalCreatedV2", "DateProposalAcceptedV2",
                    "DateProposalDeclinedV2", "DateProposalCancelledV2" -> 2;
            default -> 1;
        };
        if (envelope.eventVersion() != expectedVersion) {
            throw new IllegalArgumentException("Unsupported event version: " + envelope.eventVersion());
        }
        if (inboxRepository.existsById(envelope.eventId())) {
            return;
        }

        route(envelope);
        inboxRepository.saveAndFlush(new InboxEvent(
                envelope.eventId(), envelope.eventType(), topic, clock.instant()));
    }

    private void route(EventEnvelope envelope) {
        switch (envelope.eventType()) {
            case "UserRegisteredV1" -> register(payload(envelope, UserRegisteredV1.class));
            case "UserProfileUpdatedV1" -> profileUpdated(payload(envelope, UserProfileUpdatedV1.class));
            case "UserTelegramLinkedV1" -> telegramLinked(envelope, payload(envelope, UserTelegramLinkedV1.class));
            case "EmailVerificationRequestedV1", "PasswordResetRequestedV1" -> emailRequested(envelope, payload(
                    envelope, EmailNotificationRequestedV1.class));
            case "DateProposalCreatedV1" -> proposalCreated(envelope, payload(
                    envelope, DateProposalCreatedV1.class));
            case "DateProposalCreatedV2" -> proposalCreated(envelope, payload(
                    envelope, DateProposalCreatedV2.class));
            case "DateProposalCreatedV3" -> proposalCreated(envelope, payload(
                    envelope, DateProposalCreatedV3.class));
            case "DateProposalAcceptedV1", "DateProposalDeclinedV1", "DateProposalCancelledV1" ->
                    proposalStatusChanged(envelope, payload(envelope, DateProposalStatusChangedV1.class));
            case "DateProposalAcceptedV2", "DateProposalDeclinedV2", "DateProposalCancelledV2" ->
                    proposalStatusChanged(envelope, payload(envelope, DateProposalStatusChangedV2.class));
            case "DateProposalAcceptedV3", "DateProposalDeclinedV3", "DateProposalCancelledV3" ->
                    proposalStatusChanged(envelope, payload(envelope, DateProposalStatusChangedV3.class));
            case "DateProposalDecisionResultV1" -> proposalDecisionResult(
                    envelope, payload(envelope, DateProposalDecisionResultV1.class));
            case "CoupleActivatedV1", "CoupleClosedV1" -> {
                // Reserved for later couple-specific notifications. Recording it in the inbox is intentional.
            }
            default -> throw new IllegalArgumentException("Unsupported event type: " + envelope.eventType());
        }
    }

    private void register(UserRegisteredV1 event) {
        contactService.register(event.userId(), event.displayName(), event.email());
    }

    private void profileUpdated(UserProfileUpdatedV1 event) {
        contactService.updateProfile(event.userId(), event.displayName());
    }

    private void telegramLinked(EventEnvelope envelope, UserTelegramLinkedV1 event) {
        contactService.linkTelegram(
                event.userId(),
                event.telegramUserId(),
                event.botAccess() ? event.telegramUserId() : null,
                event.username(),
                event.botAccess());
        if (event.botAccess()) {
            notificationService.create(
                    envelope.eventId(),
                    event.userId(),
                    "TELEGRAM_LINKED",
                    "Telegram подключён",
                    "Теперь бот For my L сможет присылать уведомления о свиданиях.",
                    properties.webAppUrl() + "/settings");
        }
    }

    private void emailRequested(EventEnvelope envelope, EmailNotificationRequestedV1 event) {
        if (!contactRepository.existsById(event.userId())) {
            contactService.register(event.userId(), event.email(), event.email());
        }
        String title = "PASSWORD_RESET".equals(event.template())
                ? "Восстановление пароля For my L"
                : "Подтверждение email For my L";
        notificationService.create(
                envelope.eventId(),
                event.userId(),
                event.template(),
                title,
                "Откройте ссылку, чтобы завершить действие в For my L.",
                event.actionUrl());
    }

    private void proposalCreated(EventEnvelope envelope, DateProposalCreatedV1 event) {
        notificationService.create(
                envelope.eventId(),
                event.responderId(),
                "DATE_PROPOSAL_CREATED",
                "Новое предложение свидания",
                proposalBody(event.scheduledAt(), event.placeName(), event.placeAddress(), event.description()),
                dateUrl(event.proposalId()),
                event.proposalId(),
                event.coupleId());
    }

    private void proposalCreated(EventEnvelope envelope, DateProposalCreatedV2 event) {
        notificationService.create(
                envelope.eventId(), event.responderId(), "DATE_PROPOSAL_CREATED",
                "Новое предложение свидания 💌",
                proposalBody(event.scheduledAt(), event.placeName(), event.placeAddress(), event.description()),
                dateUrl(event.proposalId()), event.proposalId(), event.coupleId(), event.placeCoverMediaId());
    }

    private void proposalCreated(EventEnvelope envelope, DateProposalCreatedV3 event) {
        notificationService.create(
                envelope.eventId(), event.responderId(), "DATE_PROPOSAL_CREATED",
                "Новое предложение свидания 💌",
                proposalBody(event.scheduledAt(), event.placeName(), event.placeAddress(),
                        event.description(), event.selectionType(), event.eventTitle(),
                        event.eventPrice(), event.eventSourceUrl()),
                dateUrl(event.proposalId()), event.proposalId(), event.coupleId(), event.coverMediaId());
    }

    private void proposalStatusChanged(EventEnvelope envelope, DateProposalStatusChangedV1 event) {
        UUID recipient;
        String title;
        switch (event.status()) {
            case "ACCEPTED" -> {
                recipient = event.proposerId();
                title = "Свидание подтверждено";
            }
            case "DECLINED" -> {
                recipient = event.proposerId();
                title = "Предложение отклонено";
            }
            case "CANCELLED" -> {
                recipient = event.changedBy().equals(event.proposerId())
                        ? event.responderId()
                        : event.proposerId();
                title = "Свидание отменено";
            }
            default -> throw new IllegalArgumentException("Unsupported proposal status: " + event.status());
        }
        notificationService.create(
                envelope.eventId(),
                recipient,
                "DATE_PROPOSAL_" + event.status(),
                title,
                proposalBody(event.scheduledAt(), event.placeName(), event.placeAddress(), event.description()),
                dateUrl(event.proposalId()), event.proposalId(), event.coupleId());
        if (!recipient.equals(event.changedBy())) {
            notificationService.create(
                    envelope.eventId(), event.changedBy(), "DATE_PROPOSAL_" + event.status(), title,
                    proposalBody(event.scheduledAt(), event.placeName(), event.placeAddress(), event.description()),
                    dateUrl(event.proposalId()), event.proposalId(), event.coupleId());
        }
        if ("ACCEPTED".equals(event.status())) {
            reminderService.schedule(envelope, event);
        } else if ("CANCELLED".equals(event.status())) {
            reminderService.cancel(event.proposalId());
        }
    }

    private void proposalStatusChanged(EventEnvelope envelope, DateProposalStatusChangedV2 event) {
        UUID recipient;
        String title;
        switch (event.status()) {
            case "ACCEPTED" -> {
                recipient = event.proposerId();
                title = "Свидание подтверждено 💞";
            }
            case "DECLINED" -> {
                recipient = event.proposerId();
                title = "Предложение отклонено";
            }
            case "CANCELLED" -> {
                recipient = event.changedBy().equals(event.proposerId())
                        ? event.responderId() : event.proposerId();
                title = "Свидание отменено";
            }
            default -> throw new IllegalArgumentException("Unsupported proposal status: " + event.status());
        }
        notificationService.create(
                envelope.eventId(), recipient, "DATE_PROPOSAL_" + event.status(), title,
                proposalBody(event.scheduledAt(), event.placeName(), event.placeAddress(), event.description()),
                dateUrl(event.proposalId()), event.proposalId(), event.coupleId(), event.placeCoverMediaId());
        if (!recipient.equals(event.changedBy())) {
            notificationService.create(
                    envelope.eventId(), event.changedBy(), "DATE_PROPOSAL_" + event.status(), title,
                    proposalBody(event.scheduledAt(), event.placeName(), event.placeAddress(), event.description()),
                    dateUrl(event.proposalId()), event.proposalId(), event.coupleId(), event.placeCoverMediaId());
        }
        if ("ACCEPTED".equals(event.status())) {
            reminderService.schedule(envelope, event);
        } else if ("CANCELLED".equals(event.status())) {
            reminderService.cancel(event.proposalId());
        }
    }

    private void proposalStatusChanged(EventEnvelope envelope, DateProposalStatusChangedV3 event) {
        UUID recipient;
        String title;
        switch (event.status()) {
            case "ACCEPTED" -> {
                recipient = event.proposerId();
                title = "Свидание подтверждено 💞";
            }
            case "DECLINED" -> {
                recipient = event.proposerId();
                title = "Предложение отклонено";
            }
            case "CANCELLED" -> {
                recipient = event.changedBy().equals(event.proposerId())
                        ? event.responderId() : event.proposerId();
                title = "Свидание отменено";
            }
            default -> throw new IllegalArgumentException("Unsupported proposal status: " + event.status());
        }
        notificationService.create(
                envelope.eventId(), recipient, "DATE_PROPOSAL_" + event.status(), title,
                proposalBody(event.scheduledAt(), event.placeName(), event.placeAddress(),
                        event.description(), event.selectionType(), event.eventTitle(),
                        event.eventPrice(), event.eventSourceUrl()),
                dateUrl(event.proposalId()), event.proposalId(), event.coupleId(), event.coverMediaId());
        if (!recipient.equals(event.changedBy())) {
            notificationService.create(
                    envelope.eventId(), event.changedBy(), "DATE_PROPOSAL_" + event.status(), title,
                    proposalBody(event.scheduledAt(), event.placeName(), event.placeAddress(),
                            event.description(), event.selectionType(), event.eventTitle(),
                            event.eventPrice(), event.eventSourceUrl()),
                    dateUrl(event.proposalId()), event.proposalId(), event.coupleId(), event.coverMediaId());
        }
        if ("ACCEPTED".equals(event.status())) {
            reminderService.schedule(envelope, event);
        } else if ("CANCELLED".equals(event.status())) {
            reminderService.cancel(event.proposalId());
        }
    }

    private void proposalDecisionResult(EventEnvelope envelope, DateProposalDecisionResultV1 event) {
        String title;
        String body;
        if (event.successful()) {
            boolean accepted = "ACCEPT".equals(event.decision());
            title = accepted ? "Свидание подтверждено 💞" : "Предложение отклонено";
            body = accepted
                    ? "Свидание подтверждено. Все детали ждут вас в For my L."
                    : "Предложение отклонено. Детали доступны в For my L.";
        } else {
            title = "Не удалось изменить свидание";
            body = switch (event.errorCode()) {
                case "PROPOSAL_NOT_FOUND" -> "Предложение не найдено или больше недоступно.";
                case "ACTION_NOT_ALLOWED" -> "Это действие уже недоступно для текущего статуса свидания.";
                case "COUPLE_NOT_ACTIVE" -> "Пара больше не активна.";
                default -> "Повторите действие на сайте For my L.";
            };
        }
        String actionUrl = dateUrl(event.proposalId());
        boolean originalCardWillBeEdited = telegramDecisionService.result(
                event.requestId(), event.actorId(), title);
        if (originalCardWillBeEdited) {
            notificationService.createInAppOnly(
                    envelope.eventId(), event.actorId(), "DATE_PROPOSAL_DECISION_RESULT",
                    title, body, actionUrl, event.proposalId(), event.coupleId());
        } else {
            notificationService.create(
                    envelope.eventId(), event.actorId(), "DATE_PROPOSAL_DECISION_RESULT",
                    title, body, actionUrl, event.proposalId(), event.coupleId());
        }
    }

    private String proposalBody(java.time.Instant scheduledAt, String place, String address, String description) {
        ZonedDateTime local = scheduledAt.atZone(MOSCOW);
        StringBuilder body = new StringBuilder()
                .append("📅 Дата: ").append(DATE_FORMAT.format(local))
                .append("\n⏰ Время: ").append(TIME_FORMAT.format(local))
                .append("\n📍 ").append(location(place, address));
        if (description != null && !description.isBlank()) {
            body.append("\n\n💭 ").append(description.strip());
        }
        return body.toString();
    }

    private String proposalBody(
            java.time.Instant scheduledAt,
            String place,
            String address,
            String description,
            String selectionType,
            String eventTitle,
            String eventPrice,
            String eventSourceUrl) {
        if (!"EVENT".equals(selectionType)) {
            return proposalBody(scheduledAt, place, address, description);
        }
        ZonedDateTime local = scheduledAt.atZone(MOSCOW);
        StringBuilder body = new StringBuilder()
                .append("📅 Дата: ").append(DATE_FORMAT.format(local))
                .append("\n⏰ Время: ").append(TIME_FORMAT.format(local))
                .append("\n🎟 ").append(eventTitle)
                .append("\n📍 ").append(location(place, address));
        if (eventPrice != null && !eventPrice.isBlank()) {
            body.append("\n💳 ").append(eventPrice);
        }
        if (description != null && !description.isBlank()) {
            body.append("\n\n💭 ").append(description.strip());
        }
        if (eventSourceUrl != null && !eventSourceUrl.isBlank()) {
            body.append("\nИсточник: KudaGo — ").append(eventSourceUrl);
        }
        return body.toString();
    }

    private static String location(String place, String address) {
        String normalizedPlace = place == null ? "" : place.strip();
        String normalizedAddress = address == null ? "" : address.strip();
        if (normalizedPlace.isEmpty()) return normalizedAddress;
        if (normalizedAddress.isEmpty() || normalizedPlace.equalsIgnoreCase(normalizedAddress)) {
            return normalizedPlace;
        }
        return normalizedPlace + ", " + normalizedAddress;
    }

    private String dateUrl(UUID proposalId) {
        return properties.webAppUrl() + "/dates/" + proposalId;
    }

    private <T> T payload(EventEnvelope envelope, Class<T> type) {
        try {
            return objectMapper.treeToValue(envelope.payload(), type);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid payload for " + envelope.eventType(), exception);
        }
    }

    private <T> T deserialize(String message, Class<T> type) {
        try {
            return objectMapper.readValue(message, type);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid event envelope", exception);
        }
    }
}
