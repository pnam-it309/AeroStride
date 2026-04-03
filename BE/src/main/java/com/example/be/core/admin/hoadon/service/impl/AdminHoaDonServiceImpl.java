package com.example.be.core.admin.hoadon.service.impl;

import com.example.be.core.admin.hoadon.model.request.AdminHoaDonRequest;
import com.example.be.core.admin.hoadon.model.response.AdminHoaDonResponse;
import com.example.be.core.admin.hoadon.repository.AdminHoaDonRepository;
import com.example.be.core.admin.hoadon.service.AdminHoaDonService;
import com.example.be.entity.HoaDon;
import com.example.be.infrastructure.constants.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AdminHoaDonServiceImpl implements AdminHoaDonService {

    @Autowired
    private AdminHoaDonRepository repository;

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
}
