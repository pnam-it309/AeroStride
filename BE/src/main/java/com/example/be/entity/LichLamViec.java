package com.example.be.entity;

import com.example.be.core.common.base.PrimaryEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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

    public enum TrangThaiLichLamViec {
        CHO_XAC_NHAN,
        DA_XAC_NHAN,
        DA_HUY
    }
}
