package com.example.be.core.admin.hoadon.service.impl;

import com.example.be.core.admin.hoadon.mapper.AdminHoaDonMapper;
import com.example.be.core.admin.hoadon.model.request.AdminHoaDonRequest;
import com.example.be.core.admin.hoadon.model.request.AdminUpdateHdctRequest;
import com.example.be.core.admin.hoadon.model.request.AdminUpdateHoaDonRequest;
import com.example.be.core.admin.hoadon.model.response.AdminHoaDonDetailResponse;
import com.example.be.core.admin.hoadon.model.response.AdminHoaDonResponse;
import com.example.be.core.admin.hoadon.repository.AdminHoaDonRepository;
import com.example.be.core.admin.hoadon.service.AdminHoaDonService;
import com.example.be.entity.*;
import com.example.be.infrastructure.constants.MessageConstants;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.infrastructure.exceptions.BusinessException;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.infrastructure.exceptions.SystemException;
import com.example.be.repository.*;
import com.example.be.utils.ExcelUtils;
import com.example.be.utils.HelperUtils;
import com.example.be.utils.CodeUtils;
import com.example.be.utils.SearchUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminHoaDonServiceImpl implements AdminHoaDonService {

    private final AdminHoaDonRepository repository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final LichSuTrangThaiHoaDonRepository lichSuTrangThaiHoaDonRepository;
    private final NhanVienRepository nhanVienRepository;
    private final ChiTietSanPhamRepository chiTietSanPhamRepository;
    private final KhachHangRepository khachHangRepository;
    private final TemplateEngine templateEngine;
    private final AdminHoaDonMapper hoaDonMapper;


    @Override
    public Page<AdminHoaDonResponse> getPage(AdminHoaDonRequest req) {
        prepareRequest(req);
        return SearchUtils.execute(req, pageable -> repository.getAllHoaDon(pageable, req));
    }

    private void prepareRequest(AdminHoaDonRequest req) {
        if (req.getSearch() != null && req.getSearch().trim().isEmpty()) req.setSearch(null);
        if (req.getIdKhachHang() != null && req.getIdKhachHang().trim().isEmpty()) req.setIdKhachHang(null);
        if (req.getTenKhachHang() != null && req.getTenKhachHang().trim().isEmpty()) req.setTenKhachHang(null);
        if (req.getLoaiDon() != null && req.getLoaiDon().trim().isEmpty()) req.setLoaiDon(null);
        if (req.getNgayTao() != null && req.getNgayTao().trim().isEmpty()) req.setNgayTao(null);

        req.setTuNgayLong(parseDateToLong(req.getTuNgay(), false));
        req.setDenNgayLong(parseDateToLong(req.getDenNgay(), true));
    }

    private Long parseDateToLong(String dateStr, boolean endOfDay) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        try {
            LocalDate date = LocalDate.parse(dateStr.trim());
            if (endOfDay) {
                return date.atTime(23, 59, 59, 999).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AdminHoaDonDetailResponse detail(String id) {
        HoaDon hd = repository.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException(MessageConstants.HOA_DON_NOT_FOUND));
        // Explicitly initialize collections that were removed from EntityGraph to avoid MultipleBagFetchException
        // Hibernate will load each in a single efficient query (1 query per collection)
        if (hd.getListsLichSuHoaDon() != null) hd.getListsLichSuHoaDon().size();
        if (hd.getListsGiaoDichThanhToan() != null) {
            hd.getListsGiaoDichThanhToan().forEach(gd -> {
                if (gd.getPhuongThucThanhToan() != null) gd.getPhuongThucThanhToan().getTen();
            });
        }
        return hoaDonMapper.toDetailResponse(hd);
    }

    @Override
    @Transactional
    public AdminHoaDonDetailResponse updateStatus(String id, Integer status, String note) {
        HoaDon hd = repository.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException(MessageConstants.HOA_DON_NOT_FOUND));

        OrderStatus oldStatus = hd.getTrangThai();
        OrderStatus newStatus;

        if (status != null && status >= 0 && status < OrderStatus.values().length) {
            newStatus = OrderStatus.values()[status];
        } else {
            throw new BusinessException(MessageConstants.INVALID_STATUS);
        }

        if (oldStatus == newStatus) return detail(id);

        // BẮT BUỘC: Kiểm tra luồng trạng thái hợp lệ
        if (oldStatus != null && !oldStatus.canTransitionTo(newStatus)) {
            throw new BusinessException(
                String.format(MessageConstants.INVALID_STATUS_TRANSITION, oldStatus.name(), newStatus.name())
            );
        }

        hd.setTrangThai(newStatus);
        hd.setNgayCapNhat(System.currentTimeMillis());
        repository.save(hd);

        LichSuTrangThaiHoaDon history = LichSuTrangThaiHoaDon.builder()
                .hoaDon(hd)
                .trangThaiCu(oldStatus != null ? oldStatus.ordinal() : null)
                .trangThaiMoi(newStatus.ordinal())
                .ghiChu(note != null && !note.trim().isEmpty() ? note : "Cập nhật trạng thái")
                .nguoiThucHien(com.example.be.utils.SecurityUtils.getCurrentUserEmail().orElse("ADMIN"))
                .build();
        history.setNgayTao(System.currentTimeMillis());
        lichSuTrangThaiHoaDonRepository.save(history);

        if (newStatus == OrderStatus.DA_HUY || newStatus == OrderStatus.HOAN_DON) {
            hd.getListsHoaDonChiTiet().forEach(detail -> {
                ChiTietSanPham ct = detail.getChiTietSanPham();
                if (ct != null) {
                    ct.setSoLuong(ct.getSoLuong() + detail.getSoLuong());
                    chiTietSanPhamRepository.save(ct);
                }
            });
        }

        return detail(id);
    }

    @Override
    @Transactional
    public AdminHoaDonDetailResponse updateInfo(String id, AdminUpdateHoaDonRequest request) {
        HoaDon hd = repository.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException(MessageConstants.HOA_DON_NOT_FOUND));

        if (request.getTenNguoiNhan() != null) hd.setTenNguoiNhan(request.getTenNguoiNhan());
        hd.setSoDienThoaiNguoiNhan(request.getSoDienThoaiNguoiNhan());
        hd.setDiaChiNguoiNhan(request.getDiaChiNguoiNhan());
        hd.setGhiChu(request.getGhiChu());

        if (request.getIdKhachHang() != null) {
            hd.setKhachHang(khachHangRepository.findById(request.getIdKhachHang()).orElse(null));
        }

        if (request.getIdNhanVien() != null) {
            hd.setNhanVien(nhanVienRepository.findById(request.getIdNhanVien()).orElse(null));
        }

        hd.setNgayCapNhat(System.currentTimeMillis());
        logHistory(hd, "Cập nhật thông tin giao hàng/khách hàng");

        repository.save(hd);
        return detail(id);
    }

    @Override
    @Transactional
    public AdminHoaDonDetailResponse updateHdct(String id, AdminUpdateHdctRequest request) {
        HoaDon hd = repository.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException(MessageConstants.HOA_DON_NOT_FOUND));
        ChiTietSanPham ctsp = chiTietSanPhamRepository.findById(request.getIdChiTietSanPham())
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PRODUCT_DETAIL_NOT_FOUND));

        HoaDonChiTiet hdct = hoaDonChiTietRepository.findByHoaDonAndChiTietSanPham(hd, ctsp);

        int currentQty = (hdct != null) ? hdct.getSoLuong() : 0;
        int delta = request.getSoLuong() - currentQty;

        if (ctsp.getSoLuong() < delta) {
            throw new BusinessException(MessageConstants.PRODUCT_OUT_OF_STOCK + " (Còn lại: " + ctsp.getSoLuong() + ")");
        }

        // Phát hiện đổi giá: giá hiện tại của SP khác giá đã chốt trên dòng hóa đơn
        java.math.BigDecimal giaCu = (hdct != null) ? hdct.getDonGia() : null;
        java.math.BigDecimal giaMoi = ctsp.getGiaBan();
        boolean giaThayDoi = giaCu != null && giaMoi != null && giaCu.compareTo(giaMoi) != 0;

        if (hdct != null) {
            hdct.setSoLuong(request.getSoLuong());
            // Áp giá mới (trở thành bản ghi mới với đơn giá hiện hành)
            hdct.setDonGia(giaMoi);
        } else {
            hdct = HoaDonChiTiet.builder()
                    .hoaDon(hd)
                    .chiTietSanPham(ctsp)
                    .soLuong(request.getSoLuong())
                    .donGia(giaMoi)
                    .build();
            hdct.setMaHoaDonChiTiet(CodeUtils.generateRandom(HoaDonChiTiet.class));
            hdct.setNgayTao(System.currentTimeMillis());
        }

        ctsp.setSoLuong(ctsp.getSoLuong() - delta);
        chiTietSanPhamRepository.save(ctsp);
        hoaDonChiTietRepository.save(hdct);

        if (giaThayDoi) {
            logHistory(hd, "Giá sản phẩm '" + ctsp.getSanPham().getTen() + "' đổi từ "
                    + formatTien(giaCu) + " thành " + formatTien(giaMoi));
        } else {
            logHistory(hd, "Thay đổi sản phẩm: " + ctsp.getSanPham().getTen() + " (SL: " + request.getSoLuong() + ")");
        }

        recalculateTotal(hd);
        return detail(id);
    }

    @Override
    @Transactional
    public AdminHoaDonDetailResponse removeHdct(String id, String idHdct) {
        HoaDon hd = repository.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException(MessageConstants.HOA_DON_NOT_FOUND));
        HoaDonChiTiet hdct = hoaDonChiTietRepository.findById(idHdct)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PRODUCT_DETAIL_NOT_FOUND));
        
        ChiTietSanPham ct = hdct.getChiTietSanPham();
        if (ct != null) {
            ct.setSoLuong(ct.getSoLuong() + hdct.getSoLuong());
            chiTietSanPhamRepository.save(ct);
        }
        
        hoaDonChiTietRepository.delete(hdct);
        logHistory(hd, "Xóa sản phẩm khỏi hóa đơn");
        recalculateTotal(hd);
        return detail(id);
    }

    @Override
    @Transactional
    public AdminHoaDonDetailResponse confirmRefund(String id) {
        HoaDon hd = repository.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException(MessageConstants.HOA_DON_NOT_FOUND));

        if (hd.getTrangThai() != OrderStatus.DA_HUY) {
            throw new BusinessException("Chỉ xác nhận hoàn phí cho đơn hàng đã hủy");
        }
        if (Boolean.TRUE.equals(hd.getDaHoanPhi())) {
            throw new BusinessException("Đơn hàng này đã được xác nhận hoàn phí");
        }

        hd.setDaHoanPhi(true);
        hd.setNgayCapNhat(System.currentTimeMillis());
        repository.save(hd);

        logHistory(hd, "Xác nhận đã hoàn phí cho khách hàng");
        return detail(id);
    }

    // Định dạng tiền VND cho ghi chú lịch sử (vd: 1.250.000đ)
    private String formatTien(java.math.BigDecimal value) {
        if (value == null) return "0đ";
        return String.format("%,dđ", value.longValue()).replace(',', '.');
    }

    private void logHistory(HoaDon hd, String note) {
        LichSuTrangThaiHoaDon history = LichSuTrangThaiHoaDon.builder()
                .hoaDon(hd)
                .trangThaiMoi(hd.getTrangThai().ordinal())
                .trangThaiCu(hd.getTrangThai().ordinal())
                .ghiChu(note)
                .nguoiThucHien(com.example.be.utils.SecurityUtils.getCurrentUserEmail().orElse("ADMIN"))
                .build();
        history.setNgayTao(System.currentTimeMillis());
        lichSuTrangThaiHoaDonRepository.save(history);
    }

    private HoaDon recalculateTotal(HoaDon hd) {
        List<HoaDonChiTiet> details = hoaDonChiTietRepository.findAllByHoaDon(hd);
        java.math.BigDecimal total = details.stream()
                .map(d -> d.getDonGia().multiply(java.math.BigDecimal.valueOf(d.getSoLuong())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        hd.setTongTien(total);
        hd.setTongTienSauGiam(total);
        hd.setNgayCapNhat(System.currentTimeMillis());
        return repository.save(hd);
    }

    @Override
    public Map<String, Long> getCounts(AdminHoaDonRequest req) {
        prepareRequest(req);

        Map<String, Long> counts = new HashMap<>();
        counts.put("all", repository.countWithFilter(req));

        repository.countByTrangThai(req).forEach(map -> {
            counts.put(String.valueOf(map.get("status")), (Long) map.get("count"));
        });
        return counts;
    }

    @Override
    public byte[] exportExcel(AdminHoaDonRequest req) {
        prepareRequest(req);
        Pageable pageable = SearchUtils.buildPageable(req);
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
            throw new SystemException(MessageConstants.EXCEL_EXPORT_ERROR + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String generateInvoiceHtml(String id) {
        HoaDon hd = repository.findForPrintById(id).orElseThrow(() -> new ResourceNotFoundException(MessageConstants.HOA_DON_NOT_FOUND));
        
        Context context = new Context();
        context.setVariable("hd", hd);
        context.setVariable("details", hd.getListsHoaDonChiTiet());
        context.setVariable("ngayIn", new Date());

        return templateEngine.process("email/invoice-print", context);
    }
}
