package com.example.be.infrastructure.listener;

import jakarta.persistence.PrePersist;
import com.example.be.core.common.base.PrimaryEntity;
import com.example.be.infrastructure.constants.EntityStatus;
import com.example.be.utils.HelperUtils;
import java.util.UUID;

public class PrimaryEntityListener {

    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof PrimaryEntity primaryEntity) {
            if (primaryEntity.getId() == null || primaryEntity.getId().isEmpty()) {
                primaryEntity.setId(HelperUtils.generateUUID());
            }
            if (primaryEntity.getStatus() == null) {
                primaryEntity.setStatus(EntityStatus.ACTIVE);
            }
            if (primaryEntity.getCreatedAt() == null) {
                primaryEntity.setCreatedAt(System.currentTimeMillis());
            }
        }
    }
}
