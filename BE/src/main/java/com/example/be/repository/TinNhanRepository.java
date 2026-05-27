package com.example.be.repository;

import com.example.be.entity.TinNhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TinNhanRepository extends JpaRepository<TinNhan, String> {
}
