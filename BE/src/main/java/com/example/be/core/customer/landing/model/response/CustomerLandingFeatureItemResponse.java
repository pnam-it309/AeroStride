package com.example.be.core.customer.landing.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLandingFeatureItemResponse {
    private String title;
    private String desc;
    private String icon;
    private String color;
    private String size;
    private String queryParam;
    private String queryValue;
}
