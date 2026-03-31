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
@Table(name = "phieu_giam_gia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverrides({
    @AttributeOverride(name = "ma", column = @Column(name = "ma_phieu_giam_gia")),
    @AttributeOverride(name = "ten", column = @Column(name = "ten_phieu"))
})
public class PhieuGiamGia extends BaseCodeNameEntity {

    @Column(name = "loai_phieu")
    private String loaiPhieu;

    @Column(name = "phan_tram_giam_gia")
    private Integer phanTramGiamGia;

    @Column(name = "so_tien_giam")
    private BigDecimal soTienGiam;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "don_hang_toi_thieu")
    private BigDecimal donHangToiThieu;

    @Column(name = "giam_toi_da")
    private BigDecimal giamToiDa;

    @Column(name = "ngay_bat_dau")
    private Long ngayBatDau;

    @Column(name = "ngay_ket_thuc")
    private Long ngayKetThuc;

    @Column(name = "ghi_chu")
    private String ghiChu;

}
