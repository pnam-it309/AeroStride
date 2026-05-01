package com.example.be.entity;

import com.example.be.core.common.base.PrimaryEntity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lich_su_trang_thai_hoa_don")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichSuTrangThaiHoaDon extends PrimaryEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hoa_don")
    @JsonIgnoreProperties("listsLichSuHoaDon")
    private HoaDon hoaDon;

    @Column(name = "trang_thai_cu")
    private Integer trangThaiCu;

    @Column(name = "trang_thai_moi")
    private Integer trangThaiMoi;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @Column(name = "nguoi_thuc_hien")
    private String nguoiThucHien;

}
