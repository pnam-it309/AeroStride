package com.example.be.repository;

import com.example.be.entity.AiSynonym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiSynonymRepository extends JpaRepository<AiSynonym, String> {
    Optional<AiSynonym> findByWord(String word);
}
