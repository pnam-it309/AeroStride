package com.example.be.infrastructure.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private String ten;
    private String email;
    private String sdt;
    private Boolean gioiTinh;
    private LocalDate ngaySinh;
    private String hinhAnh;
    private String tinh;
    private String thanhPho;
    private String phuongXa;
    private String diaChiChiTiet;
}
