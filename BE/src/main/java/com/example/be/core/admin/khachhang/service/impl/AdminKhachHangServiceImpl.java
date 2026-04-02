package com.example.be.core.admin.khachhang.service.impl;

import com.example.be.core.admin.khachhang.model.request.AdminKhachHangRequest;
import com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse;
import com.example.be.core.admin.khachhang.repository.AdminKhachHangRepository;
import com.example.be.core.admin.khachhang.service.AdminKhachHangService;
import org.springframework.data.domain.PageRequest;
import com.example.be.entity.KhachHang;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.repository.DiaChiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminKhachHangServiceImpl implements AdminKhachHangService {
    @Autowired
    private AdminKhachHangRepository adminKhachHangRepository;
    @Autowired
    private DiaChiRepository diaChiRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    // getAll
    @Override
    public List<AdminKhachHangResponse> hienThi() {
        return adminKhachHangRepository.hienThi();
    }

    //phan trang
    @Override
    public Page<AdminKhachHangResponse> phanTrang(AdminKhachHangRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return adminKhachHangRepository.phanTrang(pageable);
    }

    // TÌM KIẾM
    @Override
    public Page<AdminKhachHangResponse> timKiem(AdminKhachHangRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return adminKhachHangRepository.timKiem(request.getKeyword(), pageable);
    }

    //lọc theo trạng thái
    @Override
    public Page<AdminKhachHangResponse> locKH(AdminKhachHangRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        return adminKhachHangRepository.filterAll(
            request.getKeyword(),
            request.getTrangThai(),
            request.getGioiTinh(),
            pageable
        );
    }
// detail
    @Override
    public AdminKhachHangResponse detail(String id) {
        AdminKhachHangResponse response = adminKhachHangRepository.detail(id);
        if (response == null)
            throw new RuntimeException("Không tìm thấy khách hàng");
        return response;
    }
    // ── CREATE ────────────────────────────────────────────────────────────
    @Override
    public AdminKhachHangResponse add(AdminKhachHangRequest request) {
        if (adminKhachHangRepository.existsByMa(request.getMa()))
            throw new RuntimeException("Mã khách hàng đã tồn tại");
        if (adminKhachHangRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Email đã tồn tại");
        if (adminKhachHangRepository.existsByTenTaiKhoan(request.getTenTaiKhoan()))
            throw new RuntimeException("Tên tài khoản đã tồn tại");

        KhachHang kh = toEntity(request);
        kh.setTrangThai(TrangThai.DANG_HOAT_DONG);

        if (request.getMatKhau() != null && !request.getMatKhau().isBlank())
            kh.setMatKhau(passwordEncoder.encode(request.getMatKhau()));

        if (request.getIdDiaChi() != null)
            kh.setDiaChi(diaChiRepository.findById(request.getIdDiaChi())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ")));


        adminKhachHangRepository.save(kh);
        return adminKhachHangRepository.detail(kh.getId());
    }

    // ── UPDATE ────────────────────────────────────────────────────────────
    @Override
    public AdminKhachHangResponse update(String id, AdminKhachHangRequest req) {
        KhachHang kh = adminKhachHangRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        if (adminKhachHangRepository.existsByMaAndIdNot(req.getMa(), String.valueOf(id)))
            throw new RuntimeException("Mã khách hàng đã tồn tại");
        if (adminKhachHangRepository.existsByEmailAndIdNot(req.getEmail(), String.valueOf(id)))
            throw new RuntimeException("Email đã được sử dụng");
        if (adminKhachHangRepository.existsByTenTaiKhoanAndIdNot(req.getTenTaiKhoan(), String.valueOf(id)))
            throw new RuntimeException("Tên tài khoản đã được sử dụng");

        updateEntity(kh, req);

        if (req.getMatKhau() != null && !req.getMatKhau().isBlank())
            kh.setMatKhau(passwordEncoder.encode(req.getMatKhau()));

        if (req.getIdDiaChi() != null)
            kh.setDiaChi(diaChiRepository.findById(req.getIdDiaChi())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ")));
        else
            kh.setDiaChi(null);

        adminKhachHangRepository.save(kh);
        return adminKhachHangRepository.detail(kh.getId());
    }

    // ── ĐỔI TRẠNG THÁI ───────────────────────────────────────────────────
    @Override
    public void doiTrangThai(String id, TrangThai trangThai) {
        KhachHang kh = adminKhachHangRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
        kh.setTrangThai(trangThai);
        adminKhachHangRepository.save(kh);
    }
    // ── DELETE (soft) ─────────────────────────────────────────────────────
    @Override
    public void delete(String id) {
        KhachHang kh = adminKhachHangRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
        kh.setTrangThai(TrangThai.KHONG_HOAT_DONG);
        adminKhachHangRepository.save(kh);
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────
    private KhachHang toEntity(AdminKhachHangRequest req) {
        KhachHang kh = new KhachHang();
        kh.setMa(req.getMa());
        kh.setTen(req.getTen());
        kh.setEmail(req.getEmail());
        kh.setTenTaiKhoan(req.getTenTaiKhoan());
        kh.setGioiTinh(req.getGioiTinh());
        kh.setSdt(req.getSdt());
        kh.setNgaySinh(req.getNgaySinh());
        kh.setHinhAnh(req.getHinhAnh());
        kh.setGhiChu(req.getGhiChu());
        return kh;
    }

    private void updateEntity(KhachHang kh, AdminKhachHangRequest req) {
        kh.setMa(req.getMa());
        kh.setTen(req.getTen());
        kh.setEmail(req.getEmail());
        kh.setTenTaiKhoan(req.getTenTaiKhoan());
        kh.setGioiTinh(req.getGioiTinh());
        kh.setSdt(req.getSdt());
        kh.setNgaySinh(req.getNgaySinh());
        kh.setHinhAnh(req.getHinhAnh());
        kh.setGhiChu(req.getGhiChu());
    }
}
