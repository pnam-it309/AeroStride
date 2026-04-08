package com.example.be.core.admin.thongke.service.impl;

import com.example.be.core.admin.thongke.model.response.AdminThongKeResponse;
import com.example.be.core.admin.thongke.repository.AdminThongKeRepository;
import com.example.be.core.admin.thongke.service.AdminThongKeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminThongKeServiceImpl implements AdminThongKeService {

    private final AdminThongKeRepository thongKeRepository;

    // OrderStatus ordinal: PENDING_PAYMENT=0, PROCESSING=1, SHIPPED=2, DELIVERED=3, CANCELLED=4, REFUNDED=5

    @Override
    public AdminThongKeResponse getTongQuan(LocalDate tuNgay, LocalDate denNgay) {
        Long tuNgayMs = tuNgay != null ? tuNgay.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() : null;
        Long denNgayMs = denNgay != null ? denNgay.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() : null;

        BigDecimal tongDoanhThu = thongKeRepository.sumDoanhThu(tuNgayMs, denNgayMs);
        Long tongDonHang = thongKeRepository.countTongDon(tuNgayMs, denNgayMs);
        Long donHoanThanh = thongKeRepository.countByTrangThai(3, tuNgayMs, denNgayMs); // DELIVERED
        Long donChoXacNhan = thongKeRepository.countByTrangThai(0, tuNgayMs, denNgayMs); // PENDING_PAYMENT
        Long donDangGiao = thongKeRepository.countByTrangThai(2, tuNgayMs, denNgayMs);  // SHIPPED
        Long donDaHuy = thongKeRepository.countByTrangThai(4, tuNgayMs, denNgayMs);     // CANCELLED

        // Tổng khách hàng (không lọc theo ngày)
        Long tongKhachHang = thongKeRepository.count();

        return AdminThongKeResponse.builder()
                .tongDoanhThu(tongDoanhThu)
                .tongDonHang(tongDonHang)
                .donHangHoanThanh(donHoanThanh)
                .donHangChoXacNhan(donChoXacNhan)
                .donHangDangGiao(donDangGiao)
                .donHangDaHuy(donDaHuy)
                .tongKhachHang(tongKhachHang)
                .sanPhamSapHet(0L) // TODO: kết nối sau khi có API sản phẩm
                .build();
    }

    @Override
    public List<AdminThongKeResponse.DoanhThuNgay> getDoanhThuTheoNgay(LocalDate tuNgay, LocalDate denNgay) {
        // Mặc định: 30 ngày gần nhất
        if (tuNgay == null) tuNgay = LocalDate.now().minusDays(29);
        if (denNgay == null) denNgay = LocalDate.now();

        Long tuNgayMs = tuNgay.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        Long denNgayMs = denNgay.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

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
