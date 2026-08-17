package com.example.be.core.admin.lichlamviec.repository;

import com.example.be.entity.LichLamViec;
import com.example.be.repository.LichLamViecRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AdminLichLamViecRepository extends LichLamViecRepository, JpaSpecificationExecutor<LichLamViec> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"nhanVien", "caLam"})
    @org.springframework.data.jpa.repository.Query("SELECT l FROM LichLamViec l WHERE " +
           "(:ngayLam IS NULL OR l.ngayLam = :ngayLam) AND " +
           "(:caId IS NULL OR (l.caLam IS NOT NULL AND (l.caLam.id = :caId OR l.caLam.tenCa = :caId))) AND " +
           "(:keyword IS NULL OR (l.nhanVien IS NOT NULL AND (LOWER(l.nhanVien.ten) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(l.nhanVien.ma) LIKE LOWER(CONCAT('%', :keyword, '%'))))) " +
           "ORDER BY l.ngayLam DESC, l.ngayTao DESC")
    List<LichLamViec> searchSchedules(
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            @org.springframework.data.repository.query.Param("caId") String caId,
            @org.springframework.data.repository.query.Param("ngayLam") LocalDate ngayLam
    );

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"nhanVien", "caLam"})
    List<LichLamViec> findByNgayLamBetween(LocalDate start, LocalDate end);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"nhanVien", "caLam"})
    java.util.Optional<LichLamViec> findFirstByNhanVienIdAndNgayLamAndGioVaoIsNotNullAndGioRaIsNull(String nhanVienId, LocalDate ngayLam);
}
