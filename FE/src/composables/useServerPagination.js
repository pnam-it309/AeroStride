import { reactive, ref } from 'vue';

/**
 * Composable phân trang + tải dữ liệu server-side dùng chung.
 * - initialLoading: true khi lần đầu tải (chưa có data) → skeleton rows
 * - fetching: true khi pagination sau đó → dim + progress bar, không xóa bảng
 *
 * @param {(pageable: { page: number, size: number }) => Promise<any>} fetchPage
 * @param {{ pageSize?: number, onError?: (error: any) => void, onLoaded?: () => void }} [options]
 */
export function useServerPagination(fetchPage, { pageSize = 10, onError, onLoaded } = {}) {
    const items = ref([]);
    const loading = ref(true);        // backward-compat: initialLoading || fetching
    const initialLoading = ref(true); // skeleton phase
    const fetching = ref(false);      // dim phase
    const pagination = reactive({ page: 1, size: pageSize });
    const totalElements = ref(0);
    const totalPages = ref(1);

    const clearCache = () => {};

    let currentRequestId = 0;

    // Tải trang hiện tại
    const load = async (forceFresh = false) => {
        const targetPage = pagination.page;
        const targetSize = pagination.size;

        const requestId = ++currentRequestId;

        if (items.value.length > 0 && !forceFresh) {
            fetching.value = true;
        } else {
            initialLoading.value = true;
        }
        loading.value = true;

        try {
            const response = await fetchPage({ page: Math.max(targetPage - 1, 0), size: targetSize });
            if (requestId !== currentRequestId) return;
            const result = response?.data || response;
            const loadedItems = Array.isArray(result?.content) ? result.content : [];
            const total = Number(result?.totalElements ?? loadedItems.length);
            const pages = Math.max(Number(result?.totalPages ?? 1), 1);

            items.value = loadedItems;
            totalElements.value = total;
            totalPages.value = pages;

            if (pagination.page > pages) {
                pagination.page = pages;
                await load();
                return;
            }

            if (onLoaded) onLoaded();
        } catch (error) {
            if (requestId !== currentRequestId) return;
            items.value = [];
            totalElements.value = 0;
            totalPages.value = 1;
            if (onError) {
                onError(error);
            } else if (import.meta.env.DEV) {
                console.error('useServerPagination load error:', error);
            }
        } finally {
            if (requestId === currentRequestId) {
                initialLoading.value = false;
                fetching.value = false;
                loading.value = false;
            }
        }
    };

    // Về trang đầu rồi tải lại (dùng khi đổi bộ lọc/từ khóa).
    const reload = async () => {
        clearCache();
        pagination.page = 1;
        await load(true);
    };

    return { items, loading, initialLoading, fetching, pagination, totalElements, totalPages, load, reload, clearCache };
}
