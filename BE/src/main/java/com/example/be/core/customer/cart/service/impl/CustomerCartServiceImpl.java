package com.example.be.core.customer.cart.service.impl;

import com.example.be.core.customer.cart.model.request.CustomerCartSyncItemRequest;
import com.example.be.core.customer.cart.model.request.CustomerCartSyncRequest;
import com.example.be.core.customer.cart.model.response.CustomerCartItemResponse;
import com.example.be.core.customer.cart.model.response.CustomerCartSyncResponse;
import com.example.be.core.customer.cart.service.CustomerCartService;
import com.example.be.core.customer.cart.repository.CustomerCartChiTietSanPhamRepository;
import com.example.be.core.customer.sanpham.repository.CustomerSanPhamChiTietDotGiamGiaRepository;
import com.example.be.entity.ChiTietDotGiamGia;
import com.example.be.entity.ChiTietSanPham;
import com.example.be.entity.DotGiamGia;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerCartServiceImpl implements CustomerCartService {

    private final CustomerCartChiTietSanPhamRepository chiTietSanPhamRepository;
    private final CustomerSanPhamChiTietDotGiamGiaRepository chiTietDotGiamGiaRepository;

    @Override
    @Transactional(readOnly = true)
    public CustomerCartSyncResponse syncCart(CustomerCartSyncRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return new CustomerCartSyncResponse(new ArrayList<>(), BigDecimal.ZERO);
        }

        List<String> ids = request.getItems().stream()
                .map(CustomerCartSyncItemRequest::getIdChiTietSanPham)
                .collect(Collectors.toList());

        List<ChiTietSanPham> dbItems = chiTietSanPhamRepository.findAllById(ids);
        Map<String, ChiTietSanPham> dbMap = dbItems.stream()
                .collect(Collectors.toMap(ChiTietSanPham::getId, Function.identity()));

        // Lấy thông tin đợt giảm giá đang active
        List<ChiTietDotGiamGia> relations = chiTietDotGiamGiaRepository.findAllByChiTietSanPhamIdIn(ids);
        long now = System.currentTimeMillis();
        Map<String, BigDecimal> discountMap = new java.util.HashMap<>();
        for (ChiTietDotGiamGia rel : relations) {
            if (rel.getChiTietSanPham() == null) continue;
            DotGiamGia d = rel.getDotGiamGia();
            if (d != null && d.getTrangThai() == TrangThai.DANG_HOAT_DONG
                    && d.getNgayBatDau() != null && d.getNgayKetThuc() != null
                    && d.getNgayBatDau() <= now && now <= d.getNgayKetThuc()
                    && d.getSoTienGiam() != null) {
                String variantId = rel.getChiTietSanPham().getId();
                discountMap.merge(variantId, d.getSoTienGiam(), (a, b) -> a.compareTo(b) >= 0 ? a : b);
            }
        }

        List<CustomerCartItemResponse> responses = new ArrayList<>();
        BigDecimal tongTien = BigDecimal.ZERO;

        for (CustomerCartSyncItemRequest item : request.getItems()) {
            ChiTietSanPham ctsp = dbMap.get(item.getIdChiTietSanPham());
            if (ctsp == null) {
                responses.add(CustomerCartItemResponse.builder()
                        .idChiTietSanPham(item.getIdChiTietSanPham())
                        .isAvailable(false)
                        .message("Sản phẩm không tồn tại")
                        .build());
                continue;
            }

            Integer requestedQty = item.getSoLuong() == null ? 1 : item.getSoLuong();
            Integer stock = ctsp.getSoLuong();
            
            boolean isAvailable = true;
            String message = null;
            Integer finalQty = requestedQty;

            if (stock <= 0) {
                isAvailable = false;
                message = "Sản phẩm đã hết hàng";
                finalQty = 0;
            } else if (requestedQty > stock) {
                message = "Chỉ còn " + stock + " sản phẩm";
                finalQty = stock;
            }

            BigDecimal giaGoc = ctsp.getGiaBan() != null ? ctsp.getGiaBan() : BigDecimal.ZERO;
            BigDecimal discount = discountMap.getOrDefault(ctsp.getId(), BigDecimal.ZERO);
            BigDecimal giaBan = giaGoc.subtract(discount).max(BigDecimal.ZERO);
            tongTien = tongTien.add(giaBan.multiply(BigDecimal.valueOf(finalQty)));

            responses.add(CustomerCartItemResponse.builder()
                    .idChiTietSanPham(ctsp.getId())
                    .maChiTietSanPham(ctsp.getMaChiTietSanPham())
                    .tenSanPham(ctsp.getSanPham() != null ? ctsp.getSanPham().getTen() : "")
                    .hinhAnh(ctsp.getSanPham() != null ? ctsp.getSanPham().getHinhAnh() : "")
                    .tenMauSac(ctsp.getMauSac() != null ? ctsp.getMauSac().getTen() : "")
                    .tenKichThuoc(ctsp.getKichThuoc() != null ? ctsp.getKichThuoc().getTen() : "")
                    .giaBan(giaBan)
                    .giaGoc(discount.compareTo(BigDecimal.ZERO) > 0 ? giaGoc : null)
                    .soLuong(finalQty)
                    .soLuongTonKho(stock)
                    .isAvailable(isAvailable)
                    .message(message)
                    .build());
        }

        return new CustomerCartSyncResponse(responses, tongTien);
    }
}
