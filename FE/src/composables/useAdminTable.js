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
            
            // 1. Tìm kiếm danh sách dữ liệu (Duyệt qua các cấu trúc phổ biến)
            let dataList = [];
            if (Array.isArray(result)) {
                dataList = result;
            } else if (Array.isArray(result?.content)) {
                dataList = result.content;
            } else if (Array.isArray(result?.data)) {
                dataList = result.data;
            } else if (Array.isArray(result?.data?.content)) {
                dataList = result.data.content;
            } else if (Array.isArray(result?.data?.data)) {
                dataList = result.data.data;
            }
            items.value = dataList;

            // 2. Tìm kiếm tổng số bản ghi
            // Ưu tiên các trường ở root, sau đó tìm trong data, sau đó mới fallback về độ dài mảng
            const total = result?.totalElements || result?.totalCount || result?.total || 
                          result?.data?.totalElements || result?.data?.totalCount || result?.data?.total ||
                          result?.total_elements || result?.total_count || 
                          items.value.length;
            
            pagination.value.totalElements = total;
            
            // 3. Tính toán số trang (Có logic fallback nếu thiếu totalElements)
            const isPageFull = items.value.length >= pagination.value.size;
            let calculatedPages = Math.ceil(total / pagination.value.size) || 1;
            
            // Nếu không có total từ BE nhưng trang hiện tại đang đầy, giả định có ít nhất 1 trang nữa để hiện nút Next
            if (total === items.value.length && isPageFull) {
                calculatedPages = pagination.value.page + 1;
            }

            pagination.value.totalPages = result?.totalPages || result?.total_pages || result?.totalPage || calculatedPages;
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
