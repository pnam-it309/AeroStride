<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import api from '@/services/apiService';
import { API_CHAT } from '@/constants/apiPaths';
import { chatSocket } from '@/services/chatSocket';
import { useNotificationStore } from '@/stores/notificationStore';
import { useAuthStore } from '@/stores/authStore';

const notificationStore = useNotificationStore();
const authStore = useAuthStore();
const customers = ref([]);
const activeChat = ref(null);
const chatMessages = ref([]);

// Filter out system messages for admin view
const displayMessages = computed(() => {
    return chatMessages.value.filter(m => m.sender !== 'system');
});

const newMessage = ref('');
const isAccepted = ref(false);
const isLoading = ref(false);
const isMessagesLoading = ref(false);
const messagesContainer = ref(null);

// Filters
const chatType = ref('CUSTOMER'); // 'CUSTOMER', 'INTERNAL'
const chatStatus = ref('ACTIVE'); // 'PENDING', 'ACTIVE', 'CLOSED'
const searchQuery = ref('');

const scrollToBottom = () => {
    setTimeout(() => {
        if (messagesContainer.value) {
            messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
        }
    }, 100);
};

// Computed list based on filters
const filteredConversations = computed(() => {
    return customers.value.filter(c => {
        const matchesType = c.type === chatType.value;
        const matchesStatus = c.status === chatStatus.value;
        const matchesSearch = c.name.toLowerCase().includes(searchQuery.value.toLowerCase());
        return matchesType && matchesStatus && matchesSearch;
    });
});

// Lấy danh sách hội thoại từ Backend
const fetchConversations = async () => {
    isLoading.value = true;
    try {
        const response = await api.get(API_CHAT.CONVERSATIONS);
        customers.value = response.data?.data || [];
    } catch (error) {
        console.error('Lỗi khi tải danh sách hội thoại:', error);
    } finally {
        isLoading.value = false;
    }
};

// Lấy lịch sử tin nhắn của hội thoại đang chọn
const fetchMessages = async (conversationId) => {
    isMessagesLoading.value = true;
    try {
        const response = await api.get(API_CHAT.MESSAGES(conversationId));
        chatMessages.value = response.data?.data || [];
    } catch (error) {
        console.error('Lỗi khi tải tin nhắn:', error);
    } finally {
        isMessagesLoading.value = false;
        scrollToBottom();
    }
};

const sendMessage = async () => {
    if (!newMessage.value.trim() || !activeChat.value) return;
    
    const messageData = {
        conversationId: activeChat.value.id,
        text: newMessage.value,
        sender: authStore.user?.username || 'STAFF'
    };

    try {
        await api.post(API_CHAT.SEND, messageData);
        newMessage.value = '';
        scrollToBottom();
    } catch (error) {
        console.error('Lỗi khi gửi tin nhắn:', error);
    }
};

const selectChat = (customer) => {
    activeChat.value = customer;
    isAccepted.value = customer.isAccepted || false;
    fetchMessages(customer.id);
};

const acceptChat = async () => {
    if (!activeChat.value) return;
    
    try {
        const response = await api.post(API_CHAT.ACCEPT(activeChat.value.id));
        if (response.data?.success) {
            isAccepted.value = true;
            // Update local state
            activeChat.value.status = 'ACTIVE';
            activeChat.value.isAccepted = true;
            
            fetchConversations();
        }
    } catch (error) {
        console.error('Lỗi khi tiếp nhận cuộc trò chuyện:', error);
    }
};

onMounted(() => {
    fetchConversations();
    notificationStore.resetUnreadChat();

    chatSocket.connect(() => {
        chatSocket.subscribe('/topic/notifications', (msg) => {
            fetchConversations();
        });

        chatSocket.subscribe('/topic/messages', (msg) => {
            const data = typeof msg === 'string' ? JSON.parse(msg) : msg;
            if (activeChat.value && data.conversationId === activeChat.value.id) {
                chatMessages.value.push(data);
                scrollToBottom();
            } else {
                notificationStore.incrementUnreadChat();
            }
            fetchConversations();
        });
    });
});
</script>

<template>
    <v-container fluid class="chat-management-page pa-0 fill-height bg-white">
        <v-row no-gutters class="fill-height">
            <!-- Sidebar: Conversation List -->
            <v-col cols="12" md="3" class="border-r d-flex flex-column fill-height shadow-sm">
                <!-- Type Tabs -->
                <v-tabs v-model="chatType" color="primary" grow density="compact" class="border-b">
                    <v-tab value="CUSTOMER">KHÁCH HÀNG</v-tab>
                    <v-tab value="INTERNAL">NỘI BỘ</v-tab>
                </v-tabs>

                <!-- Status Filters -->
                <div class="pa-3 border-b bg-grey-lighten-4">
                    <v-chip-group v-model="chatStatus" mandatory selected-class="bg-primary text-white" class="status-chips">
                        <v-chip value="ACTIVE" size="small" variant="flat">Đang hoạt động</v-chip>
                        <v-chip value="PENDING" size="small" variant="flat">Chờ nhận</v-chip>
                        <v-chip value="CLOSED" size="small" variant="flat">Đóng</v-chip>
                    </v-chip-group>
                    
                    <v-text-field
                        v-model="searchQuery"
                        prepend-inner-icon="mdi-magnify"
                        placeholder="Tìm kiếm..."
                        variant="solo"
                        flat
                        density="compact"
                        bg-color="white"
                        hide-details
                        class="mt-2 rounded-lg border"
                    ></v-text-field>
                </div>
                
                <div v-if="isLoading" class="d-flex justify-center align-center py-10">
                    <v-progress-circular indeterminate color="primary"></v-progress-circular>
                </div>

                <v-list v-else class="flex-grow-1 overflow-y-auto pa-0 chat-list">
                    <v-list-item
                        v-for="c in filteredConversations"
                        :key="c.id"
                        :active="activeChat?.id === c.id"
                        class="px-4 py-4 border-b transition-all"
                        @click="selectChat(c)"
                    >
                        <template v-slot:prepend>
                            <v-avatar size="48" color="primary" class="elevation-2">
                                <span class="text-white text-h6">{{ c.avatar || c.name.charAt(0) }}</span>
                            </v-avatar>
                        </template>
                        <v-list-item-title class="font-weight-black text-slate-800">{{ c.name }}</v-list-item-title>
                        <v-list-item-subtitle class="text-truncate mt-1 text-slate-500">{{ c.lastMsg || 'Bắt đầu cuộc trò chuyện...' }}</v-list-item-subtitle>
                        <template v-slot:append>
                            <div class="d-flex flex-column align-end">
                                <span class="text-caption text-grey mb-1">{{ c.time }}</span>
                                <v-badge v-if="c.unread" :content="c.unread" color="error" inline></v-badge>
                            </div>
                        </template>
                    </v-list-item>
                    
                    <div v-if="filteredConversations.length === 0" class="text-center py-16 px-4">
                        <v-icon size="48" color="grey-lighten-1">mdi-message-off-outline</v-icon>
                        <div class="mt-3 text-grey-darken-1 font-weight-medium">Không có cuộc trò chuyện nào</div>
                        <div class="text-caption text-grey">Hãy thử thay đổi bộ lọc hoặc tìm kiếm</div>
                    </div>
                </v-list>
            </v-col>

            <!-- Main Chat Area -->
            <v-col cols="12" md="9" class="bg-slate-50 d-flex flex-column fill-height overflow-hidden">
                <template v-if="activeChat">
                    <!-- Chat Header -->
                    <div class="chat-header pa-4 bg-white border-b d-flex align-center justify-space-between shadow-sm z-10">
                        <div class="d-flex align-center">
                            <v-avatar color="primary" size="40" class="mr-3">
                                <span class="text-white font-weight-bold">{{ activeChat.avatar || activeChat.name.charAt(0) }}</span>
                            </v-avatar>
                            <div>
                                <div class="text-h6 font-weight-black text-slate-800 leading-tight">{{ activeChat.name }}</div>
                                <div class="d-flex align-center">
                                    <span class="status-dot mr-2" :class="activeChat.status.toLowerCase()"></span>
                                    <span class="text-caption font-weight-bold" :class="`text-${activeChat.status === 'ACTIVE' ? 'success' : activeChat.status === 'PENDING' ? 'warning' : 'grey'}`">
                                        {{ activeChat.status === 'ACTIVE' ? 'Đang hoạt động' : activeChat.status === 'PENDING' ? 'Chờ tiếp nhận' : 'Đã đóng' }}
                                    </span>
                                </div>
                            </div>
                        </div>
                        <div class="d-flex align-center gap-2">
                            <v-btn v-if="activeChat.status === 'PENDING'" color="primary" variant="flat" prepend-icon="mdi-check-circle" @click="acceptChat" class="rounded-lg px-6">Tiếp nhận</v-btn>
                            <v-btn icon="mdi-dots-vertical" variant="text" color="slate-600"></v-btn>
                        </div>
                    </div>

                    <!-- Messages -->
                    <div ref="messagesContainer" class="chat-messages-area pa-6 flex-grow-1 overflow-y-auto bg-transparent">
                        <div v-if="isMessagesLoading" class="d-flex justify-center align-center fill-height">
                            <v-progress-circular indeterminate color="primary" size="32"></v-progress-circular>
                        </div>
                        <template v-else>
                            <div v-for="(m, idx) in displayMessages" :key="m.id || idx" 
                                 class="msg-wrapper mb-4 d-flex"
                                 :class="m.sender === authStore.user?.username ? 'justify-end' : 'justify-start'">
                                <div class="msg-box" :class="m.sender === authStore.user?.username ? 'msg-staff' : 'msg-other'">
                                    <div class="msg-content">{{ m.text }}</div>
                                    <div class="msg-footer d-flex align-center justify-end mt-1">
                                        <span class="msg-time">{{ m.time }}</span>
                                        <v-icon v-if="m.sender === authStore.user?.username" size="14" color="blue" class="ml-1">mdi-check-all</v-icon>
                                    </div>
                                </div>
                            </div>
                        </template>
                    </div>

                    <!-- Input Area -->
                    <div class="chat-input-container pa-4 bg-white border-t position-relative shadow-top">
                        <div v-if="activeChat.status === 'PENDING'" class="overlay-lock d-flex align-center justify-center">
                            <div class="text-center">
                                <v-icon color="primary" size="32" class="mb-2">mdi-shield-lock-outline</v-icon>
                                <div class="text-subtitle-1 font-weight-bold text-slate-700">Vui lòng tiếp nhận cuộc trò chuyện</div>
                                <div class="text-caption text-slate-500">Bạn cần tiếp nhận để bắt đầu gửi tin nhắn</div>
                            </div>
                        </div>

                        <v-row no-gutters align="center" :class="{ 'blur-sm': activeChat.status === 'PENDING' }">
                            <v-col>
                                <v-textarea
                                    v-model="newMessage"
                                    placeholder="Nhập tin nhắn..."
                                    rows="1"
                                    auto-grow
                                    variant="solo"
                                    flat
                                    bg-color="slate-50"
                                    hide-details
                                    density="comfortable"
                                    class="rounded-xl chat-textarea"
                                    @keyup.enter.exact.prevent="sendMessage"
                                    :disabled="activeChat.status === 'PENDING' || activeChat.status === 'CLOSED'"
                                ></v-textarea>
                            </v-col>
                            <v-btn icon="mdi-emoticon-happy-outline" variant="text" color="slate-500" class="mx-2"></v-btn>
                            <v-btn color="primary" icon="mdi-send" elevation="2" @click="sendMessage" :disabled="!newMessage.trim() || activeChat.status === 'PENDING' || activeChat.status === 'CLOSED'"></v-btn>
                        </v-row>
                    </div>
                </template>

                <!-- Empty State -->
                <div v-else class="d-flex flex-column justify-center align-center fill-height text-slate-400">
                    <div class="empty-chat-illustration mb-6">
                        <v-icon size="120" color="primary-lighten-4">mdi-forum-outline</v-icon>
                    </div>
                    <div class="text-h5 font-weight-black text-slate-800">Trung tâm tin nhắn</div>
                    <div class="text-subtitle-1">Chọn một hội thoại bên trái để bắt đầu làm việc</div>
                </div>
            </v-col>
        </v-row>
    </v-container>
</template>

<style scoped lang="scss">
.chat-management-page {
    height: calc(100vh - 64px);
    overflow: hidden;
}

.status-chips {
    :deep(.v-chip) {
        font-weight: 700 !important;
        letter-spacing: 0.5px;
    }
}

.chat-list-item {
    &.v-list-item--active {
        background-color: #EEF2FF !important;
        border-right: 4px solid #4F46E5;
    }
}

.status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    display: inline-block;
    &.active { background-color: #10B981; box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2); }
    &.pending { background-color: #F59E0B; }
    &.closed { background-color: #94A3B8; }
}

.msg-wrapper {
    width: 100%;
}

.msg-box {
    max-width: 65%;
    padding: 10px 14px;
    position: relative;
    box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.msg-staff {
    background-color: #4F46E5;
    color: white;
    border-radius: 16px 16px 4px 16px;
    .msg-time { color: rgba(255, 255, 255, 0.7); }
}

.msg-other {
    background-color: white;
    color: #1E293B;
    border-radius: 16px 16px 16px 4px;
    border: 1px solid #E2E8F0;
    .msg-time { color: #94A3B8; }
}

.msg-time {
    font-size: 10px;
    font-weight: 600;
}

.overlay-lock {
    position: absolute;
    inset: 0;
    background: rgba(255, 255, 255, 0.9);
    z-index: 10;
    backdrop-filter: blur(2px);
}

.chat-textarea {
    :deep(.v-field__input) {
        padding-top: 12px;
        padding-bottom: 12px;
    }
}

.shadow-top {
    box-shadow: 0 -4px 12px rgba(0,0,0,0.03);
}

.z-10 { z-index: 10; }
.leading-tight { line-height: 1.25; }
.text-slate-800 { color: #1E293B; }
.text-slate-500 { color: #64748B; }
.bg-slate-50 { background-color: #F8FAFC; }
.transition-all { transition: all 0.2s ease; }
</style>
