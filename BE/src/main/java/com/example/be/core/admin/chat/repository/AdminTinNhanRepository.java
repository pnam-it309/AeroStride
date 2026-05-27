package com.example.be.core.admin.chat.repository;

import com.example.be.entity.TinNhan;
import com.example.be.repository.TinNhanRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminTinNhanRepository extends TinNhanRepository, JpaSpecificationExecutor<TinNhan> {
    List<TinNhan> findByCuocHoiThoai_IdOrderByNgayTaoAsc(String conversationId);
    List<TinNhan> findByCuocHoiThoai_MaPhienOrderByNgayTaoAsc(String sessionId);

    // Lấy N tin nhắn gần nhất của cuộc hội thoại (dùng cho AI context history)
    List<TinNhan> findTop10ByCuocHoiThoai_IdOrderByNgayTaoDesc(String conversationId);
}
