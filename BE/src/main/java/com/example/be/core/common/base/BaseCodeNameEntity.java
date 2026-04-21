package com.example.be.core.common.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseCodeNameEntity extends PrimaryEntity {
    public Boolean getXoaMem() {
        try {
            return (Boolean) this.getClass().getDeclaredField("xoaMem").get(this);
        } catch (Exception e) {
            return null;
        }
    }

    @Column(name = "ma", unique = true)
    private String ma;

    @Column(name = "ten")
    private String ten;

}
