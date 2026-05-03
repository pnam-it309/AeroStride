package com.example.be.entity;

import com.example.be.core.common.base.BaseCodeNameEntity;
import com.example.be.infrastructure.constants.VaiTro;
import jakarta.persistence.*;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "nhan_vien")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@AttributeOverrides({
    @AttributeOverride(name = "ma", column = @Column(name = "ma_nhan_vien")),
    @AttributeOverride(name = "ten", column = @Column(name = "ten_nhan_vien"))
})
public class NhanVien extends BaseCodeNameEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_phan_quyen")
    private PhanQuyen phanQuyen;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "sdt")
    private String sdt;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Column(name = "gioi_tinh")
    private Boolean gioiTinh;

    @Column(name = "ten_tai_khoan", unique = true)
    private String tenTaiKhoan;

    @Column(name = "mat_khau")
    private String matKhau;

    @Column(name = "hinh_anh")
    private String hinhAnh;

    @Column(name = "diaChi")
    private String diaChi;

    @Column(name = "tinh")
    private String tinh;

    @Column(name = "thanh_pho")
    private String thanhPho;

    @Column(name = "phuong_xa")
    private String phuongXa;

    @Column(name = "dia_chi_chi_tiet")
    private String diaChiChiTiet;

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

    @Enumerated(EnumType.STRING)
    @Column(name = "reset_status")
    private ResetStatus resetStatus; // PENDING / null

    @Column(name = "reset_requested_at")
    private LocalDateTime resetRequestedAt;

    public enum ResetStatus {
        PENDING,
        APPROVED
    }

}
