package com.example.be.entity;

import com.example.be.core.common.base.BaseCodeNameEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "dot_giam_gia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverrides({
    @AttributeOverride(name = "ma", column = @Column(name = "ma_dot_giam_gia")),
    @AttributeOverride(name = "ten", column = @Column(name = "ten_dot_giam_gia"))
})
public class DotGiamGia extends BaseCodeNameEntity {

    @Column(name = "loai_giam_gia")
    private String loaiGiamGia;

    @Column(name = "so_tien_giam")
    private BigDecimal soTienGiam;

    @Column(name = "dieu_kien_giam_gia")
    private BigDecimal dieuKienGiamGia;

    @Column(name = "ngay_bat_dau")
    private Long ngayBatDau;

    @Column(name = "ngay_ket_thuc")
    private Long ngayKetThuc;

    @Column(name = "muc_uu_tien")
    private Integer mucUuTien;

    @Column(name = "mo_ta", length = 1000)
    private String moTa;

}
