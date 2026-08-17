<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { chatSocket } from '@/services/chatSocket';
import apiService from '@/services/apiService';
import { API_CHAT } from '@/constants/apiPaths';
import { CHAT_SENDER_TYPE } from '@/constants/appConstants';
import { useAuthStore } from '@/stores/authStore';
import { validateChatMessage } from '@/utils/chatModeration';
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

// Inactivity timeout 2 phút (120s) cho khách chưa đăng nhập
const GUEST_TIMEOUT_SECONDS = 120;
let guestInactivityTimer = null;

// Kiểm duyệt ngôn từ & Khóa tạm thời nếu vi phạm
const violationCount = ref(0);
const isChatLocked = ref(false);
const lockCountdown = ref(0);
let lockTimer = null;

const startLockCooldown = () => {
    isChatLocked.value = true;
    lockCountdown.value = 30;
    if (lockTimer) clearInterval(lockTimer);
    lockTimer = setInterval(() => {
        if (lockCountdown.value > 1) {
            lockCountdown.value--;
        } else {
            clearInterval(lockTimer);
            lockTimer = null;
            isChatLocked.value = false;
            violationCount.value = 0;
        }
    }, 1000);
};

const clearGuestTimer = () => {
    if (guestInactivityTimer) {
        clearTimeout(guestInactivityTimer);
        guestInactivityTimer = null;
    }
};

const resetGuestInactivityTimer = () => {
    if (authStore.isLoggedIn) {
        clearGuestTimer();
        return;
    }

    clearGuestTimer();
    guestInactivityTimer = setTimeout(() => {
        handleGuestSessionExpired();
    }, GUEST_TIMEOUT_SECONDS * 1000);
};

const handleGuestSessionExpired = () => {
    clearGuestTimer();
    if (!authStore.isLoggedIn) {
        // Tự động đóng chat khi quá 2 phút không nhắn
        isOpen.value = false;

        // Reset session guest và dữ liệu lịch sử tạm thời
        localStorage.removeItem('chat_session_id');
        localStorage.removeItem('chat_last_activity');
        const newGuestId = `guest_${Math.random().toString(36).substr(2, 9)}`;
        localStorage.setItem('chat_session_id', newGuestId);
        sessionId.value = newGuestId;

        chatHistory.value = [
            {
                id: 'session-timeout',
                sender: 'bot',
                text: 'Phiên trò chuyện đã tự động kết thúc do không có hoạt động trong 2 phút.\n\nXin chào! Bạn có thể tiếp tục trò chuyện hoặc **Đăng nhập** để lưu trữ lịch sử tin nhắn lâu dài nhé!',
                time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
            }
        ];
    }
};

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
        resetGuestInactivityTimer();
    }
};

watch(
    () => authStore.isLoggedIn,
    () => {
        clearGuestTimer();
        chatHistory.value = [];
        sessionId.value = getSessionId();
        fetchHistory();
        fetchWelcomeSuggestions();
        if (!authStore.isLoggedIn && isOpen.value) {
            resetGuestInactivityTimer();
        }
    }
);

watch(isOpen, (newVal) => {
    if (newVal) {
        updateActivity();
        resetGuestInactivityTimer();
        scrollToBottom();
    } else {
        clearGuestTimer();
    }
});

// --- Drag & Drop FAB Button Logic ---
const BUTTON_SIZE = 58;
const MARGIN = 15;
const pos = ref({ x: 0, y: 0 });
const isDragging = ref(false);
const isInitialized = ref(false);
let isPointerDown = false;
let startX = 0;
let startY = 0;
let initialPosX = 0;
let initialPosY = 0;
let hasDragged = false;

const initPosition = () => {
    const saved = localStorage.getItem('customer_chat_btn_position');
    const winW = typeof window !== 'undefined' ? window.innerWidth : 1200;
    const winH = typeof window !== 'undefined' ? window.innerHeight : 800;

    if (saved) {
        try {
            const parsed = JSON.parse(saved);
            if (typeof parsed.x === 'number' && typeof parsed.y === 'number') {
                const clampedX = Math.max(MARGIN, Math.min(winW - BUTTON_SIZE - MARGIN, parsed.x));
                const clampedY = Math.max(MARGIN, Math.min(winH - BUTTON_SIZE - MARGIN, parsed.y));
                pos.value = { x: clampedX, y: clampedY };
                isInitialized.value = true;
                return;
            }
        } catch (e) {
            console.warn('Failed to parse saved chat button position', e);
        }
    }

    // Default: bottom-right
    pos.value = {
        x: Math.max(MARGIN, winW - BUTTON_SIZE - 30),
        y: Math.max(MARGIN, winH - BUTTON_SIZE - 30)
    };
    isInitialized.value = true;
};

const handleWindowResize = () => {
    const winW = window.innerWidth;
    const winH = window.innerHeight;
    pos.value = {
        x: Math.max(MARGIN, Math.min(winW - BUTTON_SIZE - MARGIN, pos.value.x)),
        y: Math.max(MARGIN, Math.min(winH - BUTTON_SIZE - MARGIN, pos.value.y))
    };
};

const onPointerDown = (e) => {
    if (e.button && e.button !== 0) return;
    isPointerDown = true;
    hasDragged = false;
    startX = e.clientX;
    startY = e.clientY;
    initialPosX = pos.value.x;
    initialPosY = pos.value.y;

    window.addEventListener('pointermove', onPointerMove, { passive: false });
    window.addEventListener('pointerup', onPointerUp);
    window.addEventListener('pointercancel', onPointerUp);
};

const onPointerMove = (e) => {
    if (!isPointerDown) return;
    const dx = e.clientX - startX;
    const dy = e.clientY - startY;

    if (!hasDragged && Math.hypot(dx, dy) > 5) {
        hasDragged = true;
        isDragging.value = true;
    }

    if (hasDragged) {
        if (e.cancelable) e.preventDefault();
        const winW = window.innerWidth;
        const winH = window.innerHeight;
        const newX = Math.max(MARGIN, Math.min(winW - BUTTON_SIZE - MARGIN, initialPosX + dx));
        const newY = Math.max(MARGIN, Math.min(winH - BUTTON_SIZE - MARGIN, initialPosY + dy));
        pos.value = { x: newX, y: newY };
    }
};

const onPointerUp = () => {
    if (!isPointerDown) return;
    isPointerDown = false;

    window.removeEventListener('pointermove', onPointerMove);
    window.removeEventListener('pointerup', onPointerUp);
    window.removeEventListener('pointercancel', onPointerUp);

    if (hasDragged) {
        localStorage.setItem('customer_chat_btn_position', JSON.stringify(pos.value));
        setTimeout(() => {
            isDragging.value = false;
            hasDragged = false;
        }, 80);
    } else {
        isDragging.value = false;
    }
};

const handleFabClick = (e) => {
    if (hasDragged || isDragging.value) {
        e.preventDefault();
        e.stopPropagation();
        return;
    }
    isOpen.value = true;
};

const chatWindowStyle = computed(() => {
    if (typeof window === 'undefined') return {};
    const winW = window.innerWidth;
    const winH = window.innerHeight;
    const isMobile = winW <= 480;

    if (isMobile) {
        return {
            position: 'fixed',
            left: '10px',
            right: '10px',
            bottom: '10px',
            top: 'auto',
            width: `${winW - 20}px`,
            height: `${Math.min(580, winH - 20)}px`,
            maxHeight: `${winH - 20}px`,
            zIndex: 5000
        };
    }

    const winWidth = Math.min(380, winW - 30);
    const winHeight = Math.min(600, winH - 60);

    let left = pos.value.x;
    let top = pos.value.y - winHeight - 15;

    if (pos.value.x > winW / 2) {
        left = pos.value.x + BUTTON_SIZE - winWidth;
    }

    if (top < 15) {
        top = pos.value.y + BUTTON_SIZE + 15;
    }

    left = Math.max(15, Math.min(winW - winWidth - 15, left));
    top = Math.max(15, Math.min(winH - winHeight - 15, top));

    return {
        position: 'fixed',
        left: `${left}px`,
        top: `${top}px`,
        width: `${winWidth}px`,
        height: `${winHeight}px`,
        bottom: 'auto',
        right: 'auto',
        zIndex: 5000
    };
});

onUnmounted(() => {
    clearGuestTimer();
    if (lockTimer) clearInterval(lockTimer);
    window.removeEventListener('resize', handleWindowResize);
    window.removeEventListener('pointermove', onPointerMove);
    window.removeEventListener('pointerup', onPointerUp);
    window.removeEventListener('pointercancel', onPointerUp);
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
    if (isChatLocked.value) return;
    if ((!message.value.trim() && !imagePreview.value) || isSending.value) return;

    const now = Date.now();
    if (now - lastSendTime.value < COOLDOWN_MS) return;

    let userMsg = message.value;
    const lowerMsg = userMsg.toLowerCase().trim();

    // 1. KIỂM DUYỆT NỘI DUNG TIN NHẮN (Nếu có nhập text)
    if (userMsg.trim()) {
        const modResult = validateChatMessage(userMsg);
        if (!modResult.isValid) {
            violationCount.value++;
            message.value = '';

            // Thêm tin nhắn cảnh báo hệ thống vào khung chat
            chatHistory.value.push({
                id: Date.now(),
                sender: 'system',
                isWarning: true,
                text: `⚠️ **Cảnh báo:** ${modResult.reason}`
            });
            scrollToBottom();

            // Nếu vi phạm liên tục 3 lần: Tạm khóa gửi 30 giây
            if (violationCount.value >= 3) {
                startLockCooldown();
                chatHistory.value.push({
                    id: Date.now() + 1,
                    sender: 'system',
                    isWarning: true,
                    text: '⛔ Bạn đã gửi nội dung không phù hợp quá 3 lần. Khung chat tạm khóa gửi tin nhắn trong 30 giây.'
                });
                scrollToBottom();
            }
            return;
        }
    }

    // Tin nhắn hợp lệ: Reset violation count
    if (violationCount.value > 0) {
        violationCount.value = 0;
    }

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
    resetGuestInactivityTimer();

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
    initPosition();
    window.addEventListener('resize', handleWindowResize);
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

/** Mở ảnh trong tab mới khi click */
const openChatImage = (url) => {
    if (url) window.open(url, '_blank');
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
                    <v-rating v-model="ratingScore" color="amber" active-color="amber" hover size="x-large"></v-rating>

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
                    <v-btn color="black" variant="flat" :loading="isSubmittingRating" @click="submitRating" class="px-6"
                        >Gửi đánh giá</v-btn
                    >
                </v-card-actions>
            </v-card>
        </v-dialog>
        <!-- Floating Chat Icon (Draggable) -->
        <div
            v-if="!isOpen && isInitialized"
            class="chat-fab-container"
            :class="{ 'is-dragging': isDragging, 'tooltip-right': pos.x < 180 }"
            :style="{ left: `${pos.x}px`, top: `${pos.y}px`, bottom: 'auto', right: 'auto' }"
            @pointerdown="onPointerDown"
            @click="handleFabClick"
        >
            <v-btn icon color="primary" size="large" elevation="8" class="chat-fab">
                <v-icon size="26" color="white">mdi-message-text-outline</v-icon>
            </v-btn>
            <div class="chat-tooltip">Chat hỗ trợ trực tuyến!</div>
        </div>

        <!-- Chat Window -->
        <transition name="chat-slide">
            <div v-if="isOpen" class="chat-window elevation-8" :style="chatWindowStyle">
                <!-- Header -->
                <div class="chat-header">
                    <div class="header-content">
                        <v-avatar size="34" class="header-avatar elevation-1">
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

                <!-- Guest notice banner (2 min timeout) -->
                <div v-if="!authStore.isLoggedIn" class="guest-banner-notice">
                    <v-icon size="13" color="amber-darken-3" class="mr-1">mdi-clock-outline</v-icon>
                    <span>Khách: Tự đóng & reset sau 2p không gửi tin.</span>
                    <button class="guest-login-btn ml-1 font-weight-bold" @click="router.push('/user/login')">Đăng nhập</button>
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
                            <div class="system-msg-wrap" :class="{ 'is-warning': msg.isWarning }">
                                <span class="system-msg" v-html="marked(msg.text)"></span>
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
                                <!-- Ảnh gửi trực tiếp (local base64 preview) hoặc từ history (imageUrl từ server) -->
                                <div v-if="msg.image || msg.imageUrl" class="message-image">
                                    <img
                                        :src="msg.image || msg.imageUrl"
                                        alt="Uploaded Image"
                                        style="max-width: 100%; border-radius: 8px; margin-bottom: 8px; cursor: pointer"
                                        @click="openChatImage(msg.image || msg.imageUrl)"
                                    />
                                </div>
                                <div v-if="msg.text" class="message-bubble" v-html="marked(msg.text)"></div>

                                <!-- Product Showcase in Chat -->
                                <div v-if="msg.products && msg.products.length" class="product-showcase-list mt-3">
                                    <ProductShowcaseCard v-for="(p, pIdx) in msg.products" :key="p.idSanPham || p.id || pIdx" :product="p" @view-detail="goToDetail" />
                                </div>

                                <!-- Inline Quick Suggestions -->
                                <div v-if="msg.sender !== 'user' && msg.suggestions && msg.suggestions.length" class="inline-suggestions-list mt-2">
                                    <button
                                        v-for="s in msg.suggestions"
                                        :key="s"
                                        class="inline-sugg-pill"
                                        @click="sendSuggestion(s)"
                                    >
                                        <v-icon size="12" class="mr-1 text-primary">mdi-lightning-bolt</v-icon>
                                        {{ s }}
                                    </button>
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

                            <!-- Zalo Quick Link -->
                            <a href="https://zalo.me/0987654321" target="_blank" class="handoff-btn zalo-btn text-decoration-none">
                                <v-icon icon="mdi-message-processing" size="small" class="mr-2"></v-icon>
                                Chat Qua Zalo CSKH (0987.654.321)
                            </a>

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

                    <!-- Lock Notice if Spamming -->
                    <div v-if="isChatLocked" class="locked-chat-notice">
                        <v-icon size="15" color="error" class="mr-1">mdi-lock-clock</v-icon>
                        Tạm khóa gửi tin do vi phạm ngôn từ. Thử lại sau {{ lockCountdown }}s
                    </div>

                    <div class="input-container-wrapper" :class="{ 'is-disabled': isChatLocked }">
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
                                :disabled="isChatLocked"
                                @click="showSuggestions = !showSuggestions"
                            ></v-btn>

                            <!-- Image Upload Button -->
                            <v-btn
                                icon="mdi-image-outline"
                                variant="text"
                                size="small"
                                color="grey-darken-1"
                                class="mr-2"
                                :disabled="isChatLocked"
                                @click="triggerImageUpload"
                            ></v-btn>
                            <input type="file" ref="fileInput" accept="image/*" style="display: none" @change="handleImageUpload" />

                            <textarea
                                v-model="message"
                                :placeholder="isChatLocked ? `Đang tạm khóa (${lockCountdown}s)...` : 'Nhập câu hỏi của bạn...'"
                                rows="1"
                                :disabled="isChatLocked"
                                @keydown.enter.prevent="sendMessage"
                                @input="updateActivity"
                            ></textarea>
                            <v-btn
                                icon="mdi-send"
                                variant="text"
                                :disabled="(!message.trim() && !imagePreview) || isSending || isChatLocked"
                                :color="(message.trim() || imagePreview) && !isSending && !isChatLocked ? 'black' : 'grey-lighten-1'"
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
    z-index: 5000;
    cursor: grab;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    user-select: none;
    touch-action: none;

    &.is-dragging {
        cursor: grabbing !important;
        opacity: 0.95;
        transform: scale(1.06);
        transition: none !important;

        .chat-fab {
            box-shadow: 0 16px 32px rgba(37, 99, 235, 0.5) !important;
            transform: scale(1.05);
        }

        .chat-tooltip {
            display: none !important;
        }
    }

    &:not(.is-dragging):hover {
        .chat-fab {
            transform: scale(1.1);
        }
        .chat-tooltip {
            opacity: 1;
            transform: translateX(-10px);
        }
    }

    &.tooltip-right:not(.is-dragging):hover {
        .chat-tooltip {
            transform: translateX(10px);
        }
    }
}

.chat-fab {
    width: 58px !important;
    height: 58px !important;
    border-radius: 50% !important;
    background: linear-gradient(135deg, #1e257c 0%, #2563eb 100%) !important;
    box-shadow: 0 10px 25px rgba(37, 99, 235, 0.4) !important;
    transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    user-select: none;
    pointer-events: none;
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
    user-select: none;

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

.tooltip-right .chat-tooltip {
    right: auto;
    left: 70px;

    &::after {
        right: auto;
        left: -5px;
        border-left: none;
        border-right: 5px solid #000;
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
    background: linear-gradient(135deg, #1e257c 0%, #23318c 50%, #1d4ed8 100%) !important;
    padding: 16px 20px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    color: #ffffff !important;
    box-shadow: 0 4px 16px rgba(30, 37, 124, 0.2);

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
            background: linear-gradient(135deg, #1e257c 0%, #2563eb 100%) !important;
            color: #ffffff !important;
            border-radius: 18px 18px 0 18px;
            box-shadow: 0 4px 14px rgba(30, 37, 124, 0.2);
            border: none;
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

/* Product Showcase List: Carousel cuộn ngang siêu mượt */
.product-showcase-list {
    display: flex;
    flex-direction: row;
    gap: 12px;
    padding: 6px 2px 10px 2px;
    overflow-x: auto;
    scroll-snap-type: x mandatory;
    -webkit-overflow-scrolling: touch;
    width: 100%;
    max-width: 310px;

    &::-webkit-scrollbar {
        height: 4px;
    }
    &::-webkit-scrollbar-thumb {
        background: #cbd5e1;
        border-radius: 10px;
    }
    &::-webkit-scrollbar-track {
        background: transparent;
    }

    :deep(.product-showcase-card) {
        scroll-snap-align: start;
    }
}

/* Inline Quick Suggestions */
.inline-suggestions-list {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 8px;

    .inline-sugg-pill {
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        color: #1e293b;
        font-size: 0.72rem;
        font-weight: 600;
        padding: 4px 10px;
        border-radius: 20px;
        cursor: pointer;
        display: inline-flex;
        align-items: center;
        transition: all 0.2s ease;
        text-align: left;
        line-height: 1.2;

        &:hover {
            background: #eff6ff;
            border-color: #3b82f6;
            color: #1d4ed8;
            transform: translateY(-1px);
            box-shadow: 0 2px 6px rgba(59, 130, 246, 0.15);
        }
    }
}

/* Guest notice banner */
.guest-banner-notice {
    background: #fefce8;
    border-bottom: 1px solid #fef08a;
    color: #854d0e;
    font-size: 0.72rem;
    padding: 5px 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    line-height: 1.2;

    .guest-login-btn {
        background: none;
        border: none;
        color: #1e257c;
        text-decoration: underline;
        cursor: pointer;
        font-size: 0.72rem;
        padding: 0;
        &:hover {
            color: #2563eb;
        }
    }
}

/* Locked Chat Notice */
.locked-chat-notice {
    background: #fee2e2;
    color: #991b1b;
    border: 1px solid #fca5a5;
    border-radius: 8px;
    padding: 6px 10px;
    font-size: 0.75rem;
    font-weight: 600;
    text-align: center;
    margin-bottom: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
}

/* System Message */
.is-system {
    align-self: center;
    max-width: 95%;
    .system-msg-wrap {
        text-align: center;
        width: 100%;

        &.is-warning {
            .system-msg {
                background: #fff7ed !important;
                border: 1px solid #fdba74 !important;
                color: #c2410c !important;
                font-size: 0.8rem !important;
                font-weight: 500 !important;
                padding: 10px 14px !important;
                border-radius: 12px !important;
                box-shadow: 0 2px 8px rgba(249, 115, 22, 0.12);
                text-align: left;
                display: block;
                line-height: 1.45;

                :deep(p) {
                    margin-bottom: 0;
                }
            }
        }
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
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
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
    color: #1e257c;
    border: 1.5px solid #cbd5e1;
    padding: 10px;
    border-radius: 12px;
    font-size: 0.88rem;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 12px;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
        background: #1e257c;
        color: #ffffff;
        border-color: #1e257c;
        transform: translateY(-1px);
        box-shadow: 0 4px 14px rgba(30, 37, 124, 0.25);
    }

    &:active {
        transform: translateY(0);
    }
}

.zalo-btn {
    background: #0068ff !important;
    color: #ffffff !important;
    border-color: #0068ff !important;
    &:hover {
        background: #0052cc !important;
        border-color: #0052cc !important;
        color: #ffffff !important;
    }
}

.suggestions-title {
    font-size: 0.825rem;
    color: #1e257c;
    font-weight: 700;
    margin-bottom: 10px;
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
    background: #f0f4ff;
    color: #1e257c;
    border: 1.5px solid #c7d2fe;
    padding: 7px 14px;
    border-radius: 20px;
    font-size: 0.825rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
        background: #1e257c;
        color: #ffffff;
        border-color: #1e257c;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(30, 37, 124, 0.25);
    }

    &.collapse-pill {
        border-style: dashed;
        background: transparent;
        color: #64748b;
        border-color: #cbd5e1;

        &:hover {
            background: #f1f5f9;
            color: #1e257c;
            border-color: #94a3b8;
        }
    }
}
</style>
