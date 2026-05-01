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
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 10;
    private String sortBy;
    private String sortDirection;

    public void setPageNo(Integer pageNo) {
        this.page = pageNo;
    }

    public void setPageSize(Integer pageSize) {
        this.size = pageSize;
    }

    public static PageRequest of(Integer page, Integer size) {
        return new PageRequest(page, size, null, null);
    }
}
