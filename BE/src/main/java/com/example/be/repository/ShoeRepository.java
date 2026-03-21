package com.example.be.repository;

import com.example.be.entity.Shoe;
import com.example.be.infrastructure.constants.EntityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ShoeRepository extends JpaRepository<Shoe, UUID> {
    Page<Shoe> findAllByStatusNot(EntityStatus status, Pageable pageable);
    Page<Shoe> findAllByNameContainingIgnoreCaseAndStatusNot(String name, EntityStatus status, Pageable pageable);
}
