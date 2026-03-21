package com.example.be.core.common.base;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public interface BaseService<T extends PrimaryEntity, ID extends Serializable> {
    List<T> findAll();
    Page<T> findAll(Pageable pageable);
    Optional<T> findById(ID id);
    T save(T entity);
    T update(ID id, T entity);
    void deleteById(ID id);
}
