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
import com.example.be.infrastructure.constants.OrderType;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.be.infrastructure.constants.TrangThai;

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
    private final com.example.be.core.notification.EmailService emailService;
    private final com.example.be.core.payment.PaymentService paymentService;
    private final com.example.be.repository.PhieuGiamGiaRepository phieuGiamGiaRepository;

    @org.springframework.beans.factory.annotation.Value("${app.frontend_url}")
    private String frontendUrl;


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
        AdminHoaDonDetailResponse response = hoaDonMapper.toDetailResponse(hd);
        resolvePerformerNames(response);

        boolean daSuaThongTin = hasUpdatedShippingInfo(hd);
        response.setDaSuaThongTin(daSuaThongTin);
        response.setChoPhepSuaThongTin(hd.getTrangThai() == OrderStatus.CHO_XAC_NHAN && !daSuaThongTin);

        BigDecimal tienHoanTraTruoc = Boolean.FALSE.equals(hd.getDaHoanPhi()) && hd.getPhiHoanHang() != null && hd.getPhiHoanHang().compareTo(BigDecimal.ZERO) > 0
                ? hd.getPhiHoanHang()
                : null;
        response.setTienHoanTraTruoc(tienHoanTraTruoc);
        if (tienHoanTraTruoc != null) {
            response.setCanHoanPhi(true);
        }

        return response;
    }

    private void resolvePerformerNames(AdminHoaDonDetailResponse response) {
        if (response.getListsGiaoDichThanhToan() != null) {
            response.getListsGiaoDichThanhToan().forEach(gd -> {
                if (gd.getNguoiXacNhan() != null) {
                    String creator = gd.getNguoiXacNhan();
                    if ("SYSTEM".equalsIgnoreCase(creator) || "anonymousUser".equalsIgnoreCase(creator)) {
                        String method = gd.getTenPhuongThuc() != null ? gd.getTenPhuongThuc().toUpperCase() : "";
                        if (method.contains("VNPAY")) {
                            gd.setNguoiXacNhan("Cổng VNPay (Tự động)");
                        } else {
                            gd.setNguoiXacNhan("Hệ thống");
                        }
                    } else {
                        nhanVienRepository.findByTenTaiKhoanOrEmailOrSdtOrMa(creator, creator, creator, creator)
                                .ifPresentOrElse(
                                        nv -> gd.setNguoiXacNhan(nv.getMa() != null ? nv.getMa() + (nv.getTen() != null ? " - " + nv.getTen() : "") : (nv.getTen() != null ? nv.getTen() : creator)),
                                        () -> khachHangRepository.findByTenTaiKhoan(creator)
                                                .ifPresent(kh -> gd.setNguoiXacNhan(kh.getTen() != null ? kh.getTen() + " (Khách hàng)" : creator))
                                );
                    }
                }
            });
        }
        if (response.getListsLichSuHoaDon() != null) {
            response.getListsLichSuHoaDon().forEach(log -> {
                if (log.getNguoiThucHien() != null) {
                    String performer = log.getNguoiThucHien();
                    if ("SYSTEM".equalsIgnoreCase(performer) || "anonymousUser".equalsIgnoreCase(performer)) {
                        log.setNguoiThucHien("Hệ thống");
                    } else if ("GUEST".equalsIgnoreCase(performer)) {
                        log.setNguoiThucHien("Khách vãng lai");
                    } else {
                        nhanVienRepository.findByTenTaiKhoanOrEmailOrSdtOrMa(performer, performer, performer, performer)
                                .ifPresentOrElse(
                                        nv -> {
                                            String display = nv.getMa() != null ? nv.getMa() + (nv.getTen() != null ? " - " + nv.getTen() : "") : (nv.getTen() != null ? nv.getTen() : performer);
                                            log.setNguoiThucHien(display);
                                        },
                                        () -> khachHangRepository.findByTenTaiKhoan(performer)
                                                .ifPresent(kh -> log.setNguoiThucHien(kh.getTen() != null ? kh.getTen() + " (Khách hàng)" : performer))
                                );
                    }
                }
            });
        }
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

        // Với đơn ONLINE: Trừ kho khi Admin xác nhận hóa đơn (từ Chờ xác nhận -> Đã xác nhận)
        if (oldStatus == OrderStatus.CHO_XAC_NHAN && newStatus == OrderStatus.XAC_NHAN && isOnlineOrder(hd)) {
            if (hd.getListsHoaDonChiTiet() != null) {
                for (HoaDonChiTiet detail : hd.getListsHoaDonChiTiet()) {
                    ChiTietSanPham ct = detail.getChiTietSanPham();
                    if (ct != null && detail.getSoLuong() != null) {
                        int currentStock = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                        if (currentStock < detail.getSoLuong()) {
                            String tenSP = ct.getSanPham() != null ? ct.getSanPham().getTen() : ct.getMaChiTietSanPham();
                            throw new BusinessException("Sản phẩm '" + tenSP + "' hiện không đủ số lượng trong kho (" + currentStock + ") để xác nhận đơn hàng.");
                        }
                        ct.setSoLuong(currentStock - detail.getSoLuong());
                        chiTietSanPhamRepository.saveAndFlush(ct);
                    }
                }
            }
        }


        // Gán nhân viên xử lý hóa đơn khi có nhân viên đăng nhập thực hiện chuyển trạng thái
        com.example.be.utils.SecurityUtils.getCurrentUserEmail().ifPresent(email -> {
            nhanVienRepository.findByTenTaiKhoanOrEmailOrSdtOrMa(email, email, email, email).ifPresent(staff -> {
                hd.setNhanVien(staff);
            });
        });

        hd.setTrangThai(newStatus);
        hd.setNgayCapNhat(System.currentTimeMillis());
        repository.save(hd);

        // Resolve display name for the person performing the action (Mã NV - Tên NV)
        String nguoiThucHienName = com.example.be.utils.SecurityUtils.getCurrentUserEmail()
                .map(email -> nhanVienRepository.findByTenTaiKhoanOrEmailOrSdtOrMa(email, email, email, email)
                        .map(nv -> nv.getMa() != null ? nv.getMa() + (nv.getTen() != null ? " - " + nv.getTen() : "") : (nv.getTen() != null ? nv.getTen() : email))
                        .orElse(email))
                .orElse("Hệ thống");
        LichSuTrangThaiHoaDon history = LichSuTrangThaiHoaDon.builder()
                .hoaDon(hd)
                .trangThaiCu(oldStatus != null ? oldStatus.ordinal() : null)
                .trangThaiMoi(newStatus.ordinal())
                .ghiChu(note != null && !note.trim().isEmpty() ? note : "Cập nhật trạng thái")
                .nguoiThucHien(nguoiThucHienName)
                .build();
        history.setNgayTao(System.currentTimeMillis());
        lichSuTrangThaiHoaDonRepository.save(history);

        // Nếu giao lại (từ Giao thất bại / Khách không nhận sang Đang giao hoặc Hoàn thành) -> Kiểm tra tồn kho thay thế và trừ kho lại
        if ((oldStatus == OrderStatus.GIAO_THAT_BAI || oldStatus == OrderStatus.KHACH_KHONG_NHAN) 
                && (newStatus == OrderStatus.DANG_GIAO || newStatus == OrderStatus.HOAN_THANH)) {
            if (hd.getListsHoaDonChiTiet() != null) {
                if (newStatus == OrderStatus.DANG_GIAO) {
                    for (HoaDonChiTiet detail : hd.getListsHoaDonChiTiet()) {
                        ChiTietSanPham ct = detail.getChiTietSanPham();
                        if (ct != null && detail.getSoLuong() != null) {
                            int currentStock = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                            if (currentStock < detail.getSoLuong()) {
                                String tenSP = ct.getSanPham() != null ? ct.getSanPham().getTen() : ct.getMaChiTietSanPham();
                                throw new BusinessException("Không đủ tồn kho để lấy hàng thay thế giao lại. Sản phẩm '" 
                                        + tenSP + "' chỉ còn " + currentStock + " sản phẩm (cần " + detail.getSoLuong() + "). Vui lòng nhập thêm hàng trước khi giao lại.");
                            }
                        }
                    }
                }
                for (HoaDonChiTiet detail : hd.getListsHoaDonChiTiet()) {
                    ChiTietSanPham ct = detail.getChiTietSanPham();
                    if (ct != null && detail.getSoLuong() != null) {
                        int currentStock = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                        ct.setSoLuong(Math.max(0, currentStock - detail.getSoLuong()));
                        chiTietSanPhamRepository.saveAndFlush(ct);
                    }
                }
            }
        }

        if (oldStatus == OrderStatus.DA_HUY && newStatus == OrderStatus.CHO_XAC_NHAN) {
            hd.setPhiHoanHang(null);
            hd.setDaHoanPhi(false);
        }

        if (newStatus == OrderStatus.DA_HUY || newStatus == OrderStatus.HOAN_DON 
                || newStatus == OrderStatus.GIAO_THAT_BAI || newStatus == OrderStatus.KHACH_KHONG_NHAN) {
            if ((newStatus == OrderStatus.HOAN_DON || newStatus == OrderStatus.GIAO_THAT_BAI || newStatus == OrderStatus.KHACH_KHONG_NHAN) 
                    && hd.getPhiVanChuyen() != null) {
                // GHN return fee is typically equal to forward fee
                hd.setPhiHoanHang(hd.getPhiVanChuyen());
            }
            // Chỉ hoàn kho nếu kho ĐÃ TỪNG BỊ TRỪ và chưa bị hoàn kho trước đó
            boolean alreadyRestoredStock = oldStatus == OrderStatus.GIAO_THAT_BAI || oldStatus == OrderStatus.KHACH_KHONG_NHAN;
            boolean chuaTruKho = oldStatus == OrderStatus.CHO_XAC_NHAN && isOnlineOrder(hd);
            if (!chuaTruKho && !alreadyRestoredStock) {
                if (hd.getListsHoaDonChiTiet() != null) {
                    hd.getListsHoaDonChiTiet().forEach(detail -> {
                        ChiTietSanPham ct = detail.getChiTietSanPham();
                        if (ct != null && detail.getSoLuong() != null) {
                            int currentStock = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                            ct.setSoLuong(currentStock + detail.getSoLuong());
                            chiTietSanPhamRepository.saveAndFlush(ct);
                        }
                    });
                }
            }

            // Hoàn lại số lượng voucher nếu đơn hàng bị hủy
            if (newStatus == OrderStatus.DA_HUY && hd.getPhieuGiamGia() != null && oldStatus != OrderStatus.DA_HUY) {
                PhieuGiamGia v = hd.getPhieuGiamGia();
                if (v.getSoLuong() != null && v.getSoLuong() >= 0) {
                    v.setSoLuong(v.getSoLuong() + 1);
                    phieuGiamGiaRepository.save(v);
                }
            }
        }

        // Gửi email thông báo cho khách hàng với mọi thay đổi trạng thái
        guiEmailTrangThai(hd, newStatus, note);

        return detail(id);
    }

    /** Gửi email báo trạng thái mới về email khách hàng trên hóa đơn (ưu tiên tài khoản, fallback người nhận). */
    private void guiEmailTrangThai(HoaDon hd, OrderStatus newStatus, String note) {
        String email = null;
        if (hd.getKhachHang() != null && hd.getKhachHang().getEmail() != null
                && !hd.getKhachHang().getEmail().trim().isEmpty()) {
            email = hd.getKhachHang().getEmail().trim();
        } else if (hd.getEmailNguoiNhan() != null && !hd.getEmailNguoiNhan().trim().isEmpty()) {
            email = hd.getEmailNguoiNhan().trim();
        }
        if (email == null) return; // Không có email thì bỏ qua, không chặn luồng đổi trạng thái

        String tenKhachHang = hd.getKhachHang() != null && hd.getKhachHang().getTen() != null
                ? hd.getKhachHang().getTen()
                : (hd.getTenNguoiNhan() != null ? hd.getTenNguoiNhan() : "Quý khách");

        if (newStatus == OrderStatus.DANG_GIAO) {
            String donVi = "Giao Hàng Nhanh (GHN)";
            String trackingUrl = frontendUrl + "/tra-cuu?code=" + (hd.getMaHoaDon() != null ? hd.getMaHoaDon() : "");
            emailService.guiEmailVanChuyen(email, tenKhachHang, hd.getMaHoaDon(), "GHN-" + hd.getMaHoaDon(), donVi, hd.getNgayDuKienNhan(), trackingUrl);
        } else {
            emailService.guiEmailCapNhatTrangThaiHoaDon(email, tenKhachHang, hd.getMaHoaDon(),
                    trangThaiLabel(newStatus), note);
        }
    }

    private String trangThaiLabel(OrderStatus status) {
        if (status == null) return "Không xác định";
        return switch (status) {
            case CHO_XAC_NHAN -> "Chờ xác nhận";
            case XAC_NHAN -> "Đã xác nhận";
            case CHO_GIAO -> "Chờ giao hàng";
            case DANG_GIAO -> "Đang giao hàng";
            case HOAN_THANH -> "Hoàn thành";
            case DA_HUY -> "Đã hủy";
            case HOAN_DON -> "Hoàn đơn";
            case GIAO_THAT_BAI -> "Giao hàng thất bại";
            case KHACH_KHONG_NHAN -> "Khách không nhận";
        };
    }

    private boolean hasUpdatedShippingInfo(HoaDon hd) {
        if (hd.getListsLichSuHoaDon() == null || hd.getListsLichSuHoaDon().isEmpty()) {
            return false;
        }
        return hd.getListsLichSuHoaDon().stream().anyMatch(ls -> {
            String note = ls.getGhiChu();
            if (note == null) return false;
            String lower = note.toLowerCase();
            return lower.contains("cập nhật thông tin nhận hàng")
                    || lower.contains("cập nhật thông tin giao hàng")
                    || lower.contains("cập nhật thông tin giao nhận")
                    || lower.contains("thông tin người nhận")
                    || lower.contains("thay đổi địa chỉ")
                    || lower.contains("thay đổi người nhận");
        });
    }

    @Override
    @Transactional
    public AdminHoaDonDetailResponse updateInfo(String id, AdminUpdateHoaDonRequest request) {
        HoaDon hd = repository.findDetailById(id).orElseThrow(() -> new ResourceNotFoundException(MessageConstants.HOA_DON_NOT_FOUND));

        if (hd.getTrangThai() != OrderStatus.CHO_XAC_NHAN) {
            throw new BusinessException("Chỉ có thể sửa thông tin khi đơn hàng đang ở trạng thái 'Chờ xác nhận'");
        }

        if (hasUpdatedShippingInfo(hd)) {
            throw new BusinessException("Thông tin giao nhận của đơn hàng chỉ được phép thay đổi 1 lần duy nhất.");
        }

        String oldTen = hd.getTenNguoiNhan();
        String oldSdt = hd.getSoDienThoaiNguoiNhan();
        String oldDiaChi = hd.getDiaChiNguoiNhan();

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

        StringBuilder changes = new StringBuilder("Cập nhật thông tin giao hàng/khách hàng: ");
        boolean hasChange = false;
        if (request.getTenNguoiNhan() != null && !request.getTenNguoiNhan().equals(oldTen)) {
            changes.append(String.format("Tên: '%s' -> '%s'; ", oldTen, request.getTenNguoiNhan()));
            hasChange = true;
        }
        if (request.getSoDienThoaiNguoiNhan() != null && !request.getSoDienThoaiNguoiNhan().equals(oldSdt)) {
            changes.append(String.format("SĐT: '%s' -> '%s'; ", oldSdt, request.getSoDienThoaiNguoiNhan()));
            hasChange = true;
        }
        if (request.getDiaChiNguoiNhan() != null && !request.getDiaChiNguoiNhan().equals(oldDiaChi)) {
            changes.append(String.format("Địa chỉ: '%s' -> '%s'; ", oldDiaChi, request.getDiaChiNguoiNhan()));
            hasChange = true;
        }
        if (!hasChange) {
            changes.append("Lưu thông tin giao nhận");
        }

        logHistory(hd, changes.toString().trim());

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

        boolean chuaTruKho = hd.getTrangThai() == OrderStatus.CHO_XAC_NHAN && isOnlineOrder(hd);
        if (!chuaTruKho) {
            ctsp.setSoLuong(ctsp.getSoLuong() - delta);
            chiTietSanPhamRepository.saveAndFlush(ctsp);
        }
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
        
        boolean chuaTruKho = hd.getTrangThai() == OrderStatus.CHO_XAC_NHAN && isOnlineOrder(hd);
        ChiTietSanPham ct = hdct.getChiTietSanPham();
        if (ct != null && !chuaTruKho) {
            ct.setSoLuong(ct.getSoLuong() + hdct.getSoLuong());
            chiTietSanPhamRepository.saveAndFlush(ct);
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

        boolean isCancelled = hd.getTrangThai() == OrderStatus.DA_HUY;
        boolean hasPendingRefund = Boolean.FALSE.equals(hd.getDaHoanPhi()) && hd.getPhiHoanHang() != null && hd.getPhiHoanHang().compareTo(BigDecimal.ZERO) > 0;

        if (!isCancelled && !hasPendingRefund) {
            throw new BusinessException("Chỉ xác nhận hoàn phí cho đơn hàng đã hủy hoặc có phát sinh hoàn tiền thừa");
        }
        if (Boolean.TRUE.equals(hd.getDaHoanPhi())) {
            throw new BusinessException("Đơn hàng này đã được xác nhận hoàn phí");
        }

        hd.setDaHoanPhi(true);
        hd.setNgayCapNhat(System.currentTimeMillis());
        repository.save(hd);

        String amountStr = hd.getPhiHoanHang() != null ? " (" + formatTien(hd.getPhiHoanHang()) + ")" : "";
        logHistory(hd, "Xác nhận đã hoàn phí cho khách hàng" + amountStr);
        return detail(id);
    }

    // Định dạng tiền VND cho ghi chú lịch sử (vd: 1.250.000đ)
    private String formatTien(java.math.BigDecimal value) {
        if (value == null) return "0đ";
        return String.format("%,dđ", value.longValue()).replace(',', '.');
    }

    private void logHistory(HoaDon hd, String note) {
        // Gán nhân viên xử lý nếu đơn chưa có nhân viên
        com.example.be.utils.SecurityUtils.getCurrentUserEmail().ifPresent(email -> {
            nhanVienRepository.findByTenTaiKhoanOrEmailOrSdtOrMa(email, email, email, email).ifPresent(staff -> {
                if (hd.getNhanVien() == null) {
                    hd.setNhanVien(staff);
                    repository.save(hd);
                }
            });
        });

        String nguoiThucHienName = com.example.be.utils.SecurityUtils.getCurrentUserEmail()
                .map(email -> nhanVienRepository.findByTenTaiKhoanOrEmailOrSdtOrMa(email, email, email, email)
                        .map(nv -> nv.getMa() != null ? nv.getMa() + (nv.getTen() != null ? " - " + nv.getTen() : "") : (nv.getTen() != null ? nv.getTen() : email))
                        .orElse(email))
                .orElse("Hệ thống");
        LichSuTrangThaiHoaDon history = LichSuTrangThaiHoaDon.builder()
                .hoaDon(hd)
                .trangThaiMoi(hd.getTrangThai().ordinal())
                .trangThaiCu(hd.getTrangThai().ordinal())
                .ghiChu(note)
                .nguoiThucHien(nguoiThucHienName)
                .build();
        history.setNgayTao(System.currentTimeMillis());
        lichSuTrangThaiHoaDonRepository.save(history);
    }

    private HoaDon recalculateTotal(HoaDon hd) {
        BigDecimal tongTienCu = hd.getTongTienSauGiam() != null ? hd.getTongTienSauGiam() : BigDecimal.ZERO;
        List<HoaDonChiTiet> details = hoaDonChiTietRepository.findAllByHoaDon(hd);
        java.math.BigDecimal total = details.stream()
                .map(d -> d.getDonGia().multiply(java.math.BigDecimal.valueOf(d.getSoLuong())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        hd.setTongTien(total);
        BigDecimal ship = hd.getPhiVanChuyen() != null ? hd.getPhiVanChuyen() : BigDecimal.ZERO;
        BigDecimal tienGiam = BigDecimal.ZERO;
        if (hd.getPhieuGiamGia() != null) {
            var voucher = hd.getPhieuGiamGia();
            if (voucher.getDonHangToiThieu() == null || total.compareTo(voucher.getDonHangToiThieu()) >= 0) {
                if ("PHAN_TRAM".equals(voucher.getLoaiPhieu()) && voucher.getPhanTramGiamGia() != null) {
                    tienGiam = total.multiply(BigDecimal.valueOf(voucher.getPhanTramGiamGia()))
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
                    if (voucher.getGiamToiDa() != null && tienGiam.compareTo(voucher.getGiamToiDa()) > 0) {
                        tienGiam = voucher.getGiamToiDa();
                    }
                } else if (voucher.getSoTienGiam() != null) {
                    tienGiam = voucher.getSoTienGiam();
                }
            }
        }
        BigDecimal tongSauGiam = total.add(ship).subtract(tienGiam).max(BigDecimal.ZERO);
        hd.setTongTienSauGiam(tongSauGiam);
        hd.setNgayCapNhat(System.currentTimeMillis());

        // Check if pre-paid order
        boolean daThanhToan = hd.getListsGiaoDichThanhToan() != null && hd.getListsGiaoDichThanhToan().stream()
                .anyMatch(gd -> gd.getTrangThai() == TrangThai.NGUNG_HOAT_DONG && gd.getMaGiaoDichNgoai() != null && !gd.getMaGiaoDichNgoai().isBlank());

        if (daThanhToan && tongTienCu.compareTo(BigDecimal.ZERO) > 0) {
            if (tongSauGiam.compareTo(tongTienCu) < 0) {
                BigDecimal tienHoan = tongTienCu.subtract(tongSauGiam);
                hd.setDaHoanPhi(false);
                hd.setPhiHoanHang(tienHoan);
            } else if (tongSauGiam.compareTo(tongTienCu) > 0) {
                hd.setDaHoanPhi(null);
            }
        }

        return repository.save(hd);
    }

    @Override
    public Map<String, Long> getCounts(AdminHoaDonRequest req) {
        prepareRequest(req);

        Map<String, Long> counts = new HashMap<>();
        long totalAll = 0L;

        List<Map<String, Object>> statusList = repository.countByTrangThai(req);
        if (statusList != null) {
            for (Map<String, Object> map : statusList) {
                Object status = map.get("status");
                Long count = (Long) map.get("count");
                if (count != null) {
                    totalAll += count;
                    if (status != null) {
                        counts.put(String.valueOf(status), count);
                    }
                }
            }
        }
        counts.put("all", totalAll);
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
                item.getOrderType(),
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
        BigDecimal subtotal = hd.getTongTien() != null ? hd.getTongTien() : BigDecimal.ZERO;
        BigDecimal shippingFee = hd.getPhiVanChuyen() != null ? hd.getPhiVanChuyen() : BigDecimal.ZERO;
        BigDecimal payableTotal = hd.getTongTienSauGiam() != null ? hd.getTongTienSauGiam() : subtotal.add(shippingFee);
        // Giảm giá chỉ tính trên tiền hàng. tongTienSauGiam đã bao gồm phí ship,
        // nên phải trừ shippingFee ra trước để không làm dòng "Giảm giá" trên bản in bị thiếu.
        BigDecimal payableProductTotal = payableTotal.subtract(shippingFee).max(BigDecimal.ZERO);
        BigDecimal discountAmount = subtotal.subtract(payableProductTotal).max(BigDecimal.ZERO);
        
        com.example.be.entity.PhieuGiamGia pgg = hd.getPhieuGiamGia();
        if (pgg == null && hd.getPhieuGiamGiaCaNhan() != null) {
            pgg = hd.getPhieuGiamGiaCaNhan().getPhieuGiamGia();
        }
        String voucherCode = pgg != null ? pgg.getMa() : null;
        String voucherName = pgg != null ? pgg.getTen() : null;

        Context context = new Context();
        context.setVariable("hd", hd);
        context.setVariable("details", hd.getListsHoaDonChiTiet());
        context.setVariable("subtotal", subtotal);
        context.setVariable("shippingFee", shippingFee);
        context.setVariable("payableTotal", payableTotal);
        context.setVariable("discountAmount", discountAmount);
        context.setVariable("voucherCode", voucherCode);
        context.setVariable("voucherName", voucherName);
        context.setVariable("paymentMethodLabel", resolvePaymentMethodLabel(hd));
        context.setVariable("ngayIn", new Date());

        return templateEngine.process("email/invoice-print", context);
    }

    private boolean isOnlineOrder(HoaDon hoaDon) {
        if (hoaDon.getOrderType() != null) {
            return hoaDon.getOrderType() == OrderType.ONLINE;
        }
        return hoaDon.getNhanVien() == null && OrderType.ONLINE.name().equalsIgnoreCase(hoaDon.getLoaiDon());
    }

    private String resolvePaymentMethodLabel(HoaDon hd) {
        if (hd.getListsGiaoDichThanhToan() == null || hd.getListsGiaoDichThanhToan().isEmpty()) {
            return "Chưa thanh toán";
        }
        return hd.getListsGiaoDichThanhToan().stream()
                .filter(gd -> gd.getSoTien() != null && gd.getSoTien().compareTo(BigDecimal.ZERO) > 0)
                .map(gd -> gd.getPhuongThucThanhToan() != null ? gd.getPhuongThucThanhToan().getTen() : null)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse("Chưa thanh toán");
    }

    @Override
    public String createVnPayUrl(String id, String returnUrl) {
        HoaDon hoaDon = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.HOA_DON_NOT_FOUND));

        boolean daThanhToan = hoaDon.getListsGiaoDichThanhToan() != null && hoaDon.getListsGiaoDichThanhToan().stream()
                .anyMatch(gd -> gd.getTrangThai() == TrangThai.NGUNG_HOAT_DONG 
                        && gd.getMaGiaoDichNgoai() != null && !gd.getMaGiaoDichNgoai().isBlank());
        if (daThanhToan) {
            throw new BusinessException("Đơn hàng này đã được thanh toán thành công.");
        }
        if (hoaDon.getTrangThai() != OrderStatus.CHO_XAC_NHAN) {
            throw new BusinessException("Chỉ có thể tạo link thanh toán lại khi đơn đang ở trạng thái 'Chờ xác nhận'");
        }

        BigDecimal amount = hoaDon.getTongTienSauGiam() != null ? hoaDon.getTongTienSauGiam() : hoaDon.getTongTien();
        if (hoaDon.getPhiVanChuyen() != null) {
            amount = amount.add(hoaDon.getPhiVanChuyen());
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền thanh toán không hợp lệ.");
        }
        String defaultBase = (frontendUrl != null && !frontendUrl.isBlank()) ? frontendUrl : "https://aerostride.me";
        String effectiveReturnUrl = (returnUrl != null && !returnUrl.isBlank()) ? returnUrl : defaultBase + "/my-orders/" + hoaDon.getId();
        com.example.be.core.payment.dto.PaymentRequest payReq = com.example.be.core.payment.dto.PaymentRequest.builder()
                .amount(amount)
                .orderId(hoaDon.getId())
                .orderInfo("Thanh toan don hang " + (hoaDon.getMaHoaDon() != null ? hoaDon.getMaHoaDon() : hoaDon.getId()))
                .returnUrl(effectiveReturnUrl)
                .build();

        return paymentService.createPaymentUrl(payReq);
    }

    private boolean isCashOrder(HoaDon hoaDon) {
        return com.example.be.infrastructure.constants.PaymentConstants.isCashOrder(hoaDon);
    }
}
