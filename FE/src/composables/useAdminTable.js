import { ref, watch } from 'vue';

/**
 * Composable để quản lý trạng thái bảng Admin (Pagination, Filters, Loading)
 * - initialLoading: true khi chưa có data (lần đầu) → hiện skeleton rows
 * - fetching: true khi đã có data và đang tải lại (pagination/filter) → dim table + progress bar
 *
 * @param {Function} fetchFn - Hàm lấy dữ liệu từ service
 * @param {Object} initialFilters - Giá trị lọc mặc định
 */
export function useAdminTable(fetchFn, initialFilters = {}) {
    const items = ref([]);
    const loading = ref(true);        // backward-compat alias = initialLoading || fetching
    const initialLoading = ref(true); // chưa có data lần nào → skeleton
    const fetching = ref(false);      // đang tải lại (pagination / filter) → dim
    const pagination = ref({
        page: 1,
        size: 10,
        totalElements: 0,
        totalPages: 0
    });

    const filters = ref({
        search: '',
        trangThai: null,
        ...initialFilters
    });

    const clearCache = () => {};

    let currentRequestId = 0;
    let searchDebounceTimer = null;

    const loadData = async (forceFresh = false) => {
        const targetPage = pagination.value.page;
        const targetSize = pagination.value.size;
        const currentFilters = { ...filters.value };

        const requestId = ++currentRequestId;

        // Nếu đã có data: chỉ fetching (dim), không xóa trắng bảng
        if (items.value.length > 0 && !forceFresh) {
            fetching.value = true;
        } else {
            initialLoading.value = true;
        }
        loading.value = true;

        try {
            const params = {
                page: Math.max(targetPage - 1, 0),
                size: targetSize,
                ...currentFilters
            };

            const response = await fetchFn(params);
            if (requestId !== currentRequestId) return;

            const result = response;

            // Thuật toán quét sâu (Deep Scan) tìm dữ liệu và phân trang
            const findDataInObject = (obj, depth = 0) => {
                if (!obj || typeof obj !== 'object' || depth > 3) return { items: [], total: null };

                let itms = null;
                let tot = null;

                const possibleArrays = ['content', 'data', 'items', 'nhanViens', 'khachHangs', 'sanPhams', 'list'];
                for (const key of possibleArrays) {
                    if (Array.isArray(obj[key])) {
                        itms = obj[key];
                        break;
                    }
                }

                const possibleTotals = ['totalElements', 'totalCount', 'total', 'total_records', 'count'];
                for (const key of possibleTotals) {
                    if (typeof obj[key] === 'number') {
                        tot = obj[key];
                        break;
                    }
                }

                if (Array.isArray(obj)) itms = obj;

                if (!itms || tot === null) {
                    for (const key in obj) {
                        if (obj[key] && typeof obj[key] === 'object' && key !== 'items') {
                            const deep = findDataInObject(obj[key], depth + 1);
                            if (!itms && deep.items && deep.items.length > 0) itms = deep.items;
                            if (tot === null && deep.total !== null) tot = deep.total;
                            if (itms && tot !== null) break;
                        }
                    }
                }

                return { items: itms, total: tot };
            };

            const extracted = findDataInObject(result);
            const loadedItems = extracted.items || [];
            const total = extracted.total !== null ? extracted.total : loadedItems.length;

            let finalTotalPages = Math.ceil(total / targetSize) || 1;
            const beTotalPages =
                result?.totalPages ||
                result?.total_pages ||
                result?.data?.totalPages ||
                result?.data?.total_pages ||
                result?.pagination?.totalPages ||
                result?.data?.data?.totalPages;

            if (beTotalPages && extracted.total === null) {
                finalTotalPages = beTotalPages;
            }

            items.value = loadedItems;
            pagination.value.totalElements = total;
            pagination.value.totalPages = finalTotalPages;

            if (pagination.value.page > finalTotalPages && finalTotalPages > 0) {
                pagination.value.page = finalTotalPages;
            }
        } catch (error) {
            if (requestId !== currentRequestId) return;
            if (import.meta.env.DEV) {
                console.error('Error loading table data:', error);
            }
            items.value = [];
        } finally {
            if (requestId === currentRequestId) {
                initialLoading.value = false;
                fetching.value = false;
                loading.value = false;
            }
        }
    };

    watch(
        () => [pagination.value.page, pagination.value.size],
        () => {
            if (searchDebounceTimer) clearTimeout(searchDebounceTimer);
            loadData();
        }
    );

    const handleFilter = (immediate = false) => {
        pagination.value.page = 1;
        if (searchDebounceTimer) clearTimeout(searchDebounceTimer);

        if (immediate === true) {
            loadData();
            return;
        }

        // 250ms debounce – phản hồi nhanh nhưng không re-render quá nhiều
        searchDebounceTimer = setTimeout(() => {
            loadData();
        }, 250);
    };

    const handleReset = () => {
        if (searchDebounceTimer) clearTimeout(searchDebounceTimer);
        clearCache();
        filters.value = {
            search: '',
            trangThai: null,
            ...initialFilters
        };
        pagination.value.page = 1;
        loadData(true);
    };

    return {
        items,
        loading,
        initialLoading,
        fetching,
        pagination,
        filters,
        loadData,
        handleFilter,
        handleReset,
        clearCache
    };
}
