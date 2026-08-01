package com.dating.owoke.notification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import com.dating.owoke.notification.shared.messaging.service.IncomingEventProcessor;
import com.dating.owoke.notification.telegram.service.BotCommandService;
import com.dating.owoke.notification.telegram.domain.DateProposalCallback;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "owoke.delivery.enabled=false",
        "owoke.outbox.enabled=false"
})
class NotificationServiceApplicationTests {

    @Autowired
    private HealthEndpoint healthEndpoint;

    @Autowired
    private IncomingEventProcessor eventProcessor;

    @Autowired
    private BotCommandService botCommandService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE telegram_decision_requests, telegram_media_cache, telegram_updates,
                    outbox_events, failed_messages, inbox_events,
                    delivery_attempts, scheduled_notifications, notifications,
                    notification_preferences, contact_projections CASCADE
                """);
    }

    @Test
    void contextStartsAndHealthIsUp() {
        assertThat(healthEndpoint.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void duplicateDateEventCreatesOneNotificationAndOneDelivery() {
        UUID proposerId = UUID.randomUUID();
        UUID responderId = UUID.randomUUID();
        processUserRegistered(proposerId, "Alice", "alice@example.com");
        processUserRegistered(responderId, "Bob", "bob@example.com");

        UUID eventId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "proposalId", proposalId,
                "coupleId", UUID.randomUUID(),
                "proposerId", proposerId,
                "responderId", responderId,
                "scheduledAt", Instant.now().plusSeconds(172800),
                "timezone", "Europe/Moscow",
                "placeId", UUID.randomUUID(),
                "placeName", "Кафе",
                "placeAddress", "Казань",
                "description", "Вечернее свидание");
        String event = envelope(eventId, "DateProposalCreatedV1", proposalId, payload);

        eventProcessor.process("dating.events.v1", event);
        eventProcessor.process("dating.events.v1", event);

        assertThat(count("notifications")).isEqualTo(1);
        assertThat(count("delivery_attempts")).isEqualTo(1);
        assertThat(count("inbox_events")).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT channel FROM delivery_attempts", String.class)).isEqualTo("EMAIL");
    }

    @Test
    void dateV2CarriesCoverMediaIntoTelegramDelivery() {
        UUID proposerId = UUID.randomUUID();
        UUID responderId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        processUserRegistered(responderId, "Bob", "bob@example.com");
        UUID proposalId = UUID.randomUUID();
        Map<String, Object> payload = Map.ofEntries(
                Map.entry("proposalId", proposalId),
                Map.entry("coupleId", UUID.randomUUID()),
                Map.entry("proposerId", proposerId),
                Map.entry("responderId", responderId),
                Map.entry("scheduledAt", Instant.now().plusSeconds(172800)),
                Map.entry("timezone", "Europe/Moscow"),
                Map.entry("placeId", UUID.randomUUID()),
                Map.entry("placeName", "Кафе"),
                Map.entry("placeAddress", "Казань"),
                Map.entry("placeCoverMediaId", mediaId),
                Map.entry("description", "Вечернее свидание"));

        eventProcessor.process("dating.events.v1", envelope(
                UUID.randomUUID(), "DateProposalCreatedV2", 2, proposalId, payload));

        assertThat(jdbcTemplate.queryForObject("SELECT media_id FROM notifications", UUID.class))
                .isEqualTo(mediaId);
        assertThat(jdbcTemplate.queryForObject("SELECT type FROM notifications", String.class))
                .isEqualTo("DATE_PROPOSAL_CREATED");
    }

    @Test
    void eventDateV3CreatesAttributedNotificationWithCover() {
        UUID proposerId = UUID.randomUUID();
        UUID responderId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        processUserRegistered(responderId, "Bob", "bob@example.com");
        Map<String, Object> payload = Map.ofEntries(
                Map.entry("proposalId", proposalId),
                Map.entry("coupleId", UUID.randomUUID()),
                Map.entry("proposerId", proposerId),
                Map.entry("responderId", responderId),
                Map.entry("scheduledAt", Instant.now().plusSeconds(172800)),
                Map.entry("timezone", "Europe/Moscow"),
                Map.entry("selectionType", "EVENT"),
                Map.entry("placeName", "Театр Камала"),
                Map.entry("placeAddress", "Казань"),
                Map.entry("coverMediaId", mediaId),
                Map.entry("eventId", UUID.randomUUID()),
                Map.entry("eventOccurrenceId", UUID.randomUUID()),
                Map.entry("eventTitle", "Романтический спектакль"),
                Map.entry("eventSourceUrl", "https://kudago.com/kzn/event/test/"),
                Map.entry("eventPrice", "от 1000 ₽"),
                Map.entry("description", "Пойдём вместе"));

        eventProcessor.process("dating.events.v1", envelope(
                UUID.randomUUID(), "DateProposalCreatedV3", 3, proposalId, payload));

        assertThat(jdbcTemplate.queryForObject("SELECT media_id FROM notifications", UUID.class))
                .isEqualTo(mediaId);
        assertThat(jdbcTemplate.queryForObject("SELECT body FROM notifications", String.class))
                .contains("Романтический спектакль", "от 1000 ₽", "Источник: KudaGo",
                        "https://kudago.com/kzn/event/test/");
    }

    @Test
    void acceptedDateSchedulesFutureRemindersAndCancellationStopsThem() {
        UUID proposerId = UUID.randomUUID();
        UUID responderId = UUID.randomUUID();
        processUserRegistered(proposerId, "Alice", "alice@example.com");
        processUserRegistered(responderId, "Bob", "bob@example.com");
        UUID proposalId = UUID.randomUUID();
        Instant scheduledAt = Instant.now().plusSeconds(36 * 3600L);

        Map<String, Object> accepted = statusPayload(
                proposalId, proposerId, responderId, proposerId, scheduledAt, "ACCEPTED");
        eventProcessor.process("dating.events.v1", envelope(
                UUID.randomUUID(), "DateProposalAcceptedV1", proposalId, accepted));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM scheduled_notifications WHERE status = 'PENDING'", Integer.class))
                .isEqualTo(4);

        Map<String, Object> cancelled = statusPayload(
                proposalId, proposerId, responderId, proposerId, scheduledAt, "CANCELLED");
        eventProcessor.process("dating.events.v1", envelope(
                UUID.randomUUID(), "DateProposalCancelledV1", proposalId, cancelled));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM scheduled_notifications WHERE status = 'CANCELLED'", Integer.class))
                .isEqualTo(4);
    }

    @Test
    void identityEmailCommandUsesThePublishedWireEventType() {
        UUID userId = UUID.randomUUID();
        processUserRegistered(userId, "Alice", "alice@example.com");

        eventProcessor.process("notification.commands.v1", envelope(
                UUID.randomUUID(),
                "EmailVerificationRequestedV1",
                userId,
                Map.of(
                        "userId", userId,
                        "email", "alice@example.com",
                        "template", "EMAIL_VERIFICATION",
                        "actionUrl", "http://localhost:5173/verify-email?token=test")));

        assertThat(jdbcTemplate.queryForObject("SELECT type FROM notifications", String.class))
                .isEqualTo("EMAIL_VERIFICATION");
        assertThat(jdbcTemplate.queryForObject("SELECT channel FROM delivery_attempts", String.class))
                .isEqualTo("EMAIL");
    }

    @Test
    void telegramDateCallbackPublishesOneIdempotentDatingCommand() {
        UUID userId = UUID.randomUUID();
        long telegramUserId = 123456789L;
        processUserRegistered(userId, "Alice", "alice@example.com");
        eventProcessor.process("identity.events.v1", envelope(
                UUID.randomUUID(),
                "UserTelegramLinkedV1",
                userId,
                Map.of(
                        "userId", userId,
                        "telegramUserId", telegramUserId,
                        "username", "alice",
                        "botAccess", true)));

        UUID proposalId = UUID.randomUUID();
        UUID coupleId = UUID.randomUUID();
        botCommandService.handleDateProposalDecision(
                777L, telegramUserId, telegramUserId, 42L,
                new DateProposalCallback(proposalId, coupleId, "ACCEPT").encode());
        botCommandService.handleDateProposalDecision(
                777L, telegramUserId, telegramUserId, 42L,
                new DateProposalCallback(proposalId, coupleId, "ACCEPT").encode());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE event_type = 'DateProposalDecisionRequestedV1'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT topic FROM outbox_events WHERE event_type = 'DateProposalDecisionRequestedV1'",
                String.class)).isEqualTo("dating.commands.v1");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT payload FROM outbox_events WHERE event_type = 'DateProposalDecisionRequestedV1'",
                String.class)).contains(proposalId.toString(), coupleId.toString(), userId.toString(), "ACCEPT");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT event_key FROM outbox_events WHERE event_type = 'DateProposalDecisionRequestedV1'",
                String.class)).isEqualTo(coupleId.toString());
        assertThat(count("telegram_updates")).isEqualTo(1);
        assertThat(count("telegram_decision_requests")).isEqualTo(1);

        UUID requestId;
        try {
            String commandEnvelope = jdbcTemplate.queryForObject(
                    "SELECT payload FROM outbox_events WHERE event_type = 'DateProposalDecisionRequestedV1'",
                    String.class);
            requestId = UUID.fromString(objectMapper.readTree(commandEnvelope).path("eventId").asString());
        } catch (JacksonException exception) {
            throw new IllegalStateException(exception);
        }
        int deliveriesBeforeResult = count("delivery_attempts");
        eventProcessor.process("dating.events.v1", envelope(
                UUID.randomUUID(), "DateProposalDecisionResultV1", proposalId,
                Map.of(
                        "requestId", requestId,
                        "proposalId", proposalId,
                        "coupleId", coupleId,
                        "actorId", userId,
                        "decision", "ACCEPT",
                        "successful", true,
                        "errorCode", "")));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM telegram_decision_requests WHERE request_id = ?",
                String.class, requestId)).isEqualTo("READY");
        assertThat(count("delivery_attempts")).isEqualTo(deliveriesBeforeResult);
    }

    @Test
    void datingDecisionResultNotifiesTheTelegramActor() {
        UUID userId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        UUID coupleId = UUID.randomUUID();
        processUserRegistered(userId, "Alice", "alice@example.com");

        eventProcessor.process("dating.events.v1", envelope(
                UUID.randomUUID(),
                "DateProposalDecisionResultV1",
                proposalId,
                Map.of(
                        "requestId", UUID.randomUUID(),
                        "proposalId", proposalId,
                        "coupleId", coupleId,
                        "actorId", userId,
                        "decision", "ACCEPT",
                        "successful", true,
                        "errorCode", "")));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT title FROM notifications WHERE type = 'DATE_PROPOSAL_DECISION_RESULT'",
                String.class)).isEqualTo("Вы приняли свидание");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT reference_id FROM notifications WHERE type = 'DATE_PROPOSAL_DECISION_RESULT'",
                UUID.class)).isEqualTo(proposalId);
    }

    @Test
    void kafkaListenerDeduplicatesAtLeastOnceDelivery() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID registrationEventId = UUID.randomUUID();
        String registration = envelope(
                registrationEventId,
                "UserRegisteredV1",
                userId,
                Map.of("userId", userId, "displayName", "Kafka User", "email", "kafka@example.com"));

        kafkaTemplate.send("identity.events.v1", userId.toString(), registration).get();
        kafkaTemplate.send("identity.events.v1", userId.toString(), registration).get();

        waitUntil(() -> count("contact_projections") == 1 && count("inbox_events") == 1);
        assertThat(count("contact_projections")).isEqualTo(1);
        assertThat(count("inbox_events")).isEqualTo(1);
    }

    private void processUserRegistered(UUID userId, String name, String email) {
        eventProcessor.process("identity.events.v1", envelope(
                UUID.randomUUID(),
                "UserRegisteredV1",
                userId,
                Map.of("userId", userId, "displayName", name, "email", email)));
    }

    private Map<String, Object> statusPayload(
            UUID proposalId,
            UUID proposerId,
            UUID responderId,
            UUID changedBy,
            Instant scheduledAt,
            String status) {
        return Map.ofEntries(
                Map.entry("proposalId", proposalId),
                Map.entry("coupleId", UUID.randomUUID()),
                Map.entry("proposerId", proposerId),
                Map.entry("responderId", responderId),
                Map.entry("status", status),
                Map.entry("changedBy", changedBy),
                Map.entry("changedAt", Instant.now()),
                Map.entry("scheduledAt", scheduledAt),
                Map.entry("timezone", "Europe/Moscow"),
                Map.entry("placeName", "Кафе"),
                Map.entry("placeAddress", "Казань"),
                Map.entry("description", "Описание"));
    }

    private String envelope(UUID eventId, String type, UUID aggregateId, Object payload) {
        return envelope(eventId, type, 1, aggregateId, payload);
    }

    private String envelope(UUID eventId, String type, int version, UUID aggregateId, Object payload) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "eventId", eventId,
                    "eventType", type,
                    "eventVersion", version,
                    "aggregateId", aggregateId.toString(),
                    "occurredAt", Instant.now(),
                    "correlationId", UUID.randomUUID(),
                    "payload", payload));
        } catch (JacksonException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(10).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(100);
        }
    }
}
