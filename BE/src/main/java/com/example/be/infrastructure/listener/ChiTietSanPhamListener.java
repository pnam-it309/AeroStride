package com.example.be.infrastructure.listener;

import com.example.be.entity.ChiTietSanPham;
import com.example.be.infrastructure.websocket.WebSocketContextHolder;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashMap;
import java.util.Map;

public class ChiTietSanPhamListener {

    @PostUpdate
    @PostPersist
    public void onPostUpdateOrPersist(ChiTietSanPham ctsp) {
        try {
            SimpMessagingTemplate template = WebSocketContextHolder.getBean(SimpMessagingTemplate.class);
            if (template != null && ctsp != null && ctsp.getId() != null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "PRODUCT_STOCK_UPDATE");
                payload.put("id", ctsp.getId());
                payload.put("idSanPham", ctsp.getSanPham() != null ? ctsp.getSanPham().getId() : null);
                payload.put("maChiTietSanPham", ctsp.getMaChiTietSanPham());
                payload.put("soLuongTon", ctsp.getSoLuong() != null ? ctsp.getSoLuong() : 0);
                template.convertAndSend("/topic/product-stock", payload);
            }
        } catch (Exception e) {
            // Ignore websocket broadcast errors during entity update
        }
    }
}
