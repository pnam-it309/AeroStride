package com.example.be.core.admin.danhgia.repository;

import com.example.be.entity.DanhGiaSanPham;
import com.example.be.repository.DanhGiaSanPhamRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminDanhGiaRepository extends DanhGiaSanPhamRepository, JpaSpecificationExecutor<DanhGiaSanPham> {
}
