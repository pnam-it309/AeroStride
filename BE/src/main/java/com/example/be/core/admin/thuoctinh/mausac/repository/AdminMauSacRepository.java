package com.example.be.core.admin.thuoctinh.mausac.repository;

import com.example.be.entity.MauSac;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminMauSacRepository extends JpaRepository<MauSac, String> {
}
