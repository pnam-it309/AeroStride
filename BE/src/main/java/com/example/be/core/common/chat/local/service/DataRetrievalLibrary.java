package com.example.be.core.common.chat.local.service;

import com.example.be.entity.SanPham;
import com.example.be.entity.ChiTietSanPham;
import com.example.be.entity.DotGiamGia;
import com.example.be.entity.PhieuGiamGia;
import com.example.be.entity.DeGiay;
import com.example.be.entity.ChatLieu;
import com.example.be.entity.CoGiay;
import com.example.be.entity.ThuongHieu;
import com.example.be.entity.XuatXu;
import com.example.be.entity.KichThuoc;
import com.example.be.entity.MauSac;
import com.example.be.entity.ChiTietDotGiamGia;
import com.example.be.infrastructure.constants.TrangThai;

import com.example.be.repository.SanPhamRepository;
import com.example.be.repository.DotGiamGiaRepository;
import com.example.be.repository.PhieuGiamGiaRepository;
import com.example.be.repository.DeGiayRepository;
import com.example.be.repository.ChatLieuRepository;
import com.example.be.repository.CoGiayRepository;
import com.example.be.repository.ThuongHieuRepository;
import com.example.be.repository.XuatXuRepository;
import com.example.be.repository.KichThuocRepository;
import com.example.be.repository.MauSacRepository;
import com.example.be.repository.ChiTietDotGiamGiaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataRetrievalLibrary {
    private final SanPhamRepository sanPhamRepository;
    private final DotGiamGiaRepository dotGiamGiaRepository;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final DeGiayRepository deGiayRepository;
    private final ChatLieuRepository chatLieuRepository;
    private final CoGiayRepository coGiayRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final XuatXuRepository xuatXuRepository;
    private final KichThuocRepository kichThuocRepository;
    private final MauSacRepository mauSacRepository;
    private final ChiTietDotGiamGiaRepository chiTietDotGiamGiaRepository;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    // Cache structures for all formatted markdown queries
    private final java.util.Map<String, CachedData> cache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 30000; // 30 seconds

    private static class CachedData {
        final String content;
        final long timestamp;
        CachedData(String content, long timestamp) {
            this.content = content;
            this.timestamp = timestamp;
        }
    }

    private String getOrCompute(String key, java.util.function.Supplier<String> computer) {
        long now = System.currentTimeMillis();
        CachedData cached = cache.get(key);
        if (cached == null || (now - cached.timestamp > CACHE_TTL_MS)) {
            String val = computer.get();
            cache.put(key, new CachedData(val, now));
            return val;
        }
        return cached.content;
    }

    // Cache for all products
    private volatile List<SanPham> allProductsCache = null;
    private volatile long allProductsCacheExpires = 0;

    private List<SanPham> getAllProductsCached() {
        long now = System.currentTimeMillis();
        if (allProductsCache == null || now > allProductsCacheExpires) {
            synchronized (this) {
                if (allProductsCache == null || now > allProductsCacheExpires) {
                    allProductsCache = transactionTemplate.execute(status -> {
                        List<SanPham> list = sanPhamRepository.findAll();
                        for (SanPham sp : list) {
                            if (sp.getChiTietSanPhams() != null) {
                                sp.getChiTietSanPhams().size(); // Force lazy loading initialization
                            }
                        }
                        return list;
                    });
                    allProductsCacheExpires = now + CACHE_TTL_MS;
                }
            }
        }
        return allProductsCache;
    }

    public List<SanPham> getTopProducts(int limit) {
        return getAllProductsCached().stream().limit(limit).collect(Collectors.toList());
    }

    public String getTopProductsInfo(int limit) {
        return getOrCompute("top_products_info_" + limit, () -> {
            List<SanPham> products = getTopProducts(limit);
            if (products.isEmpty()) {
                return "Hiện tại AeroStride chưa có sản phẩm nào mới đăng bán.";
            }
            
            StringBuilder sb = new StringBuilder();
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            
            for (SanPham p : products) {
                sb.append("👟 **").append(p.getTen()).append("** (Mã: ").append(p.getMa()).append(")\n");
                if (p.getMoTaChiTiet() != null && !p.getMoTaChiTiet().isBlank()) {
                    sb.append("   *Kiểu dáng:* ").append(p.getMoTaChiTiet()).append("\n");
                }
                if (p.getChiTietSanPhams() != null && !p.getChiTietSanPhams().isEmpty()) {
                    BigDecimal minPrice = p.getChiTietSanPhams().stream()
                            .map(ChiTietSanPham::getGiaBan)
                            .filter(Objects::nonNull)
                            .min(BigDecimal::compareTo)
                            .orElse(null);
                    if (minPrice != null) {
                        sb.append("   *Giá chỉ từ:* ").append(formatter.format(minPrice)).append("\n");
                    } else {
                        sb.append("   *Giá chỉ từ:* Liên hệ tư vấn\n");
                    }
                } else {
                    sb.append("   *Giá chỉ từ:* 1.250.000 ₫ (Ưu đãi đặc biệt)\n");
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        });
    }

    public List<SanPham> searchProducts(String keyword) {
        return getAllProductsCached().stream()
                .filter(p -> p.getTen().toLowerCase().contains(keyword.toLowerCase()))
                .limit(5)
                .collect(Collectors.toList());
    }

    public String getActiveDiscountsInfo() {
        return getOrCompute("active_discounts_info", () -> {
            long now = System.currentTimeMillis();
            List<DotGiamGia> campaigns = dotGiamGiaRepository.findAll().stream()
                    .filter(d -> d.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                    .filter(d -> d.getNgayBatDau() == null || d.getNgayBatDau() <= now)
                    .filter(d -> d.getNgayKetThuc() == null || d.getNgayKetThuc() >= now)
                    .filter(d -> !Boolean.TRUE.equals(d.getXoaMem()))
                    .collect(Collectors.toList());

            if (campaigns.isEmpty()) {
                return "Dạ hiện tại bên em chưa diễn ra đợt giảm giá lớn nào. Tuy nhiên, shop luôn có các ưu đãi mặc định siêu hấp dẫn cho dòng sản phẩm bán chạy, anh/chị tham khảo nhé! 💖";
            }

            StringBuilder sb = new StringBuilder("AeroStride đang có các đợt giảm giá và chương trình khuyến mãi vô cùng hấp dẫn đang diễn ra:\n\n");
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

            for (DotGiamGia d : campaigns) {
                sb.append("🎉 **").append(d.getTen()).append("** (Mã: ").append(d.getMa()).append(")\n");
                if (d.getMoTa() != null && !d.getMoTa().isBlank()) {
                    sb.append("   - *Nội dung:* ").append(d.getMoTa()).append("\n");
                }
                if (d.getSoTienGiam() != null) {
                    if ("PHAN_TRAM".equalsIgnoreCase(d.getLoaiGiamGia()) || (d.getSoTienGiam().compareTo(new BigDecimal(100)) <= 0 && d.getSoTienGiam().scale() <= 2)) {
                        sb.append("   - *Mức giảm:* ").append(d.getSoTienGiam().stripTrailingZeros().toPlainString()).append("%\n");
                    } else {
                        sb.append("   - *Mức giảm:* ").append(currencyFormatter.format(d.getSoTienGiam())).append("\n");
                    }
                }
                if (d.getDieuKienGiamGia() != null && d.getDieuKienGiamGia().compareTo(BigDecimal.ZERO) > 0) {
                    sb.append("   - *Áp dụng:* Đơn hàng từ ").append(currencyFormatter.format(d.getDieuKienGiamGia())).append("\n");
                }
                if (d.getNgayKetThuc() != null) {
                    sb.append("   - *Hạn áp dụng:* Đến hết ngày ").append(sdf.format(new java.util.Date(d.getNgayKetThuc()))).append("\n");
                }
                sb.append("\n");
            }
            sb.append("Để mua hàng với giá ưu đãi này, anh/chị cứ chọn mẫu và nhắn shop giữ hàng kèm áp mã giảm ngay lập tức nhé! 🥰");
            return sb.toString().trim();
        });
    }

    public String getActiveCouponsInfo() {
        return getOrCompute("active_coupons_info", () -> {
            long now = System.currentTimeMillis();
            List<PhieuGiamGia> vouchers = phieuGiamGiaRepository.findAll().stream()
                    .filter(p -> p.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                    .filter(p -> p.getNgayBatDau() == null || p.getNgayBatDau() <= now)
                    .filter(p -> p.getNgayKetThuc() == null || p.getNgayKetThuc() >= now)
                    .filter(p -> p.getSoLuong() == null || p.getSoLuong() > 0)
                    .collect(Collectors.toList());

            if (vouchers.isEmpty()) {
                return "Dạ hiện tại hệ thống chưa phát hành phiếu giảm giá (Voucher) công khai mới. Tuy nhiên, shop luôn có ưu đãi mã **AERO10** giảm 10% cho khách hàng mới đấy ạ! 😉";
            }

            StringBuilder sb = new StringBuilder("Hiện tại AeroStride đang có các mã phiếu giảm giá (Voucher) đặc biệt sau dành cho anh/chị:\n\n");
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

            for (PhieuGiamGia p : vouchers) {
                sb.append("🎫 **").append(p.getTen()).append("** (Mã nhập: **").append(p.getMa()).append("**)\n");
                sb.append("   - *Loại phiếu:* ").append("CA_NHAN".equals(p.getHinhThuc()) ? "Voucher Cá Nhân đặc biệt" : "Voucher Công Khai toàn sàn").append("\n");
                if (p.getPhanTramGiamGia() != null && p.getPhanTramGiamGia() > 0) {
                    sb.append("   - *Mức giảm:* Giảm ").append(p.getPhanTramGiamGia()).append("%");
                    if (p.getGiamToiDa() != null && p.getGiamToiDa().compareTo(BigDecimal.ZERO) > 0) {
                        sb.append(" (Tối đa ").append(currencyFormatter.format(p.getGiamToiDa())).append(")");
                    }
                    sb.append("\n");
                } else if (p.getSoTienGiam() != null) {
                    sb.append("   - *Mức giảm:* Giảm thẳng ").append(currencyFormatter.format(p.getSoTienGiam())).append("\n");
                }
                if (p.getDonHangToiThieu() != null && p.getDonHangToiThieu().compareTo(BigDecimal.ZERO) > 0) {
                    sb.append("   - *Điều kiện đơn hàng:* Từ ").append(currencyFormatter.format(p.getDonHangToiThieu())).append("\n");
                }
                if (p.getSoLuong() != null) {
                    sb.append("   - *Số lượng còn lại:* ").append(p.getSoLuong()).append(" lượt dùng\n");
                }
                if (p.getNgayKetThuc() != null) {
                    sb.append("   - *Hạn sử dụng:* Đến ngày ").append(sdf.format(new java.util.Date(p.getNgayKetThuc()))).append("\n");
                }
                if (p.getGhiChu() != null && !p.getGhiChu().isBlank()) {
                    sb.append("   - *Ghi chú:* ").append(p.getGhiChu()).append("\n");
                }
                sb.append("\n");
            }
            sb.append("Hãy lưu lại các mã giảm giá trên để áp dụng khi thanh toán đơn hàng nha anh/chị! 🛍️");
            return sb.toString().trim();
        });
    }

    public String getPromoProductsInfo() {
        return getOrCompute("promo_products_info", () -> {
            long now = System.currentTimeMillis();
            
            List<ChiTietDotGiamGia> activePromoDetails = chiTietDotGiamGiaRepository.findAll().stream()
                    .filter(ct -> ct.getDotGiamGia() != null 
                            && ct.getDotGiamGia().getTrangThai() == TrangThai.DANG_HOAT_DONG)
                    .filter(ct -> {
                        DotGiamGia d = ct.getDotGiamGia();
                        return (d.getNgayBatDau() == null || d.getNgayBatDau() <= now)
                                && (d.getNgayKetThuc() == null || d.getNgayKetThuc() >= now);
                    })
                    .filter(ct -> ct.getChiTietSanPham() != null && !Boolean.TRUE.equals(ct.getChiTietSanPham().getXoaMem()))
                    .filter(ct -> ct.getChiTietSanPham().getSanPham() != null && !Boolean.TRUE.equals(ct.getChiTietSanPham().getSanPham().getXoaMem()))
                    .collect(Collectors.toList());

            if (activePromoDetails.isEmpty()) {
                return "Dạ hiện tại chưa có sản phẩm cụ thể nào đang áp dụng chương trình giảm giá lớn. Tuy nhiên, shop luôn có ưu đãi mã **AERO10** giảm 10% cho đơn hàng đầu tiên đấy ạ! 👟";
            }

            java.util.Map<SanPham, List<ChiTietDotGiamGia>> promoByProduct = activePromoDetails.stream()
                    .collect(Collectors.groupingBy(ct -> ct.getChiTietSanPham().getSanPham()));

            StringBuilder sb = new StringBuilder("Dạ đây là danh sách những sản phẩm đang được KHUYẾN MÃI GIẢM GIÁ cực lớn tại AeroStride:\n\n");
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

            int count = 0;
            for (java.util.Map.Entry<SanPham, List<ChiTietDotGiamGia>> entry : promoByProduct.entrySet()) {
                if (count >= 5) break;
                SanPham sp = entry.getKey();
                List<ChiTietDotGiamGia> cts = entry.getValue();

                BigDecimal maxDiscountVal = BigDecimal.ZERO;
                String discountType = "PHAN_TRAM";
                List<DotGiamGia> validCampaigns = new java.util.ArrayList<>();
                for (ChiTietDotGiamGia ct : cts) {
                    DotGiamGia d = ct.getDotGiamGia();
                    if (d != null && d.getSoTienGiam() != null) {
                        validCampaigns.add(d);
                        discountType = d.getLoaiGiamGia(); // simplified, usually all are percentages based on requirement
                    }
                }
                
                if (!validCampaigns.isEmpty()) {
                    validCampaigns.sort((c1, c2) -> c2.getSoTienGiam().compareTo(c1.getSoTienGiam()));
                    if (validCampaigns.size() == 1) {
                        maxDiscountVal = validCampaigns.get(0).getSoTienGiam();
                    } else {
                        DotGiamGia d1 = validCampaigns.get(0);
                        DotGiamGia d2 = validCampaigns.get(1);
                        BigDecimal m1 = d1.getSoTienGiam();
                        BigDecimal m2 = d2.getSoTienGiam();
                        long overlapStart = Math.max(d1.getNgayBatDau() != null ? d1.getNgayBatDau() : 0, d2.getNgayBatDau() != null ? d2.getNgayBatDau() : 0);
                        long overlapEnd = Math.min(d1.getNgayKetThuc() != null ? d1.getNgayKetThuc() : Long.MAX_VALUE, d2.getNgayKetThuc() != null ? d2.getNgayKetThuc() : Long.MAX_VALUE);
                        long overlapDays = 0;
                        if (overlapEnd >= overlapStart && overlapStart > 0 && overlapEnd < Long.MAX_VALUE) {
                            overlapDays = (overlapEnd - overlapStart) / (1000L * 60 * 60 * 24);
                        }
                        if (overlapDays < 3) {
                            maxDiscountVal = m1;
                        } else {
                            maxDiscountVal = m1.add(m2).divide(new BigDecimal("2"), 0, java.math.RoundingMode.HALF_UP);
                        }
                    }
                }

                BigDecimal minOriginalPrice = sp.getChiTietSanPhams().stream()
                        .map(ChiTietSanPham::getGiaBan)
                        .filter(Objects::nonNull)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

                BigDecimal minDiscountedPrice = BigDecimal.ZERO;
                if (minOriginalPrice.compareTo(BigDecimal.ZERO) > 0) {
                    if ("PHAN_TRAM".equalsIgnoreCase(discountType) || (maxDiscountVal.compareTo(new BigDecimal(100)) <= 0)) {
                        BigDecimal factor = BigDecimal.ONE.subtract(maxDiscountVal.divide(new BigDecimal(100), 4, java.math.RoundingMode.HALF_UP));
                        minDiscountedPrice = minOriginalPrice.multiply(factor);
                    } else {
                        minDiscountedPrice = minOriginalPrice.subtract(maxDiscountVal);
                        if (minDiscountedPrice.compareTo(BigDecimal.ZERO) < 0) {
                            minDiscountedPrice = BigDecimal.ZERO;
                        }
                    }
                }

                sb.append("👟 **").append(sp.getTen()).append("** (Mã: ").append(sp.getMa()).append(")\n");
                if (sp.getMoTaChiTiet() != null && !sp.getMoTaChiTiet().isBlank()) {
                    sb.append("   - *Kiểu dáng:* ").append(sp.getMoTaChiTiet()).append("\n");
                }
                if (sp.getThuongHieu() != null) {
                    sb.append("   - *Thương hiệu:* ").append(sp.getThuongHieu().getTen()).append("\n");
                }
                
                String discountStr = "";
                if ("PHAN_TRAM".equalsIgnoreCase(discountType) || (maxDiscountVal.compareTo(new BigDecimal(100)) <= 0)) {
                    discountStr = maxDiscountVal.stripTrailingZeros().toPlainString() + "% OFF";
                } else {
                    discountStr = "-" + currencyFormatter.format(maxDiscountVal);
                }
                sb.append("   - *Ưu đãi:* 🔥 Giảm cực sốc **").append(discountStr).append("**\n");
                sb.append("   - *Giá cũ:* ~~").append(currencyFormatter.format(minOriginalPrice)).append("~~\n");
                sb.append("   - *Giá ưu đãi:* **").append(currencyFormatter.format(minDiscountedPrice)).append("**\n\n");
                count++;
            }

            sb.append("Anh/chị có ưng ý sản phẩm khuyến mãi nào ở trên không ạ? Nhắn shop để shop hướng dẫn đặt hàng nhận ưu đãi ngay nhé! 💕");
            return sb.toString().trim();
        });
    }

    public String getShoeSolesInfo() {
        return getOrCompute("shoe_soles_info", () -> {
            List<DeGiay> list = deGiayRepository.findAll().stream()
                    .filter(item -> !Boolean.TRUE.equals(item.getXoaMem()))
                    .filter(item -> item.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                    .collect(Collectors.toList());
            if (list.isEmpty()) {
                return "Dạ hiện tại hệ thống chưa cập nhật thông tin về các loại đế giày ạ.";
            }
            StringBuilder sb = new StringBuilder("AeroStride sử dụng các dòng công nghệ đế giày cao cấp, siêu nhẹ và êm chân sau:\n");
            for (DeGiay item : list) {
                sb.append(" - 👟 **").append(item.getTen()).append("** (Mã: ").append(item.getMa()).append(")");
                if (item.getMoTa() != null && !item.getMoTa().isBlank()) {
                    sb.append(": ").append(item.getMoTa());
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        });
    }

    public String getMaterialsInfo() {
        return getOrCompute("materials_info", () -> {
            List<ChatLieu> list = chatLieuRepository.findAll().stream()
                    .filter(item -> !Boolean.TRUE.equals(item.getXoaMem()))
                    .filter(item -> item.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                    .collect(Collectors.toList());
            if (list.isEmpty()) {
                return "Dạ hiện tại hệ thống chưa cập nhật thông tin về các loại chất liệu giày ạ.";
            }
            StringBuilder sb = new StringBuilder("Giày của AeroStride được chế tác từ các chất liệu cao cấp, thoáng khí và bền bỉ:\n");
            for (ChatLieu item : list) {
                sb.append(" - 🌱 **").append(item.getTen()).append("** (Mã: ").append(item.getMa()).append(")");
                if (item.getMoTa() != null && !item.getMoTa().isBlank()) {
                    sb.append(": ").append(item.getMoTa());
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        });
    }

    public String getCollarsInfo() {
        return getOrCompute("collars_info", () -> {
            List<CoGiay> list = coGiayRepository.findAll().stream()
                    .filter(item -> !Boolean.TRUE.equals(item.getXoaMem()))
                    .filter(item -> item.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                    .collect(Collectors.toList());
            if (list.isEmpty()) {
                return "Dạ hiện tại hệ thống chưa cập nhật thông tin về các kiểu cổ giày ạ.";
            }
            StringBuilder sb = new StringBuilder("AeroStride cung cấp nhiều kiểu thiết kế cổ giày ôm chân, thời trang:\n");
            for (CoGiay item : list) {
                sb.append(" - 👟 **").append(item.getTen()).append("** (Mã: ").append(item.getMa()).append(")");
                if (item.getMoTa() != null && !item.getMoTa().isBlank()) {
                    sb.append(": ").append(item.getMoTa());
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        });
    }

    public String getBrandsInfo() {
        return getOrCompute("brands_info", () -> {
            List<ThuongHieu> list = thuongHieuRepository.findAll().stream()
                    .filter(item -> !Boolean.TRUE.equals(item.getXoaMem()))
                    .filter(item -> item.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                    .collect(Collectors.toList());
            if (list.isEmpty()) {
                return "Dạ hiện tại bên em chưa có danh mục thương hiệu cụ thể ạ.";
            }
            StringBuilder sb = new StringBuilder("Các thương hiệu giày AeroStride phân phối chính hãng bao gồm:\n");
            for (ThuongHieu item : list) {
                sb.append(" - ⭐ **").append(item.getTen()).append("** (Mã: ").append(item.getMa()).append(")");
                if (item.getMoTa() != null && !item.getMoTa().isBlank()) {
                    sb.append(": ").append(item.getMoTa());
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        });
    }

    public String getOriginsInfo() {
        return getOrCompute("origins_info", () -> {
            List<XuatXu> list = xuatXuRepository.findAll().stream()
                    .filter(item -> !Boolean.TRUE.equals(item.getXoaMem()))
                    .filter(item -> item.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                    .collect(Collectors.toList());
            if (list.isEmpty()) {
                return "Dạ hiện tại chưa có thông tin xuất xứ sản phẩm ạ.";
            }
            StringBuilder sb = new StringBuilder("Các sản phẩm của AeroStride được nhập khẩu và phân phối từ các xuất xứ uy tín:\n");
            for (XuatXu item : list) {
                sb.append(" - 🌍 **").append(item.getTen()).append("** (Mã: ").append(item.getMa()).append(")");
                if (item.getMoTa() != null && !item.getMoTa().isBlank()) {
                    sb.append(": ").append(item.getMoTa());
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        });
    }

    public String getSizesInfo() {
        return getOrCompute("sizes_info", () -> {
            List<KichThuoc> list = kichThuocRepository.findAll().stream()
                    .filter(item -> !Boolean.TRUE.equals(item.getXoaMem()))
                    .filter(item -> item.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                    .collect(Collectors.toList());
            if (list.isEmpty()) {
                return "Dạ hiện tại hệ thống chưa cập nhật danh sách size giày ạ.";
            }
            StringBuilder sb = new StringBuilder("Bảng size giày đang sẵn có tại các showroom AeroStride:\n");
            for (KichThuoc item : list) {
                sb.append(" - 👟 **Size ").append(item.getTen()).append("** (Mã: ").append(item.getMa()).append(")");
                if (item.getGiaTriKichThuoc() != null) {
                    sb.append(" - Chiều dài chân phù hợp: ").append(item.getGiaTriKichThuoc()).append(" cm");
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        });
    }

    public String getColorsInfo() {
        return getOrCompute("colors_info", () -> {
            List<MauSac> list = mauSacRepository.findAll().stream()
                    .filter(item -> !Boolean.TRUE.equals(item.getXoaMem()))
                    .filter(item -> item.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                    .collect(Collectors.toList());
            if (list.isEmpty()) {
                return "Dạ hiện tại chưa có thông tin bảng màu sắc giày ạ.";
            }
            StringBuilder sb = new StringBuilder("Các phối màu sắc thời thượng đang sẵn có tại AeroStride:\n");
            for (MauSac item : list) {
                sb.append(" - 🎨 **").append(item.getTen()).append("** (Mã: ").append(item.getMa()).append(")");
                if (item.getMaMauHex() != null && !item.getMaMauHex().isBlank()) {
                    sb.append(" - Mã màu Hex: ").append(item.getMaMauHex());
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        });
    }
}


