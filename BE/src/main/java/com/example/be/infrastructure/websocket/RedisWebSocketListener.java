package com.example.be.infrastructure.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class RedisWebSocketListener implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    public RedisWebSocketListener(@Lazy SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        
        log.info("Received message from Redis channel: {} - Content: {}", channel, body);

        // Chuyển tiếp tin nhắn từ Redis sang WebSocket cho tất cả user
        if (channel.equals("notifications")) {
            messagingTemplate.convertAndSend("/topic/notifications", body);
        }
    }
}
