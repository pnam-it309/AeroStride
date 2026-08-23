package com.example.be.core.admin.chat.repository;

import com.example.be.entity.CuocHoiThoai;
import com.example.be.entity.TinNhan;
import com.example.be.repository.TinNhanRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminTinNhanRepository extends TinNhanRepository, JpaSpecificationExecutor<TinNhan> {
    List<TinNhan> findByCuocHoiThoai_IdOrderByNgayTaoAsc(String conversationId);
    List<TinNhan> findByCuocHoiThoai_MaPhienOrderByNgayTaoAsc(String sessionId);

    // Lấy N tin nhắn gần nhất của cuộc hội thoại (dùng cho AI context history)
    List<TinNhan> findTop10ByCuocHoiThoai_IdOrderByNgayTaoDesc(String conversationId);

    Optional<TinNhan> findTopByCuocHoiThoaiOrderByNgayTaoDesc(CuocHoiThoai cuocHoiThoai);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM TinNhan t WHERE t.cuocHoiThoai.id = :conversationId AND (t.daDoc = false OR t.daDoc IS NULL) AND (t.loaiNguoiGui != :loaiNguoiGui OR t.loaiNguoiGui IS NULL)")
    int countUnreadForCustomerConv(@org.springframework.data.repository.query.Param("conversationId") String conversationId, @org.springframework.data.repository.query.Param("loaiNguoiGui") String loaiNguoiGui);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM TinNhan t WHERE t.cuocHoiThoai.id = :conversationId AND (t.daDoc = false OR t.daDoc IS NULL) AND (t.idNguoiGui != :idNguoiGui OR t.idNguoiGui IS NULL)")
    int countUnreadForInternalConv(@org.springframework.data.repository.query.Param("conversationId") String conversationId, @org.springframework.data.repository.query.Param("idNguoiGui") String idNguoiGui);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM TinNhan t WHERE t.cuocHoiThoai.maPhien = :sessionId AND (t.daDoc = false OR t.daDoc IS NULL) AND (t.loaiNguoiGui != :loaiNguoiGui OR t.loaiNguoiGui IS NULL)")
    int countUnreadForSession(@org.springframework.data.repository.query.Param("sessionId") String sessionId, @org.springframework.data.repository.query.Param("loaiNguoiGui") String loaiNguoiGui);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE TinNhan t SET t.daDoc = true WHERE t.cuocHoiThoai.id = :conversationId AND (t.daDoc = false OR t.daDoc IS NULL)")
    void markAllAsReadByConversationId(@org.springframework.data.repository.query.Param("conversationId") String conversationId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE TinNhan t SET t.daDoc = true WHERE t.cuocHoiThoai.maPhien = :sessionId AND (t.daDoc = false OR t.daDoc IS NULL)")
    void markAllAsReadBySessionId(@org.springframework.data.repository.query.Param("sessionId") String sessionId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM TinNhan t WHERE t.cuocHoiThoai.id = :conversationId")
    void deleteByConversationId(@org.springframework.data.repository.query.Param("conversationId") String conversationId);
}

