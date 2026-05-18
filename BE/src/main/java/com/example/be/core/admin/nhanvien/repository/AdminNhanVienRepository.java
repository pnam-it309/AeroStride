package com.example.be.core.admin.nhanvien.repository;

import com.example.be.core.admin.nhanvien.model.response.AdminNhanVienResponse;
import com.example.be.entity.NhanVien;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.repository.NhanVienRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminNhanVienRepository extends NhanVienRepository, JpaSpecificationExecutor<NhanVien> {

    @Override
    @EntityGraph(attributePaths = {"phanQuyen"})
    List<NhanVien> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = {"phanQuyen"})
    Page<NhanVien> findAll(Specification<NhanVien> spec, Pageable pageable);

    List<NhanVien> findByResetStatus(NhanVien.ResetStatus resetStatus);
    boolean existsByEmail(String email);
    boolean existsByTenTaiKhoan(String tenTaiKhoan);
    boolean existsByMa(String ma);
    boolean existsByEmailAndIdNot(String email, String id);
    boolean existsByTenTaiKhoanAndIdNot(String tenTaiKhoan, String id);
    boolean existsByMaAndIdNot(String ma, String id);

    interface MaOnly {
        String getMa();
    }

    List<MaOnly> findAllProjectedBy();
}
