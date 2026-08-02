package com.dating.owoke.events.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.dating.owoke.events.sync.configuration.KudaGoProperties;

class KudaGoEventClientTest {

    @Test
    void mapsCurrentCdnImagesAndOptionalExternalAttribution() {
        Instant from = Instant.parse("2026-08-02T10:00:00Z");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KudaGoEventClient client = new KudaGoEventClient(builder, new KudaGoProperties(
                true, false, "https://kudago.com", "kzn", Duration.ofDays(90), Duration.ofHours(6),
                100, List.of("exhibition")));
        server.expect(request -> assertThat(request.getURI().getQuery())
                        .contains("location=kzn", "page_size=100", "expand=dates,place,images"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson(from), MediaType.APPLICATION_JSON));

        var page = client.page(1, from, from.plus(Duration.ofDays(90)));

        assertThat(page.events()).singleElement().satisfies(event -> {
            assertThat(event.sourcePageUrl()).isEqualTo("https://kzn.kudago.com/event/test/");
            assertThat(event.images()).hasSize(2);
            assertThat(event.images().get(0).remoteUrl())
                    .isEqualTo("https://media.kudago.com/images/event/photo.jpg");
            assertThat(event.images().get(0).thumbnailUrl())
                    .isEqualTo("https://media.kudago.com/thumbs/xl/images/event/photo.jpg");
            assertThat(event.images().get(0).sourceLink()).isEqualTo("https://press.example/photo");
            assertThat(event.images().get(1).sourceLink()).isNull();
        });
        server.verify();
    }

    @Test
    void normalizesOnlyKudaGoHttpsLinks() {
        assertThat(KudaGoEventClient.normalizeKudaGoUrl("http://kudago.com/kzn/event/test/"))
                .isEqualTo("https://kudago.com/kzn/event/test/");
        assertThat(KudaGoEventClient.normalizeKudaGoUrl("https://kzn.kudago.com/event/test/"))
                .isEqualTo("https://kzn.kudago.com/event/test/");
        assertThat(KudaGoEventClient.normalizeKudaGoUrl("https://fake-kudago.com/event/test/")).isNull();
        assertThat(KudaGoEventClient.normalizeKudaGoUrl("https://example.com/event/test/")).isNull();
    }

    @Test
    void acceptsOnlyKudaGoMediaPathsForImages() {
        assertThat(KudaGoEventClient.normalizeKudaGoMediaUrl("http://kudago.com/media/images/test.jpg"))
                .isEqualTo("https://kudago.com/media/images/test.jpg");
        assertThat(KudaGoEventClient.normalizeKudaGoMediaUrl(
                "https://media.kudago.com/images/event/test.jpg"))
                .isEqualTo("https://media.kudago.com/images/event/test.jpg");
        assertThat(KudaGoEventClient.normalizeKudaGoMediaUrl(
                "https://media.kudago.com/thumbs/xl/images/event/test.jpg"))
                .isEqualTo("https://media.kudago.com/thumbs/xl/images/event/test.jpg");
        assertThat(KudaGoEventClient.normalizeKudaGoMediaUrl(
                "https://static.kudago.com/img/logo.svg")).isNull();
        assertThat(KudaGoEventClient.normalizeKudaGoMediaUrl("https://kudago.com/kzn/event/test/")).isNull();
    }

    private String responseJson(Instant from) {
        return """
                {
                  "next": null,
                  "results": [{
                    "id": 42,
                    "title": "Выставка",
                    "description": "Описание",
                    "categories": ["exhibition"],
                    "price": "500 ₽",
                    "is_free": false,
                    "age_restriction": "12+",
                    "site_url": "https://kzn.kudago.com/event/test/",
                    "dates": [{"start": %d, "end": %d, "is_continuous": false}],
                    "place": {
                      "id": 7,
                      "title": "Галерея",
                      "address": "ул. Баумана, 1",
                      "coords": {"lat": 55.79, "lon": 49.10}
                    },
                    "images": [
                      {
                        "image": "https://media.kudago.com/images/event/photo.jpg",
                        "thumbnails": {"640x384": "https://media.kudago.com/thumbs/xl/images/event/photo.jpg"},
                        "source": {"name": "Пресс-служба", "link": "https://press.example/photo"}
                      },
                      {
                        "image": "https://media.kudago.com/images/event/photo-2.jpg",
                        "thumbnails": {},
                        "source": {"name": "KudaGo", "link": ""}
                      }
                    ]
                  }]
                }
                """.formatted(from.plusSeconds(3600).getEpochSecond(), from.plusSeconds(7200).getEpochSecond());
    }
}
