package com.example.be.core.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
    private int page;
    private int size;
    private String sortBy;
    private String sortDirection;

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size, null, null);
    }
}
