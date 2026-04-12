package com.example.be.entity;

import com.example.be.core.common.base.PrimaryEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dia_chi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaChi extends PrimaryEntity {

    @Column(name = "ma_dia_chi", unique = true)
    private String maDiaChi;

    @Column(name = "tinh")
    private String tinh;

    @Column(name = "thanh_pho")
    private String thanhPho;

    @Column(name = "phuong_xa")
    private String phuongXa;

    @Column(name = "dia_chi_chi_tiet")
    private String diaChiChiTiet;

    @Column(name = "ten_nguoi_nhan")
    private String tenNguoiNhan;

    @Column(name = "sdt_nguoi_nhan")
    private String sdtNguoiNhan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang")
    private KhachHang khachHang;

    @Builder.Default
    @Column(name = "la_mac_dinh")
    private Boolean laMacDinh = false;

}
