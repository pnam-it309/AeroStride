package com.example.be.core.admin.thongke.service.impl;

import com.example.be.core.admin.thongke.model.response.AdminThongKeResponse;
import com.example.be.core.admin.thongke.repository.AdminThongKeRepository;
import com.example.be.core.admin.thongke.repository.AdminThongKeSpecification;
import com.example.be.core.admin.thongke.service.AdminThongKeService;
import com.example.be.entity.HoaDon;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.repository.KhachHangRepository;
import com.example.be.repository.SanPhamRepository;
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
    private final KhachHangRepository khachHangRepository;
    private final SanPhamRepository sanPhamRepository;

    @Override
    public AdminThongKeResponse getTongQuan(LocalDate tuNgay, LocalDate denNgay) {
        Long tuNgayMs = AccountUtils.parseDateToLong(tuNgay != null ? tuNgay.toString() : null, false);
        Long denNgayMs = AccountUtils.parseDateToLong(denNgay != null ? denNgay.toString() : null, true);

        List<Object[]> stats = thongKeRepository.getOverviewStats(tuNgayMs, denNgayMs);
        Object[] overviewRow = stats != null && !stats.isEmpty() ? stats.get(0) : new Object[10];

        BigDecimal tongDoanhThu = overviewRow[0] != null ? new BigDecimal(overviewRow[0].toString()) : BigDecimal.ZERO;
        Long tongDonHang = overviewRow[1] != null ? Long.parseLong(overviewRow[1].toString()) : 0L;
        Long donHoanThanh = overviewRow[2] != null ? Long.parseLong(overviewRow[2].toString()) : 0L;
        Long donChoXacNhan = overviewRow[3] != null ? Long.parseLong(overviewRow[3].toString()) : 0L;
        Long donDangGiao = overviewRow[4] != null ? Long.parseLong(overviewRow[4].toString()) : 0L;
        Long donDaHuy = overviewRow[5] != null ? Long.parseLong(overviewRow[5].toString()) : 0L;
        Long donHoan = overviewRow[6] != null ? Long.parseLong(overviewRow[6].toString()) : 0L;
        BigDecimal dtChoXacNhan = overviewRow[7] != null ? new BigDecimal(overviewRow[7].toString()) : BigDecimal.ZERO;
        BigDecimal dtDangGiao = overviewRow[8] != null ? new BigDecimal(overviewRow[8].toString()) : BigDecimal.ZERO;
        BigDecimal dtDaHuy = overviewRow[9] != null ? new BigDecimal(overviewRow[9].toString()) : BigDecimal.ZERO;

        List<Object[]> orderTypeStats = thongKeRepository.getOrderTypeStats(tuNgayMs, denNgayMs);
        Object[] orderTypeRow = orderTypeStats != null && !orderTypeStats.isEmpty() ? orderTypeStats.get(0) : new Object[4];
        BigDecimal doanhThuTaiQuay = orderTypeRow[0] != null ? new BigDecimal(orderTypeRow[0].toString()) : BigDecimal.ZERO;
        Long donTaiQuay = orderTypeRow[1] != null ? Long.parseLong(orderTypeRow[1].toString()) : 0L;
        BigDecimal doanhThuTrucTuyen = orderTypeRow[2] != null ? new BigDecimal(orderTypeRow[2].toString()) : BigDecimal.ZERO;
        Long donTrucTuyen = orderTypeRow[3] != null ? Long.parseLong(orderTypeRow[3].toString()) : 0L;

        Long tongKhachHang = khachHangRepository.count();
        Long tongSanPham = sanPhamRepository.count();

        // Lấy đủ dữ liệu bán sản phẩm để FE ghép chính xác vào bảng thống kê sản phẩm.
        List<Object[]> topProdRows = thongKeRepository.getTopProductsData(tuNgayMs, denNgayMs, PageRequest.of(0, 1000));
        List<AdminThongKeResponse.SanPhamBanChay> topProducts = new java.util.ArrayList<>();
        for (Object[] row : topProdRows) {
            topProducts.add(AdminThongKeResponse.SanPhamBanChay.builder()
                    .maSanPham(row[0] != null ? row[0].toString() : "")
                    .name(row[1] != null ? row[1].toString() : "")
                    .thuongHieu(row[2] != null ? row[2].toString() : "")
                    .revenue(row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO)
                    .quantity(row[4] != null ? Long.parseLong(row[4].toString()) : 0L)
                    .growth(0.0) // default growth rate
                    .build());
        }

        List<Object[]> brandRows = thongKeRepository.getBrandRevenueData(tuNgayMs, denNgayMs);
        List<AdminThongKeResponse.TyTrongThuongHieu> brandShares = new java.util.ArrayList<>();
        for (Object[] row : brandRows) {
            brandShares.add(AdminThongKeResponse.TyTrongThuongHieu.builder()
                    .name(row[0] != null ? row[0].toString() : "Khác")
                    .revenue(row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO)
                    .build());
        }
        int targetYear = denNgay != null ? denNgay.getYear() : (tuNgay != null ? tuNgay.getYear() : java.time.LocalDate.now().getYear());
        Long startOfYearMs = AccountUtils.parseDateToLong(java.time.LocalDate.of(targetYear, 1, 1).toString(), false);
        Long endOfYearMs = AccountUtils.parseDateToLong(java.time.LocalDate.of(targetYear, 12, 31).toString(), true);
        List<Object[]> custRows = thongKeRepository.getCustomerPurchaseStats(startOfYearMs, endOfYearMs);
        List<AdminThongKeResponse.KhachHangThongKe> topCustomers = new java.util.ArrayList<>();
        for (Object[] row : custRows) {
            BigDecimal customerTongChi = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            Long customerTongSanPham = row[2] != null ? Long.parseLong(row[2].toString()) : 0L;
            Long customerDonThanhCong = row[3] != null ? Long.parseLong(row[3].toString()) : 0L;
            Long customerDonHoan = row[4] != null ? Long.parseLong(row[4].toString()) : 0L;

            topCustomers.add(AdminThongKeResponse.KhachHangThongKe.builder()
                    .tenKhachHang(row[0] != null ? row[0].toString() : "Khách lẻ")
                    .tongChi(customerTongChi)
                    .tongSanPham(customerTongSanPham)
                    .donThanhCong(customerDonThanhCong)
                    .donHoan(customerDonHoan)
                    .build());
        }

        List<Object[]> empRows = thongKeRepository.getEmployeeRevenueStats(tuNgayMs, denNgayMs);
        List<AdminThongKeResponse.NhanVienThongKe> topEmployees = new java.util.ArrayList<>();
        for (Object[] row : empRows) {
            BigDecimal empTongDoanhThu = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
            Long empTongSanPham = row[3] != null ? Long.parseLong(row[3].toString()) : 0L;
            Long empTongDonHang = row[4] != null ? Long.parseLong(row[4].toString()) : 0L;

            topEmployees.add(AdminThongKeResponse.NhanVienThongKe.builder()
                    .maNhanVien(row[0] != null ? row[0].toString() : "")
                    .tenNhanVien(row[1] != null ? row[1].toString() : "")
                    .tongChi(empTongDoanhThu)
                    .tongSanPham(empTongSanPham)
                    .tongDonHang(empTongDonHang)
                    .build());
        }

        BigDecimal giaTriTrungBinh = donHoanThanh > 0
                ? tongDoanhThu.divide(BigDecimal.valueOf(donHoanThanh), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Chu kỳ doanh thu: Hôm nay, Tuần này, Tháng này, Năm này
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate startOfMonth = today.with(java.time.temporal.TemporalAdjusters.firstDayOfMonth());
        LocalDate startOfYear = today.with(java.time.temporal.TemporalAdjusters.firstDayOfYear());

        List<AdminThongKeResponse.ChuKyDoanhThu> chuKyList = new java.util.ArrayList<>();
        chuKyList.add(getChuKyStats("Hôm nay", today, today));
        chuKyList.add(getChuKyStats("Tuần này", startOfWeek, today));
        chuKyList.add(getChuKyStats("Tháng này", startOfMonth, today));
        chuKyList.add(getChuKyStats("Năm này", startOfYear, today));

        return AdminThongKeResponse.builder()
                .tongDoanhThu(tongDoanhThu != null ? tongDoanhThu : BigDecimal.ZERO)
                .tongDonHang(tongDonHang != null ? tongDonHang : 0L)
                .donHangHoanThanh(donHoanThanh != null ? donHoanThanh : 0L)
                .donHangChoXacNhan(donChoXacNhan != null ? donChoXacNhan : 0L)
                .donHangDangGiao(donDangGiao != null ? donDangGiao : 0L)
                .donHangDaHuy(donDaHuy != null ? donDaHuy : 0L)
                .donHangHoan(donHoan != null ? donHoan : 0L)
                .doanhThuChoXacNhan(dtChoXacNhan)
                .doanhThuDangGiao(dtDangGiao)
                .doanhThuDaHuy(dtDaHuy)
                .tongKhachHang(tongKhachHang)
                .tongSanPham(tongSanPham)
                .doanhThuTaiQuay(doanhThuTaiQuay)
                .doanhThuTrucTuyen(doanhThuTrucTuyen)
                .donTaiQuay(donTaiQuay)
                .donTrucTuyen(donTrucTuyen)
                .giaTriTrungBinh(giaTriTrungBinh)
                .sanPhamSapHet(0L)
                .topSanPhamBanChay(topProducts)
                .tyTrongTheoThuongHieu(brandShares)
                .topKhachHang(topCustomers)
                .topNhanVien(topEmployees)
                .chuKyDoanhThu(chuKyList)
                .build();
    }

    private AdminThongKeResponse.ChuKyDoanhThu getChuKyStats(String label, LocalDate start, LocalDate end) {
        Long startMs = AccountUtils.parseDateToLong(start.toString(), false);
        Long endMs = AccountUtils.parseDateToLong(end.toString(), true);
        List<Object[]> result = thongKeRepository.getRevenueCycleStats(startMs, endMs);
        
        BigDecimal revenue = BigDecimal.ZERO;
        Long count = 0L;
        if (result != null && !result.isEmpty()) {
            Object[] row = result.get(0);
            revenue = row[0] != null ? new BigDecimal(row[0].toString()) : BigDecimal.ZERO;
            count = row[1] != null ? Long.parseLong(row[1].toString()) : 0L;
        }
        
        BigDecimal avg = count > 0 
            ? revenue.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
            
        return AdminThongKeResponse.ChuKyDoanhThu.builder()
            .tenChuKy(label)
            .doanhThu(revenue)
            .soDon(count)
            .trungBinhDon(avg)
            .build();
    }

    @Override
    public List<AdminThongKeResponse.DoanhThuNgay> getDoanhThuTheoNgay(LocalDate tuNgay, LocalDate denNgay) {
        if (tuNgay == null) tuNgay = LocalDate.now().minusDays(29);
        if (denNgay == null) denNgay = LocalDate.now();

        Long tuNgayMs = AccountUtils.parseDateToLong(tuNgay.toString(), false);
        Long denNgayMs = AccountUtils.parseDateToLong(denNgay.toString(), true);

        // Build Specification dynamically using clean criteria
        Specification<HoaDon> spec = Specification.where(AdminThongKeSpecification.hasTrangThai(OrderStatus.HOAN_THANH))
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

    @Override
    public com.example.be.core.common.dto.PageResponse<AdminThongKeResponse.SanPhamBanChay> getProductStatistics(
            LocalDate tuNgay, LocalDate denNgay, String keyword, int page, int size, String sortBy) {

        Long tuNgayMs = AccountUtils.parseDateToLong(tuNgay != null ? tuNgay.toString() : null, false);
        Long denNgayMs = AccountUtils.parseDateToLong(denNgay != null ? denNgay.toString() : null, true);
        String kw = (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim();
        String sort = (sortBy == null || sortBy.trim().isEmpty()) ? "bestSelling" : sortBy.trim();

        // Pass unsorted pageable to prevent Spring Data from appending unknown sort aliases to countQuery
        org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size);
        org.springframework.data.domain.Page<Object[]> pageData = thongKeRepository.getProductStatistics(tuNgayMs, denNgayMs, kw, sort, pageable);

        List<AdminThongKeResponse.SanPhamBanChay> dtos = new ArrayList<>();
        for (Object[] row : pageData.getContent()) {
            dtos.add(AdminThongKeResponse.SanPhamBanChay.builder()
                    .maSanPham(row[0] != null ? row[0].toString() : "")
                    .name(row[1] != null ? row[1].toString() : "")
                    .thuongHieu(row[2] != null ? row[2].toString() : "")
                    .revenue(row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO)
                    .quantity(row[4] != null ? Long.parseLong(row[4].toString()) : 0L)
                    .growth(0.0)
                    .build());
        }

        return com.example.be.core.common.dto.PageResponse.<AdminThongKeResponse.SanPhamBanChay>builder()
                .content(dtos)
                .pageNumber(pageData.getNumber())
                .pageSize(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .last(pageData.isLast())
                .build();
    }
}
