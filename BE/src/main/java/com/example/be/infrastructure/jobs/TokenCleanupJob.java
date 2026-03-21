package com.example.be.infrastructure.jobs;

import com.example.be.core.security.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupJob {

    private final SecurityService securityService;

    /**
     * Runs at 2 AM daily to clean up expired JWT refresh tokens and OTPs.
     * Cron: "0 0 2 * * *"
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void execute() {
        log.info("Cron Job [TokenCleanupJob] - Started");
        try {
            securityService.cleanupExpiredTokens();
            log.info("Cron Job [TokenCleanupJob] - Finished successfully");
        } catch (Exception e) {
            log.error("Cron Job [TokenCleanupJob] - Failed with error: {}", e.getMessage());
        }
    }
}
