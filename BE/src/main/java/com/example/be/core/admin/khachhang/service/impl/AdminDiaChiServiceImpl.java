package com.example.be.core.admin.khachhang.service.impl;

import com.example.be.core.admin.khachhang.model.request.AdminDiaChiRequest;
import com.example.be.core.admin.khachhang.model.response.AdminDiaChiResponse;
import com.example.be.core.admin.khachhang.repository.AdminDiaChiRepository;
import com.example.be.core.admin.khachhang.service.AdminDiaChiService;
import com.example.be.entity.DiaChi;
import com.example.be.entity.KhachHang;
import com.example.be.core.admin.khachhang.repository.AdminKhachHangRepository;
import com.example.be.infrastructure.constants.MessageConstants;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
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
    private final AdminKhachHangRepository khachHangRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminDiaChiResponse> getByKhachHangId(String khId) {
        List<DiaChi> list = repository.findByKhachHangId(khId);
        KhachHang kh = khachHangRepository.findById(khId).orElse(null);
        if (kh != null && kh.getDiaChi() != null) {
            boolean exists = list.stream().anyMatch(d -> d.getId().equals(kh.getDiaChi().getId()));
            if (!exists) {
                list.add(0, kh.getDiaChi());
            }
        }

        // Deduplicate addresses by content
        java.util.Map<String, DiaChi> uniqueMap = new java.util.LinkedHashMap<>();
        for (DiaChi dc : list) {
            String key = String.format("%s|%s|%s|%s|%s|%s",
                    normalizeStr(dc.getTenNguoiNhan()),
                    normalizePhone(dc.getSdtNguoiNhan()),
                    normalizeStr(dc.getDiaChiChiTiet()),
                    normalizeStr(dc.getPhuongXa()),
                    normalizeStr(dc.getThanhPho()),
                    normalizeStr(dc.getTinh())
            );

            if (!uniqueMap.containsKey(key) || Boolean.TRUE.equals(dc.getLaMacDinh())) {
                uniqueMap.put(key, dc);
            }
        }

        return uniqueMap.values().stream().map(this::toResponse).collect(Collectors.toList());
    }

    private String normalizeStr(String str) {
        return str == null ? "" : str.trim().toLowerCase();
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("\\D", "");
    }

    @Override
    @Transactional
    public AdminDiaChiResponse add(AdminDiaChiRequest request) {
        KhachHang kh = khachHangRepository.findById(request.getIdKhachHang())
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.KHACH_HANG_NOT_FOUND + request.getIdKhachHang()));
        
        if (Boolean.TRUE.equals(request.getLaMacDinh())) {
            unsetOldDefault(kh.getId());
        }

        DiaChi dc = new DiaChi();
        BeanUtils.copyProperties(request, dc);
        if (dc.getTinh() != null) dc.setTinh(dc.getTinh().trim());
        if (dc.getThanhPho() != null) dc.setThanhPho(dc.getThanhPho().trim());
        if (dc.getPhuongXa() != null) dc.setPhuongXa(dc.getPhuongXa().trim());
        if (dc.getDiaChiChiTiet() != null) dc.setDiaChiChiTiet(dc.getDiaChiChiTiet().trim().replaceAll("\\s+", " "));
        if (dc.getTenNguoiNhan() != null) dc.setTenNguoiNhan(dc.getTenNguoiNhan().trim().replaceAll("\\s+", " "));
        if (dc.getSdtNguoiNhan() != null) dc.setSdtNguoiNhan(dc.getSdtNguoiNhan().trim());
        dc.setKhachHang(kh);
        
        dc = repository.save(dc);
        
        if (Boolean.TRUE.equals(request.getLaMacDinh())) {
            kh.setDiaChi(dc);
            khachHangRepository.save(kh);
        }
        
        return toResponse(dc);
    }

    @Override
    @Transactional
    public AdminDiaChiResponse update(String id, AdminDiaChiRequest request) {
        DiaChi dc = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.DIA_CHI_NOT_FOUND));
        
        if (Boolean.TRUE.equals(request.getLaMacDinh())) {
            unsetOldDefaultExcluding(dc.getKhachHang().getId(), id);
        }

        BeanUtils.copyProperties(request, dc, "id", "khachHang");
        if (dc.getTinh() != null) dc.setTinh(dc.getTinh().trim());
        if (dc.getThanhPho() != null) dc.setThanhPho(dc.getThanhPho().trim());
        if (dc.getPhuongXa() != null) dc.setPhuongXa(dc.getPhuongXa().trim());
        if (dc.getDiaChiChiTiet() != null) dc.setDiaChiChiTiet(dc.getDiaChiChiTiet().trim().replaceAll("\\s+", " "));
        if (dc.getTenNguoiNhan() != null) dc.setTenNguoiNhan(dc.getTenNguoiNhan().trim().replaceAll("\\s+", " "));
        if (dc.getSdtNguoiNhan() != null) dc.setSdtNguoiNhan(dc.getSdtNguoiNhan().trim());
        dc = repository.save(dc);
        
        if (Boolean.TRUE.equals(request.getLaMacDinh())) {
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
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.DIA_CHI_NOT_FOUND));
        unsetOldDefaultExcluding(dc.getKhachHang().getId(), id);
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
        unsetOldDefaultExcluding(khId, null);
    }

    private void unsetOldDefaultExcluding(String khId, String excludeAddrId) {
        List<DiaChi> list = repository.findByKhachHangId(khId);
        for (DiaChi addr : list) {
            if (excludeAddrId == null || !addr.getId().equals(excludeAddrId)) {
                if (Boolean.TRUE.equals(addr.getLaMacDinh())) {
                    addr.setLaMacDinh(false);
                    repository.save(addr);
                }
            }
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
                .tenKhachHang(dc.getKhachHang() != null ? dc.getKhachHang().getTen() : MessageConstants.KHACH_LE)
                .build();
    }
}
