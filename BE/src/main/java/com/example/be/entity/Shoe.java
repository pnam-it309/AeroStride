package com.example.be.entity;

import com.example.be.core.common.base.PrimaryEntity;
import com.example.be.infrastructure.constants.EntityStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "shoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shoe extends PrimaryEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Builder.Default
    private Integer stock = 0;

    private String brand;

    private String category;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EntityStatus status = EntityStatus.ACTIVE;
}
