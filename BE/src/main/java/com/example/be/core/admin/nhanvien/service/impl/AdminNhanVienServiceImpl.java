package com.example.be.core.admin.nhanvien.service.impl;

import com.example.be.core.admin.nhanvien.model.request.AdminNhanVienRequest;
import com.example.be.core.admin.nhanvien.model.response.AdminNhanVienResponse;
import com.example.be.core.admin.nhanvien.repository.AdminNhanVienRepository;
import com.example.be.core.admin.nhanvien.service.AdminNhanVienService;

import com.example.be.core.admin.nhanvien.service.NhanVienEmailService;
import com.example.be.entity.NhanVien;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.DuplicateResourceException;
import com.example.be.repository.PhanQuyenRepository;
import com.example.be.utils.ExcelUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;


@Service
@RequiredArgsConstructor
public class AdminNhanVienServiceImpl implements AdminNhanVienService {
    @Autowired
    private AdminNhanVienRepository adminNhanVienRepository;
    @Autowired
    private PhanQuyenRepository phanQuyenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private NhanVienEmailService emailService;

    @Override
    public List<AdminNhanVienResponse> hienThi(){
        return adminNhanVienRepository.hienThi();
    }

    // ── PHÂN TRANG
    @Override
    public Page<AdminNhanVienResponse> phanTrang(AdminNhanVienRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return adminNhanVienRepository.phanTrang(pageable);
    }

    // ── TÌM KIẾM ──────────────────────────────────────────────────────────
    @Override
    public Page<AdminNhanVienResponse> timKiem(AdminNhanVienRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return adminNhanVienRepository.searchNhanVien(request.getKeyword(), pageable);
    }

    // ── LỌC THEO TRẠNG THÁI ───────────────────────────────────────────────
    @Override
    public Page<AdminNhanVienResponse> locNV(AdminNhanVienRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return adminNhanVienRepository.filterAll(
            request.getKeyword(),
            request.getTrangThai(),
            request.getGioiTinh(),
            pageable);
    }

    // ── DETAIL ────────────────────────────────────────────────────────────
    @Override
    public AdminNhanVienResponse detail(String id) {
        AdminNhanVienResponse res = adminNhanVienRepository.detail(id);
        if (res == null)
            throw new RuntimeException("Không tìm thấy nhân viên");
        return res;
    }

    // ── CREATE ────────────────────────────────────────────────────────────
    // ── CREATE ────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AdminNhanVienResponse add(AdminNhanVienRequest request) {
        // Kiểm tra ma - bỏ qua nếu blank, hệ thống tự sinh
        if (request.getMa() != null && !request.getMa().trim().isEmpty()) {
            if (adminNhanVienRepository.existsByMa(request.getMa()))
                throw new DuplicateResourceException("Mã nhân viên này đã tồn tại.");
        }

        if (adminNhanVienRepository.existsByEmail(request.getEmail()))
            throw new DuplicateResourceException("Email này đã được sử dụng bởi một nhân viên khác.");

        // Bỏ qua check tenTaiKhoan từ request - hệ thống tự sinh bên dưới
        NhanVien nv = toEntity(request);

        // Tự sinh mã nếu trống
        if (nv.getMa() == null || nv.getMa().trim().isEmpty()) {
            nv.setMa(taoMaNhanVien());
        }

        // Tự sinh tenTaiKhoan unique
        String tenTaiKhoan;
        do {
            tenTaiKhoan = taoTenTaiKhoan(request.getTen()) + (int)(Math.random() * 1000);
        } while (adminNhanVienRepository.existsByTenTaiKhoan(tenTaiKhoan));

        // Tự sinh mật khẩu tạm (plain text để gửi mail, encode để lưu DB)
        String matKhauTam = taoMatKhauTam();

        nv.setTenTaiKhoan(tenTaiKhoan);
        nv.setMatKhau(passwordEncoder.encode(matKhauTam));

        if (request.getIdPhanQuyen() != null)
            nv.setPhanQuyen(phanQuyenRepository.findById(request.getIdPhanQuyen())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phân quyền")));

        nv.setTrangThai(TrangThai.DANG_HOAT_DONG);
        adminNhanVienRepository.save(nv);

        String tenVaiTro = (nv.getPhanQuyen() != null) ? nv.getPhanQuyen().getTen() : "Nhân viên";

        emailService.guiEmailTaiKhoanNhanVien(
            request.getEmail(),
            request.getTen(),
            tenTaiKhoan,
            matKhauTam,
            tenVaiTro
        );

        return adminNhanVienRepository.detail(nv.getId());
    }

    // ── HELPER METHODS ────────────────────────────────────────────────────
    private String taoMaNhanVien() {
        List<String> danhSachMa = adminNhanVienRepository.findAllMa();
        if (danhSachMa.isEmpty()) return "NV01";

        int max = 0;
        for (String ma : danhSachMa) {
            try {
                int so = Integer.parseInt(ma.substring(2).trim());
                if (so > max) max = so;
            } catch (Exception ignored) {}
        }
        return String.format("NV%02d", max + 1);
    }

    private String taoTenTaiKhoan(String hoTen) {
        if (hoTen == null || hoTen.isBlank()) return "nhanvien";
        String[] parts = hoTen.trim().split("\\s+");
        String ten = xoaDau(parts[parts.length - 1]).toLowerCase();
        StringBuilder vietTat = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            vietTat.append(xoaDau(String.valueOf(parts[i].charAt(0))).toLowerCase());
        }
        return ten + vietTat;
    }

    private String taoMatKhauTam() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#";
        StringBuilder sb = new StringBuilder();
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < 10; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    private String xoaDau(String s) {
        String result = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        result = result.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return result.replace("đ", "d").replace("Đ", "D");
    }

    // ── UPDATE ────────────────────────────────────────────────────────────
    @Override
    public AdminNhanVienResponse update(String id, AdminNhanVienRequest request) {
        NhanVien nv = adminNhanVienRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        if (adminNhanVienRepository.existsByMaAndIdNot(request.getMa(), id))
            throw new RuntimeException("Mã nhân viên đã tồn tại");
        if (adminNhanVienRepository.existsByEmailAndIdNot(request.getEmail(), id))
            throw new RuntimeException("Email đã được sử dụng");
        if (adminNhanVienRepository.existsByTenTaiKhoanAndIdNot(request.getTenTaiKhoan(), id))
            throw new RuntimeException("Tên tài khoản đã được sử dụng");

        updateEntity(nv, request);

        // Admin cannot update password manually

        if (request.getIdPhanQuyen() != null)
            nv.setPhanQuyen(phanQuyenRepository.findById(request.getIdPhanQuyen())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phân quyền")));
        else
            nv.setPhanQuyen(null);

        adminNhanVienRepository.save(nv);
        return adminNhanVienRepository.detail(id);
    }

    // ── ĐỔI TRẠNG THÁI ───────────────────────────────────────────────────
    @Override
    @Transactional
    public void doiTrangThai(String id, TrangThai trangThai) {
        NhanVien nv = adminNhanVienRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        nv.setTrangThai(trangThai);
        adminNhanVienRepository.saveAndFlush(nv);
    }

    // ── DELETE (soft) ─────────────────────────────────────────────────────
    @Override
    public void delete(String id) {
        NhanVien nv = adminNhanVienRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        nv.setTrangThai(TrangThai.KHONG_HOAT_DONG);
        adminNhanVienRepository.save(nv);
    }

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
            throw new RuntimeException("Lỗi xuất file Excel: " + e.getMessage());
        }
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────
    private NhanVien toEntity(AdminNhanVienRequest request) {
        NhanVien nv = new NhanVien();
            nv.setMa(request.getMa());
            nv.setTen(request.getTen());
            nv.setEmail(request.getEmail());
            nv.setTenTaiKhoan(request.getTenTaiKhoan());
            nv.setGioiTinh(request.getGioiTinh());
            nv.setSdt(request.getSdt());
            nv.setNgaySinh(request.getNgaySinh());
            nv.setHinhAnh(request.getHinhAnh());
            return nv;
    }

    private void updateEntity(NhanVien nv, AdminNhanVienRequest req) {
        nv.setMa(req.getMa());
        nv.setTen(req.getTen());
        nv.setEmail(req.getEmail());
        nv.setTenTaiKhoan(req.getTenTaiKhoan());
        nv.setGioiTinh(req.getGioiTinh());
        nv.setSdt(req.getSdt());
        nv.setNgaySinh(req.getNgaySinh());
        nv.setHinhAnh(req.getHinhAnh());
    }

    @Override
    public List<com.example.be.entity.PhanQuyen> getAllPhanQuyen() {
        return phanQuyenRepository.findAll();
    }
}
