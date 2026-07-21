package com.example.be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tu_dong_nghia_ai")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TuDongNghiaAi {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tu_goc", nullable = false)
    private String tuGoc; // User's word

    @Column(name = "tu_chuan_hoa", nullable = false)
    private String tuChuanHoa; // Standard word for logic
}
