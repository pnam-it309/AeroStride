package com.example.be.core.admin.chat.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ChatMessageResponse {
    private String id;
    private String conversationId;
    private String sender; // customer, staff, system
    private String text;
    private String time;
}
