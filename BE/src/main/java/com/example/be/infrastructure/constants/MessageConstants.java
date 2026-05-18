package com.example.be.infrastructure.constants;

/**
 * Centralized success/error messages cho tất cả controllers.
 * Tránh hardcode message strings trong controller layer.
 */
public final class MessageConstants {

    private MessageConstants() {}

    // ============ Common ============
    public static final String UPDATE_STATUS_SUCCESS = "Cập nhật trạng thái thành công!";
    public static final String INVALID_STATUS = "Trạng thái không hợp lệ!";
    public static final String INVALID_STATUS_VALUE = "Giá trị trạng thái không hợp lệ: ";
    public static final String STATUS_REQUIRED = "Trạng thái không được để trống!";
    public static final String IMPORT_SUCCESS = "Import thành công!";
    public static final String DUPLICATE_MA = "Mã đã tồn tại trong hệ thống!";
    public static final String DUPLICATE_EMAIL = "Email đã được sử dụng!";
    public static final String DUPLICATE_USERNAME = "Tên tài khoản đã được sử dụng!";

    // ============ Đợt giảm giá ============
    public static final String DOT_GIAM_GIA_ADD_SUCCESS = "Thêm đợt giảm giá thành công!";
    public static final String DOT_GIAM_GIA_UPDATE_SUCCESS = "Cập nhật đợt giảm giá thành công!";
    public static final String DOT_GIAM_GIA_DELETE_SUCCESS = "Xóa đợt giảm giá thành công!";
    public static final String DOT_GIAM_GIA_NOT_FOUND = "Không tìm thấy đợt giảm giá!";

    // ============ Hóa đơn ============
    public static final String HOA_DON_NOT_FOUND = "Không tìm thấy hóa đơn!";
    public static final String HOA_DON_WAITING_LIMIT = "Tối đa 5 hóa đơn chờ.";
    public static final String HOA_DON_EMPTY = "Hóa đơn trống.";
    public static final String HOA_DON_PAYMENT_FAILED = "Thanh toán thất bại!";

    // ============ Bán hàng ============
    public static final String PRODUCT_OUT_OF_STOCK = "Sản phẩm không đủ số lượng tồn kho.";
    public static final String PRODUCT_DETAIL_NOT_FOUND = "Không tìm thấy sản phẩm chi tiết!";

    // ============ Khách hàng ============
    public static final String KHACH_HANG_ADD_SUCCESS = "Thêm khách hàng thành công!";
    public static final String KHACH_HANG_UPDATE_SUCCESS = "Cập nhật khách hàng thành công!";
    public static final String KHACH_HANG_DELETE_SUCCESS = "Xóa khách hàng thành công!";
    public static final String KHACH_HANG_NOT_FOUND = "Không tìm thấy khách hàng với id: ";
    public static final String KHACH_HANG_MA_EXISTS = "Mã khách hàng này đã tồn tại trong hệ thống.";
    public static final String KHACH_HANG_EMAIL_EXISTS = "Email này đã được sử dụng bởi một khách hàng khác.";
    public static final String DIA_CHI_NOT_FOUND = "Địa chỉ không tồn tại";
    public static final String DIA_CHI_ADD_SUCCESS = "Thêm địa chỉ thành công!";
    public static final String DIA_CHI_UPDATE_SUCCESS = "Cập nhật địa chỉ thành công!";
    public static final String DIA_CHI_DELETE_SUCCESS = "Xóa địa chỉ thành công!";
    public static final String DIA_CHI_SET_DEFAULT_SUCCESS = "Đặt địa chỉ mặc định thành công!";
    public static final String KHACH_LE = "Khách lẻ";

    // ============ Nhân viên ============
    public static final String NHAN_VIEN_ADD_SUCCESS = "Thêm nhân viên thành công!";
    public static final String NHAN_VIEN_UPDATE_SUCCESS = "Cập nhật nhân viên thành công!";
    public static final String NHAN_VIEN_DELETE_SUCCESS = "Xóa nhân viên thành công!";
    public static final String NHAN_VIEN_MA_EXISTS = "Mã nhân viên này đã tồn tại.";
    public static final String NHAN_VIEN_EMAIL_EXISTS = "Email này đã được sử dụng bởi một nhân viên khác.";

    // ============ Phiếu giảm giá ============
    public static final String PHIEU_GIAM_GIA_ADD_SUCCESS = "Thêm phiếu giảm giá thành công!";
    public static final String PHIEU_GIAM_GIA_UPDATE_SUCCESS = "Cập nhật phiếu giảm giá thành công!";
    public static final String PHIEU_GIAM_GIA_DELETE_SUCCESS = "Xóa phiếu giảm giá thành công!";
    public static final String PHIEU_GIAM_GIA_NOT_FOUND = "Không tìm thấy phiếu giảm giá!";

    // ============ Sản phẩm ============
    public static final String SAN_PHAM_FORM_OPTIONS_SUCCESS = "Lấy dữ liệu form sản phẩm thành công";
    public static final String SAN_PHAM_CREATE_SUCCESS = "Tạo sản phẩm thành công";
    public static final String SAN_PHAM_LIST_SUCCESS = "Lấy danh sách sản phẩm thành công";
    public static final String SAN_PHAM_DETAIL_SUCCESS = "Lấy chi tiết sản phẩm thành công";
    public static final String SAN_PHAM_UPDATE_SUCCESS = "Cập nhật sản phẩm thành công";
    public static final String SAN_PHAM_DELETE_SUCCESS = "Xóa sản phẩm thành công";
    public static final String SAN_PHAM_MAX_PRICE_SUCCESS = "Lấy giá cao nhất thành công";
    public static final String SAN_PHAM_VARIANT_DUPLICATE = "Dữ liệu biến thể bị trùng lặp tổ hợp màu sắc và kích thước";

    public static final String VARIANT_LIST_SUCCESS = "Lấy danh sách biến thể thành công";
    public static final String VARIANT_ADD_SUCCESS = "Thêm biến thể thành công";
    public static final String VARIANT_UPDATE_SUCCESS = "Cập nhật biến thể thành công";
    public static final String VARIANT_DELETE_SUCCESS = "Xóa biến thể thành công";

    public static final String VARIANT_IMAGE_ADD_SUCCESS = "Thêm ảnh biến thể thành công";
    public static final String VARIANT_IMAGE_UPDATE_SUCCESS = "Cập nhật ảnh biến thể thành công";
    public static final String VARIANT_IMAGE_SET_MAIN_SUCCESS = "Đặt ảnh chính thành công";
    public static final String VARIANT_IMAGE_DELETE_SUCCESS = "Xóa ảnh biến thể thành công";

    // ============ Thuộc tính ============
    public static final String ATTRIBUTE_MA_EXISTS = "Mã thuộc tính đã tồn tại!";
    public static final String ATTRIBUTE_TEN_EXISTS = "Tên thuộc tính đã tồn tại!";

    // ============ Reset password ============
    public static final String RESET_PASSWORD_REQUEST_SUCCESS = "Đã gửi yêu cầu reset mật khẩu tới admin";
    public static final String RESET_PASSWORD_APPROVE_SUCCESS = "Đã reset mật khẩu và gửi email cho nhân viên";
    public static final String RESET_PASSWORD_NOT_FOUND = "Không tìm thấy nhân viên với email này";
    public static final String RESET_PASSWORD_ALREADY_PENDING = "Đã có yêu cầu đang chờ xử lý";
    public static final String RESET_PASSWORD_INVALID_REQUEST = "Yêu cầu không hợp lệ hoặc đã xử lý";

    // ============ CENTRALIZED EXCEPTION MESSAGES ============
    public static final String EXCEL_EXPORT_ERROR = "Lỗi xuất file Excel: ";
    public static final String TEMPLATE_LOAD_ERROR = "Lỗi tải template: ";
    public static final String EXCEL_IMPORT_ERROR = "Lỗi nhập Excel: ";
    public static final String NHAN_VIEN_NOT_FOUND = "Không tìm thấy nhân viên với id: ";
    public static final String PHAN_QUYEN_NOT_FOUND = "Không tìm thấy phân quyền";
    public static final String PHIEU_GIAM_GIA_NOT_FOUND_ID = "Không tìm thấy phiếu giảm giá với id: ";
    public static final String PHIEU_GIAM_GIA_DELETE_ERROR = "Không tìm thấy phiếu giảm giá để xóa";
    public static final String PHIEU_GIAM_GIA_UPDATE_ERROR = "Không tìm thấy phiếu giảm giá để cập nhật";
    public static final String DOT_GIAM_GIA_NOT_FOUND_ID = "Không tìm thấy đợt giảm giá với id: ";
    public static final String DOT_GIAM_GIA_DETAIL_NOT_FOUND = "Không tìm thấy chi tiết đợt giảm giá";
    public static final String MAU_SAC_NOT_FOUND = "Màu sắc không tồn tại";
    public static final String KICH_THUOC_NOT_FOUND = "Kích thước không tồn tại";
    public static final String BIEN_THE_NOT_FOUND = "Biến thể không tồn tại";
    public static final String ANH_BIEN_THE_NOT_FOUND = "Ảnh biến thể không tồn tại";
    public static final String SAN_PHAM_NOT_FOUND = "Sản phẩm không tồn tại";
    public static final String SAN_PHAM_MA_EXISTS = "Mã sản phẩm đã tồn tại: ";
    public static final String INVALID_STATUS_TRANSITION = "Trạng thái không hợp lệ: chuyển từ %s sang %s";

    // ============ POS sales exception constants ============
    public static final String HOA_DON_NOT_EXIST = "Hóa đơn không tồn tại";
    public static final String SAN_PHAM_NOT_IN_HOA_DON = "Sản phẩm không tìm thấy trong hóa đơn";
    public static final String PRODUCT_INSUFFICIENT_QTY = "Sản phẩm không đủ số lượng.";
    public static final String KHACH_HANG_NOT_EXIST = "Khách hàng không tồn tại";
    public static final String VOUCHER_NOT_EXIST = "Voucher không tồn tại";
    public static final String PRODUCT_OUT_OF_STOCK_FORMAT = "Sản phẩm %s không đủ tồn kho.";
}
