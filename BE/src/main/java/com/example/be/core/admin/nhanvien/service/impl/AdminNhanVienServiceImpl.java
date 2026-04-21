package com.example.be.core.admin.nhanvien.service.impl;

import com.example.be.core.admin.nhanvien.model.request.AdminNhanVienRequest;
import com.example.be.core.admin.nhanvien.model.response.AdminNhanVienResponse;
import com.example.be.core.admin.nhanvien.repository.AdminNhanVienRepository;
import com.example.be.core.admin.nhanvien.service.AdminNhanVienService;
import com.example.be.core.notification.EmailService;
import com.example.be.entity.NhanVien;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.DuplicateResourceException;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.repository.PhanQuyenRepository;
import com.example.be.core.storage.StorageService;
import com.example.be.utils.AccountUtils;
import com.example.be.utils.CodeUtils;
import com.example.be.utils.ExcelUtils;
import com.example.be.utils.SearchUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminNhanVienServiceImpl implements AdminNhanVienService {

    private final AdminNhanVienRepository adminNhanVienRepository;
    private final PhanQuyenRepository phanQuyenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final StorageService storageService;

    // ── HIỂN THỊ (không phân trang) ───────────────────────────────────────
    @Override
    public List<AdminNhanVienResponse> hienThi() {
        return adminNhanVienRepository.hienThi();
    }

    // ── TÌM KIẾM / LỌC / PHÂN TRANG — gộp 1 method ──────────────────────
    @Override
    public Page<AdminNhanVienResponse> search(AdminNhanVienRequest request) {
        return SearchUtils.execute(request, pageable -> 
            adminNhanVienRepository.filterAll(
                request.getKeyword(),
                request.getTrangThai(),
                request.getGioiTinh(),
                pageable
            )
        );
    }

    // ── DETAIL ────────────────────────────────────────────────────────────
    @Override
    public AdminNhanVienResponse detail(String id) {
        AdminNhanVienResponse res = adminNhanVienRepository.detail(id);
        if (res == null) {
            throw new ResourceNotFoundException("Không tìm thấy nhân viên với id: " + id);
        }
        return res;
    }

    // ── CREATE ────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AdminNhanVienResponse add(AdminNhanVienRequest request) {
        // Kiểm tra mã nếu được cung cấp
        if (request.getMa() != null && !request.getMa().trim().isEmpty()) {
            if (adminNhanVienRepository.existsByMa(request.getMa())) {
                throw new DuplicateResourceException("Mã nhân viên này đã tồn tại.");
            }
        }
        if (adminNhanVienRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email này đã được sử dụng bởi một nhân viên khác.");
        }

        NhanVien nv = toEntity(request);

        // Tự sinh mã nếu trống
        if (nv.getMa() == null || nv.getMa().trim().isEmpty()) {
            nv.setMa(CodeUtils.generateSequential("NV", adminNhanVienRepository.findAllMa()));
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
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân quyền")));
        }

        nv.setTrangThai(TrangThai.DANG_HOAT_DONG);
        adminNhanVienRepository.save(nv);

        String tenVaiTro = (nv.getPhanQuyen() != null) ? nv.getPhanQuyen().getTen() : "Nhân viên";
        emailService.guiEmailTaiKhoanNhanVien(
                request.getEmail(), request.getTen(), tenTaiKhoan, matKhauTam, tenVaiTro
        );

        return adminNhanVienRepository.detail(nv.getId());
    }

    // ── UPDATE ────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AdminNhanVienResponse update(String id, AdminNhanVienRequest request) {
        NhanVien nv = adminNhanVienRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên với id: " + id));

        if (adminNhanVienRepository.existsByMaAndIdNot(request.getMa(), id)) {
            throw new DuplicateResourceException("Mã nhân viên đã tồn tại");
        }
        if (adminNhanVienRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new DuplicateResourceException("Email đã được sử dụng");
        }
        if (adminNhanVienRepository.existsByTenTaiKhoanAndIdNot(request.getTenTaiKhoan(), id)) {
            throw new DuplicateResourceException("Tên tài khoản đã được sử dụng");
        }

        applyEntityFields(nv, request);

        if (request.getIdPhanQuyen() != null) {
            nv.setPhanQuyen(phanQuyenRepository.findById(request.getIdPhanQuyen())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân quyền")));
        } else {
            nv.setPhanQuyen(null);
        }

        adminNhanVienRepository.save(nv);
        return adminNhanVienRepository.detail(id);
    }

    // ── ĐỔI TRẠNG THÁI ───────────────────────────────────────────────────
    @Override
    @Transactional
    public void doiTrangThai(String id, TrangThai trangThai) {
        NhanVien nv = adminNhanVienRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên với id: " + id));
        nv.setTrangThai(trangThai);
        adminNhanVienRepository.saveAndFlush(nv);
    }

    // ── DELETE (soft) ─────────────────────────────────────────────────────
    @Override
    @Transactional
    public void delete(String id) {
        NhanVien nv = adminNhanVienRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên với id: " + id));
        nv.setTrangThai(TrangThai.KHONG_HOAT_DONG);
        adminNhanVienRepository.save(nv);
    }

    // ── EXPORT EXCEL ──────────────────────────────────────────────────────
    @Override
    public byte[] exportExcel() {
        List<AdminNhanVienResponse> data = adminNhanVienRepository.hienThi();
        String[] headers = {"STT", "Mã", "Tên", "Email", "SĐT", "Ngày sinh", "Giới tính", "Chức vụ", "Trạng thái"};
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
                    item.getTrangThai() == TrangThai.DANG_HOAT_DONG ? "Đang làm việc" : "Đã nghỉ việc"
            });
        } catch (IOException e) {
            throw new com.example.be.infrastructure.exceptions.SystemException(
                    "Lỗi xuất file Excel: " + e.getMessage());
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
        nv.setMa(req.getMa());
        nv.setTen(req.getTen());
        nv.setEmail(req.getEmail());
        nv.setTenTaiKhoan(req.getTenTaiKhoan());
        nv.setGioiTinh(req.getGioiTinh());
        nv.setSdt(req.getSdt());
        nv.setNgaySinh(req.getNgaySinh());
        nv.setHinhAnh(req.getHinhAnh());
    }

    /** Small random numeric suffix to avoid username collisions. */
    private static String SECURE_RANDOM_SUFFIX() {
        return String.valueOf((int) (Math.random() * 1000));
    }
}
