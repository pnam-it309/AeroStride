package com.example.be.infrastructure.listener;

import com.example.be.entity.PhieuGiamGia;
import com.example.be.infrastructure.websocket.WebSocketContextHolder;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashMap;
import java.util.Map;

public class PhieuGiamGiaListener {

    @PostUpdate
    @PostPersist
    public void onPostUpdateOrPersist(PhieuGiamGia pgg) {
        try {
            SimpMessagingTemplate template = WebSocketContextHolder.getBean(SimpMessagingTemplate.class);
            if (template != null && pgg != null && pgg.getId() != null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "VOUCHER_UPDATE");
                payload.put("id", pgg.getId());
                payload.put("ma", pgg.getMa());
                payload.put("soLuong", pgg.getSoLuong());
                payload.put("trangThai", pgg.getTrangThai() != null ? pgg.getTrangThai().name() : null);
                template.convertAndSend("/topic/voucher-updates", payload);
            }
        } catch (Exception e) {
            // Ignore websocket broadcast errors during entity update
        }
    }
}
