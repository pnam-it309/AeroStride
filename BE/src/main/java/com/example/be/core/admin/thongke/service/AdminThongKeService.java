package com.example.be.core.admin.thongke.service;

import com.example.be.core.admin.thongke.model.response.AdminThongKeResponse;

import java.time.LocalDate;
import java.util.List;

public interface AdminThongKeService {
    AdminThongKeResponse getTongQuan(LocalDate tuNgay, LocalDate denNgay);
    List<AdminThongKeResponse.DoanhThuNgay> getDoanhThuTheoNgay(LocalDate tuNgay, LocalDate denNgay);
    List<AdminThongKeResponse.DonHangGanDay> getDonHangGanDay(int limit);
}
