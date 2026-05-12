export const API_BASE = {
    ADMIN: '/admin',
    PUBLIC: '/public',
    AUTH: '/auth'
};

export const API_ADMIN = {
    DOT_GIAM_GIA: `${API_BASE.ADMIN}/dot-giam-gia`,
    HOA_DON: `${API_BASE.ADMIN}/hoa-don`,
    KHACH_HANG: `${API_BASE.ADMIN}/khach-hang`,
    NHAN_VIEN: `${API_BASE.ADMIN}/nhan-vien`,
    PHIEU_GIAM_GIA: `${API_BASE.ADMIN}/phieu-giam-gia`,
    SAN_PHAM: `${API_BASE.ADMIN}/san-pham`,
    DIA_CHI: `${API_BASE.ADMIN}/dia-chi`,
    THUOC_TINH: `${API_BASE.ADMIN}/thuoc-tinh`,
    BAN_HANG: `${API_BASE.ADMIN}/ban-hang`,
};

export const API_AUTH = {
    BASE: API_BASE.AUTH,
    LOGIN: `${API_BASE.AUTH}/login`,
    LOGOUT: `${API_BASE.AUTH}/logout`,
    REFRESH: `${API_BASE.AUTH}/refresh-token`,
};

export const API_PRODUCT = {
    SAN_PHAM: API_ADMIN.SAN_PHAM,
    BIEN_THE: `${API_ADMIN.SAN_PHAM}/variants`,
    ANH_BIEN_THE: `${API_ADMIN.SAN_PHAM}/variant-images`,
};

export const API_COMMON = {
    RESET_PASSWORD: '/reset-password-requests',
    PAYMENT: '/payment',
    STORAGE: '/storage',
};

export const API_THUOC_TINH = {
    THUONG_HIEU: `${API_ADMIN.THUOC_TINH}/thuong-hieu`,
    DANH_MUC: `${API_ADMIN.THUOC_TINH}/danh-muc`,
    MAU_SAC: `${API_ADMIN.THUOC_TINH}/mau-sac`,
    KICH_THUOC: `${API_ADMIN.THUOC_TINH}/kich-thuoc`,
    CHAT_LIEU: `${API_ADMIN.THUOC_TINH}/chat-lieu`,
    DE_GIAY: `${API_ADMIN.THUOC_TINH}/de-giay`,
    CO_GIAY: `${API_ADMIN.THUOC_TINH}/co-giay`,
    XUAT_XU: `${API_ADMIN.THUOC_TINH}/xuat-xu`,
    MUC_DICH_CHAY: `${API_ADMIN.THUOC_TINH}/muc-dich-chay`,
};

export const API_CHAT = {
    CONVERSATIONS: `${API_BASE.ADMIN}/chat/conversations`,
    MESSAGES: (id) => `${API_BASE.ADMIN}/chat/conversations/${id}/messages`,
    ACCEPT: (id) => `${API_BASE.ADMIN}/chat/conversations/${id}/accept`,
    SEND: `${API_BASE.ADMIN}/chat/send`,
    CUSTOMER_BASE: '/customer/chat',
    CUSTOMER_SEND: '/customer/chat/send',
};

export const API_LICH_LAM_VIEC = {
    SCHEDULES: `${API_BASE.ADMIN}/lich-lam-viec/schedules`,
    SHIFTS: `${API_BASE.ADMIN}/lich-lam-viec/shifts`,
    ACTIVITIES: `${API_BASE.ADMIN}/lich-lam-viec/activities`,
};
