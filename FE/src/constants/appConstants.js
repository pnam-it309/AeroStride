/**
 * APP_ROLES - Các vai trò trong hệ thống AeroStride
 * Tương ứng với VaiTro.java trong Backend (kèm tiền tố ROLE_ của Spring Security)
 */
export const APP_ROLES = {
    ADMIN: 'ROLE_QUAN_LY',
    STAFF: 'ROLE_NHAN_VIEN',
    CUSTOMER: 'ROLE_KHACH_HANG'
};

/**
 * ROLE_CODES - Mã phân quyền lưu trữ trong cơ sở dữ liệu (bảng phan_quyen)
 */
export const ROLE_CODES = {
    QUAN_LY: 'QUAN_LY',
    NHAN_VIEN: 'NHAN_VIEN',
    KHACH_HANG: 'KHACH_HANG'
};

/**
 * Helper dùng chung kiểm tra nhân viên hoặc đối tượng phân quyền thuộc nhóm Quản lý / Admin
 */
export const isManagementRole = (item) => {
    if (!item) return false;
    const ma = item?.phanQuyen?.ma || item?.maPhanQuyen || item?.ma || '';
    const ten = item?.phanQuyen?.ten || item?.tenPhanQuyen || item?.ten || '';
    const quyen = item?.phanQuyen?.quyenHan || item?.quyenHan || item?.quyen || '';

    const upperMa = String(ma).toUpperCase();
    if (upperMa.includes(ROLE_CODES.QUAN_LY) || upperMa.includes('ADMIN') || upperMa.includes('MANAGER') || upperMa.includes('QUAN_TRI')) {
        return true;
    }
    const upperQuyen = String(quyen).toUpperCase();
    if (['FULL_ACCESS', 'MANAGEMENT_ACCESS', 'MANAGER_ACCESS'].includes(upperQuyen)) {
        return true;
    }
    const lowerTen = String(ten).toLowerCase();
    return lowerTen.includes('quản lý') || lowerTen.includes('quản trị') || lowerTen.includes('admin') || lowerTen.includes('manager');
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

export const GIOI_TINH_FILTER_OPTIONS = [{ title: 'Tất cả', value: null }, ...GIOI_TINH_OPTIONS];

/**
 * ORDER_TYPES - Các loại hình đơn hàng
 */
export const ORDER_TYPES = {
    ALL: null,
    ONLINE: 'ONLINE',
    IN_STORE: 'IN_STORE',
    TAI_QUAY: 'TAI_QUAY',
    OFFLINE: 'OFFLINE'
};

export const DELIVERY_METHODS = {
    TAKEAWAY: 'TAKEAWAY',
    SHIPPING: 'SHIPPING'
};

export const ORDER_TYPE_OPTIONS = [
    { title: 'Tất cả', value: null },
    { title: 'Trực tuyến', value: ORDER_TYPES.ONLINE },
    { title: 'Cửa hàng', value: ORDER_TYPES.IN_STORE }
];
