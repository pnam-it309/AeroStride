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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.math.BigDecimal;
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

    @Override
    @Transactional(readOnly = true)
    /** Lay danh sach hoa don POS dang cho xu ly de FE hien thi tab don hang. */
    public List<AdminBanHangHoaDonResponse> getHoaDonCho() {
        return hoaDonRepository.findAllByTrangThaiAndLoaiDon(OrderStatus.CHO_XAC_NHAN, "TAI_QUAY")
                .stream().map(this::mapToHoaDonResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    /** Tao hoa don tai quay moi, gioi han so don cho de tranh mo qua nhieu tab. */
    public AdminBanHangHoaDonResponse createHoaDon() {
        if (hoaDonRepository.countByTrangThaiAndLoaiDon(OrderStatus.CHO_XAC_NHAN, "TAI_QUAY") >= 5) {
            throw new BusinessException("Chỉ được tạo tối đa 5 hóa đơn chờ");
        }
        HoaDon hoaDon = new HoaDon();
        hoaDon.setMaHoaDon(CodeUtils.generateRandom(HoaDon.class));
        hoaDon.setTrangThai(OrderStatus.CHO_XAC_NHAN);
        hoaDon.setLoaiDon("TAI_QUAY");
        hoaDon.setNgayTao(System.currentTimeMillis());
        hoaDon.setTongTien(BigDecimal.ZERO);
        hoaDon.setTongTienSauGiam(BigDecimal.ZERO);

        NhanVien nv = SecurityUtils.getCurrentUserEmail()
                .flatMap(identifier -> nhanVienRepository.findByTenTaiKhoanOrEmailOrSdtOrMa(identifier, identifier, identifier, identifier))
                .orElse(null);
        if (nv != null) {
            hoaDon.setNhanVien(nv);
            // Link to active GiaoCa (tạm thời comment theo yêu cầu không cần giao ca)
            // com.example.be.entity.GiaoCa activeGiaoCa = giaoCaRepository.findGiaoCaHienTai(nv.getId())
            //         .orElseThrow(() -> new BusinessException("Bạn phải Mở Ca làm việc trước khi tạo hóa đơn!"));
            // hoaDon.setGiaoCa(activeGiaoCa);
        }

        hoaDonRepository.save(hoaDon);
        return mapToHoaDonResponse(hoaDon);
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
        hoaDonRepository.delete(hd);
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

        HoaDonChiTiet hdct = hoaDonChiTietRepository.findByHoaDonAndChiTietSanPham(hoaDon, ctsp);
        // Don gia luu vao hoa don chi tiet la gia sau dot giam gia tai thoi diem them vao gio.
        BigDecimal effectivePrice = getEffectiveVariantPrice(ctsp);
        
        boolean priceChanged = false;
        String priceChangeMessage = null;
        
        if (hdct != null) {
            // Kiểm tra nếu giá thay đổi thì tạo bản ghi mới thay vì cập nhật số lượng
            if (hdct.getDonGia().compareTo(effectivePrice) != 0) {
                // Giá thay đổi: tạo bản ghi mới với giá mới
                priceChanged = true;
                priceChangeMessage = String.format("Giá sản phẩm %s đã thay đổi từ %sđ thành %sđ", 
                    ctsp.getSanPham() != null ? ctsp.getSanPham().getTen() : ctsp.getMaChiTietSanPham(),
                    hdct.getDonGia(), effectivePrice);
                
                HoaDonChiTiet newHdct = HoaDonChiTiet.builder()
                        .hoaDon(hoaDon)
                        .chiTietSanPham(ctsp)
                        .soLuong(request.getSoLuong())
                        .donGia(effectivePrice)
                        .build();
                newHdct.setTrangThai(TrangThai.DANG_HOAT_DONG);
                newHdct.setNgayTao(System.currentTimeMillis());
                hoaDonChiTietRepository.save(newHdct);
            } else {
                // Giá không đổi: chỉ cập nhật số lượng
                hdct.setSoLuong(hdct.getSoLuong() + request.getSoLuong());
                hoaDonChiTietRepository.save(hdct);
            }
        } else {
            // Sản phẩm chưa có trong giỏ: tạo mới
            hdct = HoaDonChiTiet.builder()
                    .hoaDon(hoaDon)
                    .chiTietSanPham(ctsp)
                    .soLuong(request.getSoLuong())
                    .donGia(effectivePrice)
                    .build();
            hdct.setTrangThai(TrangThai.DANG_HOAT_DONG);
            hdct.setNgayTao(System.currentTimeMillis());
            hoaDonChiTietRepository.save(hdct);
        }

        updateHoaDonTotals(hoaDon);
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

        if (soLuong <= 0) {
            restoreStock(ctspId, oldQty);
            hoaDonChiTietRepository.delete(hdct);
        } else {
            int delta = soLuong - oldQty;
            if (delta > 0) {
                deductStock(ctspId, delta, MessageConstants.PRODUCT_INSUFFICIENT_QTY);
            } else if (delta < 0) {
                restoreStock(ctspId, Math.abs(delta));
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

        restoreStock(hdct.getChiTietSanPham().getId(), hdct.getSoLuong());

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
    /** Chot thanh toan POS: gan khach hang, loai don, dia chi, tong tien va lich su thanh toan. */
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
            BigDecimal giaHienTai = getEffectiveVariantPrice(detail.getChiTietSanPham());
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

        // Set nhanVien based on currently authenticated user
        SecurityUtils.getCurrentUserEmail().ifPresent(username -> {
            nhanVienRepository.findByTenTaiKhoanOrEmailOrSdtOrMa(username, username, username, username).ifPresent(hd::setNhanVien);
        });

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

        // Add history record for timeline
        LichSuTrangThaiHoaDon history = LichSuTrangThaiHoaDon.builder()
                .hoaDon(hd)
                .trangThaiCu(OrderStatus.CHO_XAC_NHAN.ordinal())
                .trangThaiMoi(OrderStatus.HOAN_THANH.ordinal())
                .ghiChu("Thanh toán tại quầy thành công")
                .nguoiThucHien(SecurityUtils.getCurrentUserEmail().orElse("ADMIN"))
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
    /** Tim bien the cho man ban hang, kem gia goc/gia sau giam/badge phan tram. */
    public List<BanHangSanPhamResponse> searchSanPham(String keyword, String thuongHieu, String chatLieu, String xuatXu, String mucDich) {
        // Gioi han ket qua de khong load toan bo bien the khi o tim kiem dang rong.
        List<ChiTietSanPham> variants = chiTietSanPhamRepository
                .searchByKeywordLite(keyword, thuongHieu, chatLieu, xuatXu, mucDich, PageRequest.of(0, 2000))
                .getContent();
        Map<String, List<ChiTietDotGiamGia>> discountMap = getDiscountRelationMap(variants);

        return variants
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
                        .giaGoc(getActiveDiscountPercent(ct, discountMap).compareTo(BigDecimal.ZERO) > 0 ? ct.getGiaBan() : null)
                        .giaBan(getEffectiveVariantPrice(ct, discountMap))
                        .phanTramGiam(getActiveDiscountPercent(ct, discountMap))
                        .hinhAnh(getHinhAnhVariant(ct))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<AdminBanHangKhachHangResponse> searchKhachHang(String keyword) {
        return khachHangRepository.searchByKeyword(keyword, org.springframework.data.domain.PageRequest.of(0, 5)).stream().map(kh -> AdminBanHangKhachHangResponse.builder()
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
        BigDecimal total = hd.getTongTien();
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) return null;

        // Lấy tất cả voucher đang hoạt động
        List<PhieuGiamGia> allVouchers = phieuGiamGiaRepository.findAllByTrangThai(TrangThai.DANG_HOAT_DONG);

        PhieuGiamGia bestVoucher = null;
        BigDecimal maxDiscount = BigDecimal.ZERO;
        Integer maxPercent = 0;

        for (PhieuGiamGia voucher : allVouchers) {
            // Kiểm tra loại phiếu: công khai hoặc cá nhân
            String hinhThuc = voucher.getHinhThuc();
            
            // Nếu là phiếu cá nhân, chỉ áp dụng khi khách hàng có phiếu này
            if ("CA_NHAN".equals(hinhThuc) || "Cá nhân".equalsIgnoreCase(hinhThuc)) {
                if (hd.getKhachHang() == null) {
                    continue; // Không có khách hàng, bỏ qua phiếu cá nhân
                }
                
                // Kiểm tra khách hàng có phiếu này không
                List<com.example.be.entity.PhieuGiamGiaCaNhan> personalVouchers = 
                    phieuGiamGiaCaNhanRepository.findByKhachHangId(hd.getKhachHang().getId());
                
                boolean customerHasVoucher = personalVouchers.stream()
                    .anyMatch(pv -> pv.getPhieuGiamGia() != null 
                        && pv.getPhieuGiamGia().getId().equals(voucher.getId())
                        && Boolean.FALSE.equals(pv.getDaSuDung()));
                
                if (!customerHasVoucher) {
                    continue; // Khách hàng không có phiếu này hoặc đã dùng
                }
            }

            // Ưu tiên voucher có % giảm giá cao hơn
            Integer currentPercent = 0;
            BigDecimal discount = BigDecimal.ZERO;
            
            if ("PHAN_TRAM".equalsIgnoreCase(voucher.getLoaiPhieu()) || "PERCENT".equalsIgnoreCase(voucher.getLoaiPhieu())) {
                currentPercent = voucher.getPhanTramGiamGia() != null ? voucher.getPhanTramGiamGia() : 0;
                discount = total.multiply(BigDecimal.valueOf(currentPercent)).divide(BigDecimal.valueOf(100));
                BigDecimal max = voucher.getGiamToiDa() != null ? voucher.getGiamToiDa() : BigDecimal.valueOf(Long.MAX_VALUE);
                if (discount.compareTo(max) > 0) discount = max;
            } else {
                // Voucher cố định: tính % tương đương để so sánh
                if (total.compareTo(BigDecimal.ZERO) > 0) {
                    discount = voucher.getSoTienGiam() != null ? voucher.getSoTienGiam() : BigDecimal.ZERO;
                    currentPercent = discount.multiply(BigDecimal.valueOf(100))
                            .divide(total, 0, java.math.RoundingMode.HALF_UP).intValue();
                }
            }

            // Ưu tiên theo % giảm giá trước Sau đó mới xét số tiền giảm tuyệt đối
            if (currentPercent > maxPercent || 
                (currentPercent == maxPercent && discount.compareTo(maxDiscount) > 0)) {
                maxPercent = currentPercent;
                maxDiscount = discount;
                bestVoucher = voucher;
            }
        }
        return bestVoucher;
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
        int updated = chiTietSanPhamRepository.deductStock(variantId, qty);
        if (updated == 0) {
            throw new BusinessException(errorMessage);
        }
        return chiTietSanPhamRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.SAN_PHAM_NOT_FOUND));
    }

    private ChiTietSanPham restoreStock(String variantId, int qty) {
        int updated = chiTietSanPhamRepository.restoreStock(variantId, qty);
        if (updated == 0) {
            throw new ResourceNotFoundException(MessageConstants.SAN_PHAM_NOT_FOUND);
        }
        return chiTietSanPhamRepository.findById(variantId).orElseThrow();
    }

    /** Cap nhat tong tien hoa don moi khi gio hang/voucher thay doi. */
    private void updateHoaDonTotals(HoaDon hd) {
        List<HoaDonChiTiet> details = hoaDonChiTietRepository.findAllByHoaDon(hd);
        
        // Kiểm tra và cập nhật giá nếu đợt giảm giá thay đổi
        boolean priceUpdated = false;
        for (HoaDonChiTiet detail : details) {
            ChiTietSanPham ctsp = detail.getChiTietSanPham();
            BigDecimal currentEffectivePrice = getEffectiveVariantPrice(ctsp);
            
            if (detail.getDonGia().compareTo(currentEffectivePrice) != 0) {
                detail.setDonGia(currentEffectivePrice);
                hoaDonChiTietRepository.save(detail);
                priceUpdated = true;
            }
        }
        
        // Nếu có giá thay đổi, reload lại danh sách để tính tổng đúng
        if (priceUpdated) {
            details = hoaDonChiTietRepository.findAllByHoaDon(hd);
        }
        
        // Kiểm tra phiếu giảm giá có còn hiệu lực không
        if (hd.getPhieuGiamGia() != null) {
            PhieuGiamGia voucher = phieuGiamGiaRepository.findById(hd.getPhieuGiamGia().getId()).orElse(null);
            if (voucher == null || !TrangThai.DANG_HOAT_DONG.equals(voucher.getTrangThai())) {
                // Phiếu đã bị hủy kích hoạt hoặc không tồn tại, gỡ bỏ
                hd.setPhieuGiamGia(null);
                hoaDonRepository.save(hd);
            } else {
                // Kiểm tra thời gian hiệu lực
                long currentTime = System.currentTimeMillis();
                Long start = voucher.getNgayBatDau();
                Long end = voucher.getNgayKetThuc();
                if ((start != null && currentTime < start) || (end != null && currentTime > end)) {
                    // Phiếu hết hạn, gỡ bỏ
                    hd.setPhieuGiamGia(null);
                    hoaDonRepository.save(hd);
                }
            }
        }
        
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
        List<HoaDonChiTiet> chiTietList = hoaDonChiTietRepository.findAllByHoaDon(hd);
        List<ChiTietSanPham> variants = chiTietList.stream()
                .map(HoaDonChiTiet::getChiTietSanPham)
                .filter(Objects::nonNull)
                .toList();
        Map<String, List<ChiTietDotGiamGia>> discountMap = getDiscountRelationMap(variants);

        List<AdminBanHangHoaDonChiTietResponse> detailDTOs = chiTietList.stream()
                .map(d -> {
                    ChiTietSanPham ct = d.getChiTietSanPham();
                    Integer phanTramGiam = null;
                    if (ct.getGiaBan() != null && d.getDonGia() != null && ct.getGiaBan().compareTo(d.getDonGia()) > 0) {
                        BigDecimal giaGoc = ct.getGiaBan();
                        BigDecimal giaBan = d.getDonGia();
                        BigDecimal discount = giaGoc.subtract(giaBan);
                        phanTramGiam = discount.multiply(BigDecimal.valueOf(100))
                                .divide(giaGoc, java.math.RoundingMode.HALF_UP).intValue();
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
                        .giaGoc(ct.getGiaBan())
                        .phanTramGiam(phanTramGiam)
                        .tenDotGiamGia(tenDotGiamGia)
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
        // Sử dụng repository method có sẵn
        List<ChiTietDotGiamGia> allDiscounts = chiTietDotGiamGiaRepository.findAll();
        
        long currentTime = System.currentTimeMillis();
        
        // Filter active discounts based on time and status
        List<ChiTietDotGiamGia> activeDiscounts = allDiscounts.stream()
                .filter(d -> {
                    DotGiamGia dot = d.getDotGiamGia();
                    if (dot == null) return false;
                    // Check time range
                    Long start = dot.getNgayBatDau();
                    Long end = dot.getNgayKetThuc();
                    if (start != null && currentTime < start) return false;
                    if (end != null && currentTime > end) return false;
                    return true;
                })
                .collect(Collectors.toList());
        
        if (activeDiscounts.isEmpty() || currentTotal.compareTo(BigDecimal.ZERO) == 0) {
            return suggestions;
        }
        
        // Tìm sản phẩm phù hợp để gợi ý (đơn giản hóa: lấy sản phẩm đầu tiên có giảm giá)
        for (ChiTietDotGiamGia discount : activeDiscounts) {
            ChiTietSanPham variant = discount.getChiTietSanPham();
            if (variant != null && variant.getSanPham() != null 
                    && Boolean.TRUE.equals(variant.getSanPham().getTrangThai())) {
                
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
}
