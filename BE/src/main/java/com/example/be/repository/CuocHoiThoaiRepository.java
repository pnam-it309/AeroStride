package com.example.be.repository;

import com.example.be.entity.CuocHoiThoai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CuocHoiThoaiRepository extends JpaRepository<CuocHoiThoai, String> {

    /**
     * Tìm id các hội thoại khách vãng lai (có maPhien, CHƯA gắn khách hàng đăng nhập)
     * mà hoạt động gần nhất cũ hơn mốc thời gian truyền vào.
     * Mốc hoạt động = thời điểm tin nhắn mới nhất, hoặc thời điểm tạo nếu chưa có tin nhắn.
     * Hội thoại đã gắn khachHang (khách đã đăng nhập) KHÔNG bị chọn -> được giữ lại.
     */
    @Query("SELECT c.id FROM CuocHoiThoai c WHERE c.maPhien IS NOT NULL AND c.khachHang IS NULL " +
           "AND (SELECT COALESCE(MAX(t.ngayTao), c.ngayTao) FROM TinNhan t WHERE t.cuocHoiThoai = c) < :threshold")
    List<String> findStaleGuestConversationIds(@Param("threshold") Long threshold);

    @Modifying
    @Query("DELETE FROM CuocHoiThoai c WHERE c.id IN :ids")
    void deleteByIdIn(@Param("ids") List<String> ids);
}
