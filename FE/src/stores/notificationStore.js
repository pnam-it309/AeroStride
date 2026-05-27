import { defineStore } from 'pinia';
import webSocketService from '@/services/auth/websocketService';

export const useNotificationStore = defineStore('notification', {
    state: () => ({
        notifications: [],
        unreadChatCount: 0,
        isConnected: false
    }),

    getters: {
        unreadCount: (state) => state.notifications.filter((n) => !n.read).length,
        totalUnread: (state) => state.notifications.filter((n) => !n.read).length + state.unreadChatCount
    },

    actions: {
        init() {
            if (!this.isConnected) {
                this.connect();
            }
        },

        connect() {
            webSocketService.connect((message) => {
                this.addNotification(message);
            });
            this.isConnected = true;
        },

        addNotification(message) {
            // Check if it's a chat message
            if (message.conversationId) {
                // Lọc tin nhắn để tránh đếm thông báo cho tin nhắn của chính mình hoặc tin nhắn không thuộc phạm vi xử lý
                const userStr = sessionStorage.getItem('user');
                const user = userStr ? JSON.parse(userStr) : null;
                const currentUsername = user?.username;

                // 1. Không bao giờ thông báo tin nhắn do chính mình gửi đi
                if (message.sender === currentUsername) {
                    return;
                }

                // 2. Chỉ xử lý các tin nhắn thuộc về mình (là người nhận/tiếp nhận) hoặc là tin nhắn chờ (PENDING - chưa có staffId)
                const isMyChat = !message.staffId || message.staffId === currentUsername || message.secondStaffId === currentUsername;
                if (!isMyChat) {
                    return;
                }

                // If not on chat page, increment unread chat count
                if (!window.location.pathname.includes('/quan-ly-chat')) {
                    this.unreadChatCount++;
                }
                return;
            }

            const notification = {
                id: Date.now(),
                title: message.title || 'Thông báo mới',
                message: message.message || message.body || 'Bạn có một thông báo mới',
                type: message.type || 'info',
                timestamp: new Date(),
                read: false
            };
            this.notifications.unshift(notification);

            // Limit stored notifications
            if (this.notifications.length > 50) {
                this.notifications.pop();
            }
        },

        markAsRead(id) {
            const index = this.notifications.findIndex((n) => n.id === id);
            if (index !== -1) {
                this.notifications[index].read = true;
            }
        },

        markAllAsRead() {
            this.notifications.forEach((n) => (n.read = true));
        },

        clearAll() {
            this.notifications = [];
        },

        incrementUnreadChat() {
            this.unreadChatCount++;
        },

        setUnreadChatCount(count) {
            this.unreadChatCount = count;
        },

        resetUnreadChat() {
            this.unreadChatCount = 0;
        },

        disconnect() {
            webSocketService.disconnect();
            this.isConnected = false;
        }
    }
});
