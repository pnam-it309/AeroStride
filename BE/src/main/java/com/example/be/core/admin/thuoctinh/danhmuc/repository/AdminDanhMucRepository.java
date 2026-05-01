package com.example.be.core.admin.thuoctinh.danhmuc.repository;

import com.example.be.core.admin.thuoctinh.repository.AdminAttributeCrudRepository;
import com.example.be.entity.DanhMuc;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminDanhMucRepository extends AdminAttributeCrudRepository<DanhMuc> {
}
