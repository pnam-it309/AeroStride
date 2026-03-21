package com.example.be.repository.base;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;

/**
 * Base repository interface containing common methods for all repositories.
 * Following the architectural audit to keep specific repositories clean.
 */
@NoRepositoryBean
public interface BaseRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {
    // Add common methods here if needed, e.g., findAllActive()
}
