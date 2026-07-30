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
                TRUNCATE TABLE telegram_updates, outbox_events, failed_messages, inbox_events,
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
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "eventId", eventId,
                    "eventType", type,
                    "eventVersion", 1,
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
