package com.example.be.infrastructure.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@AllArgsConstructor
public class NotificationController {

    private final RedisTemplate<String, Object> redisTemplate;

    @MessageMapping("/send-notification")
    public void sendNotification(@Payload String message) {
        NotificationResponse response = NotificationResponse.builder()
                .content(message)
                .timestamp(LocalDateTime.now().toString())
                .build();
        
        // Thay vì gửi trực tiếp, ta publish lên Redis để tất cả các instance đều nhận được
        redisTemplate.convertAndSend("notifications", response);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationResponse {
        private String content;
        private String timestamp;
    }
}
