package com.example.be.core.customer.gioithieu.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.repository.SanPhamRepository;
import com.example.be.repository.ThuongHieuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(RoutesConstant.CUSTOMER_GIOI_THIEU)
@RequiredArgsConstructor
public class CustomerGioiThieuController {

    private final SanPhamRepository sanPhamRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final com.example.be.repository.NhanVienRepository nhanVienRepository;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAboutStats() {
        long totalProducts = sanPhamRepository.count();
        long totalBrands = thuongHieuRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", totalProducts > 0 ? totalProducts : 500);
        stats.put("totalBrands", totalBrands > 0 ? totalBrands : 12);
        stats.put("totalStores", 50);
        stats.put("satisfactionRate", "99.8%");

        return ResponseEntity.ok(ApiResponse.success(stats, "Lấy chỉ số thống kê thành công"));
    }

    @GetMapping("/team")
    public ResponseEntity<ApiResponse<java.util.List<Map<String, Object>>>> getLeadershipTeam() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 5, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "ngayTao"));
        var nhanViens = nhanVienRepository.findAll(pageable).getContent();

        String[] defaultRoles = {
            "Nhà Sáng Lập & Tổng Giám Đốc (CEO)",
            "Giám Đốc Điều Hành & Vận Hành (COO)",
            "Giám Đốc Công Nghệ (CTO)",
            "Giám Đốc Marketing & Phát Triển (CMO)",
            "Giám Đốc Trải Nghiệm Khách Hàng (CXO)"
        };

        String[] defaultQuotes = {
            "Mỗi bước chạy của khách hàng là động lực để AeroStride hoàn thiện và vươn xa.",
            "Tối ưu vận hành và trải nghiệm mua sắm hoàn hảo là kim chỉ nam trong mọi hành động.",
            "Công nghệ và AI sẽ định hình lại tương lai của ngành thời trang thể thao Việt Nam.",
            "Lan tỏa tinh thần thể thao và lối sống tích cực đến hàng triệu bạn trẻ.",
            "Sự hài lòng tuyệt đối của khách hàng là thước đo giá trị lớn nhất của chúng tôi."
        };

        String[] defaultImages = {
            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=600",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=600",
            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=600",
            "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=80&w=600",
            "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&q=80&w=600"
        };

        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (int i = 0; i < nhanViens.size(); i++) {
            var nv = nhanViens.get(i);
            Map<String, Object> map = new HashMap<>();
            map.put("id", nv.getId());
            map.put("name", nv.getTen() != null ? nv.getTen() : "Quản lý " + (i + 1));
            
            String roleName = nv.getPhanQuyen() != null && nv.getPhanQuyen().getTen() != null 
                    ? nv.getPhanQuyen().getTen() 
                    : defaultRoles[i % defaultRoles.length];
            map.put("role", roleName);

            String img = nv.getHinhAnh();
            if (img == null || img.isBlank()) {
                img = defaultImages[i % defaultImages.length];
            }
            map.put("image", img);
            map.put("quote", defaultQuotes[i % defaultQuotes.length]);
            map.put("email", nv.getEmail());
            map.put("sdt", nv.getSdt());
            result.add(map);
        }

        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách ban lãnh đạo thành công"));
    }

    @GetMapping("/brands")
    public ResponseEntity<ApiResponse<java.util.List<Map<String, Object>>>> getBrandList() {
        var brands = thuongHieuRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "ten"));

        Map<String, String> defaultBrandImages = new HashMap<>();
        defaultBrandImages.put("NIKE", "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&q=80&w=600");
        defaultBrandImages.put("ADIDAS", "https://images.unsplash.com/photo-1587563871167-1ee9c731aefb?auto=format&fit=crop&q=80&w=600");
        defaultBrandImages.put("PUMA", "https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&q=80&w=600");
        defaultBrandImages.put("NEW BALANCE", "https://images.unsplash.com/photo-1539185441755-769473a23570?auto=format&fit=crop&q=80&w=600");
        defaultBrandImages.put("CONVERSE", "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&q=80&w=600");
        defaultBrandImages.put("KAMITO", "https://images.unsplash.com/photo-1552346154-21d32810aba3?auto=format&fit=crop&q=80&w=600");
        defaultBrandImages.put("MIZUNO", "https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?auto=format&fit=crop&q=80&w=600");
        defaultBrandImages.put("ASICS", "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&q=80&w=600");
        defaultBrandImages.put("MOLTEN", "https://images.unsplash.com/photo-1519766304817-4f37bda74a29?auto=format&fit=crop&q=80&w=600");
        defaultBrandImages.put("DONG LUC", "https://images.unsplash.com/photo-1579338559194-a162d19bf842?auto=format&fit=crop&q=80&w=600");
        defaultBrandImages.put("ĐỘNG LỰC", "https://images.unsplash.com/photo-1579338559194-a162d19bf842?auto=format&fit=crop&q=80&w=600");

        Map<String, String> defaultBrandLogos = new HashMap<>();
        defaultBrandLogos.put("NIKE", "https://upload.wikimedia.org/wikipedia/commons/a/a6/Logo_NIKE.svg");
        defaultBrandLogos.put("ADIDAS", "https://upload.wikimedia.org/wikipedia/commons/2/20/Adidas_Logo.svg");
        defaultBrandLogos.put("PUMA", "https://upload.wikimedia.org/wikipedia/en/d/dc/Puma_complete_logo.svg");
        defaultBrandLogos.put("NEW BALANCE", "https://upload.wikimedia.org/wikipedia/commons/e/ea/New_Balance_logo.svg");
        defaultBrandLogos.put("CONVERSE", "https://upload.wikimedia.org/wikipedia/commons/3/30/Converse_logo.svg");
        defaultBrandLogos.put("MIZUNO", "https://upload.wikimedia.org/wikipedia/commons/c/c9/Mizuno_logo.svg");
        defaultBrandLogos.put("ASICS", "https://upload.wikimedia.org/wikipedia/commons/b/b1/Asics_Logo.svg");
        defaultBrandLogos.put("KAMITO", "https://kamito.vn/images/logo.png");
        defaultBrandLogos.put("MOLTEN", "https://upload.wikimedia.org/wikipedia/commons/8/87/Molten_Corporation_logo.svg");
        defaultBrandLogos.put("DONG LUC", "https://dongluc.vn/images/logo.png");
        defaultBrandLogos.put("ĐỘNG LỰC", "https://dongluc.vn/images/logo.png");

        String genericShoeImg = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&q=80&w=600";
        String genericLogo = "https://upload.wikimedia.org/wikipedia/commons/a/a6/Logo_NIKE.svg";

        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (var b : brands) {
            if (Boolean.TRUE.equals(b.getXoaMem())) continue;

            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("name", b.getTen());
            map.put("ma", b.getMa());

            String upperName = b.getTen() != null ? b.getTen().toUpperCase().trim() : "";
            String imgUrl = defaultBrandImages.getOrDefault(upperName, genericShoeImg);
            String logoUrl = defaultBrandLogos.getOrDefault(upperName, genericLogo);

            map.put("image", imgUrl);
            map.put("logo", logoUrl);

            result.add(map);
        }

        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách thương hiệu thành công"));
    }
}
