package com.example.be.core.admin.chat.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class AdminChatResponse {
    private String id;
    private String name;
    private String lastMsg;
    private String time;
    private Integer unread;
    private String avatar;
    private Boolean isAccepted;
    private String type; // CUSTOMER, INTERNAL
    private String status; // PENDING, ACTIVE, CLOSED
}
