package com.example.be.entity;

import com.example.be.core.common.base.PrimaryEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "giao_dich_thanh_toan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GiaoDichThanhToan extends PrimaryEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hoa_don")
    private HoaDon hoaDon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_phuong_thuc_thanh_toan")
    private PhuongThucThanhToan phuongThucThanhToan;

    @Column(name = "ma_giao_dich_thanh_toan", unique = true)
    private String maGiaoDichThanhToan;

    @Column(name = "so_tien")
    private BigDecimal soTien;

    @Column(name = "loai_giao_dich")
    private String loaiGiaoDich;

    @Column(name = "ma_giao_dich_ngoai")
    private String maGiaoDichNgoai;

    @Column(name = "ma_tham_chieu")
    private String maThamChieu;

    @Column(name = "duong_dan_thanh_toan")
    private String duongDanThanhToan;

    @Column(name = "du_lieu_qr")
    private String duLieuQr;

    @Column(name = "thoi_gian_het_han")
    private Long thoiGianHetHan;

    @Column(name = "du_lieu_phan_hoi")
    private String duLieuPhanHoi;

    @Column(name = "ghi_chu")
    private String ghiChu;

}
