import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import OrderSummaryPanel from '@/views/modules/banhang/components/OrderSummaryPanel.vue';
import { findBestVoucherUpsell } from '@/views/modules/banhang/voucherUpsell.js';

const NOW = 1_800_000_000_000;

describe('findBestVoucherUpsell', () => {
    it('selects the unmet voucher with the highest actual discount', () => {
        const result = findBestVoucherUpsell([
            {
                id: 'fixed',
                ma: 'GIAM100K',
                loaiPhieu: 'SO_TIEN',
                soTienGiam: 100_000,
                donHangToiThieu: 1_000_000,
                soLuong: 10
            },
            {
                id: 'percent',
                ma: 'GIAM20',
                loaiPhieu: 'PHAN_TRAM',
                phanTramGiamGia: 20,
                giamToiDa: 300_000,
                donHangToiThieu: 1_200_000,
                soLuong: 10
            }
        ], 800_000, NOW);

        expect(result.voucher.id).toBe('percent');
        expect(result.remainingAmount).toBe(400_000);
        expect(result.discountAmount).toBe(240_000);
    });

    it('recalculates the missing amount directly from the current cart total', () => {
        const vouchers = [{
            id: 'voucher',
            loaiPhieu: 'SO_TIEN',
            soTienGiam: 150_000,
            donHangToiThieu: 1_000_000,
            soLuong: 1
        }];

        expect(findBestVoucherUpsell(vouchers, 700_000, NOW).remainingAmount).toBe(300_000);
        expect(findBestVoucherUpsell(vouchers, 900_000, NOW).remainingAmount).toBe(100_000);
        expect(findBestVoucherUpsell(vouchers, 1_000_000, NOW)).toBeNull();
    });

    it('ignores expired and exhausted vouchers', () => {
        const result = findBestVoucherUpsell([
            {
                id: 'expired',
                loaiPhieu: 'SO_TIEN',
                soTienGiam: 500_000,
                donHangToiThieu: 1_000_000,
                ngayKetThuc: NOW - 1,
                soLuong: 10
            },
            {
                id: 'exhausted',
                loaiPhieu: 'SO_TIEN',
                soTienGiam: 400_000,
                donHangToiThieu: 1_000_000,
                soLuong: 0
            }
        ], 500_000, NOW);

        expect(result).toBeNull();
    });

    it('keeps public unlimited vouchers marked with quantity -1', () => {
        const result = findBestVoucherUpsell([
            {
                id: 'unlimited',
                ma: 'TEST1',
                hinhThuc: 'CONG_KHAI',
                soLuong: -1,
                loaiPhieu: 'TIEN_MAT',
                soTienGiam: 100_000,
                donHangToiThieu: 1_000_000,
                ngayBatDau: NOW - 1,
                ngayKetThuc: NOW + 1
            }
        ], 990_000, NOW);

        expect(result?.voucher.id).toBe('unlimited');
        expect(result?.remainingAmount).toBe(10_000);
    });

    it('suggests only a voucher that is better than the currently applied voucher', () => {
        const currentVoucher = {
            id: 'current',
            loaiPhieu: 'TIEN_MAT',
            soTienGiam: 100_000,
            donHangToiThieu: 100_000,
            soLuong: -1
        };
        const result = findBestVoucherUpsell([
            currentVoucher,
            {
                id: 'better',
                ma: 'TEST2',
                loaiPhieu: 'TIEN_MAT',
                soTienGiam: 1_000_000,
                donHangToiThieu: 1_000_000,
                soLuong: -1
            },
            {
                id: 'not-better',
                ma: 'GIAM50K',
                loaiPhieu: 'TIEN_MAT',
                soTienGiam: 50_000,
                donHangToiThieu: 800_000,
                soLuong: -1
            }
        ], 630_000, NOW, currentVoucher);

        expect(result?.voucher.id).toBe('better');
        expect(result?.remainingAmount).toBe(370_000);
        expect(result?.discountAmount).toBe(1_000_000);
        expect(result?.extraDiscountAmount).toBe(900_000);
    });
});

describe('OrderSummaryPanel voucher rendering', () => {
    const mountPanel = (props) => mount(OrderSummaryPanel, {
        props: {
            totalRawAmount: 500_000,
            voucherBaseAmount: 500_000,
            ...props
        },
        global: {
            stubs: {
                VCard: { template: '<div><slot /></div>' },
                VIcon: { template: '<i><slot /></i>' },
                VSwitch: true,
                Transition: false
            }
        }
    });

    it('shows only the voucher ticket when a voucher is applied', () => {
        const wrapper = mountPanel({
            selectedVoucherId: 'voucher-1',
            appliedVoucher: {
                id: 'voucher-1',
                ma: 'GIAM100K',
                ten: 'Giảm 100K',
                loaiPhieu: 'SO_TIEN',
                soTienGiam: 100_000,
                donHangToiThieu: 400_000
            },
            totalDiscountAmount: 100_000,
            voucherSuggestionText: 'Chưa có phiếu giảm giá phù hợp.'
        });

        expect(wrapper.text()).toContain('GIAM100K');
        expect(wrapper.text()).not.toContain('Chưa có phiếu giảm giá');
    });

    it('shows the empty message and hides the ticket when no voucher is applied', () => {
        const wrapper = mountPanel({
            selectedVoucherId: null,
            appliedVoucher: null,
            totalDiscountAmount: 0,
            voucherSuggestionText: 'Chưa có phiếu giảm giá phù hợp.'
        });

        expect(wrapper.text()).toContain('Chưa có phiếu giảm giá phù hợp.');
        expect(wrapper.find('.voucher-ticket').exists()).toBe(false);
    });

    it('shows a buy-more suggestion below an applied voucher when a better voucher exists', () => {
        const wrapper = mountPanel({
            voucherBaseAmount: 630_000,
            selectedVoucherId: 'current',
            appliedVoucher: {
                id: 'current',
                ma: 'TEST1',
                loaiPhieu: 'TIEN_MAT',
                soTienGiam: 100_000,
                donHangToiThieu: 100_000,
                soLuong: -1
            },
            vouchers: [
                {
                    id: 'current',
                    ma: 'TEST1',
                    loaiPhieu: 'TIEN_MAT',
                    soTienGiam: 100_000,
                    donHangToiThieu: 100_000,
                    soLuong: -1
                },
                {
                    id: 'better',
                    ma: 'TEST2',
                    loaiPhieu: 'TIEN_MAT',
                    soTienGiam: 1_000_000,
                    donHangToiThieu: 1_000_000,
                    soLuong: -1
                }
            ],
            productDiscountAmount: 70_000,
            voucherDiscountAmount: 100_000,
            totalDiscountAmount: 170_000
        });

        expect(wrapper.text()).toContain('Mua thêm');
        expect(wrapper.text()).toContain('370.000');
        expect(wrapper.text()).toContain('TEST2');
        expect(wrapper.text()).toContain('lợi hơn 900.000');
        expect(wrapper.find('.voucher-ticket').text()).toContain('100.000');
        expect(wrapper.find('.voucher-ticket').text()).not.toContain('170.000');
    });

    it('uses the order response suggestion when the voucher list contains only eligible vouchers', () => {
        const wrapper = mountPanel({
            voucherBaseAmount: 990_000,
            selectedVoucherId: 'current',
            appliedVoucher: {
                id: 'current',
                ma: 'TEST1',
                loaiPhieu: 'TIEN_MAT',
                soTienGiam: 100_000,
                donHangToiThieu: 100_000,
                soLuong: -1
            },
            vouchers: [],
            betterVoucherSuggestionText: 'Mua thêm 10.000 đ để nhận phiếu tốt hơn: TEST2 (-1.000.000 đ)'
        });

        expect(wrapper.text()).toContain('Mua thêm 10.000 đ');
        expect(wrapper.text()).toContain('TEST2');
    });
});
