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
