package com.example.be.infrastructure.jobs;

import com.example.be.core.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupJob {

    private final OrderService orderService;

    /**
     * Runs every hour to clean up pending orders older than 24 hours.
     * Cron: "0 0 * * * *"
     */
    @Scheduled(cron = "0 0 * * * *")
    public void execute() {
        log.info("Cron Job [OrderCleanupJob] - Started");
        try {
            orderService.cleanupExpiredPendingOrders();
            log.info("Cron Job [OrderCleanupJob] - Finished successfully");
        } catch (Exception e) {
            log.error("Cron Job [OrderCleanupJob] - Failed with error: {}", e.getMessage());
        }
    }
}
