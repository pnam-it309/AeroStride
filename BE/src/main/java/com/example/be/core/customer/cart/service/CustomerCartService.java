package com.example.be.core.customer.cart.service;
import com.example.be.core.customer.cart.model.request.CustomerCartSyncRequest;
import com.example.be.core.customer.cart.model.response.CustomerCartSyncResponse;
public interface CustomerCartService {
    CustomerCartSyncResponse syncCart(CustomerCartSyncRequest request);
}