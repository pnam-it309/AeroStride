package com.example.be.repository;

import com.example.be.entity.AiKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiKnowledgeRepository extends JpaRepository<AiKnowledge, String> {
    List<AiKnowledge> findAllByOrderByPriorityDesc();
}
