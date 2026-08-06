package com.example.be.core.customer.sanpham.model.request;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class RecommendQuizRequest {
    private Map<String, String> answers;
}
