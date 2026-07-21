package com.example.be.entity;

import com.example.be.core.common.base.AuditEntity;
import com.example.be.core.common.base.IsIdentified;
import com.example.be.infrastructure.constants.EntityProperties;
import com.example.be.infrastructure.listener.PrimaryEntityListener;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "danh_gia_san_pham")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(PrimaryEntityListener.class)
public class DanhGiaSanPham extends AuditEntity implements IsIdentified {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = EntityProperties.LENGTH_ID, updatable = false)
    private String id;

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

    @Column(name = "hinh_anh", columnDefinition = "JSON")
    private String hinhAnh; // Lưu dạng mảng URL JSON

    @Column(name = "video", length = 500)
    private String video;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai")
    @Builder.Default
    private TrangThaiDanhGia trangThai = TrangThaiDanhGia.PENDING;

    public enum TrangThaiDanhGia {
        PENDING, APPROVED, REJECTED, SPAM
    }
}
