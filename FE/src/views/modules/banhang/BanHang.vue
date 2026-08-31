<script setup>
/**
 * Module: Bán hàng tại quầy (Admin/POS)
 * View: BanHang
 * Chức năng: Màn hình chính xử lý tạo hóa đơn, thêm sản phẩm, cập nhật số lượng,
 *            chọn khách hàng, áp dụng voucher, thanh toán bằng tiền mặt/chuyển khoản (VNPay),
 *            và in hóa đơn sau khi hoàn tất.
 */
import { ref, onMounted, onUnmounted, computed, watch } from 'vue';
import api from '@/services/apiService';
import { API_ADMIN } from '@/constants/apiPaths';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';
import { dichVuVnPay } from './composables/dichVuVnPay.js';
import { initializePendingOrders } from './posInitialization.js';
import { useNotifications } from '@/services/notificationService';
import { MESSAGES } from '@/constants/messages';
import { ORDER_TYPES, DELIVERY_METHODS } from '@/constants/appConstants';
import { useUIStore } from '@/stores/ui';
import { useAuthStore } from '@/stores/authStore';
import { useBanHangStore } from '@/stores/banHangStore';
import { useLocation } from '@/composables/useLocation';
import { useAddressMapping } from '@/composables/useAddressMapping';
import { useHoaDonPrinter } from '@/composables/useHoaDonPrinter';
import { formatNumberWithDots, parseNumberFromDots, formatDateTime } from '@/utils/formatters';
import { getBackendErrorMessage } from '@/utils/errorUtils';
import { useCustomerSelect } from './composables/useCustomerSelect';

// Import Components
import OrderTabs from './components/OrderTabs.vue';
import CartTable from './components/CartTable.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import InvoiceReceiptDialog from './components/InvoiceReceiptDialog.vue';
import ProductPicker from './components/ProductPicker.vue';
import CustomerAndShippingPanel from './components/CustomerAndShippingPanel.vue';
import OrderSummaryPanel from './components/OrderSummaryPanel.vue';
import PaymentPanel from './components/PaymentPanel.vue';
import GiaoCaModal from '@/components/common/GiaoCaModal.vue';
import VnPayDialogs from './components/VnPayDialogs.vue';
import ScannerDialog from './components/ScannerDialog.vue';
import QuickAddCustomerDialog from './components/QuickAddCustomerDialog.vue';
import BetterVoucherModal from './components/BetterVoucherModal.vue';
import VoucherIneligibleModal from './components/VoucherIneligibleModal.vue';
import { dichVuGiaoCa } from '@/services/admin/dichVuGiaoCa';
import { dichVuNhanVien } from '@/services/admin/dichVuNhanVien';
import { useRoleAccess } from '@/composables/useRoleAccess';

const { addNotification } = useNotifications();
const { printHoaDonById } = useHoaDonPrinter();
const uiStore = useUIStore();
const authStore = useAuthStore();
const { isAdmin, isStaff } = useRoleAccess();
const MAX_WAITING_ORDERS = 5;
const VNPAY_PENDING_KEY = 'aerostride_pos_vnpay_pending';
const POS_ACTIVE_ORDER_KEY = 'aerostride_pos_active_order_id';

// Giao Ca State
const showGiaoCaModal = ref(false);
const giaoCaModalMode = ref('open'); // 'open' or 'close'
const currentGiaoCa = ref(null);

const checkGiaoCa = async () => {
    try {
        const res = await dichVuGiaoCa.getCaHienTai();
        const data = res?.data || res;
        if (!data || !data.id) {
            currentGiaoCa.value = null;
        } else {
            currentGiaoCa.value = data;
        }
    } catch (e) {
        currentGiaoCa.value = null;
    }
};

const openMoCaModal = () => {
    giaoCaModalMode.value = 'open';
    showGiaoCaModal.value = true;
};

const openChotCaModal = async () => {
    await checkGiaoCa();
    giaoCaModalMode.value = 'close';
    showGiaoCaModal.value = true;
};

const handleGiaoCaSuccess = async () => {
    await checkGiaoCa();
    if (orders.value.length === 0) {
        await initializePendingOrders({
            fetchPendingOrders: () => dichVuDonHang.layDonHangCho(),
            setPendingOrders: setOrders,
            preferredOrderId: getStoredActiveOrderId()
        });
    }
};

// Address and Quick Add
const { provinces, districts, wards, loadingLocations, fetchProvinces, fetchDistricts, fetchWards, cleanName } = useLocation();
const { mapCodesToNames, parseAddressString } = useAddressMapping();

// Instance riêng cho địa chỉ nhận hàng: fallback giúp form vẫn chọn được khu vực khi GHN tạm lỗi.
const {
    provinces: provincesShip,
    districts: districtsShip,
    wards: wardsShip,
    fetchProvinces: fetchProvincesShip,
    fetchDistricts: fetchDistrictsShip,
    fetchWards: fetchWardsShip
} = useLocation({ allowFallback: true });

// State
const banHangStore = useBanHangStore();
const loading = ref(false);
const orders = ref(banHangStore.orders || []);
const activeOrderIndex = ref(banHangStore.activeOrderIndex || 0);
const vouchers = ref(banHangStore.vouchers || []);
const isProcessing = ref(false);

// Sync local reactive state with Pinia store
watch(
    orders,
    (val) => {
        banHangStore.orders = val;
    },
    { deep: true }
);

watch(
    activeOrderIndex,
    (val) => {
        banHangStore.activeOrderIndex = val;
    }
);

watch(
    vouchers,
    (val) => {
        banHangStore.vouchers = val;
    }
);

// Dynamic Filter States for POS Products
const maxProductPrice = ref(7000000);

// Right Column Fields
const currentEmployeeDetail = ref(banHangStore.employeeDetail || null);

watch(
    currentEmployeeDetail,
    (val) => {
        banHangStore.employeeDetail = val;
    }
);

const {
    customerSearch,
    customerResults,
    customerLoading,
    showCustomerSuggestions,
    customerForm,
    searchCustomers,
    selectCustomer,
    onCustomerInput,
    onSelectSuggestedCustomer,
    onCustomerFormUpdate,
    ensureCustomerAndGetId,
    onRemoveCustomer
} = useCustomerSelect(
    computed(() => orders.value[activeOrderIndex.value] || null),
    (updated) => updateOrderInList(updated),
    (order, autoApply) => refreshBestVoucher(order, autoApply),
    addNotification
);

const shippingAddressSelect = ref('Chọn địa chỉ');
const expectedDeliveryDate = ref('');
const recipientName = ref('');
const recipientPhone = ref('');
const recipientAddressDetail = ref('');
const recipientProvince = ref(null);
const recipientDistrict = ref(null);
const recipientWard = ref(null);
const syncingRecipientAddress = ref(false);
const shippingFeeLoading = ref(false);
const shippingFeeError = ref('');
const shippingFeeSource = ref('');

// Order Value Adjustments
// Phí vận chuyển bắt đầu từ 0, chỉ cập nhật khi GHN trả phí hợp lệ hoặc nhân viên nhập tay.
const shippingFee = ref(0);
const surcharge = ref(0);
const isFreeShip = ref(false);
const onlyChargeIfReturned = ref(false);
const chargeTax = ref(false);
const noteType = ref('NOI_BO');

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

// Product Suggestion State
const productSuggestions = ref([]);
const showProductSuggestions = ref(false);
const suggestionLoading = ref(false);

// Computed property for suggestion calculations
const suggestionData = computed(() => {
    if (!productSuggestions.value.length || !selectedOrder.value) return null;

    const suggestion = productSuggestions.value[0];
    const currentTotal = selectedOrder.value.tongTien || 0;
    const additionalAmount = suggestion.soTienCanThem || 0;
    const discountAmount = suggestion.soTienGiam || 0;
    const newTotal = currentTotal + additionalAmount - discountAmount;

    return {
        productCode: suggestion.maSanPham || 'VCHSXLBAP',
        productName: suggestion.tenSanPham || 'Sản phẩm gợi ý',
        discountPercent: suggestion.phanTramGiam || 70,
        needToBuy: additionalAmount,
        willReduce: discountAmount,
        newTotal: newTotal
    };
});

// Function to fetch product suggestions from database
const fetchProductSuggestions = async () => {
    if (!selectedOrder.value?.id) return;

    suggestionLoading.value = true;
    try {
        const res = await dichVuDonHang.getProductSuggestions(selectedOrder.value.id);
        productSuggestions.value = res || [];
        showProductSuggestions.value = productSuggestions.value.length > 0;
    } catch (e) {
        console.error('Lỗi khi tải gợi ý sản phẩm:', e);
        productSuggestions.value = [];
        showProductSuggestions.value = false;
    } finally {
        suggestionLoading.value = false;
    }
};

// Add suggested product to cart
const onAddSuggestedProduct = async () => {
    if (!suggestionData.value) return;

    try {
        const keyword = suggestionData.value.productCode;
        const variants = await dichVuDonHang.searchSanPham({ keyword });
        if (variants && variants.length > 0) {
            const normalizedKeyword = keyword.toLowerCase();
            const exactMatch =
                variants.find(
                    (v) =>
                        String(v.maChiTietSanPham || '')
                            .trim()
                            .toLowerCase() === normalizedKeyword
                ) || variants[0];
            if (exactMatch) {
                await onAddProduct({ ...exactMatch, _soLuongMuonThem: 1 });
                showProductSuggestions.value = false;
            }
        }
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể thêm sản phẩm gợi ý', color: 'error' });
    }
};

const proceedVnPayChoice = () => {
    vnpayChoiceDialog.value.show = false;
    checkoutData.value.vnpayMethod = vnpayChoiceDialog.value.method;
    startVnPayFlow();
};

const selectedOrder = computed(() => orders.value[activeOrderIndex.value] || null);
const selectedOrderItemCount = computed(() =>
    (selectedOrder.value?.listsHoaDonChiTiet || []).reduce((sum, item) => sum + (Number(item.soLuong) || 0), 0)
);

const orderChannel = computed({
    get() {
        if (!selectedOrder.value) return 'Tại quầy';
        return selectedOrder.value.isGiaoHangLocal ? 'Giao hàng' : 'Tại quầy';
    },
    set(newVal) {
        if (selectedOrder.value) {
            selectedOrder.value.isGiaoHangLocal = newVal === 'Giao hàng' || newVal === 'Trực tuyến';
            selectedOrder.value.orderType = ORDER_TYPES.IN_STORE;
            selectedOrder.value.deliveryMethod = selectedOrder.value.isGiaoHangLocal
                ? DELIVERY_METHODS.SHIPPING
                : DELIVERY_METHODS.TAKEAWAY;
            selectedOrder.value.loaiDon = selectedOrder.value.isGiaoHangLocal ? 'GIAO_HANG' : 'TAI_QUAY';
        }
    }
});

const isGiaoHang = computed({
    get() {
        return !!selectedOrder.value?.isGiaoHangLocal;
    },
    set(val) {
        if (selectedOrder.value) {
            selectedOrder.value.isGiaoHangLocal = val;
            if (val && (!provincesShip.value || provincesShip.value.length === 0)) {
                fetchProvincesShip();
            }
            selectedOrder.value.orderType = ORDER_TYPES.IN_STORE;
            selectedOrder.value.deliveryMethod = val ? DELIVERY_METHODS.SHIPPING : DELIVERY_METHODS.TAKEAWAY;
            selectedOrder.value.loaiDon = val ? 'GIAO_HANG' : 'TAI_QUAY';
            if (!val) {
                shippingFee.value = 0;
                shippingFeeError.value = '';
                shippingFeeSource.value = '';
            } else if (!Number(shippingFee.value || 0)) {
                void calculateShippingFee();
            }
        }
    }
});

// Order Value Calculations

// ==============================
// CÁC THÔNG SỐ TÍNH TOÁN (TỪ BACKEND)
// ==============================
// Tổng tiền hàng gốc
const originalTotalAmount = computed(() => {
    return Number(selectedOrder.value?.tongTienHang || 0);
});

// Alias cho UI
const totalRawAmount = computed(() => originalTotalAmount.value);

// Phần giảm từ đợt giảm giá sản phẩm
const productDiscountAmount = computed(() => {
    return Number(selectedOrder.value?.tienGiamGiaSanPham || 0);
});

// Tổng tiền sau giảm sản phẩm
const cartSubtotalAmount = computed(() => {
    return Number(selectedOrder.value?.tongTien || 0);
});

// Tiền giảm từ phiếu giảm giá (Voucher)
const discountAmount = computed(() => {
    return Number(selectedOrder.value?.tienGiamGiaPhieu || 0);
});

// Tiền sau khi áp dụng voucher
const amountAfterAllDiscounts = computed(() => {
    return Number(selectedOrder.value?.tongTienSauGiam || 0);
});

// Tổng toàn bộ tiền giảm (Sản phẩm + Voucher)
const totalDiscountAmount = computed(() => {
    return productDiscountAmount.value + discountAmount.value;
});

// Tổng thanh toán (bao gồm phí vận chuyển)
const finalCollectAmount = computed(() => {
    const afterDiscount = Number(selectedOrder.value?.tongTienSauGiam ?? selectedOrder.value?.tongTien ?? 0);
    const shipFee = isGiaoHang.value && !isFreeShip.value ? Number(shippingFee.value || 0) : 0;
    return Math.max(0, afterDiscount + shipFee);
});

const remainingBalance = computed(() => {
    const received = Number(checkoutData.value.receivedAmount || 0);
    if (received <= 0) return 0;
    return Math.max(0, finalCollectAmount.value - received);
});

const changeAmount = computed(() => {
    const received = Number(checkoutData.value.receivedAmount || 0);
    return Math.max(0, received - finalCollectAmount.value);
});

// Kiểm tra giỏ hàng có sản phẩm hợp lệ (có ít nhất 1 sản phẩm và tất cả số lượng > 0)
const hasValidCartItems = computed(() => {
    const items = selectedOrder.value?.listsHoaDonChiTiet;
    if (!items || !items.length) return false;
    const totalQty = items.reduce((sum, item) => sum + (Number(item.soLuong) || 0), 0);
    return totalQty > 0 && !items.some((item) => !item.soLuong || Number(item.soLuong) <= 0);
});

// deliveryMethod la nguon chinh; loaiDon chi dung de doc hoa don cu.
// Chuan hoa tai mot cho de refresh/cap nhat order khong lam cong tac bi tat sai.
const isShippingOrder = (order) =>
    order?.deliveryMethod
        ? order.deliveryMethod === DELIVERY_METHODS.SHIPPING
        : ['ONLINE', 'GIAO_HANG'].includes(String(order?.loaiDon || '').toUpperCase());
const normalizeSalesOrder = (order) => {
    if (!order) return order;
    return {
        ...order,
        orderType: ORDER_TYPES.IN_STORE,
        deliveryMethod: order.deliveryMethod || (isShippingOrder(order) ? DELIVERY_METHODS.SHIPPING : DELIVERY_METHODS.TAKEAWAY),
        isGiaoHangLocal: order.isGiaoHangLocal ?? isShippingOrder(order)
    };
};

// ==============================
// GỌI API ĐỒNG BỘ SHIPPING VÀ CHANNEL
// ==============================
let shippingSyncTimeout = null;
const syncShippingAndChannel = () => {
    if (!selectedOrder.value?.id) return;

    if (shippingSyncTimeout) clearTimeout(shippingSyncTimeout);

    shippingSyncTimeout = setTimeout(async () => {
        try {
            // Công tắc "Giao hàng" là nguồn trạng thái chính của loại đơn.
            // Không dùng biến phụ để tránh lúc vừa bật giao hàng bị sync nhầm về TẠI_QUẦY.
            const loaiDon = isGiaoHang.value ? 'GIAO_HANG' : 'TAI_QUAY';
            const deliveryMethod = isGiaoHang.value ? DELIVERY_METHODS.SHIPPING : DELIVERY_METHODS.TAKEAWAY;
            const shipFee = deliveryMethod === DELIVERY_METHODS.SHIPPING && !isFreeShip.value ? Number(shippingFee.value || 0) : 0;

            const updatedOrder = await dichVuDonHang.updateShippingAndChannel(selectedOrder.value.id, {
                orderType: ORDER_TYPES.IN_STORE,
                deliveryMethod,
                loaiDon: loaiDon,
                phiVanChuyen: shipFee
            });

            // Cập nhật lại state của đơn hàng hiện tại từ phản hồi của Backend
            updateOrderInList(updatedOrder);
        } catch (error) {
            console.error('Error syncing shipping/channel:', error);
            addNotification({
                title: 'Lỗi giao hàng',
                subtitle: 'Không thể đồng bộ phí vận chuyển, vui lòng thử lại.',
                color: 'error'
            });
        }
    }, 500); // Debounce 500ms
};

// Sync shipping fee when channel or free ship changes
const lastActiveOrderId = ref(selectedOrder.value?.id || null);

// Watcher when active order changes, update note, shipping fee and reset search
watch(
    () => selectedOrder.value?.id,
    (id) => {
        checkoutData.value.note = selectedOrder.value?.ghiChu || '';

        if (id) {
            if (
                selectedOrder.value.phiVanChuyen !== undefined &&
                selectedOrder.value.phiVanChuyen !== null &&
                Number(selectedOrder.value.phiVanChuyen) > 0
            ) {
                shippingFee.value = Number(selectedOrder.value.phiVanChuyen);
            } else {
                shippingFee.value = 0;
            }
            isFreeShip.value = false;

            onlyChargeIfReturned.value = !isShippingOrder(selectedOrder.value);
        }
    }
);

// Sync shipping fee when channel or free ship changes
watch(
    [orderChannel, isFreeShip],
    ([channel, freeShip], oldVal) => {
        const currentId = selectedOrder.value?.id || null;
        if (currentId !== lastActiveOrderId.value) {
            // Just switched tabs/orders; do not reset shippingFee. Update tracking ID.
            lastActiveOrderId.value = currentId;
            return;
        }

        const oldFreeShip = oldVal ? oldVal[1] : undefined;
        const oldChannel = oldVal ? oldVal[0] : undefined;

        if (freeShip) {
            shippingFee.value = 0;
            shippingFeeError.value = '';
            shippingFeeSource.value = '';
        } else {
            if (channel === 'Giao hàng' && (oldFreeShip === true || channel !== oldChannel)) {
                void calculateShippingFee();
            } else if (channel !== oldChannel && oldChannel !== undefined) {
                shippingFee.value = 0;
                shippingFeeError.value = '';
                shippingFeeSource.value = '';
            }
        }
    },
    { immediate: true }
);

// Automatically uncheck Free Ship if a non-zero shipping fee is typed/selected
watch(shippingFee, (newVal) => {
    if (Number(newVal) > 0) {
        isFreeShip.value = false;
    }
    syncShippingAndChannel();
});

// Sync onlyChargeIfReturned when channel changes
watch(orderChannel, (channel) => {
    onlyChargeIfReturned.value = channel === 'Tại quầy';
    syncShippingAndChannel();
});

// Automatically sync received amount with total payable amount when VNPay is selected
watch(
    () => [checkoutData.value.paymentMethod, finalCollectAmount.value],
    ([method, amount]) => {
        if (method === 'VNPAY') {
            checkoutData.value.receivedAmount = Number(amount);
        }
    },
    { immediate: true }
);

const voucherSuggestionText = computed(() => {
    return selectedOrder.value?.voucherSuggestionText || '';
});

const isVoucherAutoApplied = ref({});

const showQuickAddDialog = ref(false);
const quickAddInitialData = ref({ ten: '', sdt: '', email: '' });

const handleOpenQuickAdd = (data) => {
    quickAddInitialData.value = data || { ten: '', sdt: '', email: '' };
    showQuickAddDialog.value = true;
};

const onQuickAddSuccess = async (customer) => {
    if (customer) {
        await selectCustomer(customer);
    }
};

let shipFeeCalcTimeout = null;
const debouncedCalculateShippingFee = () => {
    if (shipFeeCalcTimeout) clearTimeout(shipFeeCalcTimeout);
    shipFeeCalcTimeout = setTimeout(() => {
        void calculateShippingFee();
    }, 250);
};

// Nhận dữ liệu địa chỉ giao hàng từ panel bên phải
const onShippingPanelUpdate = (shipping) => {
    const next = shipping || {};
    recipientName.value = next.name || '';
    recipientPhone.value = next.phone || '';
    recipientAddressDetail.value = next.detail || '';
    recipientProvince.value = next.province || null;
    recipientDistrict.value = next.district || null;
    recipientWard.value = next.ward || null;

    if (isGiaoHang.value && recipientDistrict.value && recipientWard.value) {
        debouncedCalculateShippingFee();
    }
};

const openDatePicker = (event) => {
    const el = event.target.closest('.v-input');
    const input = el ? el.querySelector('input[type="date"]') : null;
    if (input && typeof input.showPicker === 'function') {
        try {
            input.showPicker();
        } catch (e) {
            console.error(e);
        }
    }
};

// onRemoveCustomer is imported from useCustomerSelect

// Chuẩn hóa định dạng danh sách đơn hàng trả về từ nhiều dạng API response khác nhau
const normalizeOrderList = (payload) => {
    if (Array.isArray(payload)) return payload.map(normalizeSalesOrder);
    if (Array.isArray(payload?.content)) return payload.content.map(normalizeSalesOrder);
    if (Array.isArray(payload?.data)) return payload.data.map(normalizeSalesOrder);
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

// localStorage chỉ giữ ID tab đang mở; dữ liệu hóa đơn luôn được khôi phục từ backend.
const getStoredActiveOrderId = () => {
    try {
        return localStorage.getItem(POS_ACTIVE_ORDER_KEY);
    } catch (e) {
        return null;
    }
};

watch(
    () => selectedOrder.value?.id,
    (id, oldId) => {
        if (oldId && oldId !== id) {
            flushPendingQtyUpdates();
        }
        try {
            if (id) localStorage.setItem(POS_ACTIVE_ORDER_KEY, id);
            else localStorage.removeItem(POS_ACTIVE_ORDER_KEY);
        } catch (e) {
            // Trình duyệt có thể chặn localStorage; việc khôi phục danh sách vẫn dựa vào backend.
        }
    }
);

// QR / Barcode Scanner Logic
const showScanner = ref(false);
let html5QrcodeScanner = null;
const scannerElementId = 'ban-hang-qr-reader';

const startScanner = () => {
    showScanner.value = true;
    setTimeout(async () => {
        const el = document.getElementById(scannerElementId);
        if (!el || el.clientWidth === 0) {
            if (showScanner.value) startScanner();
            return;
        }

        if (html5QrcodeScanner) {
            await stopScanner(false);
        }

        try {
            // Mở camera trực tiếp để thu ngân thấy hình camera ngay trong popup quét mã.
            html5QrcodeScanner = new Html5Qrcode(scannerElementId);
            await html5QrcodeScanner.start(
                { facingMode: 'environment' },
                { fps: 10, qrbox: { width: 250, height: 250 } },
                onScanSuccess,
                onScanFailure
            );
        } catch (error) {
            console.error('Camera start error:', error);
            addNotification({
                title: 'Không mở được camera',
                subtitle: 'Vui lòng cấp quyền camera cho trình duyệt hoặc kiểm tra camera đang bị ứng dụng khác dùng.',
                color: 'error'
            });
            await stopScanner();
        }
    }, 150);
};

const stopScanner = async (closeDialog = true) => {
    if (html5QrcodeScanner) {
        try {
            await html5QrcodeScanner.stop();
            await html5QrcodeScanner.clear();
        } catch (error) {
            console.error('Failed to stop scanner', error);
        }
        html5QrcodeScanner = null;
    }
    if (closeDialog) showScanner.value = false;
};

const onScanSuccess = async (decodedText) => {
    await stopScanner();
    let keyword = decodedText?.trim();
    if (!keyword) return;

    // Phân tích nếu chuỗi quét được là JSON hoặc chứa định dạng đặc biệt
    try {
        if (keyword.startsWith('{') && keyword.endsWith('}')) {
            const parsed = JSON.parse(keyword);
            keyword = parsed.maChiTietSanPham || parsed.ma || parsed.id || keyword;
        }
    } catch (e) {
        // chuỗi text thông thường
    }

    if (!selectedOrder.value) {
        addNotification({ title: 'Chưa có hóa đơn', subtitle: 'Vui lòng tạo hoặc chọn hóa đơn trước khi quét mã.', color: 'warning' });
        return;
    }

    try {
        const variants = await dichVuDonHang.searchSanPham({ keyword });
        if (variants && variants.length > 0) {
            const normalizedKeyword = keyword.toLowerCase();
            const exactMatch =
                variants.find(
                    (v) =>
                        String(v.maChiTietSanPham || '')
                            .trim()
                            .toLowerCase() === normalizedKeyword ||
                        String(v.id || '')
                            .trim()
                            .toLowerCase() === normalizedKeyword ||
                        String(v.maSanPham || '')
                            .trim()
                            .toLowerCase() === normalizedKeyword
                ) || variants[0];

            if (exactMatch) {
                if (exactMatch.trangThai !== undefined && !isActiveStatus(exactMatch.trangThai)) {
                    addNotification({ title: 'Thất bại', subtitle: 'Sản phẩm đã ngừng bán', color: 'error' });
                    return;
                }
                const currentStock = Number(exactMatch.soLuongTon ?? exactMatch.soLuong ?? 0);
                if (currentStock <= 0) {
                    addNotification({ title: 'Thất bại', subtitle: 'Sản phẩm đã hết hàng trong kho', color: 'error' });
                    return;
                }

                await onAddProduct({ ...exactMatch, _soLuongMuonThem: 1 });
            }
        } else {
            addNotification({ title: 'Không tìm thấy', subtitle: `Không tìm thấy sản phẩm có mã [${keyword}]`, color: 'warning' });
        }
    } catch (e) {
        console.error('Scan error:', e);
    }
};

const onScanFile = async (file) => {
    if (!file) return;
    try {
        if (!html5QrcodeScanner) {
            html5QrcodeScanner = new Html5Qrcode(scannerElementId);
        }
        if (html5QrcodeScanner.isScanning) {
            try {
                await html5QrcodeScanner.stop();
            } catch (e) {}
        }
        const decodedText = await html5QrcodeScanner.scanFile(file, true);
        if (decodedText) {
            onScanSuccess(decodedText);
        }
    } catch (err) {
        console.error('Lỗi quét file ảnh QR:', err);
        addNotification({
            title: 'Không nhận diện được QR',
            subtitle: 'Không thể tìm thấy mã QR trong file ảnh đã chọn. Vui lòng thử ảnh rõ nét hơn.',
            color: 'warning'
        });
    }
};

const onScanFailure = (error) => {
    // Console error ignored
};

// Global keydown handler for F9, F10 and barcode scanners
let barcodeBuffer = '';
let lastKeyTime = 0;

const handleGlobalKeyDown = (e) => {
    if (e.key === 'F10') {
        e.preventDefault();
        onCheckout();
    }
    if (e.key === 'F9') {
        e.preventDefault();
        startScanner();
    }

    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;

    const currentTime = new Date().getTime();
    if (currentTime - lastKeyTime > 100) {
        barcodeBuffer = '';
    }

    if (e.key === 'Enter') {
        if (barcodeBuffer.length > 3) {
            onScanSuccess(barcodeBuffer);
            barcodeBuffer = '';
        }
    } else if (e.key.length === 1) {
        barcodeBuffer += e.key;
    }
    lastKeyTime = currentTime;
};

const loadCurrentEmployeeDetails = async () => {
    if (banHangStore.employeeDetail) {
        currentEmployeeDetail.value = banHangStore.employeeDetail;
        return;
    }
    try {
        if (authStore.user?.username) {
            const res = await dichVuNhanVien.layThongTinCaNhan();
            if (res) {
                currentEmployeeDetail.value = res;
                banHangStore.employeeDetail = res;
            }
        }
    } catch (e) {
        console.error('Error fetching employee detail:', e);
    }
};

// Lifecycle
onMounted(async () => {
    uiStore.setBreadcrumbs([
        { title: 'Bán hàng', disabled: false, href: '/admin/ban-hang' },
        { title: 'Tạo đơn hàng', disabled: true }
    ]);
    window.addEventListener('keydown', handleGlobalKeyDown);

    const hasCache = banHangStore.isInitialized && orders.value.length > 0;
    if (!hasCache) {
        loading.value = true;
    }

    try {
        // Tải song song tất cả các dữ liệu khởi tạo cốt lõi
        const [shiftRes, empRes, pendingOrdersRes] = await Promise.allSettled([
            checkGiaoCa(),
            loadCurrentEmployeeDetails(),
            initializePendingOrders({
                fetchPendingOrders: () => dichVuDonHang.layDonHangCho(),
                setPendingOrders: setOrders,
                preferredOrderId: getStoredActiveOrderId()
            })
        ]);

        if (isStaff.value && !currentGiaoCa.value) {
            // Nhân viên chưa mở ca: Mở modal mở ca để nhân viên nhập tiền đầu két
            openMoCaModal();
        }

        banHangStore.isInitialized = true;
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: getErrorMessage(error, MESSAGES.ERROR.CONNECT_SERVER), color: 'error' });
    } finally {
        loading.value = false;
    }

    // Tác vụ nền chạy sau khi UI đã sẵn sàng
    try {
        refreshBestVoucher();
        handleVnPayCallbackFromUrl();

        // Tải trước danh mục tỉnh thành sau khi màn hình POS đã hiển thị mượt mà
        setTimeout(() => {
            if (!provincesShip.value || provincesShip.value.length === 0) {
                fetchProvincesShip();
            }
        }, 500);
    } catch (e) {
        console.error('Lỗi khi tải dữ liệu phụ:', e);
    }
});

onUnmounted(() => {
    window.removeEventListener('keydown', handleGlobalKeyDown);
    stopScanner();
    flushPendingQtyUpdates();
});

// Customer shipping address restore helpers.
// Principle: customer data can come as split fields or one full address string, so normalize first, then match to the current location list.
const getAddressDetailValue = (address) =>
    address?.diaChiChiTiet || address?.diaChiCuThe || address?.diaChiNguoiNhan || address?.diaChi || '';

const getAddressFullText = (address) => {
    if (!address) return '';
    if (address.diaChiDayDu) return address.diaChiDayDu;
    if (address.diaChiNguoiNhan) return address.diaChiNguoiNhan;
    if (address.diaChi && String(address.diaChi).includes(',')) return address.diaChi;
    return [address.diaChiChiTiet, address.phuongXa, address.thanhPho, address.tinh].filter(Boolean).join(', ');
};

const normalizeAddressRecord = (address) => {
    if (!address) return null;
    const normalized = { ...address };
    const fullText = getAddressFullText(normalized);
    const needsParse = !normalized.tinh || !normalized.thanhPho || !normalized.phuongXa || !normalized.diaChiChiTiet;

    if (needsParse && fullText) {
        const parsed = parseAddressString(fullText);
        normalized.tinh = normalized.tinh || parsed.tinh;
        normalized.thanhPho = normalized.thanhPho || parsed.thanhPho;
        normalized.phuongXa = normalized.phuongXa || parsed.phuongXa;
        normalized.diaChiChiTiet = normalized.diaChiChiTiet || parsed.diaChiChiTiet;
    }

    if (!normalized.diaChiChiTiet && getAddressDetailValue(normalized)) {
        normalized.diaChiChiTiet = getAddressDetailValue(normalized);
    }

    return normalized;
};

const normalizeAddressList = (source) => {
    const raw = source?.data?.data || source?.data || source || [];
    const list = Array.isArray(raw) ? raw : [];
    return list.map(normalizeAddressRecord).filter(Boolean);
};

const pickDefaultCustomerAddress = (customer, addrRes) => {
    const addresses = [
        ...normalizeAddressList(addrRes),
        ...normalizeAddressList(customer?.diaChis || customer?.listDiaChi || customer?.addresses),
        normalizeAddressRecord(customer?.diaChiMacDinh),
        normalizeAddressRecord(customer)
    ].filter(Boolean);

    if (!addresses.length) return null;
    return (
        addresses.find((address) => address.laMacDinh === true || address.macDinh === true) ||
        addresses.find((address) => address.trangThai === true || address.trangThai === 1 || address.trangThai === 'DANG_HOAT_DONG') ||
        addresses[0]
    );
};

const matchShippingLocation = (list, name) => {
    if (!name) return null;
    const cleanTarget = cleanName(name);
    return (
        list.find((item) => {
            const cleanItem = cleanName(item.name);
            return cleanItem === cleanTarget || cleanItem.includes(cleanTarget) || cleanTarget.includes(cleanItem);
        }) || null
    );
};

const resetRecipientAddress = () => {
    recipientName.value = '';
    recipientPhone.value = '';
    recipientAddressDetail.value = '';
    recipientProvince.value = null;
    recipientDistrict.value = null;
    recipientWard.value = null;
    shippingFee.value = 0;
    shippingFeeError.value = '';
    shippingFeeSource.value = '';
};

const applyDefaultAddressToRecipient = async (address) => {
    const normalized = normalizeAddressRecord(address);
    if (!normalized) return;

    syncingRecipientAddress.value = true;
    shippingFeeError.value = '';
    try {
        recipientAddressDetail.value = normalized.diaChiChiTiet || getAddressDetailValue(normalized) || '';
        await fetchProvincesShip();

        const provinceMatch = matchShippingLocation(provincesShip.value, normalized.tinh);
        recipientProvince.value = provinceMatch?.code || null;
        if (!provinceMatch) {
            shippingFee.value = 0;
            if (normalized.tinh && normalized.tinh.trim()) {
                shippingFeeError.value = 'Địa chỉ mặc định của khách hàng chưa khớp GHN. Vui lòng chọn lại Tỉnh/Thành.';
            } else {
                shippingFeeError.value = '';
            }
            shippingFeeSource.value = '';
            return;
        }

        await fetchDistrictsShip(provinceMatch.code);
        const districtMatch = matchShippingLocation(districtsShip.value, normalized.thanhPho);
        recipientDistrict.value = districtMatch?.code || null;
        if (!districtMatch) {
            shippingFee.value = 0;
            if (normalized.thanhPho && normalized.thanhPho.trim()) {
                shippingFeeError.value = 'Quận/Huyện của khách chưa khớp GHN. Vui lòng chọn lại Quận/Huyện.';
            } else {
                shippingFeeError.value = '';
            }
            shippingFeeSource.value = '';
            return;
        }

        await fetchWardsShip(districtMatch.code);
        const wardMatch = matchShippingLocation(wardsShip.value, normalized.phuongXa);
        recipientWard.value = wardMatch?.code || null;
        if (!wardMatch) {
            shippingFee.value = 0;
            if (normalized.phuongXa && normalized.phuongXa.trim()) {
                shippingFeeError.value = 'Phường/Xã của khách chưa khớp GHN. Vui lòng chọn lại Phường/Xã.';
            } else {
                shippingFeeError.value = '';
            }
            shippingFeeSource.value = '';
            return;
        }

        shippingFeeError.value = '';
    } finally {
        syncingRecipientAddress.value = false;
    }

    await calculateShippingFee();
};

const syncRecipientFromCustomer = async (customer, addrRes = []) => {
    recipientName.value = customer?.ten || selectedOrder.value?.tenKhachHang || '';
    recipientPhone.value = customer?.sdt || selectedOrder.value?.sdtKhachHang || '';

    const defaultAddress = pickDefaultCustomerAddress(customer, addrRes);
    if (defaultAddress) {
        await applyDefaultAddressToRecipient(defaultAddress);
    } else {
        recipientAddressDetail.value = '';
        recipientProvince.value = null;
        recipientDistrict.value = null;
        recipientWard.value = null;
        shippingFee.value = 0;
        shippingFeeError.value = 'Khách hàng chưa có địa chỉ nhận hàng mặc định.';
        shippingFeeSource.value = '';
    }
};

// Keep recipient name/phone aligned with the selected customer while staff edits the customer card.
watch(
    () => [customerForm.value.ten, customerForm.value.sdt],
    ([name, phone]) => {
        if (!selectedOrder.value?.idKhachHang) return;
        recipientName.value = name || recipientName.value;
        recipientPhone.value = phone || recipientPhone.value;
    }
);

// Tải lại danh sách voucher khi đổi hóa đơn hoặc khách hàng
const voucherRealtimeKey = computed(() => {
    const order = selectedOrder.value;
    if (!order?.id) return '';
    return `${order.id}|${order.idKhachHang || ''}`;
});

watch(
    voucherRealtimeKey,
    async (key) => {
        if (key) await refreshBestVoucher();
    },
    { flush: 'post' }
);

// Watch for order changes to fetch product suggestions
watch(
    () => [selectedOrder.value?.id, selectedOrder.value?.tongTien],
    ([id, total]) => {
        if (id && total > 0) {
            fetchProductSuggestions();
        } else {
            productSuggestions.value = [];
            showProductSuggestions.value = false;
        }
    }
);

// Watch selected customer id and fetch full details to populate the right column form
watch(
    () => selectedOrder.value?.idKhachHang,
    async (newId) => {
        if (newId) {
            try {
                const customer = await dichVuKhachHang.layChiTietKhachHang(newId);
                if (customer) {
                    customerForm.value = {
                        ten: customer.ten || '',
                        sdt: customer.sdt || '',
                        email: customer.email || '',
                        gioiTinh: customer.gioiTinh === true ? 'Nam' : customer.gioiTinh === false ? 'Nữ' : 'Khác',
                        ngaySinh: customer.ngaySinh || '',
                        tongDonHang: customer.tongDonHang || 0
                    };

                    // Restore recipient info from the customer's default address before GHN fee calculation.
                    try {
                        const addrRes = await dichVuKhachHang.layDanhSachDiaChi(newId);
                        await syncRecipientFromCustomer(customer, addrRes);
                    } catch (addrErr) {
                        console.error('Sync customer shipping address failed', addrErr);
                        await syncRecipientFromCustomer(customer, []);
                    }
                    return;
                }
            } catch (e) {
                console.error('Lấy chi tiết khách hàng thất bại', e);
            }
            customerForm.value = {
                ten: selectedOrder.value.tenKhachHang || '',
                sdt: selectedOrder.value.sdtKhachHang || '',
                email: selectedOrder.value.emailKhachHang || '',
                gioiTinh: 'Giới tính',
                ngaySinh: '',
                tongDonHang: 0
            };
        } else {
            customerForm.value = {
                ten: '',
                sdt: '',
                email: '',
                gioiTinh: 'Giới tính',
                ngaySinh: '',
                tongDonHang: 0
            };
            resetRecipientAddress();
        }
    },
    { immediate: true }
);

// Watchers for 3-level shipping address selection. Khi đồng bộ từ khách hàng thì không reset dây chuyền.
watch(
    () => recipientProvince.value,
    async (newVal) => {
        if (syncingRecipientAddress.value) return;
        recipientDistrict.value = null;
        recipientWard.value = null;
        shippingFee.value = 0;
        shippingFeeSource.value = '';
        if (newVal) {
            await fetchDistrictsShip(newVal);
        }
    }
);

watch(
    () => recipientDistrict.value,
    async (newVal) => {
        if (syncingRecipientAddress.value) return;
        recipientWard.value = null;
        shippingFee.value = 0;
        shippingFeeSource.value = '';
        if (newVal) {
            await fetchWardsShip(newVal);
        }
    }
);

watch(
    () => recipientWard.value,
    async (newVal) => {
        if (syncingRecipientAddress.value) return;
        if (newVal && isGiaoHang.value && recipientDistrict.value) {
            await calculateShippingFee();
        }
    }
);

const isGhnLocationCode = (list, code) => {
    if (!code) return false;
    if (!list || list.length === 0) return true;
    const found = list.find((item) => String(item.code) === String(code));
    return !found || found.source === 'GHN';
};

async function calculateShippingFee() {
    if (!isGiaoHang.value || isFreeShip.value) return;
    if (!recipientDistrict.value || !recipientWard.value) {
        shippingFee.value = 30000;
        shippingFeeSource.value = 'FALLBACK';
        shippingFeeError.value = '';
        return;
    }

    if (!isGhnLocationCode(districtsShip.value, recipientDistrict.value) || !isGhnLocationCode(wardsShip.value, recipientWard.value)) {
        shippingFee.value = 30000;
        shippingFeeSource.value = 'FALLBACK';
        shippingFeeError.value = '';
        return;
    }

    shippingFeeLoading.value = true;
    shippingFeeError.value = '';
    const weight = Math.max(200, 200 * (selectedOrderItemCount.value || 1));
    try {
        const res = await api.get(`${API_ADMIN.GHN}/fee`, {
            params: {
                toDistrictId: recipientDistrict.value,
                toWardCode: recipientWard.value,
                weight
            },
            silent: true
        });
        const total = Number(res?.data?.data?.total || res?.data?.total || res?.data?.data || 0);
        if (total > 0) {
            shippingFee.value = total;
            shippingFeeSource.value = 'GHN';
            shippingFeeError.value = '';
        } else {
            shippingFee.value = 30000;
            shippingFeeSource.value = 'FALLBACK';
            shippingFeeError.value = '';
        }
    } catch (e) {
        console.error('Failed to calculate shipping fee, fallback to 30.000đ', e);
        shippingFee.value = 30000;
        shippingFeeSource.value = 'FALLBACK';
        shippingFeeError.value = '';
    } finally {
        shippingFeeLoading.value = false;
    }
}

// Tự động tính lại phí ship GHN khi số lượng sản phẩm hoặc địa chỉ thay đổi
watch([selectedOrderItemCount, () => recipientWard.value, () => recipientDistrict.value, isGiaoHang], () => {
    if (isGiaoHang.value && recipientWard.value && recipientDistrict.value && !isFreeShip.value) {
        debouncedCalculateShippingFee();
    }
});

// Logic: Hóa đơn
const createNewOrder = async ({ silent = false, force = false } = {}) => {
    if (isStaff.value && !currentGiaoCa.value) {
        openMoCaModal();
        if (!silent) {
            addNotification({
                title: 'Chưa mở ca làm việc',
                subtitle: 'Vui lòng mở ca làm việc trước khi tạo hóa đơn.',
                color: 'warning'
            });
        }
        return;
    }
    if (isProcessing.value && !force) return;
    if (orders.value.length >= MAX_WAITING_ORDERS) {
        if (!silent) {
            addNotification({
                title: 'Giới hạn hóa đơn chờ',
                subtitle: `Backend hiện chỉ cho mở tối đa ${MAX_WAITING_ORDERS} hóa đơn chờ cùng lúc.`,
                color: 'warning'
            });
        }
        return;
    }
    const previousProcessing = isProcessing.value;
    isProcessing.value = true;
    try {
        const newOrder = await dichVuDonHang.taoDonHang();
        orders.value.push(normalizeSalesOrder(newOrder));
        activeOrderIndex.value = orders.value.length - 1;
    } catch (e) {
        console.error('Lỗi tạo hóa đơn:', e.response?.data || e.message || e);
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
        color: 'primary',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                for (const [key, entry] of Array.from(pendingQtyUpdates.entries())) {
                    if (entry.orderId === orderId) {
                        clearTimeout(entry.timer);
                        pendingQtyUpdates.delete(key);
                    }
                }
                await dichVuDonHang.xoaDonHang(orderId);
                orders.value.splice(index, 1);
                clampActiveOrderIndex();
                confirmDialog.value.show = false;
            } catch (e) {
                addNotification({ title: 'Lỗi', subtitle: MESSAGES.ERROR.DELETE_DATA, color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

// Logic: Sản phẩm (Optimistic & Zero-latency Instant Cart Addition)
const onAddProduct = async (product) => {
    if (!selectedOrder.value) {
        addNotification({ title: 'Cảnh báo', subtitle: 'Vui lòng chọn hoặc tạo hóa đơn trước.', color: 'warning' });
        return;
    }

    const currentOrder = selectedOrder.value;
    const qtyToAdd = Number(product._soLuongMuonThem) || 1;
    const currentStock = Number(product.soLuongTon ?? product.soLuong ?? 0);

    if (currentStock <= 0) {
        addNotification({ title: 'Hết hàng', subtitle: 'Sản phẩm đã hết hàng trong kho.', color: 'warning' });
        return;
    }

    if (!Array.isArray(currentOrder.listsHoaDonChiTiet)) {
        currentOrder.listsHoaDonChiTiet = [];
    }

    // Check if variant already in cart
    const existingIndex = currentOrder.listsHoaDonChiTiet.findIndex(
        (it) =>
            (it.idChiTietSanPham && it.idChiTietSanPham === product.id) ||
            it.id === product.id ||
            (it.maChiTietSanPham && it.maChiTietSanPham === product.maChiTietSanPham)
    );

    let prevQty = 0;
    let prevTotal = 0;
    let optimisticId = null;

    if (existingIndex !== -1) {
        const item = currentOrder.listsHoaDonChiTiet[existingIndex];
        prevQty = item.soLuong;
        prevTotal = item.thanhTien;
        item.soLuong += qtyToAdd;
        const unitPrice = Number(item.donGia) || Number(product.giaBan) || 0;
        item.thanhTien = item.soLuong * unitPrice;
        if (item.soLuongTon !== undefined) {
            item.soLuongTon = Math.max(0, Number(item.soLuongTon) - qtyToAdd);
        }
    } else {
        optimisticId = `opt_${Date.now()}_${product.id}`;
        const unitPrice = Number(product.giaBan) || 0;
        const optimisticItem = {
            id: optimisticId,
            idChiTietSanPham: product.id,
            maChiTietSanPham: product.maChiTietSanPham || '',
            maSanPham: product.maSanPham || '',
            tenSanPham: product.tenSanPham || '',
            tenMauSac: product.tenMauSac || '',
            tenKichThuoc: product.tenKichThuoc || '',
            soLuong: qtyToAdd,
            donGia: unitPrice,
            giaBan: unitPrice,
            thanhTien: qtyToAdd * unitPrice,
            hinhAnh: product.hinhAnh || '',
            phanTramGiam: Number(product.phanTramGiam) || 0,
            soLuongTon: Math.max(0, currentStock - qtyToAdd),
            _isOptimistic: true
        };
        currentOrder.listsHoaDonChiTiet.unshift(optimisticItem);
    }

    // Cập nhật tổng tiền đơn hàng và tồn kho tức thì
    currentOrder.tongTien = currentOrder.listsHoaDonChiTiet.reduce((sum, it) => sum + (Number(it.thanhTien) || 0), 0);
    banHangStore.updateProductStock(product.id, Math.max(0, currentStock - qtyToAdd));

    // Đồng bộ với backend qua background request không làm đơ giao diện
    try {
        const updated = await dichVuDonHang.addSanPham(currentOrder.id, {
            idChiTietSanPham: product.id,
            soLuong: qtyToAdd
        });
        updateOrderInList(updated);

        if (updated.priceChanged) {
            addNotification({ title: 'Giá sản phẩm thay đổi', subtitle: updated.priceChangeMessage, color: 'warning' });
        }
    } catch (e) {
        // Rollback nếu backend từ chối
        if (existingIndex !== -1 && currentOrder.listsHoaDonChiTiet[existingIndex]) {
            currentOrder.listsHoaDonChiTiet[existingIndex].soLuong = prevQty;
            currentOrder.listsHoaDonChiTiet[existingIndex].thanhTien = prevTotal;
        } else if (optimisticId) {
            const idx = currentOrder.listsHoaDonChiTiet.findIndex((it) => it.id === optimisticId);
            if (idx !== -1) currentOrder.listsHoaDonChiTiet.splice(idx, 1);
        }
        currentOrder.tongTien = currentOrder.listsHoaDonChiTiet.reduce((sum, it) => sum + (Number(it.thanhTien) || 0), 0);
        banHangStore.updateProductStock(product.id, currentStock);

        const errorMsg = e.response?.data?.message || MESSAGES.ERROR.PRODUCT_OUT_OF_STOCK;
        addNotification({ title: 'Lỗi', subtitle: errorMsg, color: 'error' });
    }
};

// Pending debounced quantity updates per cart item: key = `${orderId}_${itemId}`
const pendingQtyUpdates = new Map();

const flushPendingQtyUpdates = async () => {
    if (pendingQtyUpdates.size === 0) return;
    const entries = Array.from(pendingQtyUpdates.entries());
    const promises = [];
    for (const [key, entry] of entries) {
        clearTimeout(entry.timer);
        pendingQtyUpdates.delete(key);
        promises.push(
            dichVuDonHang
                .updateSoLuong(entry.orderId, entry.itemId, entry.targetQty)
                .then((updated) => {
                    updateOrderInList(updated);
                })
                .catch((e) => {
                    const item = selectedOrder.value?.listsHoaDonChiTiet?.find((it) => it.id === entry.itemId);
                    if (item) {
                        item.soLuong = entry.originalQty;
                        item.thanhTien = entry.originalQty * (Number(item.donGia) || 0);
                    }
                    const errorMsg = e.response?.data?.message || MESSAGES.ERROR.PRODUCT_OUT_OF_STOCK;
                    addNotification({ title: 'Lỗi', subtitle: errorMsg, color: 'error' });
                })
        );
    }
    await Promise.allSettled(promises);
};

const onUpdateQty = async (item, delta, inputEventTarget = null) => {
    if (!selectedOrder.value || !item) return;

    const currentOrderId = selectedOrder.value.id;
    const updateKey = `${currentOrderId}_${item.id}`;
    const existing = pendingQtyUpdates.get(updateKey);

    const originalQty = existing ? existing.originalQty : item.soLuong;
    const originalStock = existing ? existing.originalStock : (item.soLuongTon || 0);
    const currentOptimisticQty = item.soLuong;
    let newQty = currentOptimisticQty + delta;

    if (newQty < 1) {
        if (existing) {
            clearTimeout(existing.timer);
            pendingQtyUpdates.delete(updateKey);
        }
        if (inputEventTarget) {
            inputEventTarget.value = originalQty;
        }
        item.soLuong = originalQty;
        item.thanhTien = originalQty * (Number(item.donGia) || 0);
        onRemoveItem(item);
        return;
    }

    if (delta > 0) {
        const maxAllowed = originalQty + originalStock;
        if (newQty > maxAllowed) {
            addNotification({
                title: 'Cảnh báo',
                subtitle: `Hết số lượng sản phẩm (chỉ còn tối đa ${maxAllowed} sản phẩm).`,
                color: 'warning'
            });
            newQty = maxAllowed;
            if (newQty === currentOptimisticQty) {
                if (inputEventTarget) {
                    inputEventTarget.value = currentOptimisticQty;
                }
                return;
            }
        }
    }

    if (existing) {
        clearTimeout(existing.timer);
    }

    // Optimistic UI updates (instant local response)
    item.soLuong = newQty;
    item.thanhTien = newQty * (Number(item.donGia) || 0);
    if (inputEventTarget) {
        inputEventTarget.value = newQty;
    }

    // Tự động tính lại tổng tiền đơn hàng và voucher tức thì (0ms)
    if (selectedOrder.value?.listsHoaDonChiTiet) {
        const newTotal = selectedOrder.value.listsHoaDonChiTiet.reduce((sum, it) => sum + (Number(it.thanhTien) || 0), 0);
        selectedOrder.value.tongTien = newTotal;
        if (vouchers.value?.length) {
            const available = vouchers.value.filter((v) => !v.disabled && Number(v.donHangToiThieu || 0) <= newTotal);
            if (available.length > 0) {
                const best = available.reduce((max, cur) => {
                    const curDisc = getVoucherDiscountValue(cur, newTotal);
                    const maxDisc = getVoucherDiscountValue(max, newTotal);
                    return curDisc > maxDisc ? cur : max;
                }, available[0]);
                if (best) {
                    const disc = getVoucherDiscountValue(best, newTotal);
                    selectedOrder.value.idPhieuGiamGia = best.id;
                    selectedOrder.value.phieuGiamGia = best;
                    selectedOrder.value.tienGiamGiaPhieu = disc;
                    selectedOrder.value.tongTienSauGiam = Math.max(0, newTotal - disc);
                    selectedOrder.value.thanhTien = selectedOrder.value.tongTienSauGiam + Number(selectedOrder.value.phiVanChuyen || 0);
                }
            } else {
                selectedOrder.value.idPhieuGiamGia = null;
                selectedOrder.value.phieuGiamGia = null;
                selectedOrder.value.tienGiamGiaPhieu = 0;
                selectedOrder.value.tongTienSauGiam = newTotal;
                selectedOrder.value.thanhTien = newTotal + Number(selectedOrder.value.phiVanChuyen || 0);
            }
        }
    }

    // Debounce API call (300ms after last click)
    const timer = setTimeout(async () => {
        pendingQtyUpdates.delete(updateKey);
        try {
            const updated = await dichVuDonHang.updateSoLuong(currentOrderId, item.id, newQty);
            updateOrderInList(updated);
        } catch (e) {
            // Rollback on server rejection
            item.soLuong = originalQty;
            item.thanhTien = originalQty * (Number(item.donGia) || 0);
            if (inputEventTarget) {
                inputEventTarget.value = originalQty;
            }
            const errorMsg = e.response?.data?.message || MESSAGES.ERROR.PRODUCT_OUT_OF_STOCK;
            addNotification({ title: 'Lỗi', subtitle: errorMsg, color: 'error' });
            try {
                const data = await dichVuDonHang.layDonHangCho();
                setOrders(data, { preferOrderId: currentOrderId });
            } catch (_) {}
        }
    }, 300);

    pendingQtyUpdates.set(updateKey, {
        timer,
        orderId: currentOrderId,
        itemId: item.id,
        originalQty,
        originalStock,
        targetQty: newQty
    });
};

const onRemoveItem = (item) => {
    const currentOrderId = selectedOrder.value?.id || null;
    if (currentOrderId && item?.id) {
        const updateKey = `${currentOrderId}_${item.id}`;
        if (pendingQtyUpdates.has(updateKey)) {
            clearTimeout(pendingQtyUpdates.get(updateKey).timer);
            pendingQtyUpdates.delete(updateKey);
        }
    }
    confirmDialog.value = {
        show: true,
        title: 'Xác nhận xóa sản phẩm',
        message: `Bạn có chắc chắn muốn xóa [${item.tenSanPham}] khỏi giỏ hàng?`,
        color: 'primary',
        confirmColor: 'primary',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                if (!currentOrderId) throw new Error('Không tìm thấy hóa đơn đang chọn.');
                await dichVuDonHang.removeSanPham(currentOrderId, item.id);
                const data = await dichVuDonHang.layDonHangCho();
                setOrders(data, { preferOrderId: currentOrderId });
                confirmDialog.value.show = false;
            } catch (e) {
                addNotification({ title: 'Lỗi', subtitle: MESSAGES.ERROR.DELETE_DATA, color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

// Removed onRemoveCustomer

// Logic: Voucher
// Gắn nhãn hiển thị (customTitle) cho phiếu giảm giá trên dropdown. Chỉ phục vụ hiển thị,
// mọi tính toán giảm giá đều do BE thực hiện. Vô hiệu hóa phiếu nếu đơn hàng chưa đạt giá trị tối thiểu.
const decorateVoucher = (v, order = selectedOrder.value) => {
    let text = v.tenPhieu || v.ten || 'Phiếu giảm giá';
    const code = v.ma || v.maPhieu;
    if (code) text = `${code} - ${text}`;
    const type = String(v.loaiPhieu || '').toUpperCase();
    const discount =
        type === 'PHAN_TRAM' || type === 'PERCENT'
            ? `(Giảm ${v.phanTramGiamGia || 0}%)`
            : `(Giảm ${new Intl.NumberFormat('vi-VN').format(v.soTienGiam || 0)}đ)`;

    // Disable voucher if order total doesn't meet the minimum required amount
    const baseAmount = Number(order?.tongTien || 0);
    const disabled = Number(v.donHangToiThieu || 0) > baseAmount;

    return { ...v, customTitle: `${text} ${discount}`, disabled };
};

// BE là nguồn quyết định voucher tốt nhất (đã xét %/tiền mặt, trần giảm, thời hạn và phiếu cá nhân).
// Dùng serial để response cũ không ghi đè khi nhân viên thêm/xóa sản phẩm liên tục.
let voucherRefreshSerial = 0;
const refreshBestVoucher = async (order = selectedOrder.value) => {
    if (!order?.id) return;
    const refreshSerial = ++voucherRefreshSerial;
    try {
        const list = await dichVuDonHang.getVouchers(order.tongTien || 0);
        const decorated = (list || []).map((v) => decorateVoucher(v, order));
        if (refreshSerial !== voucherRefreshSerial) return;
        vouchers.value = decorated;
    } catch (e) {
        console.error('Lỗi khi cập nhật phiếu giảm giá:', e);
    }
};

// Cố định 1 voucher do người dùng tự chọn trên giao diện
// Áp dụng / gỡ phiếu giảm giá: BE lưu, tính lại tổng tiền sau giảm và trả về hóa đơn đã cập nhật.
const onApplyVoucher = async (voucherId, autoApply = false, isInternalCall = false) => {
    const order = selectedOrder.value;
    if (!order?.id) return;
    try {
        const updated = await dichVuDonHang.setVoucher(order.id, voucherId || null);
        updateOrderInList(updated);
        if (vouchers.value?.length) {
            vouchers.value = vouchers.value.map((v) => decorateVoucher(v, updated));
        }
    } catch (e) {
        order.suggestedVoucherId = null;
    }
};

// Logic: Thanh toán VNPay
const vnpayDialog = ref({
    show: false,
    loading: false,
    verified: false,
    statusText: '',
    orderId: '',
    amount: 0,
    paymentUrl: '',
    pollInterval: null
});

let vnpayPopup = null;

const clearVnPayPolling = () => {
    if (vnpayDialog.value.pollInterval) {
        clearInterval(vnpayDialog.value.pollInterval);
        vnpayDialog.value.pollInterval = null;
    }
};

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
    params.vnp_ResponseCode === '00' &&
    (!params.vnp_TransactionStatus || params.vnp_TransactionStatus === '00' || params.vnp_TransactionStatus === '0');

const isVnPayVerifySuccess = (verifyResult, params = {}) =>
    Boolean(verifyResult?.success) && Number(verifyResult?.status || 200) === 200 && isVnPayCallbackSuccess(params);

const buildCheckoutPayload = (order, overrides = {}) => {
    let compiledNote = checkoutData.value.note || '';

    const p = provincesShip.value.find((x) => x.code === recipientProvince.value);
    const d = districtsShip.value.find((x) => x.code === recipientDistrict.value);
    const w = wardsShip.value.find((x) => x.code === recipientWard.value);

    const shippingDetails = isGiaoHang.value
        ? [
              `Người nhận: ${recipientName.value || ''}`,
              `SĐT: ${recipientPhone.value || ''}`,
              `Địa chỉ: ${recipientAddressDetail.value || ''}`,
              w ? w.name : '',
              d ? d.name : '',
              p ? p.name : ''
          ]
              .filter(Boolean)
              .join(', ')
        : '';

    if (shippingDetails) {
        compiledNote = compiledNote ? `${compiledNote} | Ship: ${shippingDetails}` : `Ship: ${shippingDetails}`;
    }

    const fullShippingAddressStr = isGiaoHang.value
        ? [recipientAddressDetail.value || '', w ? w.name : '', d ? d.name : '', p ? p.name : ''].filter(Boolean).join(', ')
        : null;

    const cleanText = (val) => {
        if (val === null || val === undefined) return null;
        const s = String(val).trim();
        return s.length > 0 ? s : null;
    };

    const cleanPhone = (val) => {
        if (val === null || val === undefined) return null;
        const s = String(val).trim().replace(/\D/g, '');
        return s.length > 0 ? s : null;
    };

    const resolvedTenKhachHang = cleanText(customerForm.value?.ten) || cleanText(order?.tenKhachHang) || (isGiaoHang.value ? cleanText(recipientName.value) : null);
    const resolvedSdtKhachHang = cleanPhone(customerForm.value?.sdt) || cleanPhone(order?.sdtKhachHang) || (isGiaoHang.value ? cleanPhone(recipientPhone.value) : null);
    const resolvedEmailKhachHang = cleanText(customerForm.value?.email) || cleanText(order?.emailKhachHang);

    const resolvedTenNguoiNhan = isGiaoHang.value ? (cleanText(recipientName.value) || resolvedTenKhachHang) : null;
    const resolvedSdtNguoiNhan = isGiaoHang.value ? (cleanPhone(recipientPhone.value) || resolvedSdtKhachHang) : null;

    return {
        idKhachHang: order?.idKhachHang || null,
        tenKhachHang: resolvedTenKhachHang,
        sdtKhachHang: resolvedSdtKhachHang,
        emailKhachHang: resolvedEmailKhachHang,
        idPhieuGiamGia: order?.idPhieuGiamGia || null,
        tongTien: order?.tongTien || 0,
        phiVanChuyen: isGiaoHang.value ? shippingFee.value : 0,
        tongTienSauGiam: finalCollectAmount.value,
        orderType: ORDER_TYPES.IN_STORE,
        deliveryMethod: isGiaoHang.value ? DELIVERY_METHODS.SHIPPING : DELIVERY_METHODS.TAKEAWAY,
        loaiDon: isGiaoHang.value ? 'GIAO_HANG' : 'TAI_QUAY',
        ghiChu: compiledNote,
        tenNguoiNhan: resolvedTenNguoiNhan,
        sdtNguoiNhan: resolvedSdtNguoiNhan,
        diaChiNguoiNhan: fullShippingAddressStr,
        diaChiChiTiet: isGiaoHang.value ? recipientAddressDetail.value || null : null,
        tinh: isGiaoHang.value ? (p ? p.name : null) : null,
        thanhPho: isGiaoHang.value ? (d ? d.name : null) : null,
        phuongXa: isGiaoHang.value ? (w ? w.name : null) : null,
        luuDiaChiMacDinh: true,
        tienMat: 0,
        tienChuyenKhoan: 0,
        maGiaoDich: null,
        ...overrides
    };
};

const hasPaymentAmount = (payload) => Number(payload?.tienMat || 0) > 0 || Number(payload?.tienChuyenKhoan || 0) > 0;

const completePaidOrder = async (orderId) => {
    const index = orders.value.findIndex((order) => order.id === orderId);
    if (index !== -1) {
        orders.value.splice(index, 1);
    }
    clampActiveOrderIndex();
    checkoutData.value.receivedAmount = 0;
    checkoutData.value.note = '';
};

const showReceipt = (order, paymentMethod, receivedAmount, note) => {
    receiptDialog.value = {
        show: true,
        order: JSON.parse(JSON.stringify(order)),
        paymentMethod,
        receivedAmount: Number(receivedAmount || 0),
        note: note || '',
        paidAt: Date.now()
    };
};

const onCloseReceipt = async () => {
    receiptDialog.value.show = false;
};

const onPrintReceiptInvoice = async (receipt = receiptDialog.value) => {
    const orderId = receipt?.order?.id || receipt?.order?.idHoaDon || selectedOrder.value?.id;
    await printHoaDonById(orderId);
};

const onPrintInvoice = () => {
    if (!selectedOrder.value || !hasValidCartItems.value) {
        addNotification({ title: 'Cảnh báo', subtitle: 'Hóa đơn chưa có sản phẩm hợp lệ để in.', color: 'warning' });
        return;
    }

    const printOrder = JSON.parse(JSON.stringify(selectedOrder.value));
    printOrder.tenKhachHang = customerForm.value.ten || 'Khách lẻ';
    printOrder.sdtKhachHang = customerForm.value.sdt || '';

    printOrder.tongTien = selectedOrder.value.tongTien || 0;
    printOrder.tongTienSauGiam = finalCollectAmount.value;
    printOrder.phiVanChuyen = isGiaoHang.value ? Number(shippingFee.value || 0) : 0;

    showReceipt(
        printOrder,
        checkoutData.value.paymentMethod || 'CASH',
        checkoutData.value.receivedAmount || finalCollectAmount.value,
        checkoutData.value.note || ''
    );
};

const submitCheckout = async ({
    order = selectedOrder.value,
    payload,
    successMessage = MESSAGES.SUCCESS.PAYMENT,
    showReceiptAfter = true
}) => {
    if (!order?.id) {
        throw new Error('Không tìm thấy hóa đơn đang thanh toán.');
    }
    if (!order?.listsHoaDonChiTiet?.length) {
        throw new Error('Hóa đơn chưa có sản phẩm.');
    }

    const requestPayload = payload;

    const orderSnapshot = JSON.parse(JSON.stringify(order));
    orderSnapshot.tongTienSauGiam = finalCollectAmount.value;
    orderSnapshot.phiVanChuyen = isGiaoHang.value ? Number(shippingFee.value || 0) : 0;

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

const finalizeVnPayCheckout = async (tienChuyenKhoan, maGiaoDich, order = selectedOrder.value) => {
    vnpayDialog.value.loading = true;
    vnpayDialog.value.statusText = 'Đang xác nhận hóa đơn và cập nhật số lượng...';
    try {
        await submitCheckout({
            order,
            payload: buildCheckoutPayload(order, {
                tienChuyenKhoan: finalCollectAmount.value,
                maGiaoDich
            })
        });
        vnpayDialog.value.verified = true;
        sessionStorage.removeItem(VNPAY_PENDING_KEY);
        closeVnPayFlow();
    } catch (error) {
        const errMsg = getErrorMessage(error, MESSAGES.ERROR.PAYMENT_FAILED);
        addNotification({ title: 'Lỗi chốt đơn VNPay', subtitle: errMsg, color: 'error', timeout: 8000 });
        vnpayDialog.value.loading = false;
        vnpayDialog.value.show = false;
        vnpayDialog.value.statusText = '';
        sessionStorage.removeItem(VNPAY_PENDING_KEY);
    }
};

const startVnPayFlow = async () => {
    confirmDialog.value.loading = true;
    try {
        if (!selectedOrder.value?.listsHoaDonChiTiet?.length) {
            throw new Error('Hóa đơn chưa có sản phẩm.');
        }
        const orderId = selectedOrder.value.maHoaDon + '_' + Date.now();
        const payload = {
            amount: finalCollectAmount.value,
            orderId: orderId,
            orderInfo: 'Thanh toan hoa don ' + selectedOrder.value.maHoaDon,
            returnUrl: `${window.location.origin}${window.location.pathname}`
        };

        const data = await dichVuVnPay.createPaymentUrl(payload);
        if (!data || !data.paymentUrl) {
            throw new Error('Không lấy được URL thanh toán từ server');
        }

        sessionStorage.setItem(
            VNPAY_PENDING_KEY,
            JSON.stringify({
                orderId: selectedOrder.value.id,
                maHoaDon: selectedOrder.value.maHoaDon,
                amount: selectedOrder.value.tongTienSauGiam,
                transactionId: orderId
            })
        );

        vnpayDialog.value = {
            show: true,
            loading: false,
            verified: false,
            statusText:
                checkoutData.value.vnpayMethod === 'GATEWAY'
                    ? 'Đang chờ khách hàng thanh toán qua cổng VNPay...'
                    : 'Đang chờ khách hàng quét mã và thanh toán...',
            orderId: orderId,
            amount: finalCollectAmount.value,
            paymentUrl: data.paymentUrl,
            pollInterval: null
        };

        if (checkoutData.value.vnpayMethod === 'GATEWAY') {
            vnpayPopup = window.open(data.paymentUrl, 'vnpay', 'width=800,height=600');
        }

        // Tự động Polling trạng thái đơn hàng từ Backend mỗi 3 giây
        vnpayDialog.value.pollInterval = setInterval(async () => {
            try {
                if (!vnpayDialog.value.show) return;

                // 1. Kiểm tra trạng thái thanh toán từ Backend
                const status = await dichVuDonHang.checkPaymentStatus(selectedOrder.value.id);

                if (status && status.isPaid) {
                    clearInterval(vnpayDialog.value.pollInterval);
                    vnpayDialog.value.pollInterval = null;

                    if (vnpayPopup && !vnpayPopup.closed) {
                        vnpayPopup.close();
                    }

                    // Gọi API finalize để cập nhật giao diện (BE đã tự trừ kho qua IPN)
                    await finalizeVnPayCheckout(vnpayDialog.value.amount, status.transactionNo || 'VNP_AUTO', selectedOrder.value);
                    return;
                }

                // 2. Nếu là GATEWAY và popup bị đóng nhưng chưa thanh toán -> Hủy
                if (checkoutData.value.vnpayMethod === 'GATEWAY' && vnpayPopup && vnpayPopup.closed && !status.isPaid) {
                    clearInterval(vnpayDialog.value.pollInterval);
                    vnpayDialog.value.pollInterval = null;
                    if (!vnpayDialog.value.verified && sessionStorage.getItem(VNPAY_PENDING_KEY)) {
                        handleVnPayCanceled('Khách hàng đã đóng cửa sổ thanh toán.');
                    }
                }
            } catch (e) {
                console.warn('Lỗi khi kiểm tra trạng thái VNPay:', e);
            }
        }, 3000);
    } catch (error) {
        console.error('VNPay flow error:', error);
        const errorMsg = getErrorMessage(error, 'Không thể khởi tạo giao dịch VNPay');
        addNotification({ title: 'Lỗi khởi tạo VNPay', subtitle: errorMsg, color: 'error', timeout: 8000 });
    } finally {
        confirmDialog.value.loading = false;
    }
};

const onConfirmVnPayManual = async () => {
    vnpayDialog.value.loading = true;
    vnpayDialog.value.statusText = 'Đang xác nhận hóa đơn...';
    try {
        const txnNo = `VNP_MANUAL_${Date.now()}`;
        // Nếu admin bấm thủ công, ta vẫn gửi request finalize để chốt đơn
        await finalizeVnPayCheckout(vnpayDialog.value.amount, txnNo, selectedOrder.value);
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể xác nhận thanh toán', color: 'error' });
        vnpayDialog.value.loading = false;
    }
};

// Logic: Better Voucher & Ineligible Voucher Dialogs
const betterVoucherDialog = ref({
    show: false,
    currentVoucher: null,
    betterVoucher: null,
    orderTotal: 0,
    currentDiscount: 0,
    betterDiscount: 0,
    onProceed: null
});

const voucherIneligibleDialog = ref({
    show: false,
    voucher: null,
    orderTotal: 0,
    minOrder: 0,
    shortfall: 0,
    message: ''
});

const onRemoveIneligibleVoucher = async () => {
    voucherIneligibleDialog.value.show = false;
    await onApplyVoucher(null);
    addNotification({
        title: 'Đã gỡ phiếu giảm giá',
        subtitle: 'Phiếu giảm giá chưa đủ điều kiện đã được gỡ khỏi hóa đơn.',
        color: 'info'
    });
};

const onOpenVoucherIneligibleModal = () => {
    if (!selectedOrder.value) return;
    const v = selectedOrder.value.phieuGiamGia || (selectedOrder.value.idPhieuGiamGia ? vouchers.value.find((item) => String(item.id) === String(selectedOrder.value.idPhieuGiamGia)) : null);
    const minOrderVal = Number(selectedOrder.value.voucherMinOrder || v?.donHangToiThieu || 0);
    const currentTotal = Number(selectedOrder.value.tongTien || cartSubtotalAmount.value || 0);
    const shortfallVal = Number(selectedOrder.value.voucherShortfall || Math.max(0, minOrderVal - currentTotal));

    voucherIneligibleDialog.value = {
        show: true,
        voucher: v || {
            ma: selectedOrder.value.idPhieuGiamGia,
            ten: 'Phiếu giảm giá'
        },
        orderTotal: currentTotal,
        minOrder: minOrderVal,
        shortfall: shortfallVal,
        message: selectedOrder.value.voucherIneligibleMessage || `Đơn hàng chưa đạt giá trị tối thiểu ${new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(minOrderVal)} của phiếu giảm giá.`
    };
};

const onApplyBetterVoucher = async () => {
    const betterId = betterVoucherDialog.value.betterVoucher?.id || selectedOrder.value?.bestVoucherId;
    betterVoucherDialog.value.show = false;
    if (betterId) {
        await onApplyVoucher(betterId, false, false);
        addNotification({ title: 'Áp dụng voucher thành công', subtitle: 'Đã chuyển sang mã giảm giá tốt hơn.', color: 'success' });
    }
    if (betterVoucherDialog.value.onProceed) {
        const fn = betterVoucherDialog.value.onProceed;
        betterVoucherDialog.value.onProceed = null;
        await fn();
    }
};

const onKeepOldVoucher = async () => {
    betterVoucherDialog.value.show = false;
    if (betterVoucherDialog.value.onProceed) {
        const fn = betterVoucherDialog.value.onProceed;
        betterVoucherDialog.value.onProceed = null;
        await fn();
    }
};

const getVoucherDiscountValue = (voucher, total) => {
    if (!voucher) return 0;
    const type = String(voucher.loaiPhieu || voucher.loai || '').toUpperCase();
    if (type === 'PHAN_TRAM' || type === 'PERCENT') {
        const percent = Number(voucher.phanTramGiamGia || voucher.giamGia || 0);
        let disc = total * (percent / 100);
        const maxGiam = Number(voucher.giamToiDa || voucher.soTienGiamToiDa || 0);
        if (maxGiam > 0 && disc > maxGiam) disc = maxGiam;
        return disc;
    } else {
        return Number(voucher.soTienGiam || voucher.giamGia || 0);
    }
};

const checkBetterVoucherBeforeCheckout = async () => {
    const order = selectedOrder.value;
    if (!order?.id) return null;

    const total = Number(order.tongTien || 0);
    if (total <= 0) return null;

    try {
        const availableVouchers = await dichVuDonHang.getVouchers(total);
        if (!availableVouchers || !availableVouchers.length) return null;

        const currentVoucherId = order.idPhieuGiamGia || null;
        let currentDiscount = 0;
        let currentVoucherObj = null;

        if (currentVoucherId) {
            currentVoucherObj = availableVouchers.find((v) => String(v.id) === String(currentVoucherId)) || order.phieuGiamGia || null;
            if (currentVoucherObj) {
                currentDiscount = getVoucherDiscountValue(currentVoucherObj, total);
            } else {
                currentDiscount = Number(order.tienGiamGiaPhieu || order.tienGiamGia || 0);
            }
        }

        let bestVoucher = null;
        let maxDiscount = 0;

        for (const v of availableVouchers) {
            const minOrder = Number(v.donHangToiThieu || 0);
            if (total < minOrder) continue;

            const disc = getVoucherDiscountValue(v, total);
            if (disc > maxDiscount) {
                maxDiscount = disc;
                bestVoucher = v;
            }
        }

        if (bestVoucher && maxDiscount > currentDiscount + 1000) {
            if (!currentVoucherId || String(bestVoucher.id) !== String(currentVoucherId)) {
                return {
                    currentVoucher: currentVoucherObj,
                    betterVoucher: bestVoucher,
                    currentDiscount,
                    betterDiscount: maxDiscount,
                    orderTotal: total
                };
            }
        }
    } catch (e) {
        console.error('Lỗi kiểm tra voucher tốt hơn:', e);
    }
    return null;
};

// Logic: Thanh toán
// Handler chính cho nút "Thanh toán"
const onCheckout = async () => {
    if (pendingQtyUpdates.size > 0) {
        await flushPendingQtyUpdates();
    }
    const items = selectedOrder.value?.listsHoaDonChiTiet || [];
    if (!items.length) {
        addNotification({ title: 'Cảnh báo', subtitle: 'Vui lòng thêm sản phẩm trước khi thanh toán.', color: 'warning' });
        return;
    }

    const hasInvalidQty = items.some((item) => !item.soLuong || Number(item.soLuong) <= 0);
    const totalQty = items.reduce((sum, item) => sum + (Number(item.soLuong) || 0), 0);

    if (hasInvalidQty || totalQty <= 0) {
        addNotification({
            title: 'Cảnh báo',
            subtitle: 'Hết số lượng sản phẩm. Vui lòng kiểm tra lại giỏ hàng.',
            color: 'warning'
        });
        return;
    }

    // Kiểm tra điều kiện voucher trước khi thanh toán
    if (selectedOrder.value?.voucherIneligible) {
        voucherIneligibleDialog.value = {
            show: true,
            voucher: selectedOrder.value.phieuGiamGia || {
                ma: selectedOrder.value.idPhieuGiamGia,
                ten: 'Phiếu giảm giá'
            },
            orderTotal: Number(selectedOrder.value.tongTien || 0),
            minOrder: Number(selectedOrder.value.voucherMinOrder || 0),
            shortfall: Number(selectedOrder.value.voucherShortfall || 0),
            message: selectedOrder.value.voucherIneligibleMessage || ''
        };
        addNotification({
            title: 'Chưa đủ điều kiện thanh toán',
            subtitle: selectedOrder.value.voucherIneligibleMessage || 'Đơn hàng chưa đạt giá trị tối thiểu để áp dụng phiếu giảm giá này.',
            color: 'warning'
        });
        return;
    }

    // Nếu chưa nhập tiền khách đưa hoặc nhập 0, tự động thanh toán đúng số tiền cần thanh toán
    if (!checkoutData.value.receivedAmount || Number(checkoutData.value.receivedAmount) <= 0) {
        checkoutData.value.receivedAmount = Number(finalCollectAmount.value);
    }

    if (checkoutData.value.paymentMethod === 'CASH' && Number(checkoutData.value.receivedAmount) < Number(finalCollectAmount.value)) {
        addNotification({ title: 'Cảnh báo', subtitle: MESSAGES.ERROR.INSUFFICIENT_FUNDS, color: 'warning' });
        return;
    }

    // Kiểm tra nếu có voucher tốt hơn trước khi thanh toán
    if (selectedOrder.value?.canApplySuggestedVoucher && selectedOrder.value?.bestVoucherId) {
        const betterInfo = await checkBetterVoucherBeforeCheckout();
        if (betterInfo) {
            betterVoucherDialog.value = {
                show: true,
                currentVoucher: betterInfo.currentVoucher,
                betterVoucher: betterInfo.betterVoucher,
                orderTotal: betterInfo.orderTotal,
                currentDiscount: betterInfo.currentDiscount,
                betterDiscount: betterInfo.betterDiscount,
                onProceed: async () => {
                    await proceedActualCheckout();
                }
            };
            return;
        }
    }

    await proceedActualCheckout();
};

const proceedActualCheckout = async () => {
    // Ensure customer is saved/resolved first if they typed new name/phone
    isProcessing.value = true;
    try {
        await ensureCustomerAndGetId();
    } catch (e) {
        addNotification({ title: 'Cảnh báo', subtitle: e.message || 'Không thể tạo hoặc tìm khách hàng.', color: 'warning' });
        isProcessing.value = false;
        return;
    }
    isProcessing.value = false;

    if (checkoutData.value.paymentMethod === 'VNPAY') {
        await startVnPayFlow();
        return;
    }

    confirmDialog.value = {
        show: true,
        title: 'Xác nhận thanh toán',
        message: `Bạn xác nhận thanh toán hóa đơn với tổng tiền [${new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(finalCollectAmount.value)}]?`,
        color: 'primary',
        action: async () => {
            confirmDialog.value.loading = true;
            isProcessing.value = true;
            try {
                const payload = buildCheckoutPayload(selectedOrder.value, {
                    tienMat: finalCollectAmount.value,
                    tienChuyenKhoan: 0
                });
                await submitCheckout({ payload });

                checkoutData.value.receivedAmount = 0;
                checkoutData.value.note = '';
                confirmDialog.value.show = false;
            } catch (e) {
                if (e.message !== 'RELOAD_REQUIRED') {
                    addNotification({
                        title: 'Thanh toán thất bại',
                        subtitle: getErrorMessage(e, MESSAGES.ERROR.PAYMENT_FAILED),
                        color: 'error'
                    });
                }
            } finally {
                isProcessing.value = false;
                confirmDialog.value.loading = false;
            }
        }
    };
};

// Helpers
const updateOrderInList = (updated) => {
    const normalized = normalizeSalesOrder(updated);
    const idx = orders.value.findIndex((o) => o.id === normalized.id);
    if (idx !== -1) orders.value[idx] = normalized;
    clampActiveOrderIndex();

    if (normalized.listsHoaDonChiTiet && Array.isArray(normalized.listsHoaDonChiTiet)) {
        normalized.listsHoaDonChiTiet.forEach((item) => {
            if (item.idChiTietSanPham && item.soLuongTon !== undefined && item.soLuongTon !== null) {
                banHangStore.updateProductStock(item.idChiTietSanPham, item.soLuongTon);
            }
        });
    }

    // Xử lý cảnh báo voucher từ phản hồi backend
    if (normalized.id === selectedOrder.value?.id) {
        if (normalized.voucherRemoved) {
            addNotification({
                title: 'Phiếu giảm giá đã bị gỡ',
                subtitle: normalized.voucherRemovedMessage || 'Phiếu giảm giá đã bị hủy hoặc không còn khả dụng.',
                color: 'warning'
            });
        }

        if (normalized.voucherIneligible) {
            voucherIneligibleDialog.value = {
                show: true,
                voucher: normalized.phieuGiamGia || {
                    ma: normalized.idPhieuGiamGia,
                    ten: 'Phiếu giảm giá'
                },
                orderTotal: Number(normalized.tongTien || 0),
                minOrder: Number(normalized.voucherMinOrder || 0),
                shortfall: Number(normalized.voucherShortfall || 0),
                message: normalized.voucherIneligibleMessage || ''
            };
            addNotification({
                title: 'Chưa đủ điều kiện phiếu giảm giá',
                subtitle: normalized.voucherIneligibleMessage || 'Đơn hàng chưa đạt giá trị tối thiểu của phiếu giảm giá.',
                color: 'warning'
            });
        }

        if (normalized.canApplySuggestedVoucher && normalized.betterVoucherCode) {
            addNotification({
                title: 'Có voucher tốt hơn',
                subtitle: normalized.voucherSuggestionText || `Có mã [${normalized.betterVoucherCode}] giảm nhiều hơn cho đơn hàng.`,
                color: 'info'
            });
        }
    }
};

const getErrorMessage = (error, fallback) => {
    return getBackendErrorMessage(error, fallback, 'BanHang');
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
    <v-container fluid class="pos-wrapper pa-0 position-relative">
        <v-progress-linear
            v-if="loading"
            indeterminate
            color="primary"
            height="3"
            style="position: absolute; top: 0; left: 0; right: 0; z-index: 9999"
        />
        <div class="pos-shell">
            <header class="pos-header-row d-flex align-center justify-space-between">
                <div class="d-flex align-center ga-4">
                    <OrderTabs
                        :orders="orders"
                        :active-index="activeOrderIndex"
                        @select="(idx) => (activeOrderIndex = idx)"
                        @create="createNewOrder"
                        @close="closeOrder"
                    />
                </div>
                <div class="d-flex align-center ga-2 mr-2" v-if="isStaff">
                    <v-chip v-if="currentGiaoCa" color="success" size="small" variant="tonal" class="font-weight-medium">
                        <v-icon start size="14">mdi-clock-outline</v-icon>
                        Ca làm: {{ currentGiaoCa.maGiaoCa || 'Đang mở' }}
                    </v-chip>
                    <v-btn
                        v-if="currentGiaoCa"
                        color="warning"
                        size="small"
                        variant="flat"
                        class="rounded-lg text-white"
                        @click="openChotCaModal"
                    >
                        <v-icon start size="14">mdi-store-remove</v-icon>
                        Chốt Ca / Giao Ca
                    </v-btn>
                    <v-btn
                        v-else
                        color="primary"
                        size="small"
                        variant="flat"
                        class="rounded-lg"
                        @click="openMoCaModal"
                    >
                        <v-icon start size="14">mdi-store-clock</v-icon>
                        Mở Ca Làm Việc
                    </v-btn>
                </div>
            </header>

            <!-- Main Workspace Grid -->
            <v-row v-if="selectedOrder" class="pos-grid">
                <!-- Left Column (8 cols out of 12 on large, 7 cols on medium) -->
                <v-col cols="12" md="7" lg="8" class="h-100 d-flex flex-column ga-4 pr-md-2" style="min-height: 0">
                    <!-- Sản phẩm Card -->
                    <v-card
                        class="pos-card pa-4 d-flex flex-column flex-grow-1"
                        style="overflow: visible !important; z-index: 15 !important; min-height: 0"
                    >
                        <!-- Product Picker Block (Nhúng Component Mới) -->
                        <ProductPicker :active-order="selectedOrder" @add-product="onAddProduct" />

                        <!-- Cart list rendering -->
                        <div
                            class="cart-container-box rounded-lg overflow-y-auto flex-grow-1 d-flex flex-column"
                            style="min-height: 200px; background-color: #ffffff !important"
                        >
                            <CartTable
                                v-if="selectedOrder?.listsHoaDonChiTiet?.length"
                                :items="selectedOrder.listsHoaDonChiTiet"
                                @update-qty="onUpdateQty"
                                @remove="onRemoveItem"
                            />

                            <!-- Empty Cart State -->
                            <div v-else class="d-flex flex-column align-center justify-center h-100" style="background-color: #ffffff">
                                <div class="mb-2">
                                    <v-icon size="40" style="color: #cbd5e1 !important">mdi-inbox-outline</v-icon>
                                </div>
                                <div class="font-weight-medium" style="font-size: 13px !important; color: #94a3b8 !important">
                                    Giỏ hàng trống
                                </div>
                            </div>
                        </div>
                    </v-card>
                </v-col>

                <!-- Right Column (4 cols out of 12 on large, 5 cols on medium) -->
                <v-col
                    cols="12"
                    md="5"
                    lg="4"
                    class="h-100 d-flex flex-column ga-4 pl-md-2 mt-4 mt-md-0 overflow-y-auto"
                    style="min-height: 0"
                >
                    <!-- Khách hàng và Nhận hàng Card -->
                    <CustomerAndShippingPanel
                        :order="selectedOrder"
                        :is-giao-hang="isGiaoHang"
                        class="flex-shrink-0"
                        :initial-customer-form="customerForm"
                        :initial-shipping="{
                            name: recipientName,
                            phone: recipientPhone,
                            detail: recipientAddressDetail,
                            province: recipientProvince,
                            district: recipientDistrict,
                            ward: recipientWard
                        }"
                        @remove-customer="onRemoveCustomer"
                        @set-customer="onSelectSuggestedCustomer"
                        @open-quick-add="handleOpenQuickAdd"
                        @update:customer-form="onCustomerFormUpdate"
                        @update:shipping="onShippingPanelUpdate"
                    />

                    <!-- Pricing/Voucher Details (Moved from left column) -->
                    <OrderSummaryPanel
                        v-model:isGiaoHang="isGiaoHang"
                        :vouchers="vouchers"
                        class="flex-shrink-0"
                        :selected-voucher-id="selectedOrder?.idPhieuGiamGia"
                        :applied-voucher="selectedOrder?.phieuGiamGia"
                        :voucher-suggestion-text="voucherSuggestionText"
                        :better-voucher-suggestion-text="selectedOrder?.betterVoucherSuggestionText || ''"
                        :total-raw-amount="totalRawAmount"
                        :voucher-base-amount="cartSubtotalAmount"
                        :product-discount-amount="productDiscountAmount"
                        :voucher-discount-amount="discountAmount"
                        :total-discount-amount="totalDiscountAmount"
                        :final-collect-amount="finalCollectAmount"
                        v-model:shippingFee="shippingFee"
                        :shipping-fee-loading="shippingFeeLoading"
                        :shipping-fee-source="shippingFeeSource"
                        :shipping-fee-error="shippingFeeError"
                        :is-free-ship="isFreeShip"
                        @apply-voucher="onApplyVoucher"
                        @open-voucher-ineligible-modal="onOpenVoucherIneligibleModal"
                    />

                    <!-- Payment Card -->
                    <PaymentPanel
                        v-model:paymentMethod="checkoutData.paymentMethod"
                        class="flex-shrink-0"
                        v-model:receivedAmount="checkoutData.receivedAmount"
                        :final-collect-amount="finalCollectAmount"
                        :remaining-balance="remainingBalance"
                        :change-amount="changeAmount"
                        :is-processing="isProcessing"
                        :has-items="hasValidCartItems"
                        :vnpay-method="checkoutData.vnpayMethod"
                        :vnpay-dialog="vnpayDialog"
                        @checkout="onCheckout"
                    />
                </v-col>
            </v-row>

            <!-- Loading Placeholder -->
            <div v-else-if="loading" class="fill-height d-flex flex-column align-center justify-center text-slate-500 py-16">
                <v-progress-circular indeterminate color="primary" size="36" class="mb-3" />
                <span class="text-caption font-weight-medium text-slate-400">Đang chuẩn bị màn hình bán hàng...</span>
            </div>

            <!-- Empty Orders State -->
            <div v-else class="empty-orders-state d-flex flex-column align-center justify-center py-16 px-4 text-center">
                <template v-if="isStaff && !currentGiaoCa">
                    <div
                        class="empty-state-icon-box d-flex align-center justify-center rounded-circle mb-3 mx-auto"
                        style="width: 72px; height: 72px; background: rgba(254, 243, 199, 0.8); border: 1.5px dashed #f59e0b"
                    >
                        <v-icon size="36" color="warning">mdi-store-clock-outline</v-icon>
                    </div>
                    <div class="text-subtitle-1 font-weight-bold mt-1 text-slate-800">Chưa Mở Ca Làm Việc</div>
                    <div class="text-body-2 text-slate-500 mt-1" style="max-width: 440px">
                        Bạn cần mở ca làm việc và kiểm két tiền trước khi bắt đầu tạo hóa đơn bán hàng tại quầy.
                    </div>
                    <v-btn color="primary" class="mt-4 rounded-pill px-6 font-weight-bold text-none" prepend-icon="mdi-store-clock" @click="openMoCaModal">
                        Mở Ca Làm Việc Ngay
                    </v-btn>
                </template>
                <template v-else>
                    <div
                        class="empty-state-icon-box d-flex align-center justify-center rounded-circle mb-3 mx-auto"
                        style="width: 72px; height: 72px; background: rgba(241, 245, 249, 0.8); border: 1.5px dashed #cbd5e1"
                    >
                        <v-icon size="36" style="color: #94a3b8 !important">mdi-receipt-text-outline</v-icon>
                    </div>
                    <div class="text-subtitle-1 font-weight-bold mt-1 text-slate-800">Chưa có hóa đơn chờ</div>
                    <div class="text-body-2 text-slate-500 mt-1" style="max-width: 440px">Tạo hóa đơn mới để bắt đầu bán hàng tại quầy.</div>
                    <v-btn color="primary" class="mt-4 rounded-pill px-6 font-weight-bold text-none" prepend-icon="mdi-plus" :loading="isProcessing" @click="createNewOrder">
                        Tạo hóa đơn mới
                    </v-btn>
                </template>
            </div>
        </div>

        <!-- VNPay Dialogs -->
        <VnPayDialogs
            v-model:vnpayDialog="vnpayDialog"
            v-model:vnpayChoiceDialog="vnpayChoiceDialog"
            :vnpay-method="checkoutData.vnpayMethod"
            @proceed-choice="proceedVnPayChoice"
            @confirm-manual="onConfirmVnPayManual"
            @retry-qr="startVnPayFlow"
            @cancel="cancelVnPayFlow"
            @open-gateway="
                () => {
                    vnpayPopup = window.open(vnpayDialog.paymentUrl, 'vnpay', 'width=800,height=600');
                }
            "
        />

        <!-- Modal Voucher Tốt Hơn -->
        <BetterVoucherModal
            :show="betterVoucherDialog.show"
            :current-voucher="betterVoucherDialog.currentVoucher"
            :better-voucher="betterVoucherDialog.betterVoucher"
            :order-total="betterVoucherDialog.orderTotal"
            :current-discount="betterVoucherDialog.currentDiscount"
            :better-discount="betterVoucherDialog.betterDiscount"
            @apply-new="onApplyBetterVoucher"
            @keep-old="onKeepOldVoucher"
            @close="betterVoucherDialog.show = false"
        />

        <!-- Modal Voucher Chưa Đủ Điều Kiện -->
        <VoucherIneligibleModal
            :show="voucherIneligibleDialog.show"
            :voucher="voucherIneligibleDialog.voucher"
            :order-total="voucherIneligibleDialog.orderTotal"
            :min-order="voucherIneligibleDialog.minOrder"
            :shortfall="voucherIneligibleDialog.shortfall"
            :message="voucherIneligibleDialog.message"
            @remove-voucher="onRemoveIneligibleVoucher"
            @close="voucherIneligibleDialog.show = false"
        />

        <!-- Scanner dialog -->
        <ScannerDialog
            v-model="showScanner"
            :scanner-element-id="scannerElementId"
            @stop="stopScanner"
            @scan-file="onScanFile"
        />

        <!-- Confirmation Dialog -->
        <AdminConfirm
            v-model:show="confirmDialog.show"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            :color="confirmDialog.color"
            :confirm-color="confirmDialog.confirmColor"
            :loading="confirmDialog.loading"
            @confirm="confirmDialog.action"
        />

        <!-- Hóa đơn sau thanh toán -->
        <InvoiceReceiptDialog :show="receiptDialog.show" :receipt="receiptDialog" @close="onCloseReceipt" @print="onPrintReceiptInvoice" />

        <!-- Modal Thêm Nhanh Khách Hàng -->
        <QuickAddCustomerDialog v-model="showQuickAddDialog" :initial-data="quickAddInitialData" @success="onQuickAddSuccess" />

        <!-- Giao Ca Modal -->
        <GiaoCaModal
            v-model="showGiaoCaModal"
            :mode="giaoCaModalMode"
            :current-shift="currentGiaoCa"
            @success="handleGiaoCaSuccess"
        />
    </v-container>
</template>

<style scoped lang="scss">
@import '@/scss/pages/admin/_ban-hang.scss';

/* Đồng bộ font-size 13px và khoảng cách cho các mục menu select của Vuetify (do được teleport ra ngoài body) */
.v-overlay-container {
    .v-list-item {
        min-height: 32px !important;
        padding-top: 4px !important;
        padding-bottom: 4px !important;

        .v-list-item-title,
        .v-list-item__content,
        .v-list-item-subtitle {
            font-size: 13px !important;
        }
    }
}

.qr-reader-box {
    width: 100%;
    min-height: 320px;
    overflow: hidden;
    border: 1px solid #dbe3ef;
    border-radius: 12px;
    background: #0f172a;
}

:deep(.qr-reader-box video) {
    width: 100% !important;
    min-height: 320px;
    object-fit: cover;
}
</style>
