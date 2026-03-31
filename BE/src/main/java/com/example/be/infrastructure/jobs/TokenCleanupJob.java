package com.example.be.infrastructure.jobs;

import com.example.be.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * TokenCleanupJob
 * Runs at 2 AM daily to delete expired JWT refresh tokens from the DB.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void execute() {
        log.info("Cron Job [TokenCleanupJob] - Started");
        try {
            var expired = refreshTokenRepository.findAll().stream()
                    .filter(t -> t.getExpiryDate().isBefore(Instant.now()))
                    .toList();
            refreshTokenRepository.deleteAll(expired);
            log.info("Cron Job [TokenCleanupJob] - Deleted {} expired tokens", expired.size());
        } catch (Exception e) {
            log.error("Cron Job [TokenCleanupJob] - Failed with error: {}", e.getMessage());
        }
    }
}
