package com.example.be.repository;

import com.example.be.entity.TuDongNghiaAi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TuDongNghiaAiRepository extends JpaRepository<TuDongNghiaAi, String> {
    Optional<TuDongNghiaAi> findByTuGoc(String tuGoc);
}
