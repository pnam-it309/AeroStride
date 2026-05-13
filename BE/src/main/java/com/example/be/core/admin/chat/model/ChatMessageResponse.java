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
    private String sessionId;
    private String staffId; // Assigned staff for customer chat or sender for internal
    private String secondStaffId; // Receiver for internal chat
    private String sender; // customer, staff, system
    private String text;
    private String time;
}
