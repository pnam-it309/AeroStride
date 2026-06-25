package com.example.be.core.customer.landing.service.impl;

import com.example.be.core.customer.landing.service.CustomerLandingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerLandingWarmup implements ApplicationRunner {

    private final CustomerLandingService landingService;

    @Override
    @Async
    public void run(ApplicationArguments args) {
        log.info("[AppWarmup] Bắt đầu chạy ngầm nạp sẵn dữ liệu Landing Page vào bộ nhớ/cache...");
        try {
            long startTime = System.currentTimeMillis();
            landingService.getLandingProducts(6);
            landingService.getFeaturedVariants(12);
            landingService.getTopVariantsByQuantity(5);
            landingService.getLandingFeatures();
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[AppWarmup] Hoàn tất warm-up Landing Page trong {} ms!", elapsed);
        } catch (Exception e) {
            log.warn("[AppWarmup] Lỗi khi warm-up dữ liệu Landing Page: {}", e.getMessage());
        }
    }
}
