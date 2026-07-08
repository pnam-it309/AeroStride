const fs = require('fs');
let content = fs.readFileSync('FE/src/views/modules/banhang/BanHang.vue', 'utf8');

const start = content.indexOf('loading.value = true;');
const end = content.indexOf('await handleVnPayCallbackFromUrl();');

if (start > -1 && end > -1) {
    const replacement = `loading.value = true;
    try {
        fetchProvincesShip();
        await loadCurrentEmployeeDetails();
        await loadFilterOptions();
        const data = await dichVuDonHang.layDonHangCho();
        setOrders(data);

        loadingVouchers.value = true;
        try {
            const voucherResp = await dichVuPhieuGiamGia.layPhieuGiamGiaPhanTrang({
                page: 0,
                size: 1000,
                trangThai: 'DANG_HOAT_DONG',
                timelineStatus: 'DANG_HOAT_DONG'
            });
            allActiveVouchers.value = extractVoucherPageContent(voucherResp);
        } catch (error) {
            console.error('Lỗi khi tải danh sách phiếu giảm giá:', error);
        } finally {
            loadingVouchers.value = false;
        }

        if (orders.value.length === 0) {
            await createNewOrder({ force: true, silent: true });
        } else {
            activeOrderIndex.value = 0;
            if (selectedOrder.value?.id) {
                refreshBestVoucher();
            }
        }
        
        `;
    content = content.substring(0, start) + replacement + content.substring(end);
    fs.writeFileSync('FE/src/views/modules/banhang/BanHang.vue', content);
    console.log('Fixed onMounted');
} else {
    console.log('Could not find start or end markers');
}
