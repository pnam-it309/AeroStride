/**
 * Khôi phục các hóa đơn chờ từ server.
 * Không tự động tạo thêm hóa đơn rỗng.
 */
export const initializePendingOrders = async ({ fetchPendingOrders, setPendingOrders, preferredOrderId = null }) => {
    const payload = await fetchPendingOrders();
    const pendingOrders = Array.isArray(payload)
        ? payload
        : Array.isArray(payload?.content)
          ? payload.content
          : Array.isArray(payload?.data)
            ? payload.data
            : [];

    setPendingOrders(pendingOrders, { preferOrderId: preferredOrderId });

    return pendingOrders;
};
