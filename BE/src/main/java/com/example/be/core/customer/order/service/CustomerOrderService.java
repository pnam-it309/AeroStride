package com.example.be.core.customer.order.service;

import com.example.be.core.customer.order.model.request.CustomerOrderCheckoutRequest;
import com.example.be.core.customer.order.model.request.CustomerUpdateItemsRequest;
import com.example.be.core.customer.order.model.request.CustomerUpdateShippingRequest;
import com.example.be.core.customer.order.model.response.CustomerOrderResponse;
import com.example.be.entity.PhieuGiamGia;

import java.math.BigDecimal;
import java.util.List;

import com.example.be.core.customer.order.model.response.CustomerOrderStatsResponse;

public interface CustomerOrderService {

    CustomerOrderResponse checkout(CustomerOrderCheckoutRequest request, String username);

    CustomerOrderStatsResponse getMyOrderStats(String username);

    // Lấy danh sách các đơn hàng (hóa đơn online) của khách hàng, có thể lọc theo trạng thái
    List<CustomerOrderResponse> getMyOrders(String username, String trangThai, String keyword);

    CustomerOrderResponse getOrderDetail(String id, String username);

    CustomerOrderResponse trackOrder(String maHoaDon, String soDienThoai);

    void cancelOrder(String id, String username);

    CustomerOrderResponse updateShippingInfo(String id, CustomerUpdateShippingRequest request, String username);

    CustomerOrderResponse updateItems(String id, CustomerUpdateItemsRequest request, String username);

    String createVnPayUrl(String id, String returnUrl, String username);

    List<PhieuGiamGia> getAvailableVouchers(BigDecimal tongTien, String username);
}
