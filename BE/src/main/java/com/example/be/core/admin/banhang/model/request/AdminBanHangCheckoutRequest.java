package com.example.be.core.admin.banhang.model.request;

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
    private String tenKhachHang;
    private String sdtKhachHang;
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
    private String loaiDon; // TAI_QUAY, GIAO_HANG
    private String ghiChu;
    /** Thong tin nguoi nhan cho don giao hang; voi tai quay co the de trong. */
    private String tenNguoiNhan;
    private String sdtNguoiNhan;
    private String diaChiNguoiNhan;
    private String tinh;
    private String thanhPho;
    private String phuongXa;
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
}
