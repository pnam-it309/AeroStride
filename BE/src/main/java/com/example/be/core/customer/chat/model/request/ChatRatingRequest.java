package com.example.be.core.customer.chat.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRatingRequest {
    private String sessionId;
    private Integer rating;
    private String feedback;
}
