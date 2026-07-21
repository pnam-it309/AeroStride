package com.example.be.core.admin.chat.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho endpoint gửi tin nhắn chat.
 * Thay thế Map<String, String> để có type safety và validation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotBlank(message = "Conversation ID không được để trống")
    private String conversationId;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String text;

    @NotBlank(message = "Sender không được để trống")
    private String sender;
}
