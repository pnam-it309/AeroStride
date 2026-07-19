package com.example.be.core.admin.chat.controller;

import com.example.be.core.admin.chat.service.AiChatService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.entity.CuocHoiThoai;
import com.example.be.repository.CuocHoiThoaiRepository;
import com.example.be.repository.ChiTietSanPhamRepository;
import com.example.be.core.admin.hoadon.repository.AdminHoaDonRepository;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.be.infrastructure.constants.VaiTro;

@Slf4j
@RestController
@RequestMapping(RoutesConstant.ADMIN_AI_ASSISTANT)
@RequiredArgsConstructor
@PreAuthorize(VaiTro.PRE_AUTH_ADMIN_STAFF)
public class AdminAiAssistantController {

    private final AiChatService aiChatService;
    private final CuocHoiThoaiRepository cuocHoiThoaiRepository;
    private final AdminHoaDonRepository adminHoaDonRepository;
    private final ChiTietSanPhamRepository chiTietSanPhamRepository;

    @GetMapping("/summary/{id}")
    public ResponseEntity<ApiResponse<String>> summarizeChat(@PathVariable String id) {
        log.info("Admin AI Assistant requested summary for chat: {}", id);
        CuocHoiThoai chat = cuocHoiThoaiRepository.findById(id).orElse(null);
        if (chat == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Không tìm thấy cuộc hội thoại"));
        }
        
        String summary = aiChatService.summarizeChat(chat);
        return ResponseEntity.ok(ApiResponse.success(summary, "Lấy tóm tắt thành công"));
    }

    @GetMapping("/dashboard-insights")
    public ResponseEntity<ApiResponse<String>> getDashboardInsights() {
        log.info("Admin AI Assistant requested dashboard insights");
        // Count pending orders
        long pendingOrders = adminHoaDonRepository.countByTrangThai(OrderStatus.CHO_XAC_NHAN);
        
        // Count low stock products (< 5)
        long lowStockItems = chiTietSanPhamRepository.countBySoLuongLessThan(5);
        
        String insights = aiChatService.getDashboardInsights((int) pendingOrders, (int) lowStockItems);
        return ResponseEntity.ok(ApiResponse.success(insights, "Lấy phân tích thành công"));
    }
}
