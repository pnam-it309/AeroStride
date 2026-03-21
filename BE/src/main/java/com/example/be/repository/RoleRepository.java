package com.example.be.repository;

import com.example.be.entity.Role;
import com.example.be.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends BaseRepository<Role, String> {
    Optional<Role> findByName(String name);
}
