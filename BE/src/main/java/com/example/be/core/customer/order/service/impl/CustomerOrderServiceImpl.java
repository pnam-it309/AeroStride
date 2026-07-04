package com.example.be.core.customer.order.service.impl;

import com.example.be.core.customer.order.model.request.CustomerOrderCheckoutRequest;
import com.example.be.core.customer.order.model.request.CustomerUpdateItemsRequest;
import com.example.be.core.customer.order.model.request.CustomerUpdateShippingRequest;
import com.example.be.core.customer.order.model.response.CustomerOrderResponse;
import com.example.be.core.customer.order.service.CustomerOrderService;
import com.example.be.core.customer.order.repository.*;
import com.example.be.entity.*;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.utils.CodeUtils;
import com.example.be.utils.DiscountPriceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerOrderServiceImpl implements CustomerOrderService {

    private final CustomerOrderHoaDonRepository hoaDonRepository;
    private final CustomerOrderHoaDonChiTietRepository hoaDonChiTietRepository;
    private final CustomerOrderChiTietSanPhamRepository chiTietSanPhamRepository;
    private final CustomerOrderKhachHangRepository khachHangRepository;
    private final CustomerOrderPhieuGiamGiaRepository phieuGiamGiaRepository;
    private final CustomerOrderPhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;
    private final com.example.be.core.customer.sanpham.repository.CustomerSanPhamChiTietDotGiamGiaRepository chiTietDotGiamGiaRepository;
    private final CustomerOrderLichSuTrangThaiHoaDonRepository lichSuRepository;
    private final CustomerOrderGiaoDichThanhToanRepository giaoDichRepository;
    private final CustomerOrderPhuongThucThanhToanRepository phuongThucRepository;
    private final com.example.be.core.payment.PaymentService paymentService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    private static final BigDecimal PHI_VAN_CHUYEN = new BigDecimal("30000");
    private static final BigDecimal FREE_SHIP_THRESHOLD = new BigDecimal("500000");

    // Xử lý logic đặt hàng trực tuyến (checkout), tính toán tổng tiền, áp dụng voucher, tạo hóa đơn và cập nhật tồn kho
    @Override
    @Transactional
    public CustomerOrderResponse checkout(CustomerOrderCheckoutRequest request, String username) {
        // 1. Tìm khách hàng
        KhachHang khachHang = khachHangRepository.findByTenTaiKhoan(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng"));

        // 2. Validate và lấy chi tiết sản phẩm
        List<ChiTietSanPham> variants = new ArrayList<>();
        Map<String, Integer> quantityMap = new HashMap<>();

        // Lấy đợt giảm giá đang áp dụng cho các biến thể để tính giá thực tế (đồng bộ trang sản phẩm/giỏ hàng)
        List<String> variantIds = request.getItems().stream()
                .map(CustomerOrderCheckoutRequest.CartItem::getIdChiTietSanPham)
                .collect(Collectors.toList());
        Map<String, List<ChiTietDotGiamGia>> relationMap = chiTietDotGiamGiaRepository
                .findAllByChiTietSanPhamIdIn(variantIds).stream()
                .filter(rel -> rel.getChiTietSanPham() != null)
                .collect(Collectors.groupingBy(rel -> rel.getChiTietSanPham().getId()));
        Map<String, BigDecimal> giaMap = new HashMap<>();

        for (CustomerOrderCheckoutRequest.CartItem item : request.getItems()) {
            ChiTietSanPham ctsp = chiTietSanPhamRepository.findById(item.getIdChiTietSanPham())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại: " + item.getIdChiTietSanPham()));

            // Giá thực tế sau đợt giảm giá của biến thể
            BigDecimal giaHienThoi = DiscountPriceUtils.calculateDiscountedPrice(
                    ctsp.getGiaBan(), relationMap.get(ctsp.getId()));
            giaMap.put(ctsp.getId(), giaHienThoi);

            if (item.getGiaDuKien() != null && giaHienThoi.compareTo(item.getGiaDuKien()) != 0) {
                String tenSP = ctsp.getSanPham() != null ? ctsp.getSanPham().getTen() : ctsp.getMaChiTietSanPham();
                throw new RuntimeException("Giá của sản phẩm '" + tenSP + "' đã thay đổi từ đợt giảm giá. Vui lòng tải lại giỏ hàng để cập nhật giá mới nhất.");
            }

            if (ctsp.getSoLuong() < item.getSoLuong()) {
                String tenSP = ctsp.getSanPham() != null ? ctsp.getSanPham().getTen() : ctsp.getMaChiTietSanPham();
                throw new RuntimeException("Sản phẩm '" + tenSP + "' chỉ còn " + ctsp.getSoLuong() + " sản phẩm");
            }

            variants.add(ctsp);
            quantityMap.put(item.getIdChiTietSanPham(), item.getSoLuong());
        }

        // 3. Tính tổng tiền (theo giá đã áp đợt giảm giá)
        BigDecimal tongTien = BigDecimal.ZERO;
        for (ChiTietSanPham ctsp : variants) {
            int soLuong = quantityMap.get(ctsp.getId());
            tongTien = tongTien.add(giaMap.get(ctsp.getId()).multiply(BigDecimal.valueOf(soLuong)));
        }

        // 4. Tính phí vận chuyển
        BigDecimal phiVanChuyen = tongTien.compareTo(FREE_SHIP_THRESHOLD) >= 0 ? BigDecimal.ZERO : PHI_VAN_CHUYEN;

        // 5. Áp dụng voucher (nếu có)
        BigDecimal tienGiam = BigDecimal.ZERO;
        PhieuGiamGia voucher = null;
        if (request.getIdPhieuGiamGia() != null && !request.getIdPhieuGiamGia().isBlank()) {
            voucher = phieuGiamGiaRepository.findById(request.getIdPhieuGiamGia())
                    .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

            // Validate voucher
            if (voucher.getTrangThai() != com.example.be.infrastructure.constants.TrangThai.DANG_HOAT_DONG) {
                throw new RuntimeException("Voucher đã bị ngừng hoạt động");
            }
            
            long now = System.currentTimeMillis();
            if (voucher.getNgayBatDau() != null && now < voucher.getNgayBatDau()) {
                throw new RuntimeException("Voucher chưa đến thời gian sử dụng");
            }
            if (voucher.getNgayKetThuc() != null && now > voucher.getNgayKetThuc()) {
                throw new RuntimeException("Voucher đã hết hạn");
            }
            if (voucher.getSoLuong() != null && voucher.getSoLuong() <= 0) {
                throw new RuntimeException("Voucher đã hết lượt sử dụng");
            }
            if (voucher.getDonHangToiThieu() != null && tongTien.compareTo(voucher.getDonHangToiThieu()) < 0) {
                throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng voucher");
            }

            // Tính tiền giảm
            if ("PHAN_TRAM".equals(voucher.getLoaiPhieu()) && voucher.getPhanTramGiamGia() != null) {
                tienGiam = tongTien.multiply(BigDecimal.valueOf(voucher.getPhanTramGiamGia()))
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
                if (voucher.getGiamToiDa() != null && tienGiam.compareTo(voucher.getGiamToiDa()) > 0) {
                    tienGiam = voucher.getGiamToiDa();
                }
            } else if (voucher.getSoTienGiam() != null) {
                tienGiam = voucher.getSoTienGiam();
            }

            // Trừ số lượng voucher
            if (voucher.getSoLuong() != null) {
                voucher.setSoLuong(voucher.getSoLuong() - 1);
                phieuGiamGiaRepository.save(voucher);
            }
        }

        BigDecimal tongTienSauGiam = tongTien.add(phiVanChuyen).subtract(tienGiam);
        if (tongTienSauGiam.compareTo(BigDecimal.ZERO) < 0) {
            tongTienSauGiam = BigDecimal.ZERO;
        }

        // 6. Tạo hóa đơn
        String diaChiFull = String.join(", ",
                request.getDiaChi() != null ? request.getDiaChi() : "",
                request.getPhuongXa() != null ? request.getPhuongXa() : "",
                request.getQuanHuyen() != null ? request.getQuanHuyen() : "",
                request.getTinhThanh() != null ? request.getTinhThanh() : ""
        );

        String recipientEmail = (request.getEmail() != null && !request.getEmail().isBlank())
                ? request.getEmail().trim()
                : (khachHang != null && khachHang.getEmail() != null ? khachHang.getEmail().trim() : null);

        HoaDon hoaDon = HoaDon.builder()
                .maHoaDon(CodeUtils.generateRandom(HoaDon.class))
                .trangThai(OrderStatus.CHO_XAC_NHAN)
                .khachHang(khachHang)
                .loaiDon("ONLINE")
                .tongTien(tongTien)
                .phiVanChuyen(phiVanChuyen)
                .tongTienSauGiam(tongTienSauGiam)
                .tenNguoiNhan(request.getTenNguoiNhan())
                .diaChiNguoiNhan(diaChiFull)
                .soDienThoaiNguoiNhan(request.getSoDienThoai())
                .emailNguoiNhan(recipientEmail)
                .ghiChu(request.getGhiChu())
                .phieuGiamGia(voucher)
                .ngayDuKienNhan(System.currentTimeMillis() + 3L * 24 * 60 * 60 * 1000) // +3 ngày
                .build();

        hoaDon = hoaDonRepository.save(hoaDon);

        // 7. Tạo chi tiết hóa đơn và trừ tồn kho
        // Đơn VNPay: CHƯA trừ kho lúc đặt hàng, sẽ trừ khi thanh toán thành công (PaymentOrderFinalizer.markPaid).
        // Đơn COD: trừ kho ngay vì không có bước thanh toán online.
        boolean laVnPay = "VNPAY".equalsIgnoreCase(request.getPhuongThucThanhToan());
        for (ChiTietSanPham ctsp : variants) {
            int soLuong = quantityMap.get(ctsp.getId());

            HoaDonChiTiet hdct = HoaDonChiTiet.builder()
                    .hoaDon(hoaDon)
                    .chiTietSanPham(ctsp)
                    .soLuong(soLuong)
                    .donGia(giaMap.get(ctsp.getId()))
                    .build();
            hoaDonChiTietRepository.save(hdct);

            // Trừ tồn kho (chỉ với COD)
            if (!laVnPay) {
                int affected = chiTietSanPhamRepository.deductStock(ctsp.getId(), soLuong);
                if (affected == 0) {
                    String tenSP = ctsp.getSanPham() != null ? ctsp.getSanPham().getTen() : ctsp.getMaChiTietSanPham();
                    throw new RuntimeException("Sản phẩm '" + tenSP + "' không đủ tồn kho. Vui lòng tải lại giỏ hàng.");
                }
            }
        }

        // 8. Tạo lịch sử trạng thái
        LichSuTrangThaiHoaDon lichSu = LichSuTrangThaiHoaDon.builder()
                .hoaDon(hoaDon)
                .trangThaiCu(null)
                .trangThaiMoi(OrderStatus.CHO_XAC_NHAN.ordinal())
                .ghiChu("Khách hàng đặt hàng online")
                .nguoiThucHien(username)
                .build();
        lichSuRepository.save(lichSu);

        // 9. Tạo giao dịch thanh toán
        PhuongThucThanhToan pttt = null;
        List<PhuongThucThanhToan> allPttt = phuongThucRepository.findAll();
        String payMethod = request.getPhuongThucThanhToan();
        for (PhuongThucThanhToan pt : allPttt) {
            if (pt.getTen() != null && pt.getTen().toUpperCase().contains(payMethod.toUpperCase())) {
                pttt = pt;
                break;
            }
        }

        GiaoDichThanhToan giaoDich = GiaoDichThanhToan.builder()
                .hoaDon(hoaDon)
                .phuongThucThanhToan(pttt)
                .soTien(tongTienSauGiam)
                .loaiGiaoDich("COD".equalsIgnoreCase(payMethod) ? "COD" : "ONLINE")
                .ghiChu("Thanh toán đơn hàng online - " + payMethod)
                .build();
        giaoDichRepository.save(giaoDich);

        if (!"VNPAY".equalsIgnoreCase(payMethod) && recipientEmail != null && !recipientEmail.isBlank()) {
            eventPublisher.publishEvent(new com.example.be.core.common.events.OrderPlacedEvent(
                    this, hoaDon.getId(), recipientEmail, tongTienSauGiam));
            log.info("Published OrderPlacedEvent for order {} to email {}", hoaDon.getId(), recipientEmail);
        }

        log.info("Checkout thành công: hoaDon={}, khachHang={}, tongTien={}", hoaDon.getId(), username, tongTienSauGiam);

        return mapToResponse(hoaDon, payMethod);
    }

    // Lấy danh sách các đơn hàng (hóa đơn online) của khách hàng, có thể lọc theo trạng thái
    @Override
    @Transactional(readOnly = true)
    public List<CustomerOrderResponse> getMyOrders(String username, String trangThai) {
        KhachHang khachHang = khachHangRepository.findByTenTaiKhoan(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng"));

        List<HoaDon> hoaDons = hoaDonRepository.findAll().stream()
                .filter(hd -> hd.getKhachHang() != null && hd.getKhachHang().getId().equals(khachHang.getId()))
                .filter(hd -> "ONLINE".equals(hd.getLoaiDon()))
                .filter(hd -> {
                    if (trangThai == null || trangThai.isBlank()) return true;
                    return hd.getTrangThai() != null && hd.getTrangThai().name().equals(trangThai);
                })
                .sorted(Comparator.comparing(HoaDon::getNgayTao, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        return hoaDons.stream().map(hd -> mapToResponse(hd, null)).collect(Collectors.toList());
    }

    // Lấy thông tin chi tiết của một đơn hàng cụ thể thuộc về khách hàng
    @Override
    @Transactional(readOnly = true)
    public CustomerOrderResponse getOrderDetail(String id, String username) {
        HoaDon hoaDon = hoaDonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        KhachHang khachHang = khachHangRepository.findByTenTaiKhoan(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng"));

        if (hoaDon.getKhachHang() == null || !hoaDon.getKhachHang().getId().equals(khachHang.getId())) {
            throw new RuntimeException("Bạn không có quyền xem đơn hàng này");
        }

        return mapToResponse(hoaDon, null);
    }

    // Hủy đơn hàng đang chờ xác nhận, hoàn lại số lượng tồn kho và lượt sử dụng voucher
    @Override
    @Transactional
    public void cancelOrder(String id, String username) {
        HoaDon hoaDon = hoaDonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        KhachHang khachHang = khachHangRepository.findByTenTaiKhoan(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng"));

        if (hoaDon.getKhachHang() == null || !hoaDon.getKhachHang().getId().equals(khachHang.getId())) {
            throw new RuntimeException("Bạn không có quyền hủy đơn hàng này");
        }

        if (hoaDon.getTrangThai() != OrderStatus.CHO_XAC_NHAN) {
            throw new RuntimeException("Chỉ có thể hủy đơn hàng ở trạng thái 'Chờ xác nhận'");
        }

        // Hoàn tồn kho — chỉ hoàn nếu kho đã thực sự bị trừ.
        // COD: trừ kho ngay lúc đặt -> cần hoàn.
        // VNPay chưa thanh toán (vẫn ở 'Chờ xác nhận'): kho CHƯA bị trừ -> KHÔNG hoàn (tránh cộng ảo).
        if (isCashOrder(hoaDon)) {
            List<HoaDonChiTiet> items = hoaDonChiTietRepository.findAllByHoaDon(hoaDon);
            for (HoaDonChiTiet hdct : items) {
                ChiTietSanPham ctsp = hdct.getChiTietSanPham();
                ctsp.setSoLuong(ctsp.getSoLuong() + hdct.getSoLuong());
                chiTietSanPhamRepository.save(ctsp);
            }
        }

        // Hoàn voucher
        if (hoaDon.getPhieuGiamGia() != null) {
            PhieuGiamGia voucher = hoaDon.getPhieuGiamGia();
            if (voucher.getSoLuong() != null) {
                voucher.setSoLuong(voucher.getSoLuong() + 1);
                phieuGiamGiaRepository.save(voucher);
            }
        }

        // Cập nhật trạng thái
        int oldStatus = hoaDon.getTrangThai().ordinal();
        hoaDon.setTrangThai(OrderStatus.DA_HUY);
        hoaDonRepository.save(hoaDon);

        // Lưu lịch sử
        LichSuTrangThaiHoaDon lichSu = LichSuTrangThaiHoaDon.builder()
                .hoaDon(hoaDon)
                .trangThaiCu(oldStatus)
                .trangThaiMoi(OrderStatus.DA_HUY.ordinal())
                .ghiChu("Khách hàng hủy đơn hàng")
                .nguoiThucHien(username)
                .build();
        lichSuRepository.save(lichSu);

        log.info("Hủy đơn hàng thành công: hoaDon={}, khachHang={}", id, username);
    }

    // Khách cập nhật thông tin nhận hàng (sđt, địa chỉ, ghi chú) khi đơn đang chờ xác nhận.
    // Việc đổi địa chỉ KHÔNG làm thay đổi phí vận chuyển đã chốt của đơn.
    @Override
    @Transactional
    public CustomerOrderResponse updateShippingInfo(String id, CustomerUpdateShippingRequest request, String username) {
        HoaDon hoaDon = findOwnedOrder(id, username);

        if (hoaDon.getTrangThai() != OrderStatus.CHO_XAC_NHAN) {
            throw new RuntimeException("Chỉ có thể sửa thông tin khi đơn hàng đang ở trạng thái 'Chờ xác nhận'");
        }

        hoaDon.setTenNguoiNhan(request.getTenNguoiNhan());
        hoaDon.setSoDienThoaiNguoiNhan(request.getSoDienThoaiNguoiNhan());
        hoaDon.setDiaChiNguoiNhan(request.getDiaChiNguoiNhan());
        hoaDon.setGhiChu(request.getGhiChu());
        hoaDon.setNgayCapNhat(System.currentTimeMillis());
        hoaDonRepository.save(hoaDon);

        // Ghi lịch sử: ai làm gì
        LichSuTrangThaiHoaDon lichSu = LichSuTrangThaiHoaDon.builder()
                .hoaDon(hoaDon)
                .trangThaiCu(hoaDon.getTrangThai().ordinal())
                .trangThaiMoi(hoaDon.getTrangThai().ordinal())
                .ghiChu("Khách hàng cập nhật thông tin nhận hàng")
                .nguoiThucHien(username)
                .build();
        lichSuRepository.save(lichSu);

        log.info("Khách cập nhật thông tin nhận hàng: hoaDon={}, khachHang={}", id, username);
        return mapToResponse(hoaDon, null);
    }

    // Khách cập nhật số lượng sản phẩm trong đơn. Chỉ cho đơn tiền mặt (COD) đang chờ xác nhận.
    // Không được để đơn rỗng; tự động cộng/trừ tồn kho và tính lại tổng tiền (kèm voucher).
    @Override
    @Transactional
    public CustomerOrderResponse updateItems(String id, CustomerUpdateItemsRequest request, String username) {
        HoaDon hoaDon = findOwnedOrder(id, username);

        if (hoaDon.getTrangThai() != OrderStatus.CHO_XAC_NHAN) {
            throw new RuntimeException("Chỉ có thể sửa sản phẩm khi đơn hàng đang ở trạng thái 'Chờ xác nhận'");
        }
        if (!isCashOrder(hoaDon)) {
            throw new RuntimeException("Đơn thanh toán chuyển khoản không được phép thay đổi sản phẩm");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Đơn hàng phải còn ít nhất 1 sản phẩm");
        }

        List<HoaDonChiTiet> currentItems = hoaDonChiTietRepository.findAllByHoaDon(hoaDon);
        Map<String, HoaDonChiTiet> hdctByVariant = new HashMap<>();
        for (HoaDonChiTiet hdct : currentItems) {
            if (hdct.getChiTietSanPham() != null) {
                hdctByVariant.put(hdct.getChiTietSanPham().getId(), hdct);
            }
        }

        // Chỉ cho phép chỉnh số lượng các biến thể đã có sẵn trong đơn (không thêm SP mới)
        StringBuilder doiGiaNote = new StringBuilder();
        for (CustomerUpdateItemsRequest.Item item : request.getItems()) {
            HoaDonChiTiet hdct = hdctByVariant.get(item.getIdChiTietSanPham());
            if (hdct == null) {
                throw new RuntimeException("Sản phẩm không thuộc đơn hàng này");
            }
            if (item.getSoLuong() == null || item.getSoLuong() < 1) {
                throw new RuntimeException("Số lượng mỗi sản phẩm phải tối thiểu là 1");
            }

            ChiTietSanPham ctsp = hdct.getChiTietSanPham();
            int delta = item.getSoLuong() - hdct.getSoLuong(); // >0: cần thêm tồn, <0: hoàn tồn
            if (delta > 0 && ctsp.getSoLuong() < delta) {
                String tenSP = ctsp.getSanPham() != null ? ctsp.getSanPham().getTen() : ctsp.getMaChiTietSanPham();
                throw new RuntimeException("Sản phẩm '" + tenSP + "' chỉ còn " + ctsp.getSoLuong() + " sản phẩm");
            }

            ctsp.setSoLuong(ctsp.getSoLuong() - delta);
            chiTietSanPhamRepository.save(ctsp);

            // Phát hiện & áp dụng đổi giá (giống bán hàng tại quầy): tạo bản ghi với đơn giá hiện hành
            BigDecimal giaCu = hdct.getDonGia();
            BigDecimal giaMoi = ctsp.getGiaBan();
            if (giaCu != null && giaMoi != null && giaCu.compareTo(giaMoi) != 0) {
                String tenSP = ctsp.getSanPham() != null ? ctsp.getSanPham().getTen() : ctsp.getMaChiTietSanPham();
                doiGiaNote.append(String.format("Giá '%s' đổi từ %sđ thành %sđ. ",
                        tenSP, giaCu.toBigInteger(), giaMoi.toBigInteger()));
                hdct.setDonGia(giaMoi);
            }

            hdct.setSoLuong(item.getSoLuong());
            hoaDonChiTietRepository.save(hdct);
        }

        recalculateTotals(hoaDon);

        // Ghi lịch sử: ai làm gì (kèm thông tin đổi giá nếu có)
        String note = "Khách hàng cập nhật số lượng sản phẩm";
        if (doiGiaNote.length() > 0) {
            note = note + ". " + doiGiaNote.toString().trim();
        }
        LichSuTrangThaiHoaDon lichSu = LichSuTrangThaiHoaDon.builder()
                .hoaDon(hoaDon)
                .trangThaiCu(hoaDon.getTrangThai().ordinal())
                .trangThaiMoi(hoaDon.getTrangThai().ordinal())
                .ghiChu(note)
                .nguoiThucHien(username)
                .build();
        lichSuRepository.save(lichSu);

        log.info("Khách cập nhật sản phẩm: hoaDon={}, khachHang={}", id, username);
        return mapToResponse(hoaDon, null);
    }

    // Tạo lại URL thanh toán VNPay cho đơn chuyển khoản chưa thanh toán (cho phép thanh toán lại khi đã tắt/hủy cổng)
    @Override
    @Transactional(readOnly = true)
    public String createVnPayUrl(String id, String returnUrl, String username) {
        HoaDon hoaDon = findOwnedOrder(id, username);

        if (isCashOrder(hoaDon)) {
            throw new RuntimeException("Đơn thanh toán khi nhận hàng (COD) không cần thanh toán online");
        }
        if (hoaDon.getTrangThai() != OrderStatus.CHO_XAC_NHAN) {
            throw new RuntimeException("Chỉ có thể thanh toán lại khi đơn đang ở trạng thái 'Chờ xác nhận'");
        }

        BigDecimal amount = hoaDon.getTongTienSauGiam() != null ? hoaDon.getTongTienSauGiam() : hoaDon.getTongTien();
        com.example.be.core.payment.dto.PaymentRequest payReq = com.example.be.core.payment.dto.PaymentRequest.builder()
                .amount(amount)
                .orderId(hoaDon.getId())
                .orderInfo("Thanh toan don hang " + hoaDon.getMaHoaDon())
                .returnUrl(returnUrl)
                .build();

        String url = paymentService.createPaymentUrl(payReq);
        log.info("Tạo lại URL VNPay: hoaDon={}, khachHang={}", id, username);
        return url;
    }

    // Lấy danh sách phiếu giảm giá hợp lệ theo tổng tiền giỏ hàng: gồm phiếu CÔNG KHAI và
    // phiếu CÁ NHÂN đã phát riêng cho khách hàng đang đăng nhập (username có thể null nếu là khách vãng lai).
    @Override
    @Transactional(readOnly = true)
    public List<PhieuGiamGia> getAvailableVouchers(BigDecimal tongTien, String username) {
        long now = System.currentTimeMillis();

        java.util.function.Predicate<PhieuGiamGia> hopLeCaNhan = v ->
                (v.getNgayBatDau() == null || now >= v.getNgayBatDau())
                && (v.getNgayKetThuc() == null || now <= v.getNgayKetThuc())
                && (v.getDonHangToiThieu() == null || tongTien == null || tongTien.compareTo(v.getDonHangToiThieu()) >= 0)
                && v.getTrangThai() == com.example.be.infrastructure.constants.TrangThai.DANG_HOAT_DONG;

        java.util.function.Predicate<PhieuGiamGia> hopLeCongKhai = v ->
                (v.getSoLuong() == null || v.getSoLuong() > 0)
                && hopLeCaNhan.test(v);

        // LinkedHashMap: loại trùng theo id, giữ thứ tự (công khai trước, cá nhân sau)
        Map<String, PhieuGiamGia> result = new LinkedHashMap<>();

        // 1. Phiếu công khai
        phieuGiamGiaRepository.findAll().stream()
                .filter(v -> "CONG_KHAI".equals(v.getHinhThuc()))
                .filter(hopLeCongKhai)
                .forEach(v -> result.put(v.getId(), v));

        // 2. Phiếu cá nhân đã phát cho khách hàng đang đăng nhập (chưa dùng, chưa xóa mềm)
        if (username != null && !username.isBlank()) {
            khachHangRepository.findByTenTaiKhoan(username).ifPresent(kh ->
                    phieuGiamGiaCaNhanRepository.findByKhachHangId(kh.getId()).stream()
                            .filter(pgn -> !Boolean.TRUE.equals(pgn.getXoaMem()))
                            .filter(pgn -> !Boolean.TRUE.equals(pgn.getDaSuDung()))
                            .map(PhieuGiamGiaCaNhan::getPhieuGiamGia)
                            .filter(Objects::nonNull)
                            .filter(hopLeCaNhan)
                            .forEach(v -> result.put(v.getId(), v))
            );
        }

        return new ArrayList<>(result.values());
    }

    // ===== PRIVATE HELPERS =====

    // Tìm đơn hàng và xác thực quyền sở hữu của khách hàng đang đăng nhập
    private HoaDon findOwnedOrder(String id, String username) {
        HoaDon hoaDon = hoaDonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        KhachHang khachHang = khachHangRepository.findByTenTaiKhoan(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng"));

        if (hoaDon.getKhachHang() == null || !hoaDon.getKhachHang().getId().equals(khachHang.getId())) {
            throw new RuntimeException("Bạn không có quyền thao tác đơn hàng này");
        }
        return hoaDon;
    }

    // Xác định đơn có phải thanh toán tiền mặt (COD) hay không.
    // COD = tiền mặt; còn lại (VNPAY/chuyển khoản/ONLINE) = không phải tiền mặt.
    private boolean isCashOrder(HoaDon hoaDon) {
        if (hoaDon.getListsGiaoDichThanhToan() != null) {
            for (GiaoDichThanhToan gd : hoaDon.getListsGiaoDichThanhToan()) {
                String loai = gd.getLoaiGiaoDich();
                if (loai != null && loai.equalsIgnoreCase("COD")) {
                    return true;
                }
                if (gd.getPhuongThucThanhToan() != null && gd.getPhuongThucThanhToan().getTen() != null) {
                    String ten = gd.getPhuongThucThanhToan().getTen().toUpperCase();
                    if (ten.contains("COD") || ten.contains("TIEN_MAT")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Tính lại tổng tiền của đơn dựa trên các dòng sản phẩm hiện tại.
    // Giữ nguyên phí vận chuyển; áp dụng lại voucher nếu vẫn còn đủ điều kiện tối thiểu.
    private void recalculateTotals(HoaDon hoaDon) {
        List<HoaDonChiTiet> items = hoaDonChiTietRepository.findAllByHoaDon(hoaDon);
        BigDecimal tongTien = items.stream()
                .map(d -> d.getDonGia().multiply(BigDecimal.valueOf(d.getSoLuong())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal phiVanChuyen = hoaDon.getPhiVanChuyen() != null ? hoaDon.getPhiVanChuyen() : BigDecimal.ZERO;

        // Tính lại tiền giảm từ voucher đang gắn (nếu còn đủ điều kiện)
        BigDecimal tienGiam = BigDecimal.ZERO;
        PhieuGiamGia voucher = hoaDon.getPhieuGiamGia();
        if (voucher != null) {
            boolean duDieuKien = voucher.getDonHangToiThieu() == null
                    || tongTien.compareTo(voucher.getDonHangToiThieu()) >= 0;
            if (duDieuKien) {
                if ("PHAN_TRAM".equals(voucher.getLoaiPhieu()) && voucher.getPhanTramGiamGia() != null) {
                    tienGiam = tongTien.multiply(BigDecimal.valueOf(voucher.getPhanTramGiamGia()))
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
                    if (voucher.getGiamToiDa() != null && tienGiam.compareTo(voucher.getGiamToiDa()) > 0) {
                        tienGiam = voucher.getGiamToiDa();
                    }
                } else if (voucher.getSoTienGiam() != null) {
                    tienGiam = voucher.getSoTienGiam();
                }
            }
        }

        BigDecimal tongTienSauGiam = tongTien.add(phiVanChuyen).subtract(tienGiam);
        if (tongTienSauGiam.compareTo(BigDecimal.ZERO) < 0) {
            tongTienSauGiam = BigDecimal.ZERO;
        }

        hoaDon.setTongTien(tongTien);
        hoaDon.setTongTienSauGiam(tongTienSauGiam);
        hoaDon.setNgayCapNhat(System.currentTimeMillis());
        hoaDonRepository.save(hoaDon);
    }

    // Mapper chuyển đổi từ entity HoaDon sang DTO CustomerOrderResponse để trả về frontend
    private CustomerOrderResponse mapToResponse(HoaDon hoaDon, String payMethod) {
        List<HoaDonChiTiet> items = hoaDonChiTietRepository.findAllByHoaDon(hoaDon);

        List<CustomerOrderResponse.OrderItemResponse> itemResponses = items.stream().map(hdct -> {
            ChiTietSanPham ctsp = hdct.getChiTietSanPham();
            String tenSanPham = "";
            String hinhAnh = "";
            String tenMauSac = "";
            String tenKichThuoc = "";

            if (ctsp != null) {
                tenSanPham = ctsp.getSanPham() != null ? ctsp.getSanPham().getTen() : "";
                if (ctsp.getAnhChiTietSanPhams() != null && !ctsp.getAnhChiTietSanPhams().isEmpty()) {
                    hinhAnh = ctsp.getAnhChiTietSanPhams().iterator().next().getDuongDanAnh();
                }
                tenMauSac = ctsp.getMauSac() != null ? ctsp.getMauSac().getTen() : "";
                tenKichThuoc = ctsp.getKichThuoc() != null ? ctsp.getKichThuoc().getTen() : "";
            }

            Integer phanTramGiam = null;
            if (ctsp != null && ctsp.getGiaBan() != null && hdct.getDonGia() != null && ctsp.getGiaBan().compareTo(hdct.getDonGia()) > 0) {
                BigDecimal giaGoc = ctsp.getGiaBan();
                BigDecimal giaBan = hdct.getDonGia();
                BigDecimal discount = giaGoc.subtract(giaBan);
                phanTramGiam = discount.multiply(BigDecimal.valueOf(100))
                        .divide(giaGoc, java.math.RoundingMode.HALF_UP).intValue();
            }

            return CustomerOrderResponse.OrderItemResponse.builder()
                    .id(hdct.getId())
                    .idChiTietSanPham(ctsp != null ? ctsp.getId() : null)
                    .giaHienTai(ctsp != null ? ctsp.getGiaBan() : null)
                    .tenSanPham(tenSanPham)
                    .hinhAnh(hinhAnh)
                    .tenMauSac(tenMauSac)
                    .tenKichThuoc(tenKichThuoc)
                    .donGia(hdct.getDonGia())
                    .phanTramGiam(phanTramGiam)
                    .soLuong(hdct.getSoLuong())
                    .thanhTien(hdct.getDonGia().multiply(BigDecimal.valueOf(hdct.getSoLuong())))
                    .build();
        }).collect(Collectors.toList());

        // Lịch sử trạng thái
        List<CustomerOrderResponse.OrderStatusHistory> lichSuList = new ArrayList<>();
        if (hoaDon.getListsLichSuHoaDon() != null) {
            lichSuList = hoaDon.getListsLichSuHoaDon().stream()
                    .map(ls -> CustomerOrderResponse.OrderStatusHistory.builder()
                            .trangThai(ls.getTrangThaiMoi() != null ? OrderStatus.values()[ls.getTrangThaiMoi()].name() : "")
                            .trangThaiDisplay(ls.getTrangThaiMoi() != null ? CustomerOrderResponse.mapTrangThaiDisplay(OrderStatus.values()[ls.getTrangThaiMoi()].name()) : "")
                            .thoiGian(ls.getNgayTao())
                            .ghiChu(ls.getGhiChu())
                            .build())
                    .collect(Collectors.toList());
        }

        // Tính tiền giảm
        BigDecimal tienGiam = BigDecimal.ZERO;
        if (hoaDon.getTongTien() != null && hoaDon.getTongTienSauGiam() != null && hoaDon.getPhiVanChuyen() != null) {
            tienGiam = hoaDon.getTongTien().add(hoaDon.getPhiVanChuyen()).subtract(hoaDon.getTongTienSauGiam());
            if (tienGiam.compareTo(BigDecimal.ZERO) < 0) tienGiam = BigDecimal.ZERO;
        }

        // Tên người nhận: ưu tiên giá trị đã lưu trên đơn, nếu trống thì lấy từ khách hàng
        String tenNguoiNhan = hoaDon.getTenNguoiNhan();
        if ((tenNguoiNhan == null || tenNguoiNhan.isBlank()) && hoaDon.getKhachHang() != null) {
            tenNguoiNhan = hoaDon.getKhachHang().getTen();
        }

        // Payment method
        String phuongThuc = payMethod;
        if (phuongThuc == null && hoaDon.getListsGiaoDichThanhToan() != null && !hoaDon.getListsGiaoDichThanhToan().isEmpty()) {
            GiaoDichThanhToan gd = hoaDon.getListsGiaoDichThanhToan().iterator().next();
            if (gd.getPhuongThucThanhToan() != null) {
                phuongThuc = gd.getPhuongThucThanhToan().getTen();
            } else {
                phuongThuc = gd.getLoaiGiaoDich();
            }
        }

        // Phân quyền thao tác theo trạng thái + phương thức thanh toán
        boolean choXacNhan = hoaDon.getTrangThai() == OrderStatus.CHO_XAC_NHAN;
        boolean laTienMat = isCashOrder(hoaDon);
        // Sửa thông tin nhận hàng: cho phép khi đang chờ xác nhận (cả tiền mặt & chuyển khoản),
        // riêng chuyển khoản đổi địa chỉ không làm đổi phí ship.
        boolean choPhepSuaThongTin = choXacNhan;
        // Sửa số lượng sản phẩm: chỉ đơn tiền mặt (COD) và đang chờ xác nhận.
        boolean choPhepSuaSanPham = choXacNhan && laTienMat;
        boolean choPhepHuy = choXacNhan;
        // Đơn chuyển khoản (không phải tiền mặt) còn chờ xác nhận -> cho thanh toán lại
        boolean choPhepThanhToanLai = choXacNhan && !laTienMat;

        return CustomerOrderResponse.builder()
                .id(hoaDon.getId())
                .maHoaDon(hoaDon.getMaHoaDon())
                .laTienMat(laTienMat)
                .choPhepSuaThongTin(choPhepSuaThongTin)
                .choPhepSuaSanPham(choPhepSuaSanPham)
                .choPhepHuy(choPhepHuy)
                .choPhepThanhToanLai(choPhepThanhToanLai)
                .trangThai(hoaDon.getTrangThai() != null ? hoaDon.getTrangThai().name() : "")
                .trangThaiDisplay(hoaDon.getTrangThai() != null ? CustomerOrderResponse.mapTrangThaiDisplay(hoaDon.getTrangThai().name()) : "")
                .tongTien(hoaDon.getTongTien())
                .phiVanChuyen(hoaDon.getPhiVanChuyen())
                .tongTienSauGiam(hoaDon.getTongTienSauGiam())
                .tienGiam(tienGiam)
                .diaChiNguoiNhan(hoaDon.getDiaChiNguoiNhan())
                .soDienThoaiNguoiNhan(hoaDon.getSoDienThoaiNguoiNhan())
                .tenNguoiNhan(tenNguoiNhan)
                .ghiChu(hoaDon.getGhiChu())
                .loaiDon(hoaDon.getLoaiDon())
                .phuongThucThanhToan(phuongThuc)
                .ngayTao(hoaDon.getNgayTao())
                .ngayCapNhat(hoaDon.getNgayCapNhat())
                .ngayDuKienNhan(hoaDon.getNgayDuKienNhan())
                .tenVoucher(hoaDon.getPhieuGiamGia() != null ? hoaDon.getPhieuGiamGia().getTen() : null)
                .maVoucher(hoaDon.getPhieuGiamGia() != null ? hoaDon.getPhieuGiamGia().getMa() : null)
                .items(itemResponses)
                .lichSuTrangThai(lichSuList)
                .build();
    }

    @Override
    public com.example.be.core.customer.order.model.response.CustomerOrderStatsResponse getMyOrderStats(String username) {
        KhachHang khachHang = khachHangRepository.findByTenTaiKhoan(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        List<HoaDon> allOrders = hoaDonRepository.findByKhachHangId(khachHang.getId());

        long total = allOrders.size();
        long completed = allOrders.stream().filter(o -> o.getTrangThai() != null && o.getTrangThai() == OrderStatus.HOAN_THANH).count();
        long cancelled = allOrders.stream().filter(o -> o.getTrangThai() != null && o.getTrangThai() == OrderStatus.DA_HUY).count();
        long delivering = allOrders.stream().filter(o -> o.getTrangThai() != null && o.getTrangThai() == OrderStatus.DANG_GIAO).count();

        return com.example.be.core.customer.order.model.response.CustomerOrderStatsResponse.builder()
                .total(total)
                .completed(completed)
                .cancelled(cancelled)
                .delivering(delivering)
                .build();
    }
}
