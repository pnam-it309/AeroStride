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
            LocalDate tuNgay, LocalDate denNgay, String keyword, String thuongHieuId, int page, int size, String sortBy) {

        Long tuNgayMs = AccountUtils.parseDateToLong(tuNgay != null ? tuNgay.toString() : null, false);
        Long denNgayMs = AccountUtils.parseDateToLong(denNgay != null ? denNgay.toString() : null, true);
        String kw = (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim();
        String brandId = (thuongHieuId == null || thuongHieuId.trim().isEmpty()) ? null : thuongHieuId.trim();
        String sort = (sortBy == null || sortBy.trim().isEmpty()) ? "bestSelling" : sortBy.trim();

        // Pass unsorted pageable to prevent Spring Data from appending unknown sort aliases to countQuery
        org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size);
        org.springframework.data.domain.Page<Object[]> pageData = thongKeRepository.getProductStatistics(tuNgayMs, denNgayMs, kw, brandId, sort, pageable);

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

    @Override
    public byte[] exportExcelBaoCao(LocalDate tuNgay, LocalDate denNgay) {
        AdminThongKeResponse overview = getTongQuan(tuNgay, denNgay);
        List<AdminThongKeResponse.DoanhThuNgay> dailyRevenues = getDoanhThuTheoNgay(tuNgay, denNgay);

        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {

            org.apache.poi.ss.usermodel.DataFormat dataFormat = workbook.createDataFormat();

            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 15);
            titleFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.DARK_BLUE.getIndex());

            org.apache.poi.ss.usermodel.Font sectionFont = workbook.createFont();
            sectionFont.setBold(true);
            sectionFont.setFontHeightInPoints((short) 11);
            sectionFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());

            org.apache.poi.ss.usermodel.CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            org.apache.poi.ss.usermodel.CellStyle subtitleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font subFont = workbook.createFont();
            subFont.setItalic(true);
            subFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_50_PERCENT.getIndex());
            subtitleStyle.setFont(subFont);

            org.apache.poi.ss.usermodel.CellStyle sectionStyle = workbook.createCellStyle();
            sectionStyle.setFont(sectionFont);
            sectionStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.ROYAL_BLUE.getIndex());
            sectionStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            sectionStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);

            org.apache.poi.ss.usermodel.CellStyle tableHeaderStyle = workbook.createCellStyle();
            tableHeaderStyle.setFont(headerFont);
            tableHeaderStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.DARK_BLUE.getIndex());
            tableHeaderStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            tableHeaderStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            tableHeaderStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            tableHeaderStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            tableHeaderStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            tableHeaderStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            tableHeaderStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

            org.apache.poi.ss.usermodel.CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            dataStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            dataStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            dataStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

            org.apache.poi.ss.usermodel.CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.cloneStyleFrom(dataStyle);
            centerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

            org.apache.poi.ss.usermodel.CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.cloneStyleFrom(dataStyle);
            currencyStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.RIGHT);
            currencyStyle.setDataFormat(dataFormat.getFormat("#,##0 \"₫\""));

            org.apache.poi.ss.usermodel.CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(dataStyle);
            numberStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.RIGHT);
            numberStyle.setDataFormat(dataFormat.getFormat("#,##0"));

            String timeRangeStr = String.format("Thời gian báo cáo: Từ %s đến %s (Xuất lúc: %s)",
                    tuNgay != null ? tuNgay.toString() : "Toàn bộ",
                    denNgay != null ? denNgay.toString() : "Hiện tại",
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

            // -------------------------------------------------------------
            // SHEET 1: TỔNG QUAN KINH DOANH
            // -------------------------------------------------------------
            org.apache.poi.ss.usermodel.Sheet sheet1 = workbook.createSheet("1. Tổng quan kinh doanh");
            sheet1.setDisplayGridlines(true);

            int r = 0;
            org.apache.poi.ss.usermodel.Row row = sheet1.createRow(r++);
            org.apache.poi.ss.usermodel.Cell cell = row.createCell(0);
            cell.setCellValue("BÁO CÁO THỐNG KÊ DOANH THU & KINH DOANH - AEROSTRIDE");
            cell.setCellStyle(titleStyle);

            row = sheet1.createRow(r++);
            cell = row.createCell(0);
            cell.setCellValue(timeRangeStr);
            cell.setCellStyle(subtitleStyle);
            r++;

            row = sheet1.createRow(r++);
            cell = row.createCell(0);
            cell.setCellValue("A. CHỈ SỐ KINH DOANH TỔNG QUAN");
            cell.setCellStyle(sectionStyle);
            sheet1.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(r - 1, r - 1, 0, 3));

            String[][] kpiData = {
                    {"Tổng doanh thu thực tế (Hoàn thành):", overview.getTongDoanhThu() != null ? overview.getTongDoanhThu().toString() : "0", "CURRENCY"},
                    {"Doanh thu Bán hàng tại quầy:", overview.getDoanhThuTaiQuay() != null ? overview.getDoanhThuTaiQuay().toString() : "0", "CURRENCY"},
                    {"Doanh thu Bán hàng trực tuyến (Online):", overview.getDoanhThuTrucTuyen() != null ? overview.getDoanhThuTrucTuyen().toString() : "0", "CURRENCY"},
                    {"Giá trị trung bình đơn hàng (AOV):", overview.getGiaTriTrungBinh() != null ? overview.getGiaTriTrungBinh().toString() : "0", "CURRENCY"},
                    {"Tổng số đơn hàng:", String.valueOf(overview.getTongDonHang() != null ? overview.getTongDonHang() : 0L), "NUMBER"},
                    {"Số đơn hàng hoàn thành:", String.valueOf(overview.getDonHangHoanThanh() != null ? overview.getDonHangHoanThanh() : 0L), "NUMBER"},
                    {"Số đơn hàng tại quầy:", String.valueOf(overview.getDonTaiQuay() != null ? overview.getDonTaiQuay() : 0L), "NUMBER"},
                    {"Số đơn hàng trực tuyến:", String.valueOf(overview.getDonTrucTuyen() != null ? overview.getDonTrucTuyen() : 0L), "NUMBER"},
                    {"Số đơn hàng đang giao:", String.valueOf(overview.getDonHangDangGiao() != null ? overview.getDonHangDangGiao() : 0L), "NUMBER"},
                    {"Số đơn hàng chờ xác nhận:", String.valueOf(overview.getDonHangChoXacNhan() != null ? overview.getDonHangChoXacNhan() : 0L), "NUMBER"},
                    {"Số đơn hàng đã hủy:", String.valueOf(overview.getDonHangDaHuy() != null ? overview.getDonHangDaHuy() : 0L), "NUMBER"},
                    {"Số đơn hàng hoàn:", String.valueOf(overview.getDonHangHoan() != null ? overview.getDonHangHoan() : 0L), "NUMBER"},
                    {"Tổng khách hàng trong hệ thống:", String.valueOf(overview.getTongKhachHang() != null ? overview.getTongKhachHang() : 0L), "NUMBER"},
                    {"Tổng số sản phẩm:", String.valueOf(overview.getTongSanPham() != null ? overview.getTongSanPham() : 0L), "NUMBER"}
            };

            for (String[] kpi : kpiData) {
                row = sheet1.createRow(r++);
                org.apache.poi.ss.usermodel.Cell labelCell = row.createCell(0);
                labelCell.setCellValue(kpi[0]);
                labelCell.setCellStyle(dataStyle);

                org.apache.poi.ss.usermodel.Cell valCell = row.createCell(1);
                if ("CURRENCY".equals(kpi[2])) {
                    valCell.setCellValue(Double.parseDouble(kpi[1]));
                    valCell.setCellStyle(currencyStyle);
                } else {
                    valCell.setCellValue(Double.parseDouble(kpi[1]));
                    valCell.setCellStyle(numberStyle);
                }
            }

            r++;
            row = sheet1.createRow(r++);
            cell = row.createCell(0);
            cell.setCellValue("B. DOANH THU THEO THƯƠNG HIỆU");
            cell.setCellStyle(sectionStyle);
            sheet1.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(r - 1, r - 1, 0, 3));

            row = sheet1.createRow(r++);
            String[] brandHeaders = {"STT", "Thương hiệu", "Doanh thu (VNĐ)"};
            for (int i = 0; i < brandHeaders.length; i++) {
                cell = row.createCell(i);
                cell.setCellValue(brandHeaders[i]);
                cell.setCellStyle(tableHeaderStyle);
            }

            int brandIdx = 1;
            if (overview.getTyTrongTheoThuongHieu() != null) {
                for (AdminThongKeResponse.TyTrongThuongHieu b : overview.getTyTrongTheoThuongHieu()) {
                    row = sheet1.createRow(r++);
                    cell = row.createCell(0);
                    cell.setCellValue(brandIdx++);
                    cell.setCellStyle(centerStyle);

                    cell = row.createCell(1);
                    cell.setCellValue(b.getName() != null ? b.getName() : "Khác");
                    cell.setCellStyle(dataStyle);

                    cell = row.createCell(2);
                    cell.setCellValue(b.getRevenue() != null ? b.getRevenue().doubleValue() : 0.0);
                    cell.setCellStyle(currencyStyle);
                }
            }

            for (int i = 0; i < 4; i++) {
                sheet1.autoSizeColumn(i);
            }

            // -------------------------------------------------------------
            // SHEET 2: DOANH THU THEO NGÀY
            // -------------------------------------------------------------
            org.apache.poi.ss.usermodel.Sheet sheet2 = workbook.createSheet("2. Doanh thu theo ngày");
            sheet2.setDisplayGridlines(true);
            int r2 = 0;

            row = sheet2.createRow(r2++);
            cell = row.createCell(0);
            cell.setCellValue("CHI TIẾT DOANH THU THEO NGÀY");
            cell.setCellStyle(titleStyle);

            row = sheet2.createRow(r2++);
            cell = row.createCell(0);
            cell.setCellValue(timeRangeStr);
            cell.setCellStyle(subtitleStyle);
            r2++;

            row = sheet2.createRow(r2++);
            String[] dailyHeaders = {"STT", "Ngày", "Doanh thu (VNĐ)", "Số đơn hàng", "Doanh thu TB / Đơn"};
            for (int i = 0; i < dailyHeaders.length; i++) {
                cell = row.createCell(i);
                cell.setCellValue(dailyHeaders[i]);
                cell.setCellStyle(tableHeaderStyle);
            }

            int dayIdx = 1;
            if (dailyRevenues != null) {
                for (AdminThongKeResponse.DoanhThuNgay d : dailyRevenues) {
                    row = sheet2.createRow(r2++);
                    cell = row.createCell(0);
                    cell.setCellValue(dayIdx++);
                    cell.setCellStyle(centerStyle);

                    cell = row.createCell(1);
                    cell.setCellValue(d.getNgay() != null ? d.getNgay() : "");
                    cell.setCellStyle(centerStyle);

                    cell = row.createCell(2);
                    double rev = d.getDoanhThu() != null ? d.getDoanhThu().doubleValue() : 0.0;
                    cell.setCellValue(rev);
                    cell.setCellStyle(currencyStyle);

                    cell = row.createCell(3);
                    long orders = d.getSoDon() != null ? d.getSoDon() : 0L;
                    cell.setCellValue(orders);
                    cell.setCellStyle(numberStyle);

                    cell = row.createCell(4);
                    double avg = orders > 0 ? (rev / orders) : 0.0;
                    cell.setCellValue(avg);
                    cell.setCellStyle(currencyStyle);
                }
            }

            for (int i = 0; i < dailyHeaders.length; i++) {
                sheet2.autoSizeColumn(i);
            }

            // -------------------------------------------------------------
            // SHEET 3: TOP SẢN PHẨM BÁN CHẠY
            // -------------------------------------------------------------
            org.apache.poi.ss.usermodel.Sheet sheet3 = workbook.createSheet("3. Sản phẩm bán chạy");
            sheet3.setDisplayGridlines(true);
            int r3 = 0;

            row = sheet3.createRow(r3++);
            cell = row.createCell(0);
            cell.setCellValue("TOP SẢN PHẨM BÁN CHẠY VÀ DOANH THU");
            cell.setCellStyle(titleStyle);

            row = sheet3.createRow(r3++);
            cell = row.createCell(0);
            cell.setCellValue(timeRangeStr);
            cell.setCellStyle(subtitleStyle);
            r3++;

            row = sheet3.createRow(r3++);
            String[] prodHeaders = {"STT", "Mã sản phẩm", "Tên sản phẩm", "Thương hiệu", "Số lượng bán ra", "Doanh thu (VNĐ)"};
            for (int i = 0; i < prodHeaders.length; i++) {
                cell = row.createCell(i);
                cell.setCellValue(prodHeaders[i]);
                cell.setCellStyle(tableHeaderStyle);
            }

            int prodIdx = 1;
            if (overview.getTopSanPhamBanChay() != null) {
                for (AdminThongKeResponse.SanPhamBanChay p : overview.getTopSanPhamBanChay()) {
                    row = sheet3.createRow(r3++);
                    cell = row.createCell(0);
                    cell.setCellValue(prodIdx++);
                    cell.setCellStyle(centerStyle);

                    cell = row.createCell(1);
                    cell.setCellValue(p.getMaSanPham() != null ? p.getMaSanPham() : "");
                    cell.setCellStyle(centerStyle);

                    cell = row.createCell(2);
                    cell.setCellValue(p.getName() != null ? p.getName() : "");
                    cell.setCellStyle(dataStyle);

                    cell = row.createCell(3);
                    cell.setCellValue(p.getThuongHieu() != null ? p.getThuongHieu() : "");
                    cell.setCellStyle(centerStyle);

                    cell = row.createCell(4);
                    cell.setCellValue(p.getQuantity() != null ? p.getQuantity() : 0L);
                    cell.setCellStyle(numberStyle);

                    cell = row.createCell(5);
                    cell.setCellValue(p.getRevenue() != null ? p.getRevenue().doubleValue() : 0.0);
                    cell.setCellStyle(currencyStyle);
                }
            }

            for (int i = 0; i < prodHeaders.length; i++) {
                sheet3.autoSizeColumn(i);
            }

            // -------------------------------------------------------------
            // SHEET 4: TOP NHÂN VIÊN & KHÁCH HÀNG
            // -------------------------------------------------------------
            org.apache.poi.ss.usermodel.Sheet sheet4 = workbook.createSheet("4. Nhân viên & Khách hàng");
            sheet4.setDisplayGridlines(true);
            int r4 = 0;

            row = sheet4.createRow(r4++);
            cell = row.createCell(0);
            cell.setCellValue("HIỆU SUẤT NHÂN VIÊN VÀ KHÁCH HÀNG TIÊU BIỂU");
            cell.setCellStyle(titleStyle);

            row = sheet4.createRow(r4++);
            cell = row.createCell(0);
            cell.setCellValue(timeRangeStr);
            cell.setCellStyle(subtitleStyle);
            r4++;

            // Bảng 1: Nhân viên
            row = sheet4.createRow(r4++);
            cell = row.createCell(0);
            cell.setCellValue("A. TOP NHÂN VIÊN BÁN HÀNG XUẤT SẮC");
            cell.setCellStyle(sectionStyle);
            sheet4.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(r4 - 1, r4 - 1, 0, 5));

            row = sheet4.createRow(r4++);
            String[] empHeaders = {"STT", "Mã nhân viên", "Tên nhân viên", "Doanh số bán (VNĐ)", "Sản phẩm bán", "Tổng số đơn"};
            for (int i = 0; i < empHeaders.length; i++) {
                cell = row.createCell(i);
                cell.setCellValue(empHeaders[i]);
                cell.setCellStyle(tableHeaderStyle);
            }

            int empIdx = 1;
            if (overview.getTopNhanVien() != null) {
                for (AdminThongKeResponse.NhanVienThongKe emp : overview.getTopNhanVien()) {
                    row = sheet4.createRow(r4++);
                    cell = row.createCell(0);
                    cell.setCellValue(empIdx++);
                    cell.setCellStyle(centerStyle);

                    cell = row.createCell(1);
                    cell.setCellValue(emp.getMaNhanVien() != null ? emp.getMaNhanVien() : "");
                    cell.setCellStyle(centerStyle);

                    cell = row.createCell(2);
                    cell.setCellValue(emp.getTenNhanVien() != null ? emp.getTenNhanVien() : "");
                    cell.setCellStyle(dataStyle);

                    cell = row.createCell(3);
                    cell.setCellValue(emp.getTongChi() != null ? emp.getTongChi().doubleValue() : 0.0);
                    cell.setCellStyle(currencyStyle);

                    cell = row.createCell(4);
                    cell.setCellValue(emp.getTongSanPham() != null ? emp.getTongSanPham() : 0L);
                    cell.setCellStyle(numberStyle);

                    cell = row.createCell(5);
                    cell.setCellValue(emp.getTongDonHang() != null ? emp.getTongDonHang() : 0L);
                    cell.setCellStyle(numberStyle);
                }
            }

            r4++;
            row = sheet4.createRow(r4++);
            cell = row.createCell(0);
            cell.setCellValue("B. TOP KHÁCH HÀNG THÂN THIẾT");
            cell.setCellStyle(sectionStyle);
            sheet4.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(r4 - 1, r4 - 1, 0, 5));

            row = sheet4.createRow(r4++);
            String[] custHeaders = {"STT", "Tên khách hàng", "Tổng chi tiêu (VNĐ)", "Sản phẩm đã mua", "Đơn thành công", "Đơn hoàn"};
            for (int i = 0; i < custHeaders.length; i++) {
                cell = row.createCell(i);
                cell.setCellValue(custHeaders[i]);
                cell.setCellStyle(tableHeaderStyle);
            }

            int custIdx = 1;
            if (overview.getTopKhachHang() != null) {
                for (AdminThongKeResponse.KhachHangThongKe cust : overview.getTopKhachHang()) {
                    row = sheet4.createRow(r4++);
                    cell = row.createCell(0);
                    cell.setCellValue(custIdx++);
                    cell.setCellStyle(centerStyle);

                    cell = row.createCell(1);
                    cell.setCellValue(cust.getTenKhachHang() != null ? cust.getTenKhachHang() : "Khách lẻ");
                    cell.setCellStyle(dataStyle);

                    cell = row.createCell(2);
                    cell.setCellValue(cust.getTongChi() != null ? cust.getTongChi().doubleValue() : 0.0);
                    cell.setCellStyle(currencyStyle);

                    cell = row.createCell(3);
                    cell.setCellValue(cust.getTongSanPham() != null ? cust.getTongSanPham() : 0L);
                    cell.setCellStyle(numberStyle);

                    cell = row.createCell(4);
                    cell.setCellValue(cust.getDonThanhCong() != null ? cust.getDonThanhCong() : 0L);
                    cell.setCellStyle(numberStyle);

                    cell = row.createCell(5);
                    cell.setCellValue(cust.getDonHoan() != null ? cust.getDonHoan() : 0L);
                    cell.setCellStyle(numberStyle);
                }
            }

            for (int i = 0; i < custHeaders.length; i++) {
                sheet4.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new com.example.be.infrastructure.exceptions.SystemException("Lỗi khi xuất file Excel báo cáo thống kê: " + e.getMessage());
        }
    }
}
