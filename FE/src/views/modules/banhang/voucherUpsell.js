const toMoney = (value) => {
    const number = Number(value || 0);
    return Number.isFinite(number) ? Math.max(0, number) : 0;
};

const getMinimumOrderValue = (voucher) => toMoney(voucher?.donHangToiThieu ?? voucher?.min_order_value ?? voucher?.minOrderValue);

const getVoucherDiscount = (voucher, orderValue) => {
    const base = toMoney(orderValue);
    const type = String(voucher?.loaiPhieu || voucher?.type || '').toUpperCase();

    if (type === 'PHAN_TRAM' || type === 'PERCENT' || type === '1') {
        const percentage = toMoney(voucher?.phanTramGiamGia ?? voucher?.discountPercent);
        const maximum = toMoney(voucher?.giamToiDa ?? voucher?.maxDiscount);
        const percentageDiscount = (base * percentage) / 100;
        return Math.min(base, maximum > 0 ? Math.min(percentageDiscount, maximum) : percentageDiscount);
    }

    return Math.min(base, toMoney(voucher?.soTienGiam ?? voucher?.discountAmount));
};

const isCurrentlyAvailable = (voucher, now) => {
    if (!voucher || voucher.disabledByBusinessRule) return false;
    // Quy ước từ màn quản lý phiếu: -1 là vô hạn, 0 mới là hết lượt.
    if (voucher.soLuong != null && Number(voucher.soLuong) === 0) return false;
    if (voucher.ngayBatDau != null && now < Number(voucher.ngayBatDau)) return false;
    if (voucher.ngayKetThuc != null && now > Number(voucher.ngayKetThuc)) return false;
    return true;
};

const isSameVoucher = (left, right) => {
    if (!left || !right) return false;
    const leftKey = left.id ?? left.ma ?? left.maPhieu;
    const rightKey = right.id ?? right.ma ?? right.maPhieu;
    return leftKey != null && rightKey != null && String(leftKey) === String(rightKey);
};

export const findBestVoucherUpsell = (vouchers, currentOrderValue, now = Date.now(), currentVoucher = null) => {
    const currentTotal = toMoney(currentOrderValue);

    return (
        (Array.isArray(vouchers) ? vouchers : [])
            .filter((voucher) => isCurrentlyAvailable(voucher, now))
            .filter((voucher) => !isSameVoucher(voucher, currentVoucher))
            .map((voucher) => {
                const minimumOrderValue = getMinimumOrderValue(voucher);
                const discountAmount = getVoucherDiscount(voucher, minimumOrderValue);
                const currentVoucherDiscount = getVoucherDiscount(currentVoucher, minimumOrderValue);
                return {
                    voucher,
                    minimumOrderValue,
                    remainingAmount: Math.max(0, minimumOrderValue - currentTotal),
                    discountAmount,
                    extraDiscountAmount: Math.max(0, discountAmount - currentVoucherDiscount)
                };
            })
            .filter((candidate) => candidate.remainingAmount > 0 && candidate.discountAmount > 0 && candidate.extraDiscountAmount > 0)
            .sort(
                (left, right) =>
                    right.extraDiscountAmount - left.extraDiscountAmount ||
                    right.discountAmount - left.discountAmount ||
                    left.remainingAmount - right.remainingAmount ||
                    String(left.voucher?.ma || left.voucher?.id || '').localeCompare(String(right.voucher?.ma || right.voucher?.id || ''))
            )[0] || null
    );
};
