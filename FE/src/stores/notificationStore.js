import { defineStore } from 'pinia';
import webSocketService from '@/services/auth/websocketService';
import { APP_ROLES } from '@/constants/appConstants';

export const useNotificationStore = defineStore('notification', {
    state: () => ({
        notifications: [],
        // Danh sách id các cuộc hội thoại có tin nhắn chưa đọc (đếm theo CUỘC, không theo từng tin nhắn)
        // -> badge khớp với số cuộc hội thoại ở các tab (Hoạt động/Chờ/Đóng).
        unreadChatConvIds: JSON.parse(localStorage.getItem('unread_chat_conv_ids') || '[]'),
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
            // 0. Xử lý sự kiện đóng phiên trò chuyện -> xóa ngay khỏi danh sách chưa đọc
            if (message.content && typeof message.content === 'string' && message.content.startsWith('CLOSED_CONVERSATION_')) {
                const convId = message.content.replace('CLOSED_CONVERSATION_', '');
                this.markChatRead(convId);
                return;
            }

            // 1. Kiểm tra nếu là thông báo yêu cầu gặp nhân viên (Handoff Request)
            if (message.type === 'CUSTOMER_HANDOFF_REQUEST') {
                const userStr = sessionStorage.getItem('user');
                const user = userStr ? JSON.parse(userStr) : null;
                const isAdminOrStaff = user?.role === APP_ROLES.ADMIN || user?.role === APP_ROLES.STAFF;

                if (isAdminOrStaff) {
                    if (message.conversationId) {
                        this.markChatUnread(message.conversationId);
                    }
                    // Hiển thị thông báo Toast góc trên bên phải
                    import('@/stores/toastStore').then(({ useToastStore }) => {
                        const toastStore = useToastStore();
                        toastStore.showToast(
                            message.message || `Khách hàng [${message.customerName || 'Khách'}] vừa yêu cầu gặp nhân viên hỗ trợ!`,
                            'warning'
                        );
                    });

                    // Thêm vào danh sách chuông thông báo
                    const notification = {
                        id: Date.now(),
                        title: message.title || 'Khách hàng yêu cầu hỗ trợ',
                        message: message.message || 'Khách hàng yêu cầu gặp nhân viên tư vấn',
                        type: 'warning',
                        timestamp: new Date(),
                        read: false
                    };
                    this.notifications.unshift(notification);
                }
                return;
            }

            // 2. Check if it's a chat message
            if (message.conversationId) {
                // Tuyệt đối không đếm số cho tin nhắn bot tự động
                if (message.sender === 'bot' || message.isBot) {
                    return;
                }

                const userStr = sessionStorage.getItem('user');
                const user = userStr ? JSON.parse(userStr) : null;
                const currentUsername = user?.username;

                // Không bao giờ thông báo tin nhắn do chính mình gửi đi
                if (message.sender === currentUsername) {
                    return;
                }

                const isCustomerChat = message.type === 'CUSTOMER' || !message.type || message.loaiHoiThoai === 'CUSTOMER';
                const hasAssignedStaff = !!(message.staffUsername || message.staffId);

                // Cuộc trò chuyện khách hàng chưa được nhân viên tiếp nhận:
                // CHỈ hiển thị con số khi khách hàng có yêu cầu gặp nhân viên (CUSTOMER_HANDOFF_REQUEST đã xử lý ở mục 1).
                // Tuyệt đối không hiện số khi khách chỉ gửi tin nhắn thông thường cho chatbot AI.
                if (isCustomerChat && !hasAssignedStaff) {
                    return;
                }

                // Nếu cuộc trò chuyện khách hàng đã có nhân viên khác tiếp nhận -> người khác không nhận số chưa đọc
                const isAssignedToOther = message.staffUsername && message.staffUsername !== currentUsername;
                if (isAssignedToOther) {
                    return;
                }

                const isMyChat =
                    message.staffUsername === currentUsername ||
                    message.staffId === currentUsername ||
                    message.secondStaffUsername === currentUsername ||
                    message.secondStaffId === currentUsername;

                if (!isMyChat && isCustomerChat) {
                    return;
                }

                if (!window.location.pathname.includes('/quan-ly-chat')) {
                    this.markChatUnread(message.conversationId);
                }
                return;
            }

            // Check if it's a product stock update via websocket
            if (
                message &&
                (message.type === 'PRODUCT_STOCK_UPDATE' || (message.id && message.soLuongTon !== undefined && message.maChiTietSanPham))
            ) {
                import('@/stores/banHangStore').then(({ useBanHangStore }) => {
                    const banHangStore = useBanHangStore();
                    banHangStore.updateProductStock(message.id, message.soLuongTon);
                });
                import('@/stores/cartStore').then(({ useCartStore }) => {
                    const cartStore = useCartStore();
                    cartStore.syncWithBackend();
                });
                window.dispatchEvent(
                    new CustomEvent('product-stock-update', {
                        detail: {
                            id: message.id,
                            idSanPham: message.idSanPham,
                            maChiTietSanPham: message.maChiTietSanPham,
                            soLuongTon: message.soLuongTon
                        }
                    })
                );
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
                localStorage.setItem('unread_chat_conv_ids', JSON.stringify(this.unreadChatConvIds));
            }
        },

        markChatRead(conversationId) {
            if (conversationId) {
                this.unreadChatConvIds = this.unreadChatConvIds.filter(id => id !== conversationId);
                localStorage.setItem('unread_chat_conv_ids', JSON.stringify(this.unreadChatConvIds));
            }
        },

        // Giữ tên cũ để tương thích nơi gọi; giờ nhận conversationId
        incrementUnreadChat(conversationId) {
            this.markChatUnread(conversationId);
        },

        // Đồng bộ danh sách chưa đọc với danh sách hội thoại thực tế từ server
        syncUnreadConversations(conversations) {
            if (!Array.isArray(conversations)) return;
            const userStr = sessionStorage.getItem('user');
            const user = userStr ? JSON.parse(userStr) : null;
            const currentUsername = user?.username;

            const validUnreadIds = new Set(
                conversations
                    .filter((c) => {
                        const status = c.status || c.trangThaiHoiThoai;
                        if (status === 'CLOSED' || status === 'ENDED' || status === 'DONG' || status === 'DA_DONG') {
                            return false;
                        }
                        const type = c.type || c.loaiHoiThoai || 'CUSTOMER';
                        if (type === 'CUSTOMER') {
                            // 1. Nếu chưa ai tiếp nhận -> CHỈ hiển thị số khi có số chưa đọc (> 0) từ server
                            if (!c.isAccepted && !c.daChapNhan) {
                                return (c.unread > 0 || c.chuaDoc > 0);
                            }
                            // 2. Nếu ĐÃ TIẾP NHẬN -> CHỈ nhân viên đang tiếp nhận mới thấy số
                            if (currentUsername && (c.staffUsername === currentUsername || c.staffId === currentUsername || c.staffId === user?.id)) {
                                return (c.unread > 0 || c.chuaDoc > 0);
                            }
                            return false;
                        } else {
                            // INTERNAL
                            return (c.unread > 0 || c.chuaDoc > 0);
                        }
                    })
                    .map((c) => c.id)
            );
            this.unreadChatConvIds = this.unreadChatConvIds.filter((id) => validUnreadIds.has(id));
            localStorage.setItem('unread_chat_conv_ids', JSON.stringify(this.unreadChatConvIds));
        },

        resetUnreadChat() {
            this.unreadChatConvIds = [];
            localStorage.removeItem('unread_chat_conv_ids');
        },

        disconnect() {
            webSocketService.disconnect();
            this.isConnected = false;
        }
    }
});
