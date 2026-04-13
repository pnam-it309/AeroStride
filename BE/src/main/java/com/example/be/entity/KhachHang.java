package com.example.be.entity;

import com.example.be.core.common.base.BaseCodeNameEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "khach_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverrides({
    @AttributeOverride(name = "ma", column = @Column(name = "ma_nguoi_dung")),
    @AttributeOverride(name = "ten", column = @Column(name = "ten_nguoi_dung"))
})
public class KhachHang extends BaseCodeNameEntity {

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "ten_tai_khoan", unique = true)
    private String tenTaiKhoan;

    @Column(name = "mat_khau")
    private String matKhau;

    @Column(name = "gioi_tinh")
    private Boolean gioiTinh;

    @Column(name = "sdt")
    private String sdt;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Column(name = "hinh_anh")
    private String hinhAnh;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @ManyToOne
    @JoinColumn(name = "id_dia_chi", referencedColumnName ="id")
    private DiaChi diaChi;

}
