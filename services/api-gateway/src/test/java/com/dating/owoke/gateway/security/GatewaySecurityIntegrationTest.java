package com.dating.owoke.gateway.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class GatewaySecurityIntegrationTest {

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
    void stateChangingRequestRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"alice@example.com\",\"password\":\"StrongPassword123!\"}"))
                .andExpect(status().isForbidden());
    }
}
