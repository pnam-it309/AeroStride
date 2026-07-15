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
const typingTimeout = ref(null);

// Hỗ trợ câu hỏi gợi ý và gặp nhân viên
const showSuggestions = ref(false);
const DEFAULT_SUGGESTIONS = [
    'Làm thế nào để đặt hàng?',
    'Phí vận chuyển là bao nhiêu?',
    'Kiểm tra trạng thái đơn hàng',
    'Có voucher giảm giá không?',
    'Sản phẩm có bảo hành không?',
    'Hướng dẫn thanh toán online',
    'Liên hệ nhân viên hỗ trợ'
];
const welcomeSuggestions = ref([...DEFAULT_SUGGESTIONS]);
const suggestions = ref([...DEFAULT_SUGGESTIONS]);

const fetchWelcomeSuggestions = async () => {
    try {
        const response = await apiService.get(`${API_CHAT.CUSTOMER_BASE}/welcome-suggestions?sessionId=${sessionId.value}`);
        if (response.data?.success && response.data.data && response.data.data.length) {
            welcomeSuggestions.value = response.data.data;
            updateActiveSuggestions();
        }
    } catch (error) {
        console.error('Lỗi khi tải gợi ý chào mừng từ AI:', error);
    }
};

const sendSuggestion = (text) => {
    message.value = text;
    sendMessage();
    showSuggestions.value = false;
};

// Cập nhật câu hỏi gợi ý dựa trên tin nhắn mới nhất
const updateActiveSuggestions = () => {
    for (let i = chatHistory.value.length - 1; i >= 0; i--) {
        const msg = chatHistory.value[i];
        if (msg.sender !== 'user' && msg.suggestions && msg.suggestions.length) {
            suggestions.value = [...msg.suggestions];
            return;
        }
    }
    suggestions.value = [...welcomeSuggestions.value];
};

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
        fetchWelcomeSuggestions();
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

const isSending = ref(false);
const lastSendTime = ref(0);
const COOLDOWN_MS = 3000;
const typingTimeout = ref(null);

const imageFile = ref(null);
const imagePreview = ref(null);
const fileInput = ref(null);

const triggerImageUpload = () => {
    if (fileInput.value) {
        fileInput.value.click();
    }
};

const handleImageUpload = (event) => {
    const file = event.target.files[0];
    if (file) {
        if (!file.type.startsWith('image/')) {
            alert('Vui lòng chọn file hình ảnh.');
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            alert('Kích thước ảnh không được vượt quá 5MB.');
            return;
        }
        imageFile.value = file;
        const reader = new FileReader();
        reader.onload = (e) => {
            imagePreview.value = e.target.result;
        };
        reader.readAsDataURL(file);
    }
};

const clearImage = () => {
    imageFile.value = null;
    imagePreview.value = null;
    if (fileInput.value) {
        fileInput.value.value = '';
    }
};

const sendMessage = () => {
    if ((!message.value.trim() && !imagePreview.value) || isSending.value) return;

    const now = Date.now();
    if (now - lastSendTime.value < COOLDOWN_MS) return;

    let userMsg = message.value;
    const lowerMsg = userMsg.toLowerCase().trim();

    // Kiểm tra nếu tin nhắn là yêu cầu kết nối nhân viên
    const isHandoff =
        lowerMsg.includes('nhân viên') ||
        lowerMsg.includes('nhan vien') ||
        lowerMsg.includes('người thật') ||
        lowerMsg.includes('nguoi that') ||
        lowerMsg.includes('admin') ||
        lowerMsg.includes('gặp hỗ trợ') ||
        lowerMsg.includes('gap ho tro') ||
        lowerMsg.includes('gọi hỗ trợ') ||
        lowerMsg.includes('goi ho tro') ||
        lowerMsg.includes('liên hệ hỗ trợ') ||
        lowerMsg.includes('lien he ho tro') ||
        lowerMsg.includes('kết nối hỗ trợ') ||
        lowerMsg.includes('ket noi ho tro') ||
        lowerMsg.includes('nói chuyện với hỗ trợ') ||
        lowerMsg.includes('noi chuyen voi ho tro');

    if (isHandoff) {
        userMsg = 'Tôi muốn nói chuyện với nhân viên hỗ trợ.';
    }

    message.value = '';
    isSending.value = true;
    lastSendTime.value = now;

    // Tạm thời hiển thị tin nhắn user để UI phản hồi nhanh
    const tempId = Date.now();
    
    // Copy ảnh base64 để render ngay lập tức (nếu có)
    const currentImagePreview = imagePreview.value;
    
    chatHistory.value.push({
        id: tempId,
        sender: 'user',
        text: userMsg,
        image: currentImagePreview,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    });

    isTyping.value = true;

    // Tự động tắt typing indicator sau 45s nếu không có phản hồi từ WebSocket
    if (typingTimeout.value) clearTimeout(typingTimeout.value);
    typingTimeout.value = setTimeout(() => {
        if (isTyping.value) {
            isTyping.value = false;
            chatHistory.value.push({
                id: Date.now(),
                sender: 'system',
                text: 'Hệ thống đang bận. Bạn vui lòng chờ thêm giây lát hoặc nhắn "Gặp nhân viên" để được hỗ trợ trực tiếp nhé!'
            });
            scrollToBottom();
        }
    }, 45000);

    scrollToBottom();
    updateActivity();
    
    // Tạo data base64 thuần túy (bỏ header "data:image/jpeg;base64,") nếu cần
    let base64Image = null;
    if (currentImagePreview) {
        base64Image = currentImagePreview.split(',')[1];
    }
    
    // Clear image sau khi gửi
    clearImage();

    apiService
        .post(API_CHAT.CUSTOMER_SEND, {
            sessionId: sessionId.value,
            sender: CHAT_SENDER_TYPE.CUSTOMER,
            text: userMsg,
            image: base64Image
        })
        .then(() => {
            isSending.value = false;
        })
        .catch((err) => {
            console.error('Lỗi gửi tin nhắn:', err);
            isTyping.value = false;
            isSending.value = false;
            if (typingTimeout.value) clearTimeout(typingTimeout.value);
        });
};

const parseProductJson = (text) => {
    if (!text) return null;
    const match = text.match(/\[\[PRODUCT_JSON:([\s\S]*?)\]\]/);
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

const parseSuggestionsJson = (text) => {
    if (!text) return null;
    const match = text.match(/\[\[SUGGESTIONS:([\s\S]*?)\]\]/);
    if (!match) return null;
    let jsonStr = match[1].trim();

    // Self-healing: marker kết thúc bằng ]]] nên regex non-greedy cắt mất dấu ] đóng mảng.
    // Bù lại dấu ] để JSON.parse không lỗi (giống parseProductJson).
    if (jsonStr.startsWith('[') && !jsonStr.endsWith(']')) {
        jsonStr += ']';
    }

    try {
        return JSON.parse(jsonStr);
    } catch (e) {
        console.warn('Không thể parse gợi ý từ AI:', jsonStr, e);
        return null;
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
                        parsed.text = msg.text.replace(/\[\[PRODUCT_JSON:[\s\S]*?\]\]/, '');
                    }
                }

                // Thử parse gợi ý từ AI — luôn xóa marker khỏi text hiển thị (kể cả khi parse lỗi)
                if (msg.text && msg.text.includes('[[SUGGESTIONS:')) {
                    const suggs = parseSuggestionsJson(msg.text);
                    if (suggs) {
                        parsed.suggestions = suggs;
                    }
                    const currentText = parsed.text || msg.text;
                    parsed.text = currentText.replace(/\[\[SUGGESTIONS:[\s\S]*?\]\]\]?/, '').trim();
                }
                return parsed;
            });

            if (history.length === 0) {
                chatHistory.value = [{ id: 'welcome', sender: 'bot', text: 'Xin chào! AeroStride có thể giúp gì cho bạn?', time: '10:00' }];
            } else {
                chatHistory.value = history;
            }
            updateActiveSuggestions();
            scrollToBottom();
        }
    } catch (error) {
        console.error('Lỗi khi tải lịch sử chat:', error);
    }
};

onMounted(() => {
    fetchHistory();
    fetchWelcomeSuggestions();

    chatSocket.connect(() => {
        chatSocket.subscribe('/topic/messages', (msg) => {
            const data = typeof msg === 'string' ? JSON.parse(msg) : msg;
            if (data.secondStaffId || !data.sessionId || data.sessionId !== sessionId.value) return;

            // Dừng indicator khi nhận được tin nhắn mới từ bot/staff
            if (data.sender !== CHAT_SENDER_TYPE.CUSTOMER) {
                isTyping.value = false;
                if (typingTimeout.value) {
                    clearTimeout(typingTimeout.value);
                    typingTimeout.value = null;
                }
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
                    parsed.text = data.text.replace(/\[\[PRODUCT_JSON:[\s\S]*?\]\]/, '');
                }
            }

            // Parse gợi ý từ AI cho tin nhắn mới — luôn xóa marker khỏi text hiển thị
            if (data.text && data.text.includes('[[SUGGESTIONS:')) {
                const suggs = parseSuggestionsJson(data.text);
                if (suggs) {
                    parsed.suggestions = suggs;
                }
                const currentText = parsed.text || data.text;
                parsed.text = currentText.replace(/\[\[SUGGESTIONS:[\s\S]*?\]\]\]?/, '').trim();
            }

            // Hiển thị form đánh giá nếu cuộc trò chuyện bị đóng
            if (data.sender === 'system' && data.text && data.text.includes('Cuộc trò chuyện đã được đóng')) {
                showRatingForm.value = true;
                ratingConversationId.value = data.idCuocHoiThoai;
            }

            // Xóa tin nhắn tạm thời nếu trùng nội dung (giảm giật lag)
            chatHistory.value = chatHistory.value.filter((m) => m.id > 2000000000000 || m.text !== data.text || m.sender !== 'user');

            chatHistory.value.push(parsed);
            updateActiveSuggestions();
            scrollToBottom();
        });
    });
});

const goToDetail = (id) => {
    isOpen.value = false;
    router.push(`/product/${id}`);
};

// --- Rating Logic ---
const showRatingForm = ref(false);
const ratingConversationId = ref(null);
const ratingScore = ref(5);
const ratingComment = ref('');
const isSubmittingRating = ref(false);

const submitRating = async () => {
    if (!ratingConversationId.value) return;
    
    isSubmittingRating.value = true;
    try {
        await apiService.post(`${API_CHAT.CUSTOMER_BASE}/rating`, {
            conversationId: ratingConversationId.value,
            rating: ratingScore.value,
            feedback: ratingComment.value
        });
        showRatingForm.value = false;
        chatHistory.value.push({
            id: Date.now(),
            sender: 'system',
            text: 'Cảm ơn bạn đã đánh giá cuộc trò chuyện này!'
        });
        scrollToBottom();
    } catch (error) {
        console.error('Lỗi khi gửi đánh giá:', error);
    } finally {
        isSubmittingRating.value = false;
    }
};

const skipRating = () => {
    showRatingForm.value = false;
};
</script>

<template>
    <div class="customer-chat-container">
        <!-- Rating Modal -->
        <v-dialog v-model="showRatingForm" max-width="400" persistent>
            <v-card class="rating-card">
                <v-card-title class="text-center pb-0 mt-3">Đánh giá hỗ trợ</v-card-title>
                <v-card-text class="text-center pt-2">
                    <p class="text-body-2 text-grey-darken-1 mb-4">Bạn cảm thấy cuộc trò chuyện vừa rồi như thế nào?</p>
                    <v-rating
                        v-model="ratingScore"
                        color="amber"
                        active-color="amber"
                        hover
                        size="x-large"
                    ></v-rating>
                    
                    <v-textarea
                        v-model="ratingComment"
                        placeholder="Nhập góp ý của bạn (không bắt buộc)..."
                        variant="outlined"
                        auto-grow
                        rows="2"
                        class="mt-4"
                        density="compact"
                    ></v-textarea>
                </v-card-text>
                <v-card-actions class="justify-center pb-4 px-4">
                    <v-btn variant="text" color="grey-darken-1" @click="skipRating">Bỏ qua</v-btn>
                    <v-btn color="black" variant="flat" :loading="isSubmittingRating" @click="submitRating" class="px-6">Gửi đánh giá</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
        <!-- Floating Chat Icon -->
        <div v-if="!isOpen" class="chat-fab-container" @click="isOpen = true">
            <div class="chat-badge" v-if="false">1</div>
            <v-btn icon="mdi-chat-processing" color="black" size="large" elevation="8" class="chat-fab"></v-btn>
            <div class="chat-tooltip">Chat với chúng tôi!</div>
        </div>

        <!-- Chat Window -->
        <transition name="chat-slide">
            <div v-if="isOpen" class="chat-window shadow-xl">
                <!-- Header -->
                <div class="chat-header">
                    <div class="header-content">
                        <v-avatar size="34" class="header-avatar shadow-sm">
                            <img
                                src="@/assets/images/logos/logoclient.jpg"
                                alt="AeroStride"
                                style="width: 100%; height: 100%; object-fit: cover"
                            />
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
                                <div v-if="msg.sender === 'staff' && msg.idNhanVien" class="staff-name-label">
                                    {{ msg.idNhanVien }}
                                </div>
                                <div v-if="msg.image" class="message-image">
                                    <img :src="msg.image" alt="Uploaded Image" style="max-width: 100%; border-radius: 8px; margin-bottom: 8px;" />
                                </div>
                                <div v-if="msg.text" class="message-bubble" v-html="marked(msg.text)"></div>

                                <!-- Product Showcase in Chat -->
                                <div v-if="msg.products && msg.products.length" class="product-showcase-list mt-3">
                                    <ProductShowcaseCard v-for="p in msg.products" :key="p.id" :product="p" @view-detail="goToDetail" />
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
                <div class="chat-footer" style="position: relative">
                    <!-- Suggestions Panel -->
                    <transition name="chat-slide">
                        <div v-if="showSuggestions" class="suggestions-panel">
                            <!-- Handoff Button -->
                            <button class="handoff-btn" @click="sendSuggestion('Tôi muốn nói chuyện với nhân viên hỗ trợ.')">
                                <v-icon icon="mdi-account-tie" size="small" class="mr-2"></v-icon>
                                Gặp nhân viên hỗ trợ
                            </button>

                            <div class="suggestions-title">
                                <v-icon icon="mdi-lightbulb-on" color="amber-darken-2" size="small" class="mr-1"></v-icon>
                                Câu hỏi gợi ý:
                            </div>

                            <div class="suggestions-list">
                                <button v-for="s in suggestions" :key="s" class="suggestion-pill" @click="sendSuggestion(s)">
                                    {{ s }}
                                </button>
                                <button class="suggestion-pill collapse-pill" @click="showSuggestions = false">Thu gọn ↑</button>
                            </div>
                        </div>
                    </transition>

                    <div class="input-container-wrapper">
                        <!-- Image Preview Area -->
                        <div v-if="imagePreview" class="image-preview-container">
                            <img :src="imagePreview" alt="Preview" class="image-preview" />
                            <v-btn icon="mdi-close" size="x-small" color="red" class="remove-image-btn" @click="clearImage"></v-btn>
                        </div>
                        <div class="input-container">
                            <!-- Yellow lightbulb icon toggle button -->
                            <v-btn
                                icon="mdi-lightbulb"
                                variant="text"
                                size="small"
                                :color="showSuggestions ? 'amber-darken-2' : 'grey-darken-1'"
                                class="mr-2"
                                @click="showSuggestions = !showSuggestions"
                            ></v-btn>

                            <!-- Image Upload Button -->
                            <v-btn
                                icon="mdi-image-outline"
                                variant="text"
                                size="small"
                                color="grey-darken-1"
                                class="mr-2"
                                @click="triggerImageUpload"
                            ></v-btn>
                            <input
                                type="file"
                                ref="fileInput"
                                accept="image/*"
                                style="display: none"
                                @change="handleImageUpload"
                            />

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
                                :disabled="(!message.trim() && !imagePreview) || isSending"
                                :color="(message.trim() || imagePreview) && !isSending ? 'black' : 'grey-lighten-1'"
                                @click="sendMessage"
                            ></v-btn>
                        </div>
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
    border: 1px solid rgba(0, 0, 0, 0.05);
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
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
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
    word-break: break-word;
    overflow-wrap: break-word;

    :deep(p) {
        margin-bottom: 8px;
        &:last-child {
            margin-bottom: 0;
        }
    }
    :deep(ul),
    :deep(ol) {
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
    flex-direction: column;
    gap: 16px;
    padding-bottom: 10px;

    :deep(.product-showcase-card) {
        width: 100%;
        max-width: 100%;
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

    .input-container-wrapper {
        background: #f8f9fa;
        border-radius: 16px;
        border: 1px solid #eee;
        transition: all 0.2s ease;
        padding: 5px;

        &:focus-within {
            border-color: #000;
            background: #fff;
            box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.03);
        }
    }

    .image-preview-container {
        position: relative;
        padding: 8px;
        border-bottom: 1px solid #eee;
        margin-bottom: 4px;

        .image-preview {
            max-height: 100px;
            border-radius: 8px;
            display: block;
        }

        .remove-image-btn {
            position: absolute;
            top: 0;
            left: 0;
            background: rgba(255, 255, 255, 0.9);
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
    }

    .input-container {
        display: flex;
        align-items: center;
        background: transparent;
        border-radius: 0;
        padding: 0 5px 0 10px;
        border: none;

        &:focus-within {
            border-color: transparent;
            background: transparent;
            box-shadow: none;
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
            &::placeholder {
                color: #b2bec3;
            }
        }
    }
    
    .staff-name-label {
        font-size: 0.7rem;
        color: #747d8c;
        margin-bottom: 4px;
        margin-left: 8px;
        font-weight: 500;
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
        &:nth-child(2) {
            animation-delay: 0.2s;
        }
        &:nth-child(3) {
            animation-delay: 0.4s;
        }
    }
}

@keyframes typing {
    0%,
    100% {
        transform: translateY(0);
        opacity: 0.4;
    }
    50% {
        transform: translateY(-4px);
        opacity: 1;
    }
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

/* Suggestions Panel Styles */
.suggestions-panel {
    position: absolute;
    bottom: 84px;
    left: 20px;
    right: 20px;
    background: #fff;
    border-radius: 16px;
    box-shadow:
        0 -8px 24px rgba(0, 0, 0, 0.08),
        0 8px 24px rgba(0, 0, 0, 0.08);
    border: 1px solid rgba(0, 0, 0, 0.05);
    padding: 16px;
    z-index: 10;
}

.handoff-btn {
    width: 100%;
    background: #f8f9fa;
    color: #2d3436;
    border: 1px solid #dfe6e9;
    padding: 10px;
    border-radius: 12px;
    font-size: 0.88rem;
    font-weight: 600;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 12px;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
        background: #000;
        color: #fff;
        border-color: #000;
        transform: translateY(-1px);
    }

    &:active {
        transform: translateY(0);
    }
}

.suggestions-title {
    font-size: 0.8rem;
    color: #636e72;
    font-weight: 600;
    margin-bottom: 8px;
    display: flex;
    align-items: center;
}

.suggestions-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    max-height: 200px;
    overflow-y: auto;
    padding-right: 4px;

    &::-webkit-scrollbar {
        width: 4px;
    }
    &::-webkit-scrollbar-thumb {
        background: #e0e0e0;
        border-radius: 4px;
    }
}

.suggestion-pill {
    background: #fff;
    color: #e84393;
    border: 1px solid #ff7875;
    padding: 6px 14px;
    border-radius: 20px;
    font-size: 0.8rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
        background: #fff0f6;
        color: #c41d7f;
        border-color: #ff4d4f;
        transform: scale(1.02);
    }

    &.collapse-pill {
        border-style: dashed;
        background: transparent;
        color: #999;
        border-color: #ccc;

        &:hover {
            background: #fafafa;
            color: #333;
            border-color: #888;
        }
    }
}
</style>
