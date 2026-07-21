package com.example.be.entity;

import com.example.be.core.common.base.AuditEntity;
import com.example.be.core.common.base.IsIdentified;
import com.example.be.infrastructure.annotations.CodePrefix;
import com.example.be.infrastructure.constants.EntityProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "giao_ca")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CodePrefix("GC")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class GiaoCa extends AuditEntity implements IsIdentified {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = EntityProperties.LENGTH_ID, updatable = false)
    private String id;

    @Column(name = "ma_giao_ca", unique = true)
    private String maGiaoCa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien_trong_ca")
    private NhanVien nhanVienTrongCa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien_nhan_ca")
    private NhanVien nhanVienNhanCa;

    @Column(name = "thoi_gian_vao_ca")
    private Long thoiGianVaoCa;

    @Column(name = "thoi_gian_ra_ca")
    private Long thoiGianRaCa;

    @Column(name = "tien_ban_dau")
    private BigDecimal tienBanDau;

    @Column(name = "tong_doanh_thu")
    private BigDecimal tongDoanhThu; // Tiền mặt thu được trong ca

    @Column(name = "tien_thuc_te")
    private BigDecimal tienThucTe;   // Thu ngân đếm tay và nhập vào

    @Column(name = "tien_chenh_lech")
    private BigDecimal tienChenhLech; // Thực tế - (Ban đầu + Doanh thu)

    @Column(name = "ghi_chu", columnDefinition = "NVARCHAR(500)")
    private String ghiChu;

    @Column(name = "trang_thai")
    private String trangThai; // OPEN, CLOSED

}
