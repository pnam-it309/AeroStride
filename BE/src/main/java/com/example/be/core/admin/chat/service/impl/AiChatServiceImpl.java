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

    @Value("${google.gemini.api-key}")
    private String geminiApiKeyString;

    private List<String> apiKeysList = new ArrayList<>();
    private final AtomicInteger keyIndex = new AtomicInteger(0);

    @Value("${openai.api-key:}")
    private String openAiApiKeyString;

    private List<String> openAiApiKeysList = new ArrayList<>();
    private final AtomicInteger openAiKeyIndex = new AtomicInteger(0);

    @Value("${claude.api-key:}")
    private String claudeApiKeyString;

    private List<String> claudeApiKeysList = new ArrayList<>();
    private final AtomicInteger claudeKeyIndex = new AtomicInteger(0);

    @Value("${deepseek.api-key:}")
    private String deepseekApiKeyString;

    private List<String> deepseekApiKeysList = new ArrayList<>();
    private final AtomicInteger deepseekKeyIndex = new AtomicInteger(0);

    @PostConstruct
    public void initApiKeys() {
        if (geminiApiKeyString != null && !geminiApiKeyString.isBlank()) {
            apiKeysList = Arrays.stream(geminiApiKeyString.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            log.info("Đã nạp thành công {} API Keys cho Gemini.", apiKeysList.size());
        }
        if (openAiApiKeyString != null && !openAiApiKeyString.isBlank()) {
            openAiApiKeysList = Arrays.stream(openAiApiKeyString.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            log.info("Đã nạp thành công {} API Keys cho OpenAI.", openAiApiKeysList.size());
        }
        if (claudeApiKeyString != null && !claudeApiKeyString.isBlank()) {
            claudeApiKeysList = Arrays.stream(claudeApiKeyString.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            log.info("Đã nạp thành công {} API Keys cho Claude.", claudeApiKeysList.size());
        }
        if (deepseekApiKeyString != null && !deepseekApiKeyString.isBlank()) {
            deepseekApiKeysList = Arrays.stream(deepseekApiKeyString.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            log.info("Đã nạp thành công {} API Keys cho DeepSeek.", deepseekApiKeysList.size());
        }

        // Khởi động luồng chạy ngầm tải trước (warm-up) cache danh sách sản phẩm để tránh trễ ở request đầu tiên
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Đợi Spring container và database connection pool ổn định
                log.info("Bắt đầu tải trước (Warm-up) cache biến thể sản phẩm...");
                getActiveVariantsIntelligent(null);
                log.info("Tải trước cache sản phẩm hoàn tất! Chatbot sẵn sàng xử lý tức thì (0ms truy vấn).");
            } catch (Exception e) {
                log.warn("Không thể tải trước cache sản phẩm khi khởi động: {}", e.getMessage());
            }
        }).start();
    }

    private String getApiKey() {
        if (apiKeysList.isEmpty()) {
            return geminiApiKeyString;
        }
        int idx = keyIndex.getAndIncrement() % apiKeysList.size();
        if (idx < 0) idx = 0;
        return apiKeysList.get(idx);
    }

    private String getOpenAiApiKey() {
        if (openAiApiKeysList.isEmpty()) {
            return openAiApiKeyString;
        }
        int idx = openAiKeyIndex.getAndIncrement() % openAiApiKeysList.size();
        if (idx < 0) idx = 0;
        return openAiApiKeysList.get(idx);
    }

    private String getClaudeApiKey() {
        if (claudeApiKeysList.isEmpty()) {
            return claudeApiKeyString;
        }
        int idx = claudeKeyIndex.getAndIncrement() % claudeApiKeysList.size();
        if (idx < 0) idx = 0;
        return claudeApiKeysList.get(idx);
    }

    private String getDeepSeekApiKey() {
        if (deepseekApiKeysList.isEmpty()) {
            return deepseekApiKeyString;
        }
        int idx = deepseekKeyIndex.getAndIncrement() % deepseekApiKeysList.size();
        if (idx < 0) idx = 0;
        return deepseekApiKeysList.get(idx);
    }

    @Value("${google.gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    @Value("${google.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiBaseUrl;

    @Value("${openai.model:gpt-4o-mini}")
    private String openAiModel;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Value("${claude.model:claude-3-5-sonnet-20241022}")
    private String claudeModel;

    @Value("${claude.base-url:https://api.anthropic.com/v1}")
    private String claudeBaseUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String deepseekModel;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String deepseekBaseUrl;

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
            @Value("${google.gemini.timeout-ms:30000}") int timeoutMs
    ) {
        this.sanPhamService = sanPhamService;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.aiLocalService = aiLocalService;

        // [Cải tiến 3] RestTemplate có Timeout → chống treo luồng @Async
        // Tăng mặc định lên 30s để hỗ trợ prompt phức tạp
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .readTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Async
    @Override
    public void generateAndSendResponse(CuocHoiThoai conversation, String customerText) {
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

        // 1. Lấy danh sách sản phẩm thông minh dựa trên Index Database
        List<ProductVariantResponse> activeVariants = getActiveVariantsIntelligent(customerText);
        String productContext = buildProductContextFromVariants(activeVariants);
        String chatHistory = buildChatHistory(conversation.getId());
        String prompt = buildPrompt(productContext, chatHistory, customerText);

        // --- Cố gắng gọi GOOGLE GEMINI ---
        String activeGeminiKey = getApiKey();
        boolean hasGeminiKey = activeGeminiKey != null && !activeGeminiKey.isBlank() && !"your_gemini_api_key_here".equals(activeGeminiKey);
        
        if (hasGeminiKey && isModelHealthy("GEMINI")) {
            try {
                log.info("Khởi động gọi Google Gemini API...");
                String apiUrl = String.format("%s/models/%s:generateContent?key=%s",
                        geminiBaseUrl, geminiModel, activeGeminiKey);
                String botResponseText = callGeminiApi(apiUrl, prompt);
                saveAndBroadcast(conversation, botResponseText);
                log.info("Google Gemini phản hồi thành công.");
                return; // Xử lý xong, kết thúc!
            } catch (Exception e) {
                log.warn("Google Gemini API gặp sự cố. Nguyên nhân chi tiết: {}", e.getMessage());
                markModelUnhealthy("GEMINI");
            }
        } else {
            log.info("Google Gemini API Key chưa cấu hình hoặc không khỏe mạnh. Bỏ qua Gemini.");
        }

        // --- Cố gắng gọi OPENAI CHATGPT (Fallback thứ nhất) ---
        String activeOpenAiKey = getOpenAiApiKey();
        boolean hasOpenAiKey = activeOpenAiKey != null && !activeOpenAiKey.isBlank() && !"your_openai_api_key_here".equals(activeOpenAiKey);

        if (hasOpenAiKey && isModelHealthy("OPENAI")) {
            try {
                log.info("Tự động chuyển đổi: Khởi động gọi OpenAI ChatGPT API...");
                String apiUrl = String.format("%s/chat/completions", openAiBaseUrl);
                String botResponseText = callOpenAiApi(apiUrl, prompt);
                saveAndBroadcast(conversation, botResponseText);
                log.info("OpenAI ChatGPT phản hồi thành công.");
                return; // Xử lý xong, kết thúc!
            } catch (Exception e) {
                log.warn("OpenAI ChatGPT API gặp sự cố. Nguyên nhân chi tiết: {}", e.getMessage());
                markModelUnhealthy("OPENAI");
            }
        } else {
            log.info("OpenAI ChatGPT API Key chưa cấu hình hoặc không khỏe mạnh. Bỏ qua OpenAI.");
        }

        // --- Cố gắng gọi ANTHROPIC CLAUDE (Fallback thứ hai) ---
        String activeClaudeKey = getClaudeApiKey();
        boolean hasClaudeKey = activeClaudeKey != null && !activeClaudeKey.isBlank() && !"your_claude_api_key_here".equals(activeClaudeKey);

        if (hasClaudeKey && isModelHealthy("CLAUDE")) {
            try {
                log.info("Tự động chuyển đổi: Khởi động gọi Anthropic Claude API...");
                String apiUrl = String.format("%s/messages", claudeBaseUrl);
                String botResponseText = callClaudeApi(apiUrl, prompt);
                saveAndBroadcast(conversation, botResponseText);
                log.info("Anthropic Claude phản hồi thành công.");
                return; // Xử lý xong, kết thúc!
            } catch (Exception e) {
                log.warn("Anthropic Claude API gặp sự cố. Nguyên nhân chi tiết: {}", e.getMessage());
                markModelUnhealthy("CLAUDE");
            }
        } else {
            log.info("Anthropic Claude API Key chưa cấu hình hoặc không khỏe mạnh. Bỏ qua Claude.");
        }

        // --- Cố gắng gọi DEEPSEEK (Fallback thứ ba) ---
        String activeDeepSeekKey = getDeepSeekApiKey();
        boolean hasDeepSeekKey = activeDeepSeekKey != null && !activeDeepSeekKey.isBlank() && !"your_deepseek_api_key_here".equals(activeDeepSeekKey);

        if (hasDeepSeekKey && isModelHealthy("DEEPSEEK")) {
            try {
                log.info("Tự động chuyển đổi: Khởi động gọi DeepSeek API...");
                String apiUrl = String.format("%s/chat/completions", deepseekBaseUrl);
                String botResponseText = callDeepSeekApi(apiUrl, prompt);
                saveAndBroadcast(conversation, botResponseText);
                log.info("DeepSeek phản hồi thành công.");
                return; // Xử lý xong, kết thúc!
            } catch (Exception e) {
                log.warn("DeepSeek API gặp sự cố. Nguyên nhân chi tiết: {}", e.getMessage());
                markModelUnhealthy("DEEPSEEK");
            }
        } else {
            log.info("DeepSeek API Key chưa cấu hình hoặc không khỏe mạnh. Bỏ qua DeepSeek.");
        }

        // --- Cố gắng gọi LOCAL AI (Fallback thứ tư - Tuyệt đối không treo luồng) ---
        log.info("Tự động chuyển đổi: Kích hoạt AI nội bộ cục bộ làm dự phòng...");
        try {
            String localResponse = aiLocalService.generateResponse(customerText, conversation.getId());
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

        // Lấy top 20 sản phẩm đang hoạt động khi không truyền từ khóa
        if (text == null || text.isBlank()) {
            return cachedVariants.stream().limit(MAX_CONTEXT_PRODUCTS).collect(Collectors.toList());
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

        Map<String, List<ProductVariantResponse>> groupedProducts = variants.stream()
                .collect(Collectors.groupingBy(ProductVariantResponse::getTenSanPham));

        StringBuilder sb = new StringBuilder();
        sb.append("DANH SÁCH GIÀY GỢI Ý (AeroStride):\n");

        groupedProducts.forEach((tenSp, vars) -> {
            // Nếu context quá dài (trên 15k ký tự), ngừng thêm để bảo vệ an toàn prompt
            if (sb.length() > 15000) return;

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
        List<TinNhan> recentMessages =
                messageRepository.findTop10ByCuocHoiThoai_IdOrderByNgayTaoDesc(conversationId);

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
     * Xây dựng prompt hoàn chỉnh cho Gemini API.
     */
    private String buildPrompt(String productContext, String chatHistory, String customerText) {
        return String.format(
                AiChatPrompts.MAIN_SYSTEM_PROMPT,
                productContext, AiChatPrompts.STORE_POLICIES_CONTEXT, chatHistory, customerText
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
            String responseText = parts.get(0).get("text");
            if (responseText == null || responseText.isBlank()) {
                throw new RuntimeException("Phản hồi từ Gemini trống.");
            }
            return responseText;
        } catch (Exception e) {
            log.warn("Không thể parse Gemini response: {}", e.getMessage());
            throw new RuntimeException("Lỗi phân tích cú pháp phản hồi từ Gemini", e);
        }
    }

    /**
     * Gọi OpenAI ChatGPT API và trích xuất kết quả.
     */
    @SuppressWarnings("unchecked")
    private String callOpenAiApi(String apiUrl, String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", openAiModel);

        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
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
     * Gọi Anthropic Claude API và trích xuất kết quả.
     */
    @SuppressWarnings("unchecked")
    private String callClaudeApi(String apiUrl, String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", claudeModel);
        requestBody.put("max_tokens", 1024);

        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        requestBody.put("messages", List.of(message));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", getClaudeApiKey());
        headers.set("anthropic-version", "2023-06-01");
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        Map<String, Object> response = restTemplate.postForObject(apiUrl, entity, Map.class);
        return extractTextFromClaudeResponse(response);
    }

    /**
     * Trích xuất text phản hồi từ JSON response của Claude API.
     */
    @SuppressWarnings("unchecked")
    private String extractTextFromClaudeResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) response.get("content");
            Map<String, Object> firstContent = contentList.get(0);
            String responseText = (String) firstContent.get("text");
            if (responseText == null || responseText.isBlank()) {
                throw new RuntimeException("Phản hồi từ Claude trống.");
            }
            return responseText;
        } catch (Exception e) {
            log.warn("Không thể parse Claude response: {}", e.getMessage());
            throw new RuntimeException("Lỗi phân tích cú pháp phản hồi từ Claude", e);
        }
    }

    /**
     * Gọi DeepSeek API và trích xuất kết quả.
     */
    @SuppressWarnings("unchecked")
    private String callDeepSeekApi(String apiUrl, String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", deepseekModel);

        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        requestBody.put("messages", List.of(message));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getDeepSeekApiKey());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        Map<String, Object> response = restTemplate.postForObject(apiUrl, entity, Map.class);
        return extractTextFromDeepSeekResponse(response);
    }

    /**
     * Trích xuất text phản hồi từ JSON response của DeepSeek API.
     */
    @SuppressWarnings("unchecked")
    private String extractTextFromDeepSeekResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
            String responseText = (String) message.get("content");
            if (responseText == null || responseText.isBlank()) {
                throw new RuntimeException("Phản hồi từ DeepSeek trống.");
            }
            return responseText;
        } catch (Exception e) {
            log.warn("Không thể parse DeepSeek response: {}", e.getMessage());
            throw new RuntimeException("Lỗi phân tích cú pháp phản hồi từ DeepSeek", e);
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
                    map.put("tenDanhMuc", "Giày");
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

        // --- Cố gắng gọi GOOGLE GEMINI ---
        String activeGeminiKey = getApiKey();
        boolean hasGeminiKey = activeGeminiKey != null && !activeGeminiKey.isBlank() && !"your_gemini_api_key_here".equals(activeGeminiKey);
        if (hasGeminiKey && isModelHealthy("GEMINI")) {
            try {
                String apiUrl = String.format("%s/models/%s:generateContent?key=%s",
                        geminiBaseUrl, geminiModel, activeGeminiKey);
                jsonResult = callGeminiApi(apiUrl, prompt);
            } catch (Exception e) {
                log.warn("Gemini không thể sinh gợi ý chào mừng: {}", e.getMessage());
                markModelUnhealthy("GEMINI");
            }
        }

        // --- Cố gắng gọi OPENAI CHATGPT (Fallback thứ nhất) ---
        if (jsonResult == null) {
            String activeOpenAiKey = getOpenAiApiKey();
            boolean hasOpenAiKey = activeOpenAiKey != null && !activeOpenAiKey.isBlank() && !"your_openai_api_key_here".equals(activeOpenAiKey);
            if (hasOpenAiKey && isModelHealthy("OPENAI")) {
                try {
                    String apiUrl = String.format("%s/chat/completions", openAiBaseUrl);
                    jsonResult = callOpenAiApi(apiUrl, prompt);
                } catch (Exception e) {
                    log.warn("OpenAI không thể sinh gợi ý chào mừng: {}", e.getMessage());
                    markModelUnhealthy("OPENAI");
                }
            }
        }

        // --- Cố gắng gọi ANTHROPIC CLAUDE (Fallback thứ hai) ---
        if (jsonResult == null) {
            String activeClaudeKey = getClaudeApiKey();
            boolean hasClaudeKey = activeClaudeKey != null && !activeClaudeKey.isBlank() && !"your_claude_api_key_here".equals(activeClaudeKey);
            if (hasClaudeKey && isModelHealthy("CLAUDE")) {
                try {
                    String apiUrl = String.format("%s/messages", claudeBaseUrl);
                    jsonResult = callClaudeApi(apiUrl, prompt);
                } catch (Exception e) {
                    log.warn("Claude không thể sinh gợi ý chào mừng: {}", e.getMessage());
                    markModelUnhealthy("CLAUDE");
                }
            }
        }

        // --- Cố gắng gọi DEEPSEEK (Fallback thứ ba) ---
        if (jsonResult == null) {
            String activeDeepSeekKey = getDeepSeekApiKey();
            boolean hasDeepSeekKey = activeDeepSeekKey != null && !activeDeepSeekKey.isBlank() && !"your_deepseek_api_key_here".equals(activeDeepSeekKey);
            if (hasDeepSeekKey && isModelHealthy("DEEPSEEK")) {
                try {
                    String apiUrl = String.format("%s/chat/completions", deepseekBaseUrl);
                    jsonResult = callDeepSeekApi(apiUrl, prompt);
                } catch (Exception e) {
                    log.warn("DeepSeek không thể sinh gợi ý chào mừng: {}", e.getMessage());
                    markModelUnhealthy("DEEPSEEK");
                }
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
}

