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

    @Override
    public CustomerVoucherResponse getBestVoucherSuggestion(java.math.BigDecimal orderValue) {
        if (orderValue == null || orderValue.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return null;
        }

        long now = System.currentTimeMillis();
        List<PhieuGiamGia> validVouchers = voucherRepository.findAll().stream()
                .filter(v -> "CONG_KHAI".equals(v.getHinhThuc())
                        && v.getTrangThai() == TrangThai.DANG_HOAT_DONG
                        && v.getNgayBatDau() <= now
                        && v.getNgayKetThuc() >= now
                        && (v.getSoLuong() == null || v.getSoLuong() > 0)
                        && (v.getDonHangToiThieu() == null || orderValue.compareTo(v.getDonHangToiThieu()) >= 0))
                .collect(Collectors.toList());

        PhieuGiamGia bestVoucher = null;
        java.math.BigDecimal maxDiscount = java.math.BigDecimal.ZERO;

        for (PhieuGiamGia v : validVouchers) {
            java.math.BigDecimal discount = java.math.BigDecimal.ZERO;
            if ("PHAN_TRAM".equals(v.getLoaiPhieu()) && v.getPhanTramGiamGia() != null) {
                discount = orderValue.multiply(new java.math.BigDecimal(v.getPhanTramGiamGia())).divide(new java.math.BigDecimal(100));
                if (v.getGiamToiDa() != null && discount.compareTo(v.getGiamToiDa()) > 0) {
                    discount = v.getGiamToiDa();
                }
            } else if ("TIEN_MAT".equals(v.getLoaiPhieu()) && v.getSoTienGiam() != null) {
                discount = v.getSoTienGiam();
            }

            if (discount.compareTo(maxDiscount) > 0) {
                maxDiscount = discount;
                bestVoucher = v;
            }
        }

        return bestVoucher != null ? convertToResponse(bestVoucher) : null;
    }
}
