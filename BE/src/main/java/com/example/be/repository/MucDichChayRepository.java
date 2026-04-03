package com.example.be.repository;

import com.example.be.entity.MucDichChay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MucDichChayRepository extends JpaRepository<MucDichChay, String> {
}
