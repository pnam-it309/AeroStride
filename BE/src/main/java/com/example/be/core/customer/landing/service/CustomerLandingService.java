package com.example.be.core.customer.landing.service;

import com.example.be.core.customer.landing.model.response.CustomerLandingProductResponse;
import com.example.be.core.customer.landing.model.response.CustomerLandingVariantResponse;
import com.example.be.core.customer.landing.model.response.CustomerLandingFeatureItemResponse;

import java.util.List;

public interface CustomerLandingService {
    List<CustomerLandingProductResponse> getLandingProducts(Integer size);
    List<CustomerLandingVariantResponse> getFeaturedVariants(Integer size);
    List<CustomerLandingVariantResponse> getTopVariantsByQuantity(Integer size);
    List<List<CustomerLandingFeatureItemResponse>> getLandingFeatures();
}
