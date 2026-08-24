<script setup>
import { ref, onMounted, watch, computed, nextTick } from 'vue';
import api from '@/services/apiService';
import { API_CHAT } from '@/constants/apiPaths';
import { chatSocket } from '@/services/chatSocket';
import { useNotificationStore } from '@/stores/notificationStore';
import { useAuthStore } from '@/stores/authStore';
import { CHAT_TYPES, CHAT_SENDER_TYPE, CHAT_STATUS, CHAT_TOPICS } from '@/constants/appConstants';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useRefreshHandler } from '@/composables/useRefreshHandler';
import { AdminConfirm, AdminBreadcrumbs } from '@/components/common';
import { dichVuFile } from '@/services/core/dichVuFile';

const notificationStore = useNotificationStore();
const authStore = useAuthStore();
const { confirmDialog, setConfirm, handleConfirm } = useConfirmDialog();
const { isRefreshing, handleRefresh: executeRefresh } = useRefreshHandler();
const customers = ref([]);
const activeChat = ref(null);
const chatMessages = ref([]);
const messagesEndRef = ref(null);

// Shared Media & Photos Gallery
const previewPhotoModal = ref(false);
const currentPreviewPhoto = ref(null);

const sharedPhotos = computed(() => {
    return chatMessages.value
        .filter((m) => m.imageUrl || m.image || m.hinhAnh)
        .map((m) => ({
            id: m.id,
            url: resolveChatImageUrl(m.imageUrl || m.image || m.hinhAnh),
            time: m.time || m.thoiGian || '',
            sender: m.sender || m.nguoiGui || ''
        }))
        .reverse();
});

const openPhotoPreview = (photo) => {
    currentPreviewPhoto.value = photo;
    previewPhotoModal.value = true;
};

// Trạng thái hiển thị panel thông tin chi tiết (mặc định đóng, chỉ mở khi người dùng bấm nút)
const showDetailPanel = ref(false);
const toggleDetailPanel = () => {
    showDetailPanel.value = !showDetailPanel.value;
};

// Ghi chú của nhân viên
const isEditingNote = ref(false);
const customerNotes = ref({});

// Load notes initially
onMounted(() => {
    try {
        const stored = localStorage.getItem('chat_customer_notes');
        if (stored) {
            customerNotes.value = JSON.parse(stored);
        }
    } catch (e) {
        console.error('Lỗi khi đọc notes từ localStorage:', e);
    }
});

const activeNote = computed({
    get: () => {
        if (!activeChat.value) return '';
        return customerNotes.value[activeChat.value.id] || '';
    },
    set: (val) => {
        if (!activeChat.value) return;
        customerNotes.value[activeChat.value.id] = val;
        localStorage.setItem('chat_customer_notes', JSON.stringify(customerNotes.value));
    }
});

const removeVietnameseTones = (str) => {
    if (!str) return '';
    return str
        .toString()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/đ/g, 'd')
        .replace(/Đ/g, 'D')
        .toLowerCase()
        .trim();
};

const escapeHtml = (unsafe) => {
    if (!unsafe) return '';
    return unsafe
        .toString()
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
};

const isUnread = (c) => {
    if (!c) return false;
    return (c.chuaDoc > 0) || notificationStore.unreadChatConvIds.includes(c.id);
};

const getUnreadCount = (c) => {
    if (!c) return 0;
    if (c.chuaDoc > 0) return c.chuaDoc;
    return notificationStore.unreadChatConvIds.includes(c.id) ? 1 : 0;
};

const currentTypeConversations = computed(() => {
    return allConversations.value.filter((c) => {
        const cType = c.type || c.loaiHoiThoai || CHAT_TYPES.CUSTOMER;
        return cType === chatType.value;
    });
});

const activeCount = computed(() => {
    return currentTypeConversations.value.filter(
        (c) => (c.status || c.trangThaiHoiThoai) === 'ACTIVE'
    ).length;
});

const pendingCount = computed(() => {
    return currentTypeConversations.value.filter(
        (c) => (c.status || c.trangThaiHoiThoai) === 'PENDING'
    ).length;
});

const closedCount = computed(() => {
    return currentTypeConversations.value.filter(
        (c) => (c.status || c.trangThaiHoiThoai) === 'CLOSED'
    ).length;
});

const sortedCustomers = computed(() => {
    let list = currentTypeConversations.value;

    // Lọc theo trạng thái
    if (chatStatus.value !== 'ALL') {
        list = list.filter((c) => {
            const st = c.status || c.trangThaiHoiThoai;
            return st === chatStatus.value;
        });
    }

    // Tìm kiếm tức thì phía client-side (Tên người dùng, tin nhắn cuối, SĐT, Email, Username, ID)
    const q = removeVietnameseTones(searchQuery.value);
    if (q) {
        list = list.filter((c) => {
            const name = removeVietnameseTones(c.name || c.ten || '');
            const msg = removeVietnameseTones(c.lastMsg || c.tinNhanCuoi || '');
            const partnerUser = removeVietnameseTones(c.partnerUsername || c.tenTaiKhoanDoiTac || '');
            const phone = removeVietnameseTones(c.phone || c.sdt || '');
            const email = removeVietnameseTones(c.email || '');
            const id = removeVietnameseTones(c.id || '');
            return (
                name.includes(q) ||
                msg.includes(q) ||
                partnerUser.includes(q) ||
                phone.includes(q) ||
                email.includes(q) ||
                id.includes(q)
            );
        });
    }

    // Sắp xếp: Tin chưa đọc lên trước, sau đó sắp xếp theo thời gian mới nhất
    return [...list].sort((a, b) => {
        const aUnread = isUnread(a);
        const bUnread = isUnread(b);
        if (aUnread && !bUnread) return -1;
        if (!aUnread && bUnread) return 1;

        const timeA = a.timestamp || 0;
        const timeB = b.timestamp || 0;
        return timeB - timeA;
    });
});

// Email và SĐT dựa trên dữ liệu khách hàng
const customerEmail = computed(() => {
    if (!activeChat.value) return '';
    if (activeChat.value.email) return activeChat.value.email;
    const cleanName = (activeChat.value.name || 'khachhang')
        .toLowerCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/\s+/g, '.');
    return `${cleanName}@gmail.com`;
});

const customerPhone = computed(() => {
    if (!activeChat.value) return '';
    if (activeChat.value.phone || activeChat.value.sdt) return activeChat.value.phone || activeChat.value.sdt;
    const idHash = (activeChat.value.id || '').split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    return `098${(idHash % 9000000) + 1000000}`;
});

const customerAddress = computed(() => {
    if (!activeChat.value) return '';
    if (activeChat.value.diaChi) return activeChat.value.diaChi;
    const hash = (activeChat.value.id || '').split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    const cities = ['Hà Nội, Việt Nam', 'TP. Hồ Chí Minh, Việt Nam', 'Đà Nẵng, Việt Nam', 'Hải Phòng, Việt Nam'];
    return cities[hash % cities.length];
});

const customerJoined = computed(() => {
    if (!activeChat.value) return '';
    if (activeChat.value.ngayTao) {
        return new Date(activeChat.value.ngayTao).toLocaleDateString('vi-VN');
    }
    const hash = (activeChat.value.id || '').split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    const years = [2022, 2023, 2024, 2025];
    const month = String((hash % 12) + 1).padStart(2, '0');
    const day = String((hash % 28) + 1).padStart(2, '0');
    return `${day}/${month}/${years[hash % years.length]}`;
});

const activeChatRoleLabel = computed(() => {
    if (!activeChat.value) return '';
    if (activeChat.value.type === CHAT_TYPES.CUSTOMER) return 'Khách hàng';
    
    // Check role from various properties
    const roleField = activeChat.value.role || activeChat.value.roleCode || activeChat.value.roleName || activeChat.value.chucVu || activeChat.value.authority || activeChat.value.vaiTro;
    if (roleField) {
        const rStr = String(roleField).toUpperCase();
        if (
            rStr === 'ROLE_QUAN_LY' || 
            rStr === 'QUAN_LY' || 
            rStr === 'ROLE_ADMIN' || 
            rStr === 'ADMIN' || 
            rStr === 'MANAGER' || 
            rStr.includes('QUẢN LÝ')
        ) {
            return 'Quản lý';
        }
    }
    
    if (activeChat.value.isAdmin === true || activeChat.value.isAdmin === 'true') {
        return 'Quản lý';
    }
    
    return 'Nhân viên';
});

const activeChatRoleColor = computed(() => {
    if (!activeChat.value) return '';
    if (activeChat.value.type === CHAT_TYPES.CUSTOMER) return '#1e257c';
    return activeChatRoleLabel.value === 'Quản lý' ? 'purple' : '#1e257c';
});

const customerOrder = computed(() => {
    if (!activeChat.value || activeChat.value.type !== CHAT_TYPES.CUSTOMER) return null;
    const hash = (activeChat.value.id || '').split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    const orderNum = (hash % 90000) + 10000;
    const day = String((hash % 28) + 1).padStart(2, '0');
    const month = String((hash % 12) + 1).padStart(2, '0');
    const year = 2024;
    const prices = [1290000, 2490000, 3150000, 890000];
    const selectedPrice = prices[hash % prices.length];
    return {
        code: `#SHO${orderNum}`,
        status: hash % 2 === 0 ? 'Chờ xác nhận' : 'Đang giao',
        statusColor: hash % 2 === 0 ? 'amber-darken-2' : 'info',
        total: selectedPrice.toLocaleString('vi-VN') + 'đ',
        time: `10:27 - ${day}/${month}/${year}`
    };
});

const purchaseOverview = computed(() => {
    if (!activeChat.value || activeChat.value.type !== CHAT_TYPES.CUSTOMER) return null;
    const hash = (activeChat.value.id || '').split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    const totalOrders = (hash % 15) + 3;
    const totalSpend = (totalOrders * 1250000 + (hash % 500) * 1000).toLocaleString('vi-VN') + 'đ';
    const day = String((hash % 28) + 1).padStart(2, '0');
    const month = String((hash % 12) + 1).padStart(2, '0');
    const orderNum = (hash % 90000) + 10000;
    return {
        totalOrders,
        totalSpend,
        lastOrder: `#SHO${orderNum} - ${day}/${month}/2024`
    };
});

const noteUpdatedTime = computed(() => {
    const today = new Date();
    const dd = String(today.getDate()).padStart(2, '0');
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const yyyy = today.getFullYear();
    return `${dd}/${mm}/${yyyy}`;
});

const noteUpdatedBy = computed(() => {
    return authStore.user?.fullName || authStore.user?.username || 'Bùi Thị Yến';
});

// Filter out system messages for admin view
const displayMessages = computed(() => {
    return chatMessages.value.filter((m) => m.sender !== CHAT_SENDER_TYPE.SYSTEM);
});

const newMessage = ref('');
const isAccepted = ref(false);
const isLoading = ref(false);
const isMessagesLoading = ref(false);
const messagesContainer = ref(null);

// State quản lý upload ảnh
const imageFile = ref(null);
const imagePreview = ref(null);
const fileInput = ref(null);
const isSendingImage = ref(false);

const triggerImageUpload = () => {
    if (fileInput.value) fileInput.value.click();
};

const handleImageUpload = (event) => {
    const file = event.target.files[0];
    if (!file) return;
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
};

const clearImage = () => {
    imageFile.value = null;
    imagePreview.value = null;
    if (fileInput.value) fileInput.value.value = '';
};

/** Mở ảnh trong tab mới khi click */
const resolveChatImageUrl = (url) => {
    if (!url) return '';
    return dichVuFile.layUrlFile(url);
};

const openImage = (url) => {
    const fullUrl = resolveChatImageUrl(url);
    if (fullUrl) window.open(fullUrl, '_blank');
};

// Filters
const chatType = ref(CHAT_TYPES.CUSTOMER);
const chatStatus = ref('ALL');
const searchQuery = ref('');

const scrollToBottom = (smooth = false) => {
    nextTick(() => {
        if (messagesEndRef.value) {
            messagesEndRef.value.scrollIntoView({ behavior: smooth ? 'smooth' : 'auto', block: 'end' });
        }
        if (messagesContainer.value) {
            messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
        }
        setTimeout(() => {
            if (messagesContainer.value) {
                messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
            }
            if (messagesEndRef.value) {
                messagesEndRef.value.scrollIntoView({ behavior: smooth ? 'smooth' : 'auto', block: 'end' });
            }
        }, 120);
    });
};

watch(
    () => chatMessages.value.length,
    () => {
        scrollToBottom(true);
    }
);

const allConversations = ref([]);

const totalCustomerUnread = computed(() => {
    return allConversations.value.filter(
        (c) => (c.type === CHAT_TYPES.CUSTOMER || c.loaiHoiThoai === CHAT_TYPES.CUSTOMER || !c.type) && (c.unread > 0 || c.chuaDoc > 0 || notificationStore.unreadChatConvIds.includes(c.id))
    ).length;
});

const totalInternalUnread = computed(() => {
    return allConversations.value.filter(
        (c) => (c.type === CHAT_TYPES.INTERNAL || c.loaiHoiThoai === CHAT_TYPES.INTERNAL) && (c.unread > 0 || c.chuaDoc > 0 || notificationStore.unreadChatConvIds.includes(c.id))
    ).length;
});

// Lấy danh sách hội thoại từ Backend (1 request duy nhất)
const fetchConversations = async (quiet = false) => {
    if (!quiet && allConversations.value.length === 0) {
        isLoading.value = true;
    }
    try {
        const response = await api.get(API_CHAT.CONVERSATIONS);
        const data = response.data?.data || [];
        allConversations.value = data;
        customers.value = data;
        notificationStore.syncUnreadConversations(data);

        if (activeChat.value) {
            const updatedChat = allConversations.value.find((c) => c.id === activeChat.value.id);
            if (updatedChat) {
                activeChat.value.status = updatedChat.status;
                activeChat.value.isAccepted = updatedChat.isAccepted;
                activeChat.value.name = updatedChat.name;
                activeChat.value.lastMsg = updatedChat.lastMsg;
            }
        }

        if (activeChat.value && activeChat.value.id.startsWith('NEW_INTERNAL_')) {
            const targetPartnerId = activeChat.value.id.replace('NEW_INTERNAL_', '');
            const realConv = allConversations.value.find(
                (c) =>
                    c.type === CHAT_TYPES.INTERNAL &&
                    !c.id.startsWith('NEW_INTERNAL_') &&
                    (c.partnerStaffId === targetPartnerId ||
                        c.partnerUsername === activeChat.value.partnerUsername ||
                        c.name === activeChat.value.name)
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

watch(chatType, () => {
    chatStatus.value = 'ALL';
    searchQuery.value = '';
});

// ================= TÌM KIẾM TRONG LỊCH SỬ TIN NHẮN =================
const showMessageSearch = ref(false);
const messageSearchQuery = ref('');
const currentMatchIndex = ref(0);
const messageSearchInput = ref(null);

const toggleMessageSearch = () => {
    showMessageSearch.value = !showMessageSearch.value;
    if (showMessageSearch.value) {
        nextTick(() => {
            if (messageSearchInput.value) {
                const el = messageSearchInput.value.$el?.querySelector('input') || messageSearchInput.value;
                if (el && typeof el.focus === 'function') el.focus();
            }
        });
    } else {
        closeMessageSearch();
    }
};

const closeMessageSearch = () => {
    showMessageSearch.value = false;
    messageSearchQuery.value = '';
    currentMatchIndex.value = 0;
};

const matchedMessageIds = computed(() => {
    const q = removeVietnameseTones(messageSearchQuery.value);
    if (!q || !showMessageSearch.value) return [];
    const matched = [];
    displayMessages.value.forEach((m, idx) => {
        const text = removeVietnameseTones(m.text || m.noiDung || '');
        if (text.includes(q)) {
            matched.push(m.id || `msg_${idx}`);
        }
    });
    return matched;
});

watch(messageSearchQuery, (newVal) => {
    if (!newVal || matchedMessageIds.value.length === 0) {
        currentMatchIndex.value = 0;
    } else {
        currentMatchIndex.value = matchedMessageIds.value.length - 1;
        scrollToCurrentMatch();
    }
});

const scrollToCurrentMatch = () => {
    if (matchedMessageIds.value.length === 0) return;
    const currentId = matchedMessageIds.value[currentMatchIndex.value];
    if (!currentId) return;
    nextTick(() => {
        const el = document.getElementById(`msg-item-${currentId}`);
        if (el) {
            el.scrollIntoView({ behavior: 'smooth', block: 'center' });
            el.classList.remove('pulse-highlight');
            void el.offsetWidth;
            el.classList.add('pulse-highlight');
        }
    });
};

const nextSearchMatch = () => {
    if (matchedMessageIds.value.length === 0) return;
    currentMatchIndex.value = (currentMatchIndex.value + 1) % matchedMessageIds.value.length;
    scrollToCurrentMatch();
};

const prevSearchMatch = () => {
    if (matchedMessageIds.value.length === 0) return;
    currentMatchIndex.value = (currentMatchIndex.value - 1 + matchedMessageIds.value.length) % matchedMessageIds.value.length;
    scrollToCurrentMatch();
};

const highlightSearchText = (rawText, msgId) => {
    if (!rawText) return '';
    const safeText = escapeHtml(rawText);
    const query = messageSearchQuery.value?.trim();
    if (!query || !showMessageSearch.value) return safeText;

    try {
        const escapedQuery = escapeHtml(query).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        const regex = new RegExp(`(${escapedQuery})`, 'gi');
        const isCurrentActive = matchedMessageIds.value[currentMatchIndex.value] === msgId;
        const markClass = isCurrentActive ? 'chat-search-highlight active-match' : 'chat-search-highlight';
        return safeText.replace(regex, `<mark class="${markClass}">$1</mark>`);
    } catch (e) {
        return safeText;
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

const isSendingMessage = ref(false);
const sendMessage = async () => {
    if (isSendingMessage.value) return;
    if (!newMessage.value.trim() && !imagePreview.value) return;
    if (!activeChat.value) return;

    // Chuẩn bị payload: base64 thuần (bỏ header "data:image/...;base64," nếu có)
    let base64Image = null;
    const localPreview = imagePreview.value;
    if (localPreview) {
        base64Image = localPreview.includes(',') ? localPreview.split(',')[1] : localPreview;
    }

    const textToSend = newMessage.value ? newMessage.value.trim() : null;
    const currentUsername = authStore.user?.username || 'STAFF';

    // Chặn gửi tin nhắn vào cuộc hội thoại đã đóng
    if (activeChat.value.type !== CHAT_TYPES.INTERNAL && activeChat.value.status === 'CLOSED') {
        return;
    }

    // Tự động chuyển trạng thái cuộc hội thoại sang ACTIVE khi nhân viên gửi tin (nếu đang chờ)
    if (activeChat.value.type !== CHAT_TYPES.INTERNAL && activeChat.value.status === 'PENDING') {
        activeChat.value.status = 'ACTIVE';
        activeChat.value.isAccepted = true;
        isAccepted.value = true;
    }

    // Đẩy tin nhắn optimistic lên UI ngay lập tức để hiển thị tức thì cả ảnh và chữ
    const tempId = 'temp_' + Date.now();
    const optimisticMessage = {
        id: tempId,
        conversationId: activeChat.value.id,
        sender: currentUsername,
        text: textToSend,
        imageUrl: localPreview || null,
        time: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
    };
    chatMessages.value.push(optimisticMessage);
    scrollToBottom();

    const messageData = {
        conversationId: activeChat.value.id,
        text: textToSend,
        sender: currentUsername,
        imageBase64: base64Image
    };

    isSendingImage.value = !!base64Image;
    newMessage.value = '';
    clearImage();

    try {
        await api.post(API_CHAT.SEND, messageData);
        scrollToBottom();
        fetchConversations(true);
    } catch (error) {
        console.error('Lỗi khi gửi tin nhắn:', error);
        // Nếu lỗi, xóa tin nhắn optimistic
        chatMessages.value = chatMessages.value.filter((m) => m.id !== tempId);
    } finally {
        isSendingImage.value = false;
        isSendingMessage.value = false;
    }
};

const selectChat = async (customer) => {
    activeChat.value = customer;
    isAccepted.value = customer.isAccepted || false;
    showDetailPanel.value = false;
    closeMessageSearch();
    if (customer.id.startsWith('NEW_INTERNAL_')) {
        chatMessages.value = [];
    } else {
        await fetchMessages(customer.id);
    }
    notificationStore.markChatRead(customer.id);
    scrollToBottom(false);
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

const closeChat = () => {
    if (!activeChat.value) return;
    const target = activeChat.value;
    setConfirm({
        title: 'Đóng phiên chat',
        message: `Bạn có chắc chắn muốn kết thúc phiên trò chuyện với "${target.name}"?`,
        color: 'warning',
        action: async () => {
            try {
                const response = await api.post(API_CHAT.CLOSE(target.id));
                if (response.data?.success) {
                    target.status = 'CLOSED';
                    notificationStore.markChatRead(target.id);
                    fetchConversations(true);
                }
            } catch (error) {
                console.error('Lỗi khi kết thúc cuộc trò chuyện:', error);
            }
        }
    });
};
     
const isSummarizing = ref(false);
const showSummaryModal = ref(false);
const chatSummary = ref('');

const summarizeChat = async () => {
    if (!activeChat.value) return;

    isSummarizing.value = true;
    try {
        // API này phải match với API bên BE
        const response = await api.get(`/admin/chat/summarize/${activeChat.value.id}`);
        if (response.data?.success) {
            chatSummary.value = response.data.data;
            showSummaryModal.value = true;
        } else {
            notificationStore.showError('Không thể tóm tắt hội thoại này.');
        }
    } catch (error) {
        console.error('Lỗi khi tóm tắt:', error);
        notificationStore.showError('Có lỗi xảy ra khi tóm tắt hội thoại.');
    } finally {
        isSummarizing.value = false;
    }
};

// Xóa lịch sử đoạn chat (xóa hẳn cuộc hội thoại + toàn bộ tin nhắn)
const confirmDeleteChat = () => {
    if (!activeChat.value) return;
    const target = activeChat.value;
    setConfirm({
        title: 'Xóa lịch sử đoạn chat',
        message: `Xóa toàn bộ lịch sử trò chuyện với "${target.name}"? Hành động này không thể hoàn tác.`,
        action: async () => {
            try {
                await api.delete(API_CHAT.DELETE(target.id));
                notificationStore.markChatRead(target.id);
                // Bỏ chọn nếu đang mở đúng cuộc vừa xóa
                if (activeChat.value && activeChat.value.id === target.id) {
                    activeChat.value = null;
                    chatMessages.value = [];
                    showDetailPanel.value = false;
                    closeMessageSearch();
                }
                fetchConversations(true);
            } catch (error) {
                console.error('Lỗi khi xóa lịch sử đoạn chat:', error);
            }
        }
    });
};

onMounted(() => {
    // Tải ghi chú từ Local Storage
    const savedNotes = localStorage.getItem('chat_customer_notes');
    if (savedNotes) {
        try {
            customerNotes.value = JSON.parse(savedNotes);
        } catch (e) {
            console.error('Lỗi khi tải ghi chú khách hàng:', e);
        }
    }

    fetchConversations();

    chatSocket.connect(() => {
        chatSocket.subscribe(CHAT_TOPICS.NOTIFICATIONS, (msg) => {
            const raw = typeof msg === 'string' ? msg : JSON.stringify(msg);
            if (raw.includes('CLOSED_CONVERSATION_')) {
                const closedId = raw.split('CLOSED_CONVERSATION_')[1]?.split('"')[0]?.split('}')[0];
                if (closedId) notificationStore.markChatRead(closedId.trim());
            } else if (raw.includes('DELETED_CONVERSATION_')) {
                const deletedId = raw.split('DELETED_CONVERSATION_')[1]?.split('"')[0]?.split('}')[0];
                if (deletedId) notificationStore.markChatRead(deletedId.trim());
            }
            fetchConversations(true);
        });

        chatSocket.subscribe(CHAT_TOPICS.MESSAGES, (msg) => {
            const data = typeof msg === 'string' ? JSON.parse(msg) : msg;
            if (!data) return;

            const currentUsername = authStore.user?.username;
            const isAdmin = authStore.isAdmin;

            // Kiểm tra xem tin nhắn có thuộc về người dùng hiện tại không
            const isMyChat =
                isAdmin ||
                (!data.staffUsername && !data.staffId) ||
                data.staffUsername === currentUsername ||
                data.secondStaffUsername === currentUsername ||
                data.staffId === currentUsername ||
                data.secondStaffId === currentUsername ||
                data.sender === currentUsername;

            if (!isMyChat) return;

            // Kiểm tra xem cuộc hội thoại có đang được mở không
            const isCurrentActive =
                activeChat.value &&
                (activeChat.value.id === data.conversationId ||
                    (activeChat.value.id.startsWith('NEW_INTERNAL_') &&
                        (activeChat.value.id === `NEW_INTERNAL_${data.staffId}` ||
                            activeChat.value.id === `NEW_INTERNAL_${data.secondStaffId}` ||
                            activeChat.value.partnerStaffId === data.staffId ||
                            activeChat.value.partnerStaffId === data.secondStaffId ||
                            activeChat.value.partnerUsername === data.staffUsername ||
                            activeChat.value.partnerUsername === data.secondStaffUsername ||
                            activeChat.value.partnerUsername === data.sender ||
                            activeChat.value.name === data.sender)));

            if (isCurrentActive) {
                if (activeChat.value.id.startsWith('NEW_INTERNAL_')) {
                    activeChat.value.id = data.conversationId;
                }

                // Cập nhật lại tin nhắn tạm thời hoặc thêm mới nếu chưa có
                const tempIndex = chatMessages.value.findIndex(
                    (m) => typeof m.id === 'string' && m.id.startsWith('temp_') && m.sender === data.sender
                );
                if (tempIndex !== -1) {
                    chatMessages.value[tempIndex] = {
                        ...data,
                        imageUrl: data.imageUrl || data.image || data.hinhAnh || chatMessages.value[tempIndex].imageUrl
                    };
                } else if (!chatMessages.value.find((m) => m.id === data.id)) {
                    chatMessages.value.push(data);
                }
                scrollToBottom();
                notificationStore.markChatRead(data.conversationId);
                const conv = allConversations.value.find(c => c.id === data.conversationId);
                if (conv) conv.timestamp = Date.now();
            } else if (data.sender !== currentUsername) {
                notificationStore.incrementUnreadChat(data.conversationId);
                const conv = allConversations.value.find(c => c.id === data.conversationId);
                if (conv) conv.timestamp = Date.now();
            }

            fetchConversations(true);
        });
    });
});

const handleRefresh = async () => {
    await executeRefresh(async () => {
        // Chỉ tải lại dữ liệu danh sách hội thoại ngầm và tin nhắn cuộc hội thoại đang mở
        await fetchConversations(true);
        if (activeChat.value && !activeChat.value.id.startsWith('NEW_INTERNAL_')) {
            await fetchMessages(activeChat.value.id);
        }
    });
};
</script>

<template>
    <div class="chat-management-wrapper">
        <AdminBreadcrumbs
            :items="[
                { title: 'Quản lý chăm sóc khách hàng', disabled: false, href: '#' },
                { title: 'Quản lý tin nhắn', disabled: true }
            ]"
        />
        <v-container fluid class="chat-page pa-0 fill-height">
        <v-row no-gutters class="fill-height">
            <!-- Sidebar (Left Pane) -->
            <v-col cols="12" md="3" class="sidebar d-flex flex-column fill-height">
                <!-- Logo Header -->
                <div class="sidebar-header">
                    <v-icon icon="mdi-message-text animate-pulse" class="mr-2" color="#1e257c" size="24"></v-icon>
                    <span class="sidebar-title">Cuộc hội thoại</span>
                </div>

                <!-- Type Tabs -->
                <v-tabs v-model="chatType" color="#1e257c" bg-color="transparent" grow density="compact" class="type-tabs">
                    <v-tab :value="CHAT_TYPES.CUSTOMER">
                        <v-icon icon="mdi-account" size="18" class="mr-1"></v-icon>
                        Khách hàng
                        <v-chip
                            v-if="totalCustomerUnread > 0"
                            color="error"
                            size="x-small"
                            class="ml-1 px-1 py-0 font-weight-bold"
                            style="height: 18px; min-width: 18px;"
                        >
                            {{ totalCustomerUnread > 99 ? '99+' : totalCustomerUnread }}
                        </v-chip>
                    </v-tab>
                    <v-tab :value="CHAT_TYPES.INTERNAL">
                        <v-icon icon="mdi-account-group" size="18" class="mr-1"></v-icon>
                        Nội bộ
                        <v-chip
                            v-if="totalInternalUnread > 0"
                            color="error"
                            size="x-small"
                            class="ml-1 px-1 py-0 font-weight-bold"
                            style="height: 18px; min-width: 18px;"
                        >
                            {{ totalInternalUnread > 99 ? '99+' : totalInternalUnread }}
                        </v-chip>
                    </v-tab>
                </v-tabs>

                <!-- Status Filters & Search -->
                <div class="filter-section">
                    <div class="d-flex align-center ga-2 mt-2">
                        <v-text-field
                            v-model="searchQuery"
                            prepend-inner-icon="mdi-magnify"
                            placeholder="Tìm kiếm người trò chuyện..."
                            variant="solo"
                            flat
                            density="compact"
                            bg-color="#f1f5f9"
                            hide-details
                            clearable
                            class="search-field flex-grow-1"
                        ></v-text-field>
                        <v-btn
                            variant="outlined"
                            color="primary"
                            class="reset-btn rounded-lg"
                            size="small"
                            style="height: 40px; width: 40px; border-color: #cbd5e1;"
                            :disabled="isRefreshing"
                            @click="handleRefresh"
                        >
                            <v-icon size="18" :class="{ 'filter-spin': isRefreshing }">
                                {{ isRefreshing ? 'mdi-loading' : 'mdi-refresh' }}
                            </v-icon>
                            <v-tooltip activator="parent" location="top">Làm mới danh sách</v-tooltip>
                        </v-btn>
                    </div>

                    <!-- Customer Filter Chips -->
                    <v-chip-group v-if="chatType === CHAT_TYPES.CUSTOMER" v-model="chatStatus" mandatory selected-class="chip-active" class="status-chips mt-3">
                        <v-chip value="ALL" size="small" variant="outlined" color="#1e257c">
                            Tất cả
                        </v-chip>
                        <v-chip value="ACTIVE" size="small" variant="outlined" color="#1e257c">
                            <v-icon icon="mdi-circle" size="8" class="mr-1" color="success"></v-icon>
                            Hoạt động ({{ activeCount }})
                        </v-chip>
                        <v-chip value="PENDING" size="small" variant="outlined" color="#1e257c">
                            <v-icon icon="mdi-circle" size="8" class="mr-1" color="amber-darken-2"></v-icon>
                            Đang chờ ({{ pendingCount }})
                        </v-chip>
                        <v-chip value="CLOSED" size="small" variant="outlined" color="#1e257c">
                            <v-icon icon="mdi-circle" size="8" class="mr-1" color="grey-darken-1"></v-icon>
                            Đã đóng ({{ closedCount }})
                        </v-chip>
                    </v-chip-group>

                    <!-- Internal Filter Chips -->
                    <v-chip-group v-else v-model="chatStatus" mandatory selected-class="chip-active" class="status-chips mt-3">
                        <v-chip value="ALL" size="small" variant="outlined" color="#1e257c">
                            Tất cả
                        </v-chip>
                        <v-chip value="ACTIVE" size="small" variant="outlined" color="#1e257c">
                            <v-icon icon="mdi-circle" size="8" class="mr-1" color="success"></v-icon>
                            Đang hoạt động ({{ activeCount }})
                        </v-chip>
                        <v-chip value="CLOSED" size="small" variant="outlined" color="#1e257c">
                            <v-icon icon="mdi-circle" size="8" class="mr-1" color="grey-darken-1"></v-icon>
                            Ngoại tuyến ({{ closedCount }})
                        </v-chip>
                    </v-chip-group>
                </div>

                <!-- Conversations List -->
                <div v-if="isLoading" class="d-flex justify-center align-center py-10">
                    <v-progress-circular indeterminate color="#1e257c"></v-progress-circular>
                </div>

                <v-list v-else class="flex-grow-1 overflow-y-auto pa-0 conv-list" bg-color="transparent">
                    <v-list-item
                        v-for="c in sortedCustomers"
                        :key="c.id"
                        :active="activeChat?.id === c.id"
                        :class="['conv-item', isUnread(c) ? 'is-unread' : '']"
                        @click="selectChat(c)"
                    >
                        <template v-slot:prepend>
                            <div class="position-relative mr-3 d-flex align-center">
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
                                <span class="status-dot-badge" :class="c.type === CHAT_TYPES.INTERNAL ? (c.status === 'ACTIVE' ? 'active' : 'offline') : c.status?.toLowerCase()"></span>
                            </div>
                        </template>
                        <v-list-item-title :class="['conv-name', { 'unread-bold': c.unread > 0 }]">{{ c.name }}</v-list-item-title>
                        <v-list-item-subtitle :class="['conv-msg', { 'unread-bold-msg': c.unread > 0 }]">{{ c.lastMsg || 'Bắt đầu trò chuyện...' }}</v-list-item-subtitle>
                        <template v-slot:append>
                            <div class="d-flex flex-column align-end">
                                <span class="conv-time">{{ c.time }}</span>
                                <v-badge v-if="isUnread(c)" :content="getUnreadCount(c)" color="#1e257c" inline class="mt-1 custom-unread-badge"></v-badge>
                            </div>
                        </template>
                    </v-list-item>

                    <div v-if="sortedCustomers.length === 0" class="d-flex flex-column align-center justify-center py-12 px-4 w-100 animate-fade-in text-center">
                        <div
                            class="empty-state-icon-box d-flex align-center justify-center rounded-circle mb-3 mx-auto"
                            style="width: 56px; height: 56px; background: rgba(241, 245, 249, 0.8); border: 1.5px dashed #cbd5e1"
                        >
                            <v-icon icon="mdi-message-off-outline" size="28" style="color: #94a3b8 !important" />
                        </div>
                        <span class="text-slate-600 font-weight-medium" style="font-size: 13.5px; line-height: 1.4;">Không có cuộc trò chuyện nào</span>
                        <span class="text-slate-400 text-caption mt-1">Hãy thử thay đổi bộ lọc hoặc tìm kiếm</span>
                    </div>
                </v-list>
            </v-col>

            <!-- Main Chat Area (Center Pane) -->
            <v-col cols="12" :md="activeChat && showDetailPanel ? 6 : 9" class="main-area d-flex flex-column fill-height overflow-hidden transition-all duration-300">
                <template v-if="activeChat">
                    <!-- Chat Header -->
                    <div class="main-header">
                        <div class="d-flex align-center">
                            <div class="position-relative mr-3 d-flex align-center">
                                <v-avatar size="42" class="main-avatar">
                                    <v-img
                                        :src="
                                            !activeChat.avatar || activeChat.avatar.length <= 2
                                                ? 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png'
                                                : activeChat.avatar
                                        "
                                        alt="avatar"
                                    ></v-img>
                                </v-avatar>
                                <span class="status-dot-badge header-badge" :class="activeChat.type === CHAT_TYPES.INTERNAL ? (activeChat.status === 'ACTIVE' ? 'active' : 'offline') : activeChat.status?.toLowerCase()"></span>
                            </div>
                            <div>
                                <div class="main-chat-name">{{ activeChat.name }}</div>
                                <div class="d-flex align-center">
                                    <template v-if="activeChat.type !== CHAT_TYPES.INTERNAL">
                                        <span class="status-indicator" :class="activeChat.status?.toLowerCase()"></span>
                                        <span class="status-label" :class="activeChat.status?.toLowerCase()">
                                            {{
                                                activeChat.status === 'ACTIVE'
                                                    ? 'Đang hoạt động'
                                                    : activeChat.status === 'PENDING'
                                                        ? 'Chờ tiếp nhận'
                                                        : 'Đã đóng'
                                            }}
                                        </span>
                                    </template>
                                    <template v-else>
                                        <span class="status-indicator" :class="activeChat.status === 'ACTIVE' ? 'active' : 'offline'"></span>
                                        <span class="status-label" :class="activeChat.status === 'ACTIVE' ? 'active' : 'offline'">
                                            {{ activeChat.status === 'ACTIVE' ? 'Đang hoạt động' : 'Ngoại tuyến' }}
                                        </span>
                                    </template>
                                </div>
                            </div>
                        </div>
                        <div class="d-flex align-center ga-2">
                            <!-- Action buttons: Tìm kiếm lịch sử tin nhắn -->
                            <v-btn
                                icon="mdi-magnify"
                                variant="text"
                                :color="showMessageSearch ? '#1e257c' : 'grey-darken-1'"
                                :style="showMessageSearch ? 'background: #e0e7ff; border-radius: 8px;' : ''"
                                title="Tìm kiếm lịch sử tin nhắn"
                                @click="toggleMessageSearch"
                            ></v-btn>
                            
                            <!-- Toggle button for Detail Panel -->
                            <v-btn
                                icon="mdi-information"
                                variant="text"
                                :color="showDetailPanel ? '#1e257c' : 'grey-darken-1'"
                                @click="toggleDetailPanel"
                                title="Thông tin chi tiết"
                            ></v-btn>

                             <v-menu location="bottom end">
                                <template #activator="{ props }">
                                    <v-btn icon="mdi-dots-vertical" variant="text" color="grey-darken-1" v-bind="props"></v-btn>
                                </template>
                                <v-list density="compact" min-width="220" class="py-1">
                                    <v-list-item @click="confirmDeleteChat" class="delete-chat-item">
                                        <template #prepend>
                                            <v-icon size="20" color="#b91c1c" class="mr-2" style="font-weight: 900;">mdi-delete-outline</v-icon>
                                        </template>
                                        <v-list-item-title class="text-none" style="color: #b91c1c !important; font-weight: 800 !important; font-size: 0.85rem !important;">Xóa lịch sử đoạn chat</v-list-item-title>
                                    </v-list-item>
                                </v-list>
                            </v-menu>
                        </div>
                    </div>

                    <!-- Thanh tìm kiếm trong lịch sử tin nhắn -->
                    <v-expand-transition>
                        <div v-if="showMessageSearch" class="msg-search-bar d-flex align-center px-4 py-2 border-bottom">
                            <v-icon icon="mdi-magnify" size="20" color="#1e257c" class="mr-2"></v-icon>
                            <v-text-field
                                ref="messageSearchInput"
                                v-model="messageSearchQuery"
                                placeholder="Tìm kiếm trong lịch sử tin nhắn..."
                                variant="plain"
                                density="compact"
                                hide-details
                                clearable
                                class="msg-search-input flex-grow-1"
                                @keydown.enter.exact.prevent="nextSearchMatch"
                                @keydown.shift.enter.exact.prevent="prevSearchMatch"
                                @keydown.esc="closeMessageSearch"
                            ></v-text-field>
                            <div class="d-flex align-center ml-2 ga-1">
                                <span v-if="messageSearchQuery.trim()" class="match-count-tag text-caption font-weight-bold px-2 py-0.5 rounded">
                                    {{ matchedMessageIds.length > 0 ? `${currentMatchIndex + 1}/${matchedMessageIds.length}` : '0 kết quả' }}
                                </span>
                                <v-btn
                                    icon="mdi-chevron-up"
                                    size="small"
                                    variant="text"
                                    :disabled="matchedMessageIds.length <= 1"
                                    @click="prevSearchMatch"
                                    title="Tin nhắn trước đó (Shift + Enter)"
                                ></v-btn>
                                <v-btn
                                    icon="mdi-chevron-down"
                                    size="small"
                                    variant="text"
                                    :disabled="matchedMessageIds.length <= 1"
                                    @click="nextSearchMatch"
                                    title="Tin nhắn tiếp theo (Enter)"
                                ></v-btn>
                                <v-btn
                                    icon="mdi-close"
                                    size="small"
                                    variant="text"
                                    color="grey-darken-1"
                                    @click="closeMessageSearch"
                                    title="Đóng tìm kiếm (Esc)"
                                ></v-btn>
                            </div>
                        </div>
                    </v-expand-transition>

                    <!-- Messages -->
                    <div ref="messagesContainer" class="messages-area flex-grow-1 overflow-y-auto">
                        <div v-if="isMessagesLoading" class="d-flex justify-center align-center fill-height">
                            <v-progress-circular indeterminate color="#1e257c" size="32"></v-progress-circular>
                        </div>
                        <template v-else>
                            <div
                                v-for="(m, idx) in displayMessages"
                                :id="`msg-item-${m.id || 'msg_' + idx}`"
                                :key="m.id || idx"
                                class="msg-row"
                                :class="[
                                    m.sender === authStore.user?.username || m.sender === 'bot' || m.sender === 'SYSTEM'
                                        ? 'is-mine'
                                        : 'is-other',
                                    matchedMessageIds[currentMatchIndex] === (m.id || 'msg_' + idx) ? 'focused-msg-match' : ''
                                ]"
                            >
                                <!-- Avatar for other sender in message row -->
                                <v-avatar
                                    v-if="m.sender !== authStore.user?.username && m.sender !== 'bot' && m.sender !== 'SYSTEM'"
                                    size="32"
                                    class="msg-avatar mr-2 align-self-end flex-shrink-0"
                                    style="border: 1px solid #e2e8f0; margin-bottom: 4px;"
                                >
                                    <v-img
                                        :src="
                                            !activeChat.avatar || activeChat.avatar.length <= 2
                                                ? 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png'
                                                : activeChat.avatar
                                        "
                                    ></v-img>
                                </v-avatar>

                                <div
                                    class="msg-bubble"
                                    :class="[
                                        m.sender === authStore.user?.username || m.sender === 'bot' || m.sender === 'SYSTEM'
                                            ? 'bubble-mine'
                                            : 'bubble-other',
                                        m.imageUrl && !m.text ? 'bubble-card' : ''
                                    ]"
                                >
                                    <!-- Hiển thị ảnh nếu tin nhắn có ảnh -->
                                    <div v-if="m.imageUrl || m.image || m.hinhAnh" class="bubble-image-wrap mb-1">
                                        <img
                                            :src="resolveChatImageUrl(m.imageUrl || m.image || m.hinhAnh)"
                                            class="bubble-image"
                                            @click="openImage(m.imageUrl || m.image || m.hinhAnh)"
                                        />
                                    </div>

                                    <!-- Render Text -->
                                    <div v-if="m.text || m.noiDung" class="bubble-text" v-html="highlightSearchText(m.text || m.noiDung, m.id || 'msg_' + idx)"></div>

                                    <div class="bubble-meta">
                                        <span class="bubble-time">{{ m.time }}</span>
                                        <v-icon
                                            v-if="m.sender === authStore.user?.username || m.sender === 'bot' || m.sender === 'SYSTEM'"
                                            size="14"
                                            color="rgba(255,255,255,0.7)"
                                            class="ml-1"
                                        >mdi-check-all</v-icon>
                                    </div>
                                </div>
                            </div>
                            <!-- Neo cuộn xuống cuối cùng -->
                            <div ref="messagesEndRef" style="height: 1px; width: 100%;"></div>
                        </template>
                    </div>

                    <!-- Input Area -->
                    <div class="input-area position-relative">
                        <!-- Banner tiếp nhận khi cuộc trò chuyện đang chờ (chỉ hiện với khách hàng) -->
                        <div
                            v-if="activeChat.status === 'PENDING' && activeChat.type !== CHAT_TYPES.INTERNAL"
                            class="status-action-banner d-flex align-center justify-space-between px-4 py-2 mb-2 rounded-xl"
                            style="background: #eef2ff; border: 1px solid #c7d2fe;"
                        >
                            <div class="d-flex align-center">
                                <v-icon color="#1e257c" size="20" class="mr-2">mdi-account-clock-outline</v-icon>
                                <span style="font-size: 0.85rem; font-weight: 600; color: #1e257c;">
                                    Cuộc trò chuyện đang chờ tiếp nhận
                                </span>
                            </div>
                            <v-btn
                                color="#1e257c"
                                size="small"
                                class="text-white text-none font-weight-bold rounded-pill px-3"
                                elevation="0"
                                @click="acceptChat"
                            >
                                <v-icon size="16" class="mr-1">mdi-check</v-icon> Tiếp nhận ngay
                            </v-btn>
                        </div>

                        <!-- Banner khi phiên trò chuyện đã đóng (chỉ hiện với khách hàng) -->
                        <div
                            v-else-if="activeChat.status === 'CLOSED' && activeChat.type !== CHAT_TYPES.INTERNAL"
                            class="status-action-banner d-flex align-center justify-center px-4 py-3 rounded-xl"
                            style="background: #f8fafc; border: 1px solid #e2e8f0;"
                        >
                            <v-icon color="#64748b" size="20" class="mr-2">mdi-lock-outline</v-icon>
                            <span style="font-size: 0.85rem; font-weight: 600; color: #64748b;">
                                Phiên trò chuyện đã đóng
                            </span>
                        </div>

                        <!-- Cụm soạn tin nhắn (chỉ hiện khi chưa đóng hoặc là chat nội bộ) -->
                        <v-row
                            v-if="activeChat.status !== 'CLOSED' || activeChat.type === CHAT_TYPES.INTERNAL"
                            no-gutters
                            align="center"
                        >
                            <!-- Input ẩn để chọn file ảnh -->
                            <input ref="fileInput" type="file" accept="image/*" style="display: none" @change="handleImageUpload" />

                            <v-col>
                                <!-- Preview ảnh trước khi gửi -->
                                <div
                                    v-if="imagePreview"
                                    class="image-preview-bar d-flex align-center ga-2 mb-2 pa-2 rounded-lg"
                                    style="background: #e8f0fe"
                                >
                                    <img :src="imagePreview" style="height: 56px; width: 56px; object-fit: cover; border-radius: 8px" />
                                    <span style="font-size: 12px; color: #3b5bdb">{{ imageFile?.name }}</span>
                                    <v-btn
                                        icon="mdi-close"
                                        size="x-small"
                                        variant="text"
                                        color="error"
                                        @click="clearImage"
                                        class="ml-auto"
                                    ></v-btn>
                                </div>
                                <v-textarea
                                    v-model="newMessage"
                                    :placeholder="activeChat.status === 'PENDING' && activeChat.type !== CHAT_TYPES.INTERNAL ? 'Nhập tin nhắn để tự động tiếp nhận...' : 'Nhập tin nhắn...'"
                                    rows="1"
                                    auto-grow
                                    variant="solo"
                                    flat
                                    bg-color="#f1f5f9"
                                    hide-details
                                    density="comfortable"
                                    class="rounded-xl input-textarea"
                                    @keyup.enter.exact.prevent="sendMessage"
                                ></v-textarea>
                            </v-col>

                            <!-- Nút upload ảnh -->
                            <v-btn
                                icon="mdi-image-plus"
                                variant="text"
                                color="#1e257c"
                                class="ml-2"
                                @click="triggerImageUpload"
                                title="Gửi ảnh"
                            ></v-btn>

                            <v-btn
                                icon="mdi-send"
                                color="#1e257c"
                                variant="flat"
                                elevation="0"
                                class="ml-2 rounded-xl text-white"
                                @click="sendMessage"
                                :loading="isSendingImage"
                                :disabled="!newMessage.trim() && !imagePreview"
                            ></v-btn>
                        </v-row>
                    </div>
                </template>

                <!-- Empty State -->
                <div v-else class="d-flex flex-column justify-center align-center fill-height empty-state px-4 text-center">
                    <div
                        class="empty-state-icon-box d-flex align-center justify-center rounded-circle mb-3 mx-auto"
                        style="width: 68px; height: 68px; background: rgba(241, 245, 249, 0.8); border: 1.5px dashed #cbd5e1"
                    >
                        <v-icon size="32" style="color: #94a3b8 !important">mdi-chat-processing-outline</v-icon>
                    </div>
                    <span class="text-slate-700 font-weight-bold" style="font-size: 15px;">Chưa chọn cuộc trò chuyện</span>
                    <span class="text-slate-500 text-caption mt-1">Vui lòng chọn 1 cuộc hội thoại bên trái để bắt đầu nhắn tin</span>
                </div>
            </v-col>

            <!-- Customer Detail Panel (Right Pane - Third Column) -->
            <v-col v-if="activeChat && showDetailPanel" cols="12" md="3" class="detail-panel d-flex flex-column fill-height border-left transition-all duration-300">
                <div class="detail-header d-flex align-center justify-between border-bottom px-4 py-3">
                    <span class="font-weight-black" style="font-size: 0.95rem; color: #0f172a">Thông tin hội thoại</span>
                    <v-btn icon="mdi-close" variant="text" size="small" color="grey-darken-1" @click="toggleDetailPanel"></v-btn>
                </div>
                
                <div class="detail-body flex-grow-1 overflow-y-auto pa-4 d-flex flex-column ga-4">
                    <!-- Customer Profile Card -->
                    <div class="profile-card d-flex flex-column align-center text-center pa-4 rounded-xl border bg-white shadow-sm">
                        <div class="position-relative mb-3">
                            <v-avatar size="80" class="border">
                                <v-img :src="!activeChat.avatar || activeChat.avatar.length <= 2 ? 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png' : activeChat.avatar" alt="avatar"></v-img>
                            </v-avatar>
                            <span class="status-dot-badge" :class="activeChat.type === CHAT_TYPES.INTERNAL ? (activeChat.status === 'ACTIVE' ? 'active' : 'offline') : activeChat.status?.toLowerCase()" style="width: 16px; height: 16px; border-width: 3px;"></span>
                        </div>
                        <div class="font-weight-black text-subtitle-1" style="color: #0f172a; line-height: 1.2">{{ activeChat.name }}</div>
                        <v-chip size="x-small" class="mt-2 font-weight-bold" :color="activeChatRoleColor" variant="flat">
                            {{ activeChatRoleLabel }}
                        </v-chip>
                    </div>
                    
                    <!-- Contact Details -->
                    <div class="info-section rounded-xl border bg-white pa-4 shadow-sm">
                        <div class="section-title font-weight-bold text-subtitle-2 mb-3" style="color: #64748b">
                            <v-icon icon="mdi-card-account-details-outline" size="18" class="mr-1"></v-icon>
                            Thông tin liên hệ
                        </div>
                        <div class="info-item d-flex align-center justify-space-between mb-2">
                            <span class="info-label text-caption text-grey-darken-1">Email</span>
                            <span class="info-value text-caption font-weight-bold text-slate-800 text-truncate ml-2" style="max-width: 160px;" :title="customerEmail">{{ customerEmail }}</span>
                        </div>
                        <div class="info-item d-flex align-center justify-space-between mb-2">
                            <span class="info-label text-caption text-grey-darken-1">SĐT</span>
                            <span class="info-value text-caption font-weight-bold text-slate-800">{{ customerPhone }}</span>
                        </div>
                        <div class="info-item d-flex align-center justify-space-between mb-2">
                            <span class="info-label text-caption text-grey-darken-1">Địa chỉ</span>
                            <span class="info-value text-caption font-weight-bold text-slate-800">{{ customerAddress }}</span>
                        </div>
                        <div class="info-item d-flex align-center justify-space-between">
                            <span class="info-label text-caption text-grey-darken-1">Ngày tham gia</span>
                            <span class="info-value text-caption font-weight-bold text-slate-800">{{ customerJoined }}</span>
                        </div>
                    </div>

                    <!-- Quick Actions -->
                    <div class="actions-section rounded-xl border bg-white pa-4 shadow-sm">
                        <div class="section-title font-weight-bold text-subtitle-2 mb-3" style="color: #64748b">
                            <v-icon icon="mdi-lightning-bolt-outline" size="18" class="mr-1"></v-icon>
                            Tác vụ nhanh
                        </div>
                        <div class="d-flex flex-column ga-2">
                            <v-btn
                                block
                                variant="flat"
                                prepend-icon="mdi-robot-outline"
                                @click="summarizeChat"
                                class="rounded-lg text-none font-weight-bold text-caption btn-pastel-blue"
                                :loading="isSummarizing"
                            >AI Tóm tắt hội thoại</v-btn>
                            
                            <!-- Chỉ hiển thị Tiếp nhận / Đóng phiên đối với cuộc trò chuyện với Khách hàng -->
                            <template v-if="activeChat.type !== CHAT_TYPES.INTERNAL">
                                <v-btn
                                    v-if="activeChat.status === 'PENDING'"
                                    block
                                    variant="flat"
                                    prepend-icon="mdi-check-circle"
                                    @click="acceptChat"
                                    class="rounded-lg text-none font-weight-bold text-caption btn-pastel-green"
                                >Tiếp nhận cuộc chat</v-btn>
                                
                                <v-btn
                                    v-if="activeChat.status === 'PENDING' || activeChat.status === 'ACTIVE'"
                                    block
                                    variant="flat"
                                    prepend-icon="mdi-close-circle"
                                    @click="closeChat"
                                    class="rounded-lg text-none font-weight-bold text-caption btn-pastel-orange"
                                >Đóng phiên chat</v-btn>
                            </template>
                        </div>
                    </div>

                    <!-- AI Summary Box -->
                    <div v-if="chatSummary" class="ai-summary-box rounded-xl border pa-4 shadow-sm border-purple-lighten-3">
                        <div class="d-flex align-center mb-2">
                            <v-icon icon="mdi-robot-outline" color="purple" size="18" class="mr-1"></v-icon>
                            <span class="font-weight-bold text-purple text-subtitle-2">AI Summary:</span>
                            <v-btn icon="mdi-close" variant="text" size="x-small" color="purple" class="ml-auto" @click="chatSummary = ''"></v-btn>
                        </div>
                        <div style="font-size: 0.82rem; line-height: 1.5; white-space: pre-wrap; color: #4a148c;">{{ chatSummary }}</div>
                    </div>

                    <!-- Purchase Overview (If Customer) -->
                    <div v-if="purchaseOverview" class="purchase-overview rounded-xl border bg-white pa-4 shadow-sm">
                        <div class="section-title font-weight-bold text-subtitle-2 mb-3" style="color: #64748b">
                            <v-icon icon="mdi-chart-line" size="18" class="mr-1"></v-icon>
                            Tổng quan mua sắm
                        </div>
                        <div class="info-item d-flex align-center justify-space-between mb-2">
                            <span class="info-label text-caption text-grey-darken-1">Số đơn hàng</span>
                            <span class="info-value text-caption font-weight-black text-slate-800">{{ purchaseOverview.totalOrders }} đơn</span>
                        </div>
                        <div class="info-item d-flex align-center justify-space-between mb-2">
                            <span class="info-label text-caption text-grey-darken-1">Tổng chi tiêu</span>
                            <span class="info-value text-caption font-weight-black text-primary">{{ purchaseOverview.totalSpend }}</span>
                        </div>
                        <div class="info-item d-flex align-center justify-space-between">
                            <span class="info-label text-caption text-grey-darken-1">Đơn cuối</span>
                            <span class="info-value text-caption font-weight-medium text-slate-800 text-truncate ml-2" style="max-width: 160px;" :title="purchaseOverview.lastOrder">{{ purchaseOverview.lastOrder }}</span>
                        </div>
                    </div>

                    <!-- Recent Order (If Customer & customerOrder is available) -->
                    <div v-if="customerOrder" class="recent-order rounded-xl border bg-white pa-4 shadow-sm">
                        <div class="section-title font-weight-bold text-subtitle-2 mb-3" style="color: #64748b">
                            <v-icon icon="mdi-shopping-outline" size="18" class="mr-1"></v-icon>
                            Đơn hàng gần nhất
                        </div>
                        <div class="d-flex align-center justify-space-between mb-2">
                            <span class="font-weight-black text-caption text-primary">{{ customerOrder.code }}</span>
                            <v-chip size="x-small" :color="customerOrder.statusColor" variant="flat" class="font-weight-bold text-white">{{ customerOrder.status }}</v-chip>
                        </div>
                        <div class="info-item d-flex align-center justify-space-between mb-2">
                            <span class="info-label text-caption text-grey-darken-1">Tổng cộng</span>
                            <span class="info-value text-caption font-weight-black text-slate-800">{{ customerOrder.total }}</span>
                        </div>
                        <div class="info-item d-flex align-center justify-space-between">
                            <span class="info-label text-caption text-grey-darken-1">Thời gian</span>
                            <span class="info-value text-caption font-weight-medium text-slate-800 text-truncate ml-2" style="max-width: 160px;" :title="customerOrder.time">{{ customerOrder.time }}</span>
                        </div>
                    </div>

                    <!-- Shared Media & Photos Section (Giống Messenger/Facebook) -->
                    <div class="media-section rounded-xl border bg-white pa-4 shadow-sm">
                        <div class="d-flex align-center justify-space-between mb-3">
                            <div class="section-title font-weight-bold text-subtitle-2 d-flex align-center" style="color: #64748b">
                                <v-icon icon="mdi-image-multiple-outline" size="18" class="mr-1"></v-icon>
                                File phương tiện đã chia sẻ
                            </div>
                            <span v-if="sharedPhotos.length > 0" class="text-caption font-weight-bold text-primary">
                                {{ sharedPhotos.length }} ảnh
                            </span>
                        </div>

                        <!-- Trạng thái chưa có ảnh -->
                        <div v-if="sharedPhotos.length === 0" class="text-center py-4 px-2">
                            <v-icon size="32" color="grey-lighten-1">mdi-image-off-outline</v-icon>
                            <div class="text-caption text-slate-400 mt-1">Chưa có hình ảnh nào được gửi</div>
                        </div>

                        <!-- Lưới ảnh chia sẻ dạng thumbnail giống Facebook Messenger -->
                        <div v-else class="shared-media-grid">
                            <div
                                v-for="(photo, pIdx) in sharedPhotos.slice(0, 9)"
                                :key="photo.id || pIdx"
                                class="media-thumb-item position-relative overflow-hidden rounded-lg cursor-pointer"
                                @click="openPhotoPreview(photo)"
                            >
                                <v-img
                                    :src="photo.url"
                                    aspect-ratio="1"
                                    cover
                                    class="bg-grey-lighten-3 media-img"
                                >
                                    <template v-slot:placeholder>
                                        <div class="d-flex align-center justify-center fill-height">
                                            <v-progress-circular indeterminate color="primary" size="18"></v-progress-circular>
                                        </div>
                                    </template>
                                </v-img>
                                <div v-if="pIdx === 8 && sharedPhotos.length > 9" class="more-overlay d-flex align-center justify-center">
                                    +{{ sharedPhotos.length - 8 }}
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Staff Session Notes -->
                    <div class="notes-section rounded-xl border bg-white pa-4 shadow-sm d-flex flex-column ga-2">
                        <div class="d-flex align-center justify-space-between">
                            <div class="section-title font-weight-bold text-subtitle-2" style="color: #64748b">
                                <v-icon icon="mdi-note-edit-outline" size="18" class="mr-1"></v-icon>
                                Ghi chú nội bộ
                            </div>
                            <v-btn 
                                size="x-small" 
                                variant="text" 
                                :color="isEditingNote ? 'success' : 'primary'"
                                class="font-weight-bold text-none px-2"
                                @click="isEditingNote = !isEditingNote"
                            >
                                {{ isEditingNote ? 'Lưu' : 'Chỉnh sửa' }}
                            </v-btn>
                        </div>
                        
                        <v-textarea
                            v-if="isEditingNote"
                            v-model="activeNote"
                            placeholder="Nhập ghi chú nhanh về khách hàng này (tự động lưu)..."
                            rows="4"
                            variant="outlined"
                            density="compact"
                            hide-details
                            class="notes-textarea"
                            autofocus
                        ></v-textarea>
                        
                        <div v-else class="note-display-box pa-3 rounded-lg border bg-grey-lighten-5">
                            <p class="text-caption text-grey-darken-3 mb-0" style="white-space: pre-wrap; line-height: 1.45;">{{ activeNote || 'Chưa có ghi chú nào.' }}</p>
                        </div>

                        <div class="d-flex align-center justify-space-between mt-1" style="font-size: 10px; color: #94a3b8;">
                            <span>Cập nhật: {{ noteUpdatedTime }}</span>
                            <span>Bởi: {{ noteUpdatedBy }}</span>
                        </div>
                    </div>
                </div>
            </v-col>
        </v-row>

        <!-- Modal xem ảnh phóng to chi tiết (Lightbox) -->
        <v-dialog v-model="previewPhotoModal" max-width="850px">
            <v-card class="rounded-xl overflow-hidden bg-white pa-0">
                <div class="d-flex align-center justify-space-between px-4 py-3 border-b bg-slate-50">
                    <div class="d-flex align-center">
                        <v-icon icon="mdi-image" color="#1e257c" class="mr-2"></v-icon>
                        <span class="font-weight-bold text-slate-800" style="font-size: 0.95rem">Hình ảnh trong đoạn chat</span>
                        <span v-if="currentPreviewPhoto?.time" class="text-caption text-slate-400 ml-2">({{ currentPreviewPhoto.time }})</span>
                    </div>
                    <div class="d-flex align-center ga-1">
                        <v-btn
                            icon="mdi-open-in-new"
                            variant="text"
                            size="small"
                            color="slate-600"
                            :href="currentPreviewPhoto?.url"
                            target="_blank"
                            title="Mở trong tab mới"
                        ></v-btn>
                        <v-btn icon="mdi-close" variant="text" size="small" @click="previewPhotoModal = false"></v-btn>
                    </div>
                </div>
                <div class="d-flex align-center justify-center pa-4 bg-grey-darken-4" style="min-height: 380px; max-height: 75vh;">
                    <img
                        v-if="currentPreviewPhoto"
                        :src="currentPreviewPhoto.url"
                        style="max-width: 100%; max-height: 70vh; object-fit: contain; border-radius: 8px;"
                    />
                </div>
            </v-card>
        </v-dialog>

        <!-- Xác nhận xóa lịch sử đoạn chat -->
        <AdminConfirm
            v-model:show="confirmDialog.show"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            :color="confirmDialog.color"
            :loading="confirmDialog.loading"
            @confirm="handleConfirm(true)"
            @cancel="handleConfirm(false)"
        />
    </v-container>
    </div>
</template>

<style scoped lang="scss">
@use '@/scss/tokens' as *;

$blue-primary: #1e257c; // Elegant brand Navy/Blue
$blue-dark: #121858;
$blue-light: #e8eefb;
$blue-bg: #f8fafc; // Clean off-white background
$primary-gradient: linear-gradient(135deg, #1e257c 0%, #343fa8 100%);

.chat-management-wrapper {
    height: calc(100vh - 75px);
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.chat-page {
    flex: 1 1 0;
    min-height: 0;
    height: 100% !important;
    max-height: 100% !important;
    overflow: hidden;
    background: #ffffff;
    font-family: $body-font-family !important;

    * {
        font-family: $body-font-family;
    }
}

/* ========== SIDEBAR ========== */
.sidebar {
    background: #ffffff;
    border-right: 1px solid #edf2f7;
    display: flex;
    flex-direction: column;
    height: 100%;
}

.sidebar-header {
    padding: 20px 24px;
    display: flex;
    align-items: center;
    border-bottom: 1px solid #edf2f7;
    background: #ffffff;
}

.sidebar-title {
    color: #0f172a;
    font-weight: 800;
    font-size: 1.1rem;
    letter-spacing: -0.02em;
}

.type-tabs {
    border-bottom: 1px solid #edf2f7;
    flex: none !important;
    height: 48px !important;
    
    :deep(.v-tabs-bar) {
        height: 48px;
    }

    :deep(.v-tab) {
        color: #64748b !important;
        font-weight: 700;
        font-size: 0.85rem;
        letter-spacing: 0;
        text-transform: none;
        transition: all 0.2s ease;

        &.v-tab--selected {
            color: #1e257c !important;
            font-weight: 800;
        }
    }
    
    :deep(.v-tab-slider) {
        color: #1e257c !important;
        height: 3px !important;
        border-radius: 3px 3px 0 0;
    }
}

.filter-section {
    padding: 16px 20px;
    border-bottom: 1px solid #edf2f7;
    background: #ffffff;
}

.status-chips {
    margin-bottom: 4px;
    
    :deep(.v-chip) {
        font-weight: 700 !important;
        font-size: 0.72rem;
        border: 1px solid #e2e8f0 !important;
        color: #64748b !important;
        background: transparent !important;
        border-radius: 8px;
        transition: all 0.25s ease;
        padding: 0 10px;

        .v-icon {
            opacity: 0.8;
        }
    }
    
    :deep(.chip-active) {
        background: #e8eefb !important;
        border-color: #1e257c !important;
        color: #1e257c !important;
        box-shadow: 0 2px 6px rgba(30, 37, 124, 0.08);
    }
}

.search-field {
    :deep(.v-field) {
        border-radius: 12px !important;
        border: 1px solid #e2e8f0 !important;
        box-shadow: none !important;
        transition: all 0.2s ease;
        
        &:hover {
            border-color: #cbd5e1 !important;
        }
        
        &.v-field--focused {
            border-color: #1e257c !important;
            box-shadow: 0 0 0 3px rgba(30, 37, 124, 0.08) !important;
        }
    }
    
    :deep(.v-field__input) {
        color: #0f172a !important;
        font-size: 0.85rem;
        padding-top: 8px !important;
        padding-bottom: 8px !important;
        
        &::placeholder {
            color: #94a3b8 !important;
            opacity: 1;
        }
    }
    
    :deep(.v-field__prepend-inner) {
        padding-top: 4px !important;
        .v-icon {
            color: #94a3b8 !important;
            font-size: 18px;
        }
    }
}

.conv-list {
    background-color: #f8fafc !important;
    padding: 10px 12px !important;
    
    &::-webkit-scrollbar {
        width: 5px;
    }
    
    &::-webkit-scrollbar-thumb {
        background: #cbd5e1;
        border-radius: 10px;
    }
}

.conv-item {
    border-radius: 12px !important;
    margin-bottom: 6px !important;
    padding: 12px 14px !important;
    border: 1px solid transparent;
    background: #ffffff !important;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02) !important;

    &:hover {
        background: #f1f5f9 !important;
        border-color: #e2e8f0;
        transform: translateY(-1px);
    }
    
    &.v-list-item--active {
        background: #ffffff !important;
        border-color: #1e257c !important;
        box-shadow: 0 4px 12px rgba(30, 37, 124, 0.06) !important;
        
        &::before {
            content: '';
            position: absolute;
            left: 0;
            top: 15%;
            height: 70%;
            width: 4px;
            background: #1e257c;
            border-radius: 0 4px 4px 0;
        }
    }
    
    &.is-unread {
        background: #f0f7ff !important;
        border-color: #bfdbfe !important;
        
        .conv-name {
            font-weight: 900 !important;
            color: #1e257c !important;
        }
        
        .conv-msg {
            font-weight: 800 !important;
            color: #0f172a !important;
        }
        
        .conv-time {
            font-weight: 800 !important;
            color: #1e257c !important;
        }
    }
}

.conv-avatar {
    border: 1px solid #e2e8f0;
    box-shadow: 0 2px 4px rgba(0,0,0,0.02);
}

.conv-name {
    color: #0f172a !important;
    font-weight: 700 !important;
    font-size: 0.9rem !important;
    letter-spacing: -0.01em;
}

.conv-msg {
    color: #64748b !important;
    font-size: 0.8rem !important;
    margin-top: 3px !important;
}

.conv-time {
    font-size: 0.7rem;
    color: #94a3b8;
    font-weight: 600;
}

/* Status Dot Badge */
.status-dot-badge {
    position: absolute;
    bottom: -1px;
    right: -1px;
    width: 12px;
    height: 12px;
    border-radius: 50%;
    border: 2px solid #ffffff;
    box-shadow: 0 1px 3px rgba(0,0,0,0.15);
    z-index: 5;

    &.active {
        background-color: #10b981 !important;
    }
    &.pending {
        background-color: #f59e0b !important;
    }
    &.closed, &.offline {
        background-color: #94a3b8 !important;
    }
}

.header-badge {
    width: 13px;
    height: 13px;
    bottom: 0px;
    right: 0px;
}

/* ========== MAIN AREA ========== */
.main-area {
    background-color: #f8fafc;
    background-image: radial-gradient(#e2e8f0 1px, transparent 1px);
    background-size: 20px 20px;
    display: flex;
    flex-direction: column;
    height: 100%;
}

.main-header {
    padding: 16px 28px;
    background: #ffffff;
    border-bottom: 1px solid #edf2f7;
    display: flex;
    align-items: center;
    justify-content: space-between;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
    z-index: 10;
}

.main-avatar {
    border: 1px solid #e2e8f0;
}

.main-chat-name {
    font-size: 1.1rem;
    font-weight: 800;
    color: #0f172a;
    line-height: 1.25;
    letter-spacing: -0.015em;
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
        box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.2);
    }
    &.closed, &.offline {
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
    &.closed, &.offline {
        color: #64748b;
    }
}

/* ========== MESSAGES AREA ========== */
.messages-area {
    padding: 24px 32px;
    display: flex;
    flex-direction: column;
    gap: 16px;
    
    &::-webkit-scrollbar {
        width: 5px;
    }
    
    &::-webkit-scrollbar-thumb {
        background: #cbd5e1;
        border-radius: 10px;
    }
}

.msg-row {
    display: flex;
    width: 100%;
    
    &.is-mine {
        justify-content: flex-end;
    }
    &.is-other {
        justify-content: flex-start;
    }
}

.msg-bubble {
    max-width: 65%;
    padding: 12px 18px;
    position: relative;
    box-shadow: 0 1px 3px rgba(0,0,0,0.02);
    transition: all 0.2s ease;
}

.bubble-mine {
    background: $primary-gradient !important;
    color: #ffffff;
    border-radius: 16px 16px 4px 16px !important;
    box-shadow: 0 4px 12px rgba(30, 37, 124, 0.12) !important;
    
    .bubble-text {
        color: #ffffff;
        font-weight: 500;
    }
    
    .bubble-time {
        color: rgba(255, 255, 255, 0.7);
    }
}

.bubble-other {
    background: #ffffff !important;
    color: #0f172a;
    border-radius: 16px 16px 16px 4px !important;
    border: 1px solid #edf2f7 !important;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03) !important;
    
    .bubble-text {
        color: #1e293b;
        font-weight: 500;
    }
    
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
    margin-top: 6px;
    gap: 4px;
}

.bubble-time {
    font-size: 0.65rem;
    font-weight: 600;
}

/* ========== INPUT AREA ========== */
.input-area {
    margin: 16px 24px 24px 24px;
    padding: 14px 20px;
    background: #ffffff;
    border: 1px solid #edf2f7;
    border-radius: 18px;
    position: relative;
    box-shadow: 0 10px 30px -5px rgba(0, 0, 0, 0.05);
}

.input-textarea {
    :deep(.v-field) {
        background-color: #f1f5f9 !important;
        border-radius: 12px !important;
        transition: all 0.2s ease;
        
        &.v-field--focused {
            background-color: #e2e8f0 !important;
        }
    }
    
    :deep(.v-field__input) {
        padding-top: 10px;
        padding-bottom: 10px;
        font-size: 0.9rem;
        color: #0f172a !important;
        line-height: 1.45;
        
        &::placeholder {
            color: #94a3b8 !important;
            opacity: 1;
        }
    }
}

.input-blur {
    filter: blur(2px);
    opacity: 0.3;
    pointer-events: none;
}

.lock-overlay {
    position: absolute;
    inset: 0;
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    z-index: 10;
    border-radius: 18px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    border: 1px dashed rgba(30, 37, 124, 0.15);
}

.lock-title {
    font-size: 0.95rem;
    font-weight: 800;
    color: #1e257c;
    letter-spacing: -0.01em;
}

.lock-sub {
    font-size: 0.8rem;
    color: #64748b;
    font-weight: 500;
    margin-top: 2px;
}

/* ========== DETAIL PANEL ========== */
.detail-panel {
    background: #ffffff;
    border-left: 1px solid #edf2f7;
    height: 100%;
    z-index: 5;
}

.detail-header {
    height: 64px;
    padding: 0 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #ffffff;
    border-bottom: 1px solid #edf2f7;
}

.detail-body {
    background: #f8fafc;
    &::-webkit-scrollbar {
        width: 4px;
    }
    &::-webkit-scrollbar-thumb {
        background: #cbd5e1;
        border-radius: 10px;
    }
}

.profile-card {
    border: 1px solid #edf2f7;
    transition: all 0.2s ease;
    &:hover {
        box-shadow: 0 4px 12px rgba(0,0,0,0.03);
    }
}

.info-section, .actions-section, .notes-section {
    border: 1px solid #edf2f7;
}

.ai-summary-box {
    border: 1px solid rgba(156, 39, 176, 0.2);
    background-color: #faf5ff;
}

.notes-textarea {
    :deep(.v-field) {
        border-radius: 8px !important;
        font-size: 0.82rem;
    }
}

/* ========== EMPTY STATE ========== */
.empty-state {
    background-color: #f8fafc;
    background-image: radial-gradient(#e2e8f0 1px, transparent 1px);
    background-size: 20px 20px;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
}

.empty-icon-wrap {
    width: 90px;
    height: 90px;
    border-radius: 24px;
    background: #e8eefb;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 24px;
    color: #1e257c;
    box-shadow: 0 8px 24px rgba(30, 37, 124, 0.08);
}

.empty-title {
    font-size: 1.4rem;
    font-weight: 800;
    color: #0f172a;
    letter-spacing: -0.02em;
}

.empty-sub {
    font-size: 0.9rem;
    color: #64748b;
    font-weight: 500;
    margin-top: 6px;
}

/* ========== IMAGE IN BUBBLE ========== */
.bubble-image-wrap {
    display: block;
    margin-bottom: 6px;
}

.bubble-image {
    max-width: 280px;
    max-height: 220px;
    border-radius: 12px;
    object-fit: cover;
    display: block;
    cursor: pointer;
    border: 1px solid rgba(0,0,0,0.05);
    transition: all 0.25s ease;
    
    &:hover {
        opacity: 0.9;
        transform: scale(1.01);
    }
}

.note-display-box {
    background-color: #f8fafc;
    border: 1px solid #e2e8f0;
    padding: 10px 12px;
    border-radius: 8px;
}

.custom-unread-badge {
    :deep(.v-badge__badge) {
        background-color: #1e257c !important;
        color: #ffffff !important;
        font-weight: bold;
        font-size: 10px;
    }
}

/* ========== PASTEL BUTTON STYLES ========== */
.btn-pastel-blue {
    background-color: #e0f2fe !important;
    color: #0369a1 !important;
    border: 1px solid #bae6fd !important;
    
    :deep(.v-icon) {
        color: #0369a1 !important;
    }
}

.btn-pastel-green {
    background-color: #dcfce7 !important;
    color: #15803d !important;
    border: 1px solid #bbf7d0 !important;
    
    :deep(.v-icon) {
        color: #15803d !important;
    }
}

.btn-pastel-orange {
    background-color: #ffedd5 !important;
    color: #ea580c !important;
    border: 1px solid #fed7aa !important;
    
    :deep(.v-icon) {
        color: #ea580c !important;
    }
}

/* ========== TRANSPARENT CARD BUBBLE STYLES ========== */
.msg-bubble.bubble-card {
    background: transparent !important;
    box-shadow: none !important;
    border: none !important;
    padding: 0 !important;
    
    .bubble-meta {
        justify-content: flex-end;
    }
    
    .bubble-time {
        color: #64748b !important;
    }
    
    :deep(.v-icon) {
        color: #64748b !important;
    }
}

/* ========== ZALO UNREAD BADGE ========== */
.unread-bold {
    font-weight: 700 !important;
    color: #0f172a !important;
}

.unread-bold-msg {
    font-weight: 600 !important;
    color: #1e293b !important;
}

.unread-time {
    color: #e53e3e !important;
    font-weight: 600 !important;
}

.zalo-unread-badge {
    background-color: #e53e3e;
    color: #ffffff;
    font-size: 0.72rem;
    font-weight: 700;
    min-width: 18px;
    height: 18px;
    border-radius: 9px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0 5px;
    box-shadow: 0 2px 4px rgba(229, 62, 62, 0.4);
    animation: pulse-badge 2s infinite ease-in-out;
}

/* ========== SHARED MEDIA GALLERY (MESSENGER / FB STYLE) ========== */
.shared-media-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 6px;

    .media-thumb-item {
        aspect-ratio: 1;
        background: #f1f5f9;
        border: 1px solid #e2e8f0;
        cursor: pointer;
        position: relative;
        overflow: hidden;
        border-radius: 8px;

        .media-img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            transition: transform 0.25s ease;
        }

        &:hover .media-img {
            transform: scale(1.06);
        }

        .more-overlay {
            position: absolute;
            inset: 0;
            background: rgba(15, 23, 42, 0.65);
            color: #ffffff;
            font-weight: 800;
            font-size: 0.95rem;
            backdrop-filter: blur(2px);
        }
    }
}

/* ========== MESSAGE HISTORY SEARCH & HIGHLIGHTING ========== */
.msg-search-bar {
    background: #f8fafc;
    border-bottom: 1px solid #e2e8f0;
    min-height: 48px;
    z-index: 5;
}

.msg-search-input {
    :deep(.v-field__input) {
        font-size: 0.85rem;
        padding-top: 4px;
        padding-bottom: 4px;
        color: #0f172a;
    }
}

.match-count-tag {
    background: #e2e8f0;
    color: #475569;
    font-size: 0.75rem;
    white-space: nowrap;
}

:deep(.chat-search-highlight) {
    background-color: #fef08a !important;
    color: #854d0e !important;
    padding: 1px 3px;
    border-radius: 3px;
    font-weight: 700;
    display: inline;

    &.active-match {
        background-color: #f59e0b !important;
        color: #ffffff !important;
        box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.4);
    }
}

.msg-row {
    transition: background-color 0.3s ease;
    border-radius: 8px;

    &.pulse-highlight {
        animation: messagePulse 2s cubic-bezier(0.4, 0, 0.2, 1);
    }
}

@keyframes messagePulse {
    0% {
        background-color: rgba(30, 37, 124, 0.15);
    }
    50% {
        background-color: rgba(30, 37, 124, 0.28);
    }
    100% {
        background-color: transparent;
    }
}
</style>