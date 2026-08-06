package com.example.be.core.customer.sanpham.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Builder
public class RecommendQuizQuestion {
    private String key;
    private String questionText;
    private List<RecommendQuizOption> options;
}
