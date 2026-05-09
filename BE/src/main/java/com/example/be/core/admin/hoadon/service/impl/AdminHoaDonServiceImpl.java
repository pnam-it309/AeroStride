package com.example.be.core.admin.hoadon.service.impl;

import com.example.be.core.admin.hoadon.model.request.AdminHoaDonRequest;
import com.example.be.core.admin.hoadon.model.request.AdminUpdateHdctRequest;
import com.example.be.core.admin.hoadon.model.request.AdminUpdateHoaDonRequest;
import com.example.be.core.admin.hoadon.model.response.AdminHoaDonResponse;
import com.example.be.core.admin.hoadon.repository.AdminHoaDonRepository;
import com.example.be.core.admin.hoadon.service.AdminHoaDonService;
import com.example.be.entity.*;
import com.example.be.infrastructure.constants.OrderStatus;
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


    @Override
    public Page<AdminHoaDonResponse> getPage(AdminHoaDonRequest req) {
        prepareRequest(req);
        return SearchUtils.execute(req, pageable -> repository.getAllHoaDon(pageable, req));
    }

    private void prepareRequest(AdminHoaDonRequest req) {
        if (req.getSearch() != null && req.getSearch().trim().isEmpty()) req.setSearch(null);
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
    public HoaDon detail(String id) {
        HoaDon hd = repository.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn với id: " + id));
        return initializeHoaDon(hd);
    }

    /**
     * Force initialization of lazy collections and proxies to prevent LazyInitializationException during JSON serialization
     */
    private HoaDon initializeHoaDon(HoaDon hd) {
        if (hd == null) return null;
        
        // Initialize lazy collections
        if (hd.getListsHoaDonChiTiet() != null) {
            hd.getListsHoaDonChiTiet().size();
            // Initialize products and their related entities within details
            hd.getListsHoaDonChiTiet().forEach(item -> {
                if (item.getChiTietSanPham() != null) {
                    ChiTietSanPham ctsp = item.getChiTietSanPham();
                    ctsp.getMaChiTietSanPham(); // Init CTSP
                    
                    if (ctsp.getSanPham() != null) {
                        SanPham sp = ctsp.getSanPham();
                        sp.getTen(); // Init SanPham
                        // Initialize all lazy attributes of SanPham that Jackson might touch
                        if (sp.getThuongHieu() != null) sp.getThuongHieu().getTen();
                        if (sp.getDanhMuc() != null) sp.getDanhMuc().getTen();
                        if (sp.getXuatXu() != null) sp.getXuatXu().getTen();
                        if (sp.getChatLieu() != null) sp.getChatLieu().getTen();
                        if (sp.getDeGiay() != null) sp.getDeGiay().getTen();
                        if (sp.getCoGiay() != null) sp.getCoGiay().getTen();
                        if (sp.getMucDichChay() != null) sp.getMucDichChay().getTen();
                    }
                    
                    if (ctsp.getMauSac() != null) ctsp.getMauSac().getTen();
                    if (ctsp.getKichThuoc() != null) ctsp.getKichThuoc().getTen();
                    
                    // Initialize image collection (needed for product thumbnail)
                    if (ctsp.getAnhChiTietSanPhams() != null) {
                        ctsp.getAnhChiTietSanPhams().size();
                    }
                    // Note: chiTietDotGiamGias is @JsonIgnore — no need to initialize
                }
            });
        }
        
        if (hd.getListsLichSuHoaDon() != null) {
            hd.getListsLichSuHoaDon().size();
        }
        
        if (hd.getListsGiaoDichThanhToan() != null) {
            hd.getListsGiaoDichThanhToan().size();
            hd.getListsGiaoDichThanhToan().forEach(gd -> {
                if (gd.getPhuongThucThanhToan() != null) {
                    gd.getPhuongThucThanhToan().getTen();
                }
            });
        }
        
        // Initialize other lazy relationships of HoaDon
        if (hd.getPhieuGiamGia() != null) hd.getPhieuGiamGia().getTen();
        
        // Initialize staff role and related entities
        if (hd.getNhanVien() != null) {
            hd.getNhanVien().getTen();
            if (hd.getNhanVien().getPhanQuyen() != null) {
                hd.getNhanVien().getPhanQuyen().getTen();
            }
        }

        // Initialize customer
        if (hd.getKhachHang() != null) {
            hd.getKhachHang().getTen();
        }
        
        return hd;
    }

    @Override
    @Transactional
    public HoaDon updateStatus(String id, Integer status, String note) {
        HoaDon hd = repository.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn"));

        OrderStatus oldStatus = hd.getTrangThai();
        OrderStatus newStatus;

        if (status != null && status >= 0 && status < OrderStatus.values().length) {
            newStatus = OrderStatus.values()[status];
        } else {
            throw new com.example.be.infrastructure.exceptions.BusinessException("Trạng thái không hợp lệ");
        }

        if (oldStatus == newStatus) return initializeHoaDon(hd);

        hd.setTrangThai(newStatus);
        hd.setNgayCapNhat(System.currentTimeMillis());
        repository.save(hd);

        LichSuTrangThaiHoaDon history = LichSuTrangThaiHoaDon.builder()
                .hoaDon(hd)
                .trangThaiCu(oldStatus != null ? oldStatus.ordinal() : null)
                .trangThaiMoi(newStatus.ordinal())
                .ghiChu(note != null && !note.trim().isEmpty() ? note : "Cập nhật trạng thái từ hệ thống quản trị")
                .nguoiThucHien("ADMIN")
                .build();
        history.setNgayTao(System.currentTimeMillis());
        lichSuTrangThaiHoaDonRepository.save(history);

        if ((newStatus == OrderStatus.DA_HUY || newStatus == OrderStatus.HOAN_DON) && (oldStatus != OrderStatus.DA_HUY && oldStatus != OrderStatus.HOAN_DON)) {
            hoaDonChiTietRepository.findAllByHoaDon(hd).forEach(detail -> {
                ChiTietSanPham ct = detail.getChiTietSanPham();
                if (ct != null) {
                    ct.setSoLuong(ct.getSoLuong() + detail.getSoLuong());
                    chiTietSanPhamRepository.save(ct);
                }
            });
        } else if ((oldStatus == OrderStatus.DA_HUY || oldStatus == OrderStatus.HOAN_DON) && (newStatus != OrderStatus.DA_HUY && newStatus != OrderStatus.HOAN_DON)) {
            // Re-activating a cancelled/returned order: deduct stock
            hoaDonChiTietRepository.findAllByHoaDon(hd).forEach(detail -> {
                ChiTietSanPham ct = detail.getChiTietSanPham();
                if (ct != null) {
                    if (ct.getSoLuong() < detail.getSoLuong()) {
                        throw new com.example.be.infrastructure.exceptions.BusinessException("Sản phẩm " + ct.getSanPham().getTen() + " không đủ tồn kho để khôi phục đơn hàng.");
                    }
                    ct.setSoLuong(ct.getSoLuong() - detail.getSoLuong());
                    chiTietSanPhamRepository.save(ct);
                }
            });
        }

        return initializeHoaDon(hd);
    }

    @Override
    @Transactional
    public HoaDon updateInfo(String id, AdminUpdateHoaDonRequest request) {
        HoaDon hd = repository.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn"));

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

        return initializeHoaDon(repository.save(hd));
    }

    @Override
    @Transactional
    public HoaDon updateHdct(String id, AdminUpdateHdctRequest request) {
        HoaDon hd = repository.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn"));
        ChiTietSanPham ctsp = chiTietSanPhamRepository.findById(request.getIdChiTietSanPham())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm chi tiết"));

        HoaDonChiTiet hdct = hoaDonChiTietRepository.findByHoaDonAndChiTietSanPham(hd, ctsp);

        int currentQty = (hdct != null) ? hdct.getSoLuong() : 0;
        int delta = request.getSoLuong() - currentQty;

        if (ctsp.getSoLuong() < delta) {
            throw new com.example.be.infrastructure.exceptions.BusinessException("Sản phẩm không đủ số lượng tồn kho (Còn lại: " + ctsp.getSoLuong() + ")");
        }

        if (hdct != null) {
            hdct.setSoLuong(request.getSoLuong());
        } else {
            hdct = HoaDonChiTiet.builder()
                    .hoaDon(hd)
                    .chiTietSanPham(ctsp)
                    .soLuong(request.getSoLuong())
                    .donGia(ctsp.getGiaBan())
                    .build();
            hdct.setMaHoaDonChiTiet(CodeUtils.generateRandom(HoaDonChiTiet.class));
            hdct.setNgayTao(System.currentTimeMillis());
        }

        ctsp.setSoLuong(ctsp.getSoLuong() - delta);
        chiTietSanPhamRepository.save(ctsp);
        hoaDonChiTietRepository.save(hdct);
        logHistory(hd, "Thay đổi sản phẩm: " + ctsp.getSanPham().getTen() + " (SL: " + request.getSoLuong() + ")");

        return initializeHoaDon(recalculateTotal(hd));
    }

    @Override
    @Transactional
    public HoaDon removeHdct(String id, String idHdct) {
        HoaDon hd = repository.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn"));
        HoaDonChiTiet hdct = hoaDonChiTietRepository.findById(idHdct)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm chi tiết"));
        
        ChiTietSanPham ct = hdct.getChiTietSanPham();
        if (ct != null) {
            ct.setSoLuong(ct.getSoLuong() + hdct.getSoLuong());
            chiTietSanPhamRepository.save(ct);
        }
        
        hoaDonChiTietRepository.delete(hdct);
        logHistory(hd, "Xóa sản phẩm khỏi hóa đơn");
        return initializeHoaDon(recalculateTotal(hd));
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
            throw new SystemException("Lỗi xuất file Excel: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String generateInvoiceHtml(String id) {
        HoaDon hd = repository.findForPrint(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn"));
        List<com.example.be.entity.HoaDonChiTiet> details = hoaDonChiTietRepository.findAllByHoaDon(hd);
        
        // Ensure all lazy relationships are initialized for the template
        details.forEach(item -> {
            if (item.getChiTietSanPham() != null) {
                ChiTietSanPham ct = item.getChiTietSanPham();
                if (ct.getSanPham() != null) ct.getSanPham().getTen();
                if (ct.getMauSac() != null) ct.getMauSac().getTen();
                if (ct.getKichThuoc() != null) ct.getKichThuoc().getTen();
            }
        });

        Context context = new Context();
        context.setVariable("hd", hd);
        context.setVariable("details", details);
        context.setVariable("ngayIn", new Date());

        return templateEngine.process("email/invoice-print", context);
    }
}
