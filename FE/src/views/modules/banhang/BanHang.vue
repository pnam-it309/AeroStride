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
import { dichVuVnPay } from './dichVuVnPay';
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
import { useLocation } from '@/composables/useLocation';
import { useAddressMapping } from '@/composables/useAddressMapping';
import { useHoaDonPrinter } from '@/composables/useHoaDonPrinter';
import { GIOI_TINH_OPTIONS } from '@/constants/appConstants';
import { isActiveStatus } from '@/utils/statusUtils';

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
const loading = ref(false);
const orders = ref([]);
const activeOrderIndex = ref(0);
const vouchers = ref([]);
const isProcessing = ref(false);

// Redesigned Page Custom State
const orderWarehouse = ref('KHO ANSHA BIGSIZE');

// Dynamic Filter States for POS Products
const maxProductPrice = ref(7000000);

const customerSearch = ref('');
const customerResults = ref([]);
const customerLoading = ref(false);
const showQuickAddDialog = ref(false);
const quickAddLoading = ref(false);
const quickAddForm = ref({ ten: '', sdt: '', email: '', gioiTinh: true, tinh: null, thanhPho: null, phuongXa: null, diaChiChiTiet: '' });

// Right Column Fields
const currentEmployeeDetail = ref(null);


const customerForm = ref({
    ten: '',
    sdt: '',
    email: '',
    gioiTinh: 'Giới tính',
    ngaySinh: '',
    tongDonHang: 0
});

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
        return selectedOrder.value.isGiaoHangLocal ? 'Trực tuyến' : 'Tại quầy';
    },
    set(newVal) {
        if (selectedOrder.value) {
            selectedOrder.value.isGiaoHangLocal = (newVal === 'Trực tuyến');
            selectedOrder.value.loaiDon = selectedOrder.value.isGiaoHangLocal ? 'ONLINE' : 'TAI_QUAY';
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
            selectedOrder.value.loaiDon = val ? 'ONLINE' : 'TAI_QUAY';
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
            const loaiDon = isGiaoHang.value ? 'ONLINE' : 'TAI_QUAY';
            const shipFee = loaiDon === 'ONLINE' && !isFreeShip.value ? Number(shippingFee.value || 0) : 0;

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

const getVoucherDiscountLabel = (voucher) => {
    if (!voucher) return '';
    const isPercent = String(voucher.loaiPhieu || '').toUpperCase() === 'PHAN_TRAM' || String(voucher.loaiPhieu || '').toUpperCase() === 'PERCENT';
    if (isPercent) {
        return `${voucher.phanTramGiamGia || 0}%`;
    } else {
        const cash = Number(voucher.soTienGiam || 0);
        if (cash >= 1000) {
            return `${Math.round(cash / 1000)}k`;
        }
        return `${cash}đ`;
    }
};

const getVoucherBaseAmount = (order = selectedOrder.value) =>
    Number(order?.tongTien ?? amountAfterProductDiscount.value ?? 0);

const getVoucherCode = (voucher) => voucher?.ma || voucher?.maPhieu || voucher?.tenPhieu || voucher?.ten || 'Phiếu giảm giá';

const getVoucherFreshTime = (voucher) => {
    const raw = voucher?.ngayTao || voucher?.createdAt || voucher?.createdDate || voucher?.ngayBatDau || 0;
    const numeric = Number(raw);
    if (Number.isFinite(numeric)) return numeric;
    const parsedDate = Date.parse(raw);
    return Number.isFinite(parsedDate) ? parsedDate : 0;
};

const getPotentialDiscount = (v, baseAmount = getVoucherBaseAmount()) => {
    if (!v) return 0;
    const amount = Math.max(0, Number(baseAmount || 0));
    const type = String(v.loaiPhieu || '').toUpperCase();
    if (type === 'PHAN_TRAM' || type === 'PERCENT') {
        const percent = Number(v.phanTramGiamGia || 0);
        let discount = (amount * percent) / 100;
        if (v.giamToiDa && Number(v.giamToiDa) > 0) {
            discount = Math.min(discount, Number(v.giamToiDa));
        }
        return Math.min(amount, Math.max(0, discount));
    } else {
        return Math.min(amount, Math.max(0, Number(v.soTienGiam || 0)));
    }
};

const isVoucherEligibleForAmount = (voucher, amount) =>
    Number(voucher?.donHangToiThieu || 0) <= Number(amount || 0);

// Chọn PGG tốt nhất theo đúng tổng tiền đang xét: giảm nhiều nhất, nếu bằng nhau ưu tiên phiếu mới hơn.
const pickBestVoucherForOrder = (order = selectedOrder.value, source = vouchers.value) => {
    if (!source?.length || !order) return null;
    const currentTotal = getVoucherBaseAmount(order);
    const eligible = source.filter((voucher) => isVoucherEligibleForAmount(voucher, currentTotal));
    if (!eligible.length) return null;

    return [...eligible].sort((a, b) => {
        const discA = getPotentialDiscount(a, currentTotal);
        const discB = getPotentialDiscount(b, currentTotal);
        if (discB !== discA) return discB - discA;
        return getVoucherFreshTime(b) - getVoucherFreshTime(a);
    })[0];
};

const appliedVoucher = computed(() => {
    if (!vouchers.value?.length || !selectedOrder.value?.idPhieuGiamGia) {
        return null;
    }
    return vouchers.value.find(v => String(v.id) === String(selectedOrder.value.idPhieuGiamGia)) || null;
});

const bestEligibleVoucher = computed(() => {
    return pickBestVoucherForOrder();
});

const nextBetterVoucher = computed(() => {
    if (!vouchers.value?.length || !selectedOrder.value) return null;

    const currentTotal = getVoucherBaseAmount();
    const eligibleDiscount = bestEligibleVoucher.value ? getPotentialDiscount(bestEligibleVoucher.value, currentTotal) : 0;

    const candidates = vouchers.value.filter(v => {
        const minOrder = Number(v.donHangToiThieu || 0);
        if (minOrder <= currentTotal) return false;

        const potDiscount = getPotentialDiscount(v, minOrder);
        return potDiscount > eligibleDiscount;
    });

    if (!candidates.length) return null;

    // Gợi ý phiếu tốt nhất dựa trên giá trị giảm giá tiềm năng lớn nhất, sau đó mới đến đơn tối thiểu nhỏ nhất để tối ưu quyền lợi cho khách hàng
    candidates.sort((a, b) => {
        const discA = getPotentialDiscount(a, Number(a.donHangToiThieu || 0));
        const discB = getPotentialDiscount(b, Number(b.donHangToiThieu || 0));
        if (discB !== discA) return discB - discA;
        return Number(a.donHangToiThieu || 0) - Number(b.donHangToiThieu || 0);
    });

    return candidates[0];
});

const remainingForSuggestedVoucher = computed(() => {
    if (!nextBetterVoucher.value || !selectedOrder.value) return 0;
    const minVal = Number(nextBetterVoucher.value.donHangToiThieu || 0);
    const current = getVoucherBaseAmount();
    return Math.max(0, minVal - current);
});

// Dong goi cau goi y PGG cho UI: tu dong chon ma tot nhat, dong thoi bao ma tiep theo neu chua du dieu kien.
const voucherSuggestionText = computed(() => {
    if (!selectedOrder.value?.listsHoaDonChiTiet?.length) return '';

    const currentTotal = getVoucherBaseAmount();
    const best = bestEligibleVoucher.value;
    const applied = appliedVoucher.value;

    if (best?.id && applied?.id && String(best.id) === String(applied.id)) {
        return `Đã áp dụng mã giảm giá ưu đãi nhất: ${getVoucherCode(best)} (-${formatCurrency(getPotentialDiscount(best, currentTotal))})`;
    }

    if (best?.id) {
        return `Bấm để áp dụng mã giảm giá ưu đãi nhất: ${getVoucherCode(best)} (-${formatCurrency(getPotentialDiscount(best, currentTotal))})`;
    }

    return nextBetterVoucher.value ? '' : 'Chưa có phiếu giảm giá phù hợp cho đơn hiện tại.';
});

// Goi y upsell: neu co phieu giam gia tot hon nhung don chua du muc toi thieu thi hien can mua them bao nhieu.
const betterVoucherSuggestionText = computed(() => {
    if (!selectedOrder.value?.listsHoaDonChiTiet?.length || !nextBetterVoucher.value) return '';
    const currentTotal = getVoucherBaseAmount();
    const futureBase = Math.max(currentTotal, Number(nextBetterVoucher.value.donHangToiThieu || 0));
    return `Mua thêm ${formatCurrency(remainingForSuggestedVoucher.value)} để nhận phiếu tốt hơn: ${getVoucherCode(nextBetterVoucher.value)} (-${formatCurrency(getPotentialDiscount(nextBetterVoucher.value, futureBase))})`;
});

const voucherSuggestionClass = computed(() =>
    bestEligibleVoucher.value ? 'text-success' : (nextBetterVoucher.value ? 'text-deep-orange-darken-3' : 'text-grey-darken-1')
);

const canApplySuggestedVoucher = computed(() => {
    const best = bestEligibleVoucher.value;
    if (!best?.id || !selectedOrder.value?.id) return false;
    return String(selectedOrder.value.idPhieuGiamGia || '') !== String(best.id);
});

const isVoucherAutoApplied = ref({});

// Search Customer Dropdown
const searchCustomers = async () => {
    const kw = customerSearch.value?.trim();
    if (!kw || kw.length < 2) {
        customerResults.value = [];
        return;
    }
    customerLoading.value = true;
    try {
        const data = await dichVuDonHang.searchKhachHang(kw);
        customerResults.value = data || [];
    } catch (e) {
        console.error(e);
    } finally {
        customerLoading.value = false;
    }
};

let customerDebounce = null;
watch(customerSearch, () => {
    if (customerDebounce) clearTimeout(customerDebounce);
    customerDebounce = setTimeout(() => {
        searchCustomers();
    }, 300);
});

const selectCustomer = async (customer) => {
    if (!customer?.id) return;
    try {
        const updated = await dichVuDonHang.setKhachHang(selectedOrder.value.id, customer.id);
        updateOrderInList(updated);
        refreshBestVoucher(updated);
        customerSearch.value = '';
        customerResults.value = [];
    } catch (e) {
        addNotification({ title: 'Lỗi khách hàng', subtitle: 'Không thể gắn khách hàng vào hóa đơn.', color: 'error' });
    }
};
const showCustomerSuggestions = ref(false);

const onCustomerInput = () => {
    const searchVal = (customerForm.value.ten || '') + ' ' + (customerForm.value.sdt || '');
    customerSearch.value = searchVal.trim();
    showCustomerSuggestions.value = true;
};

const onSelectSuggestedCustomer = async (c) => {
    showCustomerSuggestions.value = false;
    await selectCustomer(c);
};

// Nhận dữ liệu khách hàng nhân viên nhập trong panel bên phải.
// Component con dùng form nội bộ, còn màn chính giữ state này để lưu khách mới khi thanh toán.
const onCustomerFormUpdate = (form) => {
    customerForm.value = { ...customerForm.value, ...(form || {}) };
};

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

const ensureCustomerAndGetId = async () => {
    if (selectedOrder.value?.idKhachHang) {
        return selectedOrder.value.idKhachHang;
    }

    const ten = customerForm.value.ten?.trim();
    const sdt = customerForm.value.sdt?.trim();
    const email = customerForm.value.email?.trim() || null;
    const ns = customerForm.value.ngaySinh?.trim() || null;
    const gt = customerForm.value.gioiTinh === 'Nam' ? true : customerForm.value.gioiTinh === 'Nữ' ? false : null;

    if (!ten && !sdt) {
        return null;
    }

    if (!ten || !sdt) {
        throw new Error('Vui lòng điền đầy đủ Tên khách hàng và SĐT để thêm khách hàng mới.');
    }

    const existed = await findExistingCustomerByContact(sdt, email);
    if (existed) {
        await selectCustomer(existed);
        return existed.id;
    }

    const newCustomerPayload = {
        ten,
        sdt,
        email,
        gioiTinh: gt,
        ngaySinh: ns
    };

    const created = await dichVuKhachHang.taoKhachHang(newCustomerPayload);
    if (created?.id) {
        await selectCustomer(created);
        return created.id;
    }
    return null;
};


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
        try {
            if (!selectedOrder.value?.id) return;
            await dichVuDonHang.removeSanPham(selectedOrder.value.id, item.id);
            const data = await dichVuDonHang.layDonHangCho();
            setOrders(data, { preferOrderId: selectedOrder.value.id });
            refreshBestVoucher();
        } catch (e) {
            addNotification({ title: 'Lỗi', subtitle: MESSAGES.ERROR.DELETE_DATA, color: 'error' });
        }
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

const onRemoveCustomer = async () => {
    try {
        const updated = await dichVuDonHang.setKhachHang(selectedOrder.value.id, null);
        updateOrderInList(updated);
        refreshBestVoucher(updated);
        addNotification({ title: 'Thêm khách hàng thành công.', subtitle: 'Đã gỡ khách hàng khỏi hóa đơn.', color: 'success' });
    } catch (e) {
        addNotification({ title: 'Không thêm được khách hàng.', subtitle: getErrorMessage(e, 'Không thể gỡ khách hàng khỏi hóa đơn.'), color: 'error' });
    }
};

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

        // Kiểm tra xem phiếu giảm giá hiện đang áp dụng có còn thỏa mãn điều kiện tối thiểu không
        const currentAppliedVoucherId = order.idPhieuGiamGia;
        const appliedVoucherObj = decorated.find(v => String(v.id) === String(currentAppliedVoucherId));
        const isAppliedStillEligible = appliedVoucherObj && isVoucherEligibleForAmount(appliedVoucherObj, getVoucherBaseAmount(order));

        // Nếu phiếu do người dùng tự chọn đã hết hiệu lực, tự động hoàn tác về trạng thái tự động chọn
        if (currentAppliedVoucherId && !isAppliedStillEligible) {
            isVoucherAutoApplied.value[order.id] = true;
        }

        if (autoApply) {
            // Chỉ tự động chọn nếu người dùng chưa chọn thủ công, hoặc đã bị thu hồi do hết hiệu lực ở trên
            const shouldAutoApply = isVoucherAutoApplied.value[order.id] !== false;
            if (shouldAutoApply) {
                isVoucherAutoApplied.value[order.id] = true;
                const best = pickBestVoucherForOrder(order, decorated);
                if (best?.id) {
                    if (String(order.idPhieuGiamGia) !== String(best.id)) {
                        await onApplyVoucher(best.id, false, true);
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
    const best = bestEligibleVoucher.value;
    if (!best?.id) return;
    await onApplyVoucher(best.id);
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
            amount: finalCollectAmount.value,
            paymentUrl: data.paymentUrl,
            pollInterval: null
        };

        vnpayDialog.value.pollInterval = setInterval(async () => {
            // QR mode khong co popup de doc callback, nen chi cho phep thu ngan xac nhan da nhan tien.
            try {
                await Promise.resolve();
            } catch (e) {
                // Keep QR waiting screen alive.
            }
        }, 1000);

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

// Customer Dialog Add Quick
const resetQuickAddForm = () => {
    quickAddForm.value = { ten: '', sdt: '', email: '', gioiTinh: true, tinh: null, thanhPho: null, phuongXa: null, diaChiChiTiet: '' };
};

const openQuickAddDialog = async () => {
    resetQuickAddForm();
    if (provinces.value.length === 0) {
        await fetchProvinces();
    }
    showQuickAddDialog.value = true;
};

const findExactCustomer = (list, phone, email) => {
    const normalizedPhone = String(phone ?? '').replace(/\D/g, '');
    const normalizedEmail = String(email ?? '').trim().toLowerCase();

    return list.find((customer) => {
        const customerPhone = String(customer?.sdt ?? '').replace(/\D/g, '');
        const customerEmail = String(customer?.email ?? '').trim().toLowerCase();

        if (normalizedPhone && customerPhone === normalizedPhone) return true;
        if (normalizedEmail && customerEmail === normalizedEmail) return true;
        return false;
    });
};

const findExistingCustomerByContact = async (phone, email) => {
    const keywords = [phone, email].filter(Boolean);
    for (const keyword of keywords) {
        try {
            const searchData = await dichVuDonHang.searchKhachHang(keyword);
            const matched = findExactCustomer(Array.isArray(searchData) ? searchData : [], phone, email);
            if (matched) return matched;
        } catch (e) {
            console.error(e);
        }
    }
    const allCustomers = await dichVuKhachHang.layTatCaKhachHang();
    // BE trả PageResponse chuẩn ({ content: [...] }) nên chỉ cần đọc .content
    const content = allCustomers?.content || [];
    return findExactCustomer(content, phone, email) || null;
};

const submitQuickAdd = async () => {
    const phone = quickAddForm.value.sdt?.trim() || '';
    const email = quickAddForm.value.email?.trim() || '';
    const name = quickAddForm.value.ten?.trim() || '';

    if (!phone || !name) {
        addNotification({ title: 'Thiếu thông tin', subtitle: 'Vui lòng nhập Tên và Số điện thoại.', color: 'warning' });
        return;
    }

    const phoneRegex = /^(0[3|5|7|8|9])[0-9]{8}$/;
    if (!phoneRegex.test(phone)) {
        addNotification({ title: 'SĐT không hợp lệ', subtitle: 'Số điện thoại phải có 10 số và bắt đầu bằng 03, 05, 07, 08, hoặc 09.', color: 'warning' });
        return;
    }

    quickAddLoading.value = true;
    try {
        const existedCustomer = await findExistingCustomerByContact(phone, email);
        if (existedCustomer) {
            await selectCustomer(existedCustomer);
            addNotification({
                title: 'Đã tìm thấy',
                subtitle: `Khách hàng này đã tồn tại, tự động gán khách hàng ${existedCustomer.ten || existedCustomer.sdt}`,
                color: 'success'
            });
            showQuickAddDialog.value = false;
            return;
        }

        const newCustomerPayload = {
            ten: name,
            sdt: phone,
            email: email,
            gioiTinh: quickAddForm.value.gioiTinh,
            tenTaiKhoan: '',
            matKhau: '',
            trangThai: 'DANG_HOAT_DONG',
            ghiChu: 'Khách tạo nhanh tại quầy'
        };

        const createdCustomer = await dichVuKhachHang.taoKhachHang(newCustomerPayload);

        if (createdCustomer?.id && quickAddForm.value.tinh && quickAddForm.value.thanhPho && quickAddForm.value.phuongXa && quickAddForm.value.diaChiChiTiet) {
            try {
                const addressPayload = mapCodesToNames(quickAddForm.value, provinces.value, districts.value, wards.value);
                await dichVuKhachHang.taoDiaChi({
                    idKhachHang: createdCustomer.id,
                    tinh: addressPayload.tinh,
                    thanhPho: addressPayload.thanhPho,
                    phuongXa: addressPayload.phuongXa,
                    diaChiChiTiet: addressPayload.diaChiChiTiet,
                    tenNguoiNhan: name,
                    sdtNguoiNhan: phone,
                    laMacDinh: true
                });
            } catch (err) {
                console.error("Lỗi tạo địa chỉ", err);
            }
        }

        const targetCustomer = createdCustomer?.id ? createdCustomer : await findExistingCustomerByContact(phone, email);
        if (targetCustomer) {
            await selectCustomer(targetCustomer);
            addNotification({ title: 'Thành công', subtitle: 'Đã thêm khách mới vào hóa đơn.', color: 'success' });
        } else {
            addNotification({ title: 'Tạo thành công', subtitle: 'Đã thêm khách mới.', color: 'info' });
        }
        showQuickAddDialog.value = false;
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: e?.response?.data?.message || 'Không thể tạo khách hàng.', color: 'error' });
    } finally {
        quickAddLoading.value = false;
    }
};

const formatCurrency = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

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
                    <CustomerAndShippingPanel :order="selectedOrder" :is-giao-hang="isGiaoHang"
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
                        @update:customer-form="onCustomerFormUpdate"
                        @update:shipping="onShippingPanelUpdate" />

                    <!-- Pricing/Voucher Details (Moved from left column) -->
                    <OrderSummaryPanel v-model:isGiaoHang="isGiaoHang" :vouchers="vouchers"
                        class="flex-shrink-0"
                        :selected-voucher-id="selectedOrder?.idPhieuGiamGia"
                        :voucher-suggestion-text="voucherSuggestionText"
                        :voucher-suggestion-class="voucherSuggestionClass"
                        :can-apply-suggested-voucher="canApplySuggestedVoucher"
                        :better-voucher-suggestion-text="betterVoucherSuggestionText"
                        :total-raw-amount="totalRawAmount" :product-discount-amount="productDiscountAmount"
                        :applied-discount-summary="appliedDiscountSummary"
                        :total-discount-amount="totalDiscountAmount" :final-collect-amount="finalCollectAmount"
                        v-model:shippingFee="shippingFee" :shipping-fee-loading="shippingFeeLoading"
                        :shipping-fee-source="shippingFeeSource" :shipping-fee-error="shippingFeeError"
                        :is-free-ship="isFreeShip" @apply-voucher="onApplyVoucher"
                        @apply-suggested-voucher="applyBestVoucherFromSuggestion" />

                    <!-- Payment Card -->
                    <PaymentPanel v-model:paymentMethod="checkoutData.paymentMethod"
                        class="flex-shrink-0"
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
                        <div class="text-body-2 font-weight-medium text-grey-darken-2">{{ vnpayDialog.statusText }}
                        </div>
                    </div>

                    <div v-else-if="vnpayDialog.verified" class="pa-8 d-flex flex-column align-center">
                        <v-icon color="success" size="64" class="mb-4">mdi-check-circle</v-icon>
                        <div class="text-h6 font-weight-bold text-success mb-2">Giao dịch thành công!</div>
                        <div class="text-body-2 text-grey-darken-1">Đơn hàng đang được hoàn tất...</div>
                    </div>

                    <div v-else class="w-100 d-flex flex-column align-center">
                        <template v-if="checkoutData.vnpayMethod === 'QR'">
                            <div class="pa-2 bg-white rounded-lg elevation-2 mb-4 d-inline-block">
                                <QrcodeVue v-if="vnpayDialog.paymentUrl" :value="vnpayDialog.paymentUrl" :size="220"
                                    level="H" render-as="canvas" />
                                <div v-else class="d-flex align-center justify-center text-grey"
                                    style="width: 220px; height: 220px;">
                                    Chưa có mã QR
                                </div>
                            </div>
                            <div class="text-h5 font-weight-bold text-error mb-1">
                                {{ new Intl.NumberFormat('vi-VN', {
                                    style: 'currency', currency: 'VND'
                                }).format(vnpayDialog.amount) }}
                            </div>
                            <div class="text-caption text-grey-darken-1 mb-6 px-4 text-center">
                                Sử dụng ứng dụng ngân hàng hoặc ví VNPay để quét mã.
                            </div>
                        </template>
                        <template v-else>
                            <div class="text-h5 font-weight-bold text-error mb-4">
                                {{ new Intl.NumberFormat('vi-VN', {
                                    style: 'currency', currency: 'VND'
                                }).format(vnpayDialog.amount) }}
                            </div>
                            <div class="text-caption text-grey-darken-1 mb-6 px-4 text-center">
                                Vui lòng hoàn tất thanh toán trên VNPay.
                            </div>
                            <v-btn color="#005BAA" class="mb-6 rounded-lg text-white font-weight-bold"
                                @click="() => { vnpayPopup = window.open(vnpayDialog.paymentUrl, 'vnpay', 'width=800,height=600'); }">
                                Mở lại thanh toán
                            </v-btn>
                        </template>

                        <v-btn block color="#005BAA" class="mb-3 rounded-lg text-white font-weight-bold" height="48"
                            @click="onConfirmVnPayManual">
                            XÁC NHẬN ĐÃ NHẬN TIỀN
                        </v-btn>

                        <v-btn v-if="checkoutData.vnpayMethod === 'QR'" block variant="outlined" color="grey-darken-1"
                            class="rounded-lg font-weight-bold" height="48" @click="startVnPayFlow">
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
                    <v-btn class="flex-grow-1 rounded-lg font-weight-bold" variant="outlined" color="grey-darken-1"
                        height="44" @click="vnpayChoiceDialog.show = false">
                        Hủy
                    </v-btn>
                    <v-btn class="flex-grow-1 rounded-lg font-weight-bold text-white" color="#4285F4" height="44"
                        @click="proceedVnPayChoice">
                        Tiếp tục
                    </v-btn>
                </div>
            </v-card>
        </v-dialog>

        <!-- Scanner dialog -->
        <v-dialog v-model="showScanner" max-width="500" transition="dialog-bottom-transition">
            <v-card class="rounded-lg pa-4">
                <div class="d-flex justify-space-between align-center mb-4">
                    <span class="text-h6 font-weight-bold">Quét mã sản phẩm</span>
                    <v-btn icon variant="text" @click="() => stopScanner()">
                        <XIcon />
                    </v-btn>
                </div>
                <div :id="scannerElementId" class="qr-reader-box"></div>
                <div class="mt-4 text-center text-caption text-grey">Đưa mã QR hoặc Barcode của sản phẩm vào
                    khung
                    hình
                </div>
            </v-card>
        </v-dialog>

        <!-- Quick Add Customer Dialog -->
        <v-dialog v-model="showQuickAddDialog" max-width="650" transition="dialog-bottom-transition" persistent>
            <v-card class="rounded-lg overflow-hidden">
                <v-card-title
                    class="text-subtitle-1 font-weight-bold pa-4 border-b bg-slate-50 d-flex justify-space-between align-center">
                    Thêm nhanh thông tin khách hàng
                    <v-btn icon size="small" variant="text" @click="showQuickAddDialog = false">
                        <XIcon size="20" />
                    </v-btn>
                </v-card-title>
                <v-card-text class="pa-5">
                    <div class="text-body-2 text-medium-emphasis mb-4">
                        Nếu SĐT đã tồn tại, hệ thống sẽ tự động nhận diện và gán khách hàng vào đơn.
                    </div>

                    <v-row dense>
                        <v-col cols="12" md="6">
                            <v-text-field v-model="quickAddForm.ten" label="Tên khách hàng" placeholder="Nhập tên..."
                                variant="outlined" density="comfortable" hide-details="auto" class="mb-3 text-body-2"
                                maxlength="100" />
                        </v-col>
                        <v-col cols="12" md="6">
                            <v-text-field v-model="quickAddForm.sdt" label="Số điện thoại"
                                placeholder="Ví dụ: 0912345678" variant="outlined" density="comfortable"
                                hide-details="auto" class="mb-3 text-body-2"
                                @input="quickAddForm.sdt = String($event.target.value || '').replace(/[^0-9]/g, '')" />
                        </v-col>
                        <v-col cols="12" md="6">
                            <v-select v-model="quickAddForm.gioiTinh" :items="GIOI_TINH_OPTIONS" label="Giới tính"
                                variant="outlined" density="comfortable" hide-details="auto" class="mb-3 text-body-2" />
                        </v-col>
                        <v-col cols="12" md="6">
                            <v-text-field v-model="quickAddForm.email" label="Email (Không bắt buộc)"
                                placeholder="Ví dụ: abc@gmail.com" variant="outlined" density="comfortable"
                                hide-details="auto" class="mb-3 text-body-2" />
                        </v-col>

                        <v-col cols="12">
                            <div class="text-subtitle-2 font-weight-bold mt-2 mb-2 text-slate-700">Địa chỉ
                                (Tùy
                                chọn)
                            </div>
                        </v-col>
                        <v-col cols="12" md="4">
                            <v-autocomplete v-model="quickAddForm.tinh" :items="provinces" item-title="name"
                                item-value="code" placeholder="Tỉnh / Thành phố" variant="outlined" bg-color="white"
                                density="compact" hide-details :loading="loadingLocations.provinces"
                                @update:model-value="(val) => { quickAddForm.thanhPho = null; quickAddForm.phuongXa = null; if (val) fetchDistricts(val); }"
                                class="mb-3 text-body-2" />
                        </v-col>
                        <v-col cols="12" md="4">
                            <v-autocomplete v-model="quickAddForm.thanhPho" :items="districts" item-title="name"
                                item-value="code" placeholder="Quận / Huyện" variant="outlined" bg-color="white"
                                density="compact" hide-details :loading="loadingLocations.districts"
                                :disabled="!quickAddForm.tinh"
                                @update:model-value="(val) => { quickAddForm.phuongXa = null; if (val) fetchWards(val); }"
                                class="mb-3 text-body-2" />
                        </v-col>
                        <v-col cols="12" md="4">
                            <v-autocomplete v-model="quickAddForm.phuongXa" :items="wards" item-title="name"
                                item-value="code" placeholder="Phường / Xã" variant="outlined" bg-color="white"
                                density="compact" hide-details :loading="loadingLocations.wards"
                                :disabled="!quickAddForm.thanhPho" class="mb-3 text-body-2" />
                        </v-col>
                        <v-col cols="12">
                            <v-text-field v-model="quickAddForm.diaChiChiTiet"
                                placeholder="Địa chỉ cụ thể (Số nhà, đường...)" variant="outlined" density="compact"
                                hide-details class="text-body-2" />
                        </v-col>
                    </v-row>
                </v-card-text>
                <v-card-actions class="px-6 pb-5 border-t bg-slate-50">
                    <v-spacer />
                    <v-btn variant="tonal" color="slate-500" class="rounded-lg text-none"
                        @click="showQuickAddDialog = false">Hủy</v-btn>
                    <v-btn :loading="quickAddLoading" color="primary" variant="flat"
                        class="px-6 rounded-lg font-weight-bold text-none" @click="submitQuickAdd"> Thêm
                        nhanh
                    </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

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
