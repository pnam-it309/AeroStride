import { reactive, ref } from 'vue';

/**
 * Composable phân trang + tải dữ liệu server-side dùng chung.
 * Tích hợp bộ nhớ đệm trang (Client-side Page Cache) giúp chuyển trang 0ms tức thì.
 *
 * @param {(pageable: { page: number, size: number }) => Promise<any>} fetchPage
 * @param {{ pageSize?: number, onError?: (error: any) => void, onLoaded?: () => void }} [options]
 */
export function useServerPagination(fetchPage, { pageSize = 10, onError, onLoaded } = {}) {
    const items = ref([]);
    const loading = ref(true);
    const pagination = reactive({ page: 1, size: pageSize });
    const totalElements = ref(0);
    const totalPages = ref(1);

    const pageCache = new Map();

    const clearCache = () => {
        pageCache.clear();
    };

    let currentRequestId = 0;

    // Tải trang hiện tại với kiểm tra cache 0ms
    const load = async (forceFresh = false) => {
        const targetPage = pagination.page;
        const targetSize = pagination.size;
        const cacheKey = `p${targetPage}_s${targetSize}`;

        if (!forceFresh && pageCache.has(cacheKey)) {
            const cached = pageCache.get(cacheKey);
            items.value = cached.items || [];
            totalElements.value = cached.totalElements || 0;
            totalPages.value = cached.totalPages || 1;
            loading.value = false;
            if (onLoaded) onLoaded();
            return;
        }

        const requestId = ++currentRequestId;
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

            pageCache.set(cacheKey, {
                items: loadedItems,
                totalElements: total,
                totalPages: pages
            });

            if (pageCache.size > 50) {
                const firstKey = pageCache.keys().next().value;
                pageCache.delete(firstKey);
            }

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

    return { items, loading, pagination, totalElements, totalPages, load, reload, clearCache };
}
