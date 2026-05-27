<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';
import { dichVuVnPay } from './dichVuVnPay';
import { dichVuPhieuGiamGia } from '@/services/admin/dichVuPhieuGiamGia';
import { useNotifications } from '@/services/notificationService';
import { MESSAGES } from '@/constants/messages';

// Import Components
import OrderTabs from './components/OrderTabs.vue';
import ProductPicker from './components/ProductPicker.vue';
import CartTable from './components/CartTable.vue';
import CustomerSelector from './components/CustomerSelector.vue';
import CheckoutPanel from './components/CheckoutPanel.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import InvoiceReceiptDialog from './components/InvoiceReceiptDialog.vue';

const { addNotification } = useNotifications();
const MAX_WAITING_ORDERS = 5;
const VNPAY_PENDING_KEY = 'aerostride_pos_vnpay_pending';
const BYPASS_PAYMENT_RECORD_INSERT = false; // Đã fix backend: gd.setId() bị xóa, không còn lỗi merge().

// State
const loading = ref(false);
const orders = ref([]);
const activeOrderIndex = ref(0);
const vouchers = ref([]);
const isProcessing = ref(false);
const isAutoApplyingVoucher = ref(false);

// State hiển thị hóa đơn sau thanh toán
const receiptDialog = ref({
    show: false,
    order: null,
    paymentMethod: 'CASH',
    receivedAmount: 0,
    note: '',
    paidAt: null
});

const checkoutData = ref({
    paymentMethod: 'CASH',
    receivedAmount: 0,
    note: ''
});

// Confirmation Logic
const confirmDialog = ref({
    show: false,
    title: '',
    message: '',
    color: 'primary',
    action: null,
    loading: false
});

const selectedOrder = computed(() => orders.value[activeOrderIndex.value] || null);
const selectedOrderItemCount = computed(() =>
    (selectedOrder.value?.listsHoaDonChiTiet || []).reduce((sum, item) => sum + (Number(item.soLuong) || 0), 0)
);

const normalizeOrderList = (payload) => {
    if (Array.isArray(payload)) return payload;
    if (Array.isArray(payload?.content)) return payload.content;
    if (Array.isArray(payload?.data)) return payload.data;
    return [];
};

const clampActiveOrderIndex = () => {
    if (!orders.value.length) {
        activeOrderIndex.value = 0;
        return;
    }

    if (activeOrderIndex.value < 0 || activeOrderIndex.value >= orders.value.length) {
        activeOrderIndex.value = Math.max(0, orders.value.length - 1);
    }
};

const setOrders = (payload, { preferOrderId = null } = {}) => {
    orders.value = normalizeOrderList(payload);

    if (preferOrderId) {
        const preferredIndex = orders.value.findIndex((order) => order.id === preferOrderId);
        if (preferredIndex !== -1) {
            activeOrderIndex.value = preferredIndex;
        }
    }

    clampActiveOrderIndex();
};

// Lifecycle
onMounted(async () => {
    loading.value = true;
    try {
        const data = await dichVuDonHang.layDonHangCho();
        setOrders(data);
        await handleVnPayCallbackFromUrl();
        if (orders.value.length === 0) await createNewOrder();
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: getErrorMessage(error, MESSAGES.ERROR.CONNECT_SERVER), color: 'error' });
    } finally {
        loading.value = false;
    }
});

onUnmounted(() => {
    clearVnPayPolling();
});

// Watchers
watch(() => selectedOrder.value?.id, (id) => {
    if (id) refreshBestVoucher();
});

watch(
    () => [selectedOrder.value?.id, selectedOrder.value?.idKhachHang, selectedOrder.value?.tongTien],
    () => {
        if (selectedOrder.value?.id) refreshBestVoucher();
    }
);

// Logic: Hóa đơn
const createNewOrder = async ({ silent = false, force = false } = {}) => {
    if (isProcessing.value && !force) return; // Prevent double clicks
    if (orders.value.length >= MAX_WAITING_ORDERS) {
        if (!silent) {
            addNotification({ title: 'Giới hạn hóa đơn chờ', subtitle: `Backend hiện chỉ cho mở tối đa ${MAX_WAITING_ORDERS} hóa đơn chờ cùng lúc.`, color: 'warning' });
        }
        return;
    }
    const previousProcessing = isProcessing.value;
    isProcessing.value = true;
    try {
        const newOrder = await dichVuDonHang.taoDonHang();
        orders.value.push(newOrder);
        activeOrderIndex.value = orders.value.length - 1;
    } catch (e) {
        if (!silent) {
            addNotification({ title: 'Không thể tạo hóa đơn', subtitle: getErrorMessage(e, MESSAGES.ERROR.SAVE_DATA), color: 'error' });
        }
    } finally {
        isProcessing.value = previousProcessing;
    }
};

const closeOrder = (orderId, index) => {
    confirmDialog.value = {
        show: true,
        title: 'Xác nhận xóa hóa đơn',
        message: 'Bạn có chắc chắn muốn xóa hóa đơn chờ này không? Dữ liệu giỏ hàng sẽ bị mất.',
        color: 'error',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                await dichVuDonHang.xoaDonHang(orderId);
                orders.value.splice(index, 1);
                clampActiveOrderIndex();
                if (orders.value.length === 0) await createNewOrder();
                confirmDialog.value.show = false;
            } catch (e) {
                addNotification({ title: 'Lỗi', subtitle: MESSAGES.ERROR.DELETE_DATA, color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

// Logic: Sản phẩm
const onAddProduct = async (product) => {
    if (!selectedOrder.value) {
        addNotification({ title: 'Cảnh báo', subtitle: 'Vui lòng chọn hoặc tạo hóa đơn trước.', color: 'warning' });
        return;
    }
    if (isProcessing.value) return;
    isProcessing.value = true;
    try {
        const updated = await dichVuDonHang.addSanPham(selectedOrder.value.id, {
            idChiTietSanPham: product.id,
            soLuong: 1
        });
        updateOrderInList(updated);
        await refreshBestVoucher(updated);
        addNotification({ title: 'Thêm thành công', subtitle: product.tenSanPham, color: 'success' });
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: MESSAGES.ERROR.PRODUCT_OUT_OF_STOCK, color: 'error' });
    } finally {
        isProcessing.value = false;
    }
};

const onUpdateQty = async (item, delta) => {
    const newQty = item.soLuong + delta;
    if (newQty < 1) return;

    if (delta > 0) {
        let sumInOtherCarts = 0;
        orders.value.forEach(order => {
            if (order.id !== selectedOrder.value.id && order.listsHoaDonChiTiet) {
                order.listsHoaDonChiTiet.forEach(oItem => {
                    if (oItem.idChiTietSanPham === item.idChiTietSanPham) {
                        sumInOtherCarts += oItem.soLuong || 0;
                    }
                });
            }
        });
        const maxAllowed = Math.max(0, (item.soLuongTon || 0) - sumInOtherCarts);
        if (newQty > maxAllowed) {
            addNotification({
                title: 'Không đủ hàng',
                subtitle: `Không thể tăng số lượng. Sản phẩm đã đạt giới hạn tồn kho cho phép (Tối đa ${maxAllowed} sản phẩm có thể thêm).`,
                color: 'warning'
            });
            return;
        }
    }

    try {
        const updated = await dichVuDonHang.updateSoLuong(selectedOrder.value.id, item.id, newQty);
        updateOrderInList(updated);
        await refreshBestVoucher(updated);
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: MESSAGES.ERROR.PRODUCT_OUT_OF_STOCK, color: 'error' });
    }
};

const onRemoveItem = (item) => {
    const currentOrderId = selectedOrder.value?.id || null;
    confirmDialog.value = {
        show: true,
        title: 'Xác nhận xóa sản phẩm',
        message: `Bạn có chắc chắn muốn xóa [${item.tenSanPham}] khỏi giỏ hàng?`,
        color: 'warning',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                if (!currentOrderId) throw new Error('Không tìm thấy hóa đơn đang chọn.');
                await dichVuDonHang.removeSanPham(currentOrderId, item.id);
                const data = await dichVuDonHang.layDonHangCho();
                setOrders(data, { preferOrderId: currentOrderId });
                await refreshBestVoucher();
                confirmDialog.value.show = false;
            } catch (e) {
                addNotification({ title: 'Lỗi', subtitle: MESSAGES.ERROR.DELETE_DATA, color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

// Logic: Khách hàng & Voucher
const onSelectCustomer = async (customer) => {
    try {
        const updated = await dichVuDonHang.setKhachHang(selectedOrder.value.id, customer.id);
        updateOrderInList(updated);
        await refreshBestVoucher(updated);
    } catch (e) {
        addNotification({ title: 'Lỗi khách hàng', subtitle: getErrorMessage(e, 'Không thể gắn khách hàng vào hóa đơn.'), color: 'error' });
    }
};

const onRemoveCustomer = async () => {
    try {
        const updated = await dichVuDonHang.setKhachHang(selectedOrder.value.id, null);
        updateOrderInList(updated);
        await refreshBestVoucher(updated);
    } catch (e) {
        addNotification({ title: 'Lỗi khách hàng', subtitle: getErrorMessage(e, 'Không thể bỏ khách hàng khỏi hóa đơn.'), color: 'error' });
    }
};

const calculateVoucherDiscount = (voucher, total) => {
    if (!voucher || !total) return 0;
    const minimum = Number(voucher.donHangToiThieu || 0);
    if (Number(total) < minimum) return 0;

    const type = String(voucher.loaiPhieu || '').toUpperCase();
    if (type === 'PHAN_TRAM' || type === 'PERCENT') {
        const percent = Number(voucher.phanTramGiamGia || 0);
        const maxDiscount = Number(voucher.giamToiDa || Number.MAX_SAFE_INTEGER);
        return Math.min((Number(total) * percent) / 100, maxDiscount);
    }

    return Number(voucher.soTienGiam || 0);
};

const isPersonalVoucher = (voucher) => {
    const value = String(voucher?.hinhThuc ?? '').toUpperCase();
    return value === 'CA_NHAN' || value === 'CANHAN' || value === 'PERSONAL' || value === 'PRIVATE';
};

const isPublicVoucher = (voucher) => {
    const value = String(voucher?.hinhThuc ?? '').toUpperCase();
    return !value || value === 'CONG_KHAI' || value === 'CONGKHAI' || value === 'PUBLIC' || value === 'ALL';
};

const extractVoucherPageContent = (response) => {
    if (Array.isArray(response?.data?.content)) return response.data.content;
    if (Array.isArray(response?.content)) return response.content;
    if (Array.isArray(response?.data)) return response.data;
    if (Array.isArray(response)) return response;
    return [];
};

const loadEligibleVouchers = async (order) => {
    const total = Number(order?.tongTien || 0);
    const customerId = order?.idKhachHang || null;
    const response = await dichVuPhieuGiamGia.layPhieuGiamGiaPhanTrang({
        page: 0,
        size: 1000,
        trangThai: 'DANG_HOAT_DONG',
        timelineStatus: 'DANG_HOAT_DONG'
    });

    return extractVoucherPageContent(response).filter((voucher) => {
        if (calculateVoucherDiscount(voucher, total) <= 0) return false;
        if (isPublicVoucher(voucher)) return true;
        if (!isPersonalVoucher(voucher) || !customerId) return false;

        const assignedCustomerIds = Array.isArray(voucher.listIdKhachHang) ? voucher.listIdKhachHang : [];
        return assignedCustomerIds.includes(customerId);
    });
};

const pickBestVoucher = (items, total) => {
    return [...(items || [])]
        .map((voucher) => ({
            voucher,
            discount: calculateVoucherDiscount(voucher, total)
        }))
        .filter((entry) => entry.discount > 0)
        .sort((a, b) => b.discount - a.discount)[0]?.voucher || null;
};

const refreshBestVoucher = async (orderOverride = selectedOrder.value) => {
    if (!orderOverride?.id || isAutoApplyingVoucher.value) return;
    try {
        const availableVouchers = await loadEligibleVouchers(orderOverride);
        vouchers.value = availableVouchers || [];

        const bestVoucher = pickBestVoucher(vouchers.value, orderOverride.tongTien || 0);
        const bestVoucherId = bestVoucher?.id || null;
        const currentVoucherId = orderOverride.idPhieuGiamGia || null;

        if (bestVoucherId !== currentVoucherId) {
            isAutoApplyingVoucher.value = true;
            const updated = await dichVuDonHang.setVoucher(orderOverride.id, bestVoucherId);
            updateOrderInList(updated);
        }
    } catch (e) {
        vouchers.value = [];
    } finally {
        isAutoApplyingVoucher.value = false;
    }
};

const onApplyVoucher = async (voucherId) => {
    try {
        const updated = await dichVuDonHang.setVoucher(selectedOrder.value.id, voucherId);
        updateOrderInList(updated);
    } catch (e) {}
};

// Logic: Thanh toán VNPay
const vnpayDialog = ref({
    show: false,
    loading: false,
    statusText: '',
    orderId: '',
    amount: 0,
    popup: null,
    pollInterval: null
});

const clearVnPayPolling = () => {
    if (vnpayDialog.value.pollInterval) {
        clearInterval(vnpayDialog.value.pollInterval);
        vnpayDialog.value.pollInterval = null;
    }
};

const closeVnPayFlow = () => {
    clearVnPayPolling();
    if (vnpayDialog.value.popup && !vnpayDialog.value.popup.closed) {
        vnpayDialog.value.popup.close();
    }
    vnpayDialog.value.show = false;
    vnpayDialog.value.loading = false;
    vnpayDialog.value.popup = null;
};

const cancelVnPayFlow = () => {
    sessionStorage.removeItem(VNPAY_PENDING_KEY);
    closeVnPayFlow();
    addNotification({ title: 'Hủy thanh toán', subtitle: 'Giao dịch VNPay đã được hủy bỏ', color: 'info' });
};

const buildCheckoutPayload = (order, overrides = {}) => {
    const payableAmount = Number(order?.tongTienSauGiam || 0);
    return {
        idKhachHang: order?.idKhachHang || null,
        idPhieuGiamGia: order?.idPhieuGiamGia || null,
        tongTien: order?.tongTien || 0,
        phiVanChuyen: 0,
        tongTienSauGiam: payableAmount,
        loaiDon: 'TAI_QUAY',
        ghiChu: checkoutData.value.note || '',
        tienMat: 0,
        tienChuyenKhoan: 0,
        maGiaoDich: null,
        ...overrides
    };
};

const hasPaymentAmount = (payload) => Number(payload?.tienMat || 0) > 0 || Number(payload?.tienChuyenKhoan || 0) > 0;

const buildCheckoutPayloadWithoutPaymentRecord = (payload) => {
    const paymentNote = [
        payload?.ghiChu,
        Number(payload?.tienMat || 0) > 0 ? `Tiền mặt: ${payload.tienMat}` : '',
        Number(payload?.tienChuyenKhoan || 0) > 0 ? `Chuyển khoản/VNPay: ${payload.tienChuyenKhoan}` : '',
        payload?.maGiaoDich ? `Mã GD: ${payload.maGiaoDich}` : ''
    ].filter(Boolean).join(' | ');

    return {
        ...payload,
        ghiChu: paymentNote,
        tienMat: null,
        tienChuyenKhoan: null
    };
};

const completePaidOrder = async (orderId) => {
    const index = orders.value.findIndex((order) => order.id === orderId);
    if (index !== -1) {
        orders.value.splice(index, 1);
    }
    if (orders.value.length === 0) {
        await createNewOrder({ force: true, silent: true });
    }
    clampActiveOrderIndex();
    checkoutData.value.receivedAmount = 0;
    checkoutData.value.note = '';
};

const showReceipt = (order, paymentMethod, receivedAmount, note) => {
    receiptDialog.value = {
        show: true,
        order: JSON.parse(JSON.stringify(order)), // deep clone để tránh mất data khi xóa order
        paymentMethod,
        receivedAmount: Number(receivedAmount || 0),
        note: note || '',
        paidAt: Date.now()
    };
};

const onCloseReceipt = async () => {
    receiptDialog.value.show = false;
};

const submitCheckout = async ({ order = selectedOrder.value, payload, successMessage = MESSAGES.SUCCESS.PAYMENT, showReceiptAfter = true }) => {
    if (!order?.id) {
        throw new Error('Không tìm thấy hóa đơn đang thanh toán.');
    }
    if (!order?.listsHoaDonChiTiet?.length) {
        throw new Error('Hóa đơn chưa có sản phẩm.');
    }

    const requestPayload = BYPASS_PAYMENT_RECORD_INSERT && hasPaymentAmount(payload)
        ? buildCheckoutPayloadWithoutPaymentRecord(payload)
        : payload;

    // Snapshot dữ liệu trước khi checkout để hiển thị hóa đơn
    const orderSnapshot = JSON.parse(JSON.stringify(order));
    const pmMethod = checkoutData.value.paymentMethod;
    const pmReceived = checkoutData.value.receivedAmount;
    const pmNote = checkoutData.value.note;

    await dichVuDonHang.checkout(order.id, requestPayload);
    addNotification({ title: 'Thành công', subtitle: successMessage, color: 'success' });

    // Xóa order khỏi danh sách
    await completePaidOrder(order.id);

    // Hiển thị hóa đơn sau khi thanh toán
    if (showReceiptAfter) {
        showReceipt(orderSnapshot, pmMethod, pmReceived, pmNote);
    }
};

const finalizeVnPayCheckout = async (tienChuyenKhoan, maGiaoDich, order = selectedOrder.value) => {
    vnpayDialog.value.loading = true;
    vnpayDialog.value.statusText = 'Đang xác nhận hóa đơn và cập nhật tồn kho...';
    try {
        await submitCheckout({
            order,
            payload: buildCheckoutPayload(order, {
                tienChuyenKhoan,
                maGiaoDich
            })
        });
        sessionStorage.removeItem(VNPAY_PENDING_KEY);
        closeVnPayFlow();
    } catch (error) {
        addNotification({ title: 'Không thể hoàn tất VNPay', subtitle: getErrorMessage(error, MESSAGES.ERROR.PAYMENT_FAILED), color: 'error' });
        vnpayDialog.value.loading = false;
        vnpayDialog.value.statusText = 'Giao dịch đã được xử lý nhưng hệ thống chưa thể chốt hóa đơn. Vui lòng kiểm tra lại đơn hàng.';
    }
};

const confirmVnPayManual = () => {
    const timestamp = Date.now();
    const manualTxn = `VNP_manual_${timestamp}`;
    finalizeVnPayCheckout(selectedOrder.value.tongTienSauGiam, manualTxn);
};

const startVnPayFlow = async () => {
    confirmDialog.value.loading = true;
    try {
        if (!selectedOrder.value?.listsHoaDonChiTiet?.length) {
            throw new Error('Hóa đơn chưa có sản phẩm.');
        }
        const orderId = selectedOrder.value.maHoaDon + '_' + Date.now();
        const payload = {
            amount: selectedOrder.value.tongTienSauGiam,
            orderId: orderId,
            orderInfo: 'Thanh toan hoa don ' + selectedOrder.value.maHoaDon
        };
        
        const data = await dichVuVnPay.createPaymentUrl(payload);
        if (!data || !data.paymentUrl) {
            throw new Error('Không lấy được URL thanh toán từ server');
        }
        
        sessionStorage.setItem(VNPAY_PENDING_KEY, JSON.stringify({
            orderId: selectedOrder.value.id,
            maHoaDon: selectedOrder.value.maHoaDon,
            amount: selectedOrder.value.tongTienSauGiam,
            createdAt: Date.now()
        }));

        // Popup VNPay có thể chuyển qua domain ngân hàng rồi quay về return URL backend.
        // Vì khác origin, FE không luôn đọc được location; do đó vẫn có pending-state dự phòng.
        const popup = window.open(data.paymentUrl, '_blank', 'width=900,height=750,location=yes,status=yes,scrollbars=yes');
        
        confirmDialog.value.show = false;
        
        vnpayDialog.value = {
            show: true,
            loading: false,
            statusText: 'Vui lòng hoàn tất thanh toán trong cửa sổ VNPay.',
            orderId: orderId,
            amount: selectedOrder.value.tongTienSauGiam,
            popup: popup,
            pollInterval: null
        };
        
        vnpayDialog.value.pollInterval = setInterval(async () => {
            if (!popup || popup.closed) {
                clearVnPayPolling();
                vnpayDialog.value.statusText = 'Cửa sổ thanh toán đã bị đóng. Bạn có thể xác nhận thủ công nếu đã thanh toán.';
                return;
            }
            
            try {
                const currentUrl = popup.location.href;
                if (currentUrl && (currentUrl.includes('/vnpay-callback') || currentUrl.includes('vnp_ResponseCode'))) {
                    clearVnPayPolling();
                    
                    vnpayDialog.value.loading = true;
                    vnpayDialog.value.statusText = 'Đang kiểm tra kết quả giao dịch...';
                    
                    const urlObj = new URL(currentUrl);
                    const params = {};
                    urlObj.searchParams.forEach((val, key) => {
                        params[key] = val;
                    });
                    
                    popup.close();
                    vnpayDialog.value.popup = null;
                    
                    try {
                        const verifyResult = await dichVuVnPay.verifyPaymentCallback(params);
                        if (verifyResult && verifyResult.success) {
                            const txnNo = params['vnp_TransactionNo'] || `VNP_${Date.now()}`;
                            await finalizeVnPayCheckout(selectedOrder.value.tongTienSauGiam, txnNo);
                        } else {
                            addNotification({ 
                                title: 'Thanh toán thất bại', 
                                subtitle: verifyResult?.message || 'VNPay xác thực giao dịch không thành công', 
                                color: 'error' 
                            });
                            vnpayDialog.value.loading = false;
                            vnpayDialog.value.statusText = 'Giao dịch không hợp lệ hoặc đã bị hủy.';
                        }
                    } catch (err) {
                        console.error('Verify callback error:', err);
                        vnpayDialog.value.loading = false;
                        vnpayDialog.value.statusText = 'Không thể xác thực giao dịch tự động. Vui lòng bấm xác nhận thủ công.';
                    }
                }
            } catch (e) {
                // Khi popup đang ở domain VNPay/bank/backend khác origin, browser chặn đọc URL.
                // FE sẽ tiếp tục chờ tới khi popup đóng hoặc khi nhận callback trên URL frontend.
            }
        }, 1000);
        
    } catch (error) {
        console.error('VNPay flow error:', error);
        addNotification({ title: 'Lỗi VNPay', subtitle: getErrorMessage(error, 'Không thể khởi tạo giao dịch VNPay'), color: 'error' });
    } finally {
        confirmDialog.value.loading = false;
    }
};

// Logic: Thanh toán
const onCheckout = () => {
    if (!selectedOrder.value?.listsHoaDonChiTiet?.length) {
        addNotification({ title: 'Cảnh báo', subtitle: 'Vui lòng thêm sản phẩm trước khi thanh toán.', color: 'warning' });
        return;
    }

    if (checkoutData.value.paymentMethod === 'CASH' && Number(checkoutData.value.receivedAmount || 0) < Number(selectedOrder.value.tongTienSauGiam || 0)) {
        addNotification({ title: 'Cảnh báo', subtitle: MESSAGES.ERROR.INSUFFICIENT_FUNDS, color: 'warning' });
        return;
    }

    if (checkoutData.value.paymentMethod === 'VNPAY') {
        confirmDialog.value = {
            show: true,
            title: 'Xác nhận thanh toán VNPay',
            message: `Hệ thống sẽ mở cổng thanh toán VNPay với số tiền [${new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(selectedOrder.value.tongTienSauGiam)}]. Bạn có muốn tiếp tục không?`,
            color: 'primary',
            action: startVnPayFlow,
            loading: false
        };
        return;
    }

    confirmDialog.value = {
        show: true,
        title: 'Xác nhận thanh toán',
        message: `Bạn xác nhận thanh toán hóa đơn với tổng tiền [${new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(selectedOrder.value.tongTienSauGiam)}]?`,
        color: 'primary',
        action: async () => {
            confirmDialog.value.loading = true;
            isProcessing.value = true;
            const orderSnapshot = selectedOrder.value;
            try {
                await submitCheckout({
                    order: orderSnapshot,
                    payload: buildCheckoutPayload(orderSnapshot, {
                        tienMat: orderSnapshot.tongTienSauGiam,
                        tienChuyenKhoan: 0,
                        maGiaoDich: `CASH_${Date.now()}`
                    })
                });
                confirmDialog.value.show = false;
            } catch (e) {
                addNotification({ title: 'Thanh toán tiền mặt thất bại', subtitle: getErrorMessage(e, MESSAGES.ERROR.PAYMENT_FAILED), color: 'error' });
            } finally {
                isProcessing.value = false;
                confirmDialog.value.loading = false;
            }
        }
    };
};

// Helpers
const updateOrderInList = (updated) => {
    if (!updated?.id) return;
    const idx = orders.value.findIndex(o => o.id === updated.id);
    if (idx !== -1) orders.value[idx] = updated;
    clampActiveOrderIndex();
};

const getErrorMessage = (error, fallback) => {
    return error?.response?.data?.message || error?.response?.data?.error || error?.message || fallback;
};

const getStoredVnPayOrder = () => {
    try {
        const pending = JSON.parse(sessionStorage.getItem(VNPAY_PENDING_KEY) || '{}');
        if (!pending?.orderId) return null;
        return orders.value.find((order) => order.id === pending.orderId) || null;
    } catch (e) {
        return null;
    }
};

const handleVnPayCallbackFromUrl = async () => {
    const params = new URLSearchParams(window.location.search);
    if (!params.has('vnp_ResponseCode')) return;

    const callbackParams = {};
    params.forEach((value, key) => {
        callbackParams[key] = value;
    });

    const pendingOrder = getStoredVnPayOrder();
    if (!pendingOrder) {
        addNotification({
            title: 'Không tìm thấy hóa đơn VNPay',
            subtitle: 'Không có phiên thanh toán VNPay đang chờ trong trình duyệt.',
            color: 'warning'
        });
        return;
    }

    try {
        const verifyResult = await dichVuVnPay.verifyPaymentCallback(callbackParams);
        if (!verifyResult?.success) {
            throw new Error(verifyResult?.message || 'VNPay không xác nhận giao dịch thành công.');
        }

        const transactionNo = callbackParams.vnp_TransactionNo || `VNP_${Date.now()}`;
        await submitCheckout({
            order: pendingOrder,
            payload: buildCheckoutPayload(pendingOrder, {
                tienChuyenKhoan: pendingOrder.tongTienSauGiam,
                maGiaoDich: transactionNo
            })
        });
        sessionStorage.removeItem(VNPAY_PENDING_KEY);
        window.history.replaceState({}, document.title, window.location.pathname);
    } catch (error) {
        addNotification({
            title: 'Xác nhận VNPay thất bại',
            subtitle: getErrorMessage(error, 'Không thể xác nhận kết quả thanh toán VNPay.'),
            color: 'error'
        });
    }
};
</script>

<template>
    <v-container fluid class="pos-wrapper pa-0">
        <div class="pos-shell">
            <header class="pos-header">
                <div>
                    <div class="text-h6 font-weight-bold text-slate-900">Bán hàng tại quầy</div>
                    <div class="text-caption text-grey-darken-1">
                        {{ orders.length }}/{{ MAX_WAITING_ORDERS }} hóa đơn chờ
                        <span v-if="selectedOrder"> - {{ selectedOrderItemCount }} sản phẩm trong đơn hiện tại</span>
                    </div>
                </div>
                <OrderTabs
                    class="flex-grow-1 min-w-0"
                    :orders="orders"
                    :active-index="activeOrderIndex"
                    @select="idx => activeOrderIndex = idx"
                    @create="createNewOrder"
                    @close="closeOrder"
                />
            </header>

            <v-row v-if="selectedOrder" no-gutters class="pos-grid">
                <v-col cols="12" xl="5" lg="5" class="pos-column cart-column">
                    <div class="column-head">
                        <div>
                            <div class="text-subtitle-1 font-weight-bold">Giỏ hàng</div>
                            <div class="text-caption text-grey-darken-1">Mã hóa đơn: {{ selectedOrder.maHoaDon }}</div>
                        </div>
                    </div>
                    <CartTable
                        :items="selectedOrder.listsHoaDonChiTiet"
                        @update-qty="onUpdateQty"
                        @remove="onRemoveItem"
                    />
                </v-col>

                <v-col cols="12" xl="4" lg="4" class="pos-column product-column">
                    <ProductPicker
                        :active-order-id="selectedOrder.id"
                        :orders="orders"
                        :loading-external="isProcessing"
                        @add-product="onAddProduct"
                    />
                </v-col>

                <v-col cols="12" xl="3" lg="3" class="pos-column payment-column">
                    <div class="customer-block">
                        <CustomerSelector
                            :selected-customer-name="selectedOrder.tenKhachHang"
                            :selected-customer-phone="selectedOrder.sdtKhachHang"
                            :active-order-id="selectedOrder.id"
                            @select="onSelectCustomer"
                            @remove="onRemoveCustomer"
                        />
                    </div>

                    <CheckoutPanel
                        :order="selectedOrder"
                        :vouchers="vouchers"
                        :checkout-data="checkoutData"
                        :loading="isProcessing"
                        @apply-voucher="onApplyVoucher"
                        @checkout="onCheckout"
                    />
                </v-col>
            </v-row>

            <div v-else-if="loading" class="fill-height d-flex align-center justify-center text-grey">
                <v-progress-circular indeterminate color="primary"></v-progress-circular>
            </div>

            <div v-else class="empty-orders-state">
                <v-icon size="56" color="grey-lighten-1">mdi-receipt-text-outline</v-icon>
                <div class="text-subtitle-1 font-weight-bold mt-3">Chưa có hóa đơn chờ</div>
                <div class="text-body-2 text-grey-darken-1 mt-1">Tạo hóa đơn mới để bắt đầu bán hàng tại quầy.</div>
                <v-btn color="primary" class="mt-4 rounded-lg" :loading="isProcessing" @click="createNewOrder">
                    Tạo hóa đơn
                </v-btn>
            </div>
        </div>

        <!-- Confirmation Dialog -->
        <AdminConfirm 
            v-model:show="confirmDialog.show"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            :color="confirmDialog.color"
            :loading="confirmDialog.loading"
            @confirm="confirmDialog.action"
        />

        <!-- VNPay Processing Dialog -->
        <v-dialog v-model="vnpayDialog.show" persistent max-width="500">
            <v-card class="rounded-xl pa-6 text-center">
                <div class="d-flex justify-space-between align-center mb-4">
                    <span class="text-h6 font-weight-bold">Thanh toán qua VNPay</span>
                    <v-btn icon variant="text" size="small" :disabled="vnpayDialog.loading" @click="cancelVnPayFlow">
                        <v-icon>mdi-close</v-icon>
                    </v-btn>
                </div>
                
                <div class="my-6">
                    <div v-if="vnpayDialog.loading" class="d-flex flex-column align-center">
                        <v-progress-circular indeterminate color="primary" size="64" width="6" class="mb-4"></v-progress-circular>
                        <div class="text-body-1 font-weight-medium text-grey-darken-2">{{ vnpayDialog.statusText }}</div>
                    </div>
                    <div v-else class="d-flex flex-column align-center">
                        <div class="vnpay-logo-wrapper mb-4">
                            <v-icon size="64" color="primary" class="animate-pulse">mdi-credit-card-scan-outline</v-icon>
                        </div>
                        <div class="text-h5 font-weight-bold text-primary mb-2">
                            {{ new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(vnpayDialog.amount) }}
                        </div>
                        <div class="text-caption text-grey mb-4">Mã hóa đơn: <b>{{ selectedOrder?.maHoaDon }}</b></div>
                        <v-alert type="info" variant="tonal" class="rounded-lg mb-4 text-left" density="compact">
                            Một cửa sổ thanh toán VNPay đã được mở. Vui lòng hoàn tất thanh toán trên cửa sổ đó.
                        </v-alert>
                        <div class="text-body-2 text-grey-darken-1">{{ vnpayDialog.statusText }}</div>
                    </div>
                </div>

                <v-divider class="my-4"></v-divider>

                <div class="d-flex gap-3 justify-center">
                    <v-btn
                        variant="outlined"
                        color="error"
                        class="rounded-lg px-4"
                        :disabled="vnpayDialog.loading"
                        @click="cancelVnPayFlow"
                    >
                        Hủy giao dịch
                    </v-btn>
                    <v-btn
                        color="success"
                        class="rounded-lg px-4 font-weight-bold text-white shadow"
                        :loading="vnpayDialog.loading"
                        @click="confirmVnPayManual"
                    >
                        Xác nhận thanh toán
                    </v-btn>
                </div>
            </v-card>
        </v-dialog>

        <!-- Hóa đơn sau thanh toán -->
        <InvoiceReceiptDialog
            :show="receiptDialog.show"
            :receipt="receiptDialog"
            @close="onCloseReceipt"
        />
    </v-container>
</template>

<style scoped>
.pos-wrapper {
    height: calc(100vh - 64px);
    overflow: hidden;
    background: #eef2f6;
}

.pos-shell {
    height: 100%;
    display: flex;
    flex-direction: column;
    gap: 14px;
    padding: 16px;
}

.pos-header {
    display: flex;
    align-items: center;
    gap: 18px;
    min-height: 74px;
    padding: 12px 16px;
    background: #ffffff;
    border: 1px solid #dfe5ee;
    border-radius: 8px;
    box-shadow: 0 6px 18px rgba(15, 23, 42, 0.04);
}

.pos-grid {
    flex: 1;
    min-height: 0;
    gap: 14px;
    flex-wrap: nowrap;
}

.pos-column {
    height: 100%;
    min-height: 0;
    overflow-y: auto;
    background: #ffffff;
    border: 1px solid #dfe5ee;
    border-radius: 8px;
    padding: 14px;
    box-shadow: 0 6px 18px rgba(15, 23, 42, 0.04);
}

.cart-column {
    display: flex;
    flex-direction: column;
}

.product-column {
    background: #f8fafc;
}

.payment-column {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.column-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
    flex-shrink: 0;
}

.customer-block {
    padding: 12px;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    background: #f8fafc;
    flex-shrink: 0;
}

.min-w-0 {
    min-width: 0;
}

.empty-orders-state {
    flex: 1;
    min-height: 420px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #475569;
    background: #ffffff;
    border: 1px dashed #cbd5e1;
    border-radius: 8px;
}

.shadow-lg {
    box-shadow: -10px 0 25px -5px rgba(0, 0, 0, 0.05) !important;
}

@media (max-width: 1264px) {
    .pos-wrapper {
        height: auto;
        overflow-y: auto;
    }
    .pos-grid {
        flex-wrap: wrap;
    }
    .pos-column {
        height: auto;
        min-height: 520px;
    }
    .pos-header {
        align-items: flex-start;
        flex-direction: column;
    }
}

.animate-pulse {
    animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}
@keyframes pulse {
    0%, 100% { opacity: 1; transform: scale(1); }
    50% { opacity: .5; transform: scale(0.95); }
}
.vnpay-logo-wrapper {
    background: #f1f5f9;
    border-radius: 50%;
    width: 96px;
    height: 96px;
    display: flex;
    align-items: center;
    justify-content: center;
}
.gap-3 {
    gap: 12px;
}
</style>
