package com.example.be.core.admin.hoadon.service.impl;

import com.example.be.core.admin.hoadon.model.request.AdminHoaDonRequest;
import com.example.be.core.admin.hoadon.model.response.AdminHoaDonResponse;
import com.example.be.core.admin.hoadon.repository.AdminHoaDonRepository;
import com.example.be.core.admin.hoadon.service.AdminHoaDonService;
import com.example.be.entity.HoaDon;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.utils.ExcelUtils;
import com.example.be.repository.HoaDonChiTietRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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
    private TemplateEngine templateEngine;

    @Override
    public Page<AdminHoaDonResponse> getPage(AdminHoaDonRequest req) {
        // 1. Chuẩn hóa dữ liệu lọc
        if (req.getSearch() != null && req.getSearch().trim().isEmpty()) req.setSearch(null);
        if (req.getTenKhachHang() != null && req.getTenKhachHang().trim().isEmpty()) req.setTenKhachHang(null);
        if (req.getLoaiDon() != null && req.getLoaiDon().trim().isEmpty()) req.setLoaiDon(null);
        if (req.getNgayTao() != null && req.getNgayTao().trim().isEmpty()) req.setNgayTao(null);

        // 2. Phân trang (Spring tính từ trang 0)
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize());

        // 3. Trả thẳng kết quả từ Repository về (Hết sạch lỗi đỏ!)
        return repository.getAllHoaDon(pageable, req);
    }

    @Override
    public HoaDon detail(String id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public HoaDon updateStatus(String id, Integer status) {
        HoaDon hd = repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        if (status >= 0 && status < OrderStatus.values().length) {
            hd.setTrangThai(OrderStatus.values()[status]);
        }
        return repository.save(hd);
    }

    @Override
    public java.util.Map<String, Long> getCounts() {
        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        counts.put("all", repository.countAll());

        java.util.List<java.util.Map<String, Object>> data = repository.countByTrangThai();
        for (java.util.Map<String, Object> map : data) {
            String status = String.valueOf(map.get("status"));
            Long count = (Long) map.get("count");
            counts.put(status, count);
        }
        return counts;
    }

    @Override
    public byte[] exportExcel(AdminHoaDonRequest req) {
        // Fetch all data without pagination for export
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
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
