package com.example.be.entity;

import com.example.be.core.common.base.PrimaryEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "danh_gia_san_pham")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DanhGiaSanPham extends PrimaryEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hoa_don")
    private HoaDon hoaDon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_san_pham")
    private SanPham sanPham;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang")
    private KhachHang khachHang;

    @Column(name = "diem_danh_gia")
    private Integer diemDanhGia;

    @Column(name = "noi_dung", columnDefinition = "NVARCHAR(2000)")
    private String noiDung;

    @Column(name = "hinh_anh", columnDefinition = "LONGTEXT")
    private String hinhAnh; // Lưu dạng mảng URL JSON

    @Column(name = "video", length = 500)
    private String video;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_danh_gia")
    @Builder.Default
    private TrangThaiDanhGia trangThaiDanhGia = TrangThaiDanhGia.PENDING;

    public enum TrangThaiDanhGia {
        PENDING, APPROVED, REJECTED, SPAM
    }
}
