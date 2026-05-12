import { defineStore } from 'pinia';
import webSocketService from '@/services/auth/websocketService';

export const useNotificationStore = defineStore('notification', {
    state: () => ({
        notifications: [],
        unreadChatCount: 0,
        isConnected: false,
    }),
    
    getters: {
        unreadCount: (state) => state.notifications.filter(n => !n.read).length,
        totalUnread: (state) => state.notifications.filter(n => !n.read).length + state.unreadChatCount,
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
                read: false,
            };
            this.notifications.unshift(notification);
            
            // Limit stored notifications
            if (this.notifications.length > 50) {
                this.notifications.pop();
            }
        },

        markAsRead(id) {
            const index = this.notifications.findIndex(n => n.id === id);
            if (index !== -1) {
                this.notifications[index].read = true;
            }
        },

        markAllAsRead() {
            this.notifications.forEach(n => (n.read = true));
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
