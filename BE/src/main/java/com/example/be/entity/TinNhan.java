package com.example.be.entity;

import com.example.be.core.common.base.PrimaryEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tin_nhan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TinNhan extends PrimaryEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cuoc_hoi_thoai")
    private CuocHoiThoai cuocHoiThoai;

    @Column(name = "loai_nguoi_gui")
    private String loaiNguoiGui; // customer, staff, system

    @Column(name = "id_nguoi_gui")
    private String idNguoiGui;

    @Column(name = "ten_nguoi_gui")
    private String tenNguoiGui;

    @Column(name = "noi_dung", columnDefinition = "TEXT")
    private String noiDung;

}
