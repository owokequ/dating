package com.dating.owoke.places.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.dating.owoke.places.place.service.ExternalPlaceData;
import com.dating.owoke.places.place.service.PlaceService;
import com.dating.owoke.places.place.service.UpsertResult;
import com.dating.owoke.places.sync.configuration.TwoGisProperties;
import com.dating.owoke.places.sync.configuration.TwoGisQuery;
import com.dating.owoke.places.sync.exception.SyncAlreadyRunningException;
import com.dating.owoke.places.sync.exception.SyncUnavailableException;

class TwoGisSyncServiceTest {

    private final TwoGisClient client = mock(TwoGisClient.class);
    private final PlaceService placeService = mock(PlaceService.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);

    @BeforeEach
    void acquireLock() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(placeService.upsertTwoGis(any())).thenReturn(UpsertResult.CREATED);
    }

    @Test
    void mapsExplicitCategoriesAndReportsPartialFailures() {
        TwoGisQuery cafe = new TwoGisQuery("кафе", "cafe");
        TwoGisQuery restaurant = new TwoGisQuery("рестораны", "restaurant");
        TwoGisQuery entertainment = new TwoGisQuery("развлечения", "entertainment");
        TwoGisProperties properties = properties(1, 10, List.of(cafe, restaurant, entertainment));
        when(client.search(cafe, 1)).thenReturn(List.of(place("1", "CAFE")));
        when(client.search(restaurant, 1)).thenThrow(new SyncUnavailableException("2GIS returned HTTP 429"));
        when(client.search(entertainment, 1)).thenReturn(List.of(place("2", "ENTERTAINMENT")));

        var response = new TwoGisSyncService(client, properties, placeService, redisTemplate).synchronize();

        assertThat(response.received()).isEqualTo(2);
        assertThat(response.created()).isEqualTo(2);
        assertThat(response.failures()).singleElement().satisfies(failure -> {
            assertThat(failure.category()).isEqualTo("RESTAURANT");
            assertThat(failure.page()).isEqualTo(1);
            assertThat(failure.reason()).isEqualTo("2GIS returned HTTP 429");
        });
        assertThat(cafe.category()).isEqualTo("CAFE");
    }

    @Test
    void requestsNextPageOnlyWhenPreviousPageIsFull() {
        TwoGisQuery cafe = new TwoGisQuery("кафе", "CAFE");
        TwoGisProperties properties = properties(3, 2, List.of(cafe));
        when(client.search(cafe, 1)).thenReturn(List.of(place("1", "CAFE"), place("2", "CAFE")));
        when(client.search(cafe, 2)).thenReturn(List.of(place("3", "CAFE")));

        var response = new TwoGisSyncService(client, properties, placeService, redisTemplate).synchronize();

        assertThat(response.received()).isEqualTo(3);
        verify(client).search(cafe, 1);
        verify(client).search(cafe, 2);
    }

    @Test
    void rejectsConcurrentSynchronization() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertThatThrownBy(() -> new TwoGisSyncService(
                client,
                properties(1, 10, List.of(new TwoGisQuery("кафе", "CAFE"))),
                placeService,
                redisTemplate).synchronize())
                .isInstanceOf(SyncAlreadyRunningException.class);
    }

    @Test
    void failsWhenEveryProviderRequestFails() {
        TwoGisQuery cafe = new TwoGisQuery("кафе", "CAFE");
        when(client.search(cafe, 1)).thenThrow(new SyncUnavailableException("2GIS request failed"));

        assertThatThrownBy(() -> new TwoGisSyncService(
                client, properties(1, 10, List.of(cafe)), placeService, redisTemplate).synchronize())
                .isInstanceOf(SyncUnavailableException.class)
                .hasMessage("2GIS synchronization failed for every category");
    }

    private TwoGisProperties properties(int maxPages, int pageSize, List<TwoGisQuery> queries) {
        return new TwoGisProperties(
                true,
                false,
                "demo-key",
                "https://catalog.api.2gis.com",
                "49.1064,55.7963",
                20_000,
                pageSize,
                maxPages,
                5,
                queries);
    }

    private ExternalPlaceData place(String externalId, String category) {
        return new ExternalPlaceData(externalId, "Place " + externalId, category, "Kazan", 55.79, 49.10);
    }
}
