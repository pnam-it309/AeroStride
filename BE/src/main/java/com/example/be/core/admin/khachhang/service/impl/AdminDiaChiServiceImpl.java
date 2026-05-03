package com.example.be.core.admin.khachhang.service.impl;

import com.example.be.core.admin.khachhang.model.request.AdminDiaChiRequest;
import com.example.be.core.admin.khachhang.model.response.AdminDiaChiResponse;
import com.example.be.core.admin.khachhang.repository.AdminDiaChiRepository;
import com.example.be.core.admin.khachhang.service.AdminDiaChiService;
import com.example.be.entity.DiaChi;
import com.example.be.entity.KhachHang;
import com.example.be.repository.KhachHangRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDiaChiServiceImpl implements AdminDiaChiService {

    private final AdminDiaChiRepository repository;
    private final KhachHangRepository khachHangRepository;

    @Override
    public List<AdminDiaChiResponse> getByKhachHangId(String khId) {
        return repository.findByKhachHangId(khId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AdminDiaChiResponse add(AdminDiaChiRequest request) {
        KhachHang kh = khachHangRepository.findById(request.getIdKhachHang())
                .orElseThrow(() -> new RuntimeException("Khach hang khong ton tai"));
        
        DiaChi dc = new DiaChi();
        BeanUtils.copyProperties(request, dc);
        dc.setKhachHang(kh);
        
        // Save DiaChi first to ensure it has an ID before assigning to KhachHang
        dc = repository.save(dc);
        
        if (Boolean.TRUE.equals(request.getLaMacDinh())) {
            unsetOldDefault(kh.getId());
            kh.setDiaChi(dc);
            khachHangRepository.save(kh);
        }
        
        return toResponse(dc);
    }

    @Override
    @Transactional
    public AdminDiaChiResponse update(String id, AdminDiaChiRequest request) {
        DiaChi dc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dia chi khong ton tai"));
        
        BeanUtils.copyProperties(request, dc, "id", "khachHang");
        dc = repository.save(dc);
        
        if (Boolean.TRUE.equals(request.getLaMacDinh())) {
            unsetOldDefault(dc.getKhachHang().getId());
            KhachHang kh = dc.getKhachHang();
            kh.setDiaChi(dc);
            khachHangRepository.save(kh);
        }
        
        return toResponse(dc);
    }

    @Override
    @Transactional
    public void delete(String id) {
        DiaChi dc = repository.findById(id).orElse(null);
        if (dc != null) {
            List<KhachHang> khs = khachHangRepository.findByDiaChiId(id);
            if (khs != null && !khs.isEmpty()) {
                for (KhachHang kh : khs) {
                    kh.setDiaChi(null);
                    khachHangRepository.save(kh);
                }
            }
            repository.delete(dc);
        }
    }

    @Override
    @Transactional
    public void setDefault(String id) {
        DiaChi dc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dia chi khong ton tai"));
        unsetOldDefault(dc.getKhachHang().getId());
        dc.setLaMacDinh(true);
        repository.save(dc);

        // Update default address in KhachHang entity for quick reference
        KhachHang kh = dc.getKhachHang();
        if (kh != null) {
            kh.setDiaChi(dc);
            khachHangRepository.save(kh);
        }
    }

    private void unsetOldDefault(String khId) {
        DiaChi oldDefault = repository.findDefaultByKhachHangId(khId);
        if (oldDefault != null) {
            oldDefault.setLaMacDinh(false);
            repository.save(oldDefault);
        }
    }

    private AdminDiaChiResponse toResponse(DiaChi dc) {
        return AdminDiaChiResponse.builder()
                .id(dc.getId())
                .tinh(dc.getTinh())
                .thanhPho(dc.getThanhPho())
                .phuongXa(dc.getPhuongXa())
                .diaChiChiTiet(dc.getDiaChiChiTiet())
                .tenNguoiNhan(dc.getTenNguoiNhan())
                .sdtNguoiNhan(dc.getSdtNguoiNhan())
                .laMacDinh(dc.getLaMacDinh())
                .idKhachHang(dc.getKhachHang() != null ? dc.getKhachHang().getId() : null)
                .tenKhachHang(dc.getKhachHang() != null ? dc.getKhachHang().getTen() : "Khách lẻ")
                .build();
    }
}
