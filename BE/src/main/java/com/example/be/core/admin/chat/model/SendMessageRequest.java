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

    // text hoặc imageBase64 phải có ít nhất 1 cái (validation thủ công trong service)
    private String text;

    @NotBlank(message = "Sender không được để trống")
    private String sender;

    // Ảnh dưới dạng base64 (tùy chọn, có thể null nếu chỉ gửi text)
    private String imageBase64;
}
