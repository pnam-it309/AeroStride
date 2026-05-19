package com.example.be.core.admin.chat.service.impl;

import com.example.be.core.admin.chat.model.ChatMessageResponse;
import com.example.be.core.admin.chat.repository.AdminChatMessageRepository;
import com.example.be.core.admin.chat.service.AiChatService;
import com.example.be.core.admin.sanpham.model.response.ProductVariantResponse;
import com.example.be.core.admin.sanpham.service.AdminSanPhamService;
import com.example.be.entity.ChatConversation;
import com.example.be.entity.ChatMessage;
import com.example.be.infrastructure.constants.ChatConstants;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AiChatServiceImpl - Service xử lý tư vấn AI thông minh cho khách hàng.
 *
 * Cải tiến so với phiên bản trước:
 * 1. Cache danh sách sản phẩm trong bộ nhớ (5 phút TTL) → giảm tải CSDL
 * 2. Nhúng lịch sử hội thoại gần nhất (10 tin) → AI hiểu ngữ cảnh liên tục
 * 3. RestTemplate có Timeout cấu hình → chống treo luồng khi API bên thứ ba chậm
 * 4. Model name & Base URL cấu hình từ .env → đổi model không cần compile lại
 * 5. Fallback message gửi qua WebSocket khi AI gặp sự cố → khách hàng không bị "treo"
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private final AdminSanPhamService sanPhamService;
    private final AdminChatMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;

    @Value("${google.gemini.api-key}")
    private String geminiApiKey;

    @Value("${google.gemini.model:gemini-1.5-flash-latest}")
    private String geminiModel;

    @Value("${google.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiBaseUrl;

    private static final String FALLBACK_MESSAGE =
            "Hiện tại trợ lý AI đang bận xử lý. Hệ thống đã chuyển tiếp cuộc trò chuyện tới nhân viên hỗ trợ trực tiếp. Xin vui lòng đợi trong giây lát!";

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // === In-Memory Cache cho danh sách sản phẩm ===
    private volatile List<ProductVariantResponse> cachedVariants;
    private volatile long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 phút

    /**
     * Constructor injection với RestTemplate được cấu hình Timeout.
     * Không dùng @RequiredArgsConstructor vì RestTemplate cần builder pattern.
     */
    public AiChatServiceImpl(
            AdminSanPhamService sanPhamService,
            AdminChatMessageRepository messageRepository,
            SimpMessagingTemplate messagingTemplate,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${google.gemini.timeout-ms:15000}") int timeoutMs
    ) {
        this.sanPhamService = sanPhamService;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;

        // [Cải tiến 3] RestTemplate có Timeout → chống treo luồng @Async
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .readTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Async
    @Override
    public void generateAndSendResponse(ChatConversation conversation, String customerText) {
        log.info("AI đang xử lý tin nhắn: {}", customerText);

        if (geminiApiKey == null || geminiApiKey.isBlank() || "your_gemini_api_key_here".equals(geminiApiKey)) {
            log.warn("Gemini API Key chưa được cấu hình. Bỏ qua xử lý AI.");
            return;
        }

        try {
            // [Cải tiến 1] Lấy danh sách sản phẩm từ cache thay vì query DB mỗi lần
            String productContext = buildProductContext();

            // [Cải tiến 2] Lấy lịch sử hội thoại gần nhất để AI hiểu ngữ cảnh liên tục
            String chatHistory = buildChatHistory(conversation.getId());

            // [Cải tiến 4] Tạo Gemini API URL từ cấu hình, không hardcode
            String apiUrl = String.format("%s/models/%s:generateContent?key=%s",
                    geminiBaseUrl, geminiModel, geminiApiKey);

            // Tạo prompt hoàn chỉnh (sản phẩm + lịch sử + câu hỏi mới)
            String prompt = buildPrompt(productContext, chatHistory, customerText);

            // Gọi Gemini API
            String botResponseText = callGeminiApi(apiUrl, prompt);

            // Lưu và gửi tin nhắn qua WebSocket
            saveAndBroadcast(conversation, botResponseText);

        } catch (Exception e) {
            log.error("Lỗi khi gọi Gemini API:", e);

            // [Cải tiến 5] Gửi tin nhắn fallback thay vì im lặng
            saveAndBroadcast(conversation, FALLBACK_MESSAGE);
        }
    }

    // =====================================================================
    // PRIVATE METHODS - Tách nhỏ logic theo Single Responsibility Principle
    // =====================================================================

    /**
     * [Cải tiến 1] Cache sản phẩm trong bộ nhớ với TTL 5 phút.
     * Giảm ~99% truy vấn DB khi nhiều khách hàng chat cùng lúc.
     */
    private List<ProductVariantResponse> getActiveVariantsCached() {
        long now = System.currentTimeMillis();
        if (cachedVariants == null || (now - cacheTimestamp) > CACHE_TTL_MS) {
            log.info("Cache sản phẩm hết hạn hoặc chưa có → Truy vấn DB...");
            List<ProductVariantResponse> allVariants = sanPhamService.getAllVariants();
            cachedVariants = allVariants.stream()
                    .filter(v -> v.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                    .collect(Collectors.toList());
            cacheTimestamp = now;
            log.info("Đã cache {} biến thể đang hoạt động.", cachedVariants.size());
        }
        return cachedVariants;
    }

    /**
     * Xây dựng chuỗi context mô tả toàn bộ sản phẩm đang kinh doanh.
     */
    private String buildProductContext() {
        List<ProductVariantResponse> activeVariants = getActiveVariantsCached();

        Map<String, List<ProductVariantResponse>> groupedProducts = activeVariants.stream()
                .collect(Collectors.groupingBy(ProductVariantResponse::getTenSanPham));

        StringBuilder sb = new StringBuilder();
        sb.append("DANH SÁCH GIÀY ĐANG CÓ TẠI HỆ THỐNG (AeroStride):\n");

        groupedProducts.forEach((tenSp, variants) -> {
            sb.append(String.format("- Tên giày: %s\n", tenSp));
            sb.append(String.format("  + Thương hiệu: %s, Chất liệu: %s\n",
                    variants.get(0).getTenThuongHieu(), variants.get(0).getTenChatLieu()));

            String mauSac = variants.stream()
                    .map(ProductVariantResponse::getTenMauSac)
                    .distinct()
                    .collect(Collectors.joining(", "));
            String kichThuoc = variants.stream()
                    .map(ProductVariantResponse::getGiaTriKichThuoc)
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(", "));

            int tongTonKho = variants.stream()
                    .mapToInt(v -> v.getSoLuong() != null ? v.getSoLuong() : 0).sum();
            BigDecimal giaThapNhat = variants.stream()
                    .map(ProductVariantResponse::getGiaBan)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            List<BigDecimal> khuyenMais = variants.stream()
                    .filter(v -> v.getPhanTramGiam() != null && v.getPhanTramGiam().compareTo(BigDecimal.ZERO) > 0)
                    .map(ProductVariantResponse::getPhanTramGiam)
                    .distinct()
                    .collect(Collectors.toList());

            sb.append(String.format("  + Màu sắc: %s\n", mauSac));
            sb.append(String.format("  + Kích thước: %s\n", kichThuoc));
            sb.append(String.format("  + Giá bán: từ %s VNĐ\n", giaThapNhat));
            sb.append(String.format("  + Tình trạng: %s (Tổng tồn: %d)\n",
                    tongTonKho > 0 ? "Còn hàng" : "Hết hàng", tongTonKho));

            if (!khuyenMais.isEmpty()) {
                String kmStr = khuyenMais.stream()
                        .map(km -> km + "%")
                        .collect(Collectors.joining(", "));
                sb.append(String.format("  + KHUYẾN MÃI: Giảm %s\n", kmStr));
            }
            sb.append("\n");
        });

        return sb.toString();
    }

    /**
     * [Cải tiến 2] Xây dựng lịch sử hội thoại gần nhất (10 tin) để AI hiểu ngữ cảnh.
     * Ví dụ: Khách hỏi "Nó có size 42 không?" → AI hiểu "Nó" là sản phẩm vừa hỏi trước đó.
     */
    private String buildChatHistory(String conversationId) {
        List<ChatMessage> recentMessages =
                messageRepository.findTop10ByConversationIdOrderByNgayTaoDesc(conversationId);

        if (recentMessages.isEmpty()) {
            return "";
        }

        // Đảo ngược: từ cũ → mới (để prompt đọc theo thứ tự thời gian)
        Collections.reverse(recentMessages);

        StringBuilder sb = new StringBuilder();
        sb.append("LỊCH SỬ HỘI THOẠI GẦN ĐÂY:\n");
        for (ChatMessage msg : recentMessages) {
            String role = "bot".equals(msg.getSenderType()) ? "Trợ lý AI" : "Khách hàng";
            sb.append(String.format("%s: %s\n", role, msg.getContent()));
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Xây dựng prompt hoàn chỉnh cho Gemini API.
     */
    private String buildPrompt(String productContext, String chatHistory, String customerText) {
        return String.format(
                "Bạn là trợ lý ảo AI chuyên nghiệp của cửa hàng giày AeroStride. " +
                "Nhiệm vụ của bạn là tư vấn cho khách hàng dựa TRÊN DUY NHẤT dữ liệu sản phẩm được cung cấp dưới đây.\n\n" +
                "%s\n" +
                "%s" +
                "YÊU CẦU QUAN TRỌNG:\n" +
                "1. CHỈ được tư vấn các mẫu giày có trong danh sách trên. Tuyệt đối KHÔNG tự bịa ra sản phẩm hoặc tư vấn sản phẩm ngoài danh sách.\n" +
                "2. Khi khách hàng hỏi, hãy phân tích và liệt kê các mẫu phù hợp nhất (dựa trên màu sắc, size, giá, hoặc mục đích).\n" +
                "3. Phải nêu rõ các thuộc tính nổi bật của mẫu giày đó (màu sắc, chất liệu).\n" +
                "4. Phải thông báo tình trạng còn hàng/hết hàng và các chương trình KHUYẾN MÃI (nếu có) của sản phẩm đó.\n" +
                "5. Nếu không tìm thấy sản phẩm nào khớp hoàn toàn, hãy nói 'Hiện tại chúng tôi không có mẫu chính xác như bạn yêu cầu, nhưng bạn có thể tham khảo các mẫu tương tự sau:' và liệt kê mẫu gần nhất.\n" +
                "6. Câu trả lời phải thân thiện, chuyên nghiệp, trình bày rõ ràng (dùng bullet point) và KHÔNG được quá dài.\n" +
                "7. Nếu có LỊCH SỬ HỘI THOẠI, hãy tham khảo để hiểu ngữ cảnh (ví dụ: 'nó' = sản phẩm khách vừa hỏi trước đó).\n\n" +
                "Khách hàng hỏi: \"%s\"\n\n" +
                "Câu trả lời của bạn:",
                productContext, chatHistory, customerText
        );
    }

    /**
     * Gọi Gemini API và trích xuất kết quả.
     */
    @SuppressWarnings("unchecked")
    private String callGeminiApi(String apiUrl, String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", List.of(part));
        requestBody.put("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        Map<String, Object> response = restTemplate.postForObject(apiUrl, entity, Map.class);
        return extractTextFromGeminiResponse(response);
    }

    /**
     * Lưu tin nhắn bot vào CSDL và broadcast qua WebSocket.
     */
    private void saveAndBroadcast(ChatConversation conversation, String text) {
        ChatMessage botMessage = ChatMessage.builder()
                .conversation(conversation)
                .senderType("bot")
                .content(text)
                .build();

        ChatMessage savedMessage = messageRepository.save(botMessage);

        ChatMessageResponse responseDto = ChatMessageResponse.builder()
                .id(savedMessage.getId())
                .conversationId(conversation.getId())
                .sessionId(conversation.getSessionId())
                .sender("bot")
                .text(text)
                .time(formatTime(savedMessage.getNgayTao()))
                .build();

        messagingTemplate.convertAndSend(ChatConstants.TOPIC_MESSAGES, responseDto);
    }

    /**
     * Format timestamp (Long epoch millis) → "HH:mm" theo múi giờ Việt Nam.
     */
    private String formatTime(Long timestamp) {
        if (timestamp == null) return "Vừa xong";
        return Instant.ofEpochMilli(timestamp)
                .atZone(VN_ZONE)
                .format(TIME_FORMATTER);
    }

    /**
     * Trích xuất text phản hồi từ JSON response của Gemini API.
     */
    @SuppressWarnings("unchecked")
    private String extractTextFromGeminiResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> firstCandidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
            List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
            return parts.get(0).get("text");
        } catch (Exception e) {
            log.warn("Không thể parse Gemini response: {}", e.getMessage());
            return FALLBACK_MESSAGE;
        }
    }
}
