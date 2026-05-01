package com.example.be.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PaginationUtils {
    public static Pageable toPageable(Object request) {
        // Dummy implementation, replace with your logic
        return PageRequest.of(0, 10, Sort.by("id").descending());
    }
    public static Pageable createPageable(int page, int size, String sortBy, String direction) {
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(dir, sortBy));
    }
}
