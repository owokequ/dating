package com.dating.owoke.gateway.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
class GatewaySecurityIntegrationTest {

    private static final HttpServer NOTIFICATION_STUB = createNotificationStub();
    private static final HttpServer MEDIA_STUB = createMediaStub();

    @DynamicPropertySource
    static void notificationService(DynamicPropertyRegistry registry) {
        registry.add("NOTIFICATION_SERVICE_URL", () ->
                "http://localhost:" + NOTIFICATION_STUB.getAddress().getPort());
        registry.add("MEDIA_SERVICE_URL", () ->
                "http://localhost:" + MEDIA_STUB.getAddress().getPort());
    }

    @BeforeAll
    static void startNotificationStub() {
        NOTIFICATION_STUB.start();
        MEDIA_STUB.start();
    }

    @AfterAll
    static void stopNotificationStub() {
        NOTIFICATION_STUB.stop(0);
        MEDIA_STUB.stop(0);
    }

    private final MockMvc mockMvc;

    @Autowired
    GatewaySecurityIntegrationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void csrfEndpointMaterializesReadableCookie() throws Exception {
        mockMvc.perform(get("/api/v1/security/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }

    @Test
    void protectedEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicMediaCanBeReadWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/media/place-collections/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isOk());
    }

    @Test
    void stateChangingRequestRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"alice@example.com\",\"password\":\"StrongPassword123!\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void telegramWebhookUsesThePublicCsrfExemptRoute() throws Exception {
        mockMvc.perform(post("/api/v1/telegram/webhook")
                        .header("X-Telegram-Bot-Api-Secret-Token", "test-secret")
                        .contentType("application/json")
                        .content("{\"update_id\":1}"))
                .andExpect(status().isNoContent());
    }

    private static HttpServer createNotificationStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/api/v1/telegram/webhook", exchange -> {
                exchange.getRequestBody().readAllBytes();
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
            });
            return server;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static HttpServer createMediaStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/api/v1/media/", exchange -> {
                exchange.getRequestBody().readAllBytes();
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().close();
                exchange.close();
            });
            return server;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
