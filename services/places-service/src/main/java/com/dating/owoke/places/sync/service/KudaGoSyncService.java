package com.dating.owoke.places.sync.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.dating.owoke.places.place.service.ExternalPlaceData;
import com.dating.owoke.places.place.service.PlaceService;
import com.dating.owoke.places.place.service.UpsertResult;
import com.dating.owoke.places.sync.configuration.KudaGoCollection;
import com.dating.owoke.places.sync.configuration.KudaGoProperties;
import com.dating.owoke.places.sync.dto.SyncFailure;
import com.dating.owoke.places.sync.dto.SyncResponse;
import com.dating.owoke.places.sync.exception.SyncAlreadyRunningException;
import com.dating.owoke.places.sync.exception.SyncUnavailableException;

@Service
public class KudaGoSyncService {

    private static final String LOCK_KEY = "owoke:places:kudago:sync-lock";
    private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final KudaGoClient client;
    private final KudaGoProperties properties;
    private final PlaceService placeService;
    private final StringRedisTemplate redisTemplate;

    public KudaGoSyncService(
            KudaGoClient client,
            KudaGoProperties properties,
            PlaceService placeService,
            StringRedisTemplate redisTemplate) {
        this.client = client;
        this.properties = properties;
        this.placeService = placeService;
        this.redisTemplate = redisTemplate;
    }

    public SyncResponse synchronize() {
        if (!properties.isConfigured()) {
            throw new SyncUnavailableException("KudaGo synchronization is not configured");
        }
        String lockOwner = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, lockOwner, Duration.ofMinutes(10));
        if (!Boolean.TRUE.equals(acquired)) {
            throw new SyncAlreadyRunningException();
        }
        try {
            Counters counters = new Counters();
            Set<String> seenExternalIds = new HashSet<>();
            int successfulRequests = 0;
            for (KudaGoCollection collection : KudaGoCollection.values()) {
                List<ExternalPlaceData> places;
                try {
                    places = client.search(collection);
                    successfulRequests++;
                } catch (SyncUnavailableException exception) {
                    counters.fail(collection.name(), 1, exception.getMessage());
                    continue;
                }
                for (ExternalPlaceData place : places) {
                    counters.received++;
                    if (!seenExternalIds.add(place.externalId())) {
                        counters.duplicates++;
                        continue;
                    }
                    counters.add(placeService.upsertExternal(place));
                }
            }
            if (successfulRequests == 0) {
                throw new SyncUnavailableException("KudaGo synchronization failed for every category group");
            }
            return counters.response();
        } finally {
            redisTemplate.execute(RELEASE_LOCK, List.of(LOCK_KEY), lockOwner);
        }
    }

    private static final class Counters {
        private int received;
        private int created;
        private int updated;
        private int unchanged;
        private int duplicates;
        private final List<SyncFailure> failures = new ArrayList<>();

        void add(UpsertResult result) {
            switch (result) {
                case CREATED -> created++;
                case UPDATED -> updated++;
                case UNCHANGED -> unchanged++;
                case DUPLICATE -> duplicates++;
            }
        }

        void fail(String category, int page, String reason) {
            failures.add(new SyncFailure(category, page, reason));
        }

        SyncResponse response() {
            return new SyncResponse(received, created, updated, unchanged, duplicates, List.copyOf(failures));
        }
    }
}
