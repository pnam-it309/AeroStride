package com.example.be.core.customer.sanpham.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Builder
public class RecommendQuizResponse {
    private RecommendQuizQuestion nextQuestion;
    private List<CustomerProductResponse> recommendedProducts;
}
