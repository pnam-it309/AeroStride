package com.example.be.core.admin.nhanvien.service.impl;

import com.example.be.core.admin.nhanvien.model.request.AdminNhanVienRequest;
import com.example.be.core.admin.nhanvien.model.response.AdminNhanVienResponse;
import com.example.be.core.admin.nhanvien.repository.AdminNhanVienRepository;
import com.example.be.core.admin.nhanvien.service.AdminNhanVienService;
import com.example.be.entity.NhanVien;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.repository.PhanQuyenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    @Override
    public AdminNhanVienResponse add(AdminNhanVienRequest request) {
        if (adminNhanVienRepository.existsByMa(request.getMa()))
            throw new RuntimeException("Mã nhân viên đã tồn tại");
        if (adminNhanVienRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Email đã tồn tại");
        if (adminNhanVienRepository.existsByTenTaiKhoan(request.getTenTaiKhoan()))
            throw new RuntimeException("Tên tài khoản đã tồn tại");

        NhanVien nv = toEntity(request);

        if (request.getMatKhau() != null && !request.getMatKhau().isBlank())
            nv.setMatKhau(passwordEncoder.encode(request.getMatKhau()));

        if (request.getIdPhanQuyen() != null)
            nv.setPhanQuyen(phanQuyenRepository.findById(request.getIdPhanQuyen())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phân quyền")));

        nv.setTrangThai(TrangThai.DANG_HOAT_DONG);
        adminNhanVienRepository.save(nv);
        return adminNhanVienRepository.detail(nv.getId());
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

        if (request.getMatKhau() != null && !request.getMatKhau().isBlank())
            nv.setMatKhau(passwordEncoder.encode(request.getMatKhau()));

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
    public void doiTrangThai(String id, TrangThai trangThai) {
        NhanVien nv = adminNhanVienRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        nv.setTrangThai(trangThai);
        adminNhanVienRepository.save(nv);
    }

    // ── DELETE (soft) ─────────────────────────────────────────────────────
    @Override
    public void delete(String id) {
        NhanVien nv = adminNhanVienRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        nv.setTrangThai(TrangThai.KHONG_HOAT_DONG);
        adminNhanVienRepository.save(nv);
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

}
