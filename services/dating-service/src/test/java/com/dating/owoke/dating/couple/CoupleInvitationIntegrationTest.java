package com.dating.owoke.dating.couple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "owoke.outbox.enabled=false")
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

    @Autowired
    CoupleInvitationIntegrationTest(MockMvc mockMvc, ObjectMapper objectMapper, DataSource dataSource) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE outbox_events, couple_invitations, couple_members, couples CASCADE");
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

    private static String token(InvitationCreationResponse response) {
        return response.inviteUrl().substring(response.inviteUrl().lastIndexOf('/') + 1);
    }

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor
            user(UUID userId) {
        return jwt().jwt(token -> token.subject(userId.toString()));
    }
}
