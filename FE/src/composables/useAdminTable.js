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
            const result = response?.data || response;
            items.value = Array.isArray(result?.content) ? result.content : (Array.isArray(result) ? result : []);
            
            pagination.value.totalElements = result?.totalElements || items.value.length;
            pagination.value.totalPages = result?.totalPages || Math.ceil(pagination.value.totalElements / pagination.value.size) || 1;
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
