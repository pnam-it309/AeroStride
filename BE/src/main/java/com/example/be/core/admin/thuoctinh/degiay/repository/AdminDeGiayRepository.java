package com.example.be.core.admin.thuoctinh.degiay.repository;

import com.example.be.entity.DeGiay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminDeGiayRepository extends JpaRepository<DeGiay, String> {
}
