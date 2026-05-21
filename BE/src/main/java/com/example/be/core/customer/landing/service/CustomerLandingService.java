package com.example.be.core.customer.landing.service;

import com.example.be.core.customer.landing.model.response.CustomerLandingProductResponse;
import java.util.List;

public interface CustomerLandingService {
    List<CustomerLandingProductResponse> getLandingProducts(Integer size);
}
