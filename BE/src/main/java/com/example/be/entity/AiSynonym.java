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
@Table(name = "ai_synonym")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSynonym {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "word", nullable = false)
    private String word; // User's word

    @Column(name = "canonical_word", nullable = false)
    private String canonicalWord; // Standard word for logic
}
