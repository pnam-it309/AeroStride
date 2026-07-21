package com.example.be.core.customer.chat.repository;

import com.example.be.entity.TinNhan;
import com.example.be.repository.TinNhanRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerTinNhanRepository extends TinNhanRepository {
    List<TinNhan> findByCuocHoiThoai_IdOrderByNgayTaoAsc(String conversationId);
    List<TinNhan> findByCuocHoiThoai_MaPhienOrderByNgayTaoAsc(String sessionId);
    List<TinNhan> findTop10ByCuocHoiThoai_IdOrderByNgayTaoDesc(String conversationId);
}
