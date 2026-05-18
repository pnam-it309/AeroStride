package com.example.be.core.admin.chat.service;

import com.example.be.core.admin.chat.model.ChatMessageResponse;
import com.example.be.core.admin.chat.repository.AdminChatConversationRepository;
import com.example.be.core.admin.chat.repository.AdminChatMessageRepository;
import com.example.be.core.admin.chat.service.impl.AdminChatServiceImpl;
import com.example.be.entity.ChatConversation;
import com.example.be.entity.ChatMessage;
import com.example.be.repository.NhanVienRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminChatServiceTest {

    @Mock
    private AdminChatConversationRepository conversationRepository;

    @Mock
    private AdminChatMessageRepository messageRepository;

    @Mock
    private NhanVienRepository nhanVienRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private AdminChatServiceImpl adminChatService;

    @Test
    void sendMessage_whenRedisPublishFails_shouldStillSaveAndBroadcast() {
        ChatConversation savedConversation = org.mockito.Mockito.mock(ChatConversation.class);
        ChatMessage savedMessage = org.mockito.Mockito.mock(ChatMessage.class);

        when(conversationRepository.findBySessionId("session-1")).thenReturn(Optional.empty());
        when(conversationRepository.save(any(ChatConversation.class))).thenReturn(savedConversation);
        when(savedConversation.getId()).thenReturn("conv-1");
        when(messageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);
        when(savedMessage.getId()).thenReturn("msg-1");
        when(savedMessage.getNgayTao()).thenReturn(null);
        doThrow(new DataAccessResourceFailureException("Redis down"))
                .when(redisTemplate).convertAndSend(eq("notifications"), any());

        assertDoesNotThrow(() -> adminChatService.sendMessage(null, "hello", "CUSTOMER", "session-1"));

        verify(conversationRepository).save(any(ChatConversation.class));
        verify(messageRepository).save(any(ChatMessage.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/messages"), any(ChatMessageResponse.class));
        verify(redisTemplate).convertAndSend(eq("notifications"), any());

        ArgumentCaptor<ChatMessageResponse> responseCaptor = ArgumentCaptor.forClass(ChatMessageResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/messages"), responseCaptor.capture());
        assertEquals("conv-1", responseCaptor.getValue().getConversationId());
        assertEquals("hello", responseCaptor.getValue().getText());
    }
}
