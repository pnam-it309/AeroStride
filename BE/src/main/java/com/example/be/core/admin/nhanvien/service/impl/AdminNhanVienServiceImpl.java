package com.example.be.core.admin.nhanvien.service.impl;

import com.example.be.core.admin.nhanvien.model.request.AdminNhanVienRequest;
import com.example.be.core.admin.nhanvien.model.response.AdminNhanVienResponse;
import com.example.be.core.admin.nhanvien.repository.AdminNhanVienRepository;
import com.example.be.core.admin.nhanvien.service.AdminNhanVienService;
import com.example.be.core.notification.EmailService;
import com.example.be.entity.NhanVien;
import com.example.be.infrastructure.constants.MessageConstants;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.DuplicateResourceException;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.repository.PhanQuyenRepository;
import com.example.be.core.storage.StorageService;
import com.example.be.utils.AccountUtils;
import com.example.be.utils.CodeUtils;
import com.example.be.utils.ExcelUtils;
import com.example.be.utils.SearchUtils;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.be.core.admin.nhanvien.repository.AdminNhanVienSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminNhanVienServiceImpl implements AdminNhanVienService {

    private final AdminNhanVienRepository adminNhanVienRepository;
    private final PhanQuyenRepository phanQuyenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final StorageService storageService;

    // ── HIỂN THỊ (không phân trang) ───────────────────────────────────────
    @Override
    public List<AdminNhanVienResponse> hienThi() {
        return adminNhanVienRepository.findAll(Sort.by(Sort.Direction.DESC, "ngayTao"))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── TÌM KIẾM / LỌC / PHÂN TRANG — gộp 1 method ──────────────────────
    @Override
    public Page<AdminNhanVienResponse> search(AdminNhanVienRequest request) {
        Specification<NhanVien> spec = Specification.where(AdminNhanVienSpecification.keywordLike(request.getKeyword()))
                .and(AdminNhanVienSpecification.hasTrangThai(request.getTrangThai()))
                .and(AdminNhanVienSpecification.hasGioiTinh(request.getGioiTinh()));

        return SearchUtils.execute(request, pageable ->
            adminNhanVienRepository.findAll(spec, pageable).map(this::toResponse)
        );
    }

    // ── DETAIL ────────────────────────────────────────────────────────────
    @Override
    public AdminNhanVienResponse detail(String id) {
        NhanVien nv = adminNhanVienRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.NHAN_VIEN_NOT_FOUND + id));
        return toResponse(nv);
    }

    // ── CREATE ────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AdminNhanVienResponse add(AdminNhanVienRequest request) {
        // Kiểm tra mã nếu được cung cấp
        if (request.getMa() != null && !request.getMa().trim().isEmpty()) {
            if (adminNhanVienRepository.existsByMa(request.getMa())) {
                throw new DuplicateResourceException(MessageConstants.NHAN_VIEN_MA_EXISTS);
            }
        }
        if (adminNhanVienRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(MessageConstants.NHAN_VIEN_EMAIL_EXISTS);
        }

        NhanVien nv = toEntity(request);

        if (nv.getMa() == null || nv.getMa().trim().isEmpty()) {
            List<String> existingMas = adminNhanVienRepository.findAllProjectedBy().stream()
                    .map(AdminNhanVienRepository.MaOnly::getMa)
                    .toList();
            nv.setMa(CodeUtils.generateSequential("NV", existingMas));
        }

        // Tự sinh tenTaiKhoan unique
        String tenTaiKhoan;
        do {
            tenTaiKhoan = AccountUtils.taoTenTaiKhoanTuHoTen(request.getTen())
                    + SECURE_RANDOM_SUFFIX();
        } while (adminNhanVienRepository.existsByTenTaiKhoan(tenTaiKhoan));

        String matKhauTam = AccountUtils.taoMatKhauNgauNhien(10);

        nv.setTenTaiKhoan(tenTaiKhoan);
        nv.setMatKhau(passwordEncoder.encode(matKhauTam));

        if (request.getIdPhanQuyen() != null) {
            nv.setPhanQuyen(phanQuyenRepository.findById(request.getIdPhanQuyen())
                    .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PHAN_QUYEN_NOT_FOUND)));
        }

        nv.setTrangThai(TrangThai.DANG_HOAT_DONG);
        adminNhanVienRepository.save(nv);

        String tenVaiTro = (nv.getPhanQuyen() != null) ? nv.getPhanQuyen().getTen() : "Nhân viên";
        emailService.guiEmailTaiKhoanNhanVien(
                request.getEmail(), request.getTen(), tenTaiKhoan, matKhauTam, tenVaiTro
        );

        return toResponse(nv);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AdminNhanVienResponse update(String id, AdminNhanVienRequest request) {
        NhanVien nv = adminNhanVienRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.NHAN_VIEN_NOT_FOUND + id));

        if (adminNhanVienRepository.existsByMaAndIdNot(request.getMa(), id)) {
            throw new DuplicateResourceException(MessageConstants.DUPLICATE_MA);
        }
        if (adminNhanVienRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new DuplicateResourceException(MessageConstants.DUPLICATE_EMAIL);
        }
        if (adminNhanVienRepository.existsByTenTaiKhoanAndIdNot(request.getTenTaiKhoan(), id)) {
            throw new DuplicateResourceException(MessageConstants.DUPLICATE_USERNAME);
        }

        applyEntityFields(nv, request);

        if (request.getIdPhanQuyen() != null) {
            nv.setPhanQuyen(phanQuyenRepository.findById(request.getIdPhanQuyen())
                    .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PHAN_QUYEN_NOT_FOUND)));
        } else {
            nv.setPhanQuyen(null);
        }

        adminNhanVienRepository.save(nv);
        return toResponse(nv);
    }

    // ── ĐỔI TRẠNG THÁI ───────────────────────────────────────────────────
    @Override
    @Transactional
    public void doiTrangThai(String id, TrangThai trangThai) {
        NhanVien nv = adminNhanVienRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.NHAN_VIEN_NOT_FOUND + id));
        nv.setTrangThai(trangThai);
        adminNhanVienRepository.saveAndFlush(nv);
    }

    // ── DELETE (soft) ─────────────────────────────────────────────────────
    @Override
    @Transactional
    public void delete(String id) {
        NhanVien nv = adminNhanVienRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.NHAN_VIEN_NOT_FOUND + id));
        nv.setTrangThai(TrangThai.NGUNG_HOAT_DONG);
        adminNhanVienRepository.save(nv);
    }

    // ── EXPORT EXCEL ──────────────────────────────────────────────────────
    @Override
    public byte[] exportExcel() {
        List<AdminNhanVienResponse> data = hienThi();
        String[] headers = {"STT", "Mã", "Tên", "Email", "SĐT", "Ngày sinh", "Giới tính", "Địa chỉ", "Chức vụ", "Trạng thái"};
        try {
            return ExcelUtils.exportToExcel("Danh sách nhân viên", headers, data, item -> new Object[]{
                    data.indexOf(item) + 1,
                    item.getMa(),
                    item.getTen(),
                    item.getEmail(),
                    item.getSdt(),
                    item.getNgaySinh(),
                    item.getGioiTinh(),
                    item.getTenPhanQuyen(),
                    item.getTrangThai() == TrangThai.DANG_HOAT_DONG ? "Đang làm việc" : "Ngừng hoạt động"
            });
        } catch (IOException e) {
            throw new com.example.be.infrastructure.exceptions.SystemException(
                    MessageConstants.EXCEL_EXPORT_ERROR + e.getMessage());
        }
    }

    // ── PHÂN QUYỀN ───────────────────────────────────────────────────────
    @Override
    public List<com.example.be.entity.PhanQuyen> getAllPhanQuyen() {
        return phanQuyenRepository.findAll();
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        return storageService.uploadFile(file, "avatars").getFileUrl();
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────

    /** Maps request fields to a new NhanVien entity. */
    private NhanVien toEntity(AdminNhanVienRequest request) {
        NhanVien nv = new NhanVien();
        applyEntityFields(nv, request);
        return nv;
    }

    /** Applies mutable fields from request onto an existing entity (create + update). */
    private void applyEntityFields(NhanVien nv, AdminNhanVienRequest req) {
        if (req.getMa() != null && !req.getMa().isBlank()) {
            nv.setMa(req.getMa());
        }
        nv.setTen(req.getTen());
        nv.setEmail(req.getEmail());
        if (req.getTenTaiKhoan() != null && !req.getTenTaiKhoan().isBlank()) {
            nv.setTenTaiKhoan(req.getTenTaiKhoan());
        }
        nv.setGioiTinh(req.getGioiTinh());
        nv.setSdt(req.getSdt());
        nv.setNgaySinh(req.getNgaySinh());
        nv.setHinhAnh(req.getHinhAnh());
        nv.setTinh(req.getTinh());
        nv.setThanhPho(req.getThanhPho());
        nv.setPhuongXa(req.getPhuongXa());
        nv.setDiaChiChiTiet(req.getDiaChiChiTiet());
    }

    /** Small random numeric suffix to avoid username collisions. */
    private static String SECURE_RANDOM_SUFFIX() {
        return String.valueOf((int) (Math.random() * 1000));
    }

    private AdminNhanVienResponse toResponse(NhanVien nv) {
        if (nv == null) return null;
        return AdminNhanVienResponse.builder()
                .id(nv.getId())
                .ma(nv.getMa())
                .ten(nv.getTen())
                .email(nv.getEmail())
                .tenTaiKhoan(nv.getTenTaiKhoan())
                .gioiTinh(nv.getGioiTinh())
                .sdt(nv.getSdt())
                .ngaySinh(nv.getNgaySinh())
                .hinhAnh(nv.getHinhAnh())
                .tinh(nv.getTinh())
                .thanhPho(nv.getThanhPho())
                .phuongXa(nv.getPhuongXa())
                .diaChiChiTiet(nv.getDiaChiChiTiet())
                .trangThai(nv.getTrangThai())
                .ngayTao(nv.getNgayTao())
                .ngayCapNhat(nv.getNgayCapNhat())
                .maPhanQuyen(nv.getPhanQuyen() != null ? nv.getPhanQuyen().getMa() : null)
                .tenPhanQuyen(nv.getPhanQuyen() != null ? nv.getPhanQuyen().getTen() : null)
                .quyenHan(nv.getPhanQuyen() != null ? nv.getPhanQuyen().getQuyenHan() : null)
                .build();
    }
}
