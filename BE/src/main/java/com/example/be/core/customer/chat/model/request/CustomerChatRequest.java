package com.example.be.core.customer.chat.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerChatRequest {
    private String conversationId;
    private String sessionId;
    private String text;
    private String sender;
    private String imageBase64;
    private String image;

    public String getImageBase64() {
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            return imageBase64;
        }
        return image;
    }
}
