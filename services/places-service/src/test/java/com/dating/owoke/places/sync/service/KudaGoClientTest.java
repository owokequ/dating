package com.dating.owoke.places.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.dating.owoke.places.sync.configuration.KudaGoCollection;
import com.dating.owoke.places.sync.configuration.KudaGoProperties;

class KudaGoClientTest {

    @Test
    void mapsSortsAndLimitsFoodPlaces() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KudaGoClient client = new KudaGoClient(builder, new KudaGoProperties(
                true, "https://kudago.com", "kzn", 100, 2, 20));
        server.expect(request -> assertThat(request.getURI().getQuery())
                        .contains("location=kzn", "page_size=100", "text_format=text"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson(), MediaType.APPLICATION_JSON));

        var result = client.search(KudaGoCollection.FOOD);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(place -> place.externalId()).containsExactly("10", "20");
        assertThat(result).extracting(place -> place.category()).containsExactly("CAFE", "RESTAURANT");
        assertThat(result.getFirst().sourcePageUrl()).startsWith("https://");
        assertThat(result.getFirst().images()).singleElement().satisfies(image -> {
            assertThat(image.remoteUrl()).isEqualTo("https://kudago.com/media/images/place.jpg");
            assertThat(image.sourceName()).isEqualTo("KudaGo");
            assertThat(image.providerAssetKey()).hasSize(64);
        });
        server.verify();
    }

    private String responseJson() {
        return """
                {
                  "count": 4,
                  "results": [
                    {
                      "id": 20,
                      "title": "Restaurant",
                      "address": "Baumana, 1",
                      "coords": {"lat": 55.79, "lon": 49.10},
                      "categories": ["restaurants"],
                      "description": "Restaurant description",
                      "images": [],
                      "site_url": "https://kudago.com/kzn/place/restaurant/",
                      "is_closed": false,
                      "favorites_count": 7
                    },
                    {
                      "id": 10,
                      "title": "Anticafe",
                      "address": "Kremlevskaya, 2",
                      "coords": {"lat": 55.80, "lon": 49.11},
                      "categories": ["anticafe"],
                      "description": "Anticafe description",
                      "images": [{"image": "http://kudago.com/media/images/place.jpg"}],
                      "site_url": "http://kudago.com/kzn/place/anticafe/",
                      "is_closed": false,
                      "favorites_count": 9
                    },
                    {
                      "id": 30,
                      "title": "Closed",
                      "address": "Closed street",
                      "coords": {"lat": 55.80, "lon": 49.11},
                      "categories": ["restaurants"],
                      "site_url": "https://kudago.com/kzn/place/closed/",
                      "is_closed": true,
                      "favorites_count": 100
                    },
                    {
                      "id": 40,
                      "title": "Without address",
                      "address": "",
                      "coords": {"lat": 55.80, "lon": 49.11},
                      "categories": ["restaurants"],
                      "site_url": "https://kudago.com/kzn/place/no-address/",
                      "is_closed": false,
                      "favorites_count": 50
                    }
                  ]
                }
                """;
    }
}
