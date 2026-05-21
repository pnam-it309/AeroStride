package com.example.be.core.customer.voucher.service.impl;

import com.example.be.core.customer.voucher.model.response.CustomerVoucherResponse;
import com.example.be.core.customer.voucher.repository.CustomerVoucherRepository;
import com.example.be.core.customer.voucher.service.CustomerVoucherService;
import com.example.be.entity.PhieuGiamGia;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerVoucherServiceImpl implements CustomerVoucherService {

    private final CustomerVoucherRepository voucherRepository;

    @Override
    public List<CustomerVoucherResponse> getPublicVouchers() {
        long now = System.currentTimeMillis();
        
        List<PhieuGiamGia> vouchers = voucherRepository.findAll().stream()
                .filter(v -> "CONG_KHAI".equals(v.getHinhThuc()) 
                        && v.getTrangThai() == TrangThai.DANG_HOAT_DONG
                        && v.getNgayBatDau() <= now 
                        && v.getNgayKetThuc() >= now
                        && (v.getSoLuong() == null || v.getSoLuong() > 0))
                .collect(Collectors.toList());

        return vouchers.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    private CustomerVoucherResponse convertToResponse(PhieuGiamGia v) {
        return CustomerVoucherResponse.builder()
                .id(v.getId())
                .ma(v.getMa())
                .ten(v.getTen())
                .loaiPhieu(v.getLoaiPhieu())
                .hinhThuc(v.getHinhThuc())
                .phanTramGiamGia(v.getPhanTramGiamGia())
                .soTienGiam(v.getSoTienGiam())
                .soLuong(v.getSoLuong())
                .donHangToiThieu(v.getDonHangToiThieu())
                .giamToiDa(v.getGiamToiDa())
                .ngayBatDau(v.getNgayBatDau())
                .ngayKetThuc(v.getNgayKetThuc())
                .trangThai(v.getTrangThai().name())
                .build();
    }
}
