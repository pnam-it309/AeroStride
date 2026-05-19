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
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.concurrent.atomic.AtomicInteger;

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
    private String geminiApiKeyString;

    private List<String> apiKeysList = new ArrayList<>();
    private final AtomicInteger keyIndex = new AtomicInteger(0);

    @PostConstruct
    public void initApiKeys() {
        if (geminiApiKeyString != null && !geminiApiKeyString.isBlank()) {
            apiKeysList = Arrays.stream(geminiApiKeyString.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            log.info("Đã nạp thành công {} API Keys cho Gemini.", apiKeysList.size());
        }
    }

    private String getApiKey() {
        if (apiKeysList.isEmpty()) {
            return geminiApiKeyString;
        }
        int idx = keyIndex.getAndIncrement() % apiKeysList.size();
        if (idx < 0) idx = 0;
        return apiKeysList.get(idx);
    }

    @Value("${google.gemini.model:gemini-2.0-flash}")
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

        String activeKey = getApiKey();
        if (activeKey == null || activeKey.isBlank() || "your_gemini_api_key_here".equals(activeKey)) {
            log.warn("Gemini API Key chưa được cấu hình. Kích hoạt bot quy tắc làm mặc định.");
            try {
                List<ProductVariantResponse> activeVariants = getActiveVariantsCached(customerText);
                String fallbackResponse = generateRuleBasedResponse(customerText, activeVariants);
                saveAndBroadcast(conversation, fallbackResponse);
            } catch (Exception ex) {
                log.error("Lỗi nghiêm trọng khi chạy Bot quy tắc (không cấu hình key): {}", ex.getMessage(), ex);
                saveAndBroadcast(conversation, FALLBACK_MESSAGE);
            }
            return;
        }

        try {
            // [Tối ưu gói Free] Lấy danh sách sản phẩm từ cache lọc theo từ khóa và giới hạn 25 phần tử
            List<ProductVariantResponse> activeVariants = getActiveVariantsCached(customerText);

            String productContext = buildProductContextFromVariants(activeVariants);
            String chatHistory = buildChatHistory(conversation.getId());

            String apiUrl = String.format("%s/models/%s:generateContent?key=%s",
                    geminiBaseUrl, geminiModel, activeKey);

            String prompt = buildPrompt(productContext, chatHistory, customerText);

            String botResponseText = callGeminiApi(apiUrl, prompt);
            saveAndBroadcast(conversation, botResponseText);

        } catch (Exception e) {
            log.warn("Gemini API gặp lỗi (429 hoặc quá tải). Tự động kích hoạt bot quy tắc (Rule-based Fallback)...");
            try {
                List<ProductVariantResponse> activeVariants = getActiveVariantsCached(customerText);
                String fallbackResponse = generateRuleBasedResponse(customerText, activeVariants);
                saveAndBroadcast(conversation, fallbackResponse);
            } catch (Exception ex) {
                log.error("Lỗi nghiêm trọng khi chạy Bot quy tắc dự phòng: {}", ex.getMessage(), ex);
                saveAndBroadcast(conversation, FALLBACK_MESSAGE);
            }
        }
    }

    private List<ProductVariantResponse> getActiveVariantsCached(String text) {
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

        // [Tối ưu gói Free] Lọc thông minh theo từ khóa trong câu hỏi để gửi tối thiểu sản phẩm cần thiết, tránh quá tải token
        if (text == null || text.isBlank()) {
            return cachedVariants.stream().limit(15).collect(Collectors.toList());
        }

        String searchLower = text.toLowerCase();
        List<ProductVariantResponse> filtered = cachedVariants.stream()
                .filter(v -> {
                    boolean match = false;
                    if (v.getTenSanPham() != null && v.getTenSanPham().toLowerCase().contains(searchLower)) match = true;
                    if (v.getTenSanPhamDayDu() != null && v.getTenSanPhamDayDu().toLowerCase().contains(searchLower)) match = true;
                    if (v.getTenThuongHieu() != null && v.getTenThuongHieu().toLowerCase().contains(searchLower)) match = true;
                    if (v.getTenChatLieu() != null && v.getTenChatLieu().toLowerCase().contains(searchLower)) match = true;
                    if (v.getTenMauSac() != null && v.getTenMauSac().toLowerCase().contains(searchLower)) match = true;
                    if (v.getGiaTriKichThuoc() != null && v.getGiaTriKichThuoc().toLowerCase().contains(searchLower)) match = true;
                    return match;
                })
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            // Fallback lấy 15 sản phẩm đầu tiên
            return cachedVariants.stream().limit(15).collect(Collectors.toList());
        }

        return filtered.stream().limit(25).collect(Collectors.toList());
    }

    private String buildProductContextFromVariants(List<ProductVariantResponse> variants) {
        if (variants.isEmpty()) return "Hiện tại không có sản phẩm nào khả dụng.\n";

        Map<String, List<ProductVariantResponse>> groupedProducts = variants.stream()
                .collect(Collectors.groupingBy(ProductVariantResponse::getTenSanPham));

        StringBuilder sb = new StringBuilder();
        sb.append("DANH SÁCH GIÀY GỢI Ý (AeroStride):\n");

        groupedProducts.forEach((tenSp, vars) -> {
            sb.append(String.format("- %s (Thương hiệu: %s)\n", tenSp, vars.get(0).getTenThuongHieu()));
            String sizes = vars.stream().map(ProductVariantResponse::getGiaTriKichThuoc).distinct().sorted().collect(Collectors.joining(", "));
            String colors = vars.stream().map(ProductVariantResponse::getTenMauSac).distinct().collect(Collectors.joining(", "));
            BigDecimal minPrice = vars.stream().map(ProductVariantResponse::getGiaBan).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

            sb.append(String.format("  + Size: %s | Màu: %s | Giá từ: %s VNĐ\n", sizes, colors, minPrice));

            // Khuyến mãi nếu có
            vars.stream().filter(v -> v.getPhanTramGiam() != null && v.getPhanTramGiam().compareTo(BigDecimal.ZERO) > 0)
                .map(ProductVariantResponse::getPhanTramGiam).findFirst()
                .ifPresent(km -> sb.append(String.format("  + ĐANG GIẢM GIÁ: %s%%\n", km)));
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
                "1. CHỈ được tư vấn các mẫu giày có trong danh sách trên. Tuyệt đối KHÔNG tự bịa ra sản phẩm.\n" +
                "2. Nếu tìm thấy sản phẩm phù hợp, hãy đưa ra câu trả lời thân thiện VÀ ĐÍNH KÈM mã JSON của các sản phẩm đó vào cuối câu trả lời theo định dạng: [[PRODUCT_JSON:[{\"idSanPham\":\"...\", \"tenSanPham\":\"...\", \"giaBan\":..., \"tenThuongHieu\":\"...\", \"tenDanhMuc\":\"...\", \"hinhAnh\":\"...\", \"phanTramGiam\":..., \"soLuong\":...}]]] (liệt kê tối đa 3 sản phẩm phù hợp nhất).\n" +
                "3. Phải nêu rõ các thuộc tính nổi bật (màu sắc, chất liệu) trong phần văn bản.\n" +
                "4. Nếu không tìm thấy sản phẩm nào khớp hoàn toàn, hãy gợi ý mẫu gần nhất.\n" +
                "5. Giữ câu trả lời chuyên nghiệp, trình bày rõ ràng (dùng bullet point) và KHÔNG được quá dài.\n" +
                "6. Nếu có LỊCH SỬ HỘI THOẠI, hãy tham khảo để hiểu ngữ cảnh (ví dụ: 'nó' = sản phẩm khách vừa hỏi trước đó).\n\n" +
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
        // [Tối ưu gói Free] Đợi 3 giây trước khi thực hiện request để không bị quét lỗi 429 Free Tier (15 RPM)
        try {
            log.info("Đang trì hoãn 3s trước khi gọi Gemini API để tránh lỗi Rate Limit (429)...");
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Tiến trình ngủ bị gián đoạn: {}", e.getMessage());
        }

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

    /**
     * [Tính năng nâng cao] Chatbot quy tắc (Rule-based) dự phòng khi Gemini bị lỗi 429 hoặc quá tải.
     * Tự động lọc từ khóa (giá, size, màu sắc, thương hiệu) và sinh câu trả lời đính kèm JSON sản phẩm thật
     * để hiển thị giao diện thẻ sản phẩm mượt mà như AI thật.
     */
    private String generateRuleBasedResponse(String text, List<ProductVariantResponse> variants) {
        String lowerText = text.toLowerCase();
        StringBuilder sb = new StringBuilder();
        List<ProductVariantResponse> matchedVariants = new ArrayList<>();

        if (lowerText.contains("giá") || lowerText.contains("nhiêu") || lowerText.contains("tiền")) {
            sb.append("Dạ, AeroStride xin gửi bạn bảng giá tham khảo của các mẫu giày nổi bật hiện có tại cửa hàng:\n\n");
            
            Map<String, List<ProductVariantResponse>> grouped = variants.stream()
                    .collect(Collectors.groupingBy(ProductVariantResponse::getTenSanPham));
            
            int count = 0;
            for (Map.Entry<String, List<ProductVariantResponse>> entry : grouped.entrySet()) {
                if (count >= 3) break;
                String productName = entry.getKey();
                List<ProductVariantResponse> vars = entry.getValue();
                BigDecimal minPrice = vars.stream().map(ProductVariantResponse::getGiaBan).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                sb.append(String.format("• %s: giá từ %,.0f VNĐ\n", productName, minPrice));
                
                if (!vars.isEmpty()) {
                    matchedVariants.add(vars.get(0));
                }
                count++;
            }
            sb.append("\nBạn có thể xem chi tiết các mẫu và đặt mua trực tiếp ở thẻ sản phẩm bên dưới nhé!");
            
        } else if (lowerText.contains("size") || lowerText.contains("kích") || lowerText.contains("cỡ")) {
            sb.append("Dạ, các mẫu giày tại AeroStride hiện có sẵn các size phổ biến từ 38 đến 43.\n\n");
            
            Map<String, List<ProductVariantResponse>> grouped = variants.stream()
                    .collect(Collectors.groupingBy(ProductVariantResponse::getTenSanPham));
            
            int count = 0;
            for (Map.Entry<String, List<ProductVariantResponse>> entry : grouped.entrySet()) {
                if (count >= 3) break;
                String productName = entry.getKey();
                List<ProductVariantResponse> vars = entry.getValue();
                String sizes = vars.stream().map(ProductVariantResponse::getGiaTriKichThuoc).distinct().sorted().collect(Collectors.joining(", "));
                sb.append(String.format("• %s: hiện có size [%s]\n", productName, sizes));
                
                if (!vars.isEmpty()) {
                    matchedVariants.add(vars.get(0));
                }
                count++;
            }
            sb.append("\nBạn chọn size phù hợp để shop lên đơn ngay cho bạn nha!");

        } else if (lowerText.contains("màu") || lowerText.contains("đen") || lowerText.contains("trắng") || lowerText.contains("đỏ") || lowerText.contains("xanh")) {
            sb.append("Chào bạn! AeroStride sở hữu bộ sưu tập giày với màu sắc cực kỳ đa dạng và thời thượng (Đen, Trắng, Đỏ, Xanh...).\n\n");
            
            Map<String, List<ProductVariantResponse>> grouped = variants.stream()
                    .collect(Collectors.groupingBy(ProductVariantResponse::getTenSanPham));
            
            int count = 0;
            for (Map.Entry<String, List<ProductVariantResponse>> entry : grouped.entrySet()) {
                if (count >= 3) break;
                String productName = entry.getKey();
                List<ProductVariantResponse> vars = entry.getValue();
                String colors = vars.stream().map(ProductVariantResponse::getTenMauSac).distinct().collect(Collectors.joining(", "));
                sb.append(String.format("• %s: có các màu [%s]\n", productName, colors));
                
                if (!vars.isEmpty()) {
                    matchedVariants.add(vars.get(0));
                }
                count++;
            }
            sb.append("\nBạn có thể lướt xem và chọn màu yêu thích ở bên dưới nha!");

        } else {
            String matchedBrand = null;
            if (lowerText.contains("nike")) matchedBrand = "nike";
            else if (lowerText.contains("adidas")) matchedBrand = "adidas";
            else if (lowerText.contains("puma")) matchedBrand = "puma";
            else if (lowerText.contains("vans")) matchedBrand = "vans";
            
            if (matchedBrand != null) {
                String finalBrand = matchedBrand;
                List<ProductVariantResponse> brandVars = variants.stream()
                        .filter(v -> v.getTenThuongHieu() != null && v.getTenThuongHieu().toLowerCase().contains(finalBrand))
                        .collect(Collectors.toList());
                
                if (!brandVars.isEmpty()) {
                    sb.append(String.format("Dạ, shop đang có sẵn các mẫu giày thuộc thương hiệu %s cực hot dưới đây:\n\n", matchedBrand.toUpperCase()));
                    Map<String, List<ProductVariantResponse>> grouped = brandVars.stream()
                            .collect(Collectors.groupingBy(ProductVariantResponse::getTenSanPham));
                    
                    int count = 0;
                    for (Map.Entry<String, List<ProductVariantResponse>> entry : grouped.entrySet()) {
                        if (count >= 3) break;
                        String productName = entry.getKey();
                        List<ProductVariantResponse> vars = entry.getValue();
                        BigDecimal minPrice = vars.stream().map(ProductVariantResponse::getGiaBan).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                        sb.append(String.format("• %s: chỉ từ %,.0f VNĐ\n", productName, minPrice));
                        
                        if (!vars.isEmpty()) {
                            matchedVariants.add(vars.get(0));
                        }
                        count++;
                    }
                } else {
                    sb.append(String.format("Dạ, hiện tại dòng sản phẩm thương hiệu %s đã tạm hết hàng mất rồi ạ. Bạn tham khảo các mẫu bán chạy khác của AeroStride dưới đây nhé!\n", matchedBrand.toUpperCase()));
                }
            } else {
                sb.append("Xin chào! Trợ lý ảo AeroStride rất vui được hỗ trợ bạn. Dưới đây là một số mẫu giày bán chạy nhất tuần này tại cửa hàng:\n\n");
                
                Map<String, List<ProductVariantResponse>> grouped = variants.stream()
                        .collect(Collectors.groupingBy(ProductVariantResponse::getTenSanPham));
                
                int count = 0;
                for (Map.Entry<String, List<ProductVariantResponse>> entry : grouped.entrySet()) {
                    if (count >= 3) break;
                    String productName = entry.getKey();
                    List<ProductVariantResponse> vars = entry.getValue();
                    BigDecimal minPrice = vars.stream().map(ProductVariantResponse::getGiaBan).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                    sb.append(String.format("• %s: giá từ %,.0f VNĐ (đang bán chạy)\n", productName, minPrice));
                    
                    if (!vars.isEmpty()) {
                        matchedVariants.add(vars.get(0));
                    }
                    count++;
                }
            }
        }

        if (!matchedVariants.isEmpty()) {
            sb.append("\n\n[[PRODUCT_JSON:[");
            String jsonArray = matchedVariants.stream().map(v -> {
                String imgUrl = (v.getImages() != null && !v.getImages().isEmpty()) ? v.getImages().get(0).getDuongDanAnh() : "";
                return String.format(
                        "{\"idSanPham\":\"%s\",\"tenSanPham\":\"%s\",\"giaBan\":%s,\"tenThuongHieu\":\"%s\",\"tenDanhMuc\":\"Giày\",\"hinhAnh\":\"%s\",\"phanTramGiam\":%s,\"soLuong\":%d}",
                        v.getIdSanPham(),
                        v.getTenSanPham().replace("\"", "\\\""),
                        v.getGiaBan(),
                        v.getTenThuongHieu().replace("\"", "\\\""),
                        imgUrl,
                        v.getPhanTramGiam() != null ? v.getPhanTramGiam() : "0",
                        v.getSoLuong()
                );
            }).collect(Collectors.joining(","));
            sb.append(jsonArray).append("]]]");
        }

        return sb.toString();
    }
}
