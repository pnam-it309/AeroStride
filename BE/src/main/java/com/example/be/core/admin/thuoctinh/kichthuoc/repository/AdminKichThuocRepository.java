package com.example.be.core.admin.thuoctinh.kichthuoc.repository;

import com.example.be.entity.KichThuoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminKichThuocRepository extends JpaRepository<KichThuoc, String> {
}
