package com.example.be.core.admin.thongke.repository;

import com.example.be.core.admin.thongke.model.response.AdminThongKeResponse;
import com.example.be.entity.HoaDon;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface AdminThongKeRepositoryCustom {
    List<AdminThongKeResponse.DoanhThuNgay> getDoanhThuTheoNgay(Specification<HoaDon> spec);
}
