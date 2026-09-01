package com.example.be.core.customer.tintuc.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.customer.tintuc.model.request.CustomerBinhLuanRequest;
import com.example.be.core.customer.tintuc.model.response.CustomerTinTucResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(RoutesConstant.CUSTOMER_TIN_TUC)
@RequiredArgsConstructor
public class CustomerTinTucController {

    private final ConcurrentHashMap<String, CustomerTinTucResponse> articles = new ConcurrentHashMap<>();

    @PostConstruct
    public void initArticles() {
        articles.put("1", CustomerTinTucResponse.builder()
                .id("1")
                .title("Xu hướng giày thể thao 2026: Lên ngôi của các tông màu Retro & Công nghệ Đệm khí")
                .category("Giới giày")
                .image("https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&q=80&w=800")
                .date("15 Thg 07, 2026")
                .author("Nguyễn Huy Đức")
                .likes(128)
                .excerpt("Năm 2026 chứng kiến sự trở lại mạnh mẽ của các thiết kế thập niên 90 kết hợp với phối màu cổ điển cá tính.")
                .content("""
                        <p>Thời trang thể thao năm 2026 không chỉ xoay quanh sự tối giản mà đang trỗi dậy mạnh mẽ xu hướng <strong>Retro Vintage</strong> kết hợp công nghệ hiện đại. Các mẫu thiết kế huyền thoại từ thập niên 90 đang được làm mới với phối màu bứt phá.</p>
                        <h3>1. Sự thống trị của phối màu Pastel và Metallic</h3>
                        <p>Những gam màu nhẹ nhàng như xanh sage, hồng phấn, kết hợp chi tiết xám bạc kim loại mang lại nét tương phản đầy thu hút. Không chỉ xuất hiện trên sàn diễn thời trang, các phối màu này đã phủ sóng toàn bộ các giải chạy lớn.</p>
                        <h3>2. Đế giày dạng Bọt khí siêu nhẹ (Ultra Foam)</h3>
                        <p>Công nghệ đế bọt mật độ kép mới giúp hoàn trả lực tốt hơn 35% so với thế hệ tiền nhiệm, giảm tối đa chấn thương cho cổ chân khi vận động cường độ cao.</p>
                        <p>Hãy đến ngay cửa hàng AeroStride gần nhất để trải nghiệm bộ sưu tập retro mới nhất mùa hè này!</p>
                        """)
                .comments(new ArrayList<>(List.of(
                        new CustomerTinTucResponse.BinhLuanResponse("Minh Anh", "Phối màu retro đẹp xuất sắc, vừa làm đôi Pegasus màu này tuần trước!", "16/07/2026")
                )))
                .build());

        articles.put("2", CustomerTinTucResponse.builder()
                .id("2")
                .title("Khai trương cửa hàng flagship thứ 50 của AeroStride tại Đà Nẵng")
                .category("Sự kiện")
                .image("https://images.unsplash.com/photo-1497215842964-222b430dc094?auto=format&fit=crop&q=80&w=800")
                .date("02 Thg 07, 2026")
                .author("Hoàng Phương Nam")
                .likes(254)
                .excerpt("Sự kiện đánh dấu cột mốc quan trọng trong quá trình phủ sóng toàn quốc với không gian trải nghiệm AI độc đáo.")
                .content("""
                        <p>AeroStride chính thức khai trương Cửa hàng Flagship rộng hơn 500m² tại trung tâm TP. Đà Nẵng. Đây là chi nhánh đầu tiên tại miền Trung tích hợp công nghệ AI quét bàn chân 3D tự động khuyến nghị mẫu giày vừa vặn tuyệt đối.</p>
                        <p>Trong tuần lễ khai trương, hàng trăm quà tặng độc quyền và ưu đãi giảm 30% toàn bộ sản phẩm đang chờ đón các runner và tín đồ thời trang.</p>
                        """)
                .comments(new ArrayList<>(List.of(
                        new CustomerTinTucResponse.BinhLuanResponse("Quốc Bảo", "Quá đỉnh! Sáng nay vừa qua trải nghiệm quét chân 3D cực chuẩn.", "03/07/2026")
                )))
                .build());

        articles.put("3", CustomerTinTucResponse.builder()
                .id("3")
                .title("Black Friday Sớm: Giảm giá đến 50% toàn bộ siêu phẩm chạy bộ")
                .category("Khuyến mãi")
                .image("https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?auto=format&fit=crop&q=80&w=800")
                .date("28 Thg 06, 2026")
                .author("Phí Thu Trang")
                .likes(310)
                .excerpt("Cơ hội duy nhất trong năm để sở hữu những đôi giày chính hãng Nike, Adidas, Puma với mức giá không thể hời hơn.")
                .content("""
                        <p>Chương trình siêu ưu đãi <strong>Black Friday Sớm</strong> chính thức bắt đầu từ ngày 28/06 đến 05/07. Toàn bộ các dòng giày Nike Zoom, Ultraboost, Puma Nitro đều được giảm giá từ 20% đến 50% tại hệ thống AeroStride.</p>
                        <p>Đặc biệt: Nhập mã <code>AEROFRIDAY</code> giảm thêm 100k cho đơn hàng từ 1.500.000đ khi thanh toán online!</p>
                        """)
                .comments(new ArrayList<>())
                .build());

        articles.put("4", CustomerTinTucResponse.builder()
                .id("4")
                .title("Bí kíp bảo quản và vệ sinh giày thể thao đúng cách trong mùa mưa")
                .category("Bảo quản")
                .image("https://images.unsplash.com/photo-1512374382149-233c42b6a83b?auto=format&fit=crop&q=80&w=800")
                .date("20 Thg 06, 2026")
                .author("Lê Thị Thu Huyền")
                .likes(95)
                .excerpt("Đừng để những cơn mưa bất chợt làm hỏng đôi giày đắt tiền của bạn. Hãy bỏ túi ngay những mẹo đơn giản mà hiệu quả này.")
                .content("""
                        <p>Mùa mưa ẩm ướt là kẻ thù số một của chất liệu da lộn và vải dệt flyknit. Dưới đây là 3 bước quan trọng giúp bảo vệ đôi giày yêu thích của bạn:</p>
                        <ul>
                            <li><strong>Sử dụng bình xịt chống nước Nano:</strong> Phủ một lớp xịt bảo vệ trước khi ra ngoài giúp kháng nước và vết bẩn tới 85%.</li>
                            <li><strong>Không sấy ở nhiệt độ cao:</strong> Nhiệt độ cao làm biến dạng keo dán đế. Hãy nhét giấy báo hút ẩm và phơi ở nơi thoáng gió.</li>
                            <li><strong>Khử mùi với túi than hoạt tính:</strong> Đặt túi hút ẩm than hoạt tính vào trong giày qua đêm để loại bỏ vi khuẩn gây mùi.</li>
                        </ul>
                        """)
                .comments(new ArrayList<>())
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerTinTucResponse>>> getArticles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {

        List<CustomerTinTucResponse> result = articles.values().stream()
                .filter(a -> category == null || category.equalsIgnoreCase("Tất cả") || a.getCategory().equalsIgnoreCase(category))
                .filter(a -> keyword == null || keyword.isBlank() ||
                             a.getTitle().toLowerCase().contains(keyword.toLowerCase()) ||
                             a.getExcerpt().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerTinTucResponse>> getArticleById(@PathVariable String id) {
        CustomerTinTucResponse article = articles.get(id);
        if (article == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(404, "Không tìm thấy bài viết"));
        }
        return ResponseEntity.ok(ApiResponse.success(article));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Integer>> likeArticle(@PathVariable String id) {
        CustomerTinTucResponse article = articles.get(id);
        if (article == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Bài viết không tồn tại"));
        }
        article.setLikes(article.getLikes() + 1);
        return ResponseEntity.ok(ApiResponse.success(article.getLikes(), "Thả tim bài viết thành công!"));
    }

    @PostMapping("/{id}/comment")
    public ResponseEntity<ApiResponse<CustomerTinTucResponse.BinhLuanResponse>> commentArticle(
            @PathVariable String id,
            @Valid @RequestBody CustomerBinhLuanRequest request) {
        CustomerTinTucResponse article = articles.get(id);
        if (article == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Bài viết không tồn tại"));
        }
        String name = (request.getName() != null && !request.getName().isBlank()) ? request.getName().trim() : "Bạn đọc AeroStride";
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        CustomerTinTucResponse.BinhLuanResponse comment = new CustomerTinTucResponse.BinhLuanResponse(name, request.getText().trim(), date);
        article.getComments().add(comment);

        return ResponseEntity.ok(ApiResponse.success(comment, "Gửi bình luận thành công!"));
    }
}
