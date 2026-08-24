package com.example.be.core.admin.chat.service.impl;

import com.example.be.core.admin.chat.model.TinNhanResponse;
import com.example.be.core.admin.chat.repository.AdminTinNhanRepository;
import com.example.be.core.admin.chat.service.AiChatService;
import com.example.be.core.admin.sanpham.model.response.ProductVariantResponse;
import com.example.be.core.admin.sanpham.service.AdminSanPhamService;
import com.example.be.entity.CuocHoiThoai;
import com.example.be.entity.TinNhan;
import com.example.be.infrastructure.constants.ChatConstants;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.constants.AiChatPrompts;
import com.example.be.core.common.chat.local.service.AiLocalService;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.chat.client.ChatClient;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
 * 6. Tích hợp AI nội bộ (Local AI) làm dự phòng khi hết request hoặc không có key.
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private final AdminSanPhamService sanPhamService;
    private final AdminTinNhanRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiLocalService aiLocalService;
    private final ChatClient chatClient;

    @Value("${openai.api-key:}")
    private String openAiApiKeyString;

    private List<String> openAiApiKeysList = new ArrayList<>();
    private final AtomicInteger openAiKeyIndex = new AtomicInteger(0);

    @PostConstruct
    public void initApiKeys() {
        if (openAiApiKeyString != null && !openAiApiKeyString.isBlank()) {
            openAiApiKeysList = Arrays.stream(openAiApiKeyString.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            log.info("Đã nạp thành công {} API Keys cho OpenAI.", openAiApiKeysList.size());
        }
    }

    private String getOpenAiApiKey() {
        if (openAiApiKeysList.isEmpty()) {
            return openAiApiKeyString;
        }
        int idx = openAiKeyIndex.getAndIncrement() % openAiApiKeysList.size();
        if (idx < 0) idx = 0;
        return openAiApiKeysList.get(idx);
    }

    @Value("${openai.model:gpt-4o-mini}")
    private String openAiModel;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // === In-Memory Cache cho danh sách sản phẩm ===
    private volatile List<ProductVariantResponse> cachedVariants;
    private volatile long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 phút
    private static final int MAX_CONTEXT_PRODUCTS = 20; // Giới hạn số lượng sản phẩm gửi lên AI để tránh quá tải

    // Circuit Breaker tracker for unhealthy model APIs
    private final Map<String, Long> unhealthyModels = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long UNHEALTHY_COOLDOWN_MS = 2 * 60 * 1000; // 2 minutes

    private boolean isModelHealthy(String modelName) {
        Long unhealthyUntil = unhealthyModels.get(modelName);
        if (unhealthyUntil == null) {
            return true;
        }
        if (System.currentTimeMillis() > unhealthyUntil) {
            unhealthyModels.remove(modelName);
            return true;
        }
        return false;
    }

    private void markModelUnhealthy(String modelName) {
        log.warn("Mô hình AI {} gặp sự cố, đánh dấu không khỏe mạnh trong 2 phút để ngắt mạch (Circuit Breaker) chuyển đổi tức thì.", modelName);
        unhealthyModels.put(modelName, System.currentTimeMillis() + UNHEALTHY_COOLDOWN_MS);
    }

    /**
     * Constructor injection với RestTemplate được cấu hình Timeout.
     * Không dùng @RequiredArgsConstructor vì RestTemplate cần builder pattern.
     */
    public AiChatServiceImpl(
            AdminSanPhamService sanPhamService,
            AdminTinNhanRepository messageRepository,
            SimpMessagingTemplate messagingTemplate,
            RestTemplateBuilder restTemplateBuilder,
            AiLocalService aiLocalService,
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            @Value("${google.gemini.timeout-ms:8000}") int timeoutMs
    ) {
        this.sanPhamService = sanPhamService;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.aiLocalService = aiLocalService;

        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        this.chatClient = (builder != null) ? builder.build() : null;

        // [Cải tiến 3] RestTemplate có Timeout 8s → chống treo luồng @Async, nhanh chóng chuyển Local AI
        int effectiveTimeout = Math.min(timeoutMs, 8000);
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofMillis(effectiveTimeout))
                .readTimeout(Duration.ofMillis(effectiveTimeout))
                .build();
    }

    @Async
    @Override
    public void generateAndSendResponse(CuocHoiThoai conversation, String customerText) {
        generateAndSendResponse(conversation, customerText, null);
    }

    @Async
    @Override
    public void generateAndSendResponse(CuocHoiThoai conversation, String customerText, String imageBase64) {
        log.info("AI đang xử lý tin nhắn: {}", customerText);

        // [Handoff Interceptor] Phát hiện từ khóa yêu cầu gặp nhân viên để phản hồi tức thì
        String lowerInput = customerText.toLowerCase().trim();
        if (lowerInput.contains("nhân viên") || lowerInput.contains("nhan vien") || 
            lowerInput.contains("người thật") || lowerInput.contains("nguoi that") || 
            lowerInput.contains("admin") || lowerInput.contains("gặp nhân viên") || 
            lowerInput.contains("gặp hỗ trợ") || lowerInput.contains("gap ho tro") || 
            lowerInput.contains("gọi hỗ trợ") || lowerInput.contains("goi ho tro") || 
            lowerInput.contains("liên hệ hỗ trợ") || lowerInput.contains("lien he ho tro") || 
            lowerInput.contains("kết nối hỗ trợ") || lowerInput.contains("ket noi ho tro") || 
            lowerInput.contains("nói chuyện với hỗ trợ") || lowerInput.contains("noi chuyen voi ho tro") || 
            lowerInput.contains("goi admin") || lowerInput.contains("gọi admin")) {
            
            log.info("Đã phát hiện từ khóa gặp nhân viên trong: '{}'. Thực hiện phản hồi handoff tức thì.", customerText);
            String handoffResponse = AiChatPrompts.HANDOFF_RESPONSE;
            
            // Đính kèm các câu hỏi gợi ý phù hợp trong lúc chờ nhân viên hỗ trợ
            List<String> waitingSuggs = List.of(
                "Xem giờ mở cửa của showroom",
                "Chính sách bảo hành và đổi trả",
                "Xem địa chỉ showroom AeroStride"
            );
            
            try {
                handoffResponse += "\n\n[[SUGGESTIONS:" + objectMapper.writeValueAsString(waitingSuggs) + "]]";
            } catch (Exception e) {
                log.error("Lỗi serialize suggestions cho handoff: {}", e.getMessage());
            }
            
            saveAndBroadcast(conversation, handoffResponse);
            return;
        }

        // Fallback customerText if only image was sent
        String effectiveCustomerText = customerText;
        if ((effectiveCustomerText == null || effectiveCustomerText.trim().isEmpty()) && StringUtils.hasText(imageBase64)) {
            effectiveCustomerText = "Tôi vừa gửi hình ảnh mẫu giày này, bạn hãy quan sát và tư vấn chi tiết giúp tôi nhé!";
        } else if (effectiveCustomerText == null) {
            effectiveCustomerText = "";
        }

        // 1. Lấy danh sách sản phẩm thông minh dựa trên Index Database
        List<ProductVariantResponse> relevantVariants = getActiveVariantsIntelligent(effectiveCustomerText);
        String productContext = buildProductContextFromVariants(relevantVariants);
        String chatHistory = buildChatHistory(conversation.getId());
        String prompt = buildPrompt(chatHistory, productContext, effectiveCustomerText, conversation);

        // --- Cố gắng gọi OpenAI / Vision API (Primary Model) ---
        String activeOpenAiKey = getOpenAiApiKey();
        boolean hasOpenAiKey = activeOpenAiKey != null && !activeOpenAiKey.isBlank() && !"your_openai_api_key_here".equals(activeOpenAiKey);

        if (hasOpenAiKey && isModelHealthy("OPENAI")) {
            try {
                log.info("Khởi động gọi OpenAI API (có ảnh: {})...", StringUtils.hasText(imageBase64));
                String apiUrl = String.format("%s/chat/completions", openAiBaseUrl);
                
                // Giới hạn thời gian tối đa 8 giây cho Cloud AI, nếu quá thời gian lập tức chuyển sang Local AI
                String botResponseText = CompletableFuture.supplyAsync(() -> callOpenAiApi(apiUrl, prompt, imageBase64))
                        .get(8000, TimeUnit.MILLISECONDS);
                        
                saveAndBroadcast(conversation, botResponseText);
                log.info("OpenAI phản hồi thành công.");
                return; // Xử lý xong, kết thúc!
            } catch (TimeoutException te) {
                log.warn("OpenAI API phản hồi quá chậm (>8s). Tự động kích hoạt Local AI dự phòng ngay lập tức!");
                markModelUnhealthy("OPENAI");
            } catch (Exception e) {
                log.warn("OpenAI API gặp sự cố. Tự động chuyển đổi sang Local AI: {}", e.getMessage());
                markModelUnhealthy("OPENAI");
            }
        }

        // --- Cố gắng gọi LOCAL AI (Fallback) ---
        log.info("Tự động chuyển đổi: Kích hoạt AI nội bộ cục bộ làm dự phòng...");
        try {
            String localResponse = aiLocalService.generateResponse(effectiveCustomerText, conversation.getId());
            if (StringUtils.hasText(imageBase64) && !localResponse.contains("ảnh") && !localResponse.contains("hình")) {
                localResponse = "Cảm ơn bạn đã gửi hình ảnh mẫu giày! AeroStride đã nhận được ảnh và đang đối chiếu với các mẫu giày sẵn có.\n\n" + localResponse;
            }
            saveAndBroadcast(conversation, localResponse);
            log.info("AI nội bộ phản hồi thành công.");
        } catch (Exception ex) {
            log.error("Lỗi nghiêm trọng khi chạy AI nội bộ dự phòng: {}", ex.getMessage(), ex);
            saveAndBroadcast(conversation, AiChatPrompts.FALLBACK_MESSAGE);
        }
    }

    private int iterativeWordMatch(String[] queryWords, String attribute) {
        if (queryWords == null || attribute == null) {
            return 0;
        }
        String attrLower = attribute.toLowerCase();
        int score = 0;
        for (String word : queryWords) {
            if (word.length() >= 2 && attrLower.contains(word)) {
                score++;
            }
        }
        return score;
    }

    private int calculateMatchScore(ProductVariantResponse v, String queryLower, String[] queryWords, BigDecimal targetPrice) {
        int score = 0;

        // 1. Khớp nguyên cụm từ (Exact phrase matching)
        if (v.getTenSanPham() != null && queryLower.contains(v.getTenSanPham().toLowerCase())) score += 50;
        if (v.getTenSanPhamDayDu() != null && queryLower.contains(v.getTenSanPhamDayDu().toLowerCase())) score += 60;
        if (v.getTenThuongHieu() != null && queryLower.contains(v.getTenThuongHieu().toLowerCase())) score += 30;
        if (v.getTenMauSac() != null && queryLower.contains(v.getTenMauSac().toLowerCase())) score += 30;
        if (v.getGiaTriKichThuoc() != null && queryLower.contains(v.getGiaTriKichThuoc().toLowerCase())) score += 40;
        if (v.getTenChatLieu() != null && queryLower.contains(v.getTenChatLieu().toLowerCase())) score += 15;

        // 2. Khớp theo giá (Price matching)
        if (targetPrice != null && v.getGiaBan() != null) {
            BigDecimal diff = v.getGiaBan().subtract(targetPrice).abs();
            // Nếu giá khớp chính xác hoặc lệch dưới 100k, cộng điểm cực cao
            if (diff.compareTo(new BigDecimal("100000")) <= 0) {
                score += 100;
            } else if (diff.compareTo(new BigDecimal("500000")) <= 0) {
                score += 30;
            }
        }

        // 3. Khớp từng từ đơn lẻ (Iterative word-by-word token matching)
        if (v.getTenSanPham() != null) {
            score += iterativeWordMatch(queryWords, v.getTenSanPham()) * 10;
        }
        if (v.getTenThuongHieu() != null) {
            score += iterativeWordMatch(queryWords, v.getTenThuongHieu()) * 8;
        }
        if (v.getTenMauSac() != null) {
            score += iterativeWordMatch(queryWords, v.getTenMauSac()) * 8;
        }
        if (v.getGiaTriKichThuoc() != null) {
            score += iterativeWordMatch(queryWords, v.getGiaTriKichThuoc()) * 10;
        }
        if (v.getTenChatLieu() != null) {
            score += iterativeWordMatch(queryWords, v.getTenChatLieu()) * 5;
        }

        return score;
    }

    /**
     * Lấy danh sách sản phẩm thông minh bằng cách kết hợp trích xuất thông tin từ tin nhắn
     * và truy vấn trực tiếp vào Database thông qua Index.
     */
    private List<ProductVariantResponse> getActiveVariantsIntelligent(String text) {
        if (text == null || text.isBlank()) {
            return getActiveVariantsCached(null);
        }

        String queryLower = text.toLowerCase().trim();
        
        // 1. Trích xuất giá (ví dụ: 130k, 1.2tr, 500000)
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        
        try {
            // Regex phát hiện giá (đơn giản hóa cho AI Context)
            if (queryLower.matches(".*\\d+[kK].*")) {
                String val = queryLower.replaceAll("[^0-9]", "");
                maxPrice = new BigDecimal(val).multiply(new BigDecimal("1000"));
                // Thêm biên độ cho linh hoạt
                minPrice = maxPrice.subtract(new BigDecimal("200000")).max(BigDecimal.ZERO);
                maxPrice = maxPrice.add(new BigDecimal("200000"));
            } else if (queryLower.matches(".*\\d{5,}.*")) {
                String val = queryLower.replaceAll("[^0-9]", "");
                BigDecimal target = new BigDecimal(val);
                minPrice = target.subtract(new BigDecimal("200000")).max(BigDecimal.ZERO);
                maxPrice = target.add(new BigDecimal("200000"));
            }
        } catch (Exception e) {
            log.warn("Lỗi trích xuất giá: {}", e.getMessage());
        }

        // 2. Trích xuất từ khóa thương hiệu hoặc tên
        String keyword = null;
        List<String> commonBrands = List.of("nike", "adidas", "puma", "vans", "converse", "jordan");
        for (String brand : commonBrands) {
            if (queryLower.contains(brand)) {
                keyword = brand;
                break;
            }
        }

        // 3. Truy vấn Database thông qua Index (Surgical retrieval)
        log.info("Đang truy vấn Database AI Search (Keyword: {}, Price: {} - {})", keyword, minPrice, maxPrice);
        List<ProductVariantResponse> results = sanPhamService.searchVariantsForAi(keyword, minPrice, maxPrice, MAX_CONTEXT_PRODUCTS);

        if (results.isEmpty()) {
            // Nếu không tìm thấy bằng surgical search, quay lại dùng cache top sản phẩm hot
            log.info("Surgical search không có kết quả, quay lại dùng cache top sản phẩm.");
            return getActiveVariantsCached(text);
        }

        return results;
    }

    private List<ProductVariantResponse> getActiveVariantsCached(String text) {
        if (text == null || text.isBlank()) {
            return sanPhamService.searchVariantsForAi(null, null, null, MAX_CONTEXT_PRODUCTS);
        }

        long now = System.currentTimeMillis();
        if (cachedVariants == null || (now - cacheTimestamp) > CACHE_TTL_MS) {
            log.info("Cache sản phẩm hết hạn hoặc chưa có → Truy vấn DB top sản phẩm...");
            List<ProductVariantResponse> allVariants = sanPhamService.searchVariantsForAi(null, null, null, 50);
            cachedVariants = allVariants.stream()
                    .filter(v -> v.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                    .collect(Collectors.toList());
            cacheTimestamp = now;
            log.info("Đã cache {} biến thể đang hoạt động cho AI.", cachedVariants.size());
        }

        String queryLower = text.toLowerCase().trim();
        String[] queryWords = queryLower.split("\\s+");

        // Phát hiện giá mục tiêu (ví dụ: 130k, 130000, 1tr2)
        BigDecimal targetPrice = null;
        try {
            if (queryLower.matches(".*\\d+[kK].*")) {
                String val = queryLower.replaceAll("[^0-9]", "");
                targetPrice = new BigDecimal(val).multiply(new BigDecimal("1000"));
            } else if (queryLower.matches(".*\\d{5,}.*")) {
                String val = queryLower.replaceAll("[^0-9]", "");
                targetPrice = new BigDecimal(val);
            }
        } catch (Exception e) {
            log.warn("Lỗi trích xuất giá từ query: {}", e.getMessage());
        }

        // Nhận diện từ khóa yêu cầu sắp xếp theo giá
        boolean sortByPriceAsc = queryLower.contains("thấp đến cao") || queryLower.contains("rẻ nhất") || 
                                 queryLower.contains("tăng dần") || queryLower.contains("giá rẻ") || 
                                 queryLower.contains("thấp nhất") || queryLower.contains("giá tốt") ||
                                 queryLower.contains("bình dân");

        boolean sortByPriceDesc = queryLower.contains("cao đến thấp") || queryLower.contains("đắt nhất") || 
                                  queryLower.contains("giảm dần") || queryLower.contains("cao nhất") || 
                                  queryLower.contains("đắt tiền") || queryLower.contains("sang chảnh") ||
                                  queryLower.contains("cao cấp");

        // Sử dụng đệ quy và tính điểm trùng khớp để lấy chính xác sản phẩm mong muốn
        class ScoredVariant {
            final ProductVariantResponse variant;
            int score;
            ScoredVariant(ProductVariantResponse variant, int score) {
                this.variant = variant;
                this.score = score;
            }
        }

        List<ScoredVariant> scoredList = new ArrayList<>();
        BigDecimal finalTargetPrice = targetPrice;
        for (ProductVariantResponse v : cachedVariants) {
            int score = calculateMatchScore(v, queryLower, queryWords, finalTargetPrice);
            // Nếu khách hỏi giá tăng/giảm dần chung chung, hoặc hỏi giá mà không khớp từ khóa đặc thù nào khác,
            // ta vẫn cho điểm cơ bản = 1 để đưa vào danh sách sắp xếp giá
            if (score == 0 && (sortByPriceAsc || sortByPriceDesc)) {
                score = 1;
            }
            if (score > 0) {
                scoredList.add(new ScoredVariant(v, score));
            }
        }

        // Thực hiện sắp xếp theo yêu cầu của khách hàng
        if (sortByPriceAsc) {
            // Giá tăng dần (thấp đến cao). Nếu giá bằng nhau thì xếp theo điểm trùng khớp giảm dần
            scoredList.sort((a, b) -> {
                java.math.BigDecimal priceA = a.variant.getGiaBan() != null ? a.variant.getGiaBan() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal priceB = b.variant.getGiaBan() != null ? b.variant.getGiaBan() : java.math.BigDecimal.ZERO;
                int priceCompare = priceA.compareTo(priceB);
                if (priceCompare != 0) {
                    return priceCompare;
                }
                return Integer.compare(b.score, a.score);
            });
        } else if (sortByPriceDesc) {
            // Giá giảm dần (cao xuống thấp). Nếu giá bằng nhau thì xếp theo điểm trùng khớp giảm dần
            scoredList.sort((a, b) -> {
                java.math.BigDecimal priceA = a.variant.getGiaBan() != null ? a.variant.getGiaBan() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal priceB = b.variant.getGiaBan() != null ? b.variant.getGiaBan() : java.math.BigDecimal.ZERO;
                int priceCompare = priceB.compareTo(priceA);
                if (priceCompare != 0) {
                    return priceCompare;
                }
                return Integer.compare(b.score, a.score);
            });
        } else {
            // Sắp xếp mặc định theo điểm phù hợp giảm dần
            scoredList.sort((a, b) -> Integer.compare(b.score, a.score));
        }

        List<ProductVariantResponse> filtered = scoredList.stream()
                .map(sv -> sv.variant)
                .limit(MAX_CONTEXT_PRODUCTS) // [GIỚI HẠN] Chỉ lấy tối đa 20 sản phẩm phù hợp nhất
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            // Lấy top 15 sản phẩm mới nhất làm dữ liệu dự phòng thay vì trả về toàn bộ danh mục (gây treo AI)
            return cachedVariants.stream().limit(15).collect(Collectors.toList());
        }

        return filtered;
    }

    private String buildProductContextFromVariants(List<ProductVariantResponse> variants) {
        if (variants.isEmpty()) return "Hiện tại không có sản phẩm nào khả dụng.\n";

        java.util.Map<String, List<ProductVariantResponse>> groupedProducts = variants.stream()
                .filter(v -> v.getTenSanPham() != null)
                .collect(Collectors.groupingBy(ProductVariantResponse::getTenSanPham));

        StringBuilder sb = new StringBuilder();
        sb.append("DANH SÁCH GIÀY GỢI Ý (AeroStride):\n");

        groupedProducts.forEach((tenSp, vars) -> {
            if (sb.length() > 15000) return;

            String brand = vars.get(0).getTenThuongHieu() != null ? vars.get(0).getTenThuongHieu() : "AeroStride";
            sb.append(String.format("- %s (Thương hiệu: %s)\n", tenSp, brand));
            String sizes = vars.stream()
                    .map(v -> v.getGiaTriKichThuoc() != null ? String.valueOf(v.getGiaTriKichThuoc()) : "N/A")
                    .distinct().sorted().collect(Collectors.joining(", "));
            String colors = vars.stream()
                    .map(v -> v.getTenMauSac() != null ? v.getTenMauSac() : "N/A")
                    .distinct().collect(Collectors.joining(", "));
            java.math.BigDecimal minPrice = vars.stream()
                    .map(v -> v.getGiaBan() != null ? v.getGiaBan() : java.math.BigDecimal.ZERO)
                    .min(java.math.BigDecimal::compareTo).orElse(java.math.BigDecimal.ZERO);

            sb.append(String.format("  + Size: %s | Màu: %s | Giá từ: %s VNĐ\n", sizes, colors, minPrice));

            vars.stream().filter(v -> v.getPhanTramGiam() != null && v.getPhanTramGiam().compareTo(java.math.BigDecimal.ZERO) > 0)
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
        List<TinNhan> recentMessages =
                messageRepository.findTop10ByCuocHoiThoai_IdOrderByNgayTaoDesc(conversationId);

        // Giảm số lượng tin nhắn từ 10 xuống 5 để tối ưu hóa context size (giúp phản hồi nhanh hơn)
        if (recentMessages.size() > 5) {
            recentMessages = recentMessages.subList(0, 5);
        }

        if (recentMessages.isEmpty()) {
            return "";
        }

        // Đảo ngược: từ cũ → mới (để prompt đọc theo thứ tự thời gian)
        Collections.reverse(recentMessages);

        StringBuilder sb = new StringBuilder();
        sb.append("LỊCH SỬ HỘI THOẠI GẦN ĐÂY:\n");
        for (TinNhan msg : recentMessages) {
            String role = "bot".equals(msg.getLoaiNguoiGui()) ? "Trợ lý AI" : "Khách hàng";
            sb.append(String.format("%s: %s\n", role, msg.getNoiDung()));
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Xây dựng prompt hoàn chỉnh cho AI API.
     */
    private String buildPrompt(String chatHistory, String productContext, String customerText, CuocHoiThoai conversation) {
        StringBuilder userContext = new StringBuilder();
        userContext.append("THÔNG TIN NGƯỜI DÙNG HIỆN TẠI:\n");
        userContext.append("- Mã phiên (Session ID): ").append(conversation.getMaPhien() != null ? conversation.getMaPhien() : "Không có").append("\n");
        if (conversation.getKhachHang() != null) {
            com.example.be.entity.KhachHang kh = conversation.getKhachHang();
            userContext.append("- ID Người dùng: ").append(kh.getId()).append("\n");
            userContext.append("- Họ tên: ").append(kh.getTen() != null ? kh.getTen() : "Không rõ").append("\n");
            userContext.append("- Số điện thoại: ").append(kh.getSdt() != null ? kh.getSdt() : "Không rõ").append("\n");
            userContext.append("- Email: ").append(kh.getEmail() != null ? kh.getEmail() : "Không rõ").append("\n");
        } else {
            userContext.append("- Trạng thái: Khách vãng lai (Chưa đăng nhập)\n");
        }
        userContext.append("\n");

        String fullContext = userContext.toString()
                + (productContext != null ? productContext : "") + "\n"
                + AiChatPrompts.STORE_POLICIES_CONTEXT + "\n"
                + (chatHistory != null ? chatHistory : "");

        return String.format(
                AiChatPrompts.MAIN_SYSTEM_PROMPT,
                fullContext, customerText
        );
    }



    /**
     * Lưu tin nhắn bot vào CSDL và broadcast qua WebSocket.
     */
    private void saveAndBroadcast(CuocHoiThoai conversation, String text) {
        TinNhan botMessage = TinNhan.builder()
                .cuocHoiThoai(conversation)
                .loaiNguoiGui("bot")
                .noiDung(text)
                .build();

        TinNhan savedMessage = messageRepository.save(botMessage);

        TinNhanResponse responseDto = TinNhanResponse.builder()
                .id(savedMessage.getId())
                .idCuocHoiThoai(conversation.getId())
                .maPhien(conversation.getMaPhien())
                .nguoiGui("bot")
                .noiDung(text)
                .thoiGian(formatTime(savedMessage.getNgayTao()))
                .build();

        messagingTemplate.convertAndSend(ChatConstants.TOPIC_MESSAGES, responseDto);
    }

    @Override
    public String summarizeChat(CuocHoiThoai conversation) {
        String chatHistory = buildChatHistory(conversation.getId());
        String prompt = "Dựa trên lịch sử hội thoại sau, hãy tóm tắt nội dung chính (khoảng 3-4 dòng) và đánh dấu xem có cần nhân viên chú ý đặc biệt không (ví dụ: đòi hoàn tiền, khiếu nại, ...):\n\n" + chatHistory;
        
        String activeOpenAiKey = getOpenAiApiKey();
        if (activeOpenAiKey != null && !activeOpenAiKey.isBlank() && !"your_openai_api_key_here".equals(activeOpenAiKey)) {
            try {
                String apiUrl = String.format("%s/chat/completions", openAiBaseUrl);
                return callOpenAiApi(apiUrl, prompt);
            } catch (Exception e) {
                log.error("Lỗi tóm tắt bằng OpenAI/Ollama: {}", e.getMessage());
            }
        }
        return "Không thể tóm tắt hội thoại lúc này do lỗi kết nối AI.";
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
     * Gọi OpenAI ChatGPT API và trích xuất kết quả.
     */
    @SuppressWarnings("unchecked")
    private String callOpenAiApi(String apiUrl, String prompt) {
        return callOpenAiApi(apiUrl, prompt, null);
    }

    /**
     * Gọi OpenAI ChatGPT API (hỗ trợ Vision Multimodal khi có ảnh) và trích xuất kết quả.
     */
    @SuppressWarnings("unchecked")
    private String callOpenAiApi(String apiUrl, String prompt, String imageBase64) {
        if (this.chatClient != null && !StringUtils.hasText(imageBase64)) {
            log.info("Sử dụng Spring AI ChatClient (với Tool searchProducts) để gọi OpenAI...");
            return chatClient.prompt()
                    .user(prompt)
                    .tools("searchProducts", "getStorePolicies")
                    .call()
                    .content();
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", openAiModel);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");

        if (StringUtils.hasText(imageBase64)) {
            List<Map<String, Object>> contentList = new ArrayList<>();

            // 1. Text part
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", prompt + "\n\n[QUAN TRỌNG - THỊ GIÁC AI]: Khách hàng vừa gửi kèm một HÌNH ẢNH mẫu giày/sản phẩm. Bạn hãy quan sát kỹ các đặc điểm trong ảnh (màu sắc, phom dáng, loại đế, thương hiệu/logo nếu có, phong cách thể thao/chạy bộ/lifestyle) để phân tích chi tiết, đưa ra nhận xét chuyên nghiệp và gợi ý các mẫu giày tương tự đang có sẵn tại cửa hàng AeroStride!");
            contentList.add(textPart);

            // 2. Image part (OpenAI Vision standard format)
            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("type", "image_url");
            Map<String, String> imageUrlMap = new HashMap<>();
            String pureBase64 = imageBase64.contains(",") ? imageBase64.split(",", 2)[1] : imageBase64;
            imageUrlMap.put("url", "data:image/jpeg;base64," + pureBase64);
            imagePart.put("image_url", imageUrlMap);
            contentList.add(imagePart);

            message.put("content", contentList);
        } else {
            message.put("content", prompt);
        }

        requestBody.put("messages", List.of(message));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getOpenAiApiKey());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        Map<String, Object> response = restTemplate.postForObject(apiUrl, entity, Map.class);
        return extractTextFromOpenAiResponse(response);
    }

    /**
     * Trích xuất text phản hồi từ JSON response của OpenAI API.
     */
    @SuppressWarnings("unchecked")
    private String extractTextFromOpenAiResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
            String responseText = (String) message.get("content");
            if (responseText == null || responseText.isBlank()) {
                throw new RuntimeException("Phản hồi từ OpenAI trống.");
            }
            return responseText;
        } catch (Exception e) {
            log.warn("Không thể parse OpenAI response: {}", e.getMessage());
            throw new RuntimeException("Lỗi phân tích cú pháp phản hồi từ OpenAI", e);
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

        // Kiểm tra dữ liệu sản phẩm trong cơ sở dữ liệu
        if (variants == null || variants.isEmpty()) {
            return AiChatPrompts.RULE_NO_PRODUCTS;
        }

        // Quy tắc chào hỏi và đáp lại
        boolean isGreeting = lowerText.contains("chào") || lowerText.contains("hello") || lowerText.contains("hi ") || lowerText.equals("hi") || lowerText.contains("chúc buổi");
        boolean isThankYou = lowerText.contains("cảm ơn") || lowerText.contains("cám ơn") || lowerText.contains("thank") || lowerText.contains("tks");
        boolean isGoodbye = lowerText.contains("tạm biệt") || lowerText.contains("bye") || lowerText.contains("hẹn gặp lại");
        boolean isAcknowledge = lowerText.equals("ok") || lowerText.equals("dạ") || lowerText.equals("vâng") || lowerText.contains("tốt quá") || lowerText.contains("được rồi");

        if (isGreeting) {
            sb.append(AiChatPrompts.RULE_GREETING_START);
            
            // Đính kèm các sản phẩm hot bán chạy để chào mừng khách hàng
            Map<String, List<ProductVariantResponse>> grouped = variants.stream()
                    .collect(Collectors.groupingBy(ProductVariantResponse::getTenSanPham));
            int count = 0;
            for (Map.Entry<String, List<ProductVariantResponse>> entry : grouped.entrySet()) {
                if (count >= 3) break;
                if (!entry.getValue().isEmpty()) {
                    matchedVariants.add(entry.getValue().get(0));
                    count++;
                }
            }
            sb.append(AiChatPrompts.RULE_GREETING_END);

        } else if (isThankYou) {
            sb.append(AiChatPrompts.RULE_THANK_YOU);

        } else if (isGoodbye) {
            sb.append(AiChatPrompts.RULE_GOODBYE);

        } else if (isAcknowledge) {
            sb.append(AiChatPrompts.RULE_ACKNOWLEDGE);

        } else if (lowerText.contains("giá") || lowerText.contains("nhiêu") || lowerText.contains("tiền")) {
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

        } else if (lowerText.contains("chất liệu") || lowerText.contains("làm bằng") || lowerText.contains("da") || lowerText.contains("vải") || lowerText.contains("cao su")) {
            sb.append("Dạ, AeroStride sử dụng các chất liệu cao cấp tuyển chọn (Da bò bền bỉ, Vải Mesh thoáng khí, đế Cao su êm chân). Dưới đây là chất liệu của từng mẫu:\n\n");
            
            Map<String, List<ProductVariantResponse>> grouped = variants.stream()
                    .collect(Collectors.groupingBy(ProductVariantResponse::getTenSanPham));
            
            int count = 0;
            for (Map.Entry<String, List<ProductVariantResponse>> entry : grouped.entrySet()) {
                if (count >= 3) break;
                String productName = entry.getKey();
                List<ProductVariantResponse> vars = entry.getValue();
                String material = vars.get(0).getTenChatLieu() != null ? vars.get(0).getTenChatLieu() : "Da cao cấp";
                sb.append(String.format("• %s: chất liệu [%s]\n", productName, material));
                
                if (!vars.isEmpty()) {
                    matchedVariants.add(vars.get(0));
                }
                count++;
            }
            sb.append("\nCác chất liệu này giúp nâng niu bàn chân và mang lại trải nghiệm đi giày thoải mái nhất!");

        } else if (lowerText.contains("còn hàng") || lowerText.contains("còn không") || lowerText.contains("số lượng") || lowerText.contains("hết")) {
            sb.append("Dạ, AeroStride xin cập nhật tình trạng tồn kho của các mẫu sản phẩm đang có sẵn tại shop:\n\n");
            
            Map<String, List<ProductVariantResponse>> grouped = variants.stream()
                    .collect(Collectors.groupingBy(ProductVariantResponse::getTenSanPham));
            
            int count = 0;
            for (Map.Entry<String, List<ProductVariantResponse>> entry : grouped.entrySet()) {
                if (count >= 3) break;
                String productName = entry.getKey();
                List<ProductVariantResponse> vars = entry.getValue();
                int totalStock = vars.stream().mapToInt(v -> v.getSoLuong() != null ? v.getSoLuong() : 0).sum();
                String stockStatus = totalStock > 20 ? "Còn nhiều hàng" : (totalStock > 0 ? "Còn ít hàng" : "Hết hàng");
                sb.append(String.format("• %s: %s (tổng %d đôi)\n", productName, stockStatus, totalStock));
                
                if (!vars.isEmpty()) {
                    matchedVariants.add(vars.get(0));
                }
                count++;
            }
            sb.append("\nBạn ưng ý mẫu nào hãy nhắn ngay để shop giữ hàng nhé!");

        } else if (lowerText.contains("khuyến mãi") || lowerText.contains("giảm giá") || lowerText.contains("sale") || lowerText.contains("ưu đãi")) {
            sb.append("Dạ, AeroStride xin gửi bạn các chương trình ưu đãi và giảm giá cực sốc tại shop:\n\n");
            
            Map<String, List<ProductVariantResponse>> grouped = variants.stream()
                    .collect(Collectors.groupingBy(ProductVariantResponse::getTenSanPham));
            
            int count = 0;
            for (Map.Entry<String, List<ProductVariantResponse>> entry : grouped.entrySet()) {
                if (count >= 3) break;
                String productName = entry.getKey();
                List<ProductVariantResponse> vars = entry.getValue();
                BigDecimal maxDiscount = vars.stream()
                        .map(v -> v.getPhanTramGiam() != null ? v.getPhanTramGiam() : BigDecimal.ZERO)
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                
                if (maxDiscount.compareTo(BigDecimal.ZERO) > 0) {
                    sb.append(String.format("• %s: Đang GIẢM GIÁ lên tới %s%%\n", productName, maxDiscount));
                } else {
                    sb.append(String.format("• %s: Giá cực tốt (Đồng giá)\n", productName));
                }
                
                if (!vars.isEmpty()) {
                    matchedVariants.add(vars.get(0));
                }
                count++;
            }
            sb.append("\nNhanh tay chốt đơn để không bỏ lỡ deal hời này nha bạn!");

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
            sb.append("\n\n[[PRODUCT_JSON:");
            try {
                List<Map<String, Object>> jsonList = new ArrayList<>();
                for (ProductVariantResponse v : matchedVariants) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("idSanPham", v.getIdSanPham());
                    map.put("tenSanPham", v.getTenSanPham());
                    map.put("giaBan", v.getGiaBan());
                    map.put("tenThuongHieu", v.getTenThuongHieu() != null ? v.getTenThuongHieu() : "");
                    String imgUrl = (v.getImages() != null && !v.getImages().isEmpty()) ? v.getImages().get(0).getDuongDanAnh() : "";
                    map.put("hinhAnh", imgUrl);
                    map.put("phanTramGiam", v.getPhanTramGiam() != null ? v.getPhanTramGiam() : 0);
                    map.put("soLuong", v.getSoLuong() != null ? v.getSoLuong() : 0);
                    jsonList.add(map);
                }
                sb.append(objectMapper.writeValueAsString(jsonList));
            } catch (Exception e) {
                log.error("Lỗi serialize JSON sản phẩm trong Bot quy tắc: {}", e.getMessage());
                sb.append("[]");
            }
            sb.append("]]");
        }

        // Sinh gợi ý tiếp theo phù hợp với ngữ cảnh câu hỏi quy tắc
        List<String> ruleSuggs = new ArrayList<>();
        if (lowerText.contains("giá") || lowerText.contains("nhiêu") || lowerText.contains("tiền")) {
            ruleSuggs.add("Có voucher giảm giá nào áp dụng hôm nay không?");
            ruleSuggs.add("Bên mình hỗ trợ ship COD toàn quốc không shop?");
            ruleSuggs.add("Giày Nike đang có khuyến mãi gì thế shop?");
        } else if (lowerText.contains("size") || lowerText.contains("kích") || lowerText.contains("cỡ")) {
            ruleSuggs.add("Cách đo chiều dài bàn chân chọn size thế nào?");
            ruleSuggs.add("Nếu nhận hàng đi không vừa size có được đổi mẫu không?");
            ruleSuggs.add("Chính sách bảo hành keo đế trong bao lâu ạ?");
        } else if (lowerText.contains("màu") || lowerText.contains("đen") || lowerText.contains("trắng")) {
            ruleSuggs.add("Có sẵn size 42 các màu này không shop?");
            ruleSuggs.add("Shop ơi gửi ảnh thật đôi Nike màu đen giúp em với.");
            ruleSuggs.add("Chính sách đổi trả trong vòng 7 ngày thế nào?");
        } else if (lowerText.contains("khuyến mãi") || lowerText.contains("giảm giá") || lowerText.contains("sale")) {
            ruleSuggs.add("Mã voucher 'AERO10' áp dụng cho những sản phẩm nào?");
            ruleSuggs.add("Đơn hàng từ bao nhiêu tiền thì được miễn phí ship ạ?");
            ruleSuggs.add("Mẫu giày Adidas nào đang giảm giá nhiều nhất?");
        } else {
            ruleSuggs.add("Mẫu giày nào đang bán chạy nhất tuần này shop ơi?");
            ruleSuggs.add("Có voucher giảm giá nào cho khách mới không?");
            ruleSuggs.add("Chính sách bảo hành và đổi trả của shop thế nào ạ?");
        }

        try {
            sb.append("\n\n[[SUGGESTIONS:").append(objectMapper.writeValueAsString(ruleSuggs)).append("]]");
        } catch (Exception e) {
            log.error("Lỗi serialize JSON gợi ý trong Bot quy tắc: {}", e.getMessage());
        }

        return sb.toString();
    }

    @Override
    public List<String> getDynamicWelcomeSuggestions(String sessionId) {
        // 1. Tải ngữ cảnh sản phẩm từ cache để bám sát thực tế
        List<ProductVariantResponse> activeVariants = getActiveVariantsCached(null);
        String productContext = buildProductContextFromVariants(activeVariants);

        // 2. Tạo prompt yêu cầu sinh mảng gợi ý bằng tiếng Việt ngắn gọn, thiết thực
        String systemTimeContext = String.format("Thời gian hiện tại: %s. ", java.time.LocalDateTime.now().toString());
        String prompt = String.format(
                AiChatPrompts.WELCOME_SUGGESTIONS_PROMPT,
                productContext, AiChatPrompts.STORE_POLICIES_CONTEXT, systemTimeContext
        );

        String jsonResult = null;

        // --- Cố gắng gọi OpenAI / Ollama Local ---
        String activeOpenAiKey = getOpenAiApiKey();
        boolean hasOpenAiKey = activeOpenAiKey != null && !activeOpenAiKey.isBlank() && !"your_openai_api_key_here".equals(activeOpenAiKey);
        if (hasOpenAiKey && isModelHealthy("OPENAI")) {
            try {
                String apiUrl = String.format("%s/chat/completions", openAiBaseUrl);
                jsonResult = callOpenAiApi(apiUrl, prompt);
            } catch (Exception e) {
                log.warn("OpenAI / Ollama không thể sinh gợi ý chào mừng: {}", e.getMessage());
                markModelUnhealthy("OPENAI");
            }
        }

        // Fallback sang Local AI / Cấu trúc dữ liệu cục bộ tĩnh (Tốc độ tối đa)
        if (jsonResult == null) {
            log.info("Không có API AI bên thứ ba khả dụng hoặc tất cả bị ngắt mạch, sử dụng gợi ý cục bộ động...");
            try {
                List<String> localSuggs = new ArrayList<>();
                localSuggs.add("Đợt giảm giá này có voucher gì không shop?");
                localSuggs.add("Chính sách bảo hành và đổi trả giày thế nào ạ?");
                localSuggs.add("Bên mình hỗ trợ ship COD toàn quốc không shop?");
                
                // Trích xuất thương hiệu ngẫu nhiên từ sản phẩm hoạt động để làm gợi ý sinh động
                if (activeVariants != null && !activeVariants.isEmpty()) {
                    Set<String> brands = activeVariants.stream()
                            .map(ProductVariantResponse::getTenThuongHieu)
                            .filter(b -> b != null && !b.isBlank())
                            .collect(Collectors.toSet());
                    int added = 0;
                    for (String brand : brands) {
                        if (added >= 2) break;
                        localSuggs.add(String.format("Shop ơi thương hiệu %s có những mẫu nào đang bán chạy?", brand));
                        added++;
                    }
                }
                
                if (localSuggs.size() < 5) {
                    localSuggs.add("Mẫu giày Nike nào đang hot nhất tuần này?");
                    localSuggs.add("Adidas có sẵn size 42 không ạ?");
                }
                
                return localSuggs;
            } catch (Exception e) {
                log.warn("Lỗi sinh gợi ý cục bộ: {}", e.getMessage());
            }
        }

        // Parse JSON kết quả
        try {
            if (jsonResult != null) {
                String cleanJson = jsonResult.replaceAll("```json|```", "").trim();
                int startIdx = cleanJson.indexOf('[');
                int endIdx = cleanJson.lastIndexOf(']');
                if (startIdx != -1 && endIdx != -1 && startIdx < endIdx) {
                    cleanJson = cleanJson.substring(startIdx, endIdx + 1);
                }
                return objectMapper.readValue(cleanJson, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            }
        } catch (Exception e) {
            log.error("Lỗi phân tích cú pháp gợi ý chào mừng từ JSON AI: {}, Chuỗi gốc: {}", e.getMessage(), jsonResult);
        }

        // Fallback tối cao
        return List.of(
                "Làm thế nào để đặt hàng?",
                "Phí vận chuyển là bao nhiêu?",
                "Kiểm tra trạng thái đơn hàng",
                "Có voucher giảm giá không?",
                "Sản phẩm có bảo hành không?",
                "Hướng dẫn thanh toán online",
                "Liên hệ nhân viên hỗ trợ"
        );
    }

    @Override
    public String getDashboardInsights(int pendingOrders, int lowStockItems) {
        String prompt = "Phân tích nhanh cho trang chủ Admin: Hiện có " + pendingOrders + " đơn hàng chờ xác nhận và " + lowStockItems + " sản phẩm sắp hết hàng (số lượng < 5). Viết 1-2 câu tư vấn ngắn gọn cho Admin.";
        String activeOpenAiKey = getOpenAiApiKey();
        if (activeOpenAiKey != null && !activeOpenAiKey.isBlank() && !"your_openai_api_key_here".equals(activeOpenAiKey)) {
            try {
                String apiUrl = String.format("%s/chat/completions", openAiBaseUrl);
                return callOpenAiApi(apiUrl, prompt);
            } catch (Exception e) {
                log.error("Lỗi phân tích nhanh Admin dashboard bằng OpenAI/Ollama: {}", e.getMessage());
            }
        }
        return "Hãy tập trung xác nhận các đơn hàng chờ và kiểm tra hàng tồn kho để bổ sung kịp thời.";
    }
}


