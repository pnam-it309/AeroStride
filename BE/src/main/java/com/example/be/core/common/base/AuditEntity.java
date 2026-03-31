package com.example.be.core.common.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditEntity {

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private Long ngayTao;

    @Column(name = "ngay_cap_nhat")
    private Long ngayCapNhat;

    @CreatedBy
    @Column(name = "nguoi_tao", updatable = false)
    private String nguoiTao;

    @LastModifiedBy
    @Column(name = "nguoi_cap_nhat")
    private String nguoiCapNhat;
}
