import { ref, reactive, watch } from 'vue';

/**
 * Composable để quản lý trạng thái bảng Admin (Pagination, Filters, Loading)
 * Giúp giảm bớt code lặp lại trong các file .vue
 * 
 * @param {Function} fetchFn - Hàm lấy dữ liệu từ service (ví dụ: dichVuNhanVien.getAll)
 * @param {Object} initialFilters - Giá trị lọc mặc định
 */
export function useAdminTable(fetchFn, initialFilters = {}) {
    const items = ref([]);
    const loading = ref(false);
    const pagination = ref({
        page: 1,
        size: 5,
        totalElements: 0,
        totalPages: 0
    });

    const filters = ref({
        search: '', // Sử dụng 'search' làm mặc định để tương thích tốt nhất
        trangThai: null,
        ...initialFilters
    });

    const loadData = async () => {
        if (loading.value) return;

        loading.value = true;
        try {
            // Chuẩn bị params sạch sẽ để gửi đi
            const params = {
                page: pagination.value.page - 1, // 1-indexed (UI) -> 0-indexed (API)
                size: pagination.value.size,
                ...filters.value
            };

            const response = await fetchFn(params);

            // Map dữ liệu
            const result = response;

            // 1. Thuật toán quét sâu (Deep Scan) để tìm dữ liệu và phân trang
            const findDataInObject = (obj, depth = 0) => {
                if (!obj || typeof obj !== 'object' || depth > 3) return { items: [], total: null };
                
                let items = null;
                let total = null;

                const possibleArrays = ['content', 'data', 'items', 'nhanViens', 'khachHangs', 'sanPhams', 'list'];
                for (const key of possibleArrays) {
                    if (Array.isArray(obj[key])) { items = obj[key]; break; }
                }

                const possibleTotals = ['totalElements', 'totalCount', 'total', 'total_elements', 'total_count', 'total_records', 'count'];
                for (const key of possibleTotals) {
                    if (typeof obj[key] === 'number') { total = obj[key]; break; }
                }

                if (Array.isArray(obj)) items = obj;

                // Nếu chưa đủ bộ, quét sâu vào các thuộc tính con
                if (!items || total === null) {
                    for (const key in obj) {
                        if (obj[key] && typeof obj[key] === 'object' && key !== 'items') {
                            const deep = findDataInObject(obj[key], depth + 1);
                            if (!items && deep.items && deep.items.length > 0) items = deep.items;
                            if (total === null && deep.total !== null) total = deep.total;
                            if (items && total !== null) break;
                        }
                    }
                }

                return { items, total };
            };

            const extracted = findDataInObject(result);
            items.value = extracted.items || [];
            
            const total = (extracted.total !== null) ? extracted.total : items.value.length;
            pagination.value.totalElements = total;

            // 2. Tính toán số trang (Đồng bộ tuyệt đối)
            let finalTotalPages = Math.ceil(total / pagination.value.size) || 1;
            
            const beTotalPages = result?.totalPages || result?.total_pages || 
                               result?.data?.totalPages || result?.data?.total_pages || 
                               result?.pagination?.totalPages || result?.data?.data?.totalPages;
            
            if (beTotalPages && extracted.total === null) {
                finalTotalPages = beTotalPages;
            }

            pagination.value.totalPages = finalTotalPages;

            // Logic an toàn: Nếu đang ở một trang vượt quá tổng số trang (do xóa bớt dữ liệu hoặc lỗi phân trang)
            // thì tự động quay về trang cuối cùng hợp lệ
            if (pagination.value.page > finalTotalPages && finalTotalPages > 0) {
                pagination.value.page = finalTotalPages;
                // Không cần gọi loadData() ở đây vì watch(pagination.page) sẽ tự động kích hoạt lại
            }
        } catch (error) {
            if (import.meta.env.DEV) {
                console.error('Error loading table data:', error);
            }
            items.value = [];
        } finally {
            loading.value = false;
        }
    };

    watch(() => [pagination.value.page, pagination.value.size], () => {
        loadData();
    });

    const handleFilter = () => {
        pagination.value.page = 1;
        loadData();
    };

    const handleReset = () => {
        filters.value = {
            search: '',
            trangThai: null,
            ...initialFilters
        };
        handleFilter();
    };

    return {
        items,
        loading,
        pagination,
        filters,
        loadData,
        handleFilter,
        handleReset
    };
}
