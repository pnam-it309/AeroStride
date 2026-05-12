<script setup>
import { ref, onMounted, nextTick } from 'vue';
import { chatSocket } from '@/services/chatSocket';
import apiService from '@/services/apiService';
import { API_CHAT } from '@/constants/apiPaths';

const isOpen = ref(false);
const message = ref('');
const chatHistory = ref([
    { id: 1, sender: 'bot', text: 'Xin chào! AeroStride có thể giúp gì cho bạn?', time: '10:00' },
]);
const chatBody = ref(null);
const sessionId = ref(localStorage.getItem('chat_session_id') || `guest_${Math.random().toString(36).substr(2, 9)}`);

// Lưu sessionId để duy trì phiên chat khi reload trang
if (!localStorage.getItem('chat_session_id')) {
    localStorage.setItem('chat_session_id', sessionId.value);
}

const scrollToBottom = async () => {
    await nextTick();
    if (chatBody.value) {
        chatBody.value.scrollTop = chatBody.value.scrollHeight;
    }
};

const sendMessage = () => {
    if (!message.value.trim()) return;
    
    const userMsg = message.value;
    message.value = '';
    scrollToBottom();

    // Send via API to ensure persistence
    apiService.post(API_CHAT.CUSTOMER_SEND, {
        sessionId: sessionId.value,
        sender: 'customer',
        text: userMsg
    }).catch(err => {
        console.error('Lỗi gửi tin nhắn:', err);
    });
};

const fetchHistory = async () => {
    try {
        const response = await apiService.get(`${API_CHAT.CUSTOMER_BASE}/history?sessionId=${sessionId.value}`);
        if (response.data?.success) {
            const history = response.data.data.map(msg => ({
                ...msg,
                // Map 'customer' from server to 'user' for local UI alignment
                sender: msg.sender === 'customer' ? 'user' : msg.sender
            }));
            
            // Add welcome message if history is empty
            if (history.length === 0) {
                chatHistory.value = [
                    { id: 'welcome', sender: 'bot', text: 'Xin chào! AeroStride có thể giúp gì cho bạn?', time: '10:00' }
                ];
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
            
            // Avoid duplicates
            if (chatHistory.value.find(existing => existing.id === data.id)) return;

            // Only add if it's from staff or system, or if it's our own message from another tab
            if (data.sender === 'staff' || data.sender === 'system' || (data.sender === 'customer' && !chatHistory.value.find(m => m.text === data.text))) {
                chatHistory.value.push({
                    ...data,
                    sender: data.sender === 'customer' ? 'user' : data.sender
                });
                scrollToBottom();
            }
        });
    });
});
</script>

<template>
    <div class="customer-chat-container">
        <!-- Floating Chat Icon -->
        <v-btn
            v-if="!isOpen"
            icon="mdi-message-text"
            color="black"
            size="default"
            elevation="4"
            class="chat-fab"
            @click="isOpen = true"
        ></v-btn>

        <!-- Chat Window -->
        <transition name="chat-slide">
            <div v-if="isOpen" class="chat-window shadow-lg">
                <div class="chat-header d-flex align-center justify-space-between px-4 py-3 bg-black text-white">
                    <div class="d-flex align-center">
                        <v-avatar size="32" class="mr-3 bg-white">
                            <img src="@/assets/images/logos/logo.jpg" alt="logo" />
                        </v-avatar>
                        <div>
                            <div class="text-subtitle-2 font-weight-bold">AeroStride Support</div>
                            <div class="text-caption opacity-70">Online</div>
                        </div>
                    </div>
                    <v-btn icon="mdi-close" variant="text" size="small" @click="isOpen = false"></v-btn>
                </div>

                <div ref="chatBody" class="chat-body pa-4">
                    <div v-for="msg in chatHistory" :key="msg.id" 
                         class="message-wrap mb-4" 
                         :class="{ 'user': msg.sender === 'user', 'is-system': msg.sender === 'system' }">
                        <template v-if="msg.sender === 'system'">
                            <div class="system-msg">{{ msg.text }}</div>
                        </template>
                        <template v-else>
                            <div class="message-bubble">
                                {{ msg.text }}
                            </div>
                            <div class="message-time">{{ msg.time }}</div>
                        </template>
                    </div>
                </div>

                <div class="chat-footer pa-3 border-t">
                    <v-text-field
                        v-model="message"
                        placeholder="Nhập tin nhắn..."
                        density="compact"
                        variant="solo"
                        flat
                        hide-details
                        @keyup.enter="sendMessage"
                    >
                        <template v-slot:append-inner>
                            <v-btn icon="mdi-send" variant="text" size="small" color="blue-darken-3" @click="sendMessage"></v-btn>
                        </template>
                    </v-text-field>
                </div>
            </div>
        </transition>
    </div>
</template>

<style scoped lang="scss">
.chat-fab {
    position: fixed; bottom: 30px; right: 30px; z-index: 5000;
    transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    &:hover { transform: scale(1.1) rotate(5deg); }
}

.chat-window {
    position: fixed; bottom: 100px; right: 30px; z-index: 5000;
    width: 350px; height: 500px; background: #fff; border-radius: 20px;
    display: flex; flex-direction: column; overflow: hidden;
    box-shadow: 0 20px 50px rgba(0,0,0,0.15);
}

.chat-body {
    flex: 1; overflow-y: auto; background: #f8f9fa;
    display: flex; flex-direction: column;
}

.message-wrap {
    max-width: 80%; align-self: flex-start;
    &.user { align-self: flex-end; .message-bubble { background: #000; color: #fff; border-radius: 15px 15px 0 15px; } .message-time { text-align: right; } }
}

.message-bubble {
    padding: 10px 15px; background: #fff; border-radius: 15px 15px 15px 0;
    font-size: 0.9rem; font-weight: 500; color: #333; line-height: 1.4;
    box-shadow: 0 2px 5px rgba(0,0,0,0.05);
}

.message-time { font-size: 0.65rem; color: #999; margin-top: 4px; padding: 0 4px; }

.is-system {
    max-width: 100%; align-self: center; width: 100%; margin: 12px 0;
    .system-msg {
        background: #eceff1; color: #546e7a; font-size: 0.7rem; font-weight: 800;
        padding: 4px 14px; border-radius: 100px; text-align: center;
        display: table; margin: 0 auto; border: 1px solid #cfd8dc;
    }
}

/* Transitions */
.chat-slide-enter-active, .chat-slide-leave-active { transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.chat-slide-enter-from, .chat-slide-leave-to { opacity: 0; transform: translateY(20px) scale(0.9); transform-origin: bottom right; }
</style>
