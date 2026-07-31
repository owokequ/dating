package com.dating.owoke.places.sync.service;

import java.time.Clock;
import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.dating.owoke.places.sync.configuration.TwoGisProperties;

@Component
public class TwoGisRateLimiter {

    private static final String KEY_PREFIX = "owoke:places:two-gis:rate:";

    private final StringRedisTemplate redisTemplate;
    private final TwoGisProperties properties;
    private final Clock clock;

    public TwoGisRateLimiter(
            StringRedisTemplate redisTemplate,
            TwoGisProperties properties,
            Clock clock) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.clock = clock;
    }

    public void acquire() {
        int limit = Math.max(1, properties.requestsPerSecond());
        while (true) {
            long second = clock.instant().getEpochSecond();
            String key = KEY_PREFIX + second;
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(2));
            }
            if (count != null && count <= limit) {
                return;
            }
            long millis = Math.max(10, 1000 - clock.millis() % 1000);
            try {
                Thread.sleep(millis);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("2GIS rate limiter was interrupted");
            }
        }
    }
}
