package com.example.be.entity;

import com.example.be.core.common.base.BaseCodeNameEntity;
import com.example.be.infrastructure.constants.GioiTinhKhachHang;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "san_pham")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverrides({
    @AttributeOverride(name = "ma", column = @Column(name = "ma_san_pham")),
    @AttributeOverride(name = "ten", column = @Column(name = "ten_san_pham"))
})
public class SanPham extends BaseCodeNameEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_thuong_hieu")
    private ThuongHieu thuongHieu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_danh_muc")
    private DanhMuc danhMuc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_xuat_xu")
    private XuatXu xuatXu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_muc_dich_chay")
    private MucDichChay mucDichChay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chat_lieu")
    private ChatLieu chatLieu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_de_giay")
    private DeGiay deGiay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_co_giay")
    private CoGiay coGiay;

    @Column(name = "gioi_tinh_khach_hang")
    @Enumerated(EnumType.STRING)
    private GioiTinhKhachHang gioiTinhKhachHang;

    @Column(name = "mo_ta_ngan")
    private String moTaNgan;

    @Column(name = "hinh_anh")
    private String hinhAnh;

    @Column(name = "mo_ta_chi_tiet")
    private String moTaChiTiet;

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

    @OneToMany(mappedBy = "sanPham", fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChiTietSanPham> chiTietSanPhams = new ArrayList<>();

}
