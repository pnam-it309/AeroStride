package com.example.be.core.admin.hoadon.service.impl;

import com.example.be.core.admin.hoadon.model.request.AdminHoaDonRequest;
import com.example.be.core.admin.hoadon.model.response.AdminHoaDonResponse;
import com.example.be.core.admin.hoadon.repository.AdminHoaDonRepository;
import com.example.be.core.admin.hoadon.service.AdminHoaDonService;
import com.example.be.entity.HoaDon;
import com.example.be.entity.LichSuTrangThaiHoaDon;
import com.example.be.entity.KhachHang;
import com.example.be.core.admin.hoadon.model.request.AdminUpdateHoaDonRequest;
import com.example.be.core.admin.hoadon.model.request.AdminUpdateHdctRequest;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.repository.ChiTietSanPhamRepository;
import com.example.be.repository.LichSuTrangThaiHoaDonRepository;
import com.example.be.repository.KhachHangRepository;
import com.example.be.utils.ExcelUtils;
import com.example.be.repository.HoaDonChiTietRepository;
import com.example.be.entity.HoaDonChiTiet;
import com.example.be.entity.ChiTietSanPham;
import com.example.be.utils.HelperUtils;
import com.example.be.utils.MaGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.List;

@Service
public class AdminHoaDonServiceImpl implements AdminHoaDonService {

    @Autowired
    private AdminHoaDonRepository repository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    private LichSuTrangThaiHoaDonRepository lichSuTrangThaiHoaDonRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private TemplateEngine templateEngine;

    @Override
    public Page<AdminHoaDonResponse> getPage(AdminHoaDonRequest req) {
        // 1. Chuẩn hóa dữ liệu lọc và convert ngày
        prepareRequest(req);

        // 2. Phân trang và Sắp xếp
        Sort sort = "ASC".equalsIgnoreCase(req.getSortDirection())
            ? Sort.by("ngayTao").ascending()
            : Sort.by("ngayTao").descending();
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), sort);

        // 3. Trả thẳng kết quả từ Repository về (Hết sạch lỗi đỏ!)
        return repository.getAllHoaDon(pageable, req);
    }

    private void prepareRequest(AdminHoaDonRequest req) {
        if (req.getSearch() != null && req.getSearch().trim().isEmpty()) req.setSearch(null);
        if (req.getTenKhachHang() != null && req.getTenKhachHang().trim().isEmpty()) req.setTenKhachHang(null);
        if (req.getLoaiDon() != null && req.getLoaiDon().trim().isEmpty()) req.setLoaiDon(null);
        if (req.getNgayTao() != null && req.getNgayTao().trim().isEmpty()) req.setNgayTao(null);
        
        String tuNgay = req.getTuNgay();
        String denNgay = req.getDenNgay();

        if (tuNgay != null && !tuNgay.trim().isEmpty()) {
            try {
                java.time.LocalDate date = java.time.LocalDate.parse(tuNgay.trim());
                req.setTuNgayLong(date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            } catch (Exception e) {
                req.setTuNgayLong(null);
            }
        } else {
            req.setTuNgayLong(null);
        }

        if (denNgay != null && !denNgay.trim().isEmpty()) {
            try {
                java.time.LocalDate date = java.time.LocalDate.parse(denNgay.trim());
                req.setDenNgayLong(date.atTime(23, 59, 59, 999).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            } catch (Exception e) {
                req.setDenNgayLong(null);
            }
        } else {
            req.setDenNgayLong(null);
        }
    }

    @Override
    public HoaDon detail(String id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public HoaDon updateStatus(String id, Integer status) {
        HoaDon hd = repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        
        OrderStatus oldStatus = hd.getTrangThai();
        OrderStatus newStatus = null;
        
        if (status >= 0 && status < OrderStatus.values().length) {
            newStatus = OrderStatus.values()[status];
        } else {
            throw new RuntimeException("Trạng thái không hợp lệ");
        }

        if (oldStatus == newStatus) return hd;

        // 1. Cập nhật trạng thái hóa đơn
        hd.setTrangThai(newStatus);
        hd.setNgayCapNhat(System.currentTimeMillis());
        repository.save(hd);

        // 2. Ghi lại lịch sử thay đổi
        LichSuTrangThaiHoaDon history = LichSuTrangThaiHoaDon.builder()
                .hoaDon(hd)
                .trangThaiCu(oldStatus != null ? oldStatus.ordinal() : null)
                .trangThaiMoi(newStatus.ordinal())
                .ghiChu("Cập nhật trạng thái từ hệ thống quản trị")
                .nguoiThucHien("ADMIN") // Thường lấy từ SecurityContext
                .build();
        history.setNgayTao(System.currentTimeMillis());
        lichSuTrangThaiHoaDonRepository.save(history);

        // 3. Xử lý logic nghiệp vụ đi kèm (CMS Standard)
        // Nếu hủy đơn hàng (CANCELLED), hoàn lại tồn kho
        if (newStatus == OrderStatus.CANCELLED) {
            List<HoaDonChiTiet> details = hoaDonChiTietRepository.findAllByHoaDon(hd);
            for (HoaDonChiTiet detail : details) {
                ChiTietSanPham ct = detail.getChiTietSanPham();
                if (ct != null) {
                    ct.setSoLuong(ct.getSoLuong() + detail.getSoLuong());
                    chiTietSanPhamRepository.save(ct);
                }
            }
        }

        return hd;
    }

    @Override
    @Transactional
    public HoaDon updateInfo(String id, AdminUpdateHoaDonRequest request) {
        HoaDon hd = repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        
        hd.setSoDienThoaiNguoiNhan(request.getSoDienThoaiNguoiNhan());
        hd.setDiaChiNguoiNhan(request.getDiaChiNguoiNhan());
        hd.setGhiChu(request.getGhiChu());
        
        if (request.getIdKhachHang() != null) {
            KhachHang kh = khachHangRepository.findById(request.getIdKhachHang()).orElse(null);
            hd.setKhachHang(kh);
        }
        
        hd.setNgayCapNhat(System.currentTimeMillis());
        
        // Ghi lại lịch sử
        logHistory(hd, "Cập nhật thông tin giao hàng/khách hàng");
        
        return repository.save(hd);
    }

    @Override
    @Transactional
    public HoaDon updateHdct(String id, AdminUpdateHdctRequest request) {
        HoaDon hd = repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        ChiTietSanPham ctsp = chiTietSanPhamRepository.findById(request.getIdChiTietSanPham())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm chi tiết"));

        HoaDonChiTiet hdct = hoaDonChiTietRepository.findByHoaDonAndChiTietSanPham(hd, ctsp);
        
        if (hdct != null) {
            // Cập nhật số lượng
            hdct.setSoLuong(request.getSoLuong());
        } else {
            // Thêm mới
            hdct = HoaDonChiTiet.builder()
                    .hoaDon(hd)
                    .chiTietSanPham(ctsp)
                    .soLuong(request.getSoLuong())
                    .donGia(ctsp.getGiaBan())
                    .build();
            hdct.setId(HelperUtils.generateUUID());
            hdct.setMaHoaDonChiTiet(MaGenerator.generate(HoaDonChiTiet.class));
            hdct.setNgayTao(System.currentTimeMillis());
        }
        
        hoaDonChiTietRepository.save(hdct);
        
        // Ghi lại lịch sử
        logHistory(hd, "Thay đổi sản phẩm: " + ctsp.getSanPham().getTen() + " (SL: " + request.getSoLuong() + ")");
        
        return recalculateTotal(hd);
    }

    @Override
    @Transactional
    public HoaDon removeHdct(String id, String idHdct) {
        HoaDon hd = repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        hoaDonChiTietRepository.deleteById(idHdct);
        
        logHistory(hd, "Xóa sản phẩm khỏi hóa đơn");
        
        return recalculateTotal(hd);
    }

    private void logHistory(HoaDon hd, String note) {
        LichSuTrangThaiHoaDon history = LichSuTrangThaiHoaDon.builder()
                .hoaDon(hd)
                .trangThaiMoi(hd.getTrangThai().ordinal())
                .trangThaiCu(hd.getTrangThai().ordinal())
                .ghiChu(note)
                .nguoiThucHien("ADMIN")
                .build();
        history.setNgayTao(System.currentTimeMillis());
        lichSuTrangThaiHoaDonRepository.save(history);
    }

    private HoaDon recalculateTotal(HoaDon hd) {
        List<HoaDonChiTiet> details = hoaDonChiTietRepository.findAllByHoaDon(hd);
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (HoaDonChiTiet d : details) {
            total = total.add(d.getDonGia().multiply(java.math.BigDecimal.valueOf(d.getSoLuong())));
        }
        hd.setTongTien(total);
        
        // Hiện tại tạm thời set sau giảm = tổng tiền nếu chưa có logic voucher phức tạp ở đây
        // Hoặc bạn có thể giữ nguyên logic giảm giá cũ nếu cần
        hd.setTongTienSauGiam(total); 
        
        hd.setNgayCapNhat(System.currentTimeMillis());
        return repository.save(hd);
    }

    @Override
    public java.util.Map<String, Long> getCounts(AdminHoaDonRequest req) {
        // Chuẩn hóa bộ lọc cho count
        prepareRequest(req);
        
        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        counts.put("all", repository.countWithFilter(req));

        java.util.List<java.util.Map<String, Object>> data = repository.countByTrangThai(req);
        for (java.util.Map<String, Object> map : data) {
            String status = String.valueOf(map.get("status"));
            Long count = (Long) map.get("count");
            counts.put(status, count);
        }
        return counts;
    }

    @Override
    public byte[] exportExcel(AdminHoaDonRequest req) {
        // Chuẩn hóa bộ lọc trước khi lấy dữ liệu xuất
        prepareRequest(req);
        
        // Fetch all data without pagination for export
        Sort sort = "ASC".equalsIgnoreCase(req.getSortDirection())
            ? Sort.by("ngayTao").ascending()
            : Sort.by("ngayTao").descending();
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, sort);
        List<AdminHoaDonResponse> data = repository.getAllHoaDon(pageable, req).getContent();

        String[] headers = {"STT", "Mã HĐ", "Khách hàng", "SĐT", "Loại đơn", "Ngày tạo", "Tổng tiền", "Trạng thái"};

        try {
            return ExcelUtils.exportToExcel("Danh sách hóa đơn", headers, data, item -> new Object[]{
                data.indexOf(item) + 1,
                item.getMaHoaDon(),
                item.getTenKhachHang() != null ? item.getTenKhachHang() : "Khách lẻ",
                item.getSoDienThoai() != null ? item.getSoDienThoai() : "-",
                item.getLoaiDon(),
                item.getNgayTao(),
                item.getTongTien(),
                item.getTrangThai()
            });
        } catch (IOException e) {
            throw new RuntimeException("Lỗi xuất file Excel: " + e.getMessage());
        }
    }
    @Override
    public String generateInvoiceHtml(String id) {
        HoaDon hd = repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        List<com.example.be.entity.HoaDonChiTiet> details = hoaDonChiTietRepository.findAllByHoaDon(hd);

        Context context = new Context();
        context.setVariable("hd", hd);
        context.setVariable("details", details);
        context.setVariable("ngayIn", new java.util.Date());

        return templateEngine.process("email/invoice-print", context);
    }
}
