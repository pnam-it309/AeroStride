<script setup>
import { ref, onMounted, nextTick, watch } from 'vue';
import { chatSocket } from '@/services/chatSocket';
import apiService from '@/services/apiService';
import { API_CHAT } from '@/constants/apiPaths';
import { CHAT_SENDER_TYPE } from '@/constants/appConstants';
import { useAuthStore } from '@/stores/authStore';
import { marked } from 'marked';
import ProductShowcaseCard from './ProductShowcaseCard.vue';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();
const isOpen = ref(false);
const message = ref('');
const chatHistory = ref([{ id: 1, sender: 'bot', text: 'Xin chào! AeroStride có thể giúp gì cho bạn?', time: '10:00' }]);
const chatBody = ref(null);
const isTyping = ref(false);
const isSending = ref(false);
const lastSendTime = ref(0);
const COOLDOWN_MS = 3000;

// Cấu hình marked để an toàn hơn
marked.setOptions({
    breaks: true,
    gfm: true
});

// Logic xác định sessionId
const getSessionId = () => {
    if (authStore.isLoggedIn && authStore.user?.username) {
        return `user_${authStore.user.username}`;
    }

    const now = Date.now();
    const lastActivity = localStorage.getItem('chat_last_activity');
    const savedSessionId = localStorage.getItem('chat_session_id');

    if (lastActivity && now - parseInt(lastActivity) > 30 * 60 * 1000) {
        localStorage.removeItem('chat_session_id');
        const newId = `guest_${Math.random().toString(36).substr(2, 9)}`;
        localStorage.setItem('chat_session_id', newId);
        localStorage.setItem('chat_last_activity', now.toString());
        return newId;
    }

    if (savedSessionId) {
        localStorage.setItem('chat_last_activity', now.toString());
        return savedSessionId;
    }

    const newId = `guest_${Math.random().toString(36).substr(2, 9)}`;
    localStorage.setItem('chat_session_id', newId);
    localStorage.setItem('chat_last_activity', now.toString());
    return newId;
};

const sessionId = ref(getSessionId());

const updateActivity = () => {
    if (!authStore.isLoggedIn) {
        localStorage.setItem('chat_last_activity', Date.now().toString());
    }
};

watch(
    () => authStore.isLoggedIn,
    () => {
        chatHistory.value = [];
        sessionId.value = getSessionId();
        fetchHistory();
    }
);

watch(isOpen, (newVal) => {
    if (newVal) {
        updateActivity();
        scrollToBottom();
    }
});

const scrollToBottom = async () => {
    await nextTick();
    if (chatBody.value) {
        chatBody.value.scrollTo({
            top: chatBody.value.scrollHeight,
            behavior: 'smooth'
        });
    }
};

const sendMessage = () => {
    if (!message.value.trim() || isSending.value) return;

    const now = Date.now();
    if (now - lastSendTime.value < COOLDOWN_MS) return;

    const userMsg = message.value;
    message.value = '';
    isSending.value = true;
    lastSendTime.value = now;
    
    // Tạm thời hiển thị tin nhắn user để UI phản hồi nhanh
    const tempId = Date.now();
    chatHistory.value.push({
        id: tempId,
        sender: 'user',
        text: userMsg,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    });

    isTyping.value = true;
    scrollToBottom();
    updateActivity();

    apiService
        .post(API_CHAT.CUSTOMER_SEND, {
            sessionId: sessionId.value,
            sender: CHAT_SENDER_TYPE.CUSTOMER,
            text: userMsg
        })
        .then(() => {
            isSending.value = false;
        })
        .catch((err) => {
            console.error('Lỗi gửi tin nhắn:', err);
            isTyping.value = false;
            isSending.value = false;
        });
};

const parseProductJson = (text) => {
    if (!text) return null;
    const match = text.match(/\[\[PRODUCT_JSON:(.*?)\]\]/);
    if (!match) return null;
    let jsonStr = match[1].trim();
    
    // Tự động sửa lỗi (Self-healing): Nếu chuỗi bắt đầu bằng '[' nhưng kết thúc bằng '}' (thiếu ngoặc đóng mảng do lỗi Regex cũ)
    if (jsonStr.startsWith('[') && !jsonStr.endsWith(']')) {
        jsonStr += ']';
    }
    
    // Xử lý loại bỏ ngoặc đóng thừa (nếu có 3 ngoặc ở cuối từ phiên bản cũ)
    if (jsonStr.endsWith(']')) {
        const openCount = (jsonStr.match(/\[/g) || []).length;
        const closeCount = (jsonStr.match(/\]/g) || []).length;
        if (closeCount > openCount) {
            jsonStr = jsonStr.substring(0, jsonStr.length - (closeCount - openCount));
        }
    }
    
    try {
        return JSON.parse(jsonStr);
    } catch (e) {
        console.warn('Không thể parse JSON sản phẩm (chuỗi gốc bị lỗi hoặc định dạng cũ):', jsonStr, e);
        return []; // Trả về mảng rỗng để bảo vệ giao diện
    }
};

const fetchHistory = async () => {
    try {
        const response = await apiService.get(`${API_CHAT.CUSTOMER_BASE}/history?sessionId=${sessionId.value}`);
        if (response.data?.success) {
            const history = response.data.data.map((msg) => {
                const parsed = {
                    ...msg,
                    sender: msg.sender === 'customer' ? 'user' : msg.sender
                };
                
                // Thử parse data nếu có JSON sản phẩm
                if (msg.text && msg.text.includes('[[PRODUCT_JSON:')) {
                    const products = parseProductJson(msg.text);
                    if (products) {
                        parsed.products = products;
                        parsed.text = msg.text.replace(/\[\[PRODUCT_JSON:.*?\]\]/, '');
                    }
                }
                return parsed;
            });

            if (history.length === 0) {
                chatHistory.value = [{ id: 'welcome', sender: 'bot', text: 'Xin chào! AeroStride có thể giúp gì cho bạn?', time: '10:00' }];
            } else {
                chatHistory.value = history;
            }
            scrollToBottom();
        }
    } catch (error) {
        console.error('Lỗi khi tải lịch sử chat:', error);
    }
};

onMounted(() => {
    fetchHistory();

    chatSocket.connect(() => {
        chatSocket.subscribe('/topic/messages', (msg) => {
            const data = typeof msg === 'string' ? JSON.parse(msg) : msg;
            if (data.secondStaffId || !data.sessionId || data.sessionId !== sessionId.value) return;

            // Dừng indicator khi nhận được tin nhắn mới từ bot/staff
            if (data.sender !== CHAT_SENDER_TYPE.CUSTOMER) {
                isTyping.value = false;
            }

            if (chatHistory.value.find((existing) => existing.id === data.id)) return;

            const parsed = {
                ...data,
                sender: data.sender === CHAT_SENDER_TYPE.CUSTOMER ? 'user' : data.sender
            };

            // Parse JSON sản phẩm cho tin nhắn mới
            if (data.text && data.text.includes('[[PRODUCT_JSON:')) {
                const products = parseProductJson(data.text);
                if (products) {
                    parsed.products = products;
                    parsed.text = data.text.replace(/\[\[PRODUCT_JSON:.*?\]\]/, '');
                }
            }

            // Xóa tin nhắn tạm thời nếu trùng nội dung (giảm giật lag)
            chatHistory.value = chatHistory.value.filter(m => m.id > 2000000000000 || m.text !== data.text || m.sender !== 'user');

            chatHistory.value.push(parsed);
            scrollToBottom();
        });
    });
});

const goToDetail = (id) => {
    isOpen.value = false;
    router.push(`/product/${id}`);
};
</script>

<template>
    <div class="customer-chat-container">
        <!-- Floating Chat Icon -->
        <div v-if="!isOpen" class="chat-fab-container" @click="isOpen = true">
            <div class="chat-badge" v-if="false">1</div>
            <v-btn
                icon="mdi-chat-processing"
                color="black"
                size="large"
                elevation="8"
                class="chat-fab"
            ></v-btn>
            <div class="chat-tooltip">Chat với chúng tôi!</div>
        </div>

        <!-- Chat Window -->
        <transition name="chat-slide">
            <div v-if="isOpen" class="chat-window shadow-xl">
                <!-- Header -->
                <div class="chat-header">
                    <div class="header-content">
                        <v-avatar size="34" class="header-avatar shadow-sm">
                            <img src="@/assets/images/logos/logoclient.jpg" alt="AeroStride" style="width: 100%; height: 100%; object-fit: cover;" />
                        </v-avatar>
                        <div class="header-info">
                            <div class="store-name">AeroStride Support</div>
                            <div class="store-status">
                                <span class="status-dot"></span>
                                Trực tuyến
                            </div>
                        </div>
                    </div>
                    <v-btn icon="mdi-minus" variant="text" size="small" color="white" @click="isOpen = false"></v-btn>
                </div>

                <!-- Body -->
                <div ref="chatBody" class="chat-body">
                    <div class="welcome-banner">
                        <v-icon icon="mdi-robot-happy" color="grey-darken-1" size="large" class="mb-2"></v-icon>
                        <div class="text-caption text-grey-darken-1">Chào mừng bạn đến với AeroStride! AI sẽ hỗ trợ bạn ngay lập tức.</div>
                    </div>

                    <div
                        v-for="msg in chatHistory"
                        :key="msg.id"
                        class="message-group"
                        :class="{ 'is-user': msg.sender === 'user', 'is-system': msg.sender === 'system' }"
                    >
                        <!-- System Message -->
                        <template v-if="msg.sender === 'system'">
                            <div class="system-msg-wrap">
                                <span class="system-msg">{{ msg.text }}</span>
                            </div>
                        </template>

                        <!-- Bot/Staff/User Message -->
                        <template v-else>
                            <v-avatar v-if="msg.sender !== 'user'" size="28" class="msg-avatar">
                                <v-icon v-if="msg.sender === 'bot'" icon="mdi-robot" size="small" color="black"></v-icon>
                                <v-icon v-else icon="mdi-account-tie" size="small" color="primary"></v-icon>
                            </v-avatar>
                            
                            <div class="msg-content-wrap">
                                <div v-if="msg.text" class="message-bubble" v-html="marked(msg.text)"></div>
                                
                                <!-- Product Showcase in Chat -->
                                <div v-if="msg.products && msg.products.length" class="product-showcase-list mt-3">
                                    <ProductShowcaseCard 
                                        v-for="p in msg.products" 
                                        :key="p.id" 
                                        :product="p" 
                                        @view-detail="goToDetail"
                                    />
                                </div>
                                
                                <div class="message-time">{{ msg.time }}</div>
                            </div>
                        </template>
                    </div>

                    <!-- Typing Indicator -->
                    <div v-if="isTyping" class="message-group">
                        <v-avatar size="28" class="msg-avatar">
                            <v-icon icon="mdi-robot" size="small" color="black"></v-icon>
                        </v-avatar>
                        <div class="typing-indicator">
                            <span></span>
                            <span></span>
                            <span></span>
                        </div>
                    </div>
                </div>

                <!-- Footer -->
                <div class="chat-footer">
                    <div class="input-container">
                        <textarea
                            v-model="message"
                            placeholder="Nhập câu hỏi của bạn..."
                            rows="1"
                            @keydown.enter.prevent="sendMessage"
                            @input="updateActivity"
                        ></textarea>
                        <v-btn
                            icon="mdi-send"
                            variant="text"
                            :disabled="!message.trim() || isSending"
                            :color="message.trim() && !isSending ? 'black' : 'grey-lighten-1'"
                            @click="sendMessage"
                        ></v-btn>
                    </div>
                    <div class="footer-credit">Powered by AeroStride AI</div>
                </div>
            </div>
        </transition>
    </div>
</template>

<style scoped lang="scss">
.customer-chat-container {
    font-family: 'Inter', sans-serif;
}

/* Floating FAB */
.chat-fab-container {
    position: fixed;
    bottom: 30px;
    right: 30px;
    z-index: 5000;
    cursor: pointer;
    display: flex;
    flex-direction: column;
    align-items: flex-end;

    &:hover {
        .chat-fab {
            transform: scale(1.1);
        }
        .chat-tooltip {
            opacity: 1;
            transform: translateX(-10px);
        }
    }
}

.chat-fab {
    transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.chat-tooltip {
    position: absolute;
    right: 70px;
    top: 12px;
    background: #000;
    color: #fff;
    padding: 6px 14px;
    border-radius: 8px;
    font-size: 0.8rem;
    font-weight: 600;
    white-space: nowrap;
    opacity: 0;
    transform: translateX(0);
    transition: all 0.3s ease;
    pointer-events: none;
    &::after {
        content: '';
        position: absolute;
        right: -5px;
        top: 50%;
        transform: translateY(-50%);
        border-top: 5px solid transparent;
        border-bottom: 5px solid transparent;
        border-left: 5px solid #000;
    }
}

/* Chat Window */
.chat-window {
    position: fixed;
    bottom: 100px;
    right: 30px;
    z-index: 5000;
    width: 380px;
    height: 600px;
    max-height: calc(100vh - 120px);
    background: #fff;
    border-radius: 24px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    box-shadow: 0 24px 48px rgba(0, 0, 0, 0.16);
    border: 1px solid rgba(0,0,0,0.05);
}

/* Header */
.chat-header {
    background: #000;
    padding: 16px 20px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    color: #fff;

    .header-content {
        display: flex;
        align-items: center;
    }

    .header-avatar {
        border: 2px solid #fff;
        background: #fff;
        margin-right: 12px;
    }

    .store-name {
        font-weight: 700;
        font-size: 0.95rem;
        letter-spacing: -0.01em;
    }

    .store-status {
        font-size: 0.75rem;
        display: flex;
        align-items: center;
        opacity: 0.8;

        .status-dot {
            width: 8px;
            height: 8px;
            background: #4caf50;
            border-radius: 50%;
            margin-right: 6px;
            box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.2);
        }
    }
}

/* Body */
.chat-body {
    flex: 1;
    overflow-y: auto;
    background: #fdfdfd;
    padding: 20px;
    display: flex;
    flex-direction: column;

    &::-webkit-scrollbar {
        width: 6px;
    }
    &::-webkit-scrollbar-thumb {
        background: #eee;
        border-radius: 10px;
    }
}

.welcome-banner {
    text-align: center;
    padding: 20px 10px;
    margin-bottom: 20px;
    background: #f8f9fa;
    border-radius: 16px;
    border: 1px dashed #dee2e6;
}

.message-group {
    display: flex;
    margin-bottom: 16px;
    align-self: flex-start;
    max-width: 85%;

    .msg-avatar {
        margin-right: 8px;
        align-self: flex-end;
        background: #f0f0f0;
        border: 1px solid #eee;
    }

    &.is-user {
        align-self: flex-end;
        flex-direction: row-reverse;
        .msg-avatar {
            margin-right: 0;
            margin-left: 8px;
        }
        .message-bubble {
            background: #000;
            color: #fff;
            border-radius: 18px 18px 0 18px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.1);
        }
        .message-time {
            text-align: right;
        }
    }
}

.message-bubble {
    padding: 12px 16px;
    background: #fff;
    border-radius: 18px 18px 18px 0;
    font-size: 0.92rem;
    line-height: 1.5;
    color: #2d3436;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    border: 1px solid #f1f1f1;

    :deep(p) {
        margin-bottom: 8px;
        &:last-child { margin-bottom: 0; }
    }
    :deep(ul), :deep(ol) {
        padding-left: 20px;
        margin-bottom: 8px;
    }
}

.message-time {
    font-size: 0.65rem;
    color: #b2bec3;
    margin-top: 6px;
    font-weight: 500;
}

/* Product Showcase List */
.product-showcase-list {
    display: flex;
    gap: 12px;
    overflow-x: auto;
    padding-bottom: 10px;
    margin: 0 -10px;
    padding-left: 10px;
    &::-webkit-scrollbar { height: 4px; }
    &::-webkit-scrollbar-thumb { background: #eee; border-radius: 10px; }
    
    :deep(.product-showcase-card) {
        width: 240px;
        flex-shrink: 0;
    }
}

/* System Message */
.is-system {
    align-self: center;
    max-width: 90%;
    .system-msg-wrap {
        text-align: center;
        width: 100%;
    }
    .system-msg {
        background: #f1f2f6;
        color: #747d8c;
        font-size: 0.75rem;
        font-weight: 600;
        padding: 6px 16px;
        border-radius: 20px;
        display: inline-block;
    }
}

/* Footer */
.chat-footer {
    padding: 16px 20px;
    background: #fff;
    border-top: 1px solid #f1f1f1;

    .input-container {
        display: flex;
        align-items: center;
        background: #f8f9fa;
        border-radius: 16px;
        padding: 5px 5px 5px 15px;
        border: 1px solid #eee;
        transition: all 0.2s ease;

        &:focus-within {
            border-color: #000;
            background: #fff;
            box-shadow: 0 0 0 3px rgba(0,0,0,0.03);
        }

        textarea {
            flex: 1;
            border: none;
            background: transparent;
            padding: 10px 0;
            font-size: 0.9rem;
            resize: none;
            outline: none;
            max-height: 100px;
            color: #2d3436;
            &::placeholder { color: #b2bec3; }
        }
    }

    .footer-credit {
        font-size: 0.65rem;
        text-align: center;
        color: #dfe6e9;
        margin-top: 10px;
        font-weight: 600;
        letter-spacing: 0.05em;
        text-transform: uppercase;
    }
}

/* Typing Indicator */
.typing-indicator {
    background: #f1f2f6;
    padding: 12px 16px;
    border-radius: 18px 18px 18px 0;
    display: flex;
    align-items: center;
    gap: 4px;
    span {
        width: 6px;
        height: 6px;
        background: #95a5a6;
        border-radius: 50%;
        animation: typing 1s infinite ease-in-out;
        &:nth-child(2) { animation-delay: 0.2s; }
        &:nth-child(3) { animation-delay: 0.4s; }
    }
}

@keyframes typing {
    0%, 100% { transform: translateY(0); opacity: 0.4; }
    50% { transform: translateY(-4px); opacity: 1; }
}

/* Transitions */
.chat-slide-enter-active,
.chat-slide-leave-active {
    transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}
.chat-slide-enter-from,
.chat-slide-leave-to {
    opacity: 0;
    transform: translateY(40px) scale(0.95);
    filter: blur(10px);
}
</style>
