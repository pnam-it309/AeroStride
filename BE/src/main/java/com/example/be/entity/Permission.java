package com.example.be.entity;

import com.example.be.core.common.base.PrimaryEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends PrimaryEntity {

    @Column(unique = true, nullable = false)
    private String name;
}
