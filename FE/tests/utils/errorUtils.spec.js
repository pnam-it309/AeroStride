import { describe, it, expect, vi } from 'vitest';
import { getBackendErrorMessage, formatUserErrorMessage, logDevError } from '@/utils/errorUtils';

describe('errorUtils', () => {
    describe('formatUserErrorMessage', () => {
        it('chuyển đổi status code 401 sang tiếng Việt thân thiện', () => {
            expect(formatUserErrorMessage('401')).toBe('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
            expect(formatUserErrorMessage('Request failed with status code 401')).toBe(
                'Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.'
            );
        });

        it('chuyển đổi status code 403 sang tiếng Việt thân thiện', () => {
            expect(formatUserErrorMessage('403')).toBe('Bạn không có quyền thực hiện thao tác này.');
            expect(formatUserErrorMessage('Request failed with status code 403')).toBe('Bạn không có quyền thực hiện thao tác này.');
        });

        it('chuyển đổi status code 404 và 500 sang tiếng Việt thân thiện', () => {
            expect(formatUserErrorMessage('404')).toBe('Không tìm thấy dữ liệu yêu cầu.');
            expect(formatUserErrorMessage('Request failed with status code 500')).toBe('Hệ thống máy chủ đang gặp sự cố. Vui lòng thử lại sau.');
        });

        it('chuyển đổi lỗi mạng và timeout sang tiếng Việt', () => {
            expect(formatUserErrorMessage('Network Error')).toBe('Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng của bạn.');
            expect(formatUserErrorMessage('timeout of 10000ms exceeded')).toBe('Yêu cầu xử lý quá thời gian quy định. Vui lòng thử lại sau.');
        });

        it('chuyển đổi mã lỗi hệ thống chuẩn', () => {
            expect(formatUserErrorMessage('ERR_UNAUTHORIZED')).toBe('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
            expect(formatUserErrorMessage('ERR_RATE_LIMIT')).toBe(
                'Bạn đã thao tác quá nhanh hoặc gửi quá nhiều yêu cầu. Vui lòng đợi trong giây lát.'
            );
            expect(formatUserErrorMessage('ERR_WRONG_PASSWORD')).toBe('Mật khẩu không chính xác. Vui lòng kiểm tra lại.');
        });

        it('giữ nguyên thông báo tiếng Việt có nghĩa', () => {
            const vietnameseMsg = 'Số lượng trong kho không đủ';
            expect(formatUserErrorMessage(vietnameseMsg)).toBe(vietnameseMsg);
        });
    });

    describe('getBackendErrorMessage', () => {
        it('trích xuất thông điệp từ Axios Response 401', () => {
            const axiosError = {
                response: {
                    status: 401,
                    data: {
                        message: 'Unauthorized'
                    }
                }
            };
            const result = getBackendErrorMessage(axiosError);
            expect(result).toBe('Vui lòng đăng nhập để tiếp tục.');
        });

        it('trích xuất thông điệp tiếng Việt từ backend response', () => {
            const axiosError = {
                response: {
                    status: 400,
                    data: {
                        message: 'Tên sản phẩm đã tồn tại'
                    }
                }
            };
            const result = getBackendErrorMessage(axiosError);
            expect(result).toBe('Tên sản phẩm đã tồn tại');
        });

        it('xử lý lỗi khi không có response body nhưng có status code', () => {
            const axiosError = {
                response: {
                    status: 403
                }
            };
            const result = getBackendErrorMessage(axiosError);
            expect(result).toBe('Bạn không có quyền thực hiện thao tác này.');
        });

        it('xử lý fallback message khi không có thông tin lỗi', () => {
            expect(getBackendErrorMessage(null, 'Lỗi mặc định')).toBe('Lỗi mặc định');
        });
    });

    describe('logDevError', () => {
        it('ghi log đầy đủ vào console.error cho dev', () => {
            const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
            logDevError('Test Context', new Error('Debug error'));
            expect(spy).toHaveBeenCalled();
            spy.mockRestore();
        });
    });
});
