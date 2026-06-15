package com.example.be.core.common.chat.local.service;

import com.example.be.entity.KienThucAi;
import com.example.be.entity.SanPham;
import com.example.be.repository.KienThucAiRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiLocalService {
    private final KienThucAiRepository aiKnowledgeRepository;
    private final AiWordLibrary wordLibrary;
    private final DataRetrievalLibrary dataLibrary;
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
        return cachedKnowledgeList;
    }

    private static class ChatState {
        String currentState = "NORMAL";
        Queue<String> messageQueue = new LinkedList<>();
        Queue<String> alternativeProducts = new LinkedList<>();
        Map<String, Object> conversationData = new ConcurrentHashMap<>();
        long lastActionTime = System.currentTimeMillis();
        int consecutiveFallbacks = 0;

        void addMessage(String msg) {
            if (messageQueue.size() >= 10) {
                messageQueue.poll();
            }
            messageQueue.add(msg);
        }
    }

    public String generateResponse(String userMessage) {
        return generateResponse(userMessage, "default-session");
    }

    public String generateResponse(String userMessage, String conversationId) {
        String cid = (conversationId != null && !conversationId.isBlank()) ? conversationId : "default-session";
        ChatState state = sessionStates.computeIfAbsent(cid, k -> new ChatState());

        // Invalidate state if inactive for more than 10 minutes
        long now = System.currentTimeMillis();
        if (now - state.lastActionTime > 10 * 60 * 1000) {
            state.currentState = "NORMAL";
            state.alternativeProducts.clear();
            state.conversationData.clear();
        }
        state.lastActionTime = now;
        state.addMessage(userMessage);

        String normalizedInput = wordLibrary.normalize(userMessage);
        log.info("Processing stateful local AI message (State: {}): '{}' -> '{}'", state.currentState, userMessage, normalizedInput);

        // State Machine Router
        String response;
        switch (state.currentState) {
            case "APOLOGIZING":
                response = handleApologizingState(state, normalizedInput);
                break;
            case "SATISFACTION_CHECK":
                response = handleSatisfactionCheckState(state, normalizedInput);
                break;
            case "COLLECTING_PREFERENCES":
                response = handleCollectingPreferencesState(state, normalizedInput);
                break;
            case "SIZING_CONSULTATION":
                response = handleSizingConsultationState(state, normalizedInput);
                break;
            case "NORMAL":
            default:
                response = handleNormalState(state, normalizedInput, userMessage);
                break;
        }

        // Tự động phát hiện vòng lặp tin nhắn chào mặc định khi bot không hiểu nhiều lần liên tiếp
        if (response.contains("AeroStride trợ lý ảo xin nghe")) {
            state.consecutiveFallbacks++;

            if (state.consecutiveFallbacks >= 2) {
                // Tự động kết nối với nhân viên (Handoff) để bảo vệ trải nghiệm khách hàng
                state.currentState = "NORMAL";
                state.alternativeProducts.clear();
                state.conversationData.clear();
                state.consecutiveFallbacks = 0; // reset counter after handoff

                String handoffText = "Tôi đã kết nối bạn với nhân viên hỗ trợ. Vui lòng chờ trong giây lát...";
                try {
                    List<String> waitingSuggs = List.of(
                        "Xem giờ mở cửa của showroom",
                        "Chính sách bảo hành và đổi trả",
                        "Xem địa chỉ showroom AeroStride"
                    );
                    handoffText += "\n\n[[SUGGESTIONS:" + objectMapper.writeValueAsString(waitingSuggs) + "]]";
                } catch (Exception e) {
                    log.error("Lỗi serialize suggestions cho handoff: {}", e.getMessage());
                }
                return handoffText;
            }
        } else {
            // Reset bộ đếm nếu khách nhận được câu trả lời đúng gu/hữu ích
            state.consecutiveFallbacks = 0;
        }

        return response;
    }

    private String handleNormalState(ChatState state, String normalizedInput, String rawInput) {
        String lowerInput = normalizedInput.toLowerCase();

        // 1. Phát hiện cảm xúc tiêu cực / khiếu nại của khách hàng để kích hoạt quy trình xin lỗi
        if (lowerInput.contains("lỗi") || lowerInput.contains("hỏng") || lowerInput.contains("rách") ||
            lowerInput.contains("bong keo") || lowerInput.contains("bung keo") || lowerInput.contains("tệ") ||
            lowerInput.contains("chán") || lowerInput.contains("kém") || lowerInput.contains("thất vọng") ||
            lowerInput.contains("bực mình") || lowerInput.contains("khiếu nại") || lowerInput.contains("giao chậm") ||
            lowerInput.contains("chậm trễ") || lowerInput.contains("đợi lâu") || lowerInput.contains("thiếu hàng") ||
            lowerInput.contains("nhầm size") || lowerInput.contains("nhầm màu") || lowerInput.contains("không hài lòng")) {

            String complaintReason = "trải nghiệm chưa được trọn vẹn";
            if (lowerInput.contains("keo") || lowerInput.contains("hỏng") || lowerInput.contains("rách") || lowerInput.contains("lỗi")) {
                complaintReason = "sự cố về chất lượng sản phẩm (lỗi keo/đế/chất liệu)";
            } else if (lowerInput.contains("giao") || lowerInput.contains("ship") || lowerInput.contains("chậm") || lowerInput.contains("lâu")) {
                complaintReason = "sự cố giao hàng bị chậm trễ ngoài dự kiến";
            } else if (lowerInput.contains("nhầm") || lowerInput.contains("sai") || lowerInput.contains("thiếu")) {
                complaintReason = "sự cố đóng gói nhầm lẫn mẫu mã/size giày";
            }

            state.currentState = "APOLOGIZING";
            state.conversationData.put("complaintReason", complaintReason);

            // Xếp các sản phẩm mới nhất vào Queue thay thế
            state.alternativeProducts.clear();
            List<SanPham> topProducts = dataLibrary.getTopProducts(4);
            if (topProducts != null && !topProducts.isEmpty()) {
                for (SanPham p : topProducts) {
                    state.alternativeProducts.add(p.getTen() + " (Mã: " + p.getMa() + ")");
                }
            } else {
                state.alternativeProducts.add("Giày Sneaker AeroStride Runner Pro");
                state.alternativeProducts.add("Giày Cổ Cao AeroStride Streetwear Luxe");
                state.alternativeProducts.add("Giày Da Bò AeroStride Classic Vintage");
            }

            return "Dạ, AeroStride vô cùng xin lỗi anh/chị vì " + complaintReason + " đã làm ảnh hưởng tới tâm trạng của mình ạ! 🥺 Cửa hàng luôn đặt sự hài lòng của quý khách lên vị trí cao nhất.\n\n" +
                   "Để tạ lỗi và mong muốn bù đắp trải nghiệm này, shop rất hy vọng được phép gửi tặng riêng mình một voucher giảm giá đặc biệt kèm đề xuất một số dòng giày siêu êm chân khác đang cực kỳ hot được không ạ? Anh/chị có sẵn lòng cho shop cơ hội được giới thiệu sản phẩm mới và xem mình có hài lòng không ạ? 💕";
        }

        // 2. Tra cứu Knowledge Base trong DB
        List<KienThucAi> knowledges = getKnowledgeCached();
        for (KienThucAi k : knowledges) {
            if (isMatch(normalizedInput, k.getTuKhoa())) {
                if ("PRODUCT_SEARCH".equals(k.getMucDich())) {
                    String searchTerm = extractSearchTerm(normalizedInput, k.getTuKhoa());
                    List<SanPham> searchResult = dataLibrary.searchProducts(searchTerm);
                    if (searchResult.isEmpty() || searchTerm.isEmpty()) {
                        return "Dạ, bạn muốn tìm loại gì hoặc là muốn tìm dòng sản phẩm nào ạ? 🥰";
                    }
                }
                String response = formatResponse(k, normalizedInput);
                if ("PRODUCT_LIST".equals(k.getMucDich()) || "PRODUCT_SEARCH".equals(k.getMucDich())) {
                    // Nạp hàng đợi để hỏi độ hài lòng
                    state.alternativeProducts.clear();
                    List<SanPham> list = dataLibrary.getTopProducts(5);
                    for (SanPham p : list) {
                        state.alternativeProducts.add(p.getTen() + " (Mã: " + p.getMa() + ")");
                    }
                    if (!state.alternativeProducts.isEmpty()) {
                        String current = state.alternativeProducts.poll();
                        state.conversationData.put("currentProduct", current);
                        state.currentState = "SATISFACTION_CHECK";
                        return response + "\n\nĐể bắt đầu tư vấn, shop xin gợi ý mẫu giày đầu tiên cực kỳ xuất sắc: **" + current + "**.\n" +
                               "Không biết mẫu giày này có làm anh/chị hài lòng hay ưng ý không ạ? 🥰";
                    }
                }
                return response;
            }
        }

        // 3. Xử lý Trạng thái chốt size giày
        if (lowerInput.contains("size") || lowerInput.contains("kích thước") || lowerInput.contains("kích cỡ") || lowerInput.contains("đo chân")) {
            state.currentState = "SIZING_CONSULTATION";
            return "Dạ, để shop tư vấn size giày chính xác nhất, anh/chị vui lòng cho shop biết chiều dài bàn chân đo bằng cm (ví dụ: '25cm') hoặc size giày bình thường mình hay mang nhé! 👟\n\n" +
                   dataLibrary.getSizesInfo();
        }

        // 4. Xử lý Logic Java Rule-based dự phòng
        return generateRuleBasedResponse(state, lowerInput);
    }

    private String generateRuleBasedResponse(ChatState state, String lowerInput) {
        if (lowerInput.contains("đề xuất") || lowerInput.contains("gợi ý") || lowerInput.contains("tư vấn giày") ||
            lowerInput.contains("tìm giày") || lowerInput.contains("tìm sản phẩm") || lowerInput.contains("giới thiệu") ||
            lowerInput.contains("không biết") || lowerInput.contains("chưa biết") || lowerInput.contains("chọn loại nào") ||
            lowerInput.contains("tư vấn cho t") || lowerInput.contains("tu van") || lowerInput.contains("de xuat") ||
            lowerInput.contains("goi y") || lowerInput.contains("bán chạy") || lowerInput.contains("ban chay")) {

            state.alternativeProducts.clear();
            List<SanPham> list = dataLibrary.getTopProducts(5);
            for (SanPham p : list) {
                state.alternativeProducts.add(p.getTen() + " (Mã: " + p.getMa() + ")");
            }
            if (!state.alternativeProducts.isEmpty()) {
                String current = state.alternativeProducts.poll();
                state.conversationData.put("currentProduct", current);
                state.currentState = "SATISFACTION_CHECK";
                return "Dạ, nếu bạn đang phân vân, shop xin phép được gợi ý các mẫu giày hot nhất hiện nay nhé! 🥰\n\nĐể bắt đầu, shop xin giới thiệu mẫu giày cực kỳ được yêu thích: **" + current + "**.\n\nKhông biết mẫu giày này có đúng gu của bạn không ạ?";
            }
        }

        if (lowerInput.contains("chào") || lowerInput.contains("hello") || lowerInput.contains("hi ") || lowerInput.equals("hi") || lowerInput.contains("alo")) {
            return "Dạ, AeroStride xin kính chào quý khách! Tôi là trợ lý ảo hỗ trợ chăm sóc khách hàng tự động. Tôi có thể giúp bạn tìm kiếm giày, tư vấn size, chính sách giao hàng, bảo hành đổi trả hoặc khuyến mãi. Bạn đang quan tâm đến sản phẩm hoặc chính sách nào ạ?";
        }

        if (lowerInput.contains("địa chỉ") || lowerInput.contains("ở đâu") || lowerInput.contains("chi nhánh") || lowerInput.contains("cửa hàng") || lowerInput.contains("showroom")) {
            return "Quý khách có thể ghé showroom chính thức của AeroStride tại: 123 Đường Cầu Giấy, Quận Cầu Giấy, Hà Nội. Giờ mở cửa từ 08:30 đến 22:00 hàng ngày (kể cả Thứ 7, Chủ Nhật và ngày lễ) ạ.";
        }

        if (lowerInput.contains("ship") || lowerInput.contains("giao hàng") || lowerInput.contains("phí vận chuyển") || lowerInput.contains("nhận hàng") || lowerInput.contains("vận chuyển") || lowerInput.contains("bao lâu")) {
            return "Chính sách giao hàng của AeroStride:\n- MIỄN PHÍ giao hàng toàn quốc cho đơn hàng từ 1.000.000 VNĐ (đơn dưới 1M phí ship đồng giá 30k).\n- Thời gian giao hàng: Nội thành Hà Nội/HCM: 1-2 ngày; các tỉnh khác: 3-5 ngày.\n- Hỗ trợ thanh toán COD khi nhận hàng.";
        }

        if (lowerInput.contains("đổi trả") || lowerInput.contains("bảo hành") || lowerInput.contains("lỗi") || lowerInput.contains("hoàn tiền") || lowerInput.contains("trả hàng") || lowerInput.contains("chật") || lowerInput.contains("rộng")) {
            return "Chính sách đổi trả & bảo hành tại AeroStride:\n- Đổi trả miễn phí trong vòng 7 ngày kể từ khi nhận hàng cho trường hợp không vừa size hoặc muốn đổi mẫu mới (giày chưa qua sử dụng, còn nguyên tem mác và hộp).\n- Bảo hành bong keo, hở đế, sứt chỉ hoàn toàn miễn phí trong vòng 6 tháng.\n- Hoàn tiền 100% nếu phát hiện lỗi từ nhà sản xuất.";
        }

        if (lowerInput.contains("liên hệ") || lowerInput.contains("số điện thoại") || lowerInput.contains("hotline") || lowerInput.contains("sđt") || lowerInput.contains("tổng đài")) {
            return "Bạn có thể liên hệ trực tiếp với AeroStride qua hotline khẩn cấp: 1900 88xx (hoạt động từ 08:30 - 21:00) hoặc nhắn tin trực tiếp fanpage/Zalo để được hỗ trợ tốt nhất nhé!";
        }

        // Tích hợp từ khóa động cho khuyến mãi, đợt giảm giá, phiếu giảm giá và sản phẩm khuyến mãi
        if (lowerInput.contains("sản phẩm khuyến mãi") || lowerInput.contains("sản phẩm giảm giá") ||
            lowerInput.contains("sản phẩm đang sale") || lowerInput.contains("giày sale") ||
            lowerInput.contains("đang khuyến mãi") || lowerInput.contains("san pham khuyen mai") ||
            (lowerInput.contains("sản phẩm") && lowerInput.contains("khuyến mãi")) ||
            (lowerInput.contains("giày") && lowerInput.contains("khuyến mãi")) ||
            (lowerInput.contains("sản phẩm") && lowerInput.contains("giảm giá"))) {
            return dataLibrary.getPromoProductsInfo();
        }

        if (lowerInput.contains("đợt giảm giá") || lowerInput.contains("chương trình giảm giá") ||
            lowerInput.contains("sự kiện sale") || lowerInput.contains("dot giam gia") ||
            lowerInput.contains("chương trình khuyến mãi")) {
            return dataLibrary.getActiveDiscountsInfo();
        }

        if (lowerInput.contains("phiếu giảm giá") || lowerInput.contains("mã giảm giá") ||
            lowerInput.contains("voucher") || lowerInput.contains("coupon") ||
            lowerInput.contains("mã giảm") || lowerInput.contains("phieu giam gia")) {
            return dataLibrary.getActiveCouponsInfo();
        }

        if (lowerInput.contains("khuyến mãi") || lowerInput.contains("giảm giá") || lowerInput.contains("voucher") || lowerInput.contains("sale") || lowerInput.contains("mã giảm") || lowerInput.contains("ưu đãi")) {
            return "Dạ, AeroStride đang áp dụng rất nhiều chương trình ưu đãi vô cùng lớn dành cho anh/chị:\n\n" +
                   dataLibrary.getActiveDiscountsInfo() + "\n\n" +
                   dataLibrary.getActiveCouponsInfo();
        }

        // Tích hợp các từ khóa thuộc tính sản phẩm giày
        if (lowerInput.contains("đế giày") || lowerInput.contains("de giay") || lowerInput.contains("loại đế") || lowerInput.contains("công nghệ đế")) {
            return dataLibrary.getShoeSolesInfo();
        }

        if (lowerInput.contains("chất liệu") || lowerInput.contains("chat lieu") || lowerInput.contains("làm bằng gì") || lowerInput.contains("vải gì") || lowerInput.contains("da gì")) {
            return dataLibrary.getMaterialsInfo();
        }

        if (lowerInput.contains("cổ giày") || lowerInput.contains("co giay") || lowerInput.contains("cổ cao") || lowerInput.contains("cổ thấp") || lowerInput.contains("cổ lửng")) {
            return dataLibrary.getCollarsInfo();
        }

        if (lowerInput.contains("thương hiệu") || lowerInput.contains("thuong hieu") || lowerInput.contains("hãng giày") || lowerInput.contains("nhãn hiệu")) {
            return dataLibrary.getBrandsInfo();
        }

        if (lowerInput.contains("xuất xứ") || lowerInput.contains("xuat xu") || lowerInput.contains("sản xuất ở đâu") || lowerInput.contains("nguồn gốc") || lowerInput.contains("made in")) {
            return dataLibrary.getOriginsInfo();
        }

        if (lowerInput.contains("màu sắc") || lowerInput.contains("mau sac") || lowerInput.contains("phối màu") || lowerInput.contains("màu nào") || lowerInput.contains("có những màu")) {
            return dataLibrary.getColorsInfo();
        }

        if (lowerInput.contains("đơn hàng") || lowerInput.contains("tra cứu đơn") || lowerInput.contains("check đơn") || lowerInput.contains("kiểm tra đơn")) {
            return "Dạ, để kiểm tra trạng thái đơn hàng nhanh nhất, bạn vui lòng cung cấp Số điện thoại đặt hàng hoặc Mã đơn hàng của bạn tại đây, hệ thống sẽ lập tức tra cứu thông tin vận chuyển cho bạn nhé!";
        }

        if (lowerInput.contains("nhân viên") || lowerInput.contains("nhan vien") ||
            lowerInput.contains("người thật") || lowerInput.contains("nguoi that") ||
            lowerInput.contains("admin") || lowerInput.contains("gặp nhân viên") ||
            lowerInput.contains("gặp hỗ trợ") || lowerInput.contains("gap ho tro") ||
            lowerInput.contains("gọi hỗ trợ") || lowerInput.contains("goi ho tro") ||
            lowerInput.contains("liên hệ hỗ trợ") || lowerInput.contains("lien he ho tro") ||
            lowerInput.contains("kết nối hỗ trợ") || lowerInput.contains("ket noi ho tro") ||
            lowerInput.contains("nói chuyện với hỗ trợ") || lowerInput.contains("noi chuyen voi ho tro") ||
            lowerInput.contains("gọi admin") || lowerInput.contains("goi admin")) {
            return "Tôi đã kết nối bạn với nhân viên hỗ trợ. Vui lòng chờ trong giây lát...";
        }

        if (lowerInput.contains("thanh toán") || lowerInput.contains("chuyển khoản") || lowerInput.contains("tài khoản") || lowerInput.contains("banking") || lowerInput.contains("momo") || lowerInput.contains("ngân hàng")) {
            return "AeroStride hỗ trợ thanh toán qua:\n1. Thanh toán tiền mặt khi nhận hàng (COD).\n2. Chuyển khoản ngân hàng Vietcombank (VCB):\n   - Chủ tài khoản: CONG TY TNHH AEROSTRIDE\n   - Số tài khoản: 123456789\n   - Nội dung CK: [Số điện thoại] hoặc [Mã đơn hàng]";
        }

        return "Dạ, AeroStride trợ lý ảo xin nghe! Bạn có muốn tôi giới thiệu các mẫu giày bán chạy hay tư vấn thêm về size giày, phí vận chuyển và chế độ đổi trả không ạ? Hãy cứ nhắn để tôi hỗ trợ nhé!";
    }


    private String handleApologizingState(ChatState state, String normalizedInput) {
        String lowerInput = normalizedInput.toLowerCase();

        // Khách hàng bao dung đồng ý nghe giới thiệu sản phẩm thay thế
        if (isAffirmativeResponse(normalizedInput)) {
            state.currentState = "SATISFACTION_CHECK";
            state.conversationData.put("rejectionCount", 0);

            if (!state.alternativeProducts.isEmpty()) {
                String current = state.alternativeProducts.poll();
                state.conversationData.put("currentProduct", current);
                return "Dạ tuyệt vời quá ạ! Em vô cùng cảm ơn anh/chị đã bao dung và cho shop cơ hội được phục vụ tốt hơn. 🥰\n\n" +
                       "Shop xin gửi tặng mình mã voucher **SORRY15** (giảm 15% cho mọi sản phẩm) để bù đắp trải nghiệm không vui vừa rồi.\n\n" +
                       "Đầu tiên, em xin giới thiệu mẫu giày siêu êm ái bán chạy nhất: **" + current + "**.\n" +
                       "Dòng này có lớp lót kháng khuẩn đàn hồi cao, thiết kế thông minh nâng đỡ lòng bàn chân cực tốt.\n\n" +
                       "Không biết mẫu giày này có làm anh/chị cảm thấy ưng ý hay hài lòng chưa ạ? 💕";
            } else {
                state.currentState = "COLLECTING_PREFERENCES";
                return "Dạ tuyệt vời quá ạ! Em vô cùng cảm ơn anh/chị đã mở lòng. 🥰 Shop gửi tặng mình mã giảm giá **SORRY15** bồi thường 15% nha.\n\n" +
                       "Để shop chọn được mẫu giày chuẩn gu nhất cho mình, anh/chị có thể cho shop biết mình thích dòng giày đi **chạy bộ, tập luyện** hay đi **chơi phố, đi làm**? Tông **màu sáng** hay **màu tối** ạ?";
            }
        }

        // Khách từ chối, yêu cầu gặp người thật hoặc hoàn tiền
        if (isNegativeResponse(normalizedInput) ||
            lowerInput.contains("hoàn tiền") || lowerInput.contains("người thật") || lowerInput.contains("nguoi that") ||
            lowerInput.contains("nhân viên") || lowerInput.contains("nhan vien") ||
            lowerInput.contains("gặp hỗ trợ") || lowerInput.contains("gap ho tro") ||
            lowerInput.contains("gặp nhân viên") || lowerInput.contains("admin")) {

            state.currentState = "NORMAL";
            state.alternativeProducts.clear();
            state.conversationData.clear();
            return "Dạ, shop hoàn toàn thấu hiểu và vô cùng lấy làm tiếc vì sự cố vừa rồi đã làm anh/chị bực mình đến vậy. 🥺\n\n" +
                   "Để bảo vệ quyền lợi tối đa và giải quyết nhanh nhất (đổi trả sản phẩm mới miễn phí hoặc hoàn tiền 100%), em đã chuyển tiếp trực tiếp hội thoại này sang **Trưởng bộ phận Chăm sóc khách hàng (Người thật)** trực tổng đài.\n\n" +
                   "Bạn ấy sẽ chủ động gọi điện hoặc nhắn tin hỗ trợ anh/chị giải quyết thoả đáng trong 3 phút tới ạ. Cảm ơn anh/chị đã phản ánh để shop nâng cấp dịch vụ!";
        }

        // Phản hồi khác
        return "Dạ, AeroStride rất mong muốn khắc phục lỗi lầm vừa rồi để đem lại sự hài lòng tối đa cho anh/chị. Anh/chị có sẵn lòng nhận voucher bồi thường **SORRY15** và xem đề cử mẫu giày thay thế cực tốt bên em không ạ? (Anh/chị có thể nhắn 'Đồng ý' hoặc 'Không' nha).";
    }

    private String handleSatisfactionCheckState(ChatState state, String normalizedInput) {
        String lowerInput = normalizedInput.toLowerCase();
        String currentProduct = (String) state.conversationData.getOrDefault("currentProduct", "Giày AeroStride cao cấp");
        int rejectionCount = (int) state.conversationData.getOrDefault("rejectionCount", 0);

        // Khách hàng đồng ý/ưng ý mẫu hiện tại
        if (isAffirmativeResponse(normalizedInput)) {
            state.currentState = "SIZING_CONSULTATION";
            return "Dạ tuyệt vời ông mặt trời luôn ạ! 😍 Nghe anh/chị ưng ý mẫu giày **" + currentProduct + "** là em hạnh phúc vô cùng!\n\n" +
                   "Mẫu này lên chân sang chảnh mà lại cực bền bỉ nữa. Bây giờ shop hỗ trợ tư vấn size giày nhé.\n" +
                   "Anh/chị thường đi size bao nhiêu, hoặc có thể cho shop xin chiều dài bàn chân đo bằng cm (ví dụ: '25cm' hoặc 'size 41') để shop giữ đúng size chuẩn nhất cho mình ạ? 🥰";
        }

        // Khách hàng chê/không ưng/muốn mẫu khác
        if (isNegativeResponse(normalizedInput)) {
            rejectionCount++;
            state.conversationData.put("rejectionCount", rejectionCount);

            if (!state.alternativeProducts.isEmpty()) {
                String nextProduct = state.alternativeProducts.poll();
                state.conversationData.put("currentProduct", nextProduct);

                String transitionPhrase = "Dạ shop hiểu ạ! Gu thẩm mỹ của mỗi người đều có nét riêng biệt độc đáo. Đừng lo lắng, em lập tức lấy ra một mẫu giày mang phong cách khác biệt hoàn toàn từ bộ sưu tập mới đây ạ:\n\n";
                if (rejectionCount >= 2) {
                    transitionPhrase = "Dạ vâng, shop quyết không bỏ cuộc đến khi tìm được đôi giày hoàn hảo nhất cho mình! Em đã lọc kỹ trong kho ra một siêu phẩm tiếp theo:\n\n";
                }

                return transitionPhrase + "👟 Gợi ý tiếp theo là: **" + nextProduct + "**.\n" +
                       "Dòng này có phối màu cực kỳ thời thượng, phong cách năng động thể thao trẻ trung.\n\n" +
                       "Anh/chị có thấy mẫu này ưng mắt và làm mình hài lòng hơn chưa ạ? 💕";
            } else {
                // Queue rỗng, chuyển qua thu thập sở thích chi tiết để dệt tiếp cuộc trò chuyện
                state.currentState = "COLLECTING_PREFERENCES";
                return "Dạ tiếc quá, những mẫu đề xuất sẵn trong hàng đợi hôm nay của shop vẫn chưa làm anh/chị thật sự ưng ý. 🥺 Nhưng không sao đâu ạ, kho hàng AeroStride phong phú lắm!\n\n" +
                       "Để em 'dệt' nên danh sách sản phẩm chuẩn gu nhất cho riêng mình, anh/chị có thể chia sẻ thêm sở thích được không ạ?\n" +
                       "- Mình thích giày đi **chạy bộ, tập thể thao** hay đi **chơi phố, đi làm**?\n" +
                       "- Mình chuộng tông **màu sáng nổi bật** hay tông **màu tối giản thanh lịch** ạ? 💖";
            }
        }

        // Phản hồi thông thường
        state.currentState = "NORMAL";
        state.alternativeProducts.clear();
        state.conversationData.clear();
        return "Dạ shop đã ghi nhận phản hồi của mình ạ. " + generateRuleBasedResponse(state, normalizedInput);
    }

    private String handleCollectingPreferencesState(ChatState state, String normalizedInput) {
        String lowerInput = normalizedInput.toLowerCase();

        String style = "thể thao";
        if (lowerInput.contains("chạy") || lowerInput.contains("tập") || lowerInput.contains("gym") || lowerInput.contains("thể thao")) {
            style = "giày chạy bộ/thể thao năng động";
        } else if (lowerInput.contains("chơi") || lowerInput.contains("đi làm") || lowerInput.contains("phố") || lowerInput.contains("casual") || lowerInput.contains("sneaker")) {
            style = "giày đi chơi/streetwear thời thượng";
        }

        String color = "đa dạng màu sắc";
        if (lowerInput.contains("sáng") || lowerInput.contains("trắng") || lowerInput.contains("đỏ") || lowerInput.contains("vàng")) {
            color = "tông màu sáng nổi bật tinh tế";
        } else if (lowerInput.contains("tối") || lowerInput.contains("đen") || lowerInput.contains("xám") || lowerInput.contains("nâu")) {
            color = "tông màu tối thanh lịch sạch sẽ";
        }

        state.conversationData.put("preferredStyle", style + " + " + color);

        // Tìm kiếm thực tế trong cơ sở dữ liệu dựa trên từ khóa tìm được hoặc nạp lại top products
        List<SanPham> searchResults = dataLibrary.searchProducts(lowerInput.contains("chạy") ? "Runner" : "Aero");
        if (searchResults.isEmpty()) {
            searchResults = dataLibrary.getTopProducts(4);
        }

        state.alternativeProducts.clear();
        for (SanPham p : searchResults) {
            state.alternativeProducts.add(p.getTen() + " (Mã: " + p.getMa() + ")");
        }

        if (!state.alternativeProducts.isEmpty()) {
            String current = state.alternativeProducts.poll();
            state.conversationData.put("currentProduct", current);
            state.currentState = "SATISFACTION_CHECK";
            state.conversationData.put("rejectionCount", 0);

            return "Dạ em đã ghi nhận chính xác gu của mình rồi ạ: thuộc dòng **" + style + "** và ưa thích **" + color + "**. Quả là sự lựa chọn đầy cá tính! 😎\n\n" +
                   "Em đã tức tốc rà soát kho hàng và tìm ra mẫu giày hoàn hảo nhất cho mình đây:\n\n" +
                   "👟 Mẫu đề xuất: **" + current + "**.\n" +
                   "Thiết kế này hội tụ đầy đủ tiêu chí của anh/chị, mang lại sự tự tin và thoải mái nhất khi đi.\n\n" +
                   "Không biết mẫu giày chuẩn gu này đã làm anh/chị hài lòng chưa ạ? 🥰";
        } else {
            state.currentState = "NORMAL";
            return "Dạ tiếc quá, hiện tại các sản phẩm đúng tiêu chí phong cách của anh/chị đang tạm cháy hàng tại hệ thống. 🥺 Anh/chị có muốn shop liên hệ trực tiếp khi hàng về, hoặc xem thử chính sách khuyến mãi đặc biệt 50% đang áp dụng không ạ?";
        }
    }

    private String handleSizingConsultationState(ChatState state, String normalizedInput) {
        String lowerInput = normalizedInput.toLowerCase();
        String currentProduct = (String) state.conversationData.getOrDefault("currentProduct", "Giày AeroStride cao cấp");

        // 1. Kiểm tra nếu khách nhập size trực tiếp (ví dụ: size 41, 42, 43, hoặc chỉ nhắn số 41)
        java.util.regex.Pattern sizePattern = java.util.regex.Pattern.compile("(?:size|cỡ)?\\s*(3[89]|4[012345])");
        java.util.regex.Matcher sizeMatcher = sizePattern.matcher(lowerInput);
        if (sizeMatcher.find()) {
            String sizeVal = sizeMatcher.group(1);
            state.conversationData.put("shoeSize", sizeVal);

            state.currentState = "NORMAL"; // Kết thúc chu kỳ chốt size thành công
            return "Dạ tuyệt vời quá ạ! Em đã ghi nhận và chính thức **khóa giữ hàng 1 đôi " + currentProduct + " size " + sizeVal + "** cho mình trên hệ thống showroom rồi nhé! 🥳\n\n" +
                   "Để shop gửi tặng kèm mã voucher giảm giá (nếu có) và giao tận nhà nhanh chóng trong 1-2 ngày tới, anh/chị có thể vui lòng cung cấp **Số điện thoại nhận hàng + Địa chỉ cụ thể** tại đây được không ạ? Nhân viên chốt đơn sẽ liên hệ xác nhận ngay lập tức cho mình ạ! 🚚💨";
        }

        // 2. Kiểm tra nếu khách nhập chiều dài bàn chân để tự động tính size (ví dụ: 25cm, 25.5 cm, 26 cm, 25.3)
        java.util.regex.Pattern cmPattern = java.util.regex.Pattern.compile("(\\d{2}(?:\\.\\d+)?)\\s*(?:cm|centimet|chiều dài)?");
        java.util.regex.Matcher cmMatcher = cmPattern.matcher(lowerInput);
        if (cmMatcher.find()) {
            try {
                double length = Double.parseDouble(cmMatcher.group(1));
                String determinedSize = "40";
                if (length <= 24.5) {
                    determinedSize = "39";
                } else if (length <= 25.0) {
                    determinedSize = "40";
                } else if (length <= 25.5) {
                    determinedSize = "41";
                } else if (length <= 26.0) {
                    determinedSize = "42";
                } else if (length <= 26.5) {
                    determinedSize = "43";
                } else {
                    determinedSize = "44";
                }

                state.conversationData.put("shoeSize", determinedSize);
                state.conversationData.put("footLength", length);

                state.currentState = "NORMAL"; // Hoàn tất chọn size
                return "Dạ, áp dụng thuật toán đo giày AeroStride chuẩn: với chiều dài bàn chân **" + length + " cm**, size giày khuyên dùng ôm chân êm nhất của anh/chị là **size " + determinedSize + "** ạ! Form giày dòng **" + currentProduct + "** cực kỳ thoải mái và co giãn tốt.\n\n" +
                       "Em đã lập tức lập đơn nháp và **giữ hàng size " + determinedSize + "** cho mình rồi ạ. Anh/chị có muốn shop gửi giao hàng ngay hôm nay không ạ? Mình chỉ cần cho shop xin **Số điện thoại + Địa chỉ nhận hàng** thôi nhé! Shop xin tặng kèm voucher ưu đãi đặc biệt ạ! 🥰";
            } catch (Exception e) {
                log.error("Lỗi parse centimet chân khách hàng: ", e);
            }
        }

        // Nếu không nhận diện được
        return "Dạ shop chưa nhận diện được số đo size chân của mình. Anh/chị có thể cung cấp chiều dài bàn chân cụ thể bằng cm (ví dụ: '25.5cm') hoặc ghi rõ size giày mình thường mang (ví dụ: 'size 41') để shop lập đơn giữ hàng chuẩn xác nhất nha! 👟";
    }

    private boolean isMatch(String input, String keywordString) {
        if (keywordString == null || keywordString.isEmpty()) return false;

        Pattern pattern = compiledPatterns.computeIfAbsent(keywordString, kws -> {
            String[] keywords = kws.split(",");
            StringBuilder regex = new StringBuilder(".*\\b(?:");
            for (int i = 0; i < keywords.length; i++) {
                if (i > 0) {
                    regex.append("|");
                }
                regex.append(Pattern.quote(keywords[i].trim().toLowerCase()));
            }
            regex.append(")\\b.*");
            return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
        });

        return pattern.matcher(input.toLowerCase()).matches();
    }

    private String formatResponse(KienThucAi knowledge, String input) {
        String template = knowledge.getMauCauTraLoi();

        switch (knowledge.getMucDich()) {
            case "PRODUCT_LIST":
                String productsInfo = dataLibrary.getTopProductsInfo(5);
                return template.replace("{products}", productsInfo);

            case "PRODUCT_SEARCH":
                String searchTerm = extractSearchTerm(input, knowledge.getTuKhoa());
                List<SanPham> searchResult = dataLibrary.searchProducts(searchTerm);
                if (searchResult.isEmpty() || searchTerm.isEmpty()) {
                    return "Dạ, bạn muốn tìm loại gì hoặc là muốn tìm dòng sản phẩm nào ạ? 🥰";
                }

                try {
                    String json = objectMapper.writeValueAsString(searchResult);
                    return template.replace("{search_term}", searchTerm) + " [[PRODUCT_JSON:" + json + "]]";
                } catch (Exception e) {
                    return template.replace("{search_term}", searchTerm);
                }

            default:
                return template;
        }
    }

    private String extractSearchTerm(String input, String keywords) {
        String result = input;
        for (String kw : keywords.split(",")) {
            result = result.replace(kw.trim().toLowerCase(), "");
        }
        return result.trim();
    }

    private boolean isNegativeResponse(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase().trim();
        lower = lower.replaceAll("[.,!?#]", "").trim();

        if (lower.equals("không") || lower.equals("khong") || lower.equals("ko") || lower.equals("k") || lower.equals("no")) {
            return true;
        }

        return lower.contains("không thích") || lower.contains("khong thich") ||
               lower.contains("không ưng") || lower.contains("khong ung") ||
               lower.contains("xấu") || lower.contains("xau") ||
               lower.contains("tệ") || lower.contains("te") ||
               lower.contains("chê") || lower.contains("che") ||
               lower.contains("chán") || lower.contains("chan") ||
               lower.contains("không được") || lower.contains("khong duoc") ||
               lower.contains("không hài lòng") || lower.contains("khong hai long") ||
               lower.contains("khác") || lower.contains("khac") ||
               lower.contains("đổi") || lower.contains("doi") ||
               lower.contains("chuyển") || lower.contains("chuyen") ||
               lower.contains("loại nào") || lower.contains("loai nao") ||
               lower.contains("mẫu nào") || lower.contains("mau nao") ||
               lower.contains("sao mãi") || lower.contains("sao mai") ||
               lower.contains("một kiểu") || lower.contains("mot kieu") ||
               lower.contains("1 kiểu") || lower.contains("1 kieu") ||
               lower.contains("mãi") || lower.contains("mai") ||
               lower.contains("đoàng hoàng") || lower.contains("doang hoang") ||
               lower.contains("đàng hoàng") || lower.contains("dang hoang") ||
               lower.contains("không còn") || lower.contains("khong con") ||
               lower.contains("còn ko") || lower.contains("con ko") ||
               lower.contains("còn k") || lower.contains("con k");
    }

    private boolean isAffirmativeResponse(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase().trim();
        lower = lower.replaceAll("[.,!?#]", "").trim();

        if (lower.equals("có") || lower.equals("co") || lower.equals("dạ") || lower.equals("da") ||
            lower.equals("được") || lower.equals("duoc") || lower.equals("ok") || lower.equals("yes") ||
            lower.equals("ừ") || lower.equals("uha") || lower.equals("uh") || lower.equals("yep") ||
            lower.equals("hài lòng") || lower.equals("hai long") || lower.equals("ưng") || lower.equals("ung") ||
            lower.equals("đẹp") || lower.equals("dep") || lower.equals("thích") || lower.equals("thich")) {
            return true;
        }

        return lower.contains("ưng ý") || lower.contains("ung y") ||
               lower.contains("rất ưng") || lower.contains("rat ung") ||
               lower.contains("hài lòng") || lower.contains("hai long") ||
               lower.contains("đẹp") || lower.contains("dep") ||
               lower.contains("thích") || lower.contains("thich") ||
               lower.contains("lấy mẫu này") || lower.contains("lay mau nay") ||
               lower.contains("chốt") || lower.contains("chot") ||
               lower.contains("đồng ý") || lower.contains("dong y") ||
               lower.contains("xem thử") || lower.contains("xem thu") ||
               lower.contains("gửi đi") || lower.contains("gui di") ||
               lower.contains("lấy luôn") || lower.contains("lay luon") ||
               lower.contains("duyệt") || lower.contains("duyet");
    }
}

