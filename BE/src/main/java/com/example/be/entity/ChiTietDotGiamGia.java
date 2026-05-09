package com.example.be.entity;

import com.example.be.core.common.base.PrimaryEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "chi_tiet_dot_giam_gia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChiTietDotGiamGia extends PrimaryEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dot_giam_gia")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private DotGiamGia dotGiamGia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chi_tiet_san_pham")
    @JsonIgnoreProperties({"chiTietDotGiamGias", "hoaDonChiTiets", "anhChiTietSanPhams", "hibernateLazyInitializer", "handler"})
    private ChiTietSanPham chiTietSanPham;

    @Column(name = "gia_tri_giam")
    private BigDecimal giaTriGiam;

}
