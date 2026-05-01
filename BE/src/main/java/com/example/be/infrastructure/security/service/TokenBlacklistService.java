package com.example.be.infrastructure.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private final Map<String, Long> localFallback = new ConcurrentHashMap<>();

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklistToken(String token, long expirationMs) {
        try {
            redisTemplate.opsForValue().set(token, "blacklisted", Duration.ofMillis(expirationMs));
        } catch (Exception e) {
            log.error("Redis Connection Failed: Falling back to memory storage.");
            localFallback.put(token, System.currentTimeMillis() + expirationMs);
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(token));
        } catch (Exception e) {
            log.warn("Redis Down: Checking in-memory fallback store.");
            Long expiry = localFallback.get(token);
            if (expiry != null && expiry < System.currentTimeMillis()) {
                localFallback.remove(token);
                return false;
            }
            return expiry != null;
        }
    }
}
