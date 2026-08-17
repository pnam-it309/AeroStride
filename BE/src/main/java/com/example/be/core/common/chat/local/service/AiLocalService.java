package com.example.be.core.common.chat.local.service;

import com.example.be.core.admin.sanpham.model.response.ProductVariantResponse;
import com.example.be.core.admin.sanpham.service.AdminSanPhamService;
import com.example.be.entity.KienThucAi;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.repository.KienThucAiRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiLocalService {

    private final KienThucAiRepository aiKnowledgeRepository;
    private final AiWordLibrary wordLibrary;
    private final DataRetrievalLibrary dataLibrary;
    private final AdminSanPhamService sanPhamService;
    private final ObjectMapper objectMapper;

    private final Map<String, ChatState> sessionStates = new ConcurrentHashMap<>();
    private final Map<String, Pattern> compiledPatterns = new ConcurrentHashMap<>();
    private volatile List<KienThucAi> cachedKnowledgeList = null;
    private volatile long lastKnowledgeLoad = 0;
    private static final long KNOWLEDGE_CACHE_TTL = 30000; // 30 seconds

    private List<KienThucAi> getKnowledgeCached() {
        long now = System.currentTimeMillis();
        if (cachedKnowledgeList == null || (now - lastKnowledgeLoad > KNOWLEDGE_CACHE_TTL)) {
            synchronized (this) {
                if (cachedKnowledgeList == null || (now - lastKnowledgeLoad > KNOWLEDGE_CACHE_TTL)) {
                    cachedKnowledgeList = aiKnowledgeRepository.findAllByOrderByDoUuTienDesc();
                    lastKnowledgeLoad = now;
                }
            }
        }
        return cachedKnowledgeList != null ? cachedKnowledgeList : List.of();
    }

    private static class ChatState {
        String currentState = "NORMAL";
        Queue<String> messageQueue = new LinkedList<>();
        Map<String, Object> conversationData = new ConcurrentHashMap<>();
        long lastActionTime = System.currentTimeMillis();

        void addMessage(String msg) {
            if (messageQueue.size() >= 10) {
                messageQueue.poll();
            }
            messageQueue.add(msg);
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 600000)
    public void cleanupExpiredSessions() {
        long expireThreshold = System.currentTimeMillis() - (30 * 60 * 1000);
        sessionStates.entrySet().removeIf(entry -> entry.getValue().lastActionTime < expireThreshold);
        if (compiledPatterns.size() > 500) {
            compiledPatterns.clear();
        }
    }

    public String generateResponse(String userMessage) {
        return generateResponse(userMessage, "default-session");
    }

    public String generateResponse(String userMessage, String conversationId) {
        String cid = (conversationId != null && !conversationId.isBlank()) ? conversationId : "default-session";
        ChatState state = sessionStates.computeIfAbsent(cid, k -> new ChatState());

        long now = System.currentTimeMillis();
        if (now - state.lastActionTime > 10 * 60 * 1000) {
            state.currentState = "NORMAL";
            state.conversationData.clear();
        }
        state.lastActionTime = now;
        state.addMessage(userMessage);

        String normalizedInput = wordLibrary.normalize(userMessage);
        log.info("Processing Local AI chat: '{}' -> '{}'", userMessage, normalizedInput);

        return processUserQuery(state, normalizedInput, userMessage);
    }

    /**
     * Bộ xử lý truy vấn thông minh của Local AI:
     * - Trích xuất ý định, khoảng giá, thương hiệu, màu sắc, size, phong cách.
     * - Tìm kiếm sản phẩm thật từ Database và sinh câu trả lời giàu thông tin.
     * - LUÔN LUÔN đính kèm [[PRODUCT_JSON:...]] và [[SUGGESTIONS:...]] để UI hiển thị trực quan.
     */
    private String processUserQuery(ChatState state, String normalizedInput, String rawInput) {
        String lower = normalizedInput.toLowerCase().trim();

        // 1. Kiểm tra nếu khách phàn nàn / khiếu nại chất lượng / hoàn tiền
        if (isComplaint(lower)) {
            return handleComplaint(state, lower);
        }

        // 2. Kiểm tra nếu khách yêu cầu tư vấn Size giày
        if (lower.contains("size") || lower.contains("kích cỡ") || lower.contains("kích thước") || lower.contains("đo chân") || lower.contains("chân dài")) {
            return handleSizingConsultation(lower);
        }

        // 3. Tra cứu Knowledge Base được cấu hình trong DB
        List<KienThucAi> knowledges = getKnowledgeCached();
        for (KienThucAi k : knowledges) {
            if (isMatch(normalizedInput, k.getTuKhoa())) {
                String baseResponse = formatKnowledgeResponse(k, normalizedInput);
                List<ProductVariantResponse> matchedProducts = searchSmartVariants(lower);
                return enrichResponse(baseResponse, matchedProducts, buildContextualSuggestions(lower));
            }
        }

        // 4. Kiểm tra các câu hỏi chính sách cửa hàng
        if (lower.contains("địa chỉ") || lower.contains("ở đâu") || lower.contains("showroom") || lower.contains("chi nhánh") || lower.contains("cửa hàng")) {
            String policy = "Dạ, quý khách có thể ghé showroom chính thức của **AeroStride** tại: **123 Đường Cầu Giấy, Quận Cầu Giấy, Hà Nội**.\n" +
                    "- 🕒 Giờ mở cửa: **08:30 – 22:00** hàng ngày (kể cả Thứ 7, Chủ Nhật và ngày lễ).\n" +
                    "- 📞 Hotline hỗ trợ: **1900 88xx**.\n\n" +
                    "Shop xin giới thiệu một số mẫu giày bán chạy nhất đang có sẵn tại showroom để anh/chị tham khảo trước ạ:";
            return enrichResponse(policy, getTopSellingVariants(3), List.of("Chính sách bảo hành và đổi trả thế nào?", "Có giao hàng tận nhà không shop?", "Có voucher giảm giá hôm nay không?"));
        }

        if (lower.contains("ship") || lower.contains("giao hàng") || lower.contains("vận chuyển") || lower.contains("phí ship") || lower.contains("bao lâu")) {
            String policy = "Chính sách giao hàng của **AeroStride**:\n" +
                    "- 🚚 **MIỄN PHÍ VẬN CHUYỂN** toàn quốc cho đơn hàng từ **1.000.000 VNĐ** (đơn dưới 1M phí ship đồng giá 30.000 VNĐ).\n" +
                    "- ⚡ Thời gian nhận hàng: Nội thành Hà Nội / TP.HCM từ **1-2 ngày**; các tỉnh khác từ **3-5 ngày**.\n" +
                    "- 📦 Hỗ trợ **kiểm tra hàng trước khi thanh toán (COD)**.\n\n" +
                    "Dưới đây là các mẫu giày đang được freeship nhiều nhất tại shop:";
            return enrichResponse(policy, getTopSellingVariants(3), List.of("Chính sách đổi trả trong vòng 7 ngày?", "Cách đo size giày chuẩn?", "Mẫu giày Nike nào đang sale?"));
        }

        if (lower.contains("đổi trả") || lower.contains("bảo hành") || lower.contains("trả hàng") || lower.contains("hỏng") || lower.contains("chật") || lower.contains("rộng")) {
            String policy = "Chính sách bảo hành & đổi trả tại **AeroStride**:\n" +
                    "- 🔄 **Đổi hàng miễn phí trong 7 ngày** nếu không vừa size hoặc muốn đổi sang mẫu khác (sản phẩm còn nguyên tem mác, chưa qua sử dụng).\n" +
                    "- 🛡️ **Bảo hành 6 tháng** hoàn toàn miễn phí cho lỗi keo, chỉ, đế.\n" +
                    "- 💯 **Hoàn tiền 100%** nếu sản phẩm bị lỗi do nhà sản xuất.\n\n" +
                    "Bạn có thể yên tâm chọn mẫu giày yêu thích dưới đây nhé:";
            return enrichResponse(policy, getTopSellingVariants(3), List.of("Làm thế nào để đặt hàng?", "Tư vấn size giày cho tôi", "Có voucher giảm giá cho khách mới?"));
        }

        if (lower.contains("thanh toán") || lower.contains("chuyển khoản") || lower.contains("banking") || lower.contains("cod") || lower.contains("momo") || lower.contains("vnpay")) {
            String policy = "AeroStride hỗ trợ các hình thức thanh toán linh hoạt:\n" +
                    "1. 💵 **Thanh toán tiền mặt khi nhận hàng (COD)** sau khi kiểm tra giày.\n" +
                    "2. 💳 **Chuyển khoản ngân hàng (Vietcombank - VCB)**:\n" +
                    "   - Số tài khoản: `123456789`\n" +
                    "   - Tên tài khoản: `CONG TY TNHH AEROSTRIDE`\n" +
                    "3. 📲 Quét mã **VNPay-QR** / Thẻ ngân hàng tại quầy showroom.\n\n" +
                    "Mời bạn tham khảo các mẫu giày nổi bật để lựa chọn nhé:";
            return enrichResponse(policy, getTopSellingVariants(3), List.of("Có được kiểm tra hàng trước không?", "Phí vận chuyển bao nhiêu?", "Mẫu giày bán chạy nhất tuần này?"));
        }

        // 5. Kiểm tra khuyến mãi / Voucher / Giảm giá
        if (lower.contains("khuyến mãi") || lower.contains("giảm giá") || lower.contains("voucher") || lower.contains("sale") || lower.contains("ưu đãi") || lower.contains("mã giảm")) {
            String discounts = dataLibrary.getActiveDiscountsInfo();
            String coupons = dataLibrary.getActiveCouponsInfo();
            String response = "Dạ, **AeroStride** đang áp dụng rất nhiều chương trình ưu đãi và khuyến mãi hấp dẫn dành cho bạn:\n\n" +
                    discounts + "\n\n" + coupons + "\n\n" +
                    "Dưới đây là những mẫu giày đang được áp dụng **giá ưu đãi cực sốc** hôm nay:";
            List<ProductVariantResponse> promoVariants = searchPromoVariants();
            return enrichResponse(response, promoVariants, List.of("Mã voucher 'AERO10' áp dụng thế nào?", "Giày Nike đang có giảm giá không?", "Phí ship đơn hàng bao nhiêu?"));
        }

        // 6. Xử lý tìm kiếm sản phẩm theo Thương hiệu / Giá / Màu sắc / Từ khóa
        List<ProductVariantResponse> matchedVariants = searchSmartVariants(lower);
        String recommendationText = buildRecommendationText(lower, matchedVariants);
        List<String> suggestions = buildContextualSuggestions(lower);

        return enrichResponse(recommendationText, matchedVariants, suggestions);
    }

    /**
     * Tìm kiếm và chấm điểm sản phẩm thông minh dựa trên toàn bộ thuộc tính
     */
    private List<ProductVariantResponse> searchSmartVariants(String queryLower) {
        List<ProductVariantResponse> allVariants = sanPhamService.getAllVariants().stream()
                .filter(v -> v.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                .collect(Collectors.toList());

        if (allVariants.isEmpty()) {
            return List.of();
        }

        // Nhận diện thương hiệu
        String targetBrand = null;
        List<String> brands = List.of("nike", "adidas", "puma", "vans", "converse", "jordan", "mlb");
        for (String b : brands) {
            if (queryLower.contains(b)) {
                targetBrand = b;
                break;
            }
        }

        // Nhận diện mức giá
        BigDecimal targetPrice = extractPrice(queryLower);
        boolean isUnder1M = queryLower.contains("dưới 1 triệu") || queryLower.contains("dưới 1tr") || queryLower.contains("< 1tr") || queryLower.contains("giá rẻ") || queryLower.contains("bình dân");
        boolean isOver2M = queryLower.contains("trên 2 triệu") || queryLower.contains("cao cấp") || queryLower.contains("sang chảnh");

        // Nhận diện sắp xếp giá
        boolean sortByPriceAsc = queryLower.contains("thấp đến cao") || queryLower.contains("rẻ nhất") || queryLower.contains("tăng dần");
        boolean sortByPriceDesc = queryLower.contains("cao đến thấp") || queryLower.contains("đắt nhất") || queryLower.contains("giảm dần");

        // Tách các từ trong câu hỏi
        String[] words = queryLower.split("\\s+");

        Map<ProductVariantResponse, Integer> scoreMap = new HashMap<>();
        for (ProductVariantResponse v : allVariants) {
            int score = 0;

            // Khớp thương hiệu
            if (targetBrand != null && v.getTenThuongHieu() != null && v.getTenThuongHieu().toLowerCase().contains(targetBrand)) {
                score += 80;
            }

            // Khớp tên sản phẩm
            if (v.getTenSanPham() != null && queryLower.contains(v.getTenSanPham().toLowerCase())) {
                score += 70;
            }

            // Khớp màu sắc
            if (v.getTenMauSac() != null && queryLower.contains(v.getTenMauSac().toLowerCase())) {
                score += 40;
            }

            // Khớp chất liệu
            if (v.getTenChatLieu() != null && queryLower.contains(v.getTenChatLieu().toLowerCase())) {
                score += 30;
            }

            // Khớp size
            if (v.getGiaTriKichThuoc() != null && queryLower.contains(v.getGiaTriKichThuoc().toLowerCase())) {
                score += 40;
            }

            // Khớp giá
            if (v.getGiaBan() != null) {
                if (isUnder1M && v.getGiaBan().compareTo(new BigDecimal("1000000")) <= 0) {
                    score += 50;
                } else if (isOver2M && v.getGiaBan().compareTo(new BigDecimal("2000000")) >= 0) {
                    score += 50;
                } else if (targetPrice != null) {
                    BigDecimal diff = v.getGiaBan().subtract(targetPrice).abs();
                    if (diff.compareTo(new BigDecimal("200000")) <= 0) {
                        score += 60;
                    } else if (diff.compareTo(new BigDecimal("500000")) <= 0) {
                        score += 30;
                    }
                }
            }

            // Khớp từ khóa từng từ
            for (String w : words) {
                if (w.length() >= 2) {
                    if (v.getTenSanPham() != null && v.getTenSanPham().toLowerCase().contains(w)) score += 15;
                    if (v.getTenThuongHieu() != null && v.getTenThuongHieu().toLowerCase().contains(w)) score += 10;
                    if (v.getTenMauSac() != null && v.getTenMauSac().toLowerCase().contains(w)) score += 10;
                }
            }

            if (score > 0 || sortByPriceAsc || sortByPriceDesc) {
                scoreMap.put(v, score);
            }
        }

        List<ProductVariantResponse> result = new ArrayList<>(scoreMap.keySet());

        if (sortByPriceAsc) {
            result.sort(Comparator.comparing(v -> v.getGiaBan() != null ? v.getGiaBan() : BigDecimal.ZERO));
        } else if (sortByPriceDesc) {
            result.sort((a, b) -> {
                BigDecimal pA = a.getGiaBan() != null ? a.getGiaBan() : BigDecimal.ZERO;
                BigDecimal pB = b.getGiaBan() != null ? b.getGiaBan() : BigDecimal.ZERO;
                return pB.compareTo(pA);
            });
        } else {
            result.sort((a, b) -> Integer.compare(scoreMap.getOrDefault(b, 0), scoreMap.getOrDefault(a, 0)));
        }

        // Lọc lấy sản phẩm đại diện (mỗi idSanPham lấy 1 variant)
        List<ProductVariantResponse> distinctList = new ArrayList<>();
        Set<String> seenProductIds = new HashSet<>();
        for (ProductVariantResponse v : result) {
            String pId = v.getIdSanPham() != null ? v.getIdSanPham() : v.getId();
            if (!seenProductIds.contains(pId)) {
                seenProductIds.add(pId);
                distinctList.add(v);
            }
            if (distinctList.size() >= 4) break;
        }

        if (distinctList.isEmpty()) {
            return getTopSellingVariants(4);
        }

        return distinctList;
    }

    private List<ProductVariantResponse> getTopSellingVariants(int limit) {
        List<ProductVariantResponse> all = sanPhamService.getAllVariants().stream()
                .filter(v -> v.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                .collect(Collectors.toList());

        List<ProductVariantResponse> distinctList = new ArrayList<>();
        Set<String> seenProductIds = new HashSet<>();
        for (ProductVariantResponse v : all) {
            String pId = v.getIdSanPham() != null ? v.getIdSanPham() : v.getId();
            if (!seenProductIds.contains(pId)) {
                seenProductIds.add(pId);
                distinctList.add(v);
            }
            if (distinctList.size() >= limit) break;
        }
        return distinctList;
    }

    private List<ProductVariantResponse> searchPromoVariants() {
        return sanPhamService.getAllVariants().stream()
                .filter(v -> v.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                .filter(v -> v.getPhanTramGiam() != null && v.getPhanTramGiam().compareTo(BigDecimal.ZERO) > 0)
                .sorted((a, b) -> b.getPhanTramGiam().compareTo(a.getPhanTramGiam()))
                .limit(4)
                .collect(Collectors.toList());
    }

    private BigDecimal extractPrice(String text) {
        try {
            Matcher m = Pattern.compile("(\\d+)\\s*(?:k|nghìn|ngàn|000|tr|triệu)").matcher(text);
            if (m.find()) {
                String num = m.group(1);
                if (text.contains("tr") || text.contains("triệu")) {
                    return new BigDecimal(num).multiply(new BigDecimal("1000000"));
                }
                return new BigDecimal(num).multiply(new BigDecimal("1000"));
            }
        } catch (Exception e) {
            log.warn("Lỗi trích xuất giá: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Tạo văn bản phản hồi tự nhiên, chi tiết và hấp dẫn
     */
    private String buildRecommendationText(String queryLower, List<ProductVariantResponse> variants) {
        StringBuilder sb = new StringBuilder();
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        boolean isGreeting = queryLower.contains("chào") || queryLower.contains("hello") || queryLower.contains("hi") || queryLower.equals("alo");

        if (isGreeting) {
            sb.append("Dạ, **AeroStride** xin kính chào quý khách! Em là trợ lý tư vấn sản phẩm thông minh của shop. 🥰\n\n");
            sb.append("Dưới đây là top những mẫu giày sneaker thời thượng và bán chạy nhất tại cửa hàng hôm nay:\n\n");
        } else if (queryLower.contains("nike")) {
            sb.append("Dạ, AeroStride đang có sẵn các mẫu giày **Nike chính hãng** cực kỳ hot và êm chân dưới đây ạ:\n\n");
        } else if (queryLower.contains("adidas")) {
            sb.append("Dạ, bộ sưu tập giày **Adidas** phong cách thể thao, thời trang đang sẵn hàng tại AeroStride nè bạn:\n\n");
        } else if (queryLower.contains("puma") || queryLower.contains("vans") || queryLower.contains("converse")) {
            sb.append("Dạ, shop xin gửi bạn các mẫu giày phong cách trẻ trung năng động được ưa chuộng nhất:\n\n");
        } else if (queryLower.contains("chạy") || queryLower.contains("thể thao") || queryLower.contains("gym")) {
            sb.append("Dạ, để chạy bộ và tập luyện thể thao thoải mái nhất, shop gợi ý cho bạn các mẫu giày đệm êm, bám đường cực tốt sau:\n\n");
        } else if (queryLower.contains("giá") || queryLower.contains("tiền") || queryLower.contains("bao nhiêu")) {
            sb.append("Dạ, shop xin gửi bạn bảng giá ưu đãi của các mẫu giày nổi bật hiện có sẵn tại kho:\n\n");
        } else {
            sb.append("Dạ, AeroStride xin giới thiệu đến bạn những đôi giày chuẩn gu, siêu êm và thời trang nhất hiện nay:\n\n");
        }

        for (ProductVariantResponse v : variants) {
            String name = v.getTenSanPham() != null ? v.getTenSanPham() : v.getTenSanPhamDayDu();
            String brand = v.getTenThuongHieu() != null ? v.getTenThuongHieu() : "AeroStride";
            BigDecimal price = v.getGiaBan() != null ? v.getGiaBan() : v.getGiaGoc();
            String priceStr = price != null ? currencyFormat.format(price) : "Liên hệ";

            sb.append("👟 **").append(name).append("** (Hãng: ").append(brand).append(")\n");
            sb.append("   - 💵 *Giá ưu đãi:* **").append(priceStr).append("**");
            if (v.getPhanTramGiam() != null && v.getPhanTramGiam().compareTo(BigDecimal.ZERO) > 0) {
                sb.append(" 🔥 *(Giảm ").append(v.getPhanTramGiam().stripTrailingZeros().toPlainString()).append("%!)*");
            }
            sb.append("\n");
            if (v.getTenChatLieu() != null) {
                sb.append("   - 🌱 *Chất liệu:* ").append(v.getTenChatLieu()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("Bạn có thể bấm **Xem chi tiết** ở thẻ sản phẩm bên dưới để chọn size và đặt mua, hoặc nhắn cho shop để được tư vấn size vừa vặn nhất nhé! 💕");
        return sb.toString();
    }

    /**
     * Đính kèm dữ liệu JSON sản phẩm và Gợi ý câu hỏi vào phản hồi
     */
    private String enrichResponse(String messageText, List<ProductVariantResponse> variants, List<String> suggestions) {
        StringBuilder sb = new StringBuilder(messageText);

        // 1. Đính kèm PRODUCT_JSON
        if (variants != null && !variants.isEmpty()) {
            try {
                List<Map<String, Object>> jsonList = new ArrayList<>();
                Set<String> seenProductIds = new HashSet<>();

                for (ProductVariantResponse v : variants) {
                    String pId = v.getIdSanPham() != null ? v.getIdSanPham() : v.getId();
                    if (seenProductIds.contains(pId)) continue;
                    seenProductIds.add(pId);

                    Map<String, Object> map = new HashMap<>();
                    map.put("idSanPham", pId);
                    map.put("tenSanPham", v.getTenSanPham() != null ? v.getTenSanPham() : v.getTenSanPhamDayDu());
                    map.put("giaBan", v.getGiaBan() != null ? v.getGiaBan() : v.getGiaGoc());
                    map.put("tenThuongHieu", v.getTenThuongHieu() != null ? v.getTenThuongHieu() : "AeroStride");

                    String imgUrl = v.getHinhAnh();
                    if ((imgUrl == null || imgUrl.isBlank()) && v.getImages() != null && !v.getImages().isEmpty()) {
                        imgUrl = v.getImages().get(0).getDuongDanAnh();
                    }
                    if (imgUrl == null || imgUrl.isBlank()) {
                        imgUrl = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600";
                    }
                    map.put("hinhAnh", imgUrl);
                    map.put("phanTramGiam", v.getPhanTramGiam() != null ? v.getPhanTramGiam() : 0);
                    map.put("soLuong", v.getSoLuong() != null ? v.getSoLuong() : 10);
                    map.put("chatLieu", v.getTenChatLieu() != null ? v.getTenChatLieu() : "Da cao cấp");
                    map.put("mauSac", v.getTenMauSac() != null ? v.getTenMauSac() : "Đa dạng");
                    map.put("kichThuoc", v.getGiaTriKichThuoc() != null ? v.getGiaTriKichThuoc() : "38-43");

                    jsonList.add(map);
                    if (jsonList.size() >= 4) break;
                }

                sb.append("\n\n[[PRODUCT_JSON:").append(objectMapper.writeValueAsString(jsonList)).append("]]");
            } catch (Exception e) {
                log.error("Lỗi serialize PRODUCT_JSON: {}", e.getMessage());
            }
        }

        // 2. Đính kèm SUGGESTIONS
        if (suggestions != null && !suggestions.isEmpty()) {
            try {
                sb.append("\n\n[[SUGGESTIONS:").append(objectMapper.writeValueAsString(suggestions)).append("]]");
            } catch (Exception e) {
                log.error("Lỗi serialize SUGGESTIONS: {}", e.getMessage());
            }
        }

        return sb.toString();
    }

    private List<String> buildContextualSuggestions(String queryLower) {
        if (queryLower.contains("nike")) {
            return List.of("Nike có sẵn size 41 không shop?", "Có voucher giảm giá cho giày Nike không?", "Chính sách bảo hành và đổi trả thế nào?");
        }
        if (queryLower.contains("adidas")) {
            return List.of("Mẫu Adidas nào đang giảm giá nhiều nhất?", "Bên mình có ship COD toàn quốc không?", "Cách đo chiều dài chân chọn size chuẩn?");
        }
        if (queryLower.contains("giá") || queryLower.contains("tiền")) {
            return List.of("Có mã giảm giá 'AERO10' hôm nay không?", "Đơn hàng từ bao nhiêu thì được miễn phí ship?", "Tư vấn mẫu giày sneaker dưới 1 triệu");
        }
        if (queryLower.contains("size")) {
            return List.of("Chân dài 25.5cm thì đi size bao nhiêu?", "Nếu nhận hàng không vừa có được đổi size không?", "Có sẵn size 42 các mẫu này không shop?");
        }
        return List.of("Mẫu giày nào đang bán chạy nhất tuần này?", "Shop có những chương trình khuyến mãi gì?", "Chính sách bảo hành và đổi trả trong 7 ngày");
    }

    private boolean isComplaint(String lowerInput) {
        return lowerInput.contains("lỗi") || lowerInput.contains("hỏng") || lowerInput.contains("rách") ||
                lowerInput.contains("bong keo") || lowerInput.contains("bung keo") || lowerInput.contains("tệ") ||
                lowerInput.contains("chán") || lowerInput.contains("kém") || lowerInput.contains("thất vọng") ||
                lowerInput.contains("bực mình") || lowerInput.contains("khiếu nại") || lowerInput.contains("giao chậm") ||
                lowerInput.contains("chậm trễ") || lowerInput.contains("đợi lâu") || lowerInput.contains("thiếu hàng") ||
                lowerInput.contains("nhầm size") || lowerInput.contains("nhầm màu") || lowerInput.contains("không hài lòng");
    }

    private String handleComplaint(ChatState state, String lowerInput) {
        String apology = "Dạ, **AeroStride** vô cùng xin lỗi anh/chị vì sự cố vừa rồi đã làm ảnh hưởng tới tâm trạng của mình ạ! 🥺 Cửa hàng luôn đặt sự hài lòng của quý khách lên vị trí cao nhất.\n\n" +
                "- 🎁 Shop xin gửi tặng mình mã voucher **SORRY15** (giảm ngay 15% cho mọi sản phẩm) để bù đắp trải nghiệm chưa được trọn vẹn này.\n" +
                "- 🔄 Shop hỗ trợ **đổi mới sản phẩm miễn phí 100%** hoặc hỗ trợ hoàn tiền nhanh chóng.\n\n" +
                "Anh/chị có thể nhắn *'Gặp nhân viên'* để quản lý cửa hàng hỗ trợ xử lý tức thì, hoặc tham khảo các mẫu giày chất lượng cao khác của shop dưới đây nhé:";

        return enrichResponse(apology, getTopSellingVariants(3), List.of("Tôi muốn nói chuyện với nhân viên hỗ trợ.", "Mã voucher 'SORRY15' dùng thế nào?", "Quy trình đổi trả sản phẩm lỗi"));
    }

    private String handleSizingConsultation(String lowerInput) {
        // Kiểm tra cm
        Matcher cmMatcher = Pattern.compile("(\\d{2}(?:\\.\\d+)?)\\s*(?:cm|centimet)").matcher(lowerInput);
        if (cmMatcher.find()) {
            double length = Double.parseDouble(cmMatcher.group(1));
            String size = "40";
            if (length <= 24.5) size = "39";
            else if (length <= 25.0) size = "40";
            else if (length <= 25.5) size = "41";
            else if (length <= 26.0) size = "42";
            else if (length <= 26.5) size = "43";
            else size = "44";

            String text = String.format("Dạ, với chiều dài bàn chân **%.1f cm**, size giày chuẩn và êm chân nhất của bạn là **Size %s** ạ! 🎉\n\n" +
                    "Dưới đây là các mẫu giày cực đẹp đang có sẵn **Size %s** tại kho của AeroStride:", length, size, size);
            return enrichResponse(text, getTopSellingVariants(3), List.of("Giữ hàng size " + size + " cho tôi", "Chính sách đổi size nếu không vừa?", "Có voucher giảm giá hôm nay không?"));
        }

        String guide = "Dạ, bảng hướng dẫn đo size giày chuẩn tại **AeroStride**:\n" +
                "- 📏 **24.5 cm** ➔ Size **39**\n" +
                "- 📏 **25.0 cm** ➔ Size **40**\n" +
                "- 📏 **25.5 cm** ➔ Size **41**\n" +
                "- 📏 **26.0 cm** ➔ Size **42**\n" +
                "- 📏 **26.5 cm** ➔ Size **43**\n\n" +
                "*(Nếu chân bạn hơi bè ngang hoặc mu bàn chân dày, shop khuyên bạn nên chọn tăng thêm 1 size để đi êm và thoải mái nhất nha!)* 👟\n\n" +
                "Dưới đây là các mẫu giày bán chạy nhất có đủ mọi size để bạn lựa chọn:";
        return enrichResponse(guide, getTopSellingVariants(3), List.of("Chân dài 25.5cm đi size mấy?", "Tôi muốn mua size 42", "Đổi size có mất phí không?"));
    }

    private boolean isMatch(String input, String keywordString) {
        if (keywordString == null || keywordString.isEmpty()) return false;
        Pattern pattern = compiledPatterns.computeIfAbsent(keywordString, kws -> {
            String[] keywords = kws.split(",");
            StringBuilder regex = new StringBuilder(".*\\b(?:");
            for (int i = 0; i < keywords.length; i++) {
                if (i > 0) regex.append("|");
                regex.append(Pattern.quote(keywords[i].trim().toLowerCase()));
            }
            regex.append(")\\b.*");
            return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
        });
        return pattern.matcher(input.toLowerCase()).matches();
    }

    private String formatKnowledgeResponse(KienThucAi knowledge, String input) {
        String template = knowledge.getMauCauTraLoi();
        if ("PRODUCT_LIST".equals(knowledge.getMucDich())) {
            String productsInfo = dataLibrary.getTopProductsInfo(4);
            return template.replace("{products}", productsInfo);
        }
        return template;
    }
}
