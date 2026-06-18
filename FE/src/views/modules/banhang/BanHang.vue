<script setup>
/**
 * Module: Bán hàng tại quầy (Admin/POS)
 * View: BanHang
 * Chức năng: Màn hình chính xử lý tạo hóa đơn, thêm sản phẩm, cập nhật số lượng,
 *            chọn khách hàng, áp dụng voucher, thanh toán bằng tiền mặt/chuyển khoản (VNPay),
 *            và in hóa đơn sau khi hoàn tất.
 */
import { ref, onMounted, onUnmounted, computed, watch } from 'vue';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';
import { dichVuVnPay } from './dichVuVnPay';
import { dichVuPhieuGiamGia } from '@/services/admin/dichVuPhieuGiamGia';
import { useNotifications } from '@/services/notificationService';
import { MESSAGES } from '@/constants/messages';
import { useUIStore } from '@/stores/ui';

// Import Components
import OrderTabs from './components/OrderTabs.vue';
import ProductPicker from './components/ProductPicker.vue';
import CartTable from './components/CartTable.vue';
import CustomerSelector from './components/CustomerSelector.vue';
import CheckoutPanel from './components/CheckoutPanel.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import InvoiceReceiptDialog from './components/InvoiceReceiptDialog.vue';

const { addNotification } = useNotifications();
const uiStore = useUIStore();
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
const manualVoucherLocks = ref({});
const productPickerRef = ref(null);

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
    vnpayMethod: 'QR',
    receivedAmount: 0,
    note: ''
});

const confirmDialog = ref({
    show: false,
    title: '',
    message: '',
    color: 'primary',
    action: null,
    loading: false
});

const vnpayChoiceDialog = ref({
    show: false,
    method: 'QR'
});

const proceedVnPayChoice = () => {
    vnpayChoiceDialog.value.show = false;
    checkoutData.value.vnpayMethod = vnpayChoiceDialog.value.method;
    startVnPayFlow();
};

const selectedOrder = computed(() => orders.value[activeOrderIndex.value] || null);
const selectedOrderItemCount = computed(() =>
    (selectedOrder.value?.listsHoaDonChiTiet || []).reduce((sum, item) => sum + (Number(item.soLuong) || 0), 0)
);

// Chuẩn hóa định dạng danh sách đơn hàng trả về từ nhiều dạng API response khác nhau
const normalizeOrderList = (payload) => {
    if (Array.isArray(payload)) return payload;
    if (Array.isArray(payload?.content)) return payload.content;
    if (Array.isArray(payload?.data)) return payload.data;
    return [];
};

// Đảm bảo chỉ số tab hóa đơn đang kích hoạt luôn nằm trong khoảng hợp lệ
const clampActiveOrderIndex = () => {
    if (!orders.value.length) {
        activeOrderIndex.value = 0;
        return;
    }

    if (activeOrderIndex.value < 0 || activeOrderIndex.value >= orders.value.length) {
        activeOrderIndex.value = Math.max(0, orders.value.length - 1);
    }
};

// Gán danh sách hóa đơn từ API và trỏ đến hóa đơn mong muốn (nếu có)
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

// Global keydown handler for F10
const handleGlobalKeyDown = (e) => {
    if (e.key === 'F10') {
        e.preventDefault();
        onCheckout();
    }
};

// Lifecycle
onMounted(async () => {
    uiStore.setBreadcrumbs([
        { title: 'Bán hàng', disabled: false, href: '/admin/ban-hang' },
        { title: 'Tạo đơn hàng', disabled: true }
    ]);
    window.addEventListener('keydown', handleGlobalKeyDown);
    loading.value = true;
    try {
        const data = await dichVuDonHang.layDonHangCho();
        setOrders(data);
        
        // Load offline vouchers
        const voucherResp = await dichVuPhieuGiamGia.layPhieuGiamGiaPhanTrang({
            page: 0,
            size: 1000,
            trangThai: 'DANG_HOAT_DONG',
            timelineStatus: 'DANG_HOAT_DONG'
        });
        allActiveVouchers.value = extractVoucherPageContent(voucherResp);

        await handleVnPayCallbackFromUrl();
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: getErrorMessage(error, MESSAGES.ERROR.CONNECT_SERVER), color: 'error' });
    } finally {
        loading.value = false;
    }
});

onUnmounted(() => {
    window.removeEventListener('keydown', handleGlobalKeyDown);
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
// Tạo mới một hóa đơn chờ (tab hóa đơn mới), tối đa 5 hóa đơn
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

// Xóa một hóa đơn chờ (tab hóa đơn)
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
                if (productPickerRef.value && typeof productPickerRef.value.refresh === 'function') {
                    productPickerRef.value.refresh();
                }
            } catch (e) {
                addNotification({ title: 'Lỗi', subtitle: MESSAGES.ERROR.DELETE_DATA, color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

// Logic: Sản phẩm
// Xử lý thêm một sản phẩm vào hóa đơn đang được chọn
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
            soLuong: product._soLuongMuonThem || 1
        });
        updateOrderInList(updated);
        addNotification({ title: 'Thành công', subtitle: 'Đã thêm sản phẩm vào giỏ hàng', color: 'success' });
        // Fire-and-forget: không await để tránh treo isProcessing khi debounce
        refreshBestVoucher(updated);
        
        // Tải lại bảng sản phẩm bên ngoài để cập nhật hiển thị tồn kho mới
        if (productPickerRef.value && typeof productPickerRef.value.refresh === 'function') {
            productPickerRef.value.refresh();
        }
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: MESSAGES.ERROR.PRODUCT_OUT_OF_STOCK, color: 'error' });
    } finally {
        isProcessing.value = false;
    }
};

// Xử lý thay đổi số lượng của 1 sản phẩm trong giỏ hàng (tăng/giảm)
const onUpdateQty = async (item, delta, inputEventTarget = null) => {
    let newQty = item.soLuong + delta;
    if (newQty < 1) {
        // Trừ thẳng trực tiếp về 0 => tự động xóa khỏi giỏ
        try {
            if (!selectedOrder.value?.id) return;
            const updated = await dichVuDonHang.removeSanPham(selectedOrder.value.id, item.id);
            const data = await dichVuDonHang.layDonHangCho();
            setOrders(data, { preferOrderId: selectedOrder.value.id });
            refreshBestVoucher();
            if (productPickerRef.value && typeof productPickerRef.value.refresh === 'function') {
                productPickerRef.value.refresh();
            }
        } catch (e) {
            addNotification({ title: 'Lỗi', subtitle: MESSAGES.ERROR.DELETE_DATA, color: 'error' });
        }
        return;
    }

    if (delta > 0) {
        // Tồn kho trong DB đã được trừ ngay lập tức khi thêm vào giỏ.
        // Vậy số lượng có thể thêm tối đa (maxAllowed) chính là số đang có + tồn kho còn lại.
        const maxAllowed = item.soLuong + (item.soLuongTon || 0);
        if (newQty > maxAllowed) {
            addNotification({
                title: 'Vượt quá tồn kho',
                subtitle: `Chỉ còn tối đa ${maxAllowed} sản phẩm. Hệ thống đã tự động điều chỉnh.`,
                color: 'warning'
            });
            newQty = maxAllowed;
            if (newQty === item.soLuong) {
                // Đã đạt max từ trước, chỉ khôi phục lại hiển thị ô input
                if (inputEventTarget) {
                    inputEventTarget.value = item.soLuong;
                }
                return;
            }
        }
    }

    try {
        const updated = await dichVuDonHang.updateSoLuong(selectedOrder.value.id, item.id, newQty);
        updateOrderInList(updated);
        refreshBestVoucher(updated);
        if (productPickerRef.value && typeof productPickerRef.value.refresh === 'function') {
            productPickerRef.value.refresh();
        }
    } catch (e) {
        // Khôi phục giá trị input nếu gọi API lỗi
        if (inputEventTarget) {
            inputEventTarget.value = item.soLuong;
        }
        addNotification({ title: 'Lỗi', subtitle: MESSAGES.ERROR.PRODUCT_OUT_OF_STOCK, color: 'error' });
    }
};

// Xóa 1 sản phẩm khỏi giỏ hàng
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
                refreshBestVoucher();
                confirmDialog.value.show = false;
                if (productPickerRef.value && typeof productPickerRef.value.refresh === 'function') {
                    productPickerRef.value.refresh();
                }
            } catch (e) {
                addNotification({ title: 'Lỗi', subtitle: MESSAGES.ERROR.DELETE_DATA, color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

// Logic: Khách hàng & Voucher
// Gắn một khách hàng vào hóa đơn
const onSelectCustomer = async (customer) => {
    try {
        const updated = await dichVuDonHang.setKhachHang(selectedOrder.value.id, customer.id);
        updateOrderInList(updated);
        refreshBestVoucher(updated);
    } catch (e) {
        addNotification({ title: 'Lỗi khách hàng', subtitle: getErrorMessage(e, 'Không thể gắn khách hàng vào hóa đơn.'), color: 'error' });
    }
};

// Gỡ bỏ khách hàng khỏi hóa đơn (đưa về khách lẻ)
const onRemoveCustomer = async () => {
    try {
        const updated = await dichVuDonHang.setKhachHang(selectedOrder.value.id, null);
        updateOrderInList(updated);
        refreshBestVoucher(updated);
    } catch (e) {
        addNotification({ title: 'Lỗi khách hàng', subtitle: getErrorMessage(e, 'Không thể bỏ khách hàng khỏi hóa đơn.'), color: 'error' });
    }
};

// Tính toán số tiền được giảm tương ứng với 1 voucher và tổng giá trị đơn hàng
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

// Kiểm tra voucher có phải loại Cá Nhân hay không
const isPersonalVoucher = (voucher) => {
    const value = String(voucher?.hinhThuc ?? '').toUpperCase();
    return value === 'CA_NHAN' || value === 'CANHAN' || value === 'PERSONAL' || value === 'PRIVATE';
};

// Kiểm tra voucher có phải loại Công Khai (tất cả mọi người) hay không
const isPublicVoucher = (voucher) => {
    const value = String(voucher?.hinhThuc ?? '').toUpperCase();
    return !value || value === 'CONG_KHAI' || value === 'CONGKHAI' || value === 'PUBLIC' || value === 'ALL';
};

// Bóc tách mảng voucher từ response API
const extractVoucherPageContent = (response) => {
    if (Array.isArray(response?.data?.content)) return response.data.content;
    if (Array.isArray(response?.content)) return response.content;
    if (Array.isArray(response?.data)) return response.data;
    if (Array.isArray(response)) return response;
    return [];
};

const allActiveVouchers = ref([]);

// Tải danh sách tất cả các voucher thỏa mãn điều kiện áp dụng cho hóa đơn hiện tại
const loadEligibleVouchers = (order) => {
    const total = Number(order?.tongTien || 0);
    const customerId = order?.idKhachHang || null;
    return allActiveVouchers.value.filter((voucher) => {
        if (calculateVoucherDiscount(voucher, total) <= 0) return false;
        if (isPublicVoucher(voucher)) return true;
        if (!isPersonalVoucher(voucher) || !customerId) return false;

        const assignedCustomerIds = Array.isArray(voucher.listIdKhachHang) ? voucher.listIdKhachHang : [];
        return assignedCustomerIds.includes(customerId);
    });
};

// Tự động lấy ra voucher có giá trị giảm giá cao nhất
const pickBestVoucher = (items, total) => {
    return [...(items || [])]
        .map((voucher) => ({
            voucher,
            discount: calculateVoucherDiscount(voucher, total)
        }))
        .filter((entry) => entry.discount > 0)
        .sort((a, b) => b.discount - a.discount)[0]?.voucher || null;
};

// Làm mới danh sách voucher và tự động áp dụng mã có lợi nhất (Offline)
const refreshBestVoucher = (orderOverride = selectedOrder.value) => {
    if (!orderOverride?.id) return;
    
    const availableVouchers = loadEligibleVouchers(orderOverride);
    vouchers.value = availableVouchers || [];

    const bestVoucher = pickBestVoucher(vouchers.value, orderOverride.tongTien || 0);
    const bestVoucherId = bestVoucher?.id || null;
    
    // Gợi ý mã tốt nhất vào state để UI hiển thị nhưng KHÔNG tự động apply vào idPhieuGiamGia
    orderOverride.suggestedVoucherId = bestVoucherId;

    // NẾU người dùng đã tự apply mã (có idPhieuGiamGia), kiểm tra xem nó còn hợp lệ không
    if (orderOverride.idPhieuGiamGia) {
        const currentVoucher = vouchers.value.find(v => String(v.id) === String(orderOverride.idPhieuGiamGia));
        const isCurrentValid = currentVoucher && Number(orderOverride.tongTien || 0) >= Number(currentVoucher.donHangToiThieu || 0);
        if (!isCurrentValid) {
            orderOverride.idPhieuGiamGia = null;
            manualVoucherLocks.value[orderOverride.id] = false;
        }
    }
    
    // Tính lại tổng tiền sau giảm giá để UI hiển thị ngay lập tức
    const appliedVoucher = vouchers.value.find(v => String(v.id) === String(orderOverride.idPhieuGiamGia));
    const discount = calculateVoucherDiscount(appliedVoucher, orderOverride.tongTien || 0);
    orderOverride.tongTienSauGiam = Math.max(0, (orderOverride.tongTien || 0) - discount);
};

// Cố định 1 voucher do người dùng tự chọn trên giao diện
const onApplyVoucher = (voucherId) => {
    manualVoucherLocks.value[selectedOrder.value.id] = true;
    selectedOrder.value.idPhieuGiamGia = voucherId;
    
    // Tính lại ngay lập tức
    const appliedVoucher = vouchers.value.find(v => String(v.id) === String(voucherId));
    const discount = calculateVoucherDiscount(appliedVoucher, selectedOrder.value.tongTien || 0);
    selectedOrder.value.tongTienSauGiam = Math.max(0, (selectedOrder.value.tongTien || 0) - discount);
};

// Logic: Thanh toán VNPay
const vnpayDialog = ref({
    show: false,
    loading: false,
    verified: false,
    statusText: '',
    orderId: '',
    amount: 0,
    pollInterval: null
});

let vnpayPopup = null;

// Dọn dẹp tiến trình lắng nghe thanh toán VNPay
const clearVnPayPolling = () => {
    if (vnpayDialog.value.pollInterval) {
        clearInterval(vnpayDialog.value.pollInterval);
        vnpayDialog.value.pollInterval = null;
    }
};

// Đóng cửa sổ/Tiến trình VNPay
const closeVnPayFlow = () => {
    clearVnPayPolling();
    try {
        if (vnpayPopup && !vnpayPopup.closed) {
            vnpayPopup.close();
        }
    } catch (e) {
        console.warn('Cannot check/close VNPay popup due to SecurityError:', e);
    }
    vnpayDialog.value.show = false;
    vnpayDialog.value.loading = false;
    vnpayDialog.value.verified = false;
    vnpayPopup = null;
};

// Hủy bỏ giao dịch VNPay
const cancelVnPayFlow = () => {
    sessionStorage.removeItem(VNPAY_PENDING_KEY);
    closeVnPayFlow();
    addNotification({ title: 'Hủy thanh toán', subtitle: 'Giao dịch VNPay đã được hủy bỏ', color: 'info' });
};

const handleVnPayCanceled = (subtitle = 'Cửa sổ VNPay đã đóng trước khi hệ thống nhận được kết quả thanh toán thành công.') => {
    sessionStorage.removeItem(VNPAY_PENDING_KEY);
    closeVnPayFlow();
    addNotification({
        title: 'Giao dịch bị hủy',
        subtitle,
        color: 'warning'
    });
};

const isVnPayCallbackSuccess = (params = {}) =>
    params.vnp_ResponseCode === '00' && (!params.vnp_TransactionStatus || params.vnp_TransactionStatus === '00' || params.vnp_TransactionStatus === '0');

const isVnPayVerifySuccess = (verifyResult, params = {}) =>
    Boolean(verifyResult?.success) && Number(verifyResult?.status || 200) === 200 && isVnPayCallbackSuccess(params);

// Chuẩn bị DTO thanh toán gửi lên backend
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

// Cắt giảm trường tiền mặt/chuyển khoản để fix một số lỗi mapping với backend
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

// Xử lý logic trên UI khi 1 hóa đơn đã được thanh toán thành công (Xóa tab, tạo tab mới)
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

// Hiển thị dialog in hóa đơn sau khi thanh toán
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

// Gửi request checkout đến API, sau đó trigger in hóa đơn
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

    try {
        await dichVuDonHang.checkout(order.id, requestPayload);
        addNotification({ title: 'Thành công', subtitle: successMessage, color: 'success' });

        // Xóa order khỏi danh sách
        await completePaidOrder(order.id);

        // Hiển thị hóa đơn sau khi thanh toán
        if (showReceiptAfter) {
            showReceipt(orderSnapshot, pmMethod, pmReceived, pmNote);
        }
    } catch (e) {
        const errorMsg = getErrorMessage(e, MESSAGES.ERROR.PAYMENT_FAILED);
        if (errorMsg.includes('Vui lòng tải lại giỏ hàng')) {
            confirmDialog.value = {
                show: true,
                title: 'Dữ liệu thay đổi',
                message: errorMsg + ' Bạn có muốn làm mới giỏ hàng ngay bây giờ?',
                color: 'warning',
                loading: false,
                action: async () => {
                    confirmDialog.value.loading = true;
                    try {
                        const data = await dichVuDonHang.layDonHangCho();
                        setOrders(data);
                        addNotification({ title: 'Đã cập nhật', subtitle: 'Giỏ hàng đã được làm mới.', color: 'info' });
                    } catch (err) {
                        addNotification({ title: 'Lỗi', subtitle: 'Không thể làm mới giỏ hàng', color: 'error' });
                    } finally {
                        confirmDialog.value.show = false;
                        confirmDialog.value.loading = false;
                    }
                }
            };
            // Ném lỗi để dừng luồng bên ngoài (nếu có)
            throw new Error('RELOAD_REQUIRED');
        } else {
            throw e;
        }
    }
};

// Hoàn tất luồng thanh toán chuyển khoản với mã giao dịch trả về
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
        vnpayDialog.value.verified = true;
        sessionStorage.removeItem(VNPAY_PENDING_KEY);
        closeVnPayFlow();
    } catch (error) {
        addNotification({ title: 'Không thể hoàn tất VNPay', subtitle: getErrorMessage(error, MESSAGES.ERROR.PAYMENT_FAILED), color: 'error' });
        vnpayDialog.value.loading = false;
        vnpayDialog.value.statusText = 'Giao dịch đã được xử lý nhưng hệ thống chưa thể chốt hóa đơn. Vui lòng kiểm tra lại đơn hàng.';
    }
};

// Khởi tạo quy trình thanh toán VNPay, popup thanh toán và polling trạng thái
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
            orderInfo: 'Thanh toan hoa don ' + selectedOrder.value.maHoaDon,
            returnUrl: `${window.location.origin}${window.location.pathname}`
        };

        const data = await dichVuVnPay.createPaymentUrl(payload);
        if (!data || !data.paymentUrl) {
            throw new Error('Không lấy được URL thanh toán từ server');
        }

        sessionStorage.setItem(VNPAY_PENDING_KEY, JSON.stringify({
            orderId: selectedOrder.value.id,
            maHoaDon: selectedOrder.value.maHoaDon,
            amount: selectedOrder.value.tongTienSauGiam,
            transactionId: orderId
        }));

        if (checkoutData.value.vnpayMethod === 'GATEWAY') {
            vnpayPopup = window.open(data.paymentUrl, 'vnpay', 'width=800,height=600');
            vnpayDialog.value.pollInterval = setInterval(() => {
                if (vnpayPopup && vnpayPopup.closed) {
                    clearInterval(vnpayDialog.value.pollInterval);
                    vnpayDialog.value.pollInterval = null;
                    if (!vnpayDialog.value.verified && sessionStorage.getItem(VNPAY_PENDING_KEY)) {
                        handleVnPayCanceled('Khách hàng đã đóng cửa sổ thanh toán.');
                    }
                }
            }, 1000);
            return;
        }

        vnpayDialog.value = {
            show: true,
            loading: false,
            verified: false,
            statusText: 'Đang chờ khách hàng quét mã và thanh toán...',
            orderId: orderId,
            amount: selectedOrder.value.tongTienSauGiam,
            paymentUrl: data.paymentUrl,
            qrUrl: `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(data.paymentUrl)}`,
            pollInterval: null
        };

        confirmDialog.value.show = false;
    } catch (error) {
        console.error('VNPay flow error:', error);
        addNotification({ title: 'Lỗi VNPay', subtitle: getErrorMessage(error, 'Không thể khởi tạo giao dịch VNPay'), color: 'error' });
    } finally {
        confirmDialog.value.loading = false;
    }
};

const onConfirmVnPayManual = async () => {
    vnpayDialog.value.loading = true;
    vnpayDialog.value.statusText = 'Đang xác nhận hóa đơn...';
    try {
        const txnNo = `VNP_MANUAL_${Date.now()}`;
        await finalizeVnPayCheckout(vnpayDialog.value.amount, txnNo, selectedOrder.value);
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể xác nhận thanh toán', color: 'error' });
        vnpayDialog.value.loading = false;
    }
};

// Logic: Thanh toán
// Handler chính cho nút "Thanh toán"
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
        vnpayChoiceDialog.value.show = true;
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
            try {
                const isCash = checkoutData.value.paymentMethod === 'CASH';
                const payload = {
                    idKhachHang: selectedOrder.value.idKhachHang,
                    idPhieuGiamGia: selectedOrder.value.idPhieuGiamGia,
                    tongTien: selectedOrder.value.tongTien,
                    tongTienSauGiam: selectedOrder.value.tongTienSauGiam,
                    loaiDon: 'TAI_QUAY',
                    ghiChu: checkoutData.value.note,
                    tienMat: isCash ? selectedOrder.value.tongTienSauGiam : 0,
                    tienChuyenKhoan: isCash ? 0 : selectedOrder.value.tongTienSauGiam
                };
                await submitCheckout({ payload });

                // Reset checkout data
                checkoutData.value.receivedAmount = 0;
                checkoutData.value.note = '';
                confirmDialog.value.show = false;
            } catch (e) {
                if (e.message !== 'RELOAD_REQUIRED') {
                    addNotification({ title: 'Thanh toán thất bại', subtitle: getErrorMessage(e, MESSAGES.ERROR.PAYMENT_FAILED), color: 'error' });
                }
            } finally {
                isProcessing.value = false;
                confirmDialog.value.loading = false;
            }
        }
    };
};

// Helpers
// Thay thế hóa đơn trong mảng bằng dữ liệu mới nhất
const updateOrderInList = (updated) => {
    const idx = orders.value.findIndex((o) => o.id === updated.id);
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

// Lắng nghe và xử lý khi backend trả về callback trên URL từ ngân hàng (VNPay Return)
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
        if (!isVnPayVerifySuccess(verifyResult, callbackParams)) {
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
                <OrderTabs class="flex-grow-1 min-w-0" :orders="orders" :active-index="activeOrderIndex"
                    @select="(idx) => (activeOrderIndex = idx)" @create="createNewOrder" @close="closeOrder" />
            </header>

            <v-row v-if="selectedOrder" no-gutters class="pos-grid">
                <!-- Left Column: Product Picker and Cart -->
                <v-col cols="12" xl="8" lg="8" md="8" class="d-flex flex-column gap-3 pr-3">
                    <div class="pos-column product-column" style="padding: 16px;">
                        <ProductPicker ref="productPickerRef" :active-order-id="selectedOrder.id" :orders="orders" :loading-external="isProcessing"
                            @add-product="onAddProduct" />
                    </div>
                    
                    <div class="pos-column cart-column" style="padding: 16px;">
                        <div class="column-head">
                            <div class="d-flex align-center gap-2">
                                <v-icon color="primary">mdi-cart-outline</v-icon>
                                <span class="text-subtitle-1 font-weight-bold text-slate-800">Giỏ hàng ({{ selectedOrderItemCount }} món)</span>
                            </div>
                            <v-btn variant="text" color="error" size="small" class="font-weight-bold px-0 text-none" @click="closeOrder(selectedOrder.id, activeOrderIndex)">Xoá tất cả</v-btn>
                        </div>
                        <CartTable :items="selectedOrder.listsHoaDonChiTiet" @update-qty="onUpdateQty"
                            @remove="onRemoveItem" />
                    </div>
                </v-col>

                <!-- Right Column: Customer and Checkout -->
                <v-col cols="12" xl="4" lg="4" md="4" class="pos-column payment-column" style="padding: 16px;">
                    <div class="customer-block mb-4 border-0 pa-0">
                        <CustomerSelector :selected-customer-name="selectedOrder.tenKhachHang"
                            :selected-customer-phone="selectedOrder.sdtKhachHang" :active-order-id="selectedOrder.id"
                            @select="onSelectCustomer" @remove="onRemoveCustomer" />
                    </div>

                    <CheckoutPanel :order="selectedOrder" :vouchers="vouchers" :checkout-data="checkoutData"
                        :loading="isProcessing" @apply-voucher="onApplyVoucher" @checkout="onCheckout" />
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

        <!-- VNPay QR Dialog -->
        <v-dialog v-model="vnpayDialog.show" max-width="450" persistent>
            <v-card class="rounded-xl overflow-hidden pb-4">
                <v-card-text class="pt-6 text-center d-flex flex-column align-center">
                    <div class="vnpay-logo-wrapper mb-4">
                        <v-img src="https://vnpay.vn/assets/images/logo-icon/logo-primary.svg" width="60" />
                    </div>
                    
                    <h3 class="text-h6 font-weight-bold mb-1">Thanh toán VNPay</h3>
                    <p class="text-subtitle-2 text-grey-darken-1 mb-6">Mã đơn: {{ vnpayDialog.orderId }}</p>

                    <div v-if="vnpayDialog.loading" class="pa-8 d-flex flex-column align-center">
                        <v-progress-circular indeterminate color="#005BAA" size="48" class="mb-4"></v-progress-circular>
                        <div class="text-body-2 font-weight-medium text-grey-darken-2">{{ vnpayDialog.statusText }}</div>
                    </div>

                    <div v-else-if="vnpayDialog.verified" class="pa-8 d-flex flex-column align-center">
                        <v-icon color="success" size="64" class="mb-4">mdi-check-circle</v-icon>
                        <div class="text-h6 font-weight-bold text-success mb-2">Giao dịch thành công!</div>
                        <div class="text-body-2 text-grey-darken-1">Đơn hàng đang được hoàn tất...</div>
                    </div>

                    <div v-else class="w-100 d-flex flex-column align-center">
                        <template v-if="checkoutData.vnpayMethod === 'QR'">
                            <div class="pa-2 bg-white rounded-lg elevation-2 mb-4 d-inline-block">
                                <v-img :src="vnpayDialog.qrUrl" width="220" height="220" />
                            </div>
                            <div class="text-h5 font-weight-bold text-error mb-1">
                                {{ new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(vnpayDialog.amount) }}
                            </div>
                            <div class="text-caption text-grey-darken-1 mb-6 px-4 text-center">
                                Sử dụng ứng dụng ngân hàng hoặc ví VNPay để quét mã.
                            </div>
                        </template>
                        <template v-else>
                            <div class="text-h5 font-weight-bold text-error mb-4">
                                {{ new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(vnpayDialog.amount) }}
                            </div>
                            <div class="text-caption text-grey-darken-1 mb-6 px-4 text-center">
                                Vui lòng hoàn tất thanh toán trên cửa sổ VNPay.
                            </div>
                            <v-btn color="#005BAA" class="mb-6 rounded-lg text-white font-weight-bold" @click="() => { vnpayPopup = window.open(vnpayDialog.paymentUrl, 'vnpay', 'width=800,height=600'); }">
                                Mở lại cổng thanh toán
                            </v-btn>
                        </template>

                        <v-btn block color="#005BAA" class="mb-3 rounded-lg text-white font-weight-bold" height="48" @click="onConfirmVnPayManual">
                            XÁC NHẬN ĐÃ NHẬN TIỀN
                        </v-btn>
                        
                        <v-btn v-if="checkoutData.vnpayMethod === 'QR'" block variant="outlined" color="grey-darken-1" class="rounded-lg font-weight-bold" height="48" @click="startVnPayFlow">
                            TẠO LẠI MÃ QR
                        </v-btn>

                        <v-btn variant="text" color="error" class="mt-4" size="small" @click="cancelVnPayFlow">
                            Hủy giao dịch
                        </v-btn>
                    </div>
                </v-card-text>
            </v-card>
        </v-dialog>

        <!-- VNPay Choice Dialog -->
        <v-dialog v-model="vnpayChoiceDialog.show" max-width="400" persistent>
            <v-card class="rounded-xl pa-5">
                <div class="text-center mb-5">
                    <div class="text-h6 font-weight-bold">Chọn hình thức thanh toán</div>
                </div>
                <v-radio-group v-model="vnpayChoiceDialog.method" column hide-details class="mb-5">
                    <v-radio value="QR" label="Thanh toán qua quét mã QR" color="#2E4E8E"></v-radio>
                    <v-radio value="GATEWAY" label="Nhập mã thẻ qua cổng VNPay" color="#2E4E8E"></v-radio>
                </v-radio-group>
                <div class="d-flex gap-3">
                    <v-btn class="flex-grow-1 rounded-lg font-weight-bold" variant="outlined" color="grey-darken-1" height="44" @click="vnpayChoiceDialog.show = false">
                        Hủy
                    </v-btn>
                    <v-btn class="flex-grow-1 rounded-lg font-weight-bold text-white" color="#4285F4" height="44" @click="proceedVnPayChoice">
                        Tiếp tục
                    </v-btn>
                </div>
            </v-card>
        </v-dialog>

        <!-- Confirmation Dialog -->
        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />

        <!-- Hóa đơn sau thanh toán -->
        <InvoiceReceiptDialog :show="receiptDialog.show" :receipt="receiptDialog" @close="onCloseReceipt" />
    </v-container>
</template>

<style scoped>
.pos-wrapper {
    background: #eef2f6;
    height: calc(100vh - 64px);
    overflow-y: auto;
}

.pos-wrapper :deep(.v-btn) {
    text-transform: none;
    letter-spacing: normal;
}

.pos-shell {
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
}

.pos-column {
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
    position: sticky;
    top: 16px;
    align-self: flex-start;
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

    0%,
    100% {
        opacity: 1;
        transform: scale(1);
    }

    50% {
        opacity: .5;
        transform: scale(0.95);
    }
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
