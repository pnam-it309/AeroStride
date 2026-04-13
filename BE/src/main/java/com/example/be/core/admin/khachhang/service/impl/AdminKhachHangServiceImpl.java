package com.example.be.core.admin.khachhang.service.impl;

import com.example.be.core.admin.khachhang.model.request.AdminKhachHangRequest;
import com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse;
import com.example.be.core.admin.khachhang.repository.AdminKhachHangRepository;
import com.example.be.core.admin.khachhang.service.AdminKhachHangService;
import com.example.be.core.admin.khachhang.service.AdminDiaChiService;
import com.example.be.utils.ExcelUtils;
import com.example.be.utils.MaGenerator;
import com.example.be.entity.KhachHang;
import com.example.be.entity.DiaChi;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.repository.DiaChiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.be.infrastructure.exceptions.DuplicateResourceException;

import java.io.IOException;
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

    @Autowired
    private AdminDiaChiService adminDiaChiService;

    @Override
    public List<AdminKhachHangResponse> hienThi() {
        return adminKhachHangRepository.hienThi();
    }

    @Override
    public Page<AdminKhachHangResponse> phanTrang(AdminKhachHangRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return adminKhachHangRepository.filterAll(
            request.getKeyword(),
            request.getTrangThai(),
            request.getGioiTinh(),
            pageable
        );
    }

    @Override
    public Page<AdminKhachHangResponse> timKiem(AdminKhachHangRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return adminKhachHangRepository.timKiem(request.getKeyword(), pageable);
    }

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

    @Override
    public AdminKhachHangResponse detail(String id) {
        AdminKhachHangResponse response = adminKhachHangRepository.detail(id);
        if (response == null) {
            throw new RuntimeException("Không tìm thấy khách hàng");
        }
        response.setAddresses(adminDiaChiService.getByKhachHangId(id));
        return response;
    }

    @Override
    @Transactional
    public AdminKhachHangResponse add(AdminKhachHangRequest request) {
        if (request.getMa() != null && !request.getMa().trim().isEmpty()) {
            if (adminKhachHangRepository.existsByMa(request.getMa()))
                throw new DuplicateResourceException("Mã khách hàng này đã tồn tại trong hệ thống.");
        }
        if (adminKhachHangRepository.existsByEmail(request.getEmail()))
            throw new DuplicateResourceException("Email này đã được sử dụng bởi một khách hàng khác.");
        if (adminKhachHangRepository.existsByTenTaiKhoan(request.getTenTaiKhoan()))
            throw new DuplicateResourceException("Tên tài khoản này đã tồn tại. Vui lòng chọn tên khác.");

        KhachHang kh = toEntity(request);
        if (kh.getMa() == null || kh.getMa().trim().isEmpty()) {
            kh.setMa(MaGenerator.generate(KhachHang.class));
        }
        kh.setTrangThai(TrangThai.DANG_HOAT_DONG);

        if (request.getMatKhau() != null && !request.getMatKhau().isBlank())
            kh.setMatKhau(passwordEncoder.encode(request.getMatKhau()));

        adminKhachHangRepository.save(kh);

        // Handle address
        if (request.getIdDiaChi() != null) {
            DiaChi dc = diaChiRepository.findById(request.getIdDiaChi())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));
            dc.setKhachHang(kh);
            dc.setLaMacDinh(true);
            diaChiRepository.save(dc);
        } else if (request.getTinh() != null && !request.getTinh().trim().isEmpty()) {
            DiaChi dc = new DiaChi();
            dc.setKhachHang(kh);
            dc.setTinh(request.getTinh());
            dc.setThanhPho(request.getThanhPho());
            dc.setPhuongXa(request.getPhuongXa());
            dc.setDiaChiChiTiet(request.getDiaChiChiTiet());
            dc.setLaMacDinh(true);
            dc.setTenNguoiNhan(kh.getTen());
            dc.setSdtNguoiNhan(kh.getSdt());
            diaChiRepository.save(dc);
        }

        return adminKhachHangRepository.detail(kh.getId());
    }

    @Override
    @Transactional
    public AdminKhachHangResponse update(String id, AdminKhachHangRequest req) {
        KhachHang kh = adminKhachHangRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        if (adminKhachHangRepository.existsByMaAndIdNot(req.getMa(), id))
            throw new RuntimeException("Mã khách hàng đã tồn tại");
        if (adminKhachHangRepository.existsByEmailAndIdNot(req.getEmail(), id))
            throw new RuntimeException("Email đã được sử dụng");
        if (adminKhachHangRepository.existsByTenTaiKhoanAndIdNot(req.getTenTaiKhoan(), id))
            throw new RuntimeException("Tên tài khoản đã được sử dụng");

        updateEntity(kh, req);

        if (req.getMatKhau() != null && !req.getMatKhau().isBlank())
            kh.setMatKhau(passwordEncoder.encode(req.getMatKhau()));

        adminKhachHangRepository.save(kh);

        if (req.getIdDiaChi() != null) {
            DiaChi dc = diaChiRepository.findById(req.getIdDiaChi())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));
            dc.setKhachHang(kh);
            dc.setLaMacDinh(true);
            diaChiRepository.save(dc);
        }

        return adminKhachHangRepository.detail(kh.getId());
    }

    @Override
    @Transactional
    public void doiTrangThai(String id, TrangThai trangThai) {
        KhachHang kh = adminKhachHangRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
        kh.setTrangThai(trangThai);
        adminKhachHangRepository.saveAndFlush(kh);
    }

    @Override
    public void delete(String id) {
        KhachHang kh = adminKhachHangRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
        kh.setTrangThai(TrangThai.KHONG_HOAT_DONG);
        adminKhachHangRepository.save(kh);
    }

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
            throw new RuntimeException("Lỗi xuất file Excel: " + e.getMessage());
        }
    }

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
