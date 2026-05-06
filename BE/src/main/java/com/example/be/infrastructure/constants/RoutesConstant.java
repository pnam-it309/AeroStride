package com.example.be.infrastructure.constants;

public final class RoutesConstant {
    private RoutesConstant() {}

    public static final String API_PREFIX = "/api/v1";
    
    // Auth routes
    public static final String AUTH = API_PREFIX + "/auth";
    public static final String RESET_PASSWORD = API_PREFIX + "/reset-password-requests";
    
    // Admin routes
    public static final String ADMIN = API_PREFIX + "/admin";
    public static final String ADMIN_DOT_GIAM_GIA = ADMIN + "/dot-giam-gia";
    public static final String ADMIN_PHIEU_GIAM_GIA = ADMIN + "/phieu-giam-gia";
    public static final String ADMIN_SAN_PHAM = ADMIN + "/san-pham";
    public static final String ADMIN_KHACH_HANG = ADMIN + "/khach-hang";
    public static final String ADMIN_NHAN_VIEN = ADMIN + "/nhan-vien";
    public static final String ADMIN_HOA_DON = ADMIN + "/hoa-don";
    public static final String ADMIN_THUOC_TINH = ADMIN + "/thuoc-tinh";
    public static final String ADMIN_BAN_HANG = ADMIN + "/ban-hang";
    public static final String ADMIN_DIA_CHI = ADMIN + "/dia-chi";
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

    // Common Sub-paths (Compatibility)
    public static final String HIEN_THI = "/hien-thi";
    public static final String PHAN_TRANG = "/phan-trang";
    public static final String TIM_KIEM = "/tim-kiem";
    public static final String ADD = "/add";
    public static final String UPDATE = "/update/{id}";
    public static final String DELETE = "/delete/{id}";
    public static final String DETAIL = "/detail/{id}";
    public static final String STATUS = "/status/{id}";
    public static final String EXPORT_EXCEL = "/export-excel";
    public static final String DOWNLOAD_TEMPLATE = "/download-template";
    public static final String IMPORT_EXCEL = "/import-excel";
    public static final String PENDING = "/pending";
    public static final String ID = "/{id}";

    // Ban Hang specific
    public static final String ADD_PRODUCT = "/{id}/add-product";
    public static final String UPDATE_QUANTITY = "/{id}/update-quantity/{idHdct}";
    public static final String REMOVE_PRODUCT = "/{id}/remove-product/{idHdct}";
    public static final String KHACH_HANG_SUB = "/{id}/khach-hang";
    public static final String VOUCHER_SUB = "/{id}/voucher";
    public static final String CHECKOUT = "/{id}/checkout";
    public static final String SEARCH_SAN_PHAM = "/search-san-pham";
    public static final String SEARCH_KHACH_HANG = "/search-khach-hang";
    public static final String VOUCHERS = "/vouchers";

    // Dot Giam Gia specific
    public static final String STATUS_SUB = "/{id}/trang-thai"; // Using Patch
    public static final String SAN_PHAM_AP_DUNG = "/san-pham-ap-dung";
    public static final String BIEN_THE_AP_DUNG = "/bien-the-ap-dung/{id}";

    // General Actions
    public static final String PRINT = "/{id}/print";
    public static final String STATUS_ALT = "/{id}/status";
    public static final String INFO = "/{id}/info";
    public static final String PRODUCTS = "/{id}/products";
    public static final String PRODUCTS_DETAIL = "/{id}/products/{idHdct}";
    public static final String COUNTS = "/counts";
    public static final String SET_DEFAULT = "/set-default/{id}";
    public static final String KHACH_HANG_DETAIL = "/khach-hang/{khId}";
    public static final String AVATAR = "/avatar";
    public static final String PHAN_QUYEN = "/phan-quyen";
    public static final String REQUEST = "/request";
    public static final String APPROVE = "/approve/{id}";

    // San Pham specific
    public static final String FORM_OPTIONS = "/form-options";
    public static final String VARIANTS_SUB = "/{id}/variants";
    public static final String VARIANTS = "/variants";
    public static final String VARIANT_ID = "/variants/{variantId}";
    public static final String VARIANT_IMAGES = "/variants/{variantId}/images";
    public static final String VARIANT_IMAGE_ID = "/variant-images/{imageId}";
    public static final String VARIANT_IMAGE_MAIN = "/variant-images/{imageId}/main";
}
