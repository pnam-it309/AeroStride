package com.example.be.core.admin.chat.config;

import com.example.be.core.admin.sanpham.model.response.ProductVariantResponse;
import com.example.be.core.admin.sanpham.service.AdminSanPhamService;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.constants.AiChatPrompts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class AiToolsConfig {

    private final AdminSanPhamService sanPhamService;

    public AiToolsConfig(AdminSanPhamService sanPhamService) {
        this.sanPhamService = sanPhamService;
    }

    public record ProductSearchRequest(String keyword, String brand, BigDecimal maxPrice) {}

    @Bean
    @Description("Tìm kiếm các biến thể sản phẩm (giày) trong kho của AeroStride. Trả về thông tin biến thể cụ thể bao gồm ID, size, màu, giá. Cung cấp keyword (như tên giày, size, màu) và maxPrice nếu khách hàng muốn tìm giày dưới một mức giá nào đó.")
    public Function<ProductSearchRequest, String> searchProducts() {
        return request -> {
            log.info("AI gọi hàm searchProducts: {}", request);
            
            // Lấy toàn bộ biến thể đang hoạt động
            List<ProductVariantResponse> allVariants = sanPhamService.getAllVariants().stream()
                    .filter(v -> v.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                    .collect(Collectors.toList());

            // Lọc theo các điều kiện do AI truyền vào
            List<ProductVariantResponse> filtered = allVariants.stream().filter(v -> {
                boolean match = true;
                
                // Lọc theo keyword (tìm trong tên SP, màu sắc, size, thương hiệu)
                if (request.keyword() != null && !request.keyword().isBlank()) {
                    String kw = request.keyword().toLowerCase();
                    boolean kwMatch = false;
                    if (v.getTenSanPham() != null && v.getTenSanPham().toLowerCase().contains(kw)) kwMatch = true;
                    if (v.getTenMauSac() != null && v.getTenMauSac().toLowerCase().contains(kw)) kwMatch = true;
                    if (v.getGiaTriKichThuoc() != null && v.getGiaTriKichThuoc().toLowerCase().contains(kw)) kwMatch = true;
                    if (v.getTenThuongHieu() != null && v.getTenThuongHieu().toLowerCase().contains(kw)) kwMatch = true;
                    if (!kwMatch) match = false;
                }
                
                // Lọc theo brand
                if (request.brand() != null && !request.brand().isBlank()) {
                    if (v.getTenThuongHieu() == null || !v.getTenThuongHieu().toLowerCase().contains(request.brand().toLowerCase())) {
                        match = false;
                    }
                }
                
                // Lọc theo giá tối đa
                if (request.maxPrice() != null) {
                    if (v.getGiaBan() != null && v.getGiaBan().compareTo(request.maxPrice()) > 0) {
                        match = false;
                    }
                }
                
                return match;
            }).limit(30).collect(Collectors.toList()); // Trả về tối đa 30 sản phẩm để đảm bảo không vượt quá token limit

            if (filtered.isEmpty()) {
                return "Không tìm thấy sản phẩm nào khớp với yêu cầu tìm kiếm trong hệ thống.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Tìm thấy ").append(filtered.size()).append(" sản phẩm:\n");
            for (ProductVariantResponse v : filtered) {
                sb.append(String.format("{\n  \"idSanPham\": \"%s\",\n  \"tenSanPham\": \"%s\",\n  \"giaBan\": %s,\n  \"tenThuongHieu\": \"%s\",\n  \"tenMauSac\": \"%s\",\n  \"giaTriKichThuoc\": \"%s\",\n  \"hinhAnh\": \"%s\",\n  \"phanTramGiam\": %s,\n  \"soLuong\": %s\n}\n", 
                    v.getIdSanPham(), 
                    v.getTenSanPham() != null ? v.getTenSanPham() : "", 
                    v.getGiaBan(), 
                    v.getTenThuongHieu() != null ? v.getTenThuongHieu() : "",
                    v.getTenMauSac() != null ? v.getTenMauSac() : "",
                    v.getGiaTriKichThuoc() != null ? v.getGiaTriKichThuoc() : "",
                    v.getHinhAnh() != null ? v.getHinhAnh() : "",
                    v.getPhanTramGiam() != null ? v.getPhanTramGiam() : 0,
                    v.getSoLuong()));
            }
            return sb.toString();
        };
    }

    public record PolicyRequest() {}

    @Bean
    @Description("Truy xuất các chính sách của cửa hàng AeroStride bao gồm: chính sách giao hàng (shipping), bảo hành (warranty), đổi trả (return), địa chỉ showroom, giờ mở cửa, số điện thoại hotline, thông tin thanh toán, và hướng dẫn chọn size. HÃY GỌI HÀM NÀY NẾU KHÁCH HỎI VỀ CÁC VẤN ĐỀ TRÊN.")
    public Function<PolicyRequest, String> getStorePolicies() {
        return request -> {
            log.info("AI gọi hàm getStorePolicies để lấy thông tin chính sách.");
            return AiChatPrompts.STORE_POLICIES_CONTEXT;
        };
    }
}
