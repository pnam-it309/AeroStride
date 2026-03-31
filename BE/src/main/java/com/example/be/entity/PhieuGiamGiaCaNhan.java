package com.example.be.entity;

import com.example.be.core.common.base.PrimaryEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "phieu_giam_gia_ca_nhan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhieuGiamGiaCaNhan extends PrimaryEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang")
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_phieu_giam_gia")
    private PhieuGiamGia phieuGiamGia;

    @Column(name = "ma_phieu_giam_gia_ca_nhan", unique = true)
    private String maPhieuGiamGiaCaNhan;

    @Column(name = "ngay_nhan")
    private Long ngayNhan;

    @Column(name = "da_su_dung")
    private Boolean daSuDung;

    @Column(name = "ngay_su_dung")
    private Long ngaySuDung;

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

}
