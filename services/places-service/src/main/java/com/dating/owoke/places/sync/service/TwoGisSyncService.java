package com.dating.owoke.places.sync.service;

import java.time.Duration;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.dating.owoke.places.place.service.ExternalPlaceData;
import com.dating.owoke.places.place.service.PlaceService;
import com.dating.owoke.places.place.service.UpsertResult;
import com.dating.owoke.places.sync.configuration.TwoGisProperties;
import com.dating.owoke.places.sync.dto.SyncResponse;
import com.dating.owoke.places.sync.dto.SyncFailure;
import com.dating.owoke.places.sync.configuration.TwoGisQuery;
import com.dating.owoke.places.sync.exception.SyncAlreadyRunningException;
import com.dating.owoke.places.sync.exception.SyncUnavailableException;

@Service
public class TwoGisSyncService {

    private static final String LOCK_KEY = "owoke:places:two-gis:sync-lock";
    private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final TwoGisClient client;
    private final TwoGisProperties properties;
    private final PlaceService placeService;
    private final StringRedisTemplate redisTemplate;

    public TwoGisSyncService(
            TwoGisClient client,
            TwoGisProperties properties,
            PlaceService placeService,
            StringRedisTemplate redisTemplate) {
        this.client = client;
        this.properties = properties;
        this.placeService = placeService;
        this.redisTemplate = redisTemplate;
    }

    public SyncResponse synchronize() {
        if (!properties.isConfigured()) {
            throw new SyncUnavailableException("2GIS synchronization is not configured");
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
            for (TwoGisQuery query : properties.queries()) {
                for (int page = 1; page <= Math.max(1, properties.maxPages()); page++) {
                    java.util.List<ExternalPlaceData> places;
                    try {
                        places = client.search(query, page);
                        successfulRequests++;
                    } catch (SyncUnavailableException exception) {
                        counters.fail(query.category(), page, exception.getMessage());
                        break;
                    }
                    for (ExternalPlaceData place : places) {
                        counters.received++;
                        if (!seenExternalIds.add(place.externalId())) {
                            counters.duplicates++;
                            continue;
                        }
                        counters.add(placeService.upsertTwoGis(place));
                    }
                    if (places.size() < properties.pageSize()) {
                        break;
                    }
                }
            }
            if (successfulRequests == 0) {
                throw new SyncUnavailableException("2GIS synchronization failed for every category");
            }
            return counters.response();
        } finally {
            redisTemplate.execute(RELEASE_LOCK, java.util.List.of(LOCK_KEY), lockOwner);
        }
    }

    private static final class Counters {
        private int received;
        private int created;
        private int updated;
        private int unchanged;
        private int duplicates;
        private final java.util.List<SyncFailure> failures = new java.util.ArrayList<>();

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
            return new SyncResponse(received, created, updated, unchanged, duplicates, java.util.List.copyOf(failures));
        }
    }
}
