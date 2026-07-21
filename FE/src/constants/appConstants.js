/**
 * APP_ROLES - Các vai trò trong hệ thống AeroStride
 * Tương ứng với VaiTro.java trong Backend (kèm tiền tố ROLE_ của Spring Security)
 */
export const APP_ROLES = {
    ADMIN: 'ROLE_QUAN_TRI_VIEN',
    STAFF: 'ROLE_NHAN_VIEN',
    CUSTOMER: 'ROLE_KHACH_HANG'
};

/**
 * CHAT_TYPES - Các loại hội thoại chat
 */
export const CHAT_TYPES = {
    CUSTOMER: 'CUSTOMER',
    INTERNAL: 'INTERNAL'
};

/**
 * CHAT_SENDER_TYPE - Định danh người gửi tin nhắn
 */
export const CHAT_SENDER_TYPE = {
    CUSTOMER: 'customer',
    STAFF: 'staff',
    SYSTEM: 'system'
};

/**
 * CHAT_STATUS - Trạng thái của cuộc hội thoại
 */
export const CHAT_STATUS = {
    PENDING: 'PENDING',
    ACTIVE: 'ACTIVE',
    CLOSED: 'CLOSED'
};

/**
 * CHAT_TOPICS - Các topic WebSocket cho chat
 */
export const CHAT_TOPICS = {
    MESSAGES: '/topic/messages',
    NOTIFICATIONS: '/topic/notifications'
};

/**
 * COMMON_OPTIONS - Các tùy chọn dùng chung (Giới tính, Trạng thái...)
 */
export const GIOI_TINH_OPTIONS = [
    { title: 'Nam', value: true },
    { title: 'Nữ', value: false }
];

export const GIOI_TINH_FILTER_OPTIONS = [
    { title: 'Tất cả', value: null },
    ...GIOI_TINH_OPTIONS
];

/**
 * ORDER_TYPES - Các loại hình đơn hàng
 */
export const ORDER_TYPES = {
    ALL: null,
    ONLINE: 'ONLINE',
    TAI_QUAY: 'TAI_QUAY',
    OFFLINE: 'OFFLINE'
};

export const ORDER_TYPE_OPTIONS = [
    { title: 'Tất cả', value: null },
    { title: 'Trực tuyến', value: ORDER_TYPES.ONLINE },
    { title: 'Cửa hàng', value: ORDER_TYPES.TAI_QUAY }
];

