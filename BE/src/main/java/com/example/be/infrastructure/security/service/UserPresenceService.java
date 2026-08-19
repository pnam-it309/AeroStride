package com.example.be.infrastructure.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserPresenceService {

    // Thời gian timeout để coi một tài khoản là online (15 phút)
    private static final long ONLINE_TIMEOUT_MS = 15 * 60 * 1000;

    private final Map<String, Long> userLastSeenMap = new ConcurrentHashMap<>();

    public void recordActivity(String username) {
        if (username != null && !username.isBlank()) {
            userLastSeenMap.put(username.trim().toLowerCase(), System.currentTimeMillis());
        }
    }

    public void recordLogout(String username) {
        if (username != null && !username.isBlank()) {
            userLastSeenMap.remove(username.trim().toLowerCase());
        }
    }

    public boolean isOnline(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        Long lastSeen = userLastSeenMap.get(username.trim().toLowerCase());
        if (lastSeen == null) {
            return false;
        }
        return (System.currentTimeMillis() - lastSeen) <= ONLINE_TIMEOUT_MS;
    }

    public Long getLastSeen(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return userLastSeenMap.get(username.trim().toLowerCase());
    }

    public Set<String> getOnlineUsers() {
        long now = System.currentTimeMillis();
        return userLastSeenMap.entrySet().stream()
                .filter(entry -> (now - entry.getValue()) <= ONLINE_TIMEOUT_MS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
