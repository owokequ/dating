package com.dating.owoke.places.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.dating.owoke.places.place.service.ExternalPlaceData;
import com.dating.owoke.places.place.service.PlaceService;
import com.dating.owoke.places.place.service.UpsertResult;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "owoke.outbox.enabled=false",
        "owoke.two-gis.enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers
class PlaceCatalogIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("owoke_places")
            .withUsername("owoke_places")
            .withPassword("owoke_places");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final PlaceService placeService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    PlaceCatalogIntegrationTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            PlaceService placeService,
            DataSource dataSource) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.placeService = placeService;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE outbox_events, places CASCADE");
    }

    @Test
    void adminCreatesAndArchivesPlaceWhilePublicCatalogShowsOnlyActive() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/admin/places")
                        .with(adminJwt())
                        .contentType("application/json")
                        .content(createPlaceJson("Cafe Skazka", "Baumana street, 1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cityCode").value("KZN"))
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andReturn();
        UUID placeId = UUID.fromString(objectMapper.readTree(
                created.getResponse().getContentAsString()).path("id").asString());

        mockMvc.perform(get("/api/v1/places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(placeId.toString()));

        mockMvc.perform(post("/api/v1/admin/places")
                        .with(adminJwt())
                        .contentType("application/json")
                        .content(createPlaceJson("  CAFE   SKAZKA ", " BAUMANA STREET, 1 ")))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/v1/admin/places/{id}", placeId)
                        .with(adminJwt())
                        .contentType("application/json")
                        .content(updatePlaceJson("Cafe Skazka", "Baumana street, 1", "ARCHIVED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(get("/api/v1/places/{id}", placeId))
                .andExpect(status().isNotFound());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_events", Integer.class)).isEqualTo(2);
    }

    @Test
    void userCannotCallAdminApiAndTwoGisUpsertIsIdempotent() throws Exception {
        mockMvc.perform(post("/api/v1/admin/places")
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString())
                                .claim("roles", List.of("USER"))))
                        .contentType("application/json")
                        .content(createPlaceJson("Cafe", "Kazan")))
                .andExpect(status().isForbidden());

        ExternalPlaceData data = new ExternalPlaceData(
                "2gis-1", "Restaurant", "RESTAURANT", "Kremlevskaya street, 1", 55.796, 49.106);
        assertThat(placeService.upsertTwoGis(data)).isEqualTo(UpsertResult.CREATED);
        assertThat(placeService.upsertTwoGis(data)).isEqualTo(UpsertResult.UNCHANGED);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM places", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM places", String.class)).isEqualTo("DRAFT");
        assertThat(jdbcTemplate.queryForObject("SELECT event_type FROM outbox_events", String.class))
                .isEqualTo("PlaceDraftedV1");

        mockMvc.perform(get("/api/v1/places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/admin/places")
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString())
                                .claim("roles", List.of("USER")))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/places")
                        .with(adminJwt())
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].status").value("DRAFT"));
    }

    @Test
    void twoGisRefreshPreservesAdminFieldsAndNeverReactivatesArchivedPlace() throws Exception {
        ExternalPlaceData initial = new ExternalPlaceData(
                "2gis-2", "Old name", "CAFE", "Old address", 55.796, 49.106);
        assertThat(placeService.upsertTwoGis(initial)).isEqualTo(UpsertResult.CREATED);
        UUID placeId = jdbcTemplate.queryForObject("SELECT id FROM places", UUID.class);

        mockMvc.perform(put("/api/v1/admin/places/{id}", placeId)
                        .with(adminJwt())
                        .contentType("application/json")
                        .content(updatePlaceJson(
                                "Old name", "Old address", "ARCHIVED", "Own description", 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Own description"))
                .andExpect(jsonPath("$.priceLevel").value(3));

        ExternalPlaceData refreshed = new ExternalPlaceData(
                "2gis-2", "Provider name", "RESTAURANT", "Provider address", 55.8, 49.2);
        assertThat(placeService.upsertTwoGis(refreshed)).isEqualTo(UpsertResult.UPDATED);

        mockMvc.perform(get("/api/v1/admin/places")
                        .with(adminJwt())
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Provider name"))
                .andExpect(jsonPath("$.items[0].category").value("RESTAURANT"))
                .andExpect(jsonPath("$.items[0].description").value("Own description"))
                .andExpect(jsonPath("$.items[0].priceLevel").value(3))
                .andExpect(jsonPath("$.items[0].status").value("ARCHIVED"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isEqualTo(2);
    }

    @Test
    void adminCannotChangeProviderOwnedFields() throws Exception {
        placeService.upsertTwoGis(new ExternalPlaceData(
                "2gis-3", "Provider name", "CAFE", "Provider address", 55.796, 49.106));
        UUID placeId = jdbcTemplate.queryForObject("SELECT id FROM places", UUID.class);

        mockMvc.perform(put("/api/v1/admin/places/{id}", placeId)
                        .with(adminJwt())
                        .contentType("application/json")
                        .content(updatePlaceJson(
                                "Changed by admin", "Provider address", "DRAFT", "Description", 2)))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt().jwt(token -> token
                .subject(UUID.randomUUID().toString())
                .claim("roles", List.of("ADMIN")))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private String createPlaceJson(String name, String address) {
        return """
                {
                  "name": "%s",
                  "description": "A cozy place",
                  "category": "CAFE",
                  "address": "%s",
                  "latitude": 55.796,
                  "longitude": 49.106,
                  "priceLevel": 2
                }
                """.formatted(name, address);
    }

    private String updatePlaceJson(String name, String address, String status) {
        return updatePlaceJson(name, address, status, "A cozy place", 2);
    }

    private String updatePlaceJson(
            String name,
            String address,
            String status,
            String description,
            Integer priceLevel) {
        return """
                {
                  "name": "%s",
                  "description": "%s",
                  "category": "CAFE",
                  "address": "%s",
                  "latitude": 55.796,
                  "longitude": 49.106,
                  "priceLevel": %d,
                  "status": "%s"
                }
                """.formatted(name, description, address, priceLevel, status);
    }
}
