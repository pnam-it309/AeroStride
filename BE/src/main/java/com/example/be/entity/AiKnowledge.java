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
@Table(name = "ai_knowledge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiKnowledge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "intent", nullable = false)
    private String intent;

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords; // Comma-separated keywords or patterns

    @Column(name = "response_template", columnDefinition = "TEXT")
    private String responseTemplate;

    @Column(name = "priority")
    private Integer priority;
}
