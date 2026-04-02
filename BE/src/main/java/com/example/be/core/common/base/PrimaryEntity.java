package com.example.be.core.common.base;

import jakarta.persistence.*;
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
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = EntityProperties.LENGTH_ID, updatable = false)
    private String id;

    @Column(name = "trang_thai")
    @Enumerated(EnumType.ORDINAL)
    private TrangThai trangThai;

}
