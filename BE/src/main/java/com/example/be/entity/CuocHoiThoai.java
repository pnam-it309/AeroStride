package com.example.be.entity;

import com.example.be.core.common.base.PrimaryEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cuoc_hoi_thoai")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuocHoiThoai extends PrimaryEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang")
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien nhanVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien_nhan")
    private NhanVien nhanVienNhan; // Dùng cho chat nội bộ (người nhận)

    @Column(name = "ma_phien")
    private String maPhien; // Dùng cho khách vãng lai chưa đăng nhập

    @Builder.Default
    @Column(name = "da_chap_nhan")
    private Boolean daChapNhan = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_hoi_thoai")
    @Builder.Default
    private LoaiHoiThoai loaiHoiThoai = LoaiHoiThoai.CUSTOMER;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_hoi_thoai")
    @Builder.Default
    private TrangThaiHoiThoai trangThaiHoiThoai = TrangThaiHoiThoai.PENDING;

    @Column(name = "danh_gia_chat")
    private Integer danhGiaChat; // Đánh giá sao từ 1-5

    @Column(name = "phan_hoi_chat", columnDefinition = "NVARCHAR(500)")
    private String phanHoiChat; // Feedback text

    @OneToMany(mappedBy = "cuocHoiThoai", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("ngayTao ASC")
    @Builder.Default
    private List<TinNhan> danhSachTinNhan = new ArrayList<>();

    public enum LoaiHoiThoai {
        CUSTOMER, INTERNAL
    }

    public enum TrangThaiHoiThoai {
        PENDING, ACTIVE, CLOSED
    }
}
