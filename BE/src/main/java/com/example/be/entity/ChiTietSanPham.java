package com.example.be.entity;

import com.example.be.core.common.base.PrimaryEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "chi_tiet_san_pham",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ctsp_san_pham_mau_kich_thuoc_xoa_mem",
                        columnNames = {"id_san_pham", "id_mau_sac", "id_kich_thuoc", "xoa_mem"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChiTietSanPham extends PrimaryEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_san_pham")
    private SanPham sanPham;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_kich_thuoc")
    private KichThuoc kichThuoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mau_sac")
    private MauSac mauSac;

    @Column(name = "ma_chi_tiet_san_pham", unique = true)
    private String maChiTietSanPham;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "gia_nhap")
    private BigDecimal giaNhap;

    @Column(name = "gia_ban")
    private BigDecimal giaBan;

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

    @OneToMany(mappedBy = "chiTietSanPham", fetch = FetchType.LAZY)
    @Builder.Default
    private List<AnhChiTietSanPham> anhChiTietSanPhams = new ArrayList<>();

    @OneToMany(mappedBy = "chiTietSanPham", fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChiTietDotGiamGia> chiTietDotGiamGias = new ArrayList<>();

    @OneToMany(mappedBy = "chiTietSanPham", fetch = FetchType.LAZY)
    @Builder.Default
    private List<HoaDonChiTiet> hoaDonChiTiets = new ArrayList<>();

}
