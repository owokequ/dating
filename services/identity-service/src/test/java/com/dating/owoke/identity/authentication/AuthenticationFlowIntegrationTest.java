package com.dating.owoke.identity.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;

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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import jakarta.servlet.http.Cookie;

@SpringBootTest(properties = "owoke.outbox.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthenticationFlowIntegrationTest {

    private static final String PASSWORD = "StrongPassword123!";
    private static final String REDIS_PASSWORD = "test-password";

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("owoke_identity")
            .withUsername("owoke_identity")
            .withPassword("owoke_identity");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8-alpine")
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", REDIS_PASSWORD);

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> REDIS_PASSWORD);
    }

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    AuthenticationFlowIntegrationTest(MockMvc mockMvc, DataSource dataSource) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE outbox_events, account_tokens, external_identities, "
                + "password_credentials, users CASCADE");
    }

    @Test
    void localAccountRequiresVerificationAndRefreshReuseRevokesFamily() throws Exception {
        register("  ALICE@example.com  ", "Alice");

        assertThat(jdbcTemplate.queryForObject("SELECT email FROM users", String.class))
                .isEqualTo("alice@example.com");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(loginJson("alice@example.com")))
                .andExpect(status().isUnauthorized());

        String actionUrl = jdbcTemplate.queryForObject("""
                SELECT payload #>> '{payload,actionUrl}'
                FROM outbox_events
                WHERE event_type = 'EmailVerificationRequestedV1'
                ORDER BY occurred_at DESC
                LIMIT 1
                """, String.class);
        String verificationToken = URI.create(actionUrl).getRawQuery().substring("token=".length());

        mockMvc.perform(post("/api/v1/auth/email-verifications/confirm")
                        .contentType("application/json")
                        .content("{\"token\":\"" + verificationToken + "\"}"))
                .andExpect(status().isNoContent());

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(loginJson("alice@example.com")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().httpOnly("OWOKE_ACCESS", true))
                .andExpect(cookie().httpOnly("OWOKE_REFRESH", true))
                .andReturn();

        Cookie originalRefresh = login.getResponse().getCookie("OWOKE_REFRESH");
        assertThat(originalRefresh).isNotNull();

        MvcResult refresh = mockMvc.perform(post("/api/v1/auth/refresh").cookie(originalRefresh))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie rotatedRefresh = refresh.getResponse().getCookie("OWOKE_REFRESH");
        assertThat(rotatedRefresh).isNotNull();
        assertThat(rotatedRefresh.getValue()).isNotEqualTo(originalRefresh.getValue());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(originalRefresh))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(rotatedRefresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordResetDoesNotRevealWhetherEmailExists() throws Exception {
        int before = jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_events", Integer.class);

        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType("application/json")
                        .content("{\"email\":\"missing@example.com\"}"))
                .andExpect(status().isAccepted());

        int after = jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_events", Integer.class);
        assertThat(after).isEqualTo(before);
    }

    private void register(String email, String displayName) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","displayName":"%s","password":"%s"}
                                """.formatted(email, displayName, PASSWORD)))
                .andExpect(status().isAccepted());
    }

    private static String loginJson(String email) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, PASSWORD);
    }
}
