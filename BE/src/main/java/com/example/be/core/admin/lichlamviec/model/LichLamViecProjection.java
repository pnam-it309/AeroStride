package com.example.be.core.admin.lichlamviec.model;

import com.example.be.entity.LichLamViec;
import java.time.LocalDate;
import java.time.LocalTime;

public interface LichLamViecProjection {
    String getId();
    String getNhanVienId();
    String getTenNhanVien();
    String getMaNhanVien();
    String getCaId();
    String getTenCa();
    LocalDate getNgayLam();
    LichLamViec.TrangThaiLichLamViec getTrangThaiLich();
    Boolean getTangCa();
    LocalTime getGioBatDauTangCa();
    LocalTime getGioKetThucTangCa();
    LocalTime getGioVao();
    LocalTime getGioRa();
    String getGhiChu();
    LocalTime getGioBatDauCa();
    LocalTime getGioKetThucCa();
}
