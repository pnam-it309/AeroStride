package com.example.be.core.customer.chat.repository;

import com.example.be.entity.CuocHoiThoai;
import com.example.be.repository.CuocHoiThoaiRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerCuocHoiThoaiRepository extends CuocHoiThoaiRepository {

    Optional<CuocHoiThoai> findByMaPhien(String maPhien);

    @Query("SELECT c FROM CuocHoiThoai c WHERE c.loaiHoiThoai = 'INTERNAL' AND " +
           "((c.nhanVien.id = :id1 AND c.nhanVienNhan.id = :id2) OR " +
           "(c.nhanVien.id = :id2 AND c.nhanVienNhan.id = :id1))")
    Optional<CuocHoiThoai> findInternalConversation(@Param("id1") String id1, @Param("id2") String id2);
}
