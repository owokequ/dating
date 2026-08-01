package com.dating.owoke.places.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.dating.owoke.places.place.domain.PlaceSource;
import com.dating.owoke.places.place.service.ExternalPlaceData;
import com.dating.owoke.places.place.service.PlaceService;
import com.dating.owoke.places.place.service.UpsertResult;
import com.dating.owoke.places.sync.configuration.KudaGoCollection;
import com.dating.owoke.places.sync.configuration.KudaGoProperties;
import com.dating.owoke.places.sync.exception.SyncAlreadyRunningException;
import com.dating.owoke.places.sync.exception.SyncUnavailableException;

class KudaGoSyncServiceTest {

    private final KudaGoClient client = mock(KudaGoClient.class);
    private final PlaceService placeService = mock(PlaceService.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final KudaGoProperties properties = new KudaGoProperties(
            true, "https://kudago.com", "kzn", 100, 10, 20);

    @BeforeEach
    void acquireLock() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(placeService.upsertExternal(any())).thenReturn(UpsertResult.CREATED);
    }

    @Test
    void reportsPartialFailureAndImportsSuccessfulGroup() {
        when(client.search(KudaGoCollection.FOOD)).thenReturn(List.of(place("1", "RESTAURANT")));
        when(client.search(KudaGoCollection.LEISURE))
                .thenThrow(new SyncUnavailableException("KudaGo returned HTTP 503"));

        var response = service().synchronize();

        assertThat(response.received()).isEqualTo(1);
        assertThat(response.created()).isEqualTo(1);
        assertThat(response.failures()).singleElement().satisfies(failure -> {
            assertThat(failure.category()).isEqualTo("LEISURE");
            assertThat(failure.page()).isEqualTo(1);
            assertThat(failure.reason()).isEqualTo("KudaGo returned HTTP 503");
        });
    }

    @Test
    void failsWhenBothProviderRequestsFail() {
        when(client.search(any())).thenThrow(new SyncUnavailableException("KudaGo request failed"));

        assertThatThrownBy(() -> service().synchronize())
                .isInstanceOf(SyncUnavailableException.class)
                .hasMessage("KudaGo synchronization failed for every category group");
    }

    @Test
    void rejectsConcurrentSynchronization() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertThatThrownBy(() -> service().synchronize())
                .isInstanceOf(SyncAlreadyRunningException.class);
    }

    private KudaGoSyncService service() {
        return new KudaGoSyncService(client, properties, placeService, redisTemplate);
    }

    private ExternalPlaceData place(String externalId, String category) {
        return new ExternalPlaceData(
                PlaceSource.KUDAGO,
                externalId,
                "Place " + externalId,
                "Provider description",
                category,
                "Kazan",
                55.79,
                49.10,
                "https://kudago.com/kzn/place/" + externalId + "/",
                List.of());
    }
}
