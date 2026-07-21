package com.example.be.core.customer.cart.model.request;
import lombok.*;
import java.util.List;
@Getter
@Setter
public class CustomerCartSyncRequest {
    private List<CustomerCartSyncItemRequest> items;
}