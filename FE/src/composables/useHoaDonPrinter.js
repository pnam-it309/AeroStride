import { dichVuHoaDon } from '@/services/admin/dichVuHoaDon';
import { useNotifications } from '@/services/notificationService';

// Helper in hoa don dung chung cho module Hoa don va man Ban hang.
// Nguyen ly: backend tra ve san HTML A4, FE chi mo cua so va day HTML vao de hai man co cung mau in.
// Mac dinh khong tu goi window.print(), trang HTML hoa don se co nut in rieng giong man Quan ly hoa don.
export function useHoaDonPrinter() {
    const { addNotification } = useNotifications();

    const printHoaDonById = async (orderId, options = {}) => {
        const {
            notifyPreparing = true,
            printWindow: providedPrintWindow = null
        } = options;

        if (!orderId) {
            addNotification({
                title: 'Không thể in hóa đơn',
                subtitle: 'Không tìm thấy mã hóa đơn để tạo bản in.',
                color: 'warning'
            });
            return false;
        }

        let printWindow = providedPrintWindow;

        try {
            if (notifyPreparing) {
                addNotification({ title: 'Đang chuẩn bị', subtitle: 'Đang tạo bản in hóa đơn...', color: 'info' });
            }

            // Mo popup ngay trong handler click de trinh duyet khong chan cua so in.
            if (!printWindow) {
                printWindow = window.open('', '_blank', 'width=900,height=1000');
            }

            if (!printWindow) {
                addNotification({
                    title: 'Không thể mở bản in',
                    subtitle: 'Trình duyệt đã chặn cửa sổ bật lên. Vui lòng cho phép popup để in hóa đơn.',
                    color: 'warning'
                });
                return false;
            }

            printWindow.document.open();
            printWindow.document.write('<!doctype html><html><head><title>Đang tạo hóa đơn...</title></head><body style="font-family:Arial,sans-serif;padding:24px">Đang tạo bản in hóa đơn...</body></html>');
            printWindow.document.close();

            const html = await dichVuHoaDon.inHoaDon(orderId);
            printWindow.document.open();
            printWindow.document.write(html);
            printWindow.document.close();

            return true;
        } catch (error) {
            console.error('Print invoice error:', error);
            if (providedPrintWindow && !providedPrintWindow.closed) {
                providedPrintWindow.close();
            }
            addNotification({
                title: 'Lỗi',
                subtitle: 'Không thể tải bản in hóa đơn từ máy chủ. Vui lòng kiểm tra lại kết nối.',
                color: 'error'
            });
            return false;
        }
    };

    return { printHoaDonById };
}
