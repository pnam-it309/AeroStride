package com.example.be.core.customer.cart.model.response;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerCartSyncResponse {
    private List<CustomerCartItemResponse> items;
    private BigDecimal tongTien;
}