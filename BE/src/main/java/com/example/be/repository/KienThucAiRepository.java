package com.example.be.repository;

import com.example.be.entity.KienThucAi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KienThucAiRepository extends JpaRepository<KienThucAi, String> {
    List<KienThucAi> findAllByOrderByDoUuTienDesc();
}
