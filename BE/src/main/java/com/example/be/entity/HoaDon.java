package com.example.be.entity;

import com.example.be.core.common.base.AuditEntity;
import com.example.be.core.common.base.IsIdentified;
import com.example.be.infrastructure.constants.EntityProperties;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.infrastructure.listener.PrimaryEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "hoa_don")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@EntityListeners(PrimaryEntityListener.class)
public class HoaDon extends AuditEntity implements IsIdentified {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = EntityProperties.LENGTH_ID, updatable = false)
    private String id;

    @Column(name = "trang_thai")
    @Enumerated(EnumType.ORDINAL)
    private OrderStatus trangThai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_phieu_giam_gia")
    private PhieuGiamGia phieuGiamGia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_phieu_giam_gia_ca_nhan")
    private PhieuGiamGiaCaNhan phieuGiamGiaCaNhan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang")
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien nhanVien;

    @Column(name = "ma_hoa_don", unique = true)
    private String maHoaDon;

    @Column(name = "loai_don")
    private String loaiDon;

    @Column(name = "phi_van_chuyen")
    private BigDecimal phiVanChuyen;

    @Column(name = "tong_tien")
    private BigDecimal tongTien;

    @Column(name = "tong_tien_sau_giam")
    private BigDecimal tongTienSauGiam;

    @Column(name = "tien_nguoi_mua")
    private BigDecimal tienNguoiMua;

    @Column(name = "dia_chi_nguoi_nhan")
    private String diaChiNguoiNhan;

    @Column(name = "so_dien_thoai_nguoi_nhan")
    private String soDienThoaiNguoiNhan;

    @Column(name = "ngay_du_kien_nhan")
    private Long ngayDuKienNhan;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @OneToMany(mappedBy = "hoaDon", fetch = FetchType.LAZY)
    private java.util.List<HoaDonChiTiet> listsHoaDonChiTiet;

    @OneToMany(mappedBy = "hoaDon", fetch = FetchType.LAZY)
    private java.util.List<LichSuTrangThaiHoaDon> listsLichSuHoaDon;

    @OneToMany(mappedBy = "hoaDon", fetch = FetchType.LAZY)
    private java.util.List<GiaoDichThanhToan> listsGiaoDichThanhToan;
}
