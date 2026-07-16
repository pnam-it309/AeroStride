import { defineStore } from 'pinia';
import webSocketService from '@/services/auth/websocketService';

export const useNotificationStore = defineStore('notification', {
    state: () => ({
        notifications: [],
        // Danh sách id các cuộc hội thoại có tin nhắn chưa đọc (đếm theo CUỘC, không theo từng tin nhắn)
        // -> badge khớp với số cuộc hội thoại ở các tab (Hoạt động/Chờ/Đóng).
        unreadChatConvIds: [],
        isConnected: false
    }),

    getters: {
        unreadCount: (state) => state.notifications.filter((n) => !n.read).length,
        // Số cuộc hội thoại chưa đọc (distinct), dùng cho badge "Quản lý tin nhắn"
        unreadChatCount: (state) => state.unreadChatConvIds.length,
        totalUnread() {
            return this.unreadCount + this.unreadChatCount;
        }
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

                // Nếu không ở trang chat thì đánh dấu CUỘC hội thoại này là chưa đọc (không cộng dồn từng tin)
                if (!window.location.pathname.includes('/quan-ly-chat')) {
                    this.markChatUnread(message.conversationId);
                }
                return;
            }

            // Check if it's a product stock update via websocket
            if (message && (message.type === 'PRODUCT_STOCK_UPDATE' || (message.id && message.soLuongTon !== undefined && message.maChiTietSanPham))) {
                import('@/stores/banHangStore').then(({ useBanHangStore }) => {
                    const banHangStore = useBanHangStore();
                    banHangStore.updateProductStock(message.id, message.soLuongTon);
                });
                window.dispatchEvent(new CustomEvent('product-stock-update', {
                    detail: {
                        id: message.id,
                        idSanPham: message.idSanPham,
                        maChiTietSanPham: message.maChiTietSanPham,
                        soLuongTon: message.soLuongTon
                    }
                }));
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

        // Đánh dấu 1 cuộc hội thoại là chưa đọc (chỉ thêm 1 lần dù có nhiều tin nhắn)
        markChatUnread(conversationId) {
            if (conversationId && !this.unreadChatConvIds.includes(conversationId)) {
                this.unreadChatConvIds.push(conversationId);
            }
        },

        // Giữ tên cũ để tương thích nơi gọi; giờ nhận conversationId
        incrementUnreadChat(conversationId) {
            this.markChatUnread(conversationId);
        },

        resetUnreadChat() {
            this.unreadChatConvIds = [];
        },

        disconnect() {
            webSocketService.disconnect();
            this.isConnected = false;
        }
    }
});
