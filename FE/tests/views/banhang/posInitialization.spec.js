import { describe, expect, it, vi } from 'vitest';
import { initializePendingOrders } from '@/views/modules/banhang/posInitialization';

describe('initializePendingOrders', () => {
    it('khôi phục tất cả hóa đơn chờ và không tạo hóa đơn mới', async () => {
        const pendingOrders = [{ id: 'HD-1' }, { id: 'HD-2' }];
        const setPendingOrders = vi.fn();
        const createEmptyOrder = vi.fn();

        await initializePendingOrders({
            fetchPendingOrders: vi.fn().mockResolvedValue(pendingOrders),
            setPendingOrders,
            createEmptyOrder,
            preferredOrderId: 'HD-2'
        });

        expect(setPendingOrders).toHaveBeenCalledWith(pendingOrders, { preferOrderId: 'HD-2' });
        expect(createEmptyOrder).not.toHaveBeenCalled();
    });

    it('chỉ tạo hóa đơn mới khi server trả về danh sách rỗng', async () => {
        const createEmptyOrder = vi.fn().mockResolvedValue(undefined);

        await initializePendingOrders({
            fetchPendingOrders: vi.fn().mockResolvedValue([]),
            setPendingOrders: vi.fn(),
            createEmptyOrder
        });

        expect(createEmptyOrder).toHaveBeenCalledTimes(1);
    });

    it('không tạo hóa đơn mới khi API khôi phục bị lỗi', async () => {
        const createEmptyOrder = vi.fn();

        await expect(initializePendingOrders({
            fetchPendingOrders: vi.fn().mockRejectedValue(new Error('Network error')),
            setPendingOrders: vi.fn(),
            createEmptyOrder
        })).rejects.toThrow('Network error');

        expect(createEmptyOrder).not.toHaveBeenCalled();
    });
});
