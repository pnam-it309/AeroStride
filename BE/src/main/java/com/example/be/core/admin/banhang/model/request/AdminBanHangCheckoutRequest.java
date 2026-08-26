package com.example.be.core.admin.banhang.model.request;

import com.example.be.infrastructure.constants.DeliveryMethod;
import com.example.be.infrastructure.constants.OrderType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class AdminBanHangCheckoutRequest {
    /** Id khach hang da co; null khi khach le hoac khach moi nhap tai man ban hang. */
    private String idKhachHang;
    /** Thong tin khach moi/khach le, duoc gan vao hoa don khi thanh toan thanh cong. */
    @Size(max = 100, message = "Tên khách hàng không được vượt quá 100 ký tự.")
    private String tenKhachHang;
    @Pattern(regexp = "^$|^0[0-9]{9}$", message = "Số điện thoại không hợp lệ (gồm 10 số bắt đầu bằng số 0).")
    @Size(max = 15, message = "Số điện thoại không được vượt quá 15 ký tự.")
    private String sdtKhachHang;
    @Email(message = "Email không đúng định dạng.")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự.")
    private String emailKhachHang;
    private Boolean gioiTinhKhachHang;
    private LocalDate ngaySinhKhachHang;
    /** Phieu giam gia dang ap dung cho hoa don tai quay. */
    private String idPhieuGiamGia;
    /** Tong tien hang truoc khi tru phi van chuyen va giam gia. */
    private BigDecimal tongTien;
    /** Phi van chuyen chi co y nghia voi don giao hang. */
    private BigDecimal phiVanChuyen;
    /** So tien cuoi cung can thu sau giam gia/phi van chuyen. */
    private BigDecimal tongTienSauGiam;
    /** Kenh nhan hang: TAI_QUAY hoac GIAO_HANG. */
    private OrderType orderType;
    private DeliveryMethod deliveryMethod;
    @Deprecated
    private String loaiDon;
    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự.")
    private String ghiChu;
    /** Thong tin nguoi nhan cho don giao hang; voi tai quay co the de trong. */
    @Size(max = 100, message = "Tên người nhận không được vượt quá 100 ký tự.")
    private String tenNguoiNhan;
    @Pattern(regexp = "^$|^0[0-9]{9}$", message = "Số điện thoại người nhận không hợp lệ (gồm 10 số bắt đầu bằng số 0).")
    @Size(max = 15, message = "Số điện thoại người nhận không được vượt quá 15 ký tự.")
    private String sdtNguoiNhan;
    private String diaChiNguoiNhan;
    private String tinh;
    private String thanhPho;
    private String phuongXa;
    @Size(max = 255, message = "Địa chỉ chi tiết không được vượt quá 255 ký tự.")
    private String diaChiChiTiet;
    /** Neu true thi luu dia chi nhan hang hien tai thanh dia chi mac dinh cua khach hang. */
    private Boolean luuDiaChiMacDinh;

    // Mixed Payment Info
    /** So tien thu bang tien mat trong lan thanh toan. */
    private BigDecimal tienMat;
    /** So tien thu bang chuyen khoan/VNPay trong lan thanh toan. */
    private BigDecimal tienChuyenKhoan;
    /** Ma giao dich ngan hang/VNPay dung de doi soat thanh toan. */
    private String maGiaoDich; // Mã GD chuyển khoản (vnp_TransactionNo or BANK_TX_ID)

    public void setSdtKhachHang(String sdtKhachHang) {
        this.sdtKhachHang = (sdtKhachHang != null && !sdtKhachHang.trim().isEmpty()) ? sdtKhachHang.trim() : null;
    }

    public void setSdtNguoiNhan(String sdtNguoiNhan) {
        this.sdtNguoiNhan = (sdtNguoiNhan != null && !sdtNguoiNhan.trim().isEmpty()) ? sdtNguoiNhan.trim() : null;
    }

    public void setEmailKhachHang(String emailKhachHang) {
        this.emailKhachHang = (emailKhachHang != null && !emailKhachHang.trim().isEmpty()) ? emailKhachHang.trim() : null;
    }

    public void setTenKhachHang(String tenKhachHang) {
        this.tenKhachHang = (tenKhachHang != null && !tenKhachHang.trim().isEmpty()) ? tenKhachHang.trim() : null;
    }

    public void setTenNguoiNhan(String tenNguoiNhan) {
        this.tenNguoiNhan = (tenNguoiNhan != null && !tenNguoiNhan.trim().isEmpty()) ? tenNguoiNhan.trim() : null;
    }
}
