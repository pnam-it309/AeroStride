package com.example.be.core.common.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import com.example.be.infrastructure.constants.EntityProperties;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.listener.PrimaryEntityListener;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(PrimaryEntityListener.class)
public abstract class PrimaryEntity extends AuditEntity implements IsIdentified {

    @Id
    @Column(length = EntityProperties.LENGTH_ID, updatable = false)
    private String id;

    @Column(name = "trang_thai")
    @Enumerated(EnumType.ORDINAL)
    private TrangThai trangThai;

}
