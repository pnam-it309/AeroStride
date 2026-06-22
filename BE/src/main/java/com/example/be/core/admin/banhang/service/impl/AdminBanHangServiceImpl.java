package com.example.be.core.admin.banhang.service.impl;

import com.example.be.core.admin.banhang.model.request.AdminBanHangCheckoutRequest;
import com.example.be.core.admin.banhang.model.request.AdminBanHangHoaDonChiTietRequest;
import com.example.be.core.admin.banhang.model.response.AdminBanHangHoaDonChiTietResponse;
import com.example.be.core.admin.banhang.model.response.AdminBanHangHoaDonResponse;
import com.example.be.core.admin.banhang.model.response.AdminBanHangKhachHangResponse;
import com.example.be.core.admin.banhang.model.response.BanHangSanPhamResponse;
import com.example.be.core.admin.banhang.repository.*;
import com.example.be.core.admin.banhang.service.AdminBanHangService;
import com.example.be.entity.*;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.constants.MessageConstants;
import com.example.be.infrastructure.exceptions.BusinessException;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.repository.DiaChiRepository;
import com.example.be.utils.HelperUtils;
import com.example.be.utils.CodeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

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
    private final DiaChiRepository diaChiRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminBanHangHoaDonResponse> getHoaDonCho() {
        return hoaDonRepository.findAllByTrangThaiAndLoaiDon(OrderStatus.CHO_XAC_NHAN, "TAI_QUAY")
                .stream().map(this::mapToHoaDonResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AdminBanHangHoaDonResponse createHoaDon() {
        if (hoaDonRepository.countByTrangThaiAndLoaiDon(OrderStatus.CHO_XAC_NHAN, "TAI_QUAY") >= 5) {
            throw new BusinessException(MessageConstants.HOA_DON_WAITING_LIMIT);
        }
        HoaDon hoaDon = new HoaDon();
        hoaDon.setMaHoaDon(CodeUtils.generateRandom(HoaDon.class));
        hoaDon.setTrangThai(OrderStatus.CHO_XAC_NHAN);
        hoaDon.setLoaiDon("TAI_QUAY");
        hoaDon.setNgayTao(System.currentTimeMillis());
        hoaDon.setTongTien(BigDecimal.ZERO);
        hoaDon.setTongTienSauGiam(BigDecimal.ZERO);
        hoaDonRepository.save(hoaDon);
        return mapToHoaDonResponse(hoaDon);
    }

    @Override
    @Transactional
    public void deleteHoaDon(String id) {
        HoaDon hd = hoaDonRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(MessageConstants.HOA_DON_NOT_EXIST));
        List<HoaDonChiTiet> details = hoaDonChiTietRepository.findAllByHoaDon(hd);

        // Hoàn trả tồn kho bằng atomic UPDATE
        for (HoaDonChiTiet d : details) {
            chiTietSanPhamRepository.restoreStock(d.getChiTietSanPham().getId(), d.getSoLuong());
        }

        hoaDonChiTietRepository.deleteAll(details);
        hoaDonRepository.delete(hd);
    }

    @Override
    @Transactional
    public AdminBanHangHoaDonResponse addSanPham(String idHoaDon, AdminBanHangHoaDonChiTietRequest request) {
        if (request == null || request.getIdChiTietSanPham() == null || request.getIdChiTietSanPham().isBlank()) {
            throw new BusinessException("Thiếu thông tin biến thể sản phẩm.");
        }
        if (request.getSoLuong() == null || request.getSoLuong() <= 0) {
            throw new BusinessException("Số lượng thêm vào giỏ phải lớn hơn 0.");
        }

        // Bước 1: Trừ tồn kho bằng atomic UPDATE
        int affected = chiTietSanPhamRepository.deductStock(request.getIdChiTietSanPham(), request.getSoLuong());
        if (affected == 0) {
            throw new BusinessException(MessageConstants.PRODUCT_OUT_OF_STOCK);
        }

        HoaDon hoaDon = getHoaDonOrThrow(idHoaDon);
        ChiTietSanPham ctsp = chiTietSanPhamRepository.findById(request.getIdChiTietSanPham())
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.SAN_PHAM_NOT_FOUND));

        HoaDonChiTiet hdct = hoaDonChiTietRepository.findByHoaDonAndChiTietSanPham(hoaDon, ctsp);
        if (hdct != null) {
            hdct.setSoLuong(hdct.getSoLuong() + request.getSoLuong());
        } else {
            hdct = HoaDonChiTiet.builder()
                    .hoaDon(hoaDon)
                    .chiTietSanPham(ctsp)
                    .soLuong(request.getSoLuong())
                    .donGia(ctsp.getGiaBan())
                    .build();
            hdct.setTrangThai(TrangThai.DANG_HOAT_DONG);
            hdct.setNgayTao(System.currentTimeMillis());
        }
        hoaDonChiTietRepository.save(hdct);

        updateHoaDonTotals(hoaDon);
        return mapToHoaDonResponse(hoaDon);
    }

    @Override
    @Transactional
    public AdminBanHangHoaDonResponse updateSoLuong(String idHoaDon, String idHoaDonChiTiet, Integer soLuong) {
        HoaDonChiTiet hdct = hoaDonChiTietRepository.findById(idHoaDonChiTiet)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.SAN_PHAM_NOT_IN_HOA_DON));

        String ctspId = hdct.getChiTietSanPham().getId();
        int oldQty = hdct.getSoLuong();

        if (soLuong <= 0) {
            chiTietSanPhamRepository.restoreStock(ctspId, oldQty);
            hoaDonChiTietRepository.delete(hdct);
        } else {
            int delta = soLuong - oldQty;
            if (delta > 0) {
                int affected = chiTietSanPhamRepository.deductStock(ctspId, delta);
                if (affected == 0) {
                    throw new BusinessException(MessageConstants.PRODUCT_INSUFFICIENT_QTY);
                }
            } else if (delta < 0) {
                chiTietSanPhamRepository.restoreStock(ctspId, Math.abs(delta));
            }
            hdct.setSoLuong(soLuong);
            hoaDonChiTietRepository.save(hdct);
        }
        updateHoaDonTotals(getHoaDonOrThrow(idHoaDon));
        return mapToHoaDonResponse(getHoaDonOrThrow(idHoaDon));
    }

    @Override
    @Transactional
    public void removeHoaDonChiTiet(String idHoaDon, String idHoaDonChiTiet) {
        HoaDonChiTiet hdct = hoaDonChiTietRepository.findById(idHoaDonChiTiet)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PRODUCT_DETAIL_NOT_FOUND));

        chiTietSanPhamRepository.restoreStock(hdct.getChiTietSanPham().getId(), hdct.getSoLuong());

        hoaDonChiTietRepository.delete(hdct);
        
        HoaDon hd = getHoaDonOrThrow(idHoaDon);
        List<HoaDonChiTiet> remainingDetails = hoaDonChiTietRepository.findAllByHoaDon(hd);
        boolean isEmpty = remainingDetails.isEmpty() || (remainingDetails.size() == 1 && remainingDetails.get(0).getId().equals(hdct.getId()));
        if (isEmpty) {
            hd.setTrangThai(OrderStatus.DA_HUY);
            hoaDonRepository.save(hd);
        } else {
            updateHoaDonTotals(hd);
        }
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
        hoaDonRepository.save(hd);
        return mapToHoaDonResponse(hd);
    }

    @Override
    @Transactional
    public AdminBanHangHoaDonResponse setPhieuGiamGia(String idHoaDon, String idPhieuGiamGia) {
        HoaDon hd = getHoaDonOrThrow(idHoaDon);
        PhieuGiamGia voucher = null;
        if (idPhieuGiamGia != null && !idPhieuGiamGia.isEmpty()) {
            voucher = phieuGiamGiaRepository.findById(idPhieuGiamGia).orElseThrow(() -> new ResourceNotFoundException(MessageConstants.VOUCHER_NOT_EXIST));
        }
        hd.setPhieuGiamGia(voucher);
        updateHoaDonTotals(hd);
        return mapToHoaDonResponse(hd);
    }

    @Override
    @Transactional
    public void checkout(String idHoaDon, AdminBanHangCheckoutRequest request) {
        HoaDon hd = getHoaDonOrThrow(idHoaDon);

        // Kiểm tra hóa đơn chưa được thanh toán
        if (hd.getTrangThai() == OrderStatus.HOAN_THANH) {
            throw new BusinessException("Hóa đơn này đã được thanh toán.");
        }

        List<HoaDonChiTiet> details = hoaDonChiTietRepository.findAllByHoaDon(hd);
        if (details.isEmpty()) {
            throw new BusinessException(MessageConstants.HOA_DON_EMPTY);
        }

        // Kiểm tra giá sản phẩm có bị thay đổi không
        BigDecimal tongTienThucTe = BigDecimal.ZERO;
        for (HoaDonChiTiet detail : details) {
            BigDecimal giaHienTai = detail.getChiTietSanPham().getGiaBan();
            if (giaHienTai == null) giaHienTai = BigDecimal.ZERO;
            if (detail.getDonGia() != null && detail.getDonGia().compareTo(giaHienTai) != 0) {
                throw new BusinessException("Giá sản phẩm đã thay đổi. Vui lòng tải lại giỏ hàng.");
            }
            tongTienThucTe = tongTienThucTe.add(giaHienTai.multiply(BigDecimal.valueOf(detail.getSoLuong())));
        }

        // Xử lý Voucher từ FE gửi lên (vì FE tính offline)
        if (request.getIdPhieuGiamGia() != null && !request.getIdPhieuGiamGia().isEmpty()) {
            PhieuGiamGia v = phieuGiamGiaRepository.findById(request.getIdPhieuGiamGia())
                    .orElseThrow(() -> new BusinessException("Voucher không tồn tại."));
            if (v.getTrangThai() != com.example.be.infrastructure.constants.TrangThai.DANG_HOAT_DONG) {
                 throw new BusinessException("Voucher đã hết hạn hoặc không còn hiệu lực. Vui lòng tải lại giỏ hàng.");
            }
            BigDecimal threshold = v.getDonHangToiThieu() != null ? v.getDonHangToiThieu() : BigDecimal.ZERO;
            if (tongTienThucTe.compareTo(threshold) < 0) {
                 throw new BusinessException("Tổng tiền không đủ điều kiện áp dụng Voucher. Vui lòng tải lại giỏ hàng.");
            }
            hd.setPhieuGiamGia(v);
        } else {
            hd.setPhieuGiamGia(null);
        }

        // Tồn kho đã được trừ lúc thêm vào giỏ hàng, nên không cần trừ lại ở đây nữa.
        // Chỉ cần cập nhật trạng thái hóa đơn.

        hd.setKhachHang(resolveCheckoutCustomer(hd, request));
        hd.setTrangThai(OrderStatus.HOAN_THANH);
        hd.setLoaiDon(request.getLoaiDon());
        hd.setOrderType(com.example.be.infrastructure.constants.OrderType.IN_STORE); // Luôn là IN_STORE cho POS
        hd.setPhiVanChuyen(request.getPhiVanChuyen() != null ? request.getPhiVanChuyen() : BigDecimal.ZERO);
        hd.setDiaChiNguoiNhan(normalizeBlank(request.getDiaChiNguoiNhan()));
        hd.setSoDienThoaiNguoiNhan(normalizeBlank(request.getSdtNguoiNhan()));
        hd.setTongTien(tongTienThucTe); // Lấy giá trị thực tế thay vì request
        hd.setTongTienSauGiam(request.getTongTienSauGiam()); // FE đã tính, nhưng có thể tính lại cho an toàn. Tạm dùng FE hoặc gọi hàm tính lại. 
        hd.setGhiChu(request.getGhiChu());
        hd.setNgayCapNhat(System.currentTimeMillis());
        hoaDonRepository.save(hd);

        saveDefaultShippingAddressIfNeeded(hd, request);

        if (request.getTienMat() != null && request.getTienMat().compareTo(BigDecimal.ZERO) > 0) {
            createGiaoDich(hd, "TIEN_MAT", request.getTienMat(), null);
        }
        if (request.getTienChuyenKhoan() != null && request.getTienChuyenKhoan().compareTo(BigDecimal.ZERO) > 0) {
            createGiaoDich(hd, "CHUYEN_KHOAN", request.getTienChuyenKhoan(), request.getMaGiaoDich());
        }
    }

    private KhachHang resolveCheckoutCustomer(HoaDon hd, AdminBanHangCheckoutRequest request) {
        String customerId = normalizeBlank(request.getIdKhachHang());
        if (customerId != null) {
            return khachHangRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.KHACH_HANG_NOT_EXIST));
        }

        String ten = normalizeBlank(request.getTenKhachHang());
        String sdt = normalizeBlank(request.getSdtKhachHang());
        String email = normalizeBlank(request.getEmailKhachHang());

        if (ten == null && sdt == null && email == null) {
            return hd.getKhachHang();
        }

        if (ten == null || sdt == null) {
            throw new BusinessException("Vui lòng nhập đầy đủ tên khách hàng và số điện thoại để thêm khách hàng mới.");
        }

        KhachHang existedByPhone = khachHangRepository.findFirstBySdt(sdt).orElse(null);
        if (existedByPhone != null) {
            return existedByPhone;
        }

        if (email != null) {
            KhachHang existedByEmail = khachHangRepository.findFirstByEmail(email).orElse(null);
            if (existedByEmail != null) {
                return existedByEmail;
            }
        }

        KhachHang khachHang = new KhachHang();
        khachHang.setMa(CodeUtils.generateRandom(KhachHang.class, khachHangRepository::existsByMa));
        khachHang.setTen(ten);
        khachHang.setSdt(sdt);
        khachHang.setEmail(email);
        khachHang.setGioiTinh(request.getGioiTinhKhachHang());
        khachHang.setNgaySinh(request.getNgaySinhKhachHang());
        khachHang.setGhiChu("Khách tạo từ bán hàng tại quầy");
        khachHang.setTrangThai(TrangThai.DANG_HOAT_DONG);
        khachHang.setXoaMem(false);
        khachHang.setNgayTao(System.currentTimeMillis());
        return khachHangRepository.save(khachHang);
    }

    private void saveDefaultShippingAddressIfNeeded(HoaDon hd, AdminBanHangCheckoutRequest request) {
        if (!Boolean.TRUE.equals(request.getLuuDiaChiMacDinh()) || hd.getKhachHang() == null) {
            return;
        }

        String detail = normalizeBlank(request.getDiaChiChiTiet());
        String tinh = normalizeBlank(request.getTinh());
        String thanhPho = normalizeBlank(request.getThanhPho());
        String phuongXa = normalizeBlank(request.getPhuongXa());
        String tenNguoiNhan = normalizeBlank(request.getTenNguoiNhan());
        String sdtNguoiNhan = normalizeBlank(request.getSdtNguoiNhan());

        if (detail == null || tinh == null || thanhPho == null || phuongXa == null || tenNguoiNhan == null || sdtNguoiNhan == null) {
            return;
        }

        diaChiRepository.findByKhachHangId(hd.getKhachHang().getId()).forEach(address -> {
            address.setLaMacDinh(false);
            diaChiRepository.save(address);
        });

        DiaChi diaChi = DiaChi.builder()
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
    public List<BanHangSanPhamResponse> searchSanPham(String keyword) {
        // Limit results to avoid loading the whole DB when keyword is empty
        return chiTietSanPhamRepository
                .searchByKeywordLite(keyword, PageRequest.of(0, 2000))
                .getContent()
                .stream()
                .map(ct -> BanHangSanPhamResponse.builder()
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
                        .giaBan(ct.getGiaBan())
                        .hinhAnh(getHinhAnhVariant(ct))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<AdminBanHangKhachHangResponse> searchKhachHang(String keyword) {
        return khachHangRepository.searchByKeyword(keyword).stream().map(kh -> AdminBanHangKhachHangResponse.builder()
                .id(kh.getId())
                .tenKhachHang(kh.getTen())
                .sdt(kh.getSdt())
                .email(kh.getEmail())
                .build()).collect(Collectors.toList());
    }

    @Override
    public List<PhieuGiamGia> getVouchers(BigDecimal tongTien) {
        return phieuGiamGiaRepository.findAllByTrangThaiAndDonHangToiThieuLessThanEqual(
                TrangThai.DANG_HOAT_DONG, tongTien);
    }

    private HoaDon getHoaDonOrThrow(String id) {
        return hoaDonRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(MessageConstants.HOA_DON_NOT_EXIST));
    }

    private void updateHoaDonTotals(HoaDon hd) {
        List<HoaDonChiTiet> details = hoaDonChiTietRepository.findAllByHoaDon(hd);
        BigDecimal total = details.stream()
                .map(d -> d.getDonGia().multiply(BigDecimal.valueOf(d.getSoLuong())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        hd.setTongTien(total);

        BigDecimal discounted = total;
        if (hd.getPhieuGiamGia() != null) {
            PhieuGiamGia v = hd.getPhieuGiamGia();
            BigDecimal threshold = v.getDonHangToiThieu() != null ? v.getDonHangToiThieu() : BigDecimal.ZERO;

            if (total.compareTo(threshold) >= 0) {
                BigDecimal val = BigDecimal.ZERO;
                if ("PHAN_TRAM".equalsIgnoreCase(v.getLoaiPhieu()) || "PERCENT".equalsIgnoreCase(v.getLoaiPhieu())) {
                    Integer percent = v.getPhanTramGiamGia() != null ? v.getPhanTramGiamGia() : 0;
                    val = total.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100));
                    if (v.getGiamToiDa() != null && val.compareTo(v.getGiamToiDa()) > 0) val = v.getGiamToiDa();
                } else {
                    val = v.getSoTienGiam() != null ? v.getSoTienGiam() : BigDecimal.ZERO;
                }
                discounted = total.subtract(val);
                if (discounted.compareTo(BigDecimal.ZERO) < 0) discounted = BigDecimal.ZERO;
            }
        }
        hd.setTongTienSauGiam(discounted);
        hoaDonRepository.save(hd);
    }

    private void createGiaoDich(HoaDon hd, String maPTTT, BigDecimal soTien, String maGiaoDichNgoai) {
        PhuongThucThanhToan pt = phuongThucThanhToanRepository.findByMa(maPTTT);
        if (pt == null) {
            pt = new PhuongThucThanhToan(maPTTT, maPTTT.equals("TIEN_MAT") ? "Tiền mặt" : "Chuyển khoản");
            phuongThucThanhToanRepository.save(pt);
        }
        GiaoDichThanhToan gd = GiaoDichThanhToan.builder()
                .hoaDon(hd)
                .phuongThucThanhToan(pt)
                .soTien(soTien)
                .maGiaoDichNgoai(maGiaoDichNgoai)
                .loaiGiaoDich("THANH_TOAN")
                .build();
        // Không set id thủ công: để id=null → Spring Data JPA gọi persist() → @PrePersist set id qua @GeneratedValue
        // Nếu set id thủ công → isNew()=false → Spring Data JPA gọi merge() → StaleObjectStateException
        gd.setTrangThai(TrangThai.DANG_HOAT_DONG);
        gd.setNgayTao(System.currentTimeMillis());
        giaoDichThanhToanRepository.save(gd);
    }

    private AdminBanHangHoaDonResponse mapToHoaDonResponse(HoaDon hd) {
        List<AdminBanHangHoaDonChiTietResponse> detailDTOs = hoaDonChiTietRepository.findAllByHoaDon(hd).stream()
                .map(d -> {
                    ChiTietSanPham ct = d.getChiTietSanPham();
                    return AdminBanHangHoaDonChiTietResponse.builder()
                        .id(d.getId())
                        .idChiTietSanPham(ct.getId())
                        .maChiTietSanPham(ct.getMaChiTietSanPham())
                        .tenSanPham(ct.getSanPham() != null ? ct.getSanPham().getTen() : "")
                        .tenMauSac(ct.getMauSac() != null ? ct.getMauSac().getTen() : "")
                        .tenKichThuoc(ct.getKichThuoc() != null ? ct.getKichThuoc().getTen() : "")
                        .soLuong(d.getSoLuong())
                        .donGia(d.getDonGia())
                        .thanhTien(d.getDonGia().multiply(BigDecimal.valueOf(d.getSoLuong())))
                        .soLuongTon(ct.getSoLuong())
                        .hinhAnh(getHinhAnhVariant(ct))
                        .build();
                }).collect(Collectors.toList());

        return AdminBanHangHoaDonResponse.builder()
                .id(hd.getId())
                .maHoaDon(hd.getMaHoaDon())
                .idKhachHang(hd.getKhachHang() != null ? hd.getKhachHang().getId() : null)
                .tenKhachHang(hd.getKhachHang() != null ? hd.getKhachHang().getTen() : "Khách lẻ")
                .sdtKhachHang(hd.getKhachHang() != null ? hd.getKhachHang().getSdt() : "")
                .idPhieuGiamGia(hd.getPhieuGiamGia() != null ? hd.getPhieuGiamGia().getId() : null)
                .tongTien(hd.getTongTien())
                .tongTienSauGiam(hd.getTongTienSauGiam())
                .listsHoaDonChiTiet(detailDTOs)
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
}
