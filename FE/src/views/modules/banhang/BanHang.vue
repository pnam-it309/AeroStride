<script setup>
/**
 * Module: Bán hàng tại quầy (Admin/POS)
 * View: BanHang
 * Chức năng: Màn hình chính xử lý tạo hóa đơn, thêm sản phẩm, cập nhật số lượng,
 *            chọn khách hàng, áp dụng voucher, thanh toán bằng tiền mặt/chuyển khoản (VNPay),
 *            và in hóa đơn sau khi hoàn tất.
 */
import { ref, onMounted, onUnmounted, computed, watch } from 'vue';
import { BoxIcon, XIcon } from 'vue-tabler-icons';
import { Html5Qrcode } from 'html5-qrcode';
import QrcodeVue from 'qrcode.vue';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';
import { dichVuVnPay } from './composables/dichVuVnPay.js';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';
import { dichVuNhanVien } from '@/services/admin/dichVuNhanVien';
import {
    dichVuThuongHieu,
    dichVuXuatXu,
    dichVuMucDichChay,
    dichVuChatLieu,
    dichVuMauSac,
    dichVuKichThuoc
} from '@/services/product/dichVuThuocTinh';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { useNotifications } from '@/services/notificationService';
import api from '@/services/apiService';
import { API_ADMIN } from '@/constants/apiPaths';
import { MESSAGES } from '@/constants/messages';
import { useUIStore } from '@/stores/ui';
import { useAuthStore } from '@/stores/authStore';
import { useBanHangStore } from '@/stores/banHangStore';
import { useLocation } from '@/composables/useLocation';
import { useAddressMapping } from '@/composables/useAddressMapping';
import { useHoaDonPrinter } from '@/composables/useHoaDonPrinter';
import { GIOI_TINH_OPTIONS } from '@/constants/appConstants';
import { isActiveStatus } from '@/utils/statusUtils';

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
import { dichVuGiaoCa } from '@/services/admin/dichVuGiaoCa';

const { addNotification } = useNotifications();
const { printHoaDonById } = useHoaDonPrinter();
const uiStore = useUIStore();
const authStore = useAuthStore();
const MAX_WAITING_ORDERS = 5;
const VNPAY_PENDING_KEY = 'aerostride_pos_vnpay_pending';
const BYPASS_PAYMENT_RECORD_INSERT = false;

// Giao Ca State
const showGiaoCaModal = ref(false);
const currentGiaoCa = ref(null);

const checkGiaoCa = async () => {
    try {
        const res = await dichVuGiaoCa.getCaHienTai();
        const data = res?.data || res;
        if (!data || !data.id) {
            // Tạm thời comment Giao ca theo yêu cầu
            // showGiaoCaModal.value = true;
        } else {
            currentGiaoCa.value = data;
        }
    } catch (e) {
        // Tạm thời comment Giao ca
        // showGiaoCaModal.value = true;
    }
};

const handleGiaoCaSuccess = () => {
    checkGiaoCa();
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
const orders = ref([]);
const activeOrderIndex = ref(0);
const vouchers = ref([]);
const isProcessing = ref(false);

// Redesigned Page Custom State
const orderWarehouse = ref('KHO ANSHA BIGSIZE');

// Dynamic Filter States for POS Products
const maxProductPrice = ref(7000000);

// Right Column Fields
const currentEmployeeDetail = ref(null);

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
            const exactMatch = variants.find((v) => String(v.maChiTietSanPham || '').trim().toLowerCase() === normalizedKeyword) || variants[0];
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
            selectedOrder.value.isGiaoHangLocal = (newVal === 'Giao hàng' || newVal === 'Trực tuyến');
            selectedOrder.value.loaiDon = selectedOrder.value.isGiaoHangLocal ? 'GIAO_HANG' : 'TAI_QUAY';
        }
    }
});

const isGiaoHang = computed({
    get() {
        if (!selectedOrder.value) return false;
        return !!selectedOrder.value.isGiaoHangLocal;
    },
    set(val) {
        if (selectedOrder.value) {
            selectedOrder.value.isGiaoHangLocal = val;
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

// Danh sách phần trăm giảm (duy nhất) của các sản phẩm trong đơn
const appliedDiscountPercents = computed(() => {
    const items = selectedOrder.value?.listsHoaDonChiTiet || [];
    const percents = items
        .map((item) => Number(item.phanTramGiam) || 0)
        .filter((percent) => percent > 0);
    return [...new Set(percents)].sort((a, b) => b - a);
});

const appliedDiscountSummary = computed(() =>
    appliedDiscountPercents.value.length
        ? appliedDiscountPercents.value.map((percent) => `${percent}%`).join(', ')
        : 'Không có'
);

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
    return Number(selectedOrder.value?.thanhTien || 0);
});

const remainingBalance = computed(() => {
    const received = Number(checkoutData.value.receivedAmount || 0);
    return Math.max(0, finalCollectAmount.value - received);
});

const changeAmount = computed(() => {
    const received = Number(checkoutData.value.receivedAmount || 0);
    return Math.max(0, received - finalCollectAmount.value);
});

// Backend chỉ trả loaiDon, còn UI dùng isGiaoHangLocal cho công tắc giao hàng.
// Chuẩn hóa tại một chỗ để mỗi lần refresh/cập nhật order không làm công tắc bị tắt sai.
const isShippingOrderType = (loaiDon) => ['ONLINE', 'GIAO_HANG'].includes(String(loaiDon || '').toUpperCase());
const normalizeSalesOrder = (order) => {
    if (!order) return order;
    return {
        ...order,
        isGiaoHangLocal: order.isGiaoHangLocal ?? isShippingOrderType(order.loaiDon)
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
            const shipFee = loaiDon === 'GIAO_HANG' && !isFreeShip.value ? Number(shippingFee.value || 0) : 0;

            const updatedOrder = await dichVuDonHang.updateShippingAndChannel(selectedOrder.value.id, {
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
watch(() => selectedOrder.value?.id, (id) => {
    checkoutData.value.note = selectedOrder.value?.ghiChu || '';

    if (id) {
        if (selectedOrder.value.phiVanChuyen !== undefined && selectedOrder.value.phiVanChuyen !== null) {
            shippingFee.value = Number(selectedOrder.value.phiVanChuyen);
            isFreeShip.value = Number(selectedOrder.value.phiVanChuyen) === 0 && selectedOrder.value.loaiDon === 'ONLINE';
        } else {
            const channel = selectedOrder.value.loaiDon === 'ONLINE' ? 'Trực tuyến' : 'Tại quầy';
            shippingFee.value = 0;
            isFreeShip.value = false;
        }

        const channel = selectedOrder.value.loaiDon === 'ONLINE' ? 'Trực tuyến' : 'Tại quầy';
        onlyChargeIfReturned.value = channel === 'Tại quầy';
    }
});

// Sync shipping fee when channel or free ship changes
watch([orderChannel, isFreeShip], ([channel, freeShip], oldVal) => {
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
        if (channel === 'Trực tuyến' && (oldFreeShip === true || channel !== oldChannel)) {
            void calculateShippingFee();
        } else if (channel !== oldChannel && oldChannel !== undefined) {
            shippingFee.value = 0;
            shippingFeeError.value = '';
            shippingFeeSource.value = '';
        }
    }
}, { immediate: true });

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

// Sync loaiDon when onlyChargeIfReturned changes manually
watch(onlyChargeIfReturned, (val) => {
    if (selectedOrder.value) {
        selectedOrder.value.loaiDon = val ? 'TAI_QUAY' : 'ONLINE';
    }
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

const betterVoucherSuggestionText = computed(() => {
    return selectedOrder.value?.betterVoucherSuggestionText || '';
});

const voucherSuggestionClass = computed(() =>
    selectedOrder.value?.bestVoucherId ? 'text-success' : (selectedOrder.value?.betterVoucherSuggestionText ? 'text-deep-orange-darken-3' : 'text-grey-darken-1')
);

const canApplySuggestedVoucher = computed(() => {
    return selectedOrder.value?.canApplySuggestedVoucher || false;
});

const isVoucherAutoApplied = ref({});

// Removed searchCustomers, selectCustomer, onCustomerInput, onSelectSuggestedCustomer, onCustomerFormUpdate

// Nhận dữ liệu địa chỉ giao hàng từ panel bên phải để watcher GHN ở màn chính tính lại phí ship.
const onShippingPanelUpdate = (shipping) => {
    const next = shipping || {};
    recipientName.value = next.name || '';
    recipientPhone.value = next.phone || '';
    recipientAddressDetail.value = next.detail || '';
    recipientProvince.value = next.province || null;
    recipientDistrict.value = next.district || null;
    recipientWard.value = next.ward || null;
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

// Removed ensureCustomerAndGetId


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
    const keyword = decodedText?.trim();
    if (!keyword) return;
    if (!selectedOrder.value) {
        addNotification({ title: 'Chưa có hóa đơn', subtitle: 'Vui lòng tạo hoặc chọn hóa đơn trước khi quét mã.', color: 'warning' });
        return;
    }

    try {
        const variants = await dichVuDonHang.searchSanPham({ keyword });
        if (variants && variants.length > 0) {
            const normalizedKeyword = keyword.toLowerCase();
            const exactMatch = variants.find((v) => String(v.maChiTietSanPham || '').trim().toLowerCase() === normalizedKeyword) || variants[0];

            if (exactMatch) {
                if (exactMatch.trangThai !== undefined && !isActiveStatus(exactMatch.trangThai)) {
                    addNotification({ title: 'Thất bại', subtitle: 'Sản phẩm đã ngừng bán', color: 'error' });
                    return;
                }
                const currentStock = Number(exactMatch.soLuongTon ?? exactMatch.soLuong ?? 0);
                if (currentStock <= 0) {
                    addNotification({ title: 'Thất bại', subtitle: 'Sản phẩm đã hết hàng', color: 'error' });
                    return;
                }

                await onAddProduct({ ...exactMatch, _soLuongMuonThem: 1 });
            }
        } else {
            addNotification({ title: 'Không tìm thấy', subtitle: `Không tìm thấy mã sản phẩm ${keyword}`, color: 'warning' });
        }
    } catch (e) {
        console.error('Scan error:', e);
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
    try {
        if (authStore.user?.username) {
            const res = await dichVuNhanVien.layThongTinCaNhan();
            if (res) {
                currentEmployeeDetail.value = res;
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
    loading.value = true;
    try {
        await checkGiaoCa();
        fetchProvincesShip();
        await loadCurrentEmployeeDetails();
        const data = await dichVuDonHang.layDonHangCho();
        setOrders(data);

        // Tải danh sách phiếu giảm giá cho dropdown (BE là nguồn dữ liệu)
        try {
            const list = await dichVuDonHang.getVouchers(selectedOrder.value?.tongTien || 0);
            vouchers.value = (list || []).map(v => decorateVoucher(v, selectedOrder.value));
            await refreshBestVoucher();
        } catch (e) {
            console.error('Lỗi khi tải phiếu giảm giá', e);
        }

        if (orders.value.length === 0) {
            await createNewOrder({ force: true, silent: true });
        }
        await handleVnPayCallbackFromUrl();
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: getErrorMessage(error, MESSAGES.ERROR.CONNECT_SERVER), color: 'error' });
    } finally {
        loading.value = false;
    }
});

onUnmounted(() => {
    window.removeEventListener('keydown', handleGlobalKeyDown);
    stopScanner();
});

// Customer shipping address restore helpers.
// Principle: customer data can come as split fields or one full address string, so normalize first, then match to the current location list.
const getAddressDetailValue = (address) => (
    address?.diaChiChiTiet ||
    address?.diaChiCuThe ||
    address?.diaChiNguoiNhan ||
    address?.diaChi ||
    ''
);

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
    return addresses.find((address) => address.laMacDinh === true || address.macDinh === true)
        || addresses.find((address) => address.trangThai === true || address.trangThai === 1 || address.trangThai === 'DANG_HOAT_DONG')
        || addresses[0];
};

const matchShippingLocation = (list, name) => {
    if (!name) return null;
    const cleanTarget = cleanName(name);
    return list.find((item) => {
        const cleanItem = cleanName(item.name);
        return cleanItem === cleanTarget || cleanItem.includes(cleanTarget) || cleanTarget.includes(cleanItem);
    }) || null;
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
    try {
        recipientAddressDetail.value = normalized.diaChiChiTiet || getAddressDetailValue(normalized) || '';
        await fetchProvincesShip();

        const provinceMatch = matchShippingLocation(provincesShip.value, normalized.tinh);
        recipientProvince.value = provinceMatch?.code || null;
        if (!provinceMatch) {
            shippingFee.value = 0;
            shippingFeeError.value = 'Không khớp tỉnh/thành phố với GHN.';
            shippingFeeSource.value = '';
            return;
        }

        await fetchDistrictsShip(provinceMatch.code);
        const districtMatch = matchShippingLocation(districtsShip.value, normalized.thanhPho);
        recipientDistrict.value = districtMatch?.code || null;
        if (!districtMatch) {
            shippingFee.value = 0;
            shippingFeeError.value = 'Không khớp quận/huyện với GHN.';
            shippingFeeSource.value = '';
            return;
        }

        await fetchWardsShip(districtMatch.code);
        const wardMatch = matchShippingLocation(wardsShip.value, normalized.phuongXa);
        recipientWard.value = wardMatch?.code || null;
        if (!wardMatch) {
            shippingFee.value = 0;
            shippingFeeError.value = 'Không khớp phường/xã với GHN.';
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

// Realtime PGG: mỗi lần đổi hóa đơn/khách hàng/tổng tiền/số lượng giỏ thì hỏi lại danh sách phiếu.
// Không đưa idPhieuGiamGia vào key để tránh vòng lặp sau khi FE tự áp phiếu tốt nhất.
const voucherRealtimeKey = computed(() => {
    const order = selectedOrder.value;
    if (!order?.id) return '';
    const itemKey = (order.listsHoaDonChiTiet || [])
        .map((item) => `${item.id}:${item.soLuong}`)
        .join('|');
    return `${order.id}|${order.idKhachHang || ''}|${order.tongTien || 0}|${itemKey}`;
});

watch(voucherRealtimeKey, async (key) => {
    if (key) await refreshBestVoucher();
}, { flush: 'post' });

// Watch for order changes to fetch product suggestions
watch(() => [selectedOrder.value?.id, selectedOrder.value?.tongTien], ([id, total]) => {
    if (id && total > 0) {
        fetchProductSuggestions();
    } else {
        productSuggestions.value = [];
        showProductSuggestions.value = false;
    }
});

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

const isGhnLocationCode = (list, code) => {
    const found = list.find((item) => String(item.code) === String(code));
    return !found || found.source === 'GHN';
};

async function calculateShippingFee() {
    if (!isGiaoHang.value || isFreeShip.value) return;
    if (!recipientDistrict.value || !recipientWard.value) {
        shippingFee.value = 0;
        shippingFeeSource.value = '';
        shippingFeeError.value = 'Chưa đủ địa chỉ nhận hàng để tính phí GHN.';
        return;
    }

    if (!isGhnLocationCode(districtsShip.value, recipientDistrict.value) || !isGhnLocationCode(wardsShip.value, recipientWard.value)) {
        shippingFee.value = 0;
        shippingFeeSource.value = '';
        shippingFeeError.value = 'Địa chỉ chưa có mã GHN hợp lệ để tính phí.';
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
            shippingFee.value = 0;
            shippingFeeSource.value = '';
            shippingFeeError.value = 'GHN chưa trả phí vận chuyển hợp lệ.';
        }
    } catch (e) {
        console.error('Failed to calculate shipping fee', e);
        shippingFee.value = 0;
        shippingFeeSource.value = '';
        shippingFeeError.value = 'Không tính được phí GHN, vui lòng kiểm tra địa chỉ/API.';
    } finally {
        shippingFeeLoading.value = false;
    }
}

watch(
    () => recipientWard.value,
    async (newVal) => {
        if (newVal && !syncingRecipientAddress.value) {
            await calculateShippingFee();
        }
    }
);

watch(
    () => selectedOrderItemCount.value,
    async () => {
        if (recipientWard.value && isGiaoHang.value) {
            await calculateShippingFee();
        }
    }
);

// Logic: Hóa đơn
const createNewOrder = async ({ silent = false, force = false } = {}) => {
    if (isProcessing.value && !force) return;
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
        orders.value.push(normalizeSalesOrder(newOrder));
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
        color: 'primary',
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
            soLuong: product._soLuongMuonThem || 1
        });
        updateOrderInList(updated);

        if (updated.priceChanged) {
            addNotification({ title: 'Giá sản phẩm thay đổi', subtitle: updated.priceChangeMessage, color: 'warning' });
        } else {
            addNotification({ title: 'Thành công', subtitle: 'Đã thêm sản phẩm vào giỏ hàng', color: 'success' });
        }

        refreshBestVoucher(updated);
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: MESSAGES.ERROR.PRODUCT_OUT_OF_STOCK, color: 'error' });
    } finally {
        isProcessing.value = false;
    }
};

const onUpdateQty = async (item, delta, inputEventTarget = null) => {
    let newQty = item.soLuong + delta;
    if (newQty < 1) {
        if (inputEventTarget) {
            inputEventTarget.value = item.soLuong;
        }
        onRemoveItem(item);
        return;
    }

    if (delta > 0) {
        const maxAllowed = item.soLuong + (item.soLuongTon || 0);
        if (newQty > maxAllowed) {
            addNotification({
                title: 'Vượt quá tồn kho',
                subtitle: `Chỉ còn tối đa ${maxAllowed} sản phẩm. Hệ thống đã tự động điều chỉnh.`,
                color: 'warning'
            });
            newQty = maxAllowed;
            if (newQty === item.soLuong) {
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
    } catch (e) {
        if (inputEventTarget) {
            inputEventTarget.value = item.soLuong;
        }
        addNotification({ title: 'Lỗi', subtitle: MESSAGES.ERROR.PRODUCT_OUT_OF_STOCK, color: 'error' });
    }
};

const onRemoveItem = (item) => {
    const currentOrderId = selectedOrder.value?.id || null;
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
                refreshBestVoucher();
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
    const discount = (type === 'PHAN_TRAM' || type === 'PERCENT')
        ? `(Giảm ${v.phanTramGiamGia || 0}%)`
        : `(Giảm ${new Intl.NumberFormat('vi-VN').format(v.soTienGiam || 0)}đ)`;

    // Disable voucher if order total doesn't meet the minimum required amount
    const baseAmount = getVoucherBaseAmount(order);
    const disabled = Number(v.donHangToiThieu || 0) > baseAmount;

    return { ...v, customTitle: `${text} ${discount}`, disabled };
};

// Hỏi BE danh sách phiếu giảm giá để gợi ý; FE chọn phiếu tốt nhất theo tổng tiền giỏ hiện tại.
// Dùng serial để response cũ không ghi đè khi nhân viên thêm/xóa sản phẩm liên tục.
let voucherRefreshSerial = 0;
const refreshBestVoucher = async (order = selectedOrder.value, autoApply = true) => {
    if (!order?.id) return;
    const refreshSerial = ++voucherRefreshSerial;
    try {
        const list = await dichVuDonHang.getVouchers(order.tongTien || 0);
        const decorated = (list || []).map(v => decorateVoucher(v, order));
        if (refreshSerial !== voucherRefreshSerial) return;
        vouchers.value = decorated;

        // Backend đã tính sẵn `bestVoucherId` trong selectedOrder
        // Nhưng nếu autoApply = true, chúng ta có thể gọi onApplyVoucher(order.bestVoucherId)
        if (autoApply) {
            const shouldAutoApply = isVoucherAutoApplied.value[order.id] !== false;
            if (shouldAutoApply) {
                isVoucherAutoApplied.value[order.id] = true;
                const bestId = order.bestVoucherId;
                if (bestId) {
                    if (String(order.idPhieuGiamGia) !== String(bestId)) {
                        await onApplyVoucher(bestId, false, true);
                    }
                } else {
                    if (order.idPhieuGiamGia) {
                        await onApplyVoucher(null, false, true);
                    }
                }
            }
        }
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
        if (!isInternalCall) {
            isVoucherAutoApplied.value[order.id] = false;
        }
        const updated = await dichVuDonHang.setVoucher(order.id, voucherId || null);
        updateOrderInList(updated);
        // Khi người dùng tự chọn mã thì không tự động ghi đè lại
        await refreshBestVoucher(updated, autoApply);
    } catch (e) {
        order.suggestedVoucherId = null;
    }
};

// Cho phép bấm trực tiếp dòng gợi ý để áp PGG tốt nhất hiện tại.
const applyBestVoucherFromSuggestion = async () => {
    if (!canApplySuggestedVoucher.value) return;
    const bestId = selectedOrder.value?.bestVoucherId;
    if (!bestId) return;
    await onApplyVoucher(bestId);
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
    params.vnp_ResponseCode === '00' && (!params.vnp_TransactionStatus || params.vnp_TransactionStatus === '00' || params.vnp_TransactionStatus === '0');

const isVnPayVerifySuccess = (verifyResult, params = {}) =>
    Boolean(verifyResult?.success) && Number(verifyResult?.status || 200) === 200 && isVnPayCallbackSuccess(params);

const buildCheckoutPayload = (order, overrides = {}) => {
    let compiledNote = checkoutData.value.note || '';
    if (orderChannel.value === 'Trực tuyến') {
        const p = provincesShip.value.find(x => x.code === recipientProvince.value);
        const d = districtsShip.value.find(x => x.code === recipientDistrict.value);
        const w = wardsShip.value.find(x => x.code === recipientWard.value);

        const shippingDetails = [
            `Người nhận: ${recipientName.value || ''}`,
            `SĐT: ${recipientPhone.value || ''}`,
            `Địa chỉ: ${recipientAddressDetail.value || ''}`,
            w ? w.name : '',
            d ? d.name : '',
            p ? p.name : ''
        ].filter(Boolean).join(', ');

        compiledNote = compiledNote
            ? `${compiledNote} | Ship: ${shippingDetails}`
            : `Ship: ${shippingDetails}`;
    }

    return {
        idKhachHang: order?.idKhachHang || null,
        idPhieuGiamGia: order?.idPhieuGiamGia || null,
        tongTien: order?.tongTien || 0,
        phiVanChuyen: shippingFee.value,
        tongTienSauGiam: finalCollectAmount.value,
        loaiDon: isGiaoHang.value ? 'ONLINE' : 'TAI_QUAY',
        ghiChu: compiledNote,
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
    if (!selectedOrder.value) return;

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
    vnpayDialog.value.statusText = 'Đang xác nhận hóa đơn và cập nhật tồn kho...';
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
        addNotification({ title: 'Không thể hoàn tất VNPay', subtitle: getErrorMessage(error, MESSAGES.ERROR.PAYMENT_FAILED), color: 'error' });
        vnpayDialog.value.loading = false;
        vnpayDialog.value.statusText = 'Giao dịch đã được xử lý nhưng hệ thống chưa thể chốt hóa đơn. Vui lòng kiểm tra lại đơn hàng.';
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

        sessionStorage.setItem(VNPAY_PENDING_KEY, JSON.stringify({
            orderId: selectedOrder.value.id,
            maHoaDon: selectedOrder.value.maHoaDon,
            amount: selectedOrder.value.tongTienSauGiam,
            transactionId: orderId
        }));

        vnpayDialog.value = {
            show: true,
            loading: false,
            verified: false,
            statusText: checkoutData.value.vnpayMethod === 'GATEWAY' ? 'Đang chờ khách hàng thanh toán qua cổng VNPay...' : 'Đang chờ khách hàng quét mã và thanh toán...',
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
        // Nếu admin bấm thủ công, ta vẫn gửi request finalize để chốt đơn
        await finalizeVnPayCheckout(vnpayDialog.value.amount, txnNo, selectedOrder.value);
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể xác nhận thanh toán', color: 'error' });
        vnpayDialog.value.loading = false;
    }
};

// Logic: Thanh toán
// Handler chính cho nút "Thanh toán"
const onCheckout = async () => {
    if (!selectedOrder.value?.listsHoaDonChiTiet?.length) {
        addNotification({ title: 'Cảnh báo', subtitle: 'Vui lòng thêm sản phẩm trước khi thanh toán.', color: 'warning' });
        return;
    }

    if (checkoutData.value.paymentMethod === 'CASH' && Number(checkoutData.value.receivedAmount || 0) < Number(finalCollectAmount.value)) {
        addNotification({ title: 'Cảnh báo', subtitle: MESSAGES.ERROR.INSUFFICIENT_FUNDS, color: 'warning' });
        return;
    }

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
const updateOrderInList = (updated) => {
    const normalized = normalizeSalesOrder(updated);
    const idx = orders.value.findIndex((o) => o.id === normalized.id);
    if (idx !== -1) orders.value[idx] = normalized;
    clampActiveOrderIndex();

    if (normalized.listsHoaDonChiTiet && Array.isArray(normalized.listsHoaDonChiTiet)) {
        normalized.listsHoaDonChiTiet.forEach(item => {
            if (item.idChiTietSanPham && item.soLuongTon !== undefined && item.soLuongTon !== null) {
                banHangStore.updateProductStock(item.idChiTietSanPham, item.soLuongTon);
            }
        });
    }
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



const formatNumberWithDots = (val) => {
    if (val === undefined || val === null || val === '') return '';
    const clean = String(val).replace(/\D/g, '');
    return clean.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
};

const parseNumberFromDots = (val) => {
    if (!val) return 0;
    const clean = String(val).replace(/\D/g, '');
    return clean ? Number(clean) : 0;
};

const formatDateTime = (dateStr) => {
    if (!dateStr) return '21:58 18/06/2026';
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;
    const pad = (n) => String(n).padStart(2, '0');
    return `${pad(date.getHours())}:${pad(date.getMinutes())} ${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()}`;
};
</script>

<template>
    <v-container fluid class="pos-wrapper pa-0">
        <div class="pos-shell">
            <!-- Header section containing title and tabs -->
            <header class="pos-header-row d-flex align-center justify-space-between">
                <div class="d-flex align-center gap-4">
                    <OrderTabs :orders="orders" :active-index="activeOrderIndex"
                        @select="(idx) => (activeOrderIndex = idx)" @create="createNewOrder" @close="closeOrder" />
                </div>
            </header>

            <!-- Main Workspace Grid -->
            <v-row v-if="selectedOrder" class="pos-grid">
                <!-- Left Column (8 cols out of 12) -->
                <v-col cols="12" lg="8" class="h-100 d-flex flex-column gap-4 pr-lg-2" style="min-height: 0;">
                    <!-- Sản phẩm Card -->
                    <v-card class="pos-card pa-4 d-flex flex-column flex-grow-1"
                        style="overflow: visible !important; z-index: 15 !important; min-height: 0;">

                        <!-- Product Picker Block (Nhúng Component Mới) -->
                        <ProductPicker :active-order="selectedOrder" @add-product="onAddProduct" />

                        <!-- Cart list rendering -->
                        <div class="cart-container-box rounded-lg overflow-y-auto flex-grow-1 d-flex flex-column"
                            style="min-height: 200px; background-color: #ffffff !important;">
                            <CartTable v-if="selectedOrder?.listsHoaDonChiTiet?.length"
                                :items="selectedOrder.listsHoaDonChiTiet" @update-qty="onUpdateQty"
                                @remove="onRemoveItem" />

                            <!-- Empty Cart State -->
                            <div v-else class="d-flex flex-column align-center justify-center h-100"
                                style="background-color: #ffffff;">
                                <div class="mb-2">
                                    <v-icon size="40" style="color: #cbd5e1 !important;">mdi-inbox-outline</v-icon>
                                </div>
                                <div class="font-weight-medium"
                                    style="font-size: 13px !important; color: #94a3b8 !important;">Giỏ hàng trống
                                </div>
                            </div>
                        </div>
                    </v-card>
                </v-col>

                <!-- Right Column (4 cols out of 12) -->
                <v-col cols="12" lg="4" class="h-100 d-flex flex-column gap-4 pl-lg-2 mt-4 mt-lg-0 overflow-y-auto"
                    style="min-height: 0;">
                    <!-- Khách hàng và Nhận hàng Card -->
                    <CustomerAndShippingPanel :order="selectedOrder" :is-giao-hang="isGiaoHang" class="flex-shrink-0"
                        :initial-customer-form="customerForm" :initial-shipping="{
                            name: recipientName,
                            phone: recipientPhone,
                            detail: recipientAddressDetail,
                            province: recipientProvince,
                            district: recipientDistrict,
                            ward: recipientWard
                        }" @remove-customer="onRemoveCustomer" @set-customer="onSelectSuggestedCustomer"
                        @update:customer-form="onCustomerFormUpdate" @update:shipping="onShippingPanelUpdate" />

                    <!-- Pricing/Voucher Details (Moved from left column) -->
                    <OrderSummaryPanel v-model:isGiaoHang="isGiaoHang" :vouchers="vouchers" class="flex-shrink-0"
                        :selected-voucher-id="selectedOrder?.idPhieuGiamGia"
                        :voucher-suggestion-text="voucherSuggestionText"
                        :voucher-suggestion-class="voucherSuggestionClass"
                        :can-apply-suggested-voucher="canApplySuggestedVoucher"
                        :better-voucher-suggestion-text="betterVoucherSuggestionText" :total-raw-amount="totalRawAmount"
                        :product-discount-amount="productDiscountAmount"
                        :applied-discount-summary="appliedDiscountSummary" :total-discount-amount="totalDiscountAmount"
                        :final-collect-amount="finalCollectAmount" v-model:shippingFee="shippingFee"
                        :shipping-fee-loading="shippingFeeLoading" :shipping-fee-source="shippingFeeSource"
                        :shipping-fee-error="shippingFeeError" :is-free-ship="isFreeShip"
                        @apply-voucher="onApplyVoucher" @apply-suggested-voucher="applyBestVoucherFromSuggestion" />

                    <!-- Payment Card -->
                    <PaymentPanel v-model:paymentMethod="checkoutData.paymentMethod" class="flex-shrink-0"
                        v-model:receivedAmount="checkoutData.receivedAmount" :remaining-balance="remainingBalance"
                        :change-amount="changeAmount" :is-processing="isProcessing"
                        :has-items="!!selectedOrder?.listsHoaDonChiTiet?.length"
                        :vnpay-method="checkoutData.vnpayMethod" :vnpay-dialog="vnpayDialog"
                        @print-invoice="onPrintInvoice" @checkout="onCheckout" />
                </v-col>
            </v-row>

            <!-- Loading Spinner -->
            <div v-else-if="loading" class="fill-height d-flex align-center justify-center text-grey">
                <v-progress-circular indeterminate color="primary"></v-progress-circular>
            </div>

            <!-- Empty Orders State -->
            <div v-else class="empty-orders-state">
                <v-icon size="56" color="grey-lighten-1">mdi-receipt-text-outline</v-icon>
                <div class="text-subtitle-1 font-weight-bold mt-3">Chưa có hóa đơn chờ</div>
                <div class="text-body-2 text-grey-darken-1 mt-1">Tạo hóa đơn mới để bắt đầu bán hàng tại quầy.</div>
                <v-btn color="primary" class="mt-4 rounded-lg" :loading="isProcessing" @click="createNewOrder">
                    Tạo hóa đơn
                </v-btn>
            </div>
        </div>

        <!-- VNPay Dialogs -->
        <VnPayDialogs v-model:vnpayDialog="vnpayDialog" v-model:vnpayChoiceDialog="vnpayChoiceDialog"
            :vnpay-method="checkoutData.vnpayMethod" @proceed-choice="proceedVnPayChoice"
            @confirm-manual="onConfirmVnPayManual" @retry-qr="startVnPayFlow" @cancel="cancelVnPayFlow"
            @open-gateway="() => { vnpayPopup = window.open(vnpayDialog.paymentUrl, 'vnpay', 'width=800,height=600'); }" />

        <!-- Scanner dialog -->
        <ScannerDialog v-model="showScanner" :scanner-element-id="scannerElementId" @stop="stopScanner" />

        <!-- Confirmation Dialog -->
        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :confirm-color="confirmDialog.confirmColor" :loading="confirmDialog.loading"
            @confirm="confirmDialog.action" />

        <!-- Hóa đơn sau thanh toán -->
        <InvoiceReceiptDialog :show="receiptDialog.show" :receipt="receiptDialog" @close="onCloseReceipt"
            @print="onPrintReceiptInvoice" />

        <!-- Giao Ca Modal --> <!-- Tạm thời ẩn chức năng giao ca
        <GiaoCaModal v-model="showGiaoCaModal" mode="open" @success="handleGiaoCaSuccess" /> 
        -->
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
