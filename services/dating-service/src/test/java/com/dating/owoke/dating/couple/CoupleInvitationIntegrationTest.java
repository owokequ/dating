package com.dating.owoke.dating.couple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.dating.owoke.dating.couple.dto.InvitationCreationResponse;
import com.dating.owoke.dating.dateproposal.dto.DateProposalResponse;
import com.dating.owoke.dating.dateproposal.messaging.service.DateProposalCommandProcessor;
import com.dating.owoke.dating.placeprojection.messaging.service.PlaceEventProcessor;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "owoke.outbox.enabled=false",
        "owoke.messaging.consumers-enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CoupleInvitationIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("owoke_dating")
            .withUsername("owoke_dating")
            .withPassword("owoke_dating");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final PlaceEventProcessor placeEventProcessor;
    private final DateProposalCommandProcessor dateProposalCommandProcessor;

    @Autowired
    CoupleInvitationIntegrationTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            DataSource dataSource,
            PlaceEventProcessor placeEventProcessor,
            DateProposalCommandProcessor dateProposalCommandProcessor) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.placeEventProcessor = placeEventProcessor;
        this.dateProposalCommandProcessor = dateProposalCommandProcessor;
    }

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE failed_messages, inbox_events, idempotency_records, date_proposals, place_projections, "
                + "outbox_events, couple_invitations, couple_members, couples CASCADE");
    }

    @Test
    void invitationActivatesCoupleOnlyOnceAndCloseArchivesMemberships() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        InvitationCreationResponse invitation = createInvitation(ownerId);
        String token = token(invitation);

        mockMvc.perform(get("/api/v1/couple-invitations/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitationId").value(invitation.invitationId().toString()));

        mockMvc.perform(post("/api/v1/couple-invitations/{token}/accept", token)
                        .with(user(ownerId)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/couple-invitations/{token}/accept", token)
                        .with(user(partnerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.members.length()").value(2));

        mockMvc.perform(post("/api/v1/couple-invitations/{token}/accept", token)
                        .with(user(UUID.randomUUID())))
                .andExpect(status().isGone());

        mockMvc.perform(get("/api/v1/couples/current").with(user(partnerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/couples/current/close").with(user(partnerId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/couples/current").with(user(ownerId)))
                .andExpect(status().isNotFound());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM couple_members WHERE left_at IS NOT NULL", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isEqualTo(2);
    }

    @Test
    void reissuingInvitationRevokesPreviousTokenWithoutCreatingSecondCouple() throws Exception {
        UUID ownerId = UUID.randomUUID();
        InvitationCreationResponse first = createInvitation(ownerId);
        InvitationCreationResponse second = createInvitation(ownerId);

        mockMvc.perform(get("/api/v1/couple-invitations/{token}", token(first)))
                .andExpect(status().isGone());
        mockMvc.perform(get("/api/v1/couple-invitations/{token}", token(second)))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM couples", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM couple_invitations WHERE status = 'PENDING'", Integer.class)).isEqualTo(1);
    }

    @Test
    void userCannotAcceptInvitationsIntoTwoCouples() throws Exception {
        UUID partnerId = UUID.randomUUID();
        InvitationCreationResponse first = createInvitation(UUID.randomUUID());
        InvitationCreationResponse second = createInvitation(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/couple-invitations/{token}/accept", token(first))
                        .with(user(partnerId)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/couple-invitations/{token}/accept", token(second))
                        .with(user(partnerId)))
                .andExpect(status().isConflict());
    }

    @Test
    void concurrentAcceptCreatesExactlyOnePartner() throws Exception {
        InvitationCreationResponse invitation = createInvitation(UUID.randomUUID());
        String token = token(invitation);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> results = List.of(
                    executor.submit(() -> acceptAfter(start, token, UUID.randomUUID())),
                    executor.submit(() -> acceptAfter(start, token, UUID.randomUUID())));
            start.countDown();
            List<Integer> statuses = results.stream().map(this::await).sorted().toList();
            assertThat(statuses).containsExactly(200, 410);
        }

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM couple_members WHERE role = 'PARTNER' AND left_at IS NULL", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void dateProposalUsesPlaceSnapshotAndIdempotentStatusChanges() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        activateCouple(ownerId, partnerId);
        UUID placeId = UUID.randomUUID();
        insertPlace(placeId, "Original cafe", "Kazan, Original street 1", "ACTIVE");
        UUID coverMediaId = UUID.randomUUID();
        jdbcTemplate.update(
                "UPDATE place_projections SET cover_media_id = ?, media_revision = 1 WHERE id = ?",
                coverMediaId, placeId);

        DateProposalResponse proposal = createProposal(
                ownerId, placeId, "create-proposal-1", Instant.now().plusSeconds(86_400), "Dinner");
        assertThat(proposal.status().name()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(proposal.placeCoverMediaId()).isEqualTo(coverMediaId);

        mockMvc.perform(post("/api/v1/date-proposals/{id}/accept", proposal.id())
                        .header("Idempotency-Key", "owner-cannot-accept")
                        .with(user(ownerId)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/date-proposals/{id}/accept", proposal.id())
                        .header("Idempotency-Key", "accept-proposal-1")
                        .with(user(partnerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        mockMvc.perform(post("/api/v1/date-proposals/{id}/accept", proposal.id())
                        .header("Idempotency-Key", "accept-proposal-1")
                        .with(user(partnerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        jdbcTemplate.update(
                "UPDATE place_projections SET name = ?, address = ?, version = version + 1 WHERE id = ?",
                "Renamed cafe", "Kazan, New street 2", placeId);
        mockMvc.perform(get("/api/v1/date-proposals/{id}", proposal.id()).with(user(ownerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeName").value("Original cafe"))
                .andExpect(jsonPath("$.placeAddress").value("Kazan, Original street 1"))
                .andExpect(jsonPath("$.placeCoverMediaId").value(coverMediaId.toString()));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE event_type = 'DateProposalAcceptedV2'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void archivedPlaceAndPastDateCannotCreateProposal() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        activateCouple(ownerId, partnerId);
        UUID placeId = UUID.randomUUID();
        insertPlace(placeId, "Closed cafe", "Kazan", "ARCHIVED");

        mockMvc.perform(post("/api/v1/date-proposals")
                        .header("Idempotency-Key", "archived-place")
                        .with(user(ownerId))
                        .contentType("application/json")
                        .content("""
                                {"scheduledAt":"%s","placeId":"%s"}
                                """.formatted(Instant.now().plusSeconds(3600), placeId)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/date-proposals")
                        .header("Idempotency-Key", "past-date")
                        .with(user(ownerId))
                        .contentType("application/json")
                        .content("""
                                {"scheduledAt":"%s","placeId":"%s"}
                                """.formatted(Instant.now().minusSeconds(60), placeId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void telegramDecisionCommandAcceptsProposalAndIsIdempotent() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        activateCouple(ownerId, partnerId);
        UUID placeId = UUID.randomUUID();
        insertPlace(placeId, "Original cafe", "Kazan", "ACTIVE");
        DateProposalResponse proposal = createProposal(
                ownerId, placeId, "telegram-proposal", Instant.now().plusSeconds(86_400), "Dinner");

        UUID commandId = UUID.randomUUID();
        String command = dateDecisionCommand(
                commandId, proposal.id(), proposal.coupleId(), partnerId, "ACCEPT");
        dateProposalCommandProcessor.process("dating.commands.v1", command);
        dateProposalCommandProcessor.process("dating.commands.v1", command);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM date_proposals WHERE id = ?", String.class, proposal.id()))
                .isEqualTo("ACCEPTED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM inbox_events WHERE event_id = ?", Integer.class, commandId))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE event_type = 'DateProposalAcceptedV2'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE event_type = 'DateProposalDecisionResultV1'", Integer.class))
                .isEqualTo(1);
        var resultPayload = objectMapper.readTree(jdbcTemplate.queryForObject(
                "SELECT payload FROM outbox_events WHERE event_type = 'DateProposalDecisionResultV1'", String.class))
                .path("payload");
        assertThat(resultPayload.path("successful").asBoolean()).isTrue();
        assertThat(resultPayload.path("requestId").asString()).isEqualTo(commandId.toString());
    }

    @Test
    void telegramDecisionCommandRecordsBusinessRejectionWithoutRetry() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        activateCouple(ownerId, partnerId);
        UUID placeId = UUID.randomUUID();
        insertPlace(placeId, "Original cafe", "Kazan", "ACTIVE");
        DateProposalResponse proposal = createProposal(
                ownerId, placeId, "telegram-rejected", Instant.now().plusSeconds(86_400), "Dinner");

        dateProposalCommandProcessor.process(
                "dating.commands.v1",
                dateDecisionCommand(
                        UUID.randomUUID(), proposal.id(), proposal.coupleId(), ownerId, "ACCEPT"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM date_proposals WHERE id = ?", String.class, proposal.id()))
                .isEqualTo("PENDING_CONFIRMATION");
        var resultPayload = objectMapper.readTree(jdbcTemplate.queryForObject(
                "SELECT payload FROM outbox_events WHERE event_type = 'DateProposalDecisionResultV1'", String.class))
                .path("payload");
        assertThat(resultPayload.path("successful").asBoolean()).isFalse();
        assertThat(resultPayload.path("errorCode").asString()).isEqualTo("ACTION_NOT_ALLOWED");
    }

    @Test
    void placeEventsAreIdempotentAndStaleRetryCannotRollbackProjection() {
        UUID placeId = UUID.randomUUID();
        UUID publishedEventId = UUID.randomUUID();
        Instant publishedAt = Instant.parse("2026-01-01T10:00:00Z");

        String published = placeEvent(
                publishedEventId, "PlacePublishedV1", publishedAt, placeId, "First name", "First address", "ACTIVE");
        placeEventProcessor.process("places.events.v1", published);
        placeEventProcessor.process("places.events.v1", published);
        placeEventProcessor.process("places.events.v1", placeEvent(
                UUID.randomUUID(), "PlaceArchivedV1", publishedAt.plusSeconds(60),
                placeId, "Final name", "Final address", "ARCHIVED"));
        placeEventProcessor.process("places.events.v1.retry", placeEvent(
                UUID.randomUUID(), "PlaceUpdatedV1", publishedAt.plusSeconds(30),
                placeId, "Stale name", "Stale address", "ACTIVE"));

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM place_projections", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM inbox_events", Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForMap(
                "SELECT name, address, status FROM place_projections WHERE id = ?", placeId))
                .containsEntry("name", "Final name")
                .containsEntry("address", "Final address")
                .containsEntry("status", "ARCHIVED");
    }

    private int acceptAfter(CountDownLatch start, String token, UUID userId) throws Exception {
        start.await();
        return mockMvc.perform(post("/api/v1/couple-invitations/{token}/accept", token)
                        .with(user(userId)))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private Integer await(Future<Integer> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private InvitationCreationResponse createInvitation(UUID ownerId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/couple-invitations").with(user(ownerId)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(
                result.getResponse().getContentAsString(), InvitationCreationResponse.class);
    }

    private void activateCouple(UUID ownerId, UUID partnerId) throws Exception {
        InvitationCreationResponse invitation = createInvitation(ownerId);
        mockMvc.perform(post("/api/v1/couple-invitations/{token}/accept", token(invitation))
                        .with(user(partnerId)))
                .andExpect(status().isOk());
    }

    private void insertPlace(UUID placeId, String name, String address, String status) {
        jdbcTemplate.update("""
                INSERT INTO place_projections (id, name, address, status, updated_at, version)
                VALUES (?, ?, ?, ?, now(), 0)
                """, placeId, name, address, status);
    }

    private DateProposalResponse createProposal(
            UUID userId,
            UUID placeId,
            String idempotencyKey,
            Instant scheduledAt,
            String description
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/date-proposals")
                        .header("Idempotency-Key", idempotencyKey)
                        .with(user(userId))
                        .contentType("application/json")
                        .content("""
                                {"scheduledAt":"%s","placeId":"%s","description":"%s"}
                                """.formatted(scheduledAt, placeId, description)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), DateProposalResponse.class);
    }

    private static String token(InvitationCreationResponse response) {
        return response.inviteUrl().substring(response.inviteUrl().lastIndexOf('/') + 1);
    }

    private String placeEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            UUID placeId,
            String name,
            String address,
            String status) {
        return """
                {
                  "eventId":"%s",
                  "eventType":"%s",
                  "eventVersion":1,
                  "aggregateId":"%s",
                  "occurredAt":"%s",
                  "correlationId":"%s",
                  "payload":{
                    "placeId":"%s",
                    "cityCode":"KZN",
                    "name":"%s",
                    "address":"%s",
                    "category":"CAFE",
                    "latitude":55.796,
                    "longitude":49.106,
                    "priceLevel":2,
                    "status":"%s"
                  }
                }
                """.formatted(
                eventId, eventType, placeId, occurredAt, UUID.randomUUID(), placeId, name, address, status);
    }

    private String dateDecisionCommand(
            UUID eventId,
            UUID proposalId,
            UUID coupleId,
            UUID actorId,
            String decision) {
        return """
                {
                  "eventId":"%s",
                  "eventType":"DateProposalDecisionRequestedV1",
                  "eventVersion":1,
                  "aggregateId":"%s",
                  "occurredAt":"%s",
                  "correlationId":"%s",
                  "payload":{
                    "proposalId":"%s",
                    "coupleId":"%s",
                    "actorId":"%s",
                    "decision":"%s"
                  }
                }
                """.formatted(
                eventId, coupleId, Instant.now(), UUID.randomUUID(), proposalId, coupleId, actorId, decision);
    }

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor
            user(UUID userId) {
        return jwt().jwt(token -> token.subject(userId.toString()));
    }
}
