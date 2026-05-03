package com.example.be.core.admin.khachhang.service.impl;

import com.example.be.core.admin.khachhang.model.request.AdminKhachHangRequest;
import com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse;
import com.example.be.core.admin.khachhang.repository.AdminKhachHangRepository;
import com.example.be.core.admin.khachhang.service.AdminDiaChiService;
import com.example.be.core.admin.khachhang.service.AdminKhachHangService;
import com.example.be.core.notification.EmailService;
import com.example.be.entity.DiaChi;
import com.example.be.entity.KhachHang;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.DuplicateResourceException;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.repository.DiaChiRepository;
import com.example.be.utils.AccountUtils;
import com.example.be.utils.CodeUtils;
import com.example.be.utils.ExcelUtils;
import com.example.be.utils.SearchUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminKhachHangServiceImpl implements AdminKhachHangService {

    private final AdminKhachHangRepository adminKhachHangRepository;
    private final DiaChiRepository diaChiRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminDiaChiService adminDiaChiService;
    private final EmailService emailService;

    // ── HIỂN THỊ (không phân trang) ───────────────────────────────────────
    @Override
    public List<AdminKhachHangResponse> hienThi() {
        return adminKhachHangRepository.hienThi();
    }

    // ── TÌM KIẾM / LỌC / PHÂN TRANG — gộp 1 method ──────────────────────
    @Override
    public Page<AdminKhachHangResponse> search(AdminKhachHangRequest request) {
        return SearchUtils.execute(request, pageable -> 
            adminKhachHangRepository.filterAll(
                request.getKeyword(),
                request.getTrangThai(),
                request.getGioiTinh(),
                pageable
            )
        );
    }

    // ── DETAIL ────────────────────────────────────────────────────────────
    @Override
    public AdminKhachHangResponse detail(String id) {
        AdminKhachHangResponse response = adminKhachHangRepository.detail(id);
        if (response == null) {
            throw new ResourceNotFoundException("Không tìm thấy khách hàng với id: " + id);
        }
        response.setAddresses(adminDiaChiService.getByKhachHangId(id));
        return response;
    }

    // ── CREATE ────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AdminKhachHangResponse add(AdminKhachHangRequest request) {
        if (request.getMa() != null && !request.getMa().trim().isEmpty()) {
            if (adminKhachHangRepository.existsByMa(request.getMa())) {
                throw new DuplicateResourceException("Mã khách hàng này đã tồn tại trong hệ thống.");
            }
        }
        if (adminKhachHangRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email này đã được sử dụng bởi một khách hàng khác.");
        }

        KhachHang kh = toEntity(request);

        if (kh.getMa() == null || kh.getMa().trim().isEmpty()) {
            kh.setMa(CodeUtils.generateSequential("KH", adminKhachHangRepository.findAllMa()));
        }

        // Luôn tự sinh tenTaiKhoan và matKhau tạm
        String tenTaiKhoan;
        do {
            tenTaiKhoan = AccountUtils.taoTenTaiKhoanUuid();
        } while (adminKhachHangRepository.existsByTenTaiKhoan(tenTaiKhoan));

        String matKhauTam = AccountUtils.taoMatKhauNgauNhien(10);

        kh.setTenTaiKhoan(tenTaiKhoan);
        kh.setMatKhau(passwordEncoder.encode(matKhauTam));
        kh.setTrangThai(TrangThai.DANG_HOAT_DONG);

        adminKhachHangRepository.save(kh);

        // Xử lý địa chỉ
        DiaChi dc = resolveAddress(request, kh);
        if (dc != null) {
            kh.setDiaChi(dc);
            adminKhachHangRepository.save(kh);
        }

        emailService.guiEmailTaiKhoanKhachHang(
                request.getEmail(), request.getTen(), tenTaiKhoan, matKhauTam
        );

        return this.detail(kh.getId());
    }

    // ── UPDATE ────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AdminKhachHangResponse update(String id, AdminKhachHangRequest req) {
        KhachHang kh = adminKhachHangRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với id: " + id));

        if (adminKhachHangRepository.existsByMaAndIdNot(req.getMa(), id)) {
            throw new DuplicateResourceException("Mã khách hàng đã tồn tại");
        }
        if (adminKhachHangRepository.existsByEmailAndIdNot(req.getEmail(), id)) {
            throw new DuplicateResourceException("Email đã được sử dụng");
        }
        if (adminKhachHangRepository.existsByTenTaiKhoanAndIdNot(req.getTenTaiKhoan(), id)) {
            throw new DuplicateResourceException("Tên tài khoản đã được sử dụng");
        }

        applyEntityFields(kh, req);

        if (req.getMatKhau() != null && !req.getMatKhau().isBlank()) {
            kh.setMatKhau(passwordEncoder.encode(req.getMatKhau()));
        }

        adminKhachHangRepository.save(kh);

        if (req.getIdDiaChi() != null) {
            DiaChi dc = diaChiRepository.findById(req.getIdDiaChi())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ"));
            dc.setKhachHang(kh);
            dc.setLaMacDinh(true);
            diaChiRepository.save(dc);
            
            // Sync default address field
            kh.setDiaChi(dc);
            adminKhachHangRepository.save(kh);
        }

        return adminKhachHangRepository.detail(kh.getId());
    }

    // ── ĐỔI TRẠNG THÁI ───────────────────────────────────────────────────
    @Override
    @Transactional
    public void doiTrangThai(String id, TrangThai trangThai) {
        KhachHang kh = adminKhachHangRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với id: " + id));
        kh.setTrangThai(trangThai);
        adminKhachHangRepository.saveAndFlush(kh);
    }

    // ── DELETE (soft) ─────────────────────────────────────────────────────
    @Override
    @Transactional
    public void delete(String id) {
        KhachHang kh = adminKhachHangRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với id: " + id));
        kh.setTrangThai(TrangThai.KHONG_HOAT_DONG);
        adminKhachHangRepository.save(kh);
    }

    // ── EXPORT EXCEL ──────────────────────────────────────────────────────
    @Override
    public byte[] exportExcel() {
        List<AdminKhachHangResponse> data = adminKhachHangRepository.hienThi();
        String[] headers = {"STT", "Mã", "Tên", "Email", "SĐT", "Ngày sinh", "Giới tính", "Trạng thái"};
        try {
            return ExcelUtils.exportToExcel("Danh sách khách hàng", headers, data, item -> new Object[]{
                    data.indexOf(item) + 1,
                    item.getMa(),
                    item.getTen(),
                    item.getEmail(),
                    item.getSdt(),
                    item.getNgaySinh(),
                    item.getGioiTinh(),
                    item.getTrangThai() == TrangThai.DANG_HOAT_DONG ? "Đang hoạt động" : "Ngừng hoạt động"
            });
        } catch (IOException e) {
            throw new com.example.be.infrastructure.exceptions.SystemException(
                    "Lỗi xuất file Excel: " + e.getMessage());
        }
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────

    /** Maps request fields to a new KhachHang entity. */
    private KhachHang toEntity(AdminKhachHangRequest req) {
        KhachHang kh = new KhachHang();
        applyEntityFields(kh, req);
        return kh;
    }

    /** Applies mutable fields from request onto an existing entity (create + update). */
    private void applyEntityFields(KhachHang kh, AdminKhachHangRequest req) {
        if (req.getMa() != null && !req.getMa().isBlank()) {
            kh.setMa(req.getMa());
        }
        kh.setTen(req.getTen());
        kh.setEmail(req.getEmail());
        if (req.getTenTaiKhoan() != null && !req.getTenTaiKhoan().isBlank()) {
            kh.setTenTaiKhoan(req.getTenTaiKhoan());
        }
        kh.setGioiTinh(req.getGioiTinh());
        kh.setSdt(req.getSdt());
        kh.setNgaySinh(req.getNgaySinh());
        kh.setHinhAnh(req.getHinhAnh());
        kh.setGhiChu(req.getGhiChu());
    }

    /**
     * Resolves and persists the address for a newly created KhachHang.
     * Returns the saved DiaChi or null if no address data provided.
     */
    private DiaChi resolveAddress(AdminKhachHangRequest request, KhachHang kh) {
        if (request.getTinh() != null && !request.getTinh().trim().isEmpty()) {
            DiaChi dc = new DiaChi();
            dc.setTinh(request.getTinh());
            dc.setThanhPho(request.getThanhPho());
            dc.setPhuongXa(request.getPhuongXa());
            dc.setDiaChiChiTiet(request.getDiaChiChiTiet());
            dc.setTenNguoiNhan(kh.getTen());
            dc.setSdtNguoiNhan(kh.getSdt());
            dc.setKhachHang(kh);
            dc.setLaMacDinh(true);
            return diaChiRepository.save(dc);
        }
        if (request.getIdDiaChi() != null) {
            DiaChi dc = diaChiRepository.findById(request.getIdDiaChi())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ"));
            dc.setKhachHang(kh);
            dc.setLaMacDinh(true);
            return diaChiRepository.save(dc);
        }
        return null;
    }
}
