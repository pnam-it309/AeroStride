package com.example.be.repository;

import com.example.be.entity.Permission;
import com.example.be.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends BaseRepository<Permission, String> {
    Optional<Permission> findByName(String name);
}
