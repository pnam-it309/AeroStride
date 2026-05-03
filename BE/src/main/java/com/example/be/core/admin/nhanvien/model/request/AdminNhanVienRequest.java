package com.example.be.core.admin.nhanvien.model.request;

import com.example.be.core.common.dto.PageRequest;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class AdminNhanVienRequest extends PageRequest {
    private String ma;
    private String ten;
    private String email;
    private String tenTaiKhoan;
    private String matKhau;
    private Boolean gioiTinh;
    private String sdt;
    private LocalDate ngaySinh;
    private String hinhAnh;
    private String diaChi;
    private String tinh;
    private String thanhPho;
    private String phuongXa;
    private String diaChiChiTiet;
    private String idPhanQuyen;

    // ── FILTER / TÌM KIẾM / LỌC / PHÂN TRANG ───────
    private String keyword;
    private TrangThai trangThai;

}
