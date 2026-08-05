package com.example.be.infrastructure.jobs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * KeepAliveJob
 * Cron Job tự động chạy định kỳ 14 phút 1 lần.
 * Cú pháp cron "0 *&#47;14 * * * *" đảm bảo job KHÔNG chạy khi ứng dụng vừa khởi động (startup),
 * mà chỉ chạy đúng vào các thời điểm phút 0, 14, 28, 42, 56 của mỗi giờ.
 */
@Component
@Slf4j
public class KeepAliveJob {

    @Scheduled(cron = "0 */14 * * * *")
    public void execute() {
        log.info("Cron Job [KeepAliveJob] - Heartbeat/ping định kỳ 14 phút được thực thi lúc: {}", LocalDateTime.now());
    }
}
