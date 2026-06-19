<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import api from '@/services/apiService';
import { API_CHAT } from '@/constants/apiPaths';
import { chatSocket } from '@/services/chatSocket';
import { useNotificationStore } from '@/stores/notificationStore';
import { useAuthStore } from '@/stores/authStore';
import { CHAT_TYPES, CHAT_SENDER_TYPE, CHAT_STATUS, CHAT_TOPICS } from '@/constants/appConstants';

const notificationStore = useNotificationStore();
const authStore = useAuthStore();
const customers = ref([]);
const activeChat = ref(null);
const chatMessages = ref([]);

// Filter out system messages for admin view
const displayMessages = computed(() => {
    return chatMessages.value.filter((m) => m.sender !== CHAT_SENDER_TYPE.SYSTEM);
});

const newMessage = ref('');
const isAccepted = ref(false);
const isLoading = ref(false);
const isMessagesLoading = ref(false);
const messagesContainer = ref(null);

// Filters
const chatType = ref(CHAT_TYPES.CUSTOMER);
const chatStatus = ref(CHAT_STATUS.ACTIVE);
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
    return customers.value.filter((c) => {
        const matchesType = c.type === chatType.value;
        const matchesStatus = c.status === chatStatus.value;
        const matchesSearch = c.name.toLowerCase().includes(searchQuery.value.toLowerCase());
        return matchesType && matchesStatus && matchesSearch;
    });
});

const activeCount = computed(() => {
    return customers.value.filter((c) => c.type === chatType.value && c.status === 'ACTIVE').length;
});

const pendingCount = computed(() => {
    return customers.value.filter((c) => c.type === chatType.value && c.status === 'PENDING').length;
});

const closedCount = computed(() => {
    return customers.value.filter((c) => c.type === chatType.value && c.status === 'CLOSED').length;
});

// Lấy danh sách hội thoại từ Backend
const fetchConversations = async (quiet = false) => {
    if (!quiet && customers.value.length === 0) {
        isLoading.value = true;
    }
    try {
        const response = await api.get(API_CHAT.CONVERSATIONS);
        customers.value = response.data?.data || [];

        if (activeChat.value) {
            const updatedChat = customers.value.find((c) => c.id === activeChat.value.id);
            if (updatedChat) {
                activeChat.value.status = updatedChat.status;
                activeChat.value.isAccepted = updatedChat.isAccepted;
            }
        }

        if (activeChat.value && activeChat.value.id.startsWith('NEW_INTERNAL_')) {
            const realConv = customers.value.find(
                (c) => c.type === CHAT_TYPES.INTERNAL && c.name === activeChat.value.name && !c.id.startsWith('NEW_INTERNAL_')
            );
            if (realConv) {
                activeChat.value = realConv;
                fetchMessages(realConv.id);
            }
        }
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
        fetchConversations(true);
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
            activeChat.value.status = 'ACTIVE';
            activeChat.value.isAccepted = true;
            fetchConversations(true);
        }
    } catch (error) {
        console.error('Lỗi khi tiếp nhận cuộc trò chuyện:', error);
    }
};

const closeChat = async () => {
    if (!activeChat.value) return;

    try {
        const response = await api.post(API_CHAT.CLOSE(activeChat.value.id));
        if (response.data?.success) {
            activeChat.value.status = 'CLOSED';
            fetchConversations(true);
        }
    } catch (error) {
        console.error('Lỗi khi đóng cuộc trò chuyện:', error);
    }
};

onMounted(() => {
    fetchConversations();
    notificationStore.resetUnreadChat();

    chatSocket.connect(() => {
        chatSocket.subscribe(CHAT_TOPICS.NOTIFICATIONS, (msg) => {
            fetchConversations(true);
        });

        chatSocket.subscribe(CHAT_TOPICS.MESSAGES, (msg) => {
            const data = typeof msg === 'string' ? JSON.parse(msg) : msg;

            const currentUsername = authStore.user?.username;
            const isMyChat = !data.staffId || data.staffId === currentUsername || data.secondStaffId === currentUsername;

            if (!isMyChat) return;

            if (data.sender === currentUsername) {
                if (activeChat.value && data.conversationId === activeChat.value.id) {
                    if (!chatMessages.value.find((m) => m.id === data.id)) {
                        chatMessages.value.push(data);
                        scrollToBottom();
                    }
                }
                fetchConversations(true);
                return;
            }

            if (activeChat.value && data.conversationId === activeChat.value.id) {
                chatMessages.value.push(data);
                scrollToBottom();
            } else {
                notificationStore.incrementUnreadChat();
            }
            fetchConversations(true);
        });
    });
});
</script>

<template>
    <v-container fluid class="chat-page pa-0 fill-height">
        <v-row no-gutters class="fill-height">
            <!-- Sidebar -->
            <v-col cols="12" md="3" class="sidebar d-flex flex-column fill-height">
                <!-- Logo Header -->
                <div class="sidebar-header">
                    <v-icon icon="mdi-message-text" class="mr-2" color="#1a56db" size="22"></v-icon>
                    <span class="sidebar-title">Quản lý tin nhắn</span>
                </div>

                <!-- Type Tabs -->
                <v-tabs v-model="chatType" color="#1a56db" bg-color="transparent" grow density="compact" class="type-tabs">
                    <v-tab :value="CHAT_TYPES.CUSTOMER">
                        <v-icon icon="mdi-account" size="18" class="mr-1"></v-icon>
                        Khách hàng
                    </v-tab>
                    <v-tab :value="CHAT_TYPES.INTERNAL">
                        <v-icon icon="mdi-account-group" size="18" class="mr-1"></v-icon>
                        Nội bộ
                    </v-tab>
                </v-tabs>

                <!-- Status Filters -->
                <div class="filter-section">
                    <v-chip-group v-model="chatStatus" mandatory selected-class="chip-active" class="status-chips">
                        <v-chip value="ACTIVE" size="small" variant="outlined" color="#1a56db">
                            <v-icon icon="mdi-circle" size="8" class="mr-1" color="success"></v-icon>
                            Hoạt động ({{ activeCount }})
                        </v-chip>
                        <v-chip value="PENDING" size="small" variant="outlined" color="#1a56db">
                            <v-icon icon="mdi-circle" size="8" class="mr-1" color="amber-darken-2"></v-icon>
                            Chờ ({{ pendingCount }})
                        </v-chip>
                        <v-chip value="CLOSED" size="small" variant="outlined" color="#1a56db">
                            <v-icon icon="mdi-circle" size="8" class="mr-1" color="grey-darken-1"></v-icon>
                            Đóng ({{ closedCount }})
                        </v-chip>
                    </v-chip-group>

                    <v-text-field
                        v-model="searchQuery"
                        prepend-inner-icon="mdi-magnify"
                        placeholder="Tìm cuộc trò chuyện..."
                        variant="solo"
                        flat
                        density="compact"
                        bg-color="#f0f4f8"
                        hide-details
                        class="mt-2 search-field"
                    ></v-text-field>
                </div>

                <div v-if="isLoading" class="d-flex justify-center align-center py-10">
                    <v-progress-circular indeterminate color="#1a56db"></v-progress-circular>
                </div>

                <v-list v-else class="flex-grow-1 overflow-y-auto pa-0 conv-list" bg-color="transparent">
                    <v-list-item
                        v-for="c in filteredConversations"
                        :key="c.id"
                        :active="activeChat?.id === c.id"
                        class="conv-item"
                        @click="selectChat(c)"
                    >
                        <template v-slot:prepend>
                            <v-avatar size="44" class="conv-avatar">
                                <v-img
                                    :src="
                                        !c.avatar || c.avatar.length <= 2
                                            ? 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png'
                                            : c.avatar
                                    "
                                    alt="avatar"
                                ></v-img>
                            </v-avatar>
                        </template>
                        <v-list-item-title class="conv-name">{{ c.name }}</v-list-item-title>
                        <v-list-item-subtitle class="conv-msg">{{ c.lastMsg || 'Bắt đầu trò chuyện...' }}</v-list-item-subtitle>
                        <template v-slot:append>
                            <div class="d-flex flex-column align-end">
                                <span class="conv-time">{{ c.time }}</span>
                                <v-badge v-if="c.unread" :content="c.unread" color="red" inline class="mt-1"></v-badge>
                            </div>
                        </template>
                    </v-list-item>

                    <div v-if="filteredConversations.length === 0" class="text-center py-16 px-4">
                        <v-icon size="48" color="grey-lighten-1">mdi-message-off-outline</v-icon>
                        <div class="mt-3" style="color: #64748b">Không có cuộc trò chuyện nào</div>
                        <div class="text-caption" style="color: #94a3b8">Hãy thử thay đổi bộ lọc</div>
                    </div>
                </v-list>
            </v-col>

            <!-- Main Chat Area -->
            <v-col cols="12" md="9" class="main-area d-flex flex-column fill-height overflow-hidden">
                <template v-if="activeChat">
                    <!-- Chat Header -->
                    <div class="main-header">
                        <div class="d-flex align-center">
                            <v-avatar size="42" class="mr-3 main-avatar">
                                <v-img
                                    :src="
                                        !activeChat.avatar || activeChat.avatar.length <= 2
                                            ? 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png'
                                            : activeChat.avatar
                                    "
                                    alt="avatar"
                                ></v-img>
                            </v-avatar>
                            <div>
                                <div class="main-chat-name">{{ activeChat.name }}</div>
                                <div class="d-flex align-center">
                                    <span class="status-indicator" :class="activeChat.status.toLowerCase()"></span>
                                    <span class="status-label" :class="activeChat.status.toLowerCase()">
                                        {{
                                            activeChat.status === 'ACTIVE'
                                                ? 'Đang hoạt động'
                                                : activeChat.status === 'PENDING'
                                                  ? 'Chờ tiếp nhận'
                                                  : 'Đã đóng'
                                        }}
                                    </span>
                                </div>
                            </div>
                        </div>
                        <div class="d-flex align-center ga-2">
                            <v-btn
                                v-if="activeChat.status === 'PENDING'"
                                color="#1a56db"
                                variant="flat"
                                prepend-icon="mdi-check-circle"
                                @click="acceptChat"
                                class="rounded-lg px-6 text-none font-weight-bold"
                                style="letter-spacing: 0"
                                >Tiếp nhận</v-btn
                            >
                            <v-btn
                                v-if="activeChat.status === 'PENDING' || activeChat.status === 'ACTIVE'"
                                color="error"
                                variant="flat"
                                prepend-icon="mdi-close-circle"
                                @click="closeChat"
                                class="rounded-lg px-6 text-none font-weight-bold"
                                style="letter-spacing: 0"
                                >Đóng phiên</v-btn
                            >
                            <v-btn icon="mdi-dots-vertical" variant="text" color="grey-darken-1"></v-btn>
                        </div>
                    </div>

                    <!-- Messages -->
                    <div ref="messagesContainer" class="messages-area flex-grow-1 overflow-y-auto">
                        <div v-if="isMessagesLoading" class="d-flex justify-center align-center fill-height">
                            <v-progress-circular indeterminate color="#1a56db" size="32"></v-progress-circular>
                        </div>
                        <template v-else>
                            <div
                                v-for="(m, idx) in displayMessages"
                                :key="m.id || idx"
                                class="msg-row"
                                :class="(m.sender === authStore.user?.username || m.sender === 'bot' || m.sender === 'SYSTEM') ? 'is-mine' : 'is-other'"
                            >
                                <div class="msg-bubble" :class="(m.sender === authStore.user?.username || m.sender === 'bot' || m.sender === 'SYSTEM') ? 'bubble-mine' : 'bubble-other'">
                                    <div class="bubble-text">{{ m.text }}</div>
                                    <div class="bubble-meta">
                                        <span class="bubble-time">{{ m.time }}</span>
                                        <v-icon
                                            v-if="m.sender === authStore.user?.username || m.sender === 'bot' || m.sender === 'SYSTEM'"
                                            size="14"
                                            color="rgba(255,255,255,0.7)"
                                            class="ml-1"
                                            >mdi-check-all</v-icon
                                        >
                                    </div>
                                </div>
                            </div>
                        </template>
                    </div>

                    <!-- Input Area -->
                    <div class="input-area">
                        <div v-if="activeChat.status === 'PENDING'" class="lock-overlay">
                            <v-icon color="#1a56db" size="32" class="mb-2">mdi-shield-lock-outline</v-icon>
                            <div class="lock-title">Vui lòng tiếp nhận cuộc trò chuyện</div>
                            <div class="lock-sub">Bạn cần tiếp nhận để bắt đầu gửi tin nhắn</div>
                        </div>

                        <v-row no-gutters align="center" :class="{ 'input-blur': activeChat.status === 'PENDING' }">
                            <v-col>
                                <v-textarea
                                    v-model="newMessage"
                                    placeholder="Nhập tin nhắn..."
                                    rows="1"
                                    auto-grow
                                    variant="solo"
                                    flat
                                    bg-color="#f0f4f8"
                                    hide-details
                                    density="comfortable"
                                    class="rounded-xl input-textarea"
                                    @keyup.enter.exact.prevent="sendMessage"
                                    :disabled="activeChat.status === 'PENDING' || activeChat.status === 'CLOSED'"
                                ></v-textarea>
                            </v-col>
                            <v-btn
                                icon="mdi-send"
                                color="#1a56db"
                                variant="flat"
                                elevation="0"
                                class="ml-3 rounded-xl"
                                @click="sendMessage"
                                :disabled="!newMessage.trim() || activeChat.status === 'PENDING' || activeChat.status === 'CLOSED'"
                            ></v-btn>
                        </v-row>
                    </div>
                </template>

                <!-- Empty State -->
                <div v-else class="d-flex flex-column justify-center align-center fill-height empty-state">
                    <div class="empty-icon-wrap">
                        <v-icon size="64" color="#1a56db">mdi-forum-outline</v-icon>
                    </div>
                    <div class="empty-title">Trung tâm tin nhắn</div>
                    <div class="empty-sub">Chọn một hội thoại bên trái để bắt đầu</div>
                </div>
            </v-col>
        </v-row>
    </v-container>
</template>

<style scoped lang="scss">
$blue-primary: #1a56db;
$blue-dark: #143fa6;
$blue-light: #e8eefb;
$blue-bg: #f0f4f8;

.chat-page {
    height: calc(100vh - 64px);
    overflow: hidden;
    background: #fff;
}

/* ========== SIDEBAR ========== */
.sidebar {
    background: #ffffff;
    border-right: 1px solid #e5eaf0;
}

.sidebar-header {
    padding: 18px 20px;
    display: flex;
    align-items: center;
    border-bottom: 1px solid #e5eaf0;
}

.sidebar-title {
    color: #1e293b;
    font-weight: 800;
    font-size: 1rem;
    letter-spacing: -0.01em;
}

.type-tabs {
    border-bottom: 1px solid #e5eaf0;
    :deep(.v-tab) {
        color: #64748b !important;
        font-weight: 700;
        font-size: 0.8rem;
        letter-spacing: 0.02em;
        text-transform: none;
        &.v-tab--selected {
            color: #1a56db !important;
        }
    }
    :deep(.v-tab-slider) {
        color: #1a56db !important;
    }
}

.filter-section {
    padding: 12px 16px;
    border-bottom: 1px solid #e5eaf0;
}

.status-chips {
    :deep(.v-chip) {
        font-weight: 600 !important;
        font-size: 0.7rem;
        border-color: #cbd5e1 !important;
        color: #64748b !important;
    }
    :deep(.chip-active) {
        background: #e8eefb !important;
        border-color: #1a56db !important;
        color: #1a56db !important;
    }
}

.search-field {
    :deep(.v-field) {
        border-radius: 10px !important;
        color: #1e293b;
    }
    :deep(.v-field__input) {
        color: #1e293b !important;
        font-size: 0.85rem;
        &::placeholder {
            color: #94a3b8 !important;
        }
    }
    :deep(.v-icon) {
        color: #94a3b8 !important;
    }
}

.conv-list {
    &::-webkit-scrollbar {
        width: 4px;
    }
    &::-webkit-scrollbar-thumb {
        background: #cbd5e1;
        border-radius: 10px;
    }
}

.conv-item {
    padding: 14px 16px !important;
    border-bottom: 1px solid #f1f5f9;
    transition: background 0.2s ease;
    &:hover {
        background: #f8fafc !important;
    }
    &.v-list-item--active {
        background: #e8eefb !important;
        border-left: 3px solid #1a56db;
    }
}

.conv-avatar {
    border: 2px solid #e5eaf0;
    background: #f8fafc;
}

.conv-name {
    color: #1e293b !important;
    font-weight: 700 !important;
    font-size: 0.88rem !important;
}

.conv-msg {
    color: #64748b !important;
    font-size: 0.78rem !important;
    margin-top: 2px !important;
}

.conv-time {
    font-size: 0.65rem;
    color: #94a3b8;
    font-weight: 600;
}

/* ========== MAIN AREA ========== */
.main-area {
    background: $blue-bg;
}

.main-header {
    padding: 16px 24px;
    background: #fff;
    border-bottom: 1px solid #e5eaf0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
    z-index: 10;
}

.main-avatar {
    border: 2px solid $blue-light;
    background: $blue-light;
}

.main-chat-name {
    font-size: 1.05rem;
    font-weight: 800;
    color: #1e293b;
    line-height: 1.25;
}

.status-indicator {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    display: inline-block;
    margin-right: 6px;
    &.active {
        background: #10b981;
        box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
    }
    &.pending {
        background: #f59e0b;
    }
    &.closed {
        background: #94a3b8;
    }
}

.status-label {
    font-size: 0.75rem;
    font-weight: 700;
    &.active {
        color: #10b981;
    }
    &.pending {
        color: #f59e0b;
    }
    &.closed {
        color: #94a3b8;
    }
}

/* ========== MESSAGES ========== */
.messages-area {
    padding: 24px;
    &::-webkit-scrollbar {
        width: 5px;
    }
    &::-webkit-scrollbar-thumb {
        background: #d0d7e2;
        border-radius: 10px;
    }
}

.msg-row {
    display: flex;
    margin-bottom: 12px;
    width: 100%;
    &.is-mine {
        justify-content: flex-end;
    }
    &.is-other {
        justify-content: flex-start;
    }
}

.msg-bubble {
    max-width: 60%;
    padding: 12px 16px;
    position: relative;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.bubble-mine {
    background: $blue-primary;
    color: #fff;
    border-radius: 18px 18px 4px 18px;
    .bubble-time {
        color: rgba(255, 255, 255, 0.65);
    }
}

.bubble-other {
    background: #fff;
    color: #1e293b;
    border-radius: 18px 18px 18px 4px;
    border: 1px solid #e5eaf0;
    .bubble-time {
        color: #94a3b8;
    }
}

.bubble-text {
    font-size: 0.9rem;
    line-height: 1.5;
    word-break: break-word;
}

.bubble-meta {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    margin-top: 4px;
}

.bubble-time {
    font-size: 0.65rem;
    font-weight: 600;
}

/* ========== INPUT ========== */
.input-area {
    padding: 16px 24px;
    background: #fff;
    border-top: 1px solid #e5eaf0;
    position: relative;
    box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.03);
}

.input-textarea {
    :deep(.v-field__input) {
        padding-top: 12px;
        padding-bottom: 12px;
    }
}

.input-blur {
    filter: blur(3px);
    pointer-events: none;
}

.lock-overlay {
    position: absolute;
    inset: 0;
    background: rgba(255, 255, 255, 0.92);
    z-index: 10;
    backdrop-filter: blur(4px);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
}

.lock-title {
    font-size: 0.95rem;
    font-weight: 700;
    color: #1e293b;
}

.lock-sub {
    font-size: 0.78rem;
    color: #94a3b8;
}

/* ========== EMPTY STATE ========== */
.empty-state {
    background: $blue-bg;
}

.empty-icon-wrap {
    width: 100px;
    height: 100px;
    border-radius: 50%;
    background: $blue-light;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 20px;
}

.empty-title {
    font-size: 1.3rem;
    font-weight: 800;
    color: #1e293b;
}

.empty-sub {
    font-size: 0.9rem;
    color: #94a3b8;
    margin-top: 4px;
}
</style>
