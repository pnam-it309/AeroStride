package com.example.be.utils;

import com.example.be.core.common.dto.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import java.util.function.Function;

/**
 * Ultimate DRY Search & Pagination Utility.
 * Handles the entire flow: DTO -> Pageable -> Repository Query -> Result.
 */
public final class SearchUtils {

    private static final int DEFAULT_SIZE = 10;
    private static final String DEFAULT_SORT = "ngayTao";

    private SearchUtils() {}

    /**
     * The ONLY method you need for search.
     * @param request The search request DTO extending PageRequest
     * @param repositoryCall Lambda to your repository method (e.g., p -> repo.find(keyword, p))
     */
    public static <T, R extends PageRequest> Page<T> execute(R request, Function<Pageable, Page<T>> repositoryCall) {
        return repositoryCall.apply(buildPageable(request));
    }

    /**
     * Public helper to build Pageable from DTO.
     */
    public static Pageable buildPageable(PageRequest req) {
        int page = (req.getPage() == null || req.getPage() < 0) ? 0 : req.getPage();
        int size = (req.getSize() == null || req.getSize() <= 0) ? DEFAULT_SIZE : req.getSize();
        
        String sortBy = StringUtils.hasText(req.getSortBy()) ? req.getSortBy() : DEFAULT_SORT;
        String sortDirStr = StringUtils.hasText(req.getSortDirection()) ? req.getSortDirection() : "desc";
        Sort.Direction dir = "asc".equalsIgnoreCase(sortDirStr) ? Sort.Direction.ASC : Sort.Direction.DESC;
        
        // Handle special case for legacy 'id' sort if necessary
        if ("id".equals(sortBy)) {
            return org.springframework.data.domain.PageRequest.of(page, size, Sort.by(dir, "id"));
        }
        
        return org.springframework.data.domain.PageRequest.of(page, size, Sort.by(dir, sortBy));
    }
}
