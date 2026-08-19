package com.dating.owoke.identity.telegram.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.dating.owoke.identity.authentication.exception.AuthenticationRejectedException;
import com.dating.owoke.identity.authentication.service.IssuedSession;
import com.dating.owoke.identity.authentication.service.SecureTokenGenerator;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class TelegramMobileSessionCodeService {

    private static final String CODE_PREFIX = "identity:telegram:mobile-code:";
    private static final Duration CODE_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;
    private final SecureTokenGenerator tokenGenerator;
    private final ObjectMapper objectMapper;

    public TelegramMobileSessionCodeService(
            StringRedisTemplate redisTemplate,
            SecureTokenGenerator tokenGenerator,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.tokenGenerator = tokenGenerator;
        this.objectMapper = objectMapper;
    }

    public String create(IssuedSession session) {
        String code = tokenGenerator.generate();
        try {
            redisTemplate.opsForValue().set(
                    CODE_PREFIX + tokenGenerator.hash(code),
                    objectMapper.writeValueAsString(session),
                    CODE_TTL);
            return code;
        } catch (JacksonException exception) {
            throw new IllegalStateException("Cannot serialize mobile Telegram session", exception);
        }
    }

    public IssuedSession consume(String code) {
        if (code == null || code.isBlank()) {
            throw new AuthenticationRejectedException("Mobile Telegram code is invalid or expired");
        }
        String json = redisTemplate.opsForValue().getAndDelete(CODE_PREFIX + tokenGenerator.hash(code));
        if (json == null) {
            throw new AuthenticationRejectedException("Mobile Telegram code is invalid or expired");
        }
        try {
            return objectMapper.readValue(json, IssuedSession.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Stored mobile Telegram session is corrupted", exception);
        }
    }
}
