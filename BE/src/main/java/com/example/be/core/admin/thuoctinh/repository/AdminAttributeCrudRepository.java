package com.example.be.core.admin.thuoctinh.repository;

import com.example.be.core.common.base.BaseCodeNameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface AdminAttributeCrudRepository<E extends BaseCodeNameEntity>
        extends JpaRepository<E, String>, JpaSpecificationExecutor<E> {
}
