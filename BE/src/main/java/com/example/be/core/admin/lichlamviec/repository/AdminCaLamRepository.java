package com.example.be.core.admin.lichlamviec.repository;

import com.example.be.entity.CaLam;
import com.example.be.repository.CaLamRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminCaLamRepository extends CaLamRepository, JpaSpecificationExecutor<CaLam> {
    List<CaLam> findByXoaMemFalse();
}
