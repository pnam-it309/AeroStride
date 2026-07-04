import { reactive, ref } from 'vue';

/**
 * Composable phân trang + tải dữ liệu server-side dùng chung.
 *
 * Khác với useAdminTable (spread thẳng filters vào params), composable này để CALLER tự dựng
 * params lọc bên trong `fetchPage` — phù hợp các màn cần map param tùy biến
 * (vd. khoangGia -> minGia/maxGia, sanPhamId, ...). Composable chỉ quản lý page/size/tổng trang.
 *
 * @param {(pageable: { page: number, size: number }) => Promise<any>} fetchPage
 *        Hàm gọi API, nhận { page (0-based), size } và trả PageResponse
 *        ({ content, totalElements, totalPages }) hoặc envelope { data: { ... } }.
 * @param {{ pageSize?: number, onError?: (error: any) => void, onLoaded?: () => void }} [options]
 *        onLoaded: chạy sau mỗi lần tải trang thành công (vd. đồng bộ lựa chọn, cập nhật meta header).
 */
export function useServerPagination(fetchPage, { pageSize = 5, onError, onLoaded } = {}) {
    const items = ref([]);
    const loading = ref(false);
    const pagination = reactive({ page: 1, size: pageSize });
    const totalElements = ref(0);
    const totalPages = ref(1);

    // Tải trang hiện tại. Nếu trang vượt quá tổng số trang (vd. sau khi xóa dòng cuối) thì lùi về trang cuối và tải lại.
    const load = async () => {
        if (loading.value) return;
        loading.value = true;
        try {
            const response = await fetchPage({ page: Math.max(pagination.page - 1, 0), size: pagination.size });
            const result = response?.data || response;
            items.value = Array.isArray(result?.content) ? result.content : [];
            totalElements.value = Number(result?.totalElements ?? items.value.length);
            totalPages.value = Math.max(Number(result?.totalPages ?? 1), 1);

            if (pagination.page > totalPages.value) {
                pagination.page = totalPages.value;
                loading.value = false;
                await load();
                return;
            }

            if (onLoaded) onLoaded();
        } catch (error) {
            items.value = [];
            totalElements.value = 0;
            totalPages.value = 1;
            if (onError) {
                onError(error);
            } else if (import.meta.env.DEV) {
                console.error('useServerPagination load error:', error);
            }
        } finally {
            loading.value = false;
        }
    };

    // Về trang đầu rồi tải lại (dùng khi đổi bộ lọc/từ khóa).
    const reload = async () => {
        pagination.page = 1;
        await load();
    };

    return { items, loading, pagination, totalElements, totalPages, load, reload };
}
