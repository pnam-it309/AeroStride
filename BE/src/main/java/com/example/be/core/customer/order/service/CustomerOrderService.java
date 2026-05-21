package com.example.be.core.customer.order.service;

import com.example.be.core.customer.order.model.request.CustomerOrderCheckoutRequest;
import com.example.be.core.customer.order.model.response.CustomerOrderResponse;
import com.example.be.entity.PhieuGiamGia;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerOrderService {

    CustomerOrderResponse checkout(CustomerOrderCheckoutRequest request, String username);

    List<CustomerOrderResponse> getMyOrders(String username, String trangThai);

    CustomerOrderResponse getOrderDetail(String id, String username);

    void cancelOrder(String id, String username);

    List<PhieuGiamGia> getAvailableVouchers(BigDecimal tongTien);
}
