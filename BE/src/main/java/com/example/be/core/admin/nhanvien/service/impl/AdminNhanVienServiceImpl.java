package com.example.be.core.admin.nhanvien.service.impl;

import com.example.be.core.admin.nhanvien.model.request.AdminNhanVienRequest;
import com.example.be.core.admin.nhanvien.model.response.AdminNhanVienResponse;
import com.example.be.core.admin.nhanvien.repository.AdminNhanVienRepository;
import com.example.be.core.admin.nhanvien.service.AdminNhanVienService;
import com.example.be.core.notification.EmailService;
import com.example.be.entity.NhanVien;
import com.example.be.infrastructure.constants.MessageConstants;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.constants.VaiTro;
import com.example.be.infrastructure.exceptions.DuplicateResourceException;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.infrastructure.exceptions.ValidationException;
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

    // ── OPTIONS CHO DROPDOWN (tinh gọn, không base64) ───────────────────────
    @Override
    public List<AdminNhanVienResponse> getOptions() {
        return adminNhanVienRepository.findOptions();
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
        // Trim khoảng trắng các trường văn bản
        if (request.getTen() != null) request.setTen(request.getTen().trim());
        if (request.getEmail() != null) request.setEmail(request.getEmail().trim());
        if (request.getSdt() != null) request.setSdt(request.getSdt().trim());
        if (request.getDiaChiChiTiet() != null) request.setDiaChiChiTiet(request.getDiaChiChiTiet().trim());

        // Kiểm tra mã nếu được cung cấp
        if (request.getMa() != null && !request.getMa().trim().isEmpty()) {
            if (adminNhanVienRepository.existsByMa(request.getMa().trim())) {
                throw new DuplicateResourceException("Mã nhân viên này đã tồn tại.");
            }
        }
        if (adminNhanVienRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email này đã được sử dụng bởi một nhân viên khác.");
        }
        if (adminNhanVienRepository.existsBySdt(request.getSdt())) {
            throw new DuplicateResourceException("Số điện thoại này đã được sử dụng bởi một nhân viên khác.");
        }

        // Validate độ tuổi (nếu có ngày sinh)
        if (request.getNgaySinh() != null) {
            java.time.LocalDate today = java.time.LocalDate.now();
            if (request.getNgaySinh().isAfter(today)) {
                throw new com.example.be.infrastructure.exceptions.ValidationException("Ngày sinh không thể ở trong tương lai.");
            }
            if (request.getNgaySinh().isAfter(today.minusYears(18))) {
                throw new com.example.be.infrastructure.exceptions.ValidationException("Nhân viên phải từ 18 tuổi trở lên.");
            }
            if (request.getNgaySinh().isBefore(today.minusYears(100))) {
                throw new com.example.be.infrastructure.exceptions.ValidationException("Ngày sinh không hợp lệ (không quá 100 tuổi).");
            }
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

    @Override
    @Transactional
    public void registerFace(String id, org.springframework.web.multipart.MultipartFile image) {
        NhanVien nv = adminNhanVienRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên"));
        try {
            if (image == null || image.isEmpty()) {
                throw new RuntimeException("Hình ảnh không hợp lệ");
            }
            byte[] bytes = image.getBytes();
            String base64Image = java.util.Base64.getEncoder().encodeToString(bytes);
            nv.setFaceEncoding("data:image/jpeg;base64," + base64Image);
            adminNhanVienRepository.save(nv);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Lỗi khi xử lý hình ảnh", e);
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AdminNhanVienResponse update(String id, AdminNhanVienRequest request) {
        NhanVien nv = adminNhanVienRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên với id: " + id));

        // Trim khoảng trắng các trường văn bản
        if (request.getTen() != null) request.setTen(request.getTen().trim());
        if (request.getEmail() != null) request.setEmail(request.getEmail().trim());
        if (request.getSdt() != null) request.setSdt(request.getSdt().trim());
        if (request.getDiaChiChiTiet() != null) request.setDiaChiChiTiet(request.getDiaChiChiTiet().trim());

        if (org.springframework.util.StringUtils.hasText(request.getMa()) && adminNhanVienRepository.existsByMaAndIdNot(request.getMa(), id)) {
            throw new DuplicateResourceException("Mã nhân viên đã tồn tại");
        }
        if (org.springframework.util.StringUtils.hasText(request.getEmail()) && adminNhanVienRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new DuplicateResourceException("Email đã được sử dụng");
        }
        if (org.springframework.util.StringUtils.hasText(request.getSdt()) && adminNhanVienRepository.existsBySdtAndIdNot(request.getSdt(), id)) {
            throw new DuplicateResourceException("Số điện thoại đã được sử dụng");
        }
        if (org.springframework.util.StringUtils.hasText(request.getTenTaiKhoan()) && adminNhanVienRepository.existsByTenTaiKhoanAndIdNot(request.getTenTaiKhoan(), id)) {
            throw new DuplicateResourceException("Tên tài khoản đã được sử dụng");
        }

        // Validate độ tuổi (nếu có ngày sinh)
        if (request.getNgaySinh() != null) {
            java.time.LocalDate today = java.time.LocalDate.now();
            if (request.getNgaySinh().isAfter(today)) {
                throw new com.example.be.infrastructure.exceptions.ValidationException("Ngày sinh không thể ở trong tương lai.");
            }
            if (request.getNgaySinh().isAfter(today.minusYears(18))) {
                throw new com.example.be.infrastructure.exceptions.ValidationException("Nhân viên phải từ 18 tuổi trở lên.");
            }
            if (request.getNgaySinh().isBefore(today.minusYears(100))) {
                throw new com.example.be.infrastructure.exceptions.ValidationException("Ngày sinh không hợp lệ (không quá 100 tuổi).");
            }
        }

        // Kiểm tra duy trì tối thiểu 3 Quản lý đang hoạt động khi chuyển chức vụ hoặc ngừng hoạt động
        if (isRoleAdmin(nv) && nv.getTrangThai() == TrangThai.DANG_HOAT_DONG) {
            boolean willRemainAdmin = false;
            if (request.getIdPhanQuyen() != null) {
                com.example.be.entity.PhanQuyen newPq = phanQuyenRepository.findById(request.getIdPhanQuyen()).orElse(null);
                if (newPq != null && VaiTro.isManagementRole(newPq)) {
                    willRemainAdmin = true;
                }
            }
            boolean isDeactivated = (request.getTrangThai() != null && request.getTrangThai() == TrangThai.NGUNG_HOAT_DONG);
            if (!willRemainAdmin || isDeactivated) {
                validateMinimumAdminCount(nv, !willRemainAdmin ? "chuyển chức vụ của quản lý này" : "ngừng hoạt động quản lý này");
            }
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
        validateStatusChangePermission(nv);
        if (trangThai == TrangThai.NGUNG_HOAT_DONG) {
            validateMinimumAdminCount(nv, "ngừng hoạt động quản lý này");
        }
        nv.setTrangThai(trangThai);
        adminNhanVienRepository.saveAndFlush(nv);
    }

    // ── DELETE (soft) ─────────────────────────────────────────────────────
    @Override
    @Transactional
    public void delete(String id) {
        NhanVien nv = adminNhanVienRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên với id: " + id));
        validateStatusChangePermission(nv);
        validateMinimumAdminCount(nv, "ngừng hoạt động quản lý này");
        nv.setTrangThai(TrangThai.NGUNG_HOAT_DONG);
        adminNhanVienRepository.save(nv);
    }

    private void validateMinimumAdminCount(NhanVien targetNv, String actionDescription) {
        if (isRoleAdmin(targetNv) && targetNv.getTrangThai() == TrangThai.DANG_HOAT_DONG) {
            long activeAdminCount = adminNhanVienRepository.findAll().stream()
                    .filter(n -> n.getTrangThai() == TrangThai.DANG_HOAT_DONG && isRoleAdmin(n))
                    .count();
            if (activeAdminCount <= 3) {
                throw new ValidationException("Hệ thống phải duy trì tối thiểu 3 Quản lý đang hoạt động. Không thể " + actionDescription + "!");
            }
        }
    }

    private void validateStatusChangePermission(NhanVien targetNv) {
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();

        boolean isSelf = (targetNv.getTenTaiKhoan() != null && targetNv.getTenTaiKhoan().equalsIgnoreCase(currentUsername))
                || (targetNv.getEmail() != null && targetNv.getEmail().equalsIgnoreCase(currentUsername))
                || (targetNv.getMa() != null && targetNv.getMa().equalsIgnoreCase(currentUsername));

        if (isSelf) {
            throw new ValidationException(MessageConstants.CANNOT_CHANGE_OWN_STATUS);
        }
    }

    private boolean isRoleAdmin(NhanVien nv) {
        return VaiTro.isManagementRole(nv);
    }

    // ── EXPORT EXCEL ──────────────────────────────────────────────────────
    @Override
    public byte[] exportExcel() {
        List<AdminNhanVienResponse> data = adminNhanVienRepository.hienThi();
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

    @Override
    public AdminNhanVienResponse getMe() {
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        AdminNhanVienResponse response = adminNhanVienRepository.detailByTenTaiKhoan(username);
        if (response == null) {
            throw new ResourceNotFoundException("Không tìm thấy thông tin nhân viên đang đăng nhập");
        }
        return response;
    }
}
