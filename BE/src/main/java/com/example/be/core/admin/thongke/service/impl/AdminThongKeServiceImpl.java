package com.example.be.core.admin.thongke.service.impl;

import com.example.be.core.admin.thongke.model.response.AdminThongKeResponse;
import com.example.be.core.admin.thongke.repository.AdminThongKeRepository;
import com.example.be.core.admin.thongke.service.AdminThongKeService;
import com.example.be.utils.AccountUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminThongKeServiceImpl implements AdminThongKeService {

    private final AdminThongKeRepository thongKeRepository;

    @Override
    public AdminThongKeResponse getTongQuan(LocalDate tuNgay, LocalDate denNgay) {
        // DRY: Sử dụng AccountUtils để parse date đồng nhất
        Long tuNgayMs = AccountUtils.parseDateToLong(tuNgay != null ? tuNgay.toString() : null, false);
        Long denNgayMs = AccountUtils.parseDateToLong(denNgay != null ? denNgay.toString() : null, true);

        BigDecimal tongDoanhThu = thongKeRepository.sumDoanhThu(tuNgayMs, denNgayMs);
        Long tongDonHang = thongKeRepository.countTongDon(tuNgayMs, denNgayMs);
        Long donHoanThanh = thongKeRepository.countByTrangThai(6, tuNgayMs, denNgayMs); // OrderStatus.HOAN_THANH
        Long donChoXacNhan = thongKeRepository.countByTrangThai(1, tuNgayMs, denNgayMs); // CHO_XAC_NHAN
        Long donDangGiao = thongKeRepository.countByTrangThai(3, tuNgayMs, denNgayMs);  // DANG_VAN_CHUYEN
        Long donDaHuy = thongKeRepository.countByTrangThai(7, tuNgayMs, denNgayMs);     // DA_HUY

        Long tongKhachHang = thongKeRepository.count();

        return AdminThongKeResponse.builder()
                .tongDoanhThu(tongDoanhThu != null ? tongDoanhThu : BigDecimal.ZERO)
                .tongDonHang(tongDonHang != null ? tongDonHang : 0L)
                .donHangHoanThanh(donHoanThanh != null ? donHoanThanh : 0L)
                .donHangChoXacNhan(donChoXacNhan != null ? donChoXacNhan : 0L)
                .donHangDangGiao(donDangGiao != null ? donDangGiao : 0L)
                .donHangDaHuy(donDaHuy != null ? donDaHuy : 0L)
                .tongKhachHang(tongKhachHang)
                .sanPhamSapHet(0L) 
                .build();
    }

    @Override
    public List<AdminThongKeResponse.DoanhThuNgay> getDoanhThuTheoNgay(LocalDate tuNgay, LocalDate denNgay) {
        if (tuNgay == null) tuNgay = LocalDate.now().minusDays(29);
        if (denNgay == null) denNgay = LocalDate.now();

        Long tuNgayMs = AccountUtils.parseDateToLong(tuNgay.toString(), false);
        Long denNgayMs = AccountUtils.parseDateToLong(denNgay.toString(), true);

        List<Object[]> rows = thongKeRepository.getDoanhThuTheoNgay(tuNgayMs, denNgayMs);
        List<AdminThongKeResponse.DoanhThuNgay> result = new ArrayList<>();

        for (Object[] row : rows) {
            result.add(AdminThongKeResponse.DoanhThuNgay.builder()
                    .ngay(row[0] != null ? row[0].toString() : "")
                    .doanhThu(row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO)
                    .soDon(row[2] != null ? Long.parseLong(row[2].toString()) : 0L)
                    .build());
        }
        return result;
    }

    @Override
    public List<AdminThongKeResponse.DonHangGanDay> getDonHangGanDay(int limit) {
        List<Object[]> rows = thongKeRepository.getDonHangGanDay(PageRequest.of(0, limit));
        List<AdminThongKeResponse.DonHangGanDay> result = new ArrayList<>();

        for (Object[] row : rows) {
            result.add(AdminThongKeResponse.DonHangGanDay.builder()
                    .id(row[0] != null ? row[0].toString() : "")
                    .maHoaDon(row[1] != null ? row[1].toString() : "")
                    .tenKhachHang(row[2] != null ? row[2].toString() : "Khách lẻ")
                    .ngayTao(row[3] != null ? Long.parseLong(row[3].toString()) : 0L)
                    .tongTien(row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO)
                    .trangThai(row[5] != null ? Integer.parseInt(row[5].toString()) : 0)
                    .loaiDon(row[6] != null ? row[6].toString() : "")
                    .build());
        }
        return result;
    }
}
