package com.example.be.core.common.base;

import com.example.be.infrastructure.constants.TrangThai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * BaseServiceImpl
 * Implements Soft Delete by default in the deleteById method.
 */
public abstract class BaseServiceImpl<T extends PrimaryEntity, ID extends Serializable> implements BaseService<T, ID> {

    protected abstract JpaRepository<T, ID> getRepository();

    @Override
    public List<T> findAll() {
        return getRepository().findAll();
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        return getRepository().findAll(pageable);
    }

    @Override
    public Optional<T> findById(ID id) {
        return getRepository().findById(id);
    }

    @Override
    @Transactional
    public T save(T entity) {
        return getRepository().save(entity);
    }

    @Override
    @Transactional
    public T update(ID id, T entity) {
        if (getRepository().existsById(id)) {
            entity.setId((String) id); // Assumes ID is String as per PrimaryEntity
            return getRepository().save(entity);
        }
        return null;
    }

    /**
     * Soft Delete Implementation
     * Instead of physically deleting from the database, we change the status.
     */
    @Override
    @Transactional
    public void deleteById(ID id) {
        getRepository().findById(id).ifPresent(entity -> {
            entity.setTrangThai(TrangThai.DA_XOA);
            getRepository().save(entity);
        });
    }
}
