package com.example.be.core.admin.banhang.repository;

import com.example.be.entity.PhuongThucThanhToan;
import com.example.be.repository.PhuongThucThanhToanRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminBanHangPhuongThucThanhToanRepository extends PhuongThucThanhToanRepository {
    PhuongThucThanhToan findByMa(String ma);
}
