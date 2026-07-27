/**
 * Khôi phục các hóa đơn chờ từ server trước khi cân nhắc tạo hóa đơn mới.
 * Nếu API lỗi, exception được giữ nguyên và tuyệt đối không tạo thêm hóa đơn rỗng.
 */
export const initializePendingOrders = async ({
    fetchPendingOrders,
    setPendingOrders,
    createEmptyOrder,
    preferredOrderId = null
}) => {
    const payload = await fetchPendingOrders();
    const pendingOrders = Array.isArray(payload)
        ? payload
        : Array.isArray(payload?.content)
            ? payload.content
            : Array.isArray(payload?.data)
                ? payload.data
                : [];

    setPendingOrders(pendingOrders, { preferOrderId: preferredOrderId });

    if (pendingOrders.length === 0) {
        await createEmptyOrder();
    }

    return pendingOrders;
};
