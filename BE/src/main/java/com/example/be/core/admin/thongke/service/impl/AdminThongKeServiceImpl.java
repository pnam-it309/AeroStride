package com.example.be.core.admin.thongke.service.impl;

import com.example.be.core.admin.thongke.model.response.AdminThongKeResponse;
import com.example.be.core.admin.thongke.repository.AdminThongKeRepository;
import com.example.be.core.admin.thongke.repository.AdminThongKeSpecification;
import com.example.be.core.admin.thongke.service.AdminThongKeService;
import com.example.be.entity.HoaDon;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.utils.AccountUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
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
        Long donHoanThanh = thongKeRepository.countByTrangThai(4, tuNgayMs, denNgayMs); // OrderStatus.HOAN_THANH (ordinal: 4)
        Long donChoXacNhan = thongKeRepository.countByTrangThai(0, tuNgayMs, denNgayMs); // CHO_XAC_NHAN (ordinal: 0)
        Long donDangGiao = thongKeRepository.countByTrangThai(3, tuNgayMs, denNgayMs);  // DANG_GIAO (ordinal: 3)
        Long donDaHuy = thongKeRepository.countByTrangThai(5, tuNgayMs, denNgayMs);     // DA_HUY (ordinal: 5)

        Long tongKhachHang = thongKeRepository.count();

        // Top 5 sản phẩm bán chạy
        List<Object[]> topProdRows = thongKeRepository.getTopProductsData(tuNgayMs, denNgayMs, PageRequest.of(0, 5));
        List<AdminThongKeResponse.SanPhamBanChay> topProducts = new java.util.ArrayList<>();
        for (Object[] row : topProdRows) {
            topProducts.add(AdminThongKeResponse.SanPhamBanChay.builder()
                    .name(row[0] != null ? row[0].toString() : "")
                    .revenue(row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO)
                    .quantity(row[2] != null ? Long.parseLong(row[2].toString()) : 0L)
                    .growth(0.0) // default growth rate
                    .build());
        }

        // Doanh thu theo danh mục
        List<Object[]> catRows = thongKeRepository.getSalesByCategoryData(tuNgayMs, denNgayMs);
        List<AdminThongKeResponse.DoanhThuDanhMuc> salesByCategory = new java.util.ArrayList<>();
        BigDecimal totalCatRevenue = BigDecimal.ZERO;
        for (Object[] row : catRows) {
            if (row[1] != null) {
                totalCatRevenue = totalCatRevenue.add(new BigDecimal(row[1].toString()));
            }
        }
        for (Object[] row : catRows) {
            BigDecimal value = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            double percentage = 0.0;
            if (totalCatRevenue.compareTo(BigDecimal.ZERO) > 0) {
                percentage = value.multiply(new BigDecimal(100))
                        .divide(totalCatRevenue, 2, java.math.RoundingMode.HALF_UP)
                        .doubleValue();
            }
            salesByCategory.add(AdminThongKeResponse.DoanhThuDanhMuc.builder()
                    .name(row[0] != null ? row[0].toString() : "Khác")
                    .value(value)
                    .percentage(percentage)
                    .build());
        }

        return AdminThongKeResponse.builder()
                .tongDoanhThu(tongDoanhThu != null ? tongDoanhThu : BigDecimal.ZERO)
                .tongDonHang(tongDonHang != null ? tongDonHang : 0L)
                .donHangHoanThanh(donHoanThanh != null ? donHoanThanh : 0L)
                .donHangChoXacNhan(donChoXacNhan != null ? donChoXacNhan : 0L)
                .donHangDangGiao(donDangGiao != null ? donDangGiao : 0L)
                .donHangDaHuy(donDaHuy != null ? donDaHuy : 0L)
                .tongKhachHang(tongKhachHang)
                .sanPhamSapHet(0L)
                .topSanPhamBanChay(topProducts)
                .doanhThuTheoDanhMuc(salesByCategory)
                .build();
    }

    @Override
    public List<AdminThongKeResponse.DoanhThuNgay> getDoanhThuTheoNgay(LocalDate tuNgay, LocalDate denNgay) {
        if (tuNgay == null) tuNgay = LocalDate.now().minusDays(29);
        if (denNgay == null) denNgay = LocalDate.now();

        Long tuNgayMs = AccountUtils.parseDateToLong(tuNgay.toString(), false);
        Long denNgayMs = AccountUtils.parseDateToLong(denNgay.toString(), true);

        // Build Specification dynamically using clean criteria
        Specification<HoaDon> spec = Specification.where(AdminThongKeSpecification.hasTrangThai(OrderStatus.DANG_GIAO))
                .and(AdminThongKeSpecification.ngayTaoGreaterOrEqual(tuNgayMs))
                .and(AdminThongKeSpecification.ngayTaoLessOrEqual(denNgayMs));

        return thongKeRepository.getDoanhThuTheoNgay(spec);
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
