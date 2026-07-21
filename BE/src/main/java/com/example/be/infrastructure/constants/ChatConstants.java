package com.example.be.infrastructure.constants;

/**
 * CHAT_CONSTANTS - Các hằng số cho module Chat
 */
public final class ChatConstants {

    private ChatConstants() {}

    // Topics
    public static final String TOPIC_MESSAGES = "/topic/messages";
    public static final String TOPIC_NOTIFICATIONS = "/topic/notifications";
    
    // Redis Channels
    public static final String REDIS_CHANNEL_NOTIFICATIONS = "notifications";

    // Sender Types
    public static final String SENDER_TYPE_STAFF = "staff";
    public static final String SENDER_TYPE_CUSTOMER = "customer";
    public static final String SENDER_TYPE_SYSTEM = "system";

    // Notification Messages
    public static final String MSG_NEW_CUSTOMER_WAITING = "Có khách vãng lai mới đang chờ hỗ trợ!";
    
    // Error Messages (Reusing MessageConstants is also possible, but these are chat-specific)
    public static final String ERR_CONVERSATION_NOT_ACCEPTED = "Cuộc trò chuyện chưa được tiếp nhận";
    public static final String ERR_SENDER_NOT_FOUND = "Người gửi không tồn tại";
    public static final String ERR_CONVERSATION_NOT_FOUND = "Cuộc hội thoại không tồn tại";
    
    // Display Names
    public static final String DEFAULT_STAFF_NAME = "Nhân viên";
    public static final String DEFAULT_CUSTOMER_NAME = "Khách vãng lai";
}
