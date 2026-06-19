package com.example.be.entity;

import com.example.be.core.common.base.PrimaryEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "lich_lam_viec")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichLamViec extends PrimaryEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien nhanVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ca_lam")
    private CaLam caLam;

    @Column(name = "ngay_lam")
    private LocalDate ngayLam;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_lich")
    private TrangThaiLichLamViec trangThaiLich;

    @Column(name = "gio_vao")
    private LocalTime gioVao;

    @Column(name = "gio_ra")
    private LocalTime gioRa;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @Builder.Default
    @Column(name = "tang_ca")
    private Boolean tangCa = false;

    @Column(name = "gio_bat_dau_tang_ca")
    private LocalTime gioBatDauTangCa;

    @Column(name = "gio_ket_thuc_tang_ca")
    private LocalTime gioKetThucTangCa;

    public enum TrangThaiLichLamViec {
        CHO_XAC_NHAN,
        DA_XAC_NHAN,
        DA_HUY
    }
}
