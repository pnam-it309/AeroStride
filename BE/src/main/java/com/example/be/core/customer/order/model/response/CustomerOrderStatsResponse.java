package com.example.be.core.customer.order.model.response;

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
public class CustomerOrderStatsResponse {
    private long total;
    private long completed;
    private long cancelled;
    private long delivering;
}
