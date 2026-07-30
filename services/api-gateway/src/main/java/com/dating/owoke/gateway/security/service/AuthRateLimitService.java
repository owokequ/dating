package com.dating.owoke.gateway.security.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitService.class);
    private static final String KEY_PREFIX = "gateway:rate-limit:";

    private final StringRedisTemplate redisTemplate;

    public AuthRateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String operation, String clientAddress, int limit, Duration window) {
        String key = KEY_PREFIX + operation + ':' + clientAddress;
        try {
            Long attempts = redisTemplate.opsForValue().increment(key);
            if (attempts != null && attempts == 1) {
                redisTemplate.expire(key, window);
            }
            return attempts == null || attempts <= limit;
        } catch (RuntimeException exception) {
            log.warn("Redis rate limiter is unavailable; allowing request for operation={}", operation, exception);
            return true;
        }
    }
}
