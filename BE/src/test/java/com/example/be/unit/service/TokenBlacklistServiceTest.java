package com.example.be.unit.service;

import com.example.be.infrastructure.security.service.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        // Since redisTemplate.opsForValue() is called, we need to mock it
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void blacklistToken_ShouldSetInRedis() {
        String token = "test-token";
        long expirationMs = 1000;

        tokenBlacklistService.blacklistToken(token, expirationMs);

        verify(valueOperations).set(eq(token), eq("blacklisted"), any(Duration.class));
    }

    @Test
    void isBlacklisted_WhenInRedis_ShouldReturnTrue() {
        String token = "test-token";
        when(redisTemplate.hasKey(token)).thenReturn(true);

        boolean result = tokenBlacklistService.isBlacklisted(token);

        assertTrue(result);
        verify(redisTemplate).hasKey(token);
    }

    @Test
    void isBlacklisted_WhenNotInRedis_ShouldReturnFalse() {
        String token = "other-token";
        when(redisTemplate.hasKey(token)).thenReturn(false);

        boolean result = tokenBlacklistService.isBlacklisted(token);

        assertFalse(result);
    }

    @Test
    void isBlacklisted_WhenRedisFails_ShouldCheckFallback() {
        String token = "fail-token";
        doThrow(new RuntimeException("Redis down")).when(valueOperations).set(any(), any(), any());
        when(redisTemplate.hasKey(token)).thenThrow(new RuntimeException("Redis down"));

        // Blacklist it first (will go to fallback due to fail)
        tokenBlacklistService.blacklistToken(token, 10000);

        boolean result = tokenBlacklistService.isBlacklisted(token);

        assertTrue(result);
    }
}
