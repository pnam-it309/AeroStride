package com.example.be.core.customer.order.service.impl;

import com.example.be.core.customer.order.model.request.CustomerOrderCheckoutRequest;
import com.example.be.core.customer.order.model.response.CustomerOrderResponse;
import com.example.be.core.customer.order.service.CustomerOrderService;
import com.example.be.core.customer.order.repository.*;
import com.example.be.entity.*;
import com.example.be.infrastructure.constants.OrderStatus;
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
    private final CustomerOrderLichSuTrangThaiHoaDonRepository lichSuRepository;
    private final CustomerOrderGiaoDichThanhToanRepository giaoDichRepository;
    private final CustomerOrderPhuongThucThanhToanRepository phuongThucRepository;

    private static final BigDecimal PHI_VAN_CHUYEN = new BigDecimal("30000");
    private static final BigDecimal FREE_SHIP_THRESHOLD = new BigDecimal("5000000");

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

        for (CustomerOrderCheckoutRequest.CartItem item : request.getItems()) {
            ChiTietSanPham ctsp = chiTietSanPhamRepository.findById(item.getIdChiTietSanPham())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại: " + item.getIdChiTietSanPham()));

            if (item.getGiaDuKien() != null && ctsp.getGiaBan().compareTo(item.getGiaDuKien()) != 0) {
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

        // 3. Tính tổng tiền
        BigDecimal tongTien = BigDecimal.ZERO;
        for (ChiTietSanPham ctsp : variants) {
            int soLuong = quantityMap.get(ctsp.getId());
            tongTien = tongTien.add(ctsp.getGiaBan().multiply(BigDecimal.valueOf(soLuong)));
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

        HoaDon hoaDon = HoaDon.builder()
                .trangThai(OrderStatus.CHO_XAC_NHAN)
                .khachHang(khachHang)
                .loaiDon("ONLINE")
                .tongTien(tongTien)
                .phiVanChuyen(phiVanChuyen)
                .tongTienSauGiam(tongTienSauGiam)
                .diaChiNguoiNhan(diaChiFull)
                .soDienThoaiNguoiNhan(request.getSoDienThoai())
                .ghiChu(request.getGhiChu())
                .phieuGiamGia(voucher)
                .ngayDuKienNhan(System.currentTimeMillis() + 3L * 24 * 60 * 60 * 1000) // +3 ngày
                .build();

        hoaDon = hoaDonRepository.save(hoaDon);

        // 7. Tạo chi tiết hóa đơn và trừ tồn kho
        for (ChiTietSanPham ctsp : variants) {
            int soLuong = quantityMap.get(ctsp.getId());

            HoaDonChiTiet hdct = HoaDonChiTiet.builder()
                    .hoaDon(hoaDon)
                    .chiTietSanPham(ctsp)
                    .soLuong(soLuong)
                    .donGia(ctsp.getGiaBan())
                    .build();
            hoaDonChiTietRepository.save(hdct);

            // Trừ tồn kho
            ctsp.setSoLuong(ctsp.getSoLuong() - soLuong);
            chiTietSanPhamRepository.save(ctsp);
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

        // Hoàn tồn kho
        List<HoaDonChiTiet> items = hoaDonChiTietRepository.findAllByHoaDon(hoaDon);
        for (HoaDonChiTiet hdct : items) {
            ChiTietSanPham ctsp = hdct.getChiTietSanPham();
            ctsp.setSoLuong(ctsp.getSoLuong() + hdct.getSoLuong());
            chiTietSanPhamRepository.save(ctsp);
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

    // Lấy danh sách phiếu giảm giá công khai hợp lệ dựa theo tổng tiền giỏ hàng hiện hành
    @Override
    public List<PhieuGiamGia> getAvailableVouchers(BigDecimal tongTien) {
        long now = System.currentTimeMillis();
        return phieuGiamGiaRepository.findAll().stream()
                .filter(v -> "CONG_KHAI".equals(v.getHinhThuc()))
                .filter(v -> v.getSoLuong() == null || v.getSoLuong() > 0)
                .filter(v -> v.getNgayBatDau() == null || now >= v.getNgayBatDau())
                .filter(v -> v.getNgayKetThuc() == null || now <= v.getNgayKetThuc())
                .filter(v -> v.getDonHangToiThieu() == null || tongTien == null || tongTien.compareTo(v.getDonHangToiThieu()) >= 0)
                .collect(Collectors.toList());
    }

    // ===== PRIVATE HELPERS =====

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

            return CustomerOrderResponse.OrderItemResponse.builder()
                    .id(hdct.getId())
                    .tenSanPham(tenSanPham)
                    .hinhAnh(hinhAnh)
                    .tenMauSac(tenMauSac)
                    .tenKichThuoc(tenKichThuoc)
                    .donGia(hdct.getDonGia())
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

        // Tên người nhận: lấy từ địa chỉ hoặc khách hàng
        String tenNguoiNhan = "";
        if (hoaDon.getKhachHang() != null) {
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

        return CustomerOrderResponse.builder()
                .id(hoaDon.getId())
                .maHoaDon(hoaDon.getMaHoaDon())
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
}
