<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import api from '@/services/apiService';
import { API_CHAT } from '@/constants/apiPaths';
import { chatSocket } from '@/services/chatSocket';
import { useNotificationStore } from '@/stores/notificationStore';
import { useAuthStore } from '@/stores/authStore';
import { CHAT_TYPES, CHAT_SENDER_TYPE, CHAT_STATUS, CHAT_TOPICS } from '@/constants/appConstants';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { AdminConfirm } from '@/components/common';

const notificationStore = useNotificationStore();
const authStore = useAuthStore();
const { confirmDialog, setConfirm, handleConfirm } = useConfirmDialog();
const customers = ref([]);
const activeChat = ref(null);
const chatMessages = ref([]);

// Trạng thái hiển thị panel thông tin chi tiết
const showDetailPanel = ref(true);
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
        // If it's the mock conversation and no notes are saved yet, pre-populate with the mockup note!
        if (isMockConversation(activeChat.value.id) && !customerNotes.value[activeChat.value.id]) {
            if (activeChat.value.id === 'MOCK_NGUYEN_MINH_ANH') {
                return 'Khách hàng thân thiết, hay mua giày sneaker. Ưu tiên tư vấn size và sản phẩm mới. Giao hàng nhanh khi có thể.';
            }
            if (activeChat.value.id === 'MOCK_TRAN_QUOC_HUY') {
                return 'Khách hàng hỏi size 42 Air Jordan. Quan tâm chính sách ship COD Hải Phòng.';
            }
            if (activeChat.value.id === 'MOCK_LE_THI_MAI') {
                return 'Khách hàng nữ mới, đang cần tư vấn giày hot trend.';
            }
            return 'Khách hàng giả lập hỗ trợ chăm sóc.';
        }
        return customerNotes.value[activeChat.value.id] || '';
    },
    set: (val) => {
        if (!activeChat.value) return;
        customerNotes.value[activeChat.value.id] = val;
        localStorage.setItem('chat_customer_notes', JSON.stringify(customerNotes.value));
    }
});

// Helper check for mock conversations
const isMockConversation = (id) => {
    return id && id.startsWith('MOCK_');
};

const isUnread = (c) => {
    if (!c) return false;
    if (isMockConversation(c.id)) {
        return c.unread > 0;
    }
    return notificationStore.unreadChatConvIds.includes(c.id);
};

const getUnreadCount = (c) => {
    if (!c) return 0;
    if (isMockConversation(c.id)) {
        return c.unread;
    }
    return notificationStore.unreadChatConvIds.includes(c.id) ? 1 : 0;
};

const sortedCustomers = computed(() => {
    return [...customers.value].sort((a, b) => {
        const aUnread = isUnread(a);
        const bUnread = isUnread(b);
        if (aUnread && !bUnread) return -1;
        if (!aUnread && bUnread) return 1;
        
        const timeA = a.timestamp || 0;
        const timeB = b.timestamp || 0;
        return timeB - timeA;
    });
});

// 5 Mock conversations definition for high fidelity rendering
const mockConversations = ref([
    {
        id: 'MOCK_NGUYEN_MINH_ANH',
        name: 'Nguyễn Minh Anh',
        avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150',
        type: CHAT_TYPES.CUSTOMER,
        status: 'ACTIVE',
        lastMsg: 'Dạ vâng, đơn hàng đã được tạo thành công. Shop sẽ xác nhận...',
        time: '10:28',
        unread: 0,
        isAccepted: true,
        timestamp: Date.now() - 5 * 60 * 1000
    },
    {
        id: 'MOCK_TRAN_QUOC_HUY',
        name: 'Trần Quốc Huy',
        avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150',
        type: CHAT_TYPES.CUSTOMER,
        status: 'ACTIVE',
        lastMsg: 'Shop có ship COD ra Hải Phòng không ạ?',
        time: '10:15',
        unread: 1,
        isAccepted: true,
        timestamp: Date.now() - 15 * 60 * 1000
    },
    {
        id: 'MOCK_LE_THI_MAI',
        name: 'Lê Thị Mai',
        avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150',
        type: CHAT_TYPES.CUSTOMER,
        status: 'PENDING',
        lastMsg: 'Tư vấn cho mình size giày nữ mẫu mới nhất với.',
        time: '09:40',
        unread: 1,
        isAccepted: false,
        timestamp: Date.now() - 50 * 60 * 1000
    },
    {
        id: 'MOCK_PHAM_MINH_DUC',
        name: 'Phạm Minh Đức',
        avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150',
        type: CHAT_TYPES.CUSTOMER,
        status: 'ACTIVE',
        lastMsg: 'Cảm ơn shop đã hỗ trợ nhiệt tình.',
        time: 'Hôm qua',
        unread: 1,
        isAccepted: true,
        timestamp: Date.now() - 24 * 60 * 60 * 1000
    },
    {
        id: 'MOCK_HOANG_THUY_LINH',
        name: 'Hoàng Thùy Linh',
        avatar: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150',
        type: CHAT_TYPES.CUSTOMER,
        status: 'CLOSED',
        lastMsg: 'Đã nhận được hàng.',
        time: '18/08',
        unread: 1,
        isAccepted: true,
        timestamp: Date.now() - 48 * 60 * 60 * 1000
    }
]);

const mockMessagesMap = ref({
    'MOCK_NGUYEN_MINH_ANH': [
        {
            id: 'msg1',
            conversationId: 'MOCK_NGUYEN_MINH_ANH',
            text: 'Shop ơi, mẫu Nike Air Force 1 này em mang size 38.5 thì nên chọn size nào ạ?',
            sender: 'Nguyễn Minh Anh',
            time: '10:22'
        },
        {
            id: 'msg2',
            conversationId: 'MOCK_NGUYEN_MINH_ANH',
            text: 'Chào anh/chị Minh Anh! Dạ mẫu này form chuẩn chị mang size 38.5 nhé. Nếu bàn chân hơi bè chị có thể chọn 39 để thoải mái hơn ạ.',
            sender: authStore.user?.username || 'STAFF',
            time: '10:23'
        },
        {
            id: 'msg3',
            conversationId: 'MOCK_NGUYEN_MINH_ANH',
            text: 'Dạ vâng, vậy cho em đặt size 39 màu trắng ạ. Shop còn hàng không?',
            sender: 'Nguyễn Minh Anh',
            time: '10:24'
        },
        {
            id: 'msg4',
            conversationId: 'MOCK_NGUYEN_MINH_ANH',
            text: '[PRODUCT:AF1]',
            sender: authStore.user?.username || 'STAFF',
            time: '10:25'
        },
        {
            id: 'msg5',
            conversationId: 'MOCK_NGUYEN_MINH_ANH',
            text: 'Dạ em đặt luôn 1 đôi ạ. Gửi về địa chỉ cũ giúp em nhé.',
            sender: 'Nguyễn Minh Anh',
            time: '10:26'
        },
        {
            id: 'msg6',
            conversationId: 'MOCK_NGUYEN_MINH_ANH',
            text: '[ORDER:SHO72638]',
            sender: authStore.user?.username || 'STAFF',
            time: '10:27'
        },
        {
            id: 'msg7',
            conversationId: 'MOCK_NGUYEN_MINH_ANH',
            text: 'Dạ vâng, đơn hàng đã được tạo thành công. Shop sẽ xác nhận và gửi hàng cho mình trong ngày hôm nay ạ! 🥰',
            sender: authStore.user?.username || 'STAFF',
            time: '10:28'
        }
    ],
    'MOCK_TRAN_QUOC_HUY': [
        {
            id: 'msg_h1',
            conversationId: 'MOCK_TRAN_QUOC_HUY',
            text: 'Chào shop, mình muốn hỏi đôi sneaker Air Jordan size 42 còn hàng không?',
            sender: 'Trần Quốc Huy',
            time: '10:10'
        },
        {
            id: 'msg_h2',
            conversationId: 'MOCK_TRAN_QUOC_HUY',
            text: 'Chào bạn Huy, mẫu Air Jordan 1 High bên mình còn sẵn size 42 nha.',
            sender: authStore.user?.username || 'STAFF',
            time: '10:12'
        },
        {
            id: 'msg_h3',
            conversationId: 'MOCK_TRAN_QUOC_HUY',
            text: 'Shop có ship COD ra Hải Phòng không ạ?',
            sender: 'Trần Quốc Huy',
            time: '10:15'
        }
    ],
    'MOCK_LE_THI_MAI': [
        {
            id: 'msg_m1',
            conversationId: 'MOCK_LE_THI_MAI',
            text: 'Tư vấn cho mình size giày nữ mẫu mới nhất với.',
            sender: 'Lê Thị Mai',
            time: '09:40'
        }
    ],
    'MOCK_PHAM_MINH_DUC': [
        {
            id: 'msg_d1',
            conversationId: 'MOCK_PHAM_MINH_DUC',
            text: 'Giày đi êm lắm shop ơi.',
            sender: 'Phạm Minh Đức',
            time: 'Hôm qua'
        },
        {
            id: 'msg_d2',
            conversationId: 'MOCK_PHAM_MINH_DUC',
            text: 'Dạ cảm ơn anh đã phản hồi tốt về sản phẩm ạ!',
            sender: authStore.user?.username || 'STAFF',
            time: 'Hôm qua'
        },
        {
            id: 'msg_d3',
            conversationId: 'MOCK_PHAM_MINH_DUC',
            text: 'Cảm ơn shop đã hỗ trợ nhiệt tình.',
            sender: 'Phạm Minh Đức',
            time: 'Hôm qua'
        }
    ],
    'MOCK_HOANG_THUY_LINH': [
        {
            id: 'msg_l1',
            conversationId: 'MOCK_HOANG_THUY_LINH',
            text: 'Gửi cho mình link tracking đơn hàng.',
            sender: 'Hoàng Thùy Linh',
            time: '17/08'
        },
        {
            id: 'msg_l2',
            conversationId: 'MOCK_HOANG_THUY_LINH',
            text: 'Dạ mã vận đơn của mình là VN20398402, shop gửi link theo dõi cho mình nhé.',
            sender: authStore.user?.username || 'STAFF',
            time: '17/08'
        },
        {
            id: 'msg_l3',
            conversationId: 'MOCK_HOANG_THUY_LINH',
            text: 'Đã nhận được hàng.',
            sender: 'Hoàng Thùy Linh',
            time: '18/08'
        }
    ]
});

// Giả lập Email và SĐT dựa trên dữ liệu khách hàng
const customerEmail = computed(() => {
    if (!activeChat.value) return '';
    if (isMockConversation(activeChat.value.id)) {
        if (activeChat.value.id === 'MOCK_NGUYEN_MINH_ANH') return 'minhanh.nguyen@gmail.com';
        if (activeChat.value.id === 'MOCK_TRAN_QUOC_HUY') return 'huy.tran@gmail.com';
        if (activeChat.value.id === 'MOCK_LE_THI_MAI') return 'mai.le@gmail.com';
        if (activeChat.value.id === 'MOCK_PHAM_MINH_DUC') return 'duc.pham@gmail.com';
        if (activeChat.value.id === 'MOCK_HOANG_THUY_LINH') return 'linh.hoang@gmail.com';
    }
    if (activeChat.value.email) return activeChat.value.email;
    const cleanName = activeChat.value.name
        .toLowerCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/\s+/g, '.');
    return `${cleanName}@aerostride.vn`;
});

const customerPhone = computed(() => {
    if (!activeChat.value) return '';
    if (isMockConversation(activeChat.value.id)) {
        if (activeChat.value.id === 'MOCK_NGUYEN_MINH_ANH') return '0987 654 321';
        if (activeChat.value.id === 'MOCK_TRAN_QUOC_HUY') return '0912 345 678';
        if (activeChat.value.id === 'MOCK_LE_THI_MAI') return '0903 111 222';
        if (activeChat.value.id === 'MOCK_PHAM_MINH_DUC') return '0945 888 999';
        if (activeChat.value.id === 'MOCK_HOANG_THUY_LINH') return '0978 444 555';
    }
    if (activeChat.value.phone) return activeChat.value.phone;
    const idHash = activeChat.value.id.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    return `098${(idHash % 9000000) + 1000000}`;
});

const customerAddress = computed(() => {
    if (!activeChat.value) return '';
    if (isMockConversation(activeChat.value.id)) {
        if (activeChat.value.id === 'MOCK_NGUYEN_MINH_ANH') return 'Hà Nội, Việt Nam';
        if (activeChat.value.id === 'MOCK_TRAN_QUOC_HUY') return 'Hải Phòng, Việt Nam';
        if (activeChat.value.id === 'MOCK_LE_THI_MAI') return 'Đà Nẵng, Việt Nam';
        if (activeChat.value.id === 'MOCK_PHAM_MINH_DUC') return 'TP. Hồ Chí Minh, Việt Nam';
        if (activeChat.value.id === 'MOCK_HOANG_THUY_LINH') return 'Cần Thơ, Việt Nam';
    }
    const hash = activeChat.value.id.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    const cities = ['Hà Nội, Việt Nam', 'TP. Hồ Chí Minh, Việt Nam', 'Đà Nẵng, Việt Nam', 'Hải Phòng, Việt Nam'];
    return cities[hash % cities.length];
});

const customerJoined = computed(() => {
    if (!activeChat.value) return '';
    if (isMockConversation(activeChat.value.id)) {
        if (activeChat.value.id === 'MOCK_NGUYEN_MINH_ANH') return '12/01/2023';
        if (activeChat.value.id === 'MOCK_TRAN_QUOC_HUY') return '24/05/2023';
        if (activeChat.value.id === 'MOCK_LE_THI_MAI') return '08/09/2023';
        if (activeChat.value.id === 'MOCK_PHAM_MINH_DUC') return '15/12/2022';
        if (activeChat.value.id === 'MOCK_HOANG_THUY_LINH') return '30/03/2024';
    }
    const hash = activeChat.value.id.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    const years = [2021, 2022, 2023, 2024];
    const month = String((hash % 12) + 1).padStart(2, '0');
    const day = String((hash % 28) + 1).padStart(2, '0');
    return `${day}/${month}/${years[hash % years.length]}`;
});

const activeChatRoleLabel = computed(() => {
    if (!activeChat.value) return '';
    if (activeChat.value.type === CHAT_TYPES.CUSTOMER) return 'Khách hàng';
    
    // Check role from various properties
    const roleField = activeChat.value.role || activeChat.value.roleCode || activeChat.value.roleName || activeChat.value.chucVu || activeChat.value.authority;
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
    if (isMockConversation(activeChat.value.id)) {
        if (activeChat.value.id === 'MOCK_NGUYEN_MINH_ANH') {
            return {
                code: '#SHO72638',
                status: 'Chờ xác nhận',
                statusColor: 'amber-darken-2',
                total: '2.490.000đ',
                time: '10:27 - 24/05/2024'
            };
        }
        if (activeChat.value.id === 'MOCK_TRAN_QUOC_HUY') {
            return {
                code: '#SHO91024',
                status: 'Chờ xác nhận',
                statusColor: 'amber-darken-2',
                total: '3.150.000đ',
                time: '10:15 - 19/08/2026'
            };
        }
        if (activeChat.value.id === 'MOCK_LE_THI_MAI') {
            return null; // Chưa có đơn hàng
        }
        if (activeChat.value.id === 'MOCK_PHAM_MINH_DUC') {
            return {
                code: '#SHO88310',
                status: 'Đang giao',
                statusColor: 'info',
                total: '1.890.000đ',
                time: '14:20 - 18/08/2026'
            };
        }
        if (activeChat.value.id === 'MOCK_HOANG_THUY_LINH') {
            return {
                code: '#SHO55291',
                status: 'Đã hoàn thành',
                statusColor: 'success',
                total: '1.250.000đ',
                time: '09:05 - 18/08/2026'
            };
        }
    }
    const hash = activeChat.value.id.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
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
    if (isMockConversation(activeChat.value.id)) {
        if (activeChat.value.id === 'MOCK_NGUYEN_MINH_ANH') {
            return {
                totalOrders: 12,
                totalSpend: '18.750.000đ',
                lastOrder: '#SHO72638 - 24/05/2024'
            };
        }
        if (activeChat.value.id === 'MOCK_TRAN_QUOC_HUY') {
            return {
                totalOrders: 4,
                totalSpend: '8.400.000đ',
                lastOrder: '#SHO91024 - 19/08/2026'
            };
        }
        if (activeChat.value.id === 'MOCK_LE_THI_MAI') {
            return {
                totalOrders: 0,
                totalSpend: '0đ',
                lastOrder: 'Chưa mua hàng'
            };
        }
        if (activeChat.value.id === 'MOCK_PHAM_MINH_DUC') {
            return {
                totalOrders: 18,
                totalSpend: '34.200.000đ',
                lastOrder: '#SHO88310 - 18/08/2026'
            };
        }
        if (activeChat.value.id === 'MOCK_HOANG_THUY_LINH') {
            return {
                totalOrders: 1,
                totalSpend: '1.250.000đ',
                lastOrder: '#SHO55291 - 18/08/2026'
            };
        }
    }
    const hash = activeChat.value.id.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
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

// Parse message to detect inline attachments
const parseMessage = (m) => {
    const text = m.text || '';
    if (text.includes('[PRODUCT:AF1]') || (text.includes('Nike Air Force 1') && text.includes('CW2288-111'))) {
        return {
            type: 'product',
            name: "Nike Air Force 1 '07 White",
            code: "CW2288-111",
            specs: "Màu sắc: Trắng | Size: 39",
            price: "2.490.000đ",
            status: "Còn hàng",
            imageUrl: "https://static.nike.com/a/images/t_PDP_1728_v1/f_auto,q_auto:eco/b7d3c21a-4668-450f-b47e-d3d66257474f/air-force-1-07-shoes-Wr0Sp1.png"
        };
    }
    if (text.includes('[ORDER:SHO72638]') || text.includes('Đơn hàng #SHO72638')) {
        return {
            type: 'order',
            code: "#SHO72638",
            status: "Chờ xác nhận",
            total: "2.490.000đ",
            payment: "COD"
        };
    }
    return {
        type: 'text',
        text: text
    };
};

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
const openImage = (url) => {
    if (url) window.open(url, '_blank');
};

// Filters
const chatType = ref(CHAT_TYPES.CUSTOMER);
const chatStatus = ref('ALL');
const searchQuery = ref('');

const scrollToBottom = () => {
    setTimeout(() => {
        if (messagesContainer.value) {
            messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
        }
    }, 100);
};

const stats = ref({ ACTIVE: 0, PENDING: 0, CLOSED: 0 });

const activeCount = computed(() => stats.value.ACTIVE || 0);
const pendingCount = computed(() => stats.value.PENDING || 0);
const closedCount = computed(() => stats.value.CLOSED || 0);



// Lấy danh sách hội thoại từ Backend
const fetchConversations = async (quiet = false) => {
    if (!quiet && customers.value.length === 0) {
        isLoading.value = true;
    }
    try {
        const [convRes, statsRes] = await Promise.all([
            api.get(API_CHAT.CONVERSATIONS, {
                params: {
                    type: chatType.value,
                    status: chatStatus.value === 'ALL' ? undefined : chatStatus.value,
                    search: searchQuery.value
                }
            }),
            api.get(API_CHAT.CONVERSATIONS + '/stats')
        ]);

        let backendConvs = convRes.data?.data || [];
        
        // Remove duplicates if backend returns any name matching the mock names
        const mockNames = mockConversations.value.map(c => c.name);
        backendConvs = backendConvs.filter((c) => !mockNames.includes(c.name));

        // Filter and matches for our mock conversations
        const matchedMockConvs = mockConversations.value.filter(c => {
            const matchesType = chatType.value === c.type;
            const matchesStatus = chatStatus.value === 'ALL' || chatStatus.value === c.status;
            const matchesSearch = !searchQuery.value || c.name.toLowerCase().includes(searchQuery.value.toLowerCase());
            return matchesType && matchesStatus && matchesSearch;
        });

        customers.value = [...matchedMockConvs, ...backendConvs];

        stats.value = statsRes.data?.data || { ACTIVE: 0, PENDING: 0, CLOSED: 0 };

        // Adjust stats to include mock conversations count
        mockConversations.value.forEach(c => {
            if (c.type === chatType.value) {
                if (c.status === 'ACTIVE') stats.value.ACTIVE = (stats.value.ACTIVE || 0) + 1;
                else if (c.status === 'PENDING') stats.value.PENDING = (stats.value.PENDING || 0) + 1;
                else if (c.status === 'CLOSED') stats.value.CLOSED = (stats.value.CLOSED || 0) + 1;
            }
        });

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

watch([chatType, chatStatus, searchQuery], () => {
    fetchConversations();
});

// Lấy lịch sử tin nhắn của hội thoại đang chọn
const fetchMessages = async (conversationId) => {
    if (isMockConversation(conversationId)) {
        isMessagesLoading.value = true;
        setTimeout(() => {
            chatMessages.value = [...(mockMessagesMap.value[conversationId] || [])];
            isMessagesLoading.value = false;
            scrollToBottom();
        }, 150);
        return;
    }

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
    if (!newMessage.value.trim() && !imagePreview.value) return;
    if (!activeChat.value) return;
    // Phiên đã đóng thì khóa chat, không cho gửi
    if (activeChat.value.status === 'CLOSED') return;

    if (isMockConversation(activeChat.value.id)) {
        const newMsg = {
            id: 'msg_new_' + Date.now(),
            conversationId: activeChat.value.id,
            text: newMessage.value,
            sender: authStore.user?.username || 'STAFF',
            time: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
        };
        if (!mockMessagesMap.value[activeChat.value.id]) {
            mockMessagesMap.value[activeChat.value.id] = [];
        }
        mockMessagesMap.value[activeChat.value.id].push(newMsg);
        chatMessages.value.push(newMsg);
        
        // Cập nhật tin nhắn cuối cùng trong danh sách
        const found = mockConversations.value.find(c => c.id === activeChat.value.id);
        if (found) {
            found.lastMsg = newMessage.value;
            found.time = newMsg.time;
            found.timestamp = Date.now();
        }

        newMessage.value = '';
        scrollToBottom();
        return;
    }

    // Chuẩn bị payload: base64 thuần (bỏ header "data:image/...;base64," nếu có)
    let base64Image = null;
    if (imagePreview.value) {
        base64Image = imagePreview.value.includes(',') ? imagePreview.value.split(',')[1] : imagePreview.value;
    }

    const messageData = {
        conversationId: activeChat.value.id,
        text: newMessage.value || null,
        sender: authStore.user?.username || 'STAFF',
        imageBase64: base64Image
    };

    isSendingImage.value = !!base64Image;
    try {
        await api.post(API_CHAT.SEND, messageData);
        newMessage.value = '';
        clearImage();
        scrollToBottom();
        fetchConversations(true);
    } catch (error) {
        console.error('Lỗi khi gửi tin nhắn:', error);
    } finally {
        isSendingImage.value = false;
    }
};

const selectChat = (customer) => {
    activeChat.value = customer;
    isAccepted.value = customer.isAccepted || false;
    notificationStore.markChatRead(customer.id);
    if (isMockConversation(customer.id)) {
        customer.unread = 0;
        const found = mockConversations.value.find(c => c.id === customer.id);
        if (found) found.unread = 0;
    }
    fetchMessages(customer.id);
};

const acceptChat = async () => {
    if (!activeChat.value) return;

    if (isMockConversation(activeChat.value.id)) {
        activeChat.value.status = 'ACTIVE';
        activeChat.value.isAccepted = true;
        isAccepted.value = true;
        const found = mockConversations.value.find(c => c.id === activeChat.value.id);
        if (found) {
            found.status = 'ACTIVE';
            found.isAccepted = true;
        }
        return;
    }

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

    if (isMockConversation(activeChat.value.id)) {
        activeChat.value.status = 'CLOSED';
        const found = mockConversations.value.find(c => c.id === activeChat.value.id);
        if (found) {
            found.status = 'CLOSED';
        }
        return;
    }

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
                // Bỏ chọn nếu đang mở đúng cuộc vừa xóa
                if (activeChat.value && activeChat.value.id === target.id) {
                    activeChat.value = null;
                    chatMessages.value = [];
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
        chatSocket.subscribe(CHAT_TOPICS.NOTIFICATIONS, () => {
            fetchConversations(true);
        });

        chatSocket.subscribe(CHAT_TOPICS.MESSAGES, (msg) => {
            const data = typeof msg === 'string' ? JSON.parse(msg) : msg;
            if (!data) return;

            const currentUsername = authStore.user?.username;
            const isAdmin = authStore.isAdmin;
            const isMyChat =
                isAdmin ||
                !data.staffId ||
                data.staffId === currentUsername ||
                data.secondStaffId === currentUsername;

            if (!isMyChat) return;

            const isCurrentActive =
                activeChat.value &&
                (activeChat.value.id === data.conversationId ||
                    (activeChat.value.id.startsWith('NEW_INTERNAL_') &&
                        (activeChat.value.id.includes(data.staffId) || activeChat.value.id.includes(data.secondStaffId))));

            if (isCurrentActive) {
                if (activeChat.value.id.startsWith('NEW_INTERNAL_')) {
                    activeChat.value.id = data.conversationId;
                }
                if (!chatMessages.value.find((m) => m.id === data.id)) {
                    chatMessages.value.push(data);
                    scrollToBottom();
                }
                notificationStore.markChatRead(data.conversationId);
                const conv = customers.value.find(c => c.id === data.conversationId);
                if (conv) conv.timestamp = Date.now();
            } else if (data.sender !== currentUsername) {
                notificationStore.incrementUnreadChat(data.conversationId);
                const conv = customers.value.find(c => c.id === data.conversationId);
                if (conv) conv.timestamp = Date.now();
            }

            fetchConversations(true);
        });
    });
});
</script>

<template>
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
                    </v-tab>
                    <v-tab :value="CHAT_TYPES.INTERNAL">
                        <v-icon icon="mdi-account-group" size="18" class="mr-1"></v-icon>
                        Nội bộ
                    </v-tab>
                </v-tabs>

                <!-- Status Filters & Search -->
                <div class="filter-section">
                    <div class="d-flex align-center ga-2 mt-2">
                        <v-text-field
                            v-model="searchQuery"
                            prepend-inner-icon="mdi-magnify"
                            placeholder="Tìm kiếm khách hàng, nội dung..."
                            variant="solo"
                            flat
                            density="compact"
                            bg-color="#f1f5f9"
                            hide-details
                            class="search-field flex-grow-1"
                        ></v-text-field>
                        <v-btn icon="mdi-filter-variant" variant="outlined" color="#64748b" class="rounded-lg" size="small" style="height: 40px; width: 40px; border-color: #e2e8f0;"></v-btn>
                    </div>

                    <v-chip-group v-model="chatStatus" mandatory selected-class="chip-active" class="status-chips mt-3">
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
                                <span class="status-dot-badge" :class="c.status?.toLowerCase()"></span>
                            </div>
                        </template>
                        <v-list-item-title class="conv-name">{{ c.name }}</v-list-item-title>
                        <v-list-item-subtitle class="conv-msg">{{ c.lastMsg || 'Bắt đầu trò chuyện...' }}</v-list-item-subtitle>
                        <template v-slot:append>
                            <div class="d-flex flex-column align-end">
                                <span class="conv-time">{{ c.time }}</span>
                                <v-badge v-if="isUnread(c)" :content="getUnreadCount(c)" color="#1e257c" inline class="mt-1 custom-unread-badge"></v-badge>
                            </div>
                        </template>
                    </v-list-item>

                    <div v-if="sortedCustomers.length === 0" class="text-center py-16 px-4">
                        <v-icon size="48" color="grey-lighten-1">mdi-message-off-outline</v-icon>
                        <div class="mt-3" style="color: #64748b">Không có cuộc trò chuyện nào</div>
                        <div class="text-caption" style="color: #94a3b8">Hãy thử thay đổi bộ lọc</div>
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
                                <span class="status-dot-badge header-badge" :class="activeChat.status?.toLowerCase()"></span>
                            </div>
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
                            <!-- Action buttons -->
                            <v-btn icon="mdi-magnify" variant="text" color="grey-darken-1" title="Tìm kiếm"></v-btn>
                            
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

                    <!-- Messages -->
                    <div ref="messagesContainer" class="messages-area flex-grow-1 overflow-y-auto">
                        <div v-if="isMessagesLoading" class="d-flex justify-center align-center fill-height">
                            <v-progress-circular indeterminate color="#1e257c" size="32"></v-progress-circular>
                        </div>
                        <template v-else>
                            <div
                                v-for="(m, idx) in displayMessages"
                                :key="m.id || idx"
                                class="msg-row"
                                :class="
                                    m.sender === authStore.user?.username || m.sender === 'bot' || m.sender === 'SYSTEM'
                                        ? 'is-mine'
                                        : 'is-other'
                                "
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
                                        (parseMessage(m).type === 'product' || parseMessage(m).type === 'order' || (m.imageUrl && !m.text))
                                            ? 'bubble-card'
                                            : ''
                                    ]"
                                >
                                    <!-- Render Product Card -->
                                    <div v-if="parseMessage(m).type === 'product'" class="bubble-product-card mb-1">
                                        <div class="d-flex ga-3">
                                            <v-img :src="parseMessage(m).imageUrl" width="70" height="70" class="rounded-lg bg-grey-lighten-4 flex-shrink-0" cover></v-img>
                                            <div class="flex-grow-1 d-flex flex-column justify-space-between">
                                                <div>
                                                    <div class="d-flex align-center justify-space-between ga-2">
                                                        <span class="product-card-name font-weight-bold text-caption text-truncate" style="max-width: 130px; display: inline-block;">{{ parseMessage(m).name }}</span>
                                                        <v-chip size="x-small" color="success" variant="flat" class="font-weight-black flex-shrink-0 px-1 py-0" style="height: 16px; font-size: 8px;">{{ parseMessage(m).status }}</v-chip>
                                                    </div>
                                                    <div class="text-caption text-grey-darken-1" style="font-size: 10px !important;">{{ parseMessage(m).code }}</div>
                                                    <div class="text-caption text-grey-darken-2 mt-0.5" style="font-size: 11px !important;">{{ parseMessage(m).specs }}</div>
                                                </div>
                                                <div class="d-flex align-center justify-end">
                                                    <span class="product-card-price font-weight-black text-caption" style="color: #1e257c;">{{ parseMessage(m).price }}</span>
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    <!-- Render Order Card -->
                                    <div v-else-if="parseMessage(m).type === 'order'" class="bubble-order-card mb-1">
                                        <div class="d-flex align-center ga-3 mb-2">
                                            <div class="order-icon-wrap rounded-lg d-flex align-center justify-center bg-blue-lighten-5" style="width: 36px; height: 36px; flex-shrink: 0;">
                                                <v-icon color="#1e257c" size="20">mdi-shopping</v-icon>
                                            </div>
                                            <div class="flex-grow-1">
                                                <div class="d-flex align-center justify-space-between ga-2">
                                                    <span class="order-card-title font-weight-black text-caption">Đơn hàng {{ parseMessage(m).code }}</span>
                                                    <v-chip size="x-small" color="amber-darken-2" variant="flat" class="font-weight-black flex-shrink-0 px-1" style="height: 16px; font-size: 8px;">{{ parseMessage(m).status }}</v-chip>
                                                </div>
                                                <div class="text-caption text-grey-darken-1" style="font-size: 11px !important; margin-top: 2px;">Tạm tính: {{ parseMessage(m).total }}</div>
                                            </div>
                                        </div>
                                        <v-btn block size="x-small" variant="outlined" color="#1e257c" class="rounded-lg text-none font-weight-bold text-caption py-1" style="height: 24px;">Xem chi tiết đơn hàng</v-btn>
                                    </div>

                                    <!-- Render Text/Image -->
                                    <template v-else>
                                        <div v-if="m.imageUrl" class="bubble-image-wrap mb-1">
                                            <img :src="m.imageUrl" class="bubble-image" @click="openImage(m.imageUrl)" />
                                        </div>
                                        <div v-if="m.text" class="bubble-text">{{ m.text }}</div>
                                    </template>

                                    <div class="bubble-meta">
                                        <span class="bubble-time">{{ m.time }}</span>
                                        <v-icon
                                            v-if="m.sender === authStore.user?.username || m.sender === 'bot' || m.sender === 'SYSTEM'"
                                            size="14"
                                            :color="(parseMessage(m).type === 'product' || parseMessage(m).type === 'order' || (m.imageUrl && !m.text)) ? 'grey-darken-1' : 'rgba(255,255,255,0.7)'"
                                            class="ml-1"
                                            >mdi-check-all</v-icon>
                                    </div>
                                </div>
                            </div>
                        </template>
                    </div>

                    <!-- Input Area -->
                    <div class="input-area">
                        <div v-if="activeChat.status === 'PENDING'" class="lock-overlay">
                            <v-icon color="#1e257c" size="32" class="mb-2">mdi-shield-lock-outline</v-icon>
                            <div class="lock-title">Vui lòng tiếp nhận cuộc trò chuyện</div>
                            <div class="lock-sub">Bạn cần tiếp nhận để bắt đầu gửi tin nhắn</div>
                        </div>

                        <div v-else-if="activeChat.status === 'CLOSED'" class="lock-overlay">
                            <v-icon color="#94a3b8" size="32" class="mb-2">mdi-lock-outline</v-icon>
                            <div class="lock-title">Phiên trò chuyện đã đóng</div>
                            <div class="lock-sub">Cuộc trò chuyện đã kết thúc, không thể gửi tin nhắn</div>
                        </div>

                        <v-row
                            no-gutters
                            align="center"
                            :class="{ 'input-blur': activeChat.status === 'PENDING' || activeChat.status === 'CLOSED' }"
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
                                    placeholder="Nhập tin nhắn..."
                                    rows="1"
                                    auto-grow
                                    variant="solo"
                                    flat
                                    bg-color="#f1f5f9"
                                    hide-details
                                    density="comfortable"
                                    class="rounded-xl input-textarea"
                                    @keyup.enter.exact.prevent="sendMessage"
                                    :disabled="activeChat.status === 'PENDING' || activeChat.status === 'CLOSED'"
                                ></v-textarea>
                            </v-col>

                            <!-- Nút upload ảnh -->
                            <v-btn
                                icon="mdi-image-plus"
                                variant="text"
                                color="#1e257c"
                                class="ml-2"
                                @click="triggerImageUpload"
                                :disabled="activeChat.status === 'PENDING' || activeChat.status === 'CLOSED'"
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
                                :disabled="
                                    (!newMessage.trim() && !imagePreview) ||
                                    activeChat.status === 'PENDING' ||
                                    activeChat.status === 'CLOSED'
                                "
                            ></v-btn>
                        </v-row>
                    </div>
                </template>

                <!-- Empty State -->
                <div v-else class="d-flex flex-column justify-center align-center fill-height empty-state">
                    <div class="empty-icon-wrap mb-4">
                        <v-icon size="72" color="#1e257c">mdi-chat-processing-outline</v-icon>
                    </div>
                    <div class="empty-sub text-muted font-weight-medium" style="font-size: 0.95rem; color: #64748b;">Vui lòng chọn 1 cuộc hội thoại bên trái để bắt đầu</div>
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
                            <span class="status-dot-badge" :class="activeChat.status?.toLowerCase()" style="width: 16px; height: 16px; border-width: 3px;"></span>
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
</template>

<style scoped lang="scss">
@use '@/scss/tokens' as *;

$blue-primary: #1e257c; // Elegant brand Navy/Blue
$blue-dark: #121858;
$blue-light: #e8eefb;
$blue-bg: #f8fafc; // Clean off-white background
$primary-gradient: linear-gradient(135deg, #1e257c 0%, #343fa8 100%);

.chat-page {
    height: calc(100vh - 64px);
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
    &.closed {
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

/* ========== ATTACHMENTS & MOCKUP CUSTOMS ========== */
.bubble-product-card {
    background: #ffffff;
    border: 1px solid #edf2f7;
    border-radius: 12px;
    padding: 10px;
    margin-top: 4px;
    margin-bottom: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
    min-width: 240px;
    max-width: 280px;

    .product-card-name {
        color: #0f172a;
        font-weight: 700;
    }
    
    .product-card-price {
        font-size: 0.85rem;
        letter-spacing: -0.01em;
    }
}

.bubble-order-card {
    background: #ffffff;
    border: 1px solid #edf2f7;
    border-radius: 12px;
    padding: 12px;
    margin-top: 4px;
    margin-bottom: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
    min-width: 240px;
    max-width: 280px;
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
</style>
