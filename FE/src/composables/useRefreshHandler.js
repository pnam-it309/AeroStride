import { ref } from 'vue';

/**
 * Composable để quản lý Refresh Handler
 * Giúp giảm bớt code lặp lại ở các view quản lý khi refresh dữ liệu
 */
export function useRefreshHandler() {
    const isRefreshing = ref(false);

    /**
     * Handle refresh với delay animation
     * @param {Function} resetFn - Hàm gọi để reset dữ liệu (thường là handleReset từ useAdminTable)
     * @param {Number} delayMs - Delay để tạo hiệu ứng loading (mặc định 800ms)
     */
    const handleRefresh = async (resetFn, delayMs = 800) => {
        if (isRefreshing.value) return;
        
        isRefreshing.value = true;
        try {
            resetFn();
            await new Promise(resolve => setTimeout(resolve, delayMs));
        } finally {
            isRefreshing.value = false;
        }
    };

    return {
        isRefreshing,
        handleRefresh
    };
}
