package com.example.be.infrastructure.constants;

public final class RoutesConstant {
    private RoutesConstant() {}

    public static final String API_PREFIX = "/api/v1";
    
    // Auth routes
    public static final String AUTH = API_PREFIX + "/auth";
    
    // Admin routes
    public static final String ADMIN = API_PREFIX + "/admin";
    public static final String ADMIN_DOT_GIAM_GIA = ADMIN + "/dot-giam-gia";
    public static final String ADMIN_PHIEU_GIAM_GIA = ADMIN + "/phieu-giam-gia";
    public static final String ADMIN_SAN_PHAM = ADMIN + "/san-pham";
    public static final String ADMIN_KHACH_HANG = ADMIN + "/khach-hang";
    public static final String ADMIN_NHAN_VIEN = ADMIN + "/nhan-vien";
    public static final String ADMIN_HOA_DON = ADMIN + "/hoa-don";
    public static final String ADMIN_THUOC_TINH = ADMIN + "/thuoc-tinh";
    public static final String ADMIN_THUOC_TINH_CHAT_LIEU = ADMIN_THUOC_TINH + "/chat-lieu";
    public static final String ADMIN_THUOC_TINH_DANH_MUC = ADMIN_THUOC_TINH + "/danh-muc";
    public static final String ADMIN_THUOC_TINH_THUONG_HIEU = ADMIN_THUOC_TINH + "/thuong-hieu";
    public static final String ADMIN_THUOC_TINH_MAU_SAC = ADMIN_THUOC_TINH + "/mau-sac";
    public static final String ADMIN_THUOC_TINH_KICH_THUOC = ADMIN_THUOC_TINH + "/kich-thuoc";
    public static final String ADMIN_THUOC_TINH_DE_GIAY = ADMIN_THUOC_TINH + "/de-giay";
    public static final String ADMIN_THUOC_TINH_CO_GIAY = ADMIN_THUOC_TINH + "/co-giay";
    public static final String ADMIN_THUOC_TINH_XUAT_XU = ADMIN_THUOC_TINH + "/xuat-xu";
    public static final String ADMIN_THUOC_TINH_MUC_DICH_CHAY = ADMIN_THUOC_TINH + "/muc-dich-chay";
    
    // Staff routes
    public static final String STAFF = API_PREFIX + "/staff";
    
    // Customer routes (Mobile/Web)
    public static final String CUSTOMER = API_PREFIX + "/customer";
}
