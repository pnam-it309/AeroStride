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
    @Transactional(readOnly = true)
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
        
        if (Boolean.TRUE.equals(request.getLaMacDinh())) {
            unsetOldDefault(kh.getId());
        }
        
        DiaChi savedDc = repository.save(dc);
        
        // Sync with KhachHang if this is the new default
        if (Boolean.TRUE.equals(request.getLaMacDinh())) {
            kh.setDiaChi(savedDc);
            khachHangRepository.save(kh);
        }
        
        return toResponse(savedDc);
    }

    @Override
    @Transactional
    public AdminDiaChiResponse update(String id, AdminDiaChiRequest request) {
        DiaChi dc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dia chi khong ton tai"));
        
        BeanUtils.copyProperties(request, dc, "id", "khachHang");
        
        // Ensure legacy addresses missing khachHang link are updated
        if (dc.getKhachHang() == null && request.getIdKhachHang() != null) {
            khachHangRepository.findById(request.getIdKhachHang()).ifPresent(dc::setKhachHang);
        }
        
        if (Boolean.TRUE.equals(request.getLaMacDinh()) && dc.getKhachHang() != null) {
            unsetOldDefault(dc.getKhachHang().getId());
        }
        
        DiaChi savedDc = repository.save(dc);
        
        // Sync with KhachHang if this is the new default
        if (Boolean.TRUE.equals(request.getLaMacDinh()) && dc.getKhachHang() != null) {
            KhachHang kh = dc.getKhachHang();
            kh.setDiaChi(savedDc);
            khachHangRepository.save(kh);
        }
        
        return toResponse(savedDc);
    }

    @Override
    @Transactional
    public void delete(String id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public void setDefault(String id) {
        DiaChi dc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dia chi khong ton tai"));
        
        if (dc.getKhachHang() == null) {
            throw new RuntimeException("Dia chi khong thuoc khach hang nao");
        }
        
        unsetOldDefault(dc.getKhachHang().getId());
        dc.setLaMacDinh(true);
        DiaChi savedDc = repository.save(dc);
        
        // Sync with KhachHang
        KhachHang kh = dc.getKhachHang();
        kh.setDiaChi(savedDc);
        khachHangRepository.save(kh);
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
