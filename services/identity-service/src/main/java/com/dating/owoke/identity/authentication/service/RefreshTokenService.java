package com.dating.owoke.identity.authentication.service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.dating.owoke.identity.authentication.configuration.IdentitySecurityProperties;
import com.dating.owoke.identity.authentication.exception.AuthenticationRejectedException;
import com.dating.owoke.identity.authentication.exception.RefreshTokenReuseException;

@Service
public class RefreshTokenService {

    private static final String ACTIVE_PREFIX = "identity:refresh:active:";
    private static final String USED_PREFIX = "identity:refresh:used:";
    private static final String REVOKED_PREFIX = "identity:refresh:revoked:";
    private static final String USER_FAMILIES_PREFIX = "identity:refresh:user-families:";
    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[2]) == 1 then return -2 end
            local current = redis.call('GET', KEYS[1])
            if not current or current ~= ARGV[1] then return -1 end
            if redis.call('EXISTS', KEYS[3]) == 1 then return -3 end
            redis.call('SET', KEYS[2], ARGV[1], 'PX', ARGV[2])
            redis.call('DEL', KEYS[1])
            redis.call('SET', KEYS[4], ARGV[1], 'PX', ARGV[2])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final SecureTokenGenerator tokenGenerator;
    private final Duration ttl;

    public RefreshTokenService(
            StringRedisTemplate redisTemplate,
            SecureTokenGenerator tokenGenerator,
            IdentitySecurityProperties properties) {
        this.redisTemplate = redisTemplate;
        this.tokenGenerator = tokenGenerator;
        this.ttl = properties.refreshTokenTtl();
    }

    public RefreshGrant issue(UUID userId) {
        UUID familyId = UUID.randomUUID();
        String token = tokenGenerator.generate();
        String value = value(userId, familyId);
        redisTemplate.opsForValue().set(activeKey(token), value, ttl);
        String userFamiliesKey = USER_FAMILIES_PREFIX + userId;
        redisTemplate.opsForSet().add(userFamiliesKey, familyId.toString());
        redisTemplate.expire(userFamiliesKey, ttl);
        return new RefreshGrant(userId, token);
    }

    public RefreshGrant rotate(String presentedToken) {
        String oldActiveKey = activeKey(presentedToken);
        String stored = redisTemplate.opsForValue().get(oldActiveKey);
        if (stored == null) {
            String consumed = redisTemplate.opsForValue().get(usedKey(presentedToken));
            if (consumed != null) {
                SessionValue reused = parse(consumed);
                revokeFamily(reused.familyId());
                throw new RefreshTokenReuseException();
            }
            throw new AuthenticationRejectedException("Refresh token is invalid or expired");
        }

        SessionValue session = parse(stored);
        String newToken = tokenGenerator.generate();
        Long result = redisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(
                        oldActiveKey,
                        usedKey(presentedToken),
                        revokedKey(session.familyId()),
                        activeKey(newToken)),
                stored,
                Long.toString(ttl.toMillis()));

        if (result == null || result == -1L) {
            throw new AuthenticationRejectedException("Refresh token is invalid or expired");
        }
        if (result == -2L) {
            revokeFamily(session.familyId());
            throw new RefreshTokenReuseException();
        }
        if (result == -3L) {
            throw new AuthenticationRejectedException("Refresh token family is revoked");
        }
        return new RefreshGrant(session.userId(), newToken);
    }

    public void revoke(String presentedToken) {
        String stored = redisTemplate.opsForValue().get(activeKey(presentedToken));
        if (stored != null) {
            SessionValue session = parse(stored);
            revokeFamily(session.familyId());
            redisTemplate.delete(activeKey(presentedToken));
        }
    }

    public void revokeAll(UUID userId) {
        String key = USER_FAMILIES_PREFIX + userId;
        Set<String> familyIds = redisTemplate.opsForSet().members(key);
        if (familyIds != null) {
            familyIds.forEach(value -> revokeFamily(UUID.fromString(value)));
        }
        redisTemplate.delete(key);
    }

    private void revokeFamily(UUID familyId) {
        redisTemplate.opsForValue().set(revokedKey(familyId), "1", ttl);
    }

    private String activeKey(String rawToken) {
        return ACTIVE_PREFIX + tokenGenerator.hash(rawToken);
    }

    private String usedKey(String rawToken) {
        return USED_PREFIX + tokenGenerator.hash(rawToken);
    }

    private static String revokedKey(UUID familyId) {
        return REVOKED_PREFIX + familyId;
    }

    private static String value(UUID userId, UUID familyId) {
        return userId + "|" + familyId;
    }

    private static SessionValue parse(String value) {
        String[] parts = value.split("\\|", -1);
        if (parts.length != 2) {
            throw new IllegalStateException("Corrupted refresh session");
        }
        return new SessionValue(UUID.fromString(parts[0]), UUID.fromString(parts[1]));
    }

    private record SessionValue(UUID userId, UUID familyId) {
    }
}
