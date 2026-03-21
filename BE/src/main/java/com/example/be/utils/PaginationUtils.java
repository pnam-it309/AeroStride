package com.example.be.utils;

import com.example.be.core.common.dto.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PaginationUtils {

    public static Pageable toPageable(PageRequest request) {
        int page = Math.max(0, request.getPage() - 1); // FE is 1-indexed, BE is 0-indexed
        int size = request.getSize() <= 0 ? 10 : request.getSize();
        
        if (request.getSortBy() != null && !request.getSortBy().isEmpty()) {
            Sort.Direction direction = "desc".equalsIgnoreCase(request.getSortDirection()) 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC;
            return org.springframework.data.domain.PageRequest.of(page, size, Sort.by(direction, request.getSortBy()));
        }
        
        return org.springframework.data.domain.PageRequest.of(page, size);
    }
}
