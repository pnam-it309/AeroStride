package com.example.be.core.admin.banhang.service.impl;

import com.example.be.core.admin.banhang.model.request.AdminBanHangCheckoutRequest;
import com.example.be.core.admin.banhang.model.request.AdminBanHangHoaDonChiTietRequest;
import com.example.be.core.admin.banhang.model.response.AdminBanHangHoaDonChiTietResponse;
import com.example.be.core.admin.banhang.model.response.AdminBanHangHoaDonResponse;
import com.example.be.core.admin.banhang.model.response.AdminBanHangKhachHangResponse;
import com.example.be.core.admin.banhang.model.response.BanHangSanPhamResponse;
import com.example.be.core.admin.banhang.repository.*;
import com.example.be.core.admin.banhang.service.AdminBanHangService;
import com.example.be.core.admin.dotgiamgia.repository.AdminChiTietDotGiamGiaRepository;
import com.example.be.entity.*;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.infrastructure.constants.OrderType;
import com.example.be.infrastructure.constants.DeliveryMethod;
import com.example.be.infrastructure.constants.HinhThucPhieuGiamGia;
import com.example.be.infrastructure.constants.LoaiPhieuGiamGia;
import com.example.be.infrastructure.constants.OrderType;
import com.example.be.infrastructure.constants.PaymentConstants;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.constants.MessageConstants;
import com.example.be.infrastructure.exceptions.BusinessException;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.repository.DiaChiRepository;
import com.example.be.repository.NhanVienRepository;
import com.example.be.repository.LichSuTrangThaiHoaDonRepository;
import com.example.be.utils.DiscountPriceUtils;
import com.example.be.utils.HelperUtils;
import com.example.be.utils.CodeUtils;
import com.example.be.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Xu ly luong ban hang tai quay: hoa don cho, gio hang, voucher, khach hang,
 * thanh toan va gia bien the sau dot giam gia.
 */
@Service
@RequiredArgsConstructor
public class AdminBanHangServiceImpl implements AdminBanHangService {

    private final AdminBanHangHoaDonRepository hoaDonRepository;
    private final AdminBanHangHoaDonChiTietRepository hoaDonChiTietRepository;
    private final AdminBanHangChiTietSanPhamRepository chiTietSanPhamRepository;
    private final AdminBanHangKhachHangRepository khachHangRepository;
    private final AdminBanHangPhieuGiamGiaRepository phieuGiamGiaRepository;
    private final AdminBanHangGiaoDichThanhToanRepository giaoDichThanhToanRepository;
    private final AdminBanHangPhuongThucThanhToanRepository phuongThucThanhToanRepository;
    private final AdminChiTietDotGiamGiaRepository chiTietDotGiamGiaRepository;
    private final DiaChiRepository diaChiRepository;
    private final NhanVienRepository nhanVienRepository;
    private final LichSuTrangThaiHoaDonRepository lichSuTrangThaiHoaDonRepository;
    private final com.example.be.core.admin.giaoca.repository.AdminGiaoCaRepository giaoCaRepository;
    private final com.example.be.repository.PhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;
    private final com.example.be.core.admin.sanpham.repository.AdminAnhChiTietSanPhamRepository anhChiTietSanPhamRepository;

    @Override
    @Transactional(readOnly = true)
    /** Lay danh sach hoa don POS dang cho xu ly de FE hien thi tab don hang. */
    public List<AdminBanHangHoaDonResponse> getHoaDonCho() {
        NhanVien nv = getCurrentNhanVien().orElse(null);
        String idNhanVien = nv != null ? nv.getId() : null;
        String idGiaoCa = (nv != null) ? giaoCaRepository.findGiaoCaHienTai(nv.getId()).map(com.example.be.entity.GiaoCa::getId).orElse(null) : null;
        List<HoaDon> pendingOrders = hoaDonRepository.findAllPendingPOSOrders(OrderStatus.CHO_XAC_NHAN, OrderType.IN_STORE, idNhanVien, idGiaoCa);
        if (pendingOrders.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<String> orderIds = pendingOrders.stream().map(HoaDon::getId).toList();
        List<HoaDonChiTiet> allDetails = hoaDonChiTietRepository.findAllByHoaDonIdInWithDetails(orderIds);
        Map<String, List<HoaDonChiTiet>> detailsByOrderId = allDetails.stream()
                .filter(d -> d.getHoaDon() != null)
                .collect(Collectors.groupingBy(d -> d.getHoaDon().getId()));

        List<ChiTietSanPham> allVariants = allDetails.stream()
                .map(HoaDonChiTiet::getChiTietSanPham)
                .filter(Objects::nonNull)
                .toList();
        Map<String, List<ChiTietDotGiamGia>> discountMap = getDiscountRelationMap(allVariants);
        List<PhieuGiamGia> activeVouchers = phieuGiamGiaRepository.findAllByTrangThai(TrangThai.DANG_HOAT_DONG);

        return pendingOrders.stream()
                .map(hd -> mapToHoaDonResponse(hd, detailsByOrderId.getOrDefault(hd.getId(), java.util.Collections.emptyList()), discountMap, activeVouchers))
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    /** Tao hoa don tai quay moi, gioi han so don cho de tranh mo qua nhieu tab. */
    public AdminBanHangHoaDonResponse createHoaDon() {
        NhanVien nv = getCurrentNhanVien().orElse(null);
        String idNhanVien = nv != null ? nv.getId() : null;
        String idGiaoCa = (nv != null) ? giaoCaRepository.findGiaoCaHienTai(nv.getId()).map(com.example.be.entity.GiaoCa::getId).orElse(null) : null;
        HoaDon hoaDon = new HoaDon();
        hoaDon.setMaHoaDon(CodeUtils.generateRandom(HoaDon.class));
        hoaDon.setTrangThai(OrderStatus.CHO_XAC_NHAN);
        hoaDon.setLoaiDon("TAI_QUAY");
        hoaDon.setOrderType(OrderType.IN_STORE);
        hoaDon.setDeliveryMethod(DeliveryMethod.TAKEAWAY);
        hoaDon.setNgayTao(System.currentTimeMillis());
        hoaDon.setTongTien(BigDecimal.ZERO);
        hoaDon.setTongTienSauGiam(BigDecimal.ZERO);

        if (nv != null) {
            hoaDon.setNhanVien(nv);
            com.example.be.entity.GiaoCa activeGiaoCa = giaoCaRepository.findGiaoCaHienTai(nv.getId()).orElse(null);
            boolean isManagement = com.example.be.infrastructure.constants.VaiTro.isManagementRole(nv);
            if (activeGiaoCa != null) {
                hoaDon.setGiaoCa(activeGiaoCa);
            } else if (!isManagement) {
                throw new BusinessException("Bạn phải Mở Ca làm việc trước khi tạo hóa đơn!");
            }
        }

        if (hoaDonRepository.countPendingPOSOrders(OrderStatus.CHO_XAC_NHAN, OrderType.IN_STORE, idNhanVien, idGiaoCa) >= 5) {
            throw new BusinessException("Chỉ được tạo tối đa 5 hóa đơn chờ");
        }

        hoaDonRepository.save(hoaDon);
        return mapToHoaDonResponse(hoaDon);
    }
    /** Xác định nhân viên hiện tại để mỗi quầy chỉ khôi phục các tab thuộc phiên làm việc của mình. */
    private java.util.Optional<NhanVien> getCurrentNhanVien() {
        return SecurityUtils.getCurrentUserEmail()
                .flatMap(identifier -> nhanVienRepository.findByTenTaiKhoanOrEmailOrSdtOrMa(
                        identifier, identifier, identifier, identifier));
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "productDetail", allEntries = true)
    })
    public void deleteHoaDon(String id) {
        HoaDon hd = hoaDonRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(MessageConstants.HOA_DON_NOT_EXIST));
        List<HoaDonChiTiet> details = hoaDonChiTietRepository.findAllByHoaDon(hd);

        // Hoan tra ton kho bang lock + saveAndFlush de DB cap nhat ngay.
        for (HoaDonChiTiet d : details) {
            restoreStock(d.getChiTietSanPham().getId(), d.getSoLuong());
        }

        hoaDonChiTietRepository.deleteAll(details);

        // Soft-delete: chuyển trạng thái sang DA_HUY thay vì xóa hẳn khỏi DB
        Integer trangThaiCu = hd.getTrangThai() != null ? hd.getTrangThai().ordinal() : null;
        hd.setTrangThai(OrderStatus.DA_HUY);
        hoaDonRepository.save(hd);

        // Ghi lịch sử hủy hóa đơn
        String nguoiThucHienName = SecurityUtils.getCurrentUserEmail()
                .map(email -> nhanVienRepository.findByTenTaiKhoanOrEmailOrSdtOrMa(email, email, email, email)
                        .map(nv -> nv.getTen() != null ? nv.getTen() : email)
                        .orElse(email))
                .orElse("Hệ thống");
        LichSuTrangThaiHoaDon lichSu = LichSuTrangThaiHoaDon.builder()
                .hoaDon(hd)
                .trangThaiCu(trangThaiCu)
                .trangThaiMoi(OrderStatus.DA_HUY.ordinal())
                .ghiChu("Hủy hóa đơn chờ tại quầy")
                .nguoiThucHien(nguoiThucHienName)
                .build();
        lichSuTrangThaiHoaDonRepository.save(lichSu);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "productDetail", allEntries = true)
    })
    /** Them bien the vao gio POS va tru ton kho ngay tai thoi diem them. */
    public AdminBanHangHoaDonResponse addSanPham(String idHoaDon, AdminBanHangHoaDonChiTietRequest request) {
        if (request == null || request.getIdChiTietSanPham() == null || request.getIdChiTietSanPham().isBlank()) {
            throw new BusinessException("Thiếu thông tin biến thể sản phẩm.");
        }
        if (request.getSoLuong() == null || request.getSoLuong() <= 0) {
            throw new BusinessException("Số lượng thêm vào giỏ phải lớn hơn 0.");
        }

        // Tru ton kho bang lock + saveAndFlush truoc khi ghi dong gio hang.
        HoaDon hoaDon = getHoaDonOrThrow(idHoaDon);
        ChiTietSanPham ctsp = deductStock(request.getIdChiTietSanPham(), request.getSoLuong(), MessageConstants.PRODUCT_OUT_OF_STOCK);

        List<HoaDonChiTiet> existingItems = hoaDonChiTietRepository.findAllByHoaDonAndChiTietSanPham(hoaDon, ctsp);
        // Don gia luu vao hoa don chi tiet la gia sau dot giam gia tai thoi diem them vao gio.
        BigDecimal effectivePrice = getEffectiveVariantPrice(ctsp);
        BigDecimal basePrice = ctsp.getGiaBan() != null ? ctsp.getGiaBan() : effectivePrice;

        boolean priceChanged = false;
        String priceChangeMessage = null;

        if (!existingItems.isEmpty()) {
            java.util.Optional<HoaDonChiTiet> samePriceItem = existingItems.stream()
                    .filter(item -> item.getDonGia() != null && item.getDonGia().compareTo(effectivePrice) == 0)
                    .findFirst();

            if (samePriceItem.isPresent()) {
                // Giá không đổi: chỉ cập nhật số lượng
                HoaDonChiTiet hdct = samePriceItem.get();
                hdct.setSoLuong(hdct.getSoLuong() + request.getSoLuong());
                hoaDonChiTietRepository.save(hdct);
            } else {
                // Giá đã bị thay đổi: lấy giá trước đó làm giaCu và tạo bản ghi mới
                HoaDonChiTiet previousItem = existingItems.get(existingItems.size() - 1);
                BigDecimal oldPrice = previousItem.getDonGia();

                priceChanged = true;
                priceChangeMessage = String.format("Giá sản phẩm %s đã đổi từ %s thành %s",
                    ctsp.getSanPham() != null ? ctsp.getSanPham().getTen() : ctsp.getMaChiTietSanPham(),
                    formatCurrencyVND(oldPrice), formatCurrencyVND(effectivePrice));

                HoaDonChiTiet newHdct = HoaDonChiTiet.builder()
                        .hoaDon(hoaDon)
                        .chiTietSanPham(ctsp)
                        .soLuong(request.getSoLuong())
                        .donGia(effectivePrice)
                        .giaGoc(basePrice)
                        .giaCu(oldPrice)
                        .build();
                newHdct.setTrangThai(TrangThai.DANG_HOAT_DONG);
                newHdct.setNgayTao(System.currentTimeMillis());
                hoaDonChiTietRepository.save(newHdct);
            }
        } else {
            // Sản phẩm chưa có trong giỏ: tạo mới
            HoaDonChiTiet newHdct = HoaDonChiTiet.builder()
                    .hoaDon(hoaDon)
                    .chiTietSanPham(ctsp)
                    .soLuong(request.getSoLuong())
                    .donGia(effectivePrice)
                    .giaGoc(basePrice)
                    .build();
            newHdct.setTrangThai(TrangThai.DANG_HOAT_DONG);
            newHdct.setNgayTao(System.currentTimeMillis());
            hoaDonChiTietRepository.save(newHdct);
        }

        updateHoaDonTotals(hoaDon, true);
        AdminBanHangHoaDonResponse response = mapToHoaDonResponse(hoaDon);
        response.setPriceChanged(priceChanged);
        response.setPriceChangeMessage(priceChangeMessage);
        return response;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "productDetail", allEntries = true)
    })
    public AdminBanHangHoaDonResponse updateSoLuong(String idHoaDon, String idHoaDonChiTiet, Integer soLuong) {
        HoaDonChiTiet hdct = hoaDonChiTietRepository.findById(idHoaDonChiTiet)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.SAN_PHAM_NOT_IN_HOA_DON));

        String ctspId = hdct.getChiTietSanPham().getId();
        int oldQty = hdct.getSoLuong();

        if (soLuong == null || soLuong <= 0) {
            restoreStock(ctspId, oldQty);
            hoaDonChiTietRepository.delete(hdct);
            HoaDon hd = getHoaDonOrThrow(idHoaDon);
            if (hd.getListsHoaDonChiTiet() != null) {
                hd.getListsHoaDonChiTiet().remove(hdct);
            }
            updateHoaDonTotals(hd, true);
            return mapToHoaDonResponse(hd);
        } else {
            int delta = soLuong - oldQty;
            if (delta > 0) {
                BigDecimal currentEffectivePrice = getEffectiveVariantPrice(hdct.getChiTietSanPham());
                if (hdct.getDonGia() != null && currentEffectivePrice != null && hdct.getDonGia().compareTo(currentEffectivePrice) != 0) {
                    throw new BusinessException("Sản phẩm này đã đổi giá. Không thể tăng số lượng với giá cũ, vui lòng thêm sản phẩm với giá mới.");
                }
                deductStock(ctspId, delta, MessageConstants.PRODUCT_INSUFFICIENT_QTY);
            } else if (delta < 0) {
                restoreStock(ctspId, Math.abs(delta));
            }
            hdct.setSoLuong(soLuong);
            hoaDonChiTietRepository.save(hdct);
        }
        updateHoaDonTotals(getHoaDonOrThrow(idHoaDon), true);
        return mapToHoaDonResponse(getHoaDonOrThrow(idHoaDon));
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "productDetail", allEntries = true)
    })
    public void removeHoaDonChiTiet(String idHoaDon, String idHoaDonChiTiet) {
        HoaDonChiTiet hdct = hoaDonChiTietRepository.findById(idHoaDonChiTiet)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PRODUCT_DETAIL_NOT_FOUND));

        restoreStock(hdct.getChiTietSanPham().getId(), hdct.getSoLuong());

        hoaDonChiTietRepository.delete(hdct);

        // Cập nhật tổng tiền hóa đơn (có thể = 0 nếu giỏ trống, hóa đơn vẫn giữ trạng thái CHO_XAC_NHAN)
        HoaDon hd = getHoaDonOrThrow(idHoaDon);
        updateHoaDonTotals(hd, true);
    }

    @Override
    @Transactional
    public AdminBanHangHoaDonResponse setKhachHang(String idHoaDon, String idKhachHang) {
        HoaDon hd = getHoaDonOrThrow(idHoaDon);
        KhachHang kh = null;
        if (idKhachHang != null && !idKhachHang.isEmpty()) {
            kh = khachHangRepository.findById(idKhachHang).orElseThrow(() -> new ResourceNotFoundException(MessageConstants.KHACH_HANG_NOT_EXIST));
        }
        hd.setKhachHang(kh);
        updateHoaDonTotals(hd, true);
        return mapToHoaDonResponse(hd);
    }

    @Override
    @Transactional
    public AdminBanHangHoaDonResponse updateShippingAndChannel(String idHoaDon, com.example.be.core.admin.banhang.model.request.AdminBanHangUpdateShippingRequest request) {
        HoaDon hd = hoaDonRepository.findById(idHoaDon)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        DeliveryMethod deliveryMethod = resolveDeliveryMethod(request.getDeliveryMethod(), request.getLoaiDon());
        hd.setOrderType(OrderType.IN_STORE);
        hd.setDeliveryMethod(deliveryMethod);
        hd.setLoaiDon(toLegacyLoaiDon(deliveryMethod));
        if (deliveryMethod == DeliveryMethod.SHIPPING) {
            hd.setPhiVanChuyen(request.getPhiVanChuyen());
        } else {
            hd.setPhiVanChuyen(BigDecimal.ZERO);
        }

        hoaDonRepository.save(hd);
        updateHoaDonTotals(hd, false);

        return mapToHoaDonResponse(hd);
    }

    @Override
    @Transactional
    public AdminBanHangHoaDonResponse setPhieuGiamGia(String idHoaDon, String idPhieuGiamGia) {
        HoaDon hd = getHoaDonOrThrow(idHoaDon);
        PhieuGiamGia voucher = null;
        if (idPhieuGiamGia != null && !idPhieuGiamGia.isEmpty()) {
            voucher = phieuGiamGiaRepository.findById(idPhieuGiamGia).orElseThrow(() -> new ResourceNotFoundException(MessageConstants.VOUCHER_NOT_EXIST));
            BigDecimal total = hd.getTongTien() != null ? hd.getTongTien() : BigDecimal.ZERO;
            if (!isVoucherEligible(voucher, hd, total, System.currentTimeMillis())) {
                throw new BusinessException("Phiếu giảm giá không còn phù hợp với hóa đơn hiện tại");
            }
        }
        hd.setPhieuGiamGia(voucher);
        updateHoaDonTotals(hd, false);
        return mapToHoaDonResponse(hd);
    }

    @Override
    @Transactional
    /** Chot thanh toan POS: gan khach hang, loai don, dia chi, tong tien va lich su thanh toan. */
    public void checkout(String idHoaDon, AdminBanHangCheckoutRequest request) {
        HoaDon hd = getHoaDonOrThrow(idHoaDon);

        // Kiểm tra hóa đơn chưa được xử lý
        if (hd.getTrangThai() != OrderStatus.CHO_XAC_NHAN && hd.getTrangThai() != null) {
            throw new BusinessException("Hóa đơn này đã được xử lý hoặc thanh toán.");
        }

        List<HoaDonChiTiet> details = hoaDonChiTietRepository.findAllByHoaDon(hd);
        if (details.isEmpty()) {
            throw new BusinessException(MessageConstants.HOA_DON_EMPTY);
        }

        boolean hasInvalidQuantity = details.stream().anyMatch(d -> d.getSoLuong() == null || d.getSoLuong() <= 0);
        int tongSoLuong = details.stream().mapToInt(d -> d.getSoLuong() != null ? d.getSoLuong() : 0).sum();
        if (hasInvalidQuantity || tongSoLuong <= 0) {
            throw new BusinessException(MessageConstants.PRODUCT_OUT_OF_STOCK);
        }

        // Validate applied voucher condition before checkout
        if (hd.getPhieuGiamGia() != null) {
            PhieuGiamGia v = phieuGiamGiaRepository.findById(hd.getPhieuGiamGia().getId()).orElse(null);
            long now = System.currentTimeMillis();
            if (v == null || !isVoucherAvailableForOrder(v, hd, now)) {
                throw new BusinessException("Phiếu giảm giá áp dụng cho đơn hàng này đã hết hạn hoặc bị hủy. Vui lòng kiểm tra lại trước khi thanh toán.");
            }
            BigDecimal total = hd.getTongTien() != null ? hd.getTongTien() : BigDecimal.ZERO;
            BigDecimal minOrder = v.getDonHangToiThieu() != null ? v.getDonHangToiThieu() : BigDecimal.ZERO;
            if (total.compareTo(minOrder) < 0) {
                throw new BusinessException("Đơn hàng chưa đạt giá trị tối thiểu (" + formatCurrencyVND(minOrder) + ") của phiếu giảm giá " + getVoucherCode(v) + ". Vui lòng mua thêm hoặc gỡ phiếu giảm giá trước khi thanh toán.");
            }
        }

        // Kiểm tra sản phẩm/biến thể có bị ngừng hoạt động hoặc xóa không
        BigDecimal tongTienThucTe = BigDecimal.ZERO;
        for (HoaDonChiTiet detail : details) {
            ChiTietSanPham ctsp = detail.getChiTietSanPham();
            if (ctsp == null
                    || Boolean.TRUE.equals(ctsp.getXoaMem())
                    || (ctsp.getTrangThai() != null && ctsp.getTrangThai() != TrangThai.DANG_HOAT_DONG)) {
                String tenSP = ctsp != null && ctsp.getSanPham() != null ? ctsp.getSanPham().getTen() : "trong giỏ hàng";
                throw new BusinessException(String.format("Không thể thanh toán vì sản phẩm '%s' đã ngừng hoạt động.", tenSP));
            }
            if (ctsp.getSanPham() != null
                    && (Boolean.TRUE.equals(ctsp.getSanPham().getXoaMem())
                    || (ctsp.getSanPham().getTrangThai() != null && ctsp.getSanPham().getTrangThai() != TrangThai.DANG_HOAT_DONG))) {
                throw new BusinessException(String.format("Không thể thanh toán vì sản phẩm '%s' đã ngừng hoạt động.", ctsp.getSanPham().getTen()));
            }

            BigDecimal donGiaItem = detail.getDonGia() != null ? detail.getDonGia() : BigDecimal.ZERO;
            tongTienThucTe = tongTienThucTe.add(donGiaItem.multiply(BigDecimal.valueOf(detail.getSoLuong())));
        }

        // Xử lý Voucher từ FE gửi lên hoặc tự động tự tìm phiếu mới nếu phiếu cũ hết hạn/ngừng hoạt động
        PhieuGiamGia voucher = null;
        if (request.getIdPhieuGiamGia() != null && !request.getIdPhieuGiamGia().isEmpty()) {
            PhieuGiamGia v = phieuGiamGiaRepository.findById(request.getIdPhieuGiamGia()).orElse(null);
            long currentTime = System.currentTimeMillis();
            boolean isValidVoucher = v != null
                    && TrangThai.DANG_HOAT_DONG.equals(v.getTrangThai())
                    && (v.getNgayBatDau() == null || currentTime >= v.getNgayBatDau())
                    && (v.getNgayKetThuc() == null || currentTime <= v.getNgayKetThuc())
                    && (v.getDonHangToiThieu() == null || tongTienThucTe.compareTo(v.getDonHangToiThieu()) >= 0);

            if (isValidVoucher) {
                voucher = v;
            } else {
                // Phiếu cũ bị dừng/hết hạn -> Tự động tìm phiếu thay thế tốt nhất còn hiệu lực
                voucher = getBestVoucher(hd.getId());
            }
        } else {
            voucher = getBestVoucher(hd.getId());
        }
        hd.setPhieuGiamGia(voucher);

        // Set nhanVien based on currently authenticated user
        SecurityUtils.getCurrentUserEmail().ifPresent(username -> {
            nhanVienRepository.findByTenTaiKhoanOrEmailOrSdtOrMa(username, username, username, username).ifPresent(hd::setNhanVien);
        });

        // Tồn kho đã được trừ lúc thêm vào giỏ hàng, nên không cần trừ lại ở đây nữa.
        // Chỉ cần cập nhật trạng thái hóa đơn.

        hd.setKhachHang(resolveCheckoutCustomer(hd, request));
        DeliveryMethod deliveryMethod = resolveDeliveryMethod(request.getDeliveryMethod(), request.getLoaiDon());
        boolean shippingOrder = deliveryMethod == DeliveryMethod.SHIPPING;
        OrderStatus finalStatus = shippingOrder ? OrderStatus.XAC_NHAN : OrderStatus.HOAN_THANH;
        hd.setTrangThai(finalStatus);
        BigDecimal phiVanChuyen = shippingOrder ? normalizeMoney(request.getPhiVanChuyen()) : BigDecimal.ZERO;
        BigDecimal tienGiamVoucher = calculateVoucherDiscount(tongTienThucTe, voucher);
        BigDecimal tongSauGiamHang = tongTienThucTe.subtract(tienGiamVoucher);
        if (tongSauGiamHang.compareTo(BigDecimal.ZERO) < 0) {
            tongSauGiamHang = BigDecimal.ZERO;
        }
        // Cong thuc chot don POS: final = ship + ((tien sau dot giam) - phieu giam gia).
        BigDecimal tongTienCanThu = tongSauGiamHang.add(phiVanChuyen);

        hd.setLoaiDon(toLegacyLoaiDon(deliveryMethod));
        // Nguon API la POS: khong tin orderType do client gui len.
        hd.setOrderType(OrderType.IN_STORE);
        hd.setDeliveryMethod(deliveryMethod);
        hd.setPhiVanChuyen(phiVanChuyen);
        String tenNguoiNhan = normalizeBlank(request.getTenNguoiNhan());
        String sdtNguoiNhan = normalizeBlank(request.getSdtNguoiNhan());
        String diaChiNguoiNhan = normalizeBlank(request.getDiaChiNguoiNhan());

        if (diaChiNguoiNhan == null) {
            String detail = normalizeBlank(request.getDiaChiChiTiet());
            String tinh = normalizeBlank(request.getTinh());
            String thanhPho = normalizeBlank(request.getThanhPho());
            String phuongXa = normalizeBlank(request.getPhuongXa());
            List<String> parts = java.util.stream.Stream.of(detail, phuongXa, thanhPho, tinh)
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .toList();
            if (!parts.isEmpty()) {
                diaChiNguoiNhan = String.join(", ", parts);
            }
        }

        if (hd.getKhachHang() != null) {
            if (tenNguoiNhan == null) tenNguoiNhan = hd.getKhachHang().getTen();
            if (sdtNguoiNhan == null) sdtNguoiNhan = hd.getKhachHang().getSdt();
        }

        if (tenNguoiNhan == null) tenNguoiNhan = "Khách vãng lai";
        if (sdtNguoiNhan == null) sdtNguoiNhan = "Chưa có SĐT";
        if (diaChiNguoiNhan == null) diaChiNguoiNhan = shippingOrder ? "Chưa có địa chỉ" : "Tại cửa hàng";

        hd.setTenNguoiNhan(tenNguoiNhan);
        hd.setSoDienThoaiNguoiNhan(sdtNguoiNhan);
        hd.setDiaChiNguoiNhan(diaChiNguoiNhan);

        hd.setTongTien(tongTienThucTe); // Lấy giá trị thực tế thay vì request
        hd.setTongTienSauGiam(tongTienCanThu);
        hd.setGhiChu(request.getGhiChu());
        hd.setNgayCapNhat(System.currentTimeMillis());
        hoaDonRepository.save(hd);

        saveDefaultShippingAddressIfNeeded(hd, request);

        if (request.getTienMat() != null && request.getTienMat().compareTo(BigDecimal.ZERO) > 0) {
            createGiaoDich(hd, PaymentConstants.METHOD_TIEN_MAT, request.getTienMat(), null);
        }
        if (request.getTienChuyenKhoan() != null && request.getTienChuyenKhoan().compareTo(BigDecimal.ZERO) > 0) {
            createGiaoDich(hd, PaymentConstants.METHOD_CHUYEN_KHOAN, request.getTienChuyenKhoan(), request.getMaGiaoDich());
        }

        String nguoiThucHienName = SecurityUtils.getCurrentUserEmail()
                .map(email -> nhanVienRepository.findByTenTaiKhoanOrEmailOrSdtOrMa(email, email, email, email)
                        .map(nv -> nv.getTen() != null ? nv.getTen() : email)
                        .orElse(email))
                .orElse("Hệ thống");

        // Add history record for timeline
        LichSuTrangThaiHoaDon history = LichSuTrangThaiHoaDon.builder()
                .hoaDon(hd)
                .trangThaiCu(OrderStatus.CHO_XAC_NHAN.ordinal())
                .trangThaiMoi(finalStatus.ordinal())
                .ghiChu(shippingOrder ? "Xác nhận đơn giao hàng tại quầy" : "Thanh toán tại quầy thành công")
                .nguoiThucHien(nguoiThucHienName)
                .build();
        history.setNgayTao(System.currentTimeMillis());
        lichSuTrangThaiHoaDonRepository.save(history);
    }

    private KhachHang resolveCheckoutCustomer(HoaDon hd, AdminBanHangCheckoutRequest request) {
        String customerId = normalizeBlank(request.getIdKhachHang());
        if (customerId != null) {
            return khachHangRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.KHACH_HANG_NOT_EXIST));
        }

        if (hd.getKhachHang() != null) {
            return hd.getKhachHang();
        }

        String ten = normalizeBlank(request.getTenKhachHang());
        String sdt = normalizeBlank(request.getSdtKhachHang());
        String email = normalizeBlank(request.getEmailKhachHang());

        if (ten == null) {
            ten = normalizeBlank(request.getTenNguoiNhan());
        }
        if (sdt == null) {
            sdt = normalizeBlank(request.getSdtNguoiNhan());
        }

        if (ten == null && sdt == null && email == null) {
            return null;
        }

        // Kiểm tra xem khách hàng có tồn tại dựa trên email/SĐT trước khi tạo mới để tránh trùng lặp
        if (email != null) {
            KhachHang existedByEmail = khachHangRepository.findFirstByEmail(email).orElse(null);
            if (existedByEmail != null) {
                return existedByEmail;
            }
        }

        if (sdt != null) {
            KhachHang existedByPhone = khachHangRepository.findFirstBySdt(sdt).orElse(null);
            if (existedByPhone != null) {
                return existedByPhone;
            }
        }

        return null;
    }

    private void saveDefaultShippingAddressIfNeeded(HoaDon hd, AdminBanHangCheckoutRequest request) {
        if (hd.getKhachHang() == null) {
            return;
        }

        if (Boolean.FALSE.equals(request.getLuuDiaChiMacDinh())) {
            return;
        }

        String detail = normalizeBlank(request.getDiaChiChiTiet());
        String tinh = normalizeBlank(request.getTinh());
        String thanhPho = normalizeBlank(request.getThanhPho());
        String phuongXa = normalizeBlank(request.getPhuongXa());
        String tenNguoiNhan = normalizeBlank(request.getTenNguoiNhan());
        String sdtNguoiNhan = normalizeBlank(request.getSdtNguoiNhan());

        if (tenNguoiNhan == null) {
            tenNguoiNhan = hd.getKhachHang().getTen() != null ? hd.getKhachHang().getTen() : "Khách hàng";
        }
        if (sdtNguoiNhan == null) {
            sdtNguoiNhan = hd.getKhachHang().getSdt() != null ? hd.getKhachHang().getSdt() : "";
        }

        if (detail == null && request.getDiaChiNguoiNhan() != null) {
            detail = request.getDiaChiNguoiNhan();
        }

        if (detail == null && tinh == null && phuongXa == null) {
            return;
        }

        diaChiRepository.findByKhachHangId(hd.getKhachHang().getId()).forEach(address -> {
            address.setLaMacDinh(false);
            diaChiRepository.save(address);
        });

        DiaChi diaChi = DiaChi.builder  ()
                .khachHang(hd.getKhachHang())
                .tenNguoiNhan(tenNguoiNhan)
                .sdtNguoiNhan(sdtNguoiNhan)
                .diaChiChiTiet(detail)
                .tinh(tinh)
                .thanhPho(thanhPho)
                .phuongXa(phuongXa)
                .laMacDinh(true)
                .build();
        diaChi.setTrangThai(TrangThai.DANG_HOAT_DONG);
        diaChi.setNgayTao(System.currentTimeMillis());
        diaChi = diaChiRepository.save(diaChi);

        KhachHang khachHang = hd.getKhachHang();
        khachHang.setDiaChi(diaChi);
        khachHangRepository.save(khachHang);
    }

    private String normalizeBlank(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }


    @Override
    @Transactional(readOnly = true)
    /** Tim bien the cho man ban hang, kem gia goc/gia sau giam/badge phan tram. */
    public List<BanHangSanPhamResponse> searchSanPham(String keyword, String thuongHieu, String chatLieu, String xuatXu, String mucDich, String mauSac, String kichCo, BigDecimal minGia, BigDecimal maxGia) {
        Pageable pageable = PageRequest.of(0, 50);
        List<ChiTietSanPham> variants = chiTietSanPhamRepository.searchForPOS(keyword, thuongHieu, chatLieu, xuatXu, mucDich, mauSac, kichCo, minGia, maxGia, pageable);
        Map<String, List<ChiTietDotGiamGia>> discountMap = getDiscountRelationMap(variants);

        List<String> ids = variants.stream().map(ChiTietSanPham::getId).toList();
        Map<String, String> imageMap = new HashMap<>();
        if (!ids.isEmpty()) {
            List<AnhChiTietSanPham> images = anhChiTietSanPhamRepository
                    .findAllByChiTietSanPhamIdInAndXoaMemFalseOrderByHinhAnhDaiDienDescNgayTaoAsc(ids);
            for (AnhChiTietSanPham img : images) {
                if (img.getChiTietSanPham() != null && img.getDuongDanAnh() != null && !img.getDuongDanAnh().trim().isEmpty()) {
                    imageMap.putIfAbsent(img.getChiTietSanPham().getId(), img.getDuongDanAnh());
                }
            }
        }

        return variants
                .stream()
                .map(ct -> {
                    String imgUrl = imageMap.get(ct.getId());
                    if (imgUrl == null && ct.getSanPham() != null) {
                        imgUrl = ct.getSanPham().getHinhAnh();
                    }
                    return BanHangSanPhamResponse.builder()
                            .id(ct.getId())
                            .tenSanPham(ct.getSanPham() != null ? ct.getSanPham().getTen() : null)
                            .maSanPham(ct.getSanPham() != null ? ct.getSanPham().getMa() : null)
                            .maChiTietSanPham(ct.getMaChiTietSanPham())
                            .tenThuongHieu(ct.getSanPham() != null && ct.getSanPham().getThuongHieu() != null ? ct.getSanPham().getThuongHieu().getTen() : null)
                            .tenChatLieu(ct.getSanPham() != null && ct.getSanPham().getChatLieu() != null ? ct.getSanPham().getChatLieu().getTen() : null)
                            .tenDeGiay(ct.getSanPham() != null && ct.getSanPham().getDeGiay() != null ? ct.getSanPham().getDeGiay().getTen() : null)
                            .tenMauSac(ct.getMauSac() != null ? ct.getMauSac().getTen() : null)
                            .tenKichThuoc(ct.getKichThuoc() != null ? ct.getKichThuoc().getTen() : null)
                            .soLuongTon(ct.getSoLuong())
                            .giaGoc(getActiveDiscountPercent(ct, discountMap).compareTo(BigDecimal.ZERO) > 0 ? ct.getGiaBan() : null)
                            .giaBan(getEffectiveVariantPrice(ct, discountMap))
                            .phanTramGiam(getActiveDiscountPercent(ct, discountMap))
                            .hinhAnh(imgUrl)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<AdminBanHangKhachHangResponse> searchKhachHang(String keyword) {
        String kw = keyword != null ? keyword.trim() : "";
        return khachHangRepository.searchByKeyword(kw, org.springframework.data.domain.PageRequest.of(0, 10)).stream().map(kh -> AdminBanHangKhachHangResponse.builder()
                .id(kh.getId())
                .tenKhachHang(kh.getTen())
                .sdt(kh.getSdt())
                .email(kh.getEmail())
                .build()).collect(Collectors.toList());
    }

    @Override
    public List<PhieuGiamGia> getVouchers(BigDecimal tongTien) {
        return phieuGiamGiaRepository.findAllByTrangThai(TrangThai.DANG_HOAT_DONG);
    }

    @Override
    public PhieuGiamGia getBestVoucher(String idHoaDon) {
        HoaDon hd = getHoaDonOrThrow(idHoaDon);
        List<PhieuGiamGia> allVouchers = phieuGiamGiaRepository.findAllByTrangThai(TrangThai.DANG_HOAT_DONG);
        return getBestVoucher(hd, allVouchers);
    }

    private PhieuGiamGia getBestVoucher(HoaDon hd, List<PhieuGiamGia> allVouchers) {
        BigDecimal total = hd.getTongTien();
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0 || allVouchers == null || allVouchers.isEmpty()) return null;

        PhieuGiamGia bestVoucher = null;
        BigDecimal maxDiscount = BigDecimal.ZERO;
        long currentTime = System.currentTimeMillis();
        for (PhieuGiamGia voucher : allVouchers) {
            if (!isVoucherEligible(voucher, hd, total, currentTime)) continue;

            BigDecimal discount = getPotentialDiscount(voucher, total);
            // Ưu tiên theo số tiền giảm lớn nhất
            if (discount.compareTo(maxDiscount) > 0) {
                maxDiscount = discount;
                bestVoucher = voucher;
            } else if (discount.compareTo(maxDiscount) == 0 && discount.compareTo(BigDecimal.ZERO) > 0) {
                // Nếu giảm giá bằng nhau, lấy cái nào mức % hoặc cái mới hơn. Để đơn giản cứ giữ cái đầu.
            }
        }
        return bestVoucher;
    }

    private boolean isVoucherEligible(PhieuGiamGia voucher, HoaDon hd, BigDecimal total, long currentTime) {
        if (!isVoucherAvailableForOrder(voucher, hd, currentTime)) return false;

        BigDecimal minOrder = voucher.getDonHangToiThieu() != null
                ? voucher.getDonHangToiThieu()
                : BigDecimal.ZERO;
        return total.compareTo(minOrder) >= 0;
    }

    private boolean isVoucherAvailableForOrder(PhieuGiamGia voucher, HoaDon hd, long currentTime) {
        if (voucher == null || !TrangThai.DANG_HOAT_DONG.equals(voucher.getTrangThai())) return false;
        // Quy ước dữ liệu: -1 là phiếu vô hạn, 0 mới là đã hết lượt.
        if (voucher.getSoLuong() != null && voucher.getSoLuong() == 0) return false;
        if (voucher.getNgayBatDau() != null && currentTime < voucher.getNgayBatDau()) return false;
        if (voucher.getNgayKetThuc() != null && currentTime > voucher.getNgayKetThuc()) return false;

        boolean personal = HinhThucPhieuGiamGia.isCaNhan(voucher.getHinhThuc());
        if (!personal) return true;
        if (hd.getKhachHang() == null) return false;

        return phieuGiamGiaCaNhanRepository.findByKhachHangId(hd.getKhachHang().getId()).stream()
                .anyMatch(assigned -> assigned.getPhieuGiamGia() != null
                        && voucher.getId().equals(assigned.getPhieuGiamGia().getId())
                        && !Boolean.TRUE.equals(assigned.getDaSuDung())
                        && !Boolean.TRUE.equals(assigned.getXoaMem()));
    }

    private BigDecimal getPotentialDiscount(PhieuGiamGia v, BigDecimal baseAmount) {
        if (v == null) return BigDecimal.ZERO;
        BigDecimal amount = baseAmount.max(BigDecimal.ZERO);
        if (LoaiPhieuGiamGia.isPhanTram(v.getLoaiPhieu())) {
            Integer percent = v.getPhanTramGiamGia() != null ? v.getPhanTramGiamGia() : 0;
            BigDecimal discount = amount.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100), java.math.RoundingMode.HALF_UP);
            if (v.getGiamToiDa() != null && v.getGiamToiDa().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(v.getGiamToiDa());
            }
            return amount.min(discount.max(BigDecimal.ZERO));
        } else {
            return amount.min(v.getSoTienGiam() != null ? v.getSoTienGiam().max(BigDecimal.ZERO) : BigDecimal.ZERO);
        }
    }

    private String getVoucherCode(PhieuGiamGia v) {
        if (v == null) return "";
        if (v.getMa() != null && !v.getMa().isEmpty()) return v.getMa();
        if (v.getTen() != null && !v.getTen().isEmpty()) return v.getTen();
        return "Phiếu giảm giá";
    }

    private PhieuGiamGia getNextBetterVoucher(HoaDon hd, PhieuGiamGia bestVoucher) {
        List<PhieuGiamGia> allVouchers = phieuGiamGiaRepository.findAllByTrangThai(TrangThai.DANG_HOAT_DONG);
        return getNextBetterVoucher(hd, bestVoucher, allVouchers);
    }

    private PhieuGiamGia getNextBetterVoucher(HoaDon hd, PhieuGiamGia bestVoucher, List<PhieuGiamGia> allVouchers) {
        BigDecimal total = hd.getTongTien();
        if (total == null) total = BigDecimal.ZERO;

        BigDecimal eligibleDiscount = bestVoucher != null ? getPotentialDiscount(bestVoucher, total) : BigDecimal.ZERO;
        if (allVouchers == null || allVouchers.isEmpty()) return null;

        PhieuGiamGia nextBest = null;
        BigDecimal maxPotentialDiscount = eligibleDiscount;
        BigDecimal bestMinOrder = BigDecimal.valueOf(Long.MAX_VALUE);
        long currentTime = System.currentTimeMillis();

        for (PhieuGiamGia v : allVouchers) {
            if (!isVoucherAvailableForOrder(v, hd, currentTime)) continue;
            BigDecimal minOrder = v.getDonHangToiThieu() != null ? v.getDonHangToiThieu() : BigDecimal.ZERO;
            if (minOrder.compareTo(total) <= 0) continue; // Nếu đã đủ điều kiện rồi thì khuyến mãi đó sẽ nằm trong bestVoucher.

            // Phải test potential discount nếu họ đạt được minOrder.
            BigDecimal potDiscount = getPotentialDiscount(v, minOrder);
            if (potDiscount.compareTo(maxPotentialDiscount) > 0) {
                // Ưu tiên giảm nhiều hơn, nếu bằng nhau thì ưu tiên min order thấp hơn
                maxPotentialDiscount = potDiscount;
                bestMinOrder = minOrder;
                nextBest = v;
            } else if (potDiscount.compareTo(maxPotentialDiscount) == 0 && potDiscount.compareTo(eligibleDiscount) > 0) {
                if (minOrder.compareTo(bestMinOrder) < 0) {
                    bestMinOrder = minOrder;
                    nextBest = v;
                }
            }
        }
        return nextBest;
    }

    private String formatCurrencyVND(BigDecimal amount) {
        if (amount == null) return "0đ";
        java.text.NumberFormat format = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN"));
        return format.format(amount);
    }

    /** Tinh gia POS hien tai cua mot bien the bang cach doc dot giam gia tu DB. */
    private BigDecimal getEffectiveVariantPrice(ChiTietSanPham variant) {
        if (variant == null) {
            return BigDecimal.ZERO;
        }
        List<ChiTietDotGiamGia> relations = chiTietDotGiamGiaRepository.findAllByChiTietSanPhamIdIn(List.of(variant.getId()));
        return DiscountPriceUtils.calculateDiscountedPrice(variant.getGiaBan(), relations);
    }

    /** Tinh gia POS khi da co map dot giam gia de tranh query lap trong danh sach tim kiem. */
    private BigDecimal getEffectiveVariantPrice(ChiTietSanPham variant, Map<String, List<ChiTietDotGiamGia>> relationMap) {
        if (variant == null) {
            return BigDecimal.ZERO;
        }
        return DiscountPriceUtils.calculateDiscountedPrice(variant.getGiaBan(), relationMap.getOrDefault(variant.getId(), List.of()));
    }

    private BigDecimal getActiveDiscountPercent(ChiTietSanPham variant, Map<String, List<ChiTietDotGiamGia>> relationMap) {
        if (variant == null) {
            return BigDecimal.ZERO;
        }
        return DiscountPriceUtils.getActiveDiscountPercent(relationMap.getOrDefault(variant.getId(), List.of()));
    }

    /** Gom dot giam gia theo id bien the de FE hien thi dung gia va badge giam gia. */
    private Map<String, List<ChiTietDotGiamGia>> getDiscountRelationMap(List<ChiTietSanPham> variants) {
        if (variants == null || variants.isEmpty()) {
            return Map.of();
        }
        List<String> ids = variants.stream().map(ChiTietSanPham::getId).toList();
        List<ChiTietDotGiamGia> relations = chiTietDotGiamGiaRepository.findAllByChiTietSanPhamIdIn(ids);
        return relations.stream()
                .filter(rel -> rel.getChiTietSanPham() != null)
                .collect(Collectors.groupingBy(rel -> rel.getChiTietSanPham().getId()));
    }

    private HoaDon getHoaDonOrThrow(String id) {
        return hoaDonRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(MessageConstants.HOA_DON_NOT_EXIST));
    }

    private ChiTietSanPham deductStock(String variantId, int qty, String errorMessage) {
        ChiTietSanPham ctsp = chiTietSanPhamRepository.findByIdWithPessimisticLock(variantId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.SAN_PHAM_NOT_FOUND));

        if (Boolean.TRUE.equals(ctsp.getXoaMem()) || (ctsp.getTrangThai() != null && ctsp.getTrangThai() != TrangThai.DANG_HOAT_DONG)) {
            throw new BusinessException("Sản phẩm / biến thể này đã ngừng hoạt động.");
        }
        if (ctsp.getSanPham() != null && (Boolean.TRUE.equals(ctsp.getSanPham().getXoaMem()) || (ctsp.getSanPham().getTrangThai() != null && ctsp.getSanPham().getTrangThai() != TrangThai.DANG_HOAT_DONG))) {
            throw new BusinessException("Sản phẩm / biến thể này đã ngừng hoạt động.");
        }

        if (ctsp.getSoLuong() < qty) {
            throw new BusinessException(errorMessage);
        }
        ctsp.setSoLuong(ctsp.getSoLuong() - qty);
        return chiTietSanPhamRepository.saveAndFlush(ctsp);
    }

    private ChiTietSanPham restoreStock(String variantId, int qty) {
        ChiTietSanPham ctsp = chiTietSanPhamRepository.findByIdWithPessimisticLock(variantId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.SAN_PHAM_NOT_FOUND));
        ctsp.setSoLuong(ctsp.getSoLuong() + qty);
        return chiTietSanPhamRepository.saveAndFlush(ctsp);
    }

    /** Cap nhat tong tien hoa don moi khi gio hang/voucher thay doi. */
    private void updateHoaDonTotals(HoaDon hd, boolean autoSelectBestVoucher) {
        List<HoaDonChiTiet> details = hoaDonChiTietRepository.findAllByHoaDon(hd);
        BigDecimal total = details.stream()
                .map(d -> d.getDonGia().multiply(BigDecimal.valueOf(d.getSoLuong())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        hd.setTongTien(total);

        if (hd.getPhieuGiamGia() != null) {
            PhieuGiamGia currentVoucher = hd.getPhieuGiamGia();
            if (currentVoucher.getId() != null) {
                currentVoucher = phieuGiamGiaRepository.findById(currentVoucher.getId()).orElse(currentVoucher);
            }
            long now = System.currentTimeMillis();
            if (currentVoucher == null || !isVoucherAvailableForOrder(currentVoucher, hd, now)) {
                // Voucher bị hủy, hết hạn, hoặc hết lượt dùng -> gỡ bỏ
                hd.setPhieuGiamGia(null);
            } else {
                // Voucher vẫn còn hiệu lực (kể cả khi tổng tiền chưa đạt đơn tối thiểu)
                // Giữ nguyên voucher để mapToHoaDonResponse hiển thị cảnh báo và modal cho nhân viên
                hd.setPhieuGiamGia(currentVoucher);
            }
        }

        BigDecimal discountAmount = calculateVoucherDiscount(total, hd.getPhieuGiamGia());
        BigDecimal discounted = total.subtract(discountAmount);
        if (discounted.compareTo(BigDecimal.ZERO) < 0) discounted = BigDecimal.ZERO;
        hd.setTongTienSauGiam(discounted);
        hoaDonRepository.save(hd);
    }

    /** Tinh so tien giam cua voucher tren tien hang da tru dot giam gia san pham. */
    private BigDecimal calculateVoucherDiscount(BigDecimal subtotalAfterProductDiscount, PhieuGiamGia voucher) {
        BigDecimal subtotal = normalizeMoney(subtotalAfterProductDiscount);
        if (voucher == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal threshold = normalizeMoney(voucher.getDonHangToiThieu());
        if (subtotal.compareTo(threshold) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if (LoaiPhieuGiamGia.isPhanTram(voucher.getLoaiPhieu())) {
            Integer percent = voucher.getPhanTramGiamGia() != null ? voucher.getPhanTramGiamGia() : 0;
            discount = subtotal.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100));
            BigDecimal max = normalizeMoney(voucher.getGiamToiDa());
            if (max.compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(max) > 0) {
                discount = max;
            }
        } else {
            discount = normalizeMoney(voucher.getSoTienGiam());
        }
        return discount.min(subtotal);
    }

    /** Chuan hoa tien: null/am thi dua ve 0 de tong tien khong bi am. */
    private BigDecimal normalizeMoney(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0 ? value : BigDecimal.ZERO;
    }

    private DeliveryMethod resolveDeliveryMethod(DeliveryMethod deliveryMethod, String legacyLoaiDon) {
        if (deliveryMethod != null) {
            return deliveryMethod;
        }
        return OrderType.ONLINE.name().equalsIgnoreCase(legacyLoaiDon) || "GIAO_HANG".equalsIgnoreCase(legacyLoaiDon)
                ? DeliveryMethod.SHIPPING
                : DeliveryMethod.TAKEAWAY;
    }

    private String toLegacyLoaiDon(DeliveryMethod deliveryMethod) {
        return deliveryMethod == DeliveryMethod.SHIPPING ? "GIAO_HANG" : "TAI_QUAY";
    }

    private void createGiaoDich(HoaDon hd, String maPTTT, BigDecimal soTien, String maGiaoDichNgoai) {
        PhuongThucThanhToan pt = phuongThucThanhToanRepository.findByMa(maPTTT);
        if (pt == null) {
            pt = new PhuongThucThanhToan(maPTTT, PaymentConstants.METHOD_TIEN_MAT.equals(maPTTT) ? "Tiền mặt" : "Chuyển khoản");
            phuongThucThanhToanRepository.save(pt);
        }
        GiaoDichThanhToan gd = GiaoDichThanhToan.builder()
                .hoaDon(hd)
                .phuongThucThanhToan(pt)
                .soTien(soTien)
                .maGiaoDichNgoai(maGiaoDichNgoai)
                .loaiGiaoDich(PaymentConstants.TYPE_THANH_TOAN)
                .build();
        // Không set id thủ công: để id=null → Spring Data JPA gọi persist() → @PrePersist set id qua @GeneratedValue
        // Nếu set id thủ công → isNew()=false → Spring Data JPA gọi merge() → StaleObjectStateException
        gd.setTrangThai(TrangThai.DANG_HOAT_DONG);
        gd.setNgayTao(System.currentTimeMillis());
        giaoDichThanhToanRepository.save(gd);
    }

    private AdminBanHangHoaDonResponse mapToHoaDonResponse(HoaDon hd) {
        List<HoaDonChiTiet> chiTietList = hoaDonChiTietRepository.findAllByHoaDon(hd);
        List<ChiTietSanPham> variants = chiTietList.stream()
                .map(HoaDonChiTiet::getChiTietSanPham)
                .filter(Objects::nonNull)
                .toList();
        Map<String, List<ChiTietDotGiamGia>> discountMap = getDiscountRelationMap(variants);
        List<PhieuGiamGia> activeVouchers = phieuGiamGiaRepository.findAllByTrangThai(TrangThai.DANG_HOAT_DONG);
        return mapToHoaDonResponse(hd, chiTietList, discountMap, activeVouchers);
    }

    private AdminBanHangHoaDonResponse mapToHoaDonResponse(HoaDon hd, List<HoaDonChiTiet> chiTietList, Map<String, List<ChiTietDotGiamGia>> discountMap, List<PhieuGiamGia> activeVouchers) {
        List<AdminBanHangHoaDonChiTietResponse> detailDTOs = chiTietList.stream()
                .map(d -> {
                    ChiTietSanPham ct = d.getChiTietSanPham();
                    BigDecimal currentEffectivePrice = getEffectiveVariantPrice(ct, discountMap);
                    boolean isOldPrice = d.getDonGia() != null && currentEffectivePrice != null && d.getDonGia().compareTo(currentEffectivePrice) != 0;
                    BigDecimal itemGiaGoc = d.getGiaGoc() != null ? d.getGiaGoc() : (ct.getGiaBan() != null ? ct.getGiaBan() : d.getDonGia());
                    BigDecimal giaBan = d.getDonGia();
                    Integer phanTramGiam = null;
                    if (itemGiaGoc != null && giaBan != null && itemGiaGoc.compareTo(giaBan) > 0) {
                        BigDecimal discount = itemGiaGoc.subtract(giaBan);
                        phanTramGiam = discount.multiply(BigDecimal.valueOf(100))
                                .divide(itemGiaGoc, java.math.RoundingMode.HALF_UP).intValue();
                    }
                    String tenDotGiamGia = (phanTramGiam != null && phanTramGiam > 0)
                            ? DiscountPriceUtils.getActiveDiscountName(discountMap.getOrDefault(ct.getId(), List.of()))
                            : null;

                    return AdminBanHangHoaDonChiTietResponse.builder()
                        .id(d.getId())
                        .idChiTietSanPham(ct.getId())
                        .maChiTietSanPham(ct.getMaChiTietSanPham())
                        .tenSanPham(ct.getSanPham() != null ? ct.getSanPham().getTen() : "")
                        .tenMauSac(ct.getMauSac() != null ? ct.getMauSac().getTen() : "")
                        .tenKichThuoc(ct.getKichThuoc() != null ? ct.getKichThuoc().getTen() : "")
                        .soLuong(d.getSoLuong())
                        .donGia(d.getDonGia())
                        .giaGoc(itemGiaGoc)
                        .phanTramGiam(phanTramGiam)
                        .tenDotGiamGia(tenDotGiamGia)
                        .thanhTien(d.getDonGia().multiply(BigDecimal.valueOf(d.getSoLuong())))
                        .soLuongTon(ct.getSoLuong())
                        .hinhAnh(getHinhAnhVariant(ct))
                        .giaCu(d.getGiaCu())
                        .giaHienHanh(currentEffectivePrice)
                        .isGiaCu(isOldPrice)
                        .build();
                }).collect(Collectors.toList());

        BigDecimal tongTienHang = detailDTOs.stream()
                .map(d -> d.getGiaGoc().multiply(BigDecimal.valueOf(d.getSoLuong())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tongTien = hd.getTongTien() != null ? hd.getTongTien() : BigDecimal.ZERO;
        BigDecimal tienGiamGiaSanPham = tongTienHang.subtract(tongTien);
        BigDecimal tongTienSauGiam = hd.getTongTienSauGiam() != null ? hd.getTongTienSauGiam() : tongTien;
        BigDecimal tienGiamGiaPhieu = tongTien.subtract(tongTienSauGiam);
        BigDecimal phiVanChuyen = hd.getPhiVanChuyen() != null ? hd.getPhiVanChuyen() : BigDecimal.ZERO;
        BigDecimal thanhTien = tongTienSauGiam.add(phiVanChuyen);

        // --- VOUCHER SUGGESTION & VALIDATION LOGIC ---
        String bestVoucherId = null;
        String voucherSuggestionText = "";
        String betterVoucherSuggestionText = "";
        Boolean canApplySuggestedVoucher = false;

        Boolean voucherIneligible = false;
        String voucherIneligibleReason = null;
        String voucherIneligibleMessage = null;
        BigDecimal voucherMinOrder = null;
        BigDecimal voucherShortfall = null;

        Boolean voucherRemoved = false;
        String voucherRemovedMessage = null;

        String betterVoucherCode = null;
        String betterVoucherName = null;
        BigDecimal betterVoucherDiscount = null;
        BigDecimal currentVoucherDiscount = BigDecimal.ZERO;
        BigDecimal extraSavings = null;

        PhieuGiamGia appliedVoucher = hd.getPhieuGiamGia();
        if (appliedVoucher != null && appliedVoucher.getId() != null) {
            appliedVoucher = phieuGiamGiaRepository.findById(appliedVoucher.getId()).orElse(appliedVoucher);
        }

        long now = System.currentTimeMillis();

        if (appliedVoucher != null) {
            if (!isVoucherAvailableForOrder(appliedVoucher, hd, now)) {
                voucherRemoved = true;
                voucherRemovedMessage = "Phiếu giảm giá " + getVoucherCode(appliedVoucher) + " đã bị hủy hoặc không còn khả dụng và đã được gỡ khỏi đơn hàng.";
                appliedVoucher = null;
            } else {
                BigDecimal minOrder = appliedVoucher.getDonHangToiThieu() != null ? appliedVoucher.getDonHangToiThieu() : BigDecimal.ZERO;
                if (tongTien.compareTo(minOrder) < 0) {
                    voucherIneligible = true;
                    voucherIneligibleReason = "MIN_ORDER_NOT_MET";
                    voucherMinOrder = minOrder;
                    voucherShortfall = minOrder.subtract(tongTien);
                    voucherIneligibleMessage = "Đơn hàng chưa đạt giá trị tối thiểu của phiếu " + getVoucherCode(appliedVoucher)
                            + " (Yêu cầu: " + formatCurrencyVND(minOrder) + ", hiện tại: " + formatCurrencyVND(tongTien)
                            + ", cần thêm: " + formatCurrencyVND(voucherShortfall) + ").";
                }
                currentVoucherDiscount = calculateVoucherDiscount(tongTien, appliedVoucher);
            }
        }

        if (!chiTietList.isEmpty()) {
            PhieuGiamGia bestVoucher = getBestVoucher(hd, activeVouchers);

            if (bestVoucher != null) {
                BigDecimal bestDisc = calculateVoucherDiscount(tongTien, bestVoucher);
                if (bestDisc.compareTo(BigDecimal.ZERO) > 0) {
                    bestVoucherId = bestVoucher.getId();
                    boolean bestAlreadyApplied = appliedVoucher != null
                            && bestVoucher.getId().equals(appliedVoucher.getId());
                    BigDecimal savings = bestDisc.subtract(currentVoucherDiscount);
                    
                    if (!bestAlreadyApplied && savings.compareTo(BigDecimal.ZERO) > 0) {
                        canApplySuggestedVoucher = true;
                        betterVoucherCode = getVoucherCode(bestVoucher);
                        betterVoucherName = bestVoucher.getTen();
                        betterVoucherDiscount = bestDisc;
                        extraSavings = savings;
                        voucherSuggestionText = "Có mã giảm giá tốt hơn: " + betterVoucherCode
                                + " (-" + formatCurrencyVND(bestDisc) + ", tiết kiệm thêm " + formatCurrencyVND(savings) + ")";
                    } else if (bestAlreadyApplied) {
                        voucherSuggestionText = "Đã áp dụng mã giảm giá ưu đãi nhất: " + getVoucherCode(bestVoucher)
                                + " (-" + formatCurrencyVND(bestDisc) + ")";
                    }
                }
            } else {
                PhieuGiamGia nextBetterVoucher = getNextBetterVoucher(hd, null, activeVouchers);
                if (nextBetterVoucher == null && !Boolean.TRUE.equals(voucherIneligible)) {
                    voucherSuggestionText = "Chưa có phiếu giảm giá phù hợp cho đơn hiện tại.";
                }
            }

            PhieuGiamGia nextBetterVoucher = getNextBetterVoucher(hd, bestVoucher, activeVouchers);
            if (nextBetterVoucher != null) {
                BigDecimal minVal = nextBetterVoucher.getDonHangToiThieu() != null ? nextBetterVoucher.getDonHangToiThieu() : BigDecimal.ZERO;
                BigDecimal remaining = minVal.subtract(tongTien).max(BigDecimal.ZERO);
                BigDecimal futureBase = tongTien.max(minVal);
                betterVoucherSuggestionText = "Mua thêm " + formatCurrencyVND(remaining) + " để nhận phiếu tốt hơn: " + getVoucherCode(nextBetterVoucher) + " (-" + formatCurrencyVND(getPotentialDiscount(nextBetterVoucher, futureBase)) + ")";
            }
        }
        // --- END VOUCHER SUGGESTION LOGIC ---

        return AdminBanHangHoaDonResponse.builder()
                .id(hd.getId())
                .maHoaDon(hd.getMaHoaDon())
                .idKhachHang(hd.getKhachHang() != null ? hd.getKhachHang().getId() : null)
                .tenKhachHang(hd.getKhachHang() != null ? hd.getKhachHang().getTen() : "Khách lẻ")
                .sdtKhachHang(hd.getKhachHang() != null ? hd.getKhachHang().getSdt() : "")
                .idPhieuGiamGia(appliedVoucher != null ? appliedVoucher.getId() : null)
                .phieuGiamGia(appliedVoucher)
                .orderType(hd.getOrderType() != null ? hd.getOrderType() : OrderType.IN_STORE)
                .deliveryMethod(hd.getDeliveryMethod() != null
                        ? hd.getDeliveryMethod()
                        : resolveDeliveryMethod(null, hd.getLoaiDon()))
                .loaiDon(hd.getLoaiDon())
                .tongTienHang(tongTienHang)
                .tienGiamGiaSanPham(tienGiamGiaSanPham)
                .tongTien(tongTien)
                .tienGiamGiaPhieu(tienGiamGiaPhieu)
                .tongTienSauGiam(tongTienSauGiam)
                .phiVanChuyen(phiVanChuyen)
                .thanhTien(thanhTien)
                .listsHoaDonChiTiet(detailDTOs)
                .bestVoucherId(bestVoucherId)
                .voucherSuggestionText(voucherSuggestionText)
                .betterVoucherSuggestionText(betterVoucherSuggestionText)
                .canApplySuggestedVoucher(canApplySuggestedVoucher)
                .voucherIneligible(voucherIneligible)
                .voucherIneligibleReason(voucherIneligibleReason)
                .voucherIneligibleMessage(voucherIneligibleMessage)
                .voucherMinOrder(voucherMinOrder)
                .voucherShortfall(voucherShortfall)
                .voucherRemoved(voucherRemoved)
                .voucherRemovedMessage(voucherRemovedMessage)
                .betterVoucherCode(betterVoucherCode)
                .betterVoucherName(betterVoucherName)
                .betterVoucherDiscount(betterVoucherDiscount)
                .currentVoucherDiscount(currentVoucherDiscount)
                .extraSavings(extraSavings)
                .build();
    }

    private String getHinhAnhVariant(ChiTietSanPham ct) {
        if (ct.getAnhChiTietSanPhams() != null && !ct.getAnhChiTietSanPhams().isEmpty()) {
            for (AnhChiTietSanPham img : ct.getAnhChiTietSanPhams()) {
                if (Boolean.TRUE.equals(img.getHinhAnhDaiDien()) && !Boolean.TRUE.equals(img.getXoaMem())) {
                    return img.getDuongDanAnh();
                }
            }
            for (AnhChiTietSanPham img : ct.getAnhChiTietSanPhams()) {
                if (!Boolean.TRUE.equals(img.getXoaMem())) {
                    return img.getDuongDanAnh();
                }
            }
        }
        return ct.getSanPham() != null ? ct.getSanPham().getHinhAnh() : null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.example.be.core.admin.banhang.model.response.ProductSuggestionResponse> getProductSuggestions(String idHoaDon) {
        HoaDon hoaDon = hoaDonRepository.findById(idHoaDon)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn"));

        BigDecimal currentTotal = hoaDon.getTongTien() != null ? hoaDon.getTongTien() : BigDecimal.ZERO;

        // Logic gợi ý sản phẩm: Tìm sản phẩm có đợt giảm giá đang hoạt động
        // mà nếu thêm vào sẽ đạt điều kiện giảm giá tốt hơn
        List<com.example.be.core.admin.banhang.model.response.ProductSuggestionResponse> suggestions = java.util.Collections.emptyList();

        // Lấy danh sách sản phẩm có đợt giảm giá đang hoạt động
        long currentTime = System.currentTimeMillis();
        List<ChiTietDotGiamGia> activeDiscounts = chiTietDotGiamGiaRepository.findActiveDiscounts(currentTime);

        if (activeDiscounts == null || activeDiscounts.isEmpty() || currentTotal.compareTo(BigDecimal.ZERO) == 0) {
            return suggestions;
        }

        // Tìm sản phẩm phù hợp để gợi ý (đơn giản hóa: lấy sản phẩm đầu tiên có giảm giá)
        for (ChiTietDotGiamGia discount : activeDiscounts) {
            ChiTietSanPham variant = discount.getChiTietSanPham();
            if (variant != null && variant.getSanPham() != null
                    && variant.getSanPham().getTrangThai() == TrangThai.DANG_HOAT_DONG) {

                BigDecimal giaBan = variant.getGiaBan();
                DotGiamGia dotGiamGia = discount.getDotGiamGia();

                if (giaBan != null && dotGiamGia != null) {
                    BigDecimal soTienGiam = dotGiamGia.getSoTienGiam();
                    Integer phanTramGiam = 0;

                    // Tính % giảm giá dựa trên số tiền giảm
                    if (soTienGiam != null && giaBan.compareTo(BigDecimal.ZERO) > 0) {
                        phanTramGiam = soTienGiam.multiply(BigDecimal.valueOf(100))
                                .divide(giaBan, 0, java.math.RoundingMode.HALF_UP).intValue();
                    }

                    if (soTienGiam != null && soTienGiam.compareTo(BigDecimal.ZERO) > 0) {
                        suggestions = java.util.Collections.singletonList(
                            com.example.be.core.admin.banhang.model.response.ProductSuggestionResponse.builder()
                                .maSanPham(variant.getMaChiTietSanPham())
                                .tenSanPham(variant.getSanPham().getTen())
                                .soTienCanThem(giaBan)
                                .soTienGiam(soTienGiam)
                                .phanTramGiam(phanTramGiam)
                                .build()
                        );
                        break; // Chỉ gợi ý 1 sản phẩm
                    }
                }
            }
        }

        return suggestions;
    }

    @Override
    public com.example.be.core.admin.banhang.model.response.AdminBanHangPaymentStatusResponse checkPaymentStatus(String idHoaDon) {
        HoaDon hd = hoaDonRepository.findById(idHoaDon).orElse(null);
        if (hd == null) {
            return com.example.be.core.admin.banhang.model.response.AdminBanHangPaymentStatusResponse.builder()
                    .isPaid(false).transactionNo(null).build();
        }

        // Đơn POS sau khi IPN VNPay thành công sẽ có trạng thái XAC_NHAN
        boolean isPaid = hd.getTrangThai() == OrderStatus.XAC_NHAN || hd.getTrangThai() == OrderStatus.HOAN_THANH;
        String transactionNo = null;

        if (isPaid && hd.getListsGiaoDichThanhToan() != null) {
            for (GiaoDichThanhToan gd : hd.getListsGiaoDichThanhToan()) {
                if (gd.getMaGiaoDichNgoai() != null) {
                    transactionNo = gd.getMaGiaoDichNgoai();
                    break;
                }
            }
        }

        return com.example.be.core.admin.banhang.model.response.AdminBanHangPaymentStatusResponse.builder()
                .isPaid(isPaid)
                .transactionNo(transactionNo)
                .build();
    }
}
