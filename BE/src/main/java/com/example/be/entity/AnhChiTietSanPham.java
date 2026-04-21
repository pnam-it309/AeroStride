package com.example.be.entity;

import com.example.be.core.common.base.PrimaryEntity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "anh_chi_tiet_san_pham")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AnhChiTietSanPham extends PrimaryEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chi_tiet_san_pham")
    @JsonBackReference(value = "ctsp-anh")
    private ChiTietSanPham chiTietSanPham;

    @Column(name = "duong_dan_anh")
    private String duongDanAnh;

    @Column(name = "hinh_anh_dai_dien")
    private Boolean hinhAnhDaiDien;

    @Column(name = "mo_ta")
    private String moTa;

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

}
