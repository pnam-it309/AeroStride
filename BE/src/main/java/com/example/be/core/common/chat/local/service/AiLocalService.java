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
        Set<String> suggestedProductIds = new HashSet<>();
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
            state.suggestedProductIds.clear();
        }
        state.lastActionTime = now;
        state.addMessage(userMessage);

        String normalizedInput = wordLibrary.normalize(userMessage);
        log.info("Processing Local AI chat: '{}' -> '{}'", userMessage, normalizedInput);

        return processUserQuery(state, normalizedInput, userMessage);
    }

    /**
     * Bộ xử lý thông minh phân định rõ:
     * 1. Câu hỏi FAQ/Chính sách/Xã giao -> Chỉ trả lời văn bản súc tích, KHÔNG đính kèm sản phẩm.
     * 2. Câu hỏi tìm kiếm/tư vấn sản phẩm -> Tìm kiếm chính xác và đính kèm danh sách sản phẩm.
     */
    private String processUserQuery(ChatState state, String normalizedInput, String rawInput) {
        String lower = normalizedInput.toLowerCase().trim();
        String rawLower = rawInput != null ? rawInput.toLowerCase().trim() : lower;

        // =========================================================================
        // NHÓM 1: CÂU HỎI FAQ, CHÍNH SÁCH & HỖ TRỢ (KHÔNG ĐÍNH KÈM SẢN PHẨM)
        // =========================================================================

        // 1.1. Khiếu nại / Phàn nàn chất lượng / Dịch vụ
        if (isComplaint(lower)) {
            return handleComplaint(state, lower);
        }

        // 1.2. Chào hỏi xã giao (Greeting)
        if (isGreeting(lower)) {
            String greetingText = "Dạ em chào anh/chị ạ! Em là trợ lý tư vấn của AeroStride. Em có thể giúp anh/chị tìm mẫu giày ưng ý, tư vấn chọn size hoặc giải đáp các thắc mắc về đơn hàng hôm nay ạ!";
            return enrichResponse(greetingText, null, List.of("Mẫu giày nào đang bán chạy nhất?", "Shop có những ưu đãi gì hôm nay?", "Chính sách bảo hành và đổi trả thế nào?"));
        }

        // 1.3. Cảm ơn / Tạm biệt (Thank you)
        if (isThankYou(lower)) {
            String thankText = "Dạ không có gì ạ! Chúc anh/chị một ngày vui vẻ và chọn được đôi giày ưng ý nhất tại AeroStride nhé. Nếu cần hỗ trợ thêm, anh/chị cứ nhắn em nha!";
            return enrichResponse(thankText, null, List.of("Xem bảng hướng dẫn chọn size", "Chính sách giao hàng toàn quốc", "Xem các mẫu giày mới về"));
        }

        // 1.4. Chính sách bảo hành & Đổi trả
        if (lower.contains("bảo hành") || lower.contains("đổi trả") || lower.contains("đổi hàng") || lower.contains("trả hàng") || lower.contains("lỗi keo") || lower.contains("bung keo")) {
            String policy = "Chính sách bảo hành & đổi trả tại AeroStride:\n\n" +
                    "• Đổi hàng miễn phí trong 7 ngày nếu không vừa size hoặc muốn đổi sang mẫu khác (sản phẩm còn nguyên tem mác, hộp và chưa qua sử dụng).\n" +
                    "• Bảo hành 6 tháng hoàn toàn miễn phí cho các lỗi keo, chỉ, đế từ nhà sản xuất.\n" +
                    "• Hoàn tiền 100% nếu phát hiện sản phẩm lỗi hoặc không đúng như mô tả.\n\n" +
                    "Bạn hoàn toàn có thể yên tâm khi mua sắm tại AeroStride ạ!";
            return enrichResponse(policy, null, List.of("Quy trình gửi hàng đổi trả", "Tư vấn cách chọn size chuẩn", "Có mẫu giày nào đang giảm giá?"));
        }

        // 1.5. Chính sách Giao hàng / Phí ship / Thời gian vận chuyển
        if (lower.contains("ship") || lower.contains("giao hàng") || lower.contains("vận chuyển") || lower.contains("phí ship") || lower.contains("bao lâu nhận")) {
            String policy = "Chính sách giao hàng của AeroStride:\n\n" +
                    "• MIỄN PHÍ VẬN CHUYỂN toàn quốc cho mọi đơn hàng từ 1.000.000đ (đơn dưới 1M phí ship đồng giá 30.000đ).\n" +
                    "• Thời gian nhận hàng: Nội thành Hà Nội / TP.HCM từ 1 - 2 ngày; các tỉnh thành khác từ 2 - 4 ngày.\n" +
                    "• Đồng kiểm & COD: Quý khách được mở hộp kiểm tra giày trước khi thanh toán cho nhân viên giao hàng.";
            return enrichResponse(policy, null, List.of("Chính sách đổi trả trong 7 ngày", "Hình thức thanh toán", "Xem các mẫu giày bán chạy"));
        }

        // 1.6. Địa chỉ Showroom / Cửa hàng / Giờ mở cửa
        if (lower.contains("địa chỉ") || lower.contains("ở đâu") || lower.contains("showroom") || lower.contains("cửa hàng") || lower.contains("chi nhánh") || lower.contains("mở cửa")) {
            String policy = "Thông tin hệ thống cửa hàng AeroStride:\n\n" +
                    "• Showroom chính: 123 Đường Cầu Giấy, Quận Cầu Giấy, Hà Nội.\n" +
                    "• Giờ mở cửa: 08:30 – 22:00 hàng ngày (kể cả Thứ 7, Chủ Nhật và các ngày lễ).\n" +
                    "• Hotline hỗ trợ 24/7: 1900 88xx.\n\n" +
                    "AeroStride luôn sẵn sàng đón tiếp quý khách ghé trải nghiệm trực tiếp ạ!";
            return enrichResponse(policy, null, List.of("Chính sách bảo hành và đổi trả", "Có chỗ để xe ô tô không?", "Xem mẫu giày thể thao hot"));
        }

        // 1.7. Hình thức thanh toán
        if (lower.contains("thanh toán") || lower.contains("chuyển khoản") || lower.contains("banking") || lower.contains("cod") || lower.contains("momo") || lower.contains("vnpay") || lower.contains("quẹt thẻ")) {
            String policy = "AeroStride hỗ trợ đa dạng các hình thức thanh toán thuận tiện:\n\n" +
                    "1. Thanh toán khi nhận hàng (COD): Nhận hàng, kiểm tra giày ưng ý rồi thanh toán tiền mặt cho shipper.\n" +
                    "2. Thanh toán VNPay-QR: Quét mã QR nhanh chóng qua ứng dụng ngân hàng hoặc ví điện tử.\n" +
                    "3. Chuyển khoản ngân hàng trực tiếp qua tài khoản chính thức của công ty.";
            return enrichResponse(policy, null, List.of("Có được kiểm tra hàng trước không?", "Phí vận chuyển bao nhiêu?", "Mẫu giày bán chạy nhất tuần này?"));
        }

        // 1.8. Chất liệu giày chung
        if (lower.contains("chất liệu") || lower.contains("làm bằng") || lower.contains("chất da") || lower.contains("đế gì")) {
            String materialInfo = "Chất liệu giày tại AeroStride:\n\n" +
                    "• Thân giày: Sử dụng da bò cao cấp, da tổng hợp vi sợi hoặc vải Mesh dệt 3D thoáng khí, chống hôi chân.\n" +
                    "• Lót giày: Đệm Ortholite êm ái, kháng khuẩn và đàn hồi tốt.\n" +
                    "• Đế giày: Đế cao su non nguyên khối, thiết kế rãnh sâu chống trơn trượt tối đa.";
            return enrichResponse(materialInfo, null, List.of("Xem mẫu giày chạy bộ thoáng khí", "Chính sách bảo hành keo đế", "Tư vấn chọn size giày"));
        }

        // 1.9. Hỏi về bảng size / cách đo size (khi KHÔNG tìm giày size cụ thể)
        boolean hasSpecificSizeSearch = Pattern.compile("(?:size|kích cỡ|cỡ)\\s*\\d{2}|\\d{2}\\s*(?:size)").matcher(lower).find()
                || Pattern.compile("(?:tìm|mua|kiếm|cho|đôi).*\\b(3[6-9]|4[0-6])\\b").matcher(lower).find();

        if (!hasSpecificSizeSearch && (lower.contains("bảng size") || lower.contains("hướng dẫn size") || lower.contains("cách đo") || lower.contains("chân dài") || lower.contains("đo chân") || (lower.contains("size") && !lower.contains("tìm") && !lower.contains("mua")))) {
            return handleSizingGuide(lower);
        }

        // =========================================================================
        // NHÓM 2: TÌM KIẾM VÀ TƯ VẤN SẢN PHẨM (CÓ ĐÍNH KÈM DANH SÁCH SẢN PHẨM)
        // =========================================================================

        // 2.1. Yêu cầu xem mẫu khác / Còn mẫu nào nữa không ("toi muon tim giay khac", "ko con giay nao ak", "mẫu khác", "xem thêm")
        boolean isLookingForOtherShoes = lower.contains("khác") || lower.contains("không còn") || lower.contains("ko còn") || lower.contains("hết rồi") || lower.contains("xem thêm") || lower.contains("mẫu nữa") || lower.contains("nữa không") || lower.contains("mẫu mới");
        if (isLookingForOtherShoes) {
            List<ProductVariantResponse> otherVariants = searchFreshVariants(state, lower);
            String text = "Dạ, shop còn rất nhiều mẫu giày phong cách khác nữa ạ! Mời bạn tham khảo thêm một số mẫu nổi bật dưới đây nhé:";
            return enrichResponse(text, otherVariants, List.of("Tìm giày màu trắng", "Tìm giày thể thao chạy bộ", "Mẫu giày dưới 1 triệu"));
        }

        // 2.2. Kiểm tra khuyến mãi / Voucher / Giảm giá (CÓ kèm sản phẩm sale)
        if (lower.contains("khuyến mãi") || lower.contains("giảm giá") || lower.contains("voucher") || lower.contains("sale") || lower.contains("ưu đãi") || lower.contains("mã giảm")) {
            String discounts = dataLibrary.getActiveDiscountsInfo();
            String coupons = dataLibrary.getActiveCouponsInfo();
            String response = "Dạ, AeroStride đang có nhiều chương trình ưu đãi hấp dẫn dành cho bạn:\n\n" +
                    (discounts.isBlank() ? "• Đang diễn ra nhiều đợt giảm giá trực tiếp theo sản phẩm." : discounts) + "\n\n" +
                    (coupons.isBlank() ? "• Áp dụng voucher giảm giá khi đặt hàng online." : coupons) + "\n\n" +
                    "Dưới đây là những mẫu giày đang có mức giá ưu đãi tốt nhất hôm nay:";
            List<ProductVariantResponse> promoVariants = searchPromoVariants();
            trackSuggested(state, promoVariants);
            return enrichResponse(response, promoVariants, List.of("Mã voucher áp dụng thế nào?", "Giày thể thao nào đang sale?", "Phí ship đơn hàng bao nhiêu?"));
        }

        // 2.3. Tìm kiếm sản phẩm theo thuộc tính (Size, Màu sắc, Hãng, Mức giá, Loại giày)
        List<ProductVariantResponse> matchedVariants = searchSmartVariants(lower, state);
        trackSuggested(state, matchedVariants);
        String recommendationText = buildRecommendationText(lower, matchedVariants);
        List<String> suggestions = buildContextualSuggestions(lower);

        return enrichResponse(recommendationText, matchedVariants, suggestions);
    }

    private boolean isGreeting(String lower) {
        return lower.equals("chào") || lower.equals("chào shop") || lower.equals("hello") || lower.equals("hi") || lower.equals("alo") || lower.equals("xin chào") || lower.equals("shop ơi") || lower.equals("ad ơi") || lower.equals("chào bạn");
    }

    private boolean isThankYou(String lower) {
        return lower.contains("cảm ơn") || lower.contains("thanks") || lower.contains("tks") || lower.contains("cám ơn") || lower.equals("ok") || lower.equals("ok shop") || lower.equals("tuyệt vời");
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
        String apology = "Dạ, AeroStride vô cùng xin lỗi anh/chị vì sự cố vừa rồi đã làm ảnh hưởng tới trải nghiệm của mình ạ! Cửa hàng luôn đặt quyền lợi của khách hàng lên hàng đầu.\n\n" +
                "• Shop hỗ trợ đổi mới sản phẩm miễn phí 100% hoặc hoàn tiền nhanh chóng cho đơn hàng có vấn đề.\n" +
                "• Quý khách vui lòng cung cấp Mã Đơn Hàng hoặc số điện thoại đặt hàng, hoặc bấm 'Gặp nhân viên' để chuyên viên xử lý khiếu nại hỗ trợ ngay lập tức nhé!";

        return enrichResponse(apology, null, List.of("Tôi muốn gặp nhân viên hỗ trợ", "Quy trình đổi trả sản phẩm lỗi", "Thời gian xử lý hoàn tiền"));
    }

    private String handleSizingGuide(String lowerInput) {
        // Kiểm tra nếu có cm
        Matcher cmMatcher = Pattern.compile("(\\d{2}(?:\\.\\d+)?)\\s*(?:cm|centimet)").matcher(lowerInput);
        if (cmMatcher.find()) {
            double length = Double.parseDouble(cmMatcher.group(1));
            String size = "40";
            if (length <= 24.0) size = "38";
            else if (length <= 24.5) size = "39";
            else if (length <= 25.0) size = "40";
            else if (length <= 25.5) size = "41";
            else if (length <= 26.0) size = "42";
            else if (length <= 26.5) size = "43";
            else size = "44";

            String text = String.format("Dạ, với chiều dài bàn chân %.1f cm, size giày chuẩn và vừa vặn nhất của bạn là Size %s ạ! (Nếu chân bạn hơi bè ngang hoặc mu bàn chân dày, bạn có thể chọn tăng thêm 1 size để đi thoải mái hơn nhé).\n\nBạn có muốn shop gợi ý các mẫu giày đang sẵn Size %s không ạ?", length, size, size);
            return enrichResponse(text, null, List.of("Tìm giày size " + size, "Chính sách đổi size nếu không vừa", "Cách đo bàn chân chuẩn"));
        }

        String guide = "Bảng hướng dẫn chọn size giày chuẩn tại AeroStride:\n\n" +
                "• 24.0 cm -> Size 38\n" +
                "• 24.5 cm -> Size 39\n" +
                "• 25.0 cm -> Size 40\n" +
                "• 25.5 cm -> Size 41\n" +
                "• 26.0 cm -> Size 42\n" +
                "• 26.5 cm -> Size 43\n" +
                "• 27.0 cm -> Size 44\n\n" +
                "Lưu ý: Nếu chân bạn hơi bè ngang hoặc mu bàn chân dày, shop khuyên bạn nên chọn tăng thêm 1 size để đi êm ái nhất nhé!";
        return enrichResponse(guide, null, List.of("Tìm giày size 39", "Tìm giày size 40", "Tìm giày size 41", "Chính sách đổi size nếu không vừa"));
    }

    /**
     * Tìm kiếm các mẫu giày mới chưa được gợi ý trong phiên chat
     */
    private List<ProductVariantResponse> searchFreshVariants(ChatState state, String queryLower) {
        List<ProductVariantResponse> all = sanPhamService.getAllVariants().stream()
                .filter(v -> v.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                .collect(Collectors.toList());

        List<ProductVariantResponse> fresh = new ArrayList<>();
        Set<String> seenProductIds = new HashSet<>();

        // Ưu tiên các sản phẩm chưa gợi ý
        for (ProductVariantResponse v : all) {
            String pId = v.getIdSanPham() != null ? v.getIdSanPham() : v.getId();
            if (!state.suggestedProductIds.contains(pId) && !seenProductIds.contains(pId)) {
                seenProductIds.add(pId);
                fresh.add(v);
            }
            if (fresh.size() >= 4) break;
        }

        // Nếu đã gợi ý hết, lấy lại top bán chạy
        if (fresh.isEmpty()) {
            state.suggestedProductIds.clear();
            return getTopSellingVariants(4);
        }

        trackSuggested(state, fresh);
        return fresh;
    }

    private void trackSuggested(ChatState state, List<ProductVariantResponse> variants) {
        if (state == null || variants == null) return;
        for (ProductVariantResponse v : variants) {
            String pId = v.getIdSanPham() != null ? v.getIdSanPham() : v.getId();
            state.suggestedProductIds.add(pId);
        }
    }

    /**
     * Tìm kiếm và chấm điểm sản phẩm thông minh dựa trên toàn bộ thuộc tính
     */
    private List<ProductVariantResponse> searchSmartVariants(String queryLower, ChatState state) {
        List<ProductVariantResponse> allVariants = sanPhamService.getAllVariants().stream()
                .filter(v -> v.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                .collect(Collectors.toList());

        if (allVariants.isEmpty()) {
            return List.of();
        }

        // 1. Nhận diện thương hiệu
        String targetBrand = null;
        List<String> brands = List.of("nike", "adidas", "puma", "vans", "converse", "jordan", "mlb", "asics", "new balance");
        for (String b : brands) {
            if (queryLower.contains(b)) {
                targetBrand = b;
                break;
            }
        }

        // 2. Nhận diện màu sắc
        String targetColor = null;
        List<String> colors = List.of("trắng", "đen", "đỏ", "xanh", "xám", "vàng", "hồng", "kem", "be", "cam", "tím", "nâu");
        for (String c : colors) {
            if (queryLower.contains(c)) {
                targetColor = c;
                break;
            }
        }

        // 3. Nhận diện size cụ thể (size 38, size 39, ...)
        String targetSize = null;
        Matcher sizeMatcher = Pattern.compile("\\b(3[6-9]|4[0-6])\\b").matcher(queryLower);
        if (sizeMatcher.find()) {
            targetSize = sizeMatcher.group(1);
        }

        // 4. Nhận diện mức giá
        BigDecimal targetPrice = extractPrice(queryLower);
        boolean isUnder1M = queryLower.contains("dưới 1 triệu") || queryLower.contains("dưới 1tr") || queryLower.contains("< 1tr") || queryLower.contains("giá rẻ") || queryLower.contains("bình dân");
        boolean isOver2M = queryLower.contains("trên 2 triệu") || queryLower.contains("cao cấp") || queryLower.contains("sang chảnh");

        String queryUnaccent = removeDiacritics(queryLower);
        String[] words = queryLower.split("\\s+");
        String[] unaccentWords = queryUnaccent.split("\\s+");

        Map<ProductVariantResponse, Integer> scoreMap = new HashMap<>();
        for (ProductVariantResponse v : allVariants) {
            int score = 0;

            String tenSp = v.getTenSanPham() != null ? v.getTenSanPham().toLowerCase() : "";
            String tenSpUnaccent = removeDiacritics(tenSp);
            String maSp = v.getMaSanPham() != null ? v.getMaSanPham().toLowerCase() : "";
            String maCtsp = v.getMaChiTietSanPham() != null ? v.getMaChiTietSanPham().toLowerCase() : "";
            String vBrand = v.getTenThuongHieu() != null ? v.getTenThuongHieu().toLowerCase() : "";
            String vBrandUnaccent = removeDiacritics(vBrand);
            String vColor = v.getTenMauSac() != null ? v.getTenMauSac().toLowerCase() : "";
            String vColorUnaccent = removeDiacritics(vColor);

            // Khớp mã SP hoặc mã biến thể
            if (!maSp.isEmpty() && (queryLower.contains(maSp) || queryUnaccent.contains(maSp))) score += 250;
            if (!maCtsp.isEmpty() && (queryLower.contains(maCtsp) || queryUnaccent.contains(maCtsp))) score += 250;

            // Khớp nguyên cụm tên sản phẩm
            if (!tenSp.isEmpty()) {
                if (queryLower.contains(tenSp) || tenSp.contains(queryLower)) score += 180;
                else if (queryUnaccent.contains(tenSpUnaccent) || tenSpUnaccent.contains(queryUnaccent)) score += 150;
            }

            // Khớp thương hiệu
            if (targetBrand != null && vBrand.contains(targetBrand)) {
                score += 80;
            } else if (!vBrand.isEmpty() && (queryLower.contains(vBrand) || queryUnaccent.contains(vBrandUnaccent))) {
                score += 70;
            }

            // Khớp màu sắc
            if (targetColor != null && vColor.contains(targetColor)) {
                score += 70;
            } else if (!vColor.isEmpty() && (queryLower.contains(vColor) || queryUnaccent.contains(vColorUnaccent))) {
                score += 50;
            }

            // Khớp size
            if (targetSize != null && v.getGiaTriKichThuoc() != null && v.getGiaTriKichThuoc().contains(targetSize)) {
                score += 90;
            }

            // Khớp chất liệu
            if (v.getTenChatLieu() != null && (queryLower.contains(v.getTenChatLieu().toLowerCase()) || queryUnaccent.contains(removeDiacritics(v.getTenChatLieu())))) {
                score += 30;
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
            for (int i = 0; i < words.length; i++) {
                String w = words[i];
                String uw = unaccentWords.length > i ? unaccentWords[i] : removeDiacritics(w);
                if (w.length() >= 2) {
                    if (tenSp.contains(w) || tenSpUnaccent.contains(uw)) score += 20;
                    if (vBrand.contains(w) || vBrandUnaccent.contains(uw)) score += 15;
                    if (vColor.contains(w) || vColorUnaccent.contains(uw)) score += 12;
                }
            }

            if (score > 0) {
                scoreMap.put(v, score);
            }
        }

        List<ProductVariantResponse> result = new ArrayList<>(scoreMap.keySet());
        result.sort((a, b) -> Integer.compare(scoreMap.getOrDefault(b, 0), scoreMap.getOrDefault(a, 0)));

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

    private String removeDiacritics(String str) {
        if (str == null) return "";
        String nfd = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replace("đ", "d")
                .replace("Đ", "d")
                .trim();
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
     * Tạo văn bản phản hồi tự nhiên, chuẩn Markdown, không chứa emoji 4-byte lỗi font
     */
    private String buildRecommendationText(String queryLower, List<ProductVariantResponse> variants) {
        StringBuilder sb = new StringBuilder();
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Kiểm tra size
        Matcher sizeMatcher = Pattern.compile("\\b(3[6-9]|4[0-6])\\b").matcher(queryLower);
        String foundSize = sizeMatcher.find() ? sizeMatcher.group(1) : null;

        if (foundSize != null) {
            sb.append("Dạ, AeroStride xin gửi bạn các mẫu giày đang có sẵn **Size ").append(foundSize).append("** tại cửa hàng để bạn lựa chọn nhé:\n\n");
        } else if (queryLower.contains("trắng")) {
            sb.append("Dạ, shop xin gửi bạn những mẫu giày tông **màu trắng** cực đẹp, thời trang và dễ phối đồ nhất ạ:\n\n");
        } else if (queryLower.contains("đen")) {
            sb.append("Dạ, bộ sưu tập giày **màu đen** cá tính, sang trọng và bền màu đang sẵn hàng tại AeroStride nè bạn:\n\n");
        } else if (queryLower.contains("nike")) {
            sb.append("Dạ, AeroStride đang có sẵn các mẫu giày **Nike chính hãng** cực hot và êm chân dưới đây ạ:\n\n");
        } else if (queryLower.contains("adidas")) {
            sb.append("Dạ, bộ sưu tập giày **Adidas** phong cách thể thao và năng động đang có sẵn tại AeroStride ạ:\n\n");
        } else if (queryLower.contains("chạy") || queryLower.contains("thể thao") || queryLower.contains("gym")) {
            sb.append("Dạ, để tập luyện và chạy bộ thoải mái nhất, shop gợi ý cho bạn các mẫu giày đệm êm, bám đường cực tốt sau:\n\n");
        } else if (queryLower.contains("dưới 1 triệu") || queryLower.contains("dưới 1tr") || queryLower.contains("giá rẻ")) {
            sb.append("Dạ, đây là những mẫu giày chất lượng cao với mức giá cực kỳ ưu đãi **dưới 1 triệu đồng** dành cho bạn:\n\n");
        } else {
            sb.append("Dạ, AeroStride xin giới thiệu đến bạn những đôi giày chuẩn gu, siêu êm và thời trang nhất hiện nay:\n\n");
        }

        for (ProductVariantResponse v : variants) {
            String name = v.getTenSanPham() != null ? v.getTenSanPham() : v.getTenSanPhamDayDu();
            String brand = v.getTenThuongHieu() != null ? v.getTenThuongHieu() : "AeroStride";
            BigDecimal finalPrice = v.getGiaBan() != null ? v.getGiaBan() : v.getGiaGoc();
            String priceStr = finalPrice != null ? currencyFormat.format(finalPrice) : "Liên hệ";

            sb.append("• **").append(name).append("** (Hãng: ").append(brand).append(")\n");
            if (v.getPhanTramGiam() != null && v.getPhanTramGiam().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal origPrice = v.getGiaGoc() != null ? v.getGiaGoc() : finalPrice;
                sb.append("   - Giá đang giảm: **").append(priceStr).append("**");
                sb.append(" (Giá gốc: ~~").append(currencyFormat.format(origPrice)).append("~~, Giảm **-").append(v.getPhanTramGiam().stripTrailingZeros().toPlainString()).append("%** trong Đợt giảm giá)\n");
            } else {
                sb.append("   - Giá bán: **").append(priceStr).append("**\n");
            }
            if (v.getTenChatLieu() != null) {
                sb.append("   - Chất liệu: ").append(v.getTenChatLieu()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("Bạn có thể bấm **Xem chi tiết** ở thẻ sản phẩm bên dưới để chọn size và đặt mua, hoặc nhắn cho shop nếu cần hỗ trợ thêm nhé!");
        return sb.toString();
    }

    /**
     * Đính kèm dữ liệu JSON sản phẩm và Gợi ý câu hỏi vào phản hồi
     */
    private String enrichResponse(String messageText, List<ProductVariantResponse> variants, List<String> suggestions) {
        StringBuilder sb = new StringBuilder(messageText);

        // 1. Đính kèm PRODUCT_JSON nếu có sản phẩm
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
                    map.put("giaGoc", v.getGiaGoc());
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
            return List.of("Đơn hàng từ bao nhiêu thì được miễn phí ship?", "Tư vấn mẫu giày sneaker dưới 1 triệu", "Có voucher giảm giá hôm nay không?");
        }
        if (queryLower.contains("size")) {
            return List.of("Chân dài 25.5cm thì đi size bao nhiêu?", "Nếu nhận hàng không vừa có được đổi size không?", "Có sẵn size 42 các mẫu này không shop?");
        }
        return List.of("Mẫu giày nào đang bán chạy nhất tuần này?", "Shop có những chương trình khuyến mãi gì?", "Chính sách bảo hành và đổi trả trong 7 ngày");
    }
}
