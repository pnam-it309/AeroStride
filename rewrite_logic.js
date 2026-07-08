const fs = require('fs');

const file = 'FE/src/views/modules/banhang/BanHang.vue';
const content = fs.readFileSync(file, 'utf8');

const scriptEnd = content.indexOf('</script>');

const voucherLogic = `

// UI Vue computed logic cho Layout mới
const bestVoucher = computed(() => {
    if (!selectedOrder.value?.idPhieuGiamGia) return null;
    const v = vouchers.value.find(v => String(v.id) === String(selectedOrder.value.idPhieuGiamGia));
    return v || null;
});

const removeBestVoucher = () => {
    if(selectedOrder.value) {
        selectedOrder.value.idPhieuGiamGia = null;
        selectedOrder.value.suggestedVoucherId = null;
        selectedOrder.value.tongTienSauGiam = selectedOrder.value.tongTien;
        // prevent auto apply from re-applying
        manualVoucherLocks.value[selectedOrder.value.id] = true;
    }
};

const suggestedVoucher = computed(() => {
    const total = originalTotalAmount.value || Number(selectedOrder.value?.tongTien || 0);
    const customerId = selectedOrder.value?.idKhachHang || null;
    
    // Find all vouchers that we DON'T qualify for because of donHangToiThieu
    const potentialVouchers = allActiveVouchers.value.filter(v => {
        const minimum = Number(v.donHangToiThieu || 0);
        if (total >= minimum) return false; // Already qualify

        // Check if customer eligible
        if (!isPublicVoucher(v) && isPersonalVoucher(v) && customerId) {
            const assignedCustomerIds = Array.isArray(v.listIdKhachHang) ? v.listIdKhachHang : [];
            if (!assignedCustomerIds.includes(customerId)) return false;
        }

        return true;
    });

    if (potentialVouchers.length === 0) return null;

    // Calculate if we met the minimum, what would the discount be?
    const suggestions = potentialVouchers.map(v => {
        const minimum = Number(v.donHangToiThieu || 0);
        const missingAmount = Math.max(0, minimum - total);
        
        // Calculate estimated discount on the minimum required amount
        const type = String(v.loaiPhieu || '').toUpperCase();
        let estimatedDiscount = 0;
        let giamGia = 0;
        if (type === 'PHAN_TRAM' || type === 'PERCENT') {
            const percent = Number(v.phanTramGiamGia || 0);
            const maxDiscount = Number(v.giamToiDa || Number.MAX_SAFE_INTEGER);
            estimatedDiscount = Math.min((minimum * percent) / 100, maxDiscount);
            giamGia = percent;
        } else {
            estimatedDiscount = Number(v.soTienGiam || 0);
            giamGia = 'Tiền';
        }

        return {
            ...v,
            missingAmount,
            estimatedDiscount,
            giamGia,
            ma: v.ma || v.maPhieu || v.maPhieuGiamGia
        };
    });

    suggestions.sort((a, b) => b.estimatedDiscount - a.estimatedDiscount);

    if (bestVoucher.value) {
        const currentDiscount = calculateVoucherDiscount(bestVoucher.value, total);
        const betterSuggestions = suggestions.filter(s => s.estimatedDiscount > currentDiscount);
        return betterSuggestions[0] || null;
    }

    return suggestions[0] || null;
});

const formatNumberWithDots = (num) => {
    if (!num) return '';
    return num.toString().replace(/\\B(?=(\\d{3})+(?!\\d))/g, ".");
};

const parseNumberFromDots = (str) => {
    if (!str) return null;
    const cleanStr = str.toString().replace(/\\./g, '');
    const num = Number(cleanStr);
    return isNaN(num) ? null : num;
};

// Hàm xử lý nhập tiền trả khách
const onReceivedAmountInput = (e) => {
    let rawValue = e.target.value.replace(/\\./g, '').replace(/[^0-9]/g, '');
    if (!rawValue) {
        checkoutData.value.receivedAmount = null;
        e.target.value = '';
        return;
    }
    const numValue = Number(rawValue);
    checkoutData.value.receivedAmount = numValue;
    e.target.value = formatNumberWithDots(numValue);
};

`;

if (scriptEnd !== -1) {
    const newContent = content.substring(0, scriptEnd) + voucherLogic + content.substring(scriptEnd);
    fs.writeFileSync(file, newContent, 'utf8');
    console.log('Successfully injected logic!');
}
