package com.example.be.core.admin.dotgiamgia.repository;

import com.example.be.entity.DotGiamGia;
import com.example.be.repository.DotGiamGiaRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdminDotGiamGiaRepository extends DotGiamGiaRepository, JpaSpecificationExecutor<DotGiamGia> {
}
