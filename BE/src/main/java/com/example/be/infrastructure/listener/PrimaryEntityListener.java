package com.example.be.infrastructure.listener;

import com.example.be.core.common.base.PrimaryEntity;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.utils.HelperUtils;
import com.example.be.utils.MaGenerator;
import jakarta.persistence.PrePersist;
import java.lang.reflect.Field;
import java.util.UUID;

public class PrimaryEntityListener {

    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof PrimaryEntity primaryEntity) {
            // Set ID if empty
            if (primaryEntity.getId() == null || primaryEntity.getId().isEmpty()) {
                primaryEntity.setId(HelperUtils.generateUUID());
            }
            // Set Status if empty
            if (primaryEntity.getTrangThai() == null) {
                primaryEntity.setTrangThai(TrangThai.DANG_HOAT_DONG);
            }
            // Set Created Time if empty
            if (primaryEntity.getNgayTao() == null) {
                primaryEntity.setNgayTao(System.currentTimeMillis());
            }
            // Set Ma if empty (via reflection if field exists)
            setMaIfEmpty(entity);
        }
    }

    private void setMaIfEmpty(Object entity) {
        try {
            Field maField = null;
            for (Field field : entity.getClass().getDeclaredFields()) {
                String name = field.getName();
                if (name.equals("ma") || (name.startsWith("ma") && !name.equals("matKhau"))) {
                    maField = field;
                    break;
                }
            }
            
            if (maField != null) {
                maField.setAccessible(true);
                Object value = maField.get(entity);
                if (value == null || value.toString().isEmpty()) {
                    maField.set(entity, MaGenerator.generate(entity.getClass()));
                }
            }
        } catch (Exception e) {
            // Reflection error, log or ignore.
        }
    }
}
