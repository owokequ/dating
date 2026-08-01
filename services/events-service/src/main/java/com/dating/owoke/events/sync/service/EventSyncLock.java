package com.dating.owoke.events.sync.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.dating.owoke.events.sync.exception.SyncAlreadyRunningException;

@Component
public class EventSyncLock {
    private static final String KEY = "owoke:events:kudago:sync-lock";
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);
    private final StringRedisTemplate redis;
    public EventSyncLock(StringRedisTemplate redis) { this.redis = redis; }

    public String acquire() {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redis.opsForValue().setIfAbsent(KEY, token, Duration.ofMinutes(15));
        if (!Boolean.TRUE.equals(acquired)) throw new SyncAlreadyRunningException();
        return token;
    }
    public void release(String token) { redis.execute(RELEASE, List.of(KEY), token); }
}
