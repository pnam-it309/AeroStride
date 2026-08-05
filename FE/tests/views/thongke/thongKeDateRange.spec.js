import { describe, expect, it } from 'vitest';
import { formatLocalDate, getCurrentYearToDateRange } from '@/views/modules/thongKeDateRange';

describe('thongKeDateRange', () => {
    it('tạo khoảng lũy kế từ đầu năm đến ngày hiện tại', () => {
        const now = new Date(2026, 7, 5, 22, 43, 0);

        expect(getCurrentYearToDateRange(now)).toEqual({
            year: 2026,
            startDate: '2026-01-01',
            endDate: '2026-08-05'
        });
    });

    it('định dạng theo ngày local thay vì UTC', () => {
        const localDate = new Date(2026, 0, 1, 0, 30, 0);

        expect(formatLocalDate(localDate)).toBe('2026-01-01');
    });
});
