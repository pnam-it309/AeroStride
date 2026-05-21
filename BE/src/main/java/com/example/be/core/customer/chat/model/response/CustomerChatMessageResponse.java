package com.example.be.core.customer.chat.model.response;

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
public class CustomerChatMessageResponse {
    private String id;
    private String conversationId;
    private String sessionId;
    private String sender;
    private String text;
    private String time;
    private String staffId;
    private String secondStaffId;
}
