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
import { Html5QrcodeScanner } from 'html5-qrcode';
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
import { MESSAGES } from '@/constants/messages';
import { useUIStore } from '@/stores/ui';
import { useAuthStore } from '@/stores/authStore';
import { useLocation } from '@/composables/useLocation';
import { useAddressMapping } from '@/composables/useAddressMapping';
import { GIOI_TINH_OPTIONS } from '@/constants/appConstants';
import { isActiveStatus } from '@/utils/statusUtils';

// Import Components
import OrderTabs from './components/OrderTabs.vue';
import CartTable from './components/CartTable.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import InvoiceReceiptDialog from './components/InvoiceReceiptDialog.vue';
import GiaoCaModal from '@/components/common/GiaoCaModal.vue';
import { dichVuGiaoCa } from '@/services/admin/dichVuGiaoCa';

const { addNotification } = useNotifications();
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
const { mapCodesToNames } = useAddressMapping();

// Separate instance for shipping to prevent quick-add conflicts
const {
    provinces: provincesShip,
    districts: districtsShip,
    wards: wardsShip,
    fetchProvinces: fetchProvincesShip,
    fetchDistricts: fetchDistrictsShip,
    fetchWards: fetchWardsShip
} = useLocation();

// State
const loading = ref(false);
const orders = ref([]);
const activeOrderIndex = ref(0);
const vouchers = ref([]);
const isProcessing = ref(false);

// Redesigned Page Custom State
const orderWarehouse = ref('KHO ANSHA BIGSIZE');

const productSearchKeyword = ref('');
const productSearchResults = ref([]);
const productSearchLoading = ref(false);
const showProductAutocomplete = ref(false);
const onlyInStock = ref(false);

// Dynamic Filter States for POS Products
const filterThuongHieu = ref('ALL');
const filterMucDich = ref('ALL');
const filterKhoangGia = ref('ALL');
const filterMauSac = ref('ALL');
const filterKichCo = ref('ALL');

const filterBrands = ref([]);
const filterPurposes = ref([]);
const filterColors = ref([]);
const filterSizes = ref([]);
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

// Order Value Adjustments
const shippingFee = ref(30000);
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
        const variants = await dichVuDonHang.searchSanPham(keyword);
        if (variants && variants.length > 0) {
            const exactMatch = variants.find((v) => v.maChiTietSanPham === keyword) || variants[0];
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
        }
    }
});

// Order Value Calculations
const originalTotalAmount = computed(() => {
    const items = selectedOrder.value?.listsHoaDonChiTiet || [];
    return items.reduce((sum, item) => {
        const qty = Number(item.soLuong || 0);
        const price = Number(item.donGia || 0);
        const percent = Number(item.phanTramGiam || 0);
        if (percent > 0 && percent < 100) {
            const originalPrice = price / (1 - percent / 100);
            return sum + (originalPrice * qty);
        }
        return sum + (price * qty);
    }, 0);
});

const discountAmount = computed(() => {
    const raw = originalTotalAmount.value;
    const after = Number(selectedOrder.value?.tongTienSauGiam || selectedOrder.value?.tongTien || 0);
    return Math.max(0, raw - after);
});

// Danh sách phần trăm giảm (duy nhất) của các sản phẩm trong đơn
const appliedDiscountPercents = computed(() => {
    const items = selectedOrder.value?.listsHoaDonChiTiet || [];
    const percents = items
        .map((item) => Number(item.phanTramGiam) || 0)
        .filter((percent) => percent > 0);
    return [...new Set(percents)].sort((a, b) => b - a);
});

const finalCollectAmount = computed(() => {
    const after = Number(selectedOrder.value?.tongTienSauGiam || selectedOrder.value?.tongTien || 0);
    const ship = selectedOrder.value?.loaiDon === 'ONLINE' ? Number(shippingFee.value || 0) : 0;
    return Math.max(0, after + ship + Number(surcharge.value || 0));
});

const remainingBalance = computed(() => {
    const received = Number(checkoutData.value.receivedAmount || 0);
    return Math.max(0, finalCollectAmount.value - received);
});

const changeAmount = computed(() => {
    const received = Number(checkoutData.value.receivedAmount || 0);
    return Math.max(0, received - finalCollectAmount.value);
});

// Sync shipping fee when channel or free ship changes
const lastActiveOrderId = ref(selectedOrder.value?.id || null);

// Watcher when active order changes, update note, shipping fee and reset search
watch(() => selectedOrder.value?.id, (id) => {
    productSearchKeyword.value = '';
    showProductAutocomplete.value = false;
    checkoutData.value.note = selectedOrder.value?.ghiChu || '';

    if (id) {
        if (selectedOrder.value.phiVanChuyen !== undefined && selectedOrder.value.phiVanChuyen !== null) {
            shippingFee.value = Number(selectedOrder.value.phiVanChuyen);
            isFreeShip.value = Number(selectedOrder.value.phiVanChuyen) === 0 && selectedOrder.value.loaiDon === 'ONLINE';
        } else {
            const channel = selectedOrder.value.loaiDon === 'ONLINE' ? 'Trực tuyến' : 'Tại quầy';
            shippingFee.value = channel === 'Trực tuyến' ? 30000 : 0;
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
    } else {
        if (oldFreeShip === true && !freeShip) {
            shippingFee.value = channel === 'Trực tuyến' ? 30000 : 0;
        } else if (channel !== oldChannel && oldChannel !== undefined) {
            shippingFee.value = channel === 'Trực tuyến' ? 30000 : 0;
        }
    }
}, { immediate: true });

// Automatically uncheck Free Ship if a non-zero shipping fee is typed/selected
watch(shippingFee, (newVal) => {
    if (Number(newVal) > 0) {
        isFreeShip.value = false;
    }
});

// Sync onlyChargeIfReturned when channel changes
watch(orderChannel, (channel) => {
    onlyChargeIfReturned.value = channel === 'Tại quầy';
});

// Sync loaiDon when onlyChargeIfReturned changes manually
watch(onlyChargeIfReturned, (val) => {
    if (selectedOrder.value) {
        selectedOrder.value.loaiDon = val ? 'TAI_QUAY' : 'ONLINE';
    }
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

const getPotentialDiscount = (v) => {
    if (!v) return 0;
    const type = String(v.loaiPhieu || '').toUpperCase();
    if (type === 'PHAN_TRAM' || type === 'PERCENT') {
        const minOrder = Number(v.donHangToiThieu || 0);
        const percent = Number(v.phanTramGiamGia || 0);
        let discount = (minOrder * percent) / 100;
        if (v.giamToiDa && Number(v.giamToiDa) > 0) {
            discount = Math.min(discount, Number(v.giamToiDa));
        }
        return discount;
    } else {
        return Number(v.soTienGiam || 0);
    }
};

const appliedVoucher = computed(() => {
    if (!vouchers.value?.length || !selectedOrder.value?.idPhieuGiamGia) {
        return null;
    }
    return vouchers.value.find(v => String(v.id) === String(selectedOrder.value.idPhieuGiamGia)) || null;
});

const bestEligibleVoucher = computed(() => {
    if (!vouchers.value?.length || !selectedOrder.value) return null;

    const currentTotal = Number(selectedOrder.value.tongTien || 0);

    const eligible = vouchers.value.filter(v => {
        const minOrder = Number(v.donHangToiThieu || 0);
        return minOrder <= currentTotal;
    });

    if (!eligible.length) return null;

    eligible.sort((a, b) => {
        const discA = getPotentialDiscount(a);
        const discB = getPotentialDiscount(b);
        return discB - discA;
    });

    return eligible[0];
});

const nextBetterVoucher = computed(() => {
    if (!vouchers.value?.length || !selectedOrder.value) return null;

    const currentTotal = Number(selectedOrder.value.tongTien || 0);
    const eligibleDiscount = bestEligibleVoucher.value ? getPotentialDiscount(bestEligibleVoucher.value) : 0;

    const candidates = vouchers.value.filter(v => {
        const minOrder = Number(v.donHangToiThieu || 0);
        if (minOrder <= currentTotal) return false;

        const potDiscount = getPotentialDiscount(v);
        return potDiscount > eligibleDiscount;
    });

    if (!candidates.length) return null;

    // Gợi ý mốc tiếp theo gần nhất
    candidates.sort((a, b) => Number(a.donHangToiThieu || 0) - Number(b.donHangToiThieu || 0));

    return candidates[0];
});

const remainingForSuggestedVoucher = computed(() => {
    if (!nextBetterVoucher.value || !selectedOrder.value) return 0;
    const minVal = Number(nextBetterVoucher.value.donHangToiThieu || 0);
    const current = Number(selectedOrder.value.tongTien || 0);
    return Math.max(0, minVal - current);
});

const isVoucherAutoApplied = ref({});

const fetchProductSearchResults = async (keyword) => {
    productSearchLoading.value = true;
    try {
        const params = {
            keyword: keyword || '',
            thuongHieu: filterThuongHieu.value,
            mucDich: filterMucDich.value,
            onlyInStock: onlyInStock.value
        };
        const res = await dichVuDonHang.searchSanPham(params);
        productSearchResults.value = res || [];
    } catch (e) {
        console.error('Lỗi khi tải sản phẩm:', e);
    } finally {
        productSearchLoading.value = false;
    }
};

const loadFilterOptions = async () => {
    try {
        const [th, md, ms, kt, maxPriceRes] = await Promise.allSettled([
            dichVuThuongHieu.layThuongHieu({ size: 1000 }),
            dichVuMucDichChay.layMucDichChay({ size: 1000 }),
            dichVuMauSac.layMauSac({ size: 1000 }),
            dichVuKichThuoc.layKichThuoc({ size: 1000 }),
            dichVuSanPham.layGiaLonNhat()
        ]);

        const pick = (res) => {
            if (res.status === 'fulfilled') {
                const val = res.value;
                if (Array.isArray(val)) return val;
                if (val && Array.isArray(val.content)) return val.content;
                if (val && Array.isArray(val.data)) return val.data;
            }
            return [];
        };

        filterBrands.value = [{ title: 'Thương hiệu', value: 'ALL' }, ...pick(th).map(x => ({ title: x.ten, value: x.ten }))];
        filterPurposes.value = [{ title: 'Mục đích', value: 'ALL' }, ...pick(md).map(x => ({ title: x.ten, value: x.ten }))];
        filterColors.value = [{ title: 'Màu sắc', value: 'ALL' }, ...pick(ms).map(x => ({ title: x.ten, value: x.ten }))];
        filterSizes.value = [{ title: 'Kích cỡ', value: 'ALL' }, ...pick(kt).map(x => ({ title: x.ten, value: x.ten }))];

        if (maxPriceRes.status === 'fulfilled' && maxPriceRes.value) {
            maxProductPrice.value = Number(maxPriceRes.value);
        }
    } catch (e) {
        console.error('Lỗi khi tải bộ lọc:', e);
    }
};

const priceRangeOptions = computed(() => {
    const list = [
        { title: 'Khoảng giá', value: 'ALL' },
        { title: '0 - 500.000đ', value: '0-500000', min: 0, max: 500000 }
    ];

    let currentMin = 500000;
    let currentMax = 1000000;
    const maxVal = maxProductPrice.value;

    while (currentMin < maxVal) {
        const minStr = new Intl.NumberFormat('vi-VN').format(currentMin);
        const maxStr = new Intl.NumberFormat('vi-VN').format(currentMax);
        list.push({
            title: `${minStr}đ - ${maxStr}đ`,
            value: `${currentMin}-${currentMax}`,
            min: currentMin,
            max: currentMax
        });
        currentMin = currentMax;
        currentMax = currentMin + 1000000;
    }
    return list;
});

const filteredProductSearchResults = computed(() => {
    let results = productSearchResults.value || [];

    // Filter by color (mauSac)
    if (filterMauSac.value !== 'ALL') {
        results = results.filter(p => p.tenMauSac === filterMauSac.value);
    }

    // Filter by size (kichCo / kichThuoc)
    if (filterKichCo.value !== 'ALL') {
        results = results.filter(p => p.tenKichThuoc === filterKichCo.value);
    }

    // Filter by price range (khoangGia)
    if (filterKhoangGia.value !== 'ALL') {
        const option = priceRangeOptions.value.find(o => o.value === filterKhoangGia.value);
        if (option) {
            results = results.filter(p => {
                const price = p.giaBan || 0;
                return price >= option.min && price <= option.max;
            });
        }
    }

    return results;
});

const onFilterChange = () => {
    showProductAutocomplete.value = true;
};

const resetFilters = () => {
    filterThuongHieu.value = 'ALL';
    filterMucDich.value = 'ALL';
    filterKhoangGia.value = 'ALL';
    filterMauSac.value = 'ALL';
    filterKichCo.value = 'ALL';
    productSearchKeyword.value = '';
    showProductAutocomplete.value = false;
};

let searchDebounce = null;
watch([
    productSearchKeyword,
    filterThuongHieu,
    filterMucDich,
    filterKhoangGia,
    filterMauSac,
    filterKichCo,
    onlyInStock
], () => {
    if (searchDebounce) clearTimeout(searchDebounce);
    searchDebounce = setTimeout(() => {
        fetchProductSearchResults(productSearchKeyword.value);
    }, 300);
});



const onProductSearchFocus = () => {
    showProductAutocomplete.value = true;
    fetchProductSearchResults(productSearchKeyword.value);
};

const onProductSearchBlur = () => {
    setTimeout(() => {
        showProductAutocomplete.value = false;
    }, 250);
};

const selectProductFromSearch = (variant) => {
    onAddProduct(variant);
    productSearchKeyword.value = '';
    showProductAutocomplete.value = false;
};

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

// QR / Barcode Scanner Logic
const showScanner = ref(false);
let html5QrcodeScanner = null;

const startScanner = () => {
    showScanner.value = true;
    setTimeout(() => {
        const el = document.getElementById('reader');
        if (!el || el.clientWidth === 0) {
            startScanner();
            return;
        }

        if (html5QrcodeScanner) {
            html5QrcodeScanner.clear().catch(e => console.error(e));
        }

        html5QrcodeScanner = new Html5QrcodeScanner('reader', { fps: 10, qrbox: { width: 250, height: 250 } }, false);
        html5QrcodeScanner.render(onScanSuccess, onScanFailure);
    }, 150);
};

const stopScanner = () => {
    if (html5QrcodeScanner) {
        html5QrcodeScanner.clear().catch((error) => console.error('Failed to clear scanner', error));
        html5QrcodeScanner = null;
    }
    showScanner.value = false;
};

const onScanSuccess = async (decodedText) => {
    stopScanner();
    const keyword = decodedText?.trim();
    if (!keyword) return;
    if (!selectedOrder.value) {
        addNotification({ title: 'Chưa có hóa đơn', subtitle: 'Vui lòng tạo hoặc chọn hóa đơn trước khi quét mã.', color: 'warning' });
        return;
    }

    try {
        const variants = await dichVuDonHang.searchSanPham(keyword);
        if (variants && variants.length > 0) {
            const exactMatch = variants.find((v) => v.maChiTietSanPham === keyword) || variants[0];

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
        await loadFilterOptions();
        const data = await dichVuDonHang.layDonHangCho();
        setOrders(data);

        // Tải danh sách phiếu giảm giá cho dropdown (BE là nguồn dữ liệu)
        try {
            const list = await dichVuDonHang.getVouchers(selectedOrder.value?.tongTien || 0);
            vouchers.value = (list || []).map(decorateVoucher);
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
});

// Khi chuyển tab hóa đơn, hỏi BE lại phiếu giảm giá tốt nhất để gợi ý
watch(() => selectedOrder.value?.id, (id) => {
    if (id) refreshBestVoucher();
});

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

                    if (!recipientName.value) recipientName.value = customer.ten || '';
                    if (!recipientPhone.value) recipientPhone.value = customer.sdt || '';

                    // Auto-fill default address
                    try {
                        const addrRes = await dichVuKhachHang.layDanhSachDiaChi(newId);
                        const addresses = addrRes?.data || addrRes || [];
                        if (addresses.length > 0) {
                            const defaultAddr = addresses.find(a => a.trangThai === true || a.trangThai === 1) || addresses[0];
                            if (defaultAddr) {
                                recipientAddressDetail.value = defaultAddr.diaChiChiTiet || '';

                                await fetchProvincesShip();
                                const pMatch = provincesShip.value.find(x => cleanName(x.name) === cleanName(defaultAddr.tinh));
                                if (pMatch) {
                                    recipientProvince.value = pMatch.code;
                                    await fetchDistrictsShip(pMatch.code);
                                    const dMatch = districtsShip.value.find(x => cleanName(x.name) === cleanName(defaultAddr.thanhPho));
                                    if (dMatch) {
                                        recipientDistrict.value = dMatch.code;
                                        await fetchWardsShip(dMatch.code);
                                        const wMatch = wardsShip.value.find(x => cleanName(x.name) === cleanName(defaultAddr.phuongXa));
                                        if (wMatch) {
                                            recipientWard.value = wMatch.code;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (addrErr) {
                        console.error('Lấy địa chỉ khách hàng thất bại', addrErr);
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
            recipientProvince.value = null;
            recipientDistrict.value = null;
            recipientWard.value = null;
            recipientAddressDetail.value = '';
        }
    },
    { immediate: true }
);

// Watchers for 3-level shipping address selection
watch(
    () => recipientProvince.value,
    async (newVal) => {
        recipientDistrict.value = null;
        recipientWard.value = null;
        if (newVal) {
            await fetchDistrictsShip(newVal);
        }
    }
);

watch(
    () => recipientDistrict.value,
    async (newVal) => {
        recipientWard.value = null;
        if (newVal) {
            await fetchWardsShip(newVal);
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
// mọi tính toán giảm giá đều do BE thực hiện.
const decorateVoucher = (v) => {
    let text = v.tenPhieu || v.ten || 'Phiếu giảm giá';
    const code = v.ma || v.maPhieu;
    if (code) text = `${code} - ${text}`;
    const type = String(v.loaiPhieu || '').toUpperCase();
    const discount = (type === 'PHAN_TRAM' || type === 'PERCENT')
        ? `(Giảm ${v.phanTramGiamGia || 0}%)`
        : `(Giảm ${new Intl.NumberFormat('vi-VN').format(v.soTienGiam || 0)}đ)`;
    return { ...v, customTitle: `${text} ${discount}` };
};

// Hỏi BE phiếu giảm giá tốt nhất để gợi ý (BE tự lọc điều kiện & tính giảm giá).
// Tự động áp phiếu tốt nhất nếu đủ điều kiện, ngược lại thì chỉ gợi ý.
const refreshBestVoucher = async (order = selectedOrder.value) => {
    if (!order?.id) return;
    if (order.idPhieuGiamGia) {
        order.suggestedVoucherId = null;
        return;
    }

    return Number(voucher.soTienGiam || 0);
};

const refreshBestVoucher = async (order = selectedOrder.value, autoApply = true) => {
    if (!order?.id) return;
    try {
        const list = await dichVuDonHang.getVouchers(order.tongTien || 0);
        vouchers.value = (list || []).map(decorateVoucher);

        if (autoApply) {
            isVoucherAutoApplied.value[order.id] = true;
            const best = bestEligibleVoucher.value;
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

// Cố định 1 voucher do người dùng tự chọn trên giao diện
// Áp dụng / gỡ phiếu giảm giá: BE lưu, tính lại tổng tiền sau giảm và trả về hóa đơn đã cập nhật.
const onApplyVoucher = async (voucherId) => {
    const order = selectedOrder.value;
    if (!order?.id) return;
    try {
        const updated = await dichVuDonHang.setVoucher(order.id, voucherId || null);
        updateOrderInList(updated);
        // Áp mã thì ẩn gợi ý; gỡ mã thì hỏi lại gợi ý từ BE.
        await refreshBestVoucher();
    } catch (e) {
        addNotification({ title: 'Lỗi phiếu giảm giá', subtitle: getErrorMessage(e, 'Không thể áp dụng phiếu giảm giá.'), color: 'error' });
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
        loaiDon: 'TAI_QUAY',
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

const onPrintInvoice = () => {
    if (!selectedOrder.value) return;

    const printOrder = JSON.parse(JSON.stringify(selectedOrder.value));
    printOrder.tenKhachHang = customerForm.value.ten || 'Khách lẻ';
    printOrder.sdtKhachHang = customerForm.value.sdt || '';

    printOrder.tongTien = selectedOrder.value.tongTien || 0;
    printOrder.tongTienSauGiam = finalCollectAmount.value;

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
            pollInterval: null
        };

        vnpayDialog.value.pollInterval = setInterval(async () => {
            let isClosed = false;
            try {
                isClosed = !popup || popup.closed;
            } catch (e) {
                isClosed = false;
            }

            if (isClosed) {
                if (!vnpayDialog.value.verified) {
                    handleVnPayCanceled();
                }
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
                    vnpayPopup = null;

                    try {
                        const verifyResult = await dichVuVnPay.verifyPaymentCallback(params);
                        if (isVnPayVerifySuccess(verifyResult, params)) {
                            const txnNo = params['vnp_TransactionNo'] || `VNP_${Date.now()}`;
                            const paidOrder = getStoredVnPayOrder() || selectedOrder.value;
                            vnpayDialog.value.verified = true;
                            await finalizeVnPayCheckout(paidOrder?.tongTienSauGiam || vnpayDialog.value.amount, txnNo, paidOrder);
                        } else {
                            handleVnPayCanceled(verifyResult?.message || 'VNPay không trả về trạng thái thanh toán thành công.');
                        }
                    } catch (err) {
                        console.error('Verify callback error:', err);
                        handleVnPayCanceled('Không thể xác thực kết quả VNPay. Hóa đơn chưa được hoàn tất.');
                    }
                }
            } catch (e) {
                // Ignore SecurityError
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
        confirmDialog.value = {
            show: true,
            title: 'Xác nhận thanh toán VNPay',
            message: `Hệ thống sẽ mở cổng thanh toán VNPay với số tiền [${new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(finalCollectAmount.value)}]. Bạn có muốn tiếp tục không?`,
            color: 'primary',
            action: startVnPayFlow,
            loading: false
        };
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

                const payload = {
                    idKhachHang: selectedOrder.value.idKhachHang,
                    idPhieuGiamGia: selectedOrder.value.idPhieuGiamGia,
                    tongTien: selectedOrder.value.tongTien,
                    tongTienSauGiam: finalCollectAmount.value,
                    loaiDon: 'TAI_QUAY',
                    ghiChu: compiledNote,
                    tienMat: isCash ? finalCollectAmount.value : 0,
                    tienChuyenKhoan: isCash ? 0 : finalCollectAmount.value,
                    phiVanChuyen: shippingFee.value
                };
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
                <v-col cols="12" lg="8" class="d-flex flex-column gap-4 pr-lg-2">
                    <!-- Sản phẩm Card -->
                    <v-card class="pos-card pa-4 rounded-lg border"
                        style="overflow: visible !important; z-index: 15 !important;">

                        <!-- Search and Filters Consolidated Row -->
                        <div class="d-flex align-center gap-2 mb-3 bg-slate-50 pa-1.5 rounded-lg flex-wrap w-100">
                            <!-- Custom Search Input with floating Dropdown -->
                            <div class="position-relative flex-grow-1" style="min-width: 240px; flex: 2 1 240px;">
                                <v-text-field v-model="productSearchKeyword" placeholder="Nhập mã, tên sản phẩm"
                                    variant="outlined" density="compact" hide-details prepend-inner-icon="mdi-magnify"
                                    @focus="onProductSearchFocus" @click="onProductSearchFocus"
                                    @blur="onProductSearchBlur" bg-color="white" class="search-input"
                                    autocomplete="off" />

                                <!-- Search dropdown overlay -->
                                <v-card v-if="showProductAutocomplete && filteredProductSearchResults.length > 0"
                                    class="position-absolute mt-1 shadow-lg border rounded-lg overflow-y-auto product-dropdown-card"
                                    style="max-height: 495px !important; z-index: 1000">
                                    <v-list class="pa-0">
                                        <v-list-item v-for="variant in filteredProductSearchResults" :key="variant.id"
                                            @mousedown="selectProductFromSearch(variant)"
                                            class="border-b py-3 hover-autocomplete-item pointer">
                                            <div class="d-flex justify-space-between w-100 align-start">
                                                <!-- Left info block -->
                                                <div class="d-flex align-start flex-grow-1">
                                                    <v-avatar rounded="lg" size="48"
                                                        class="mr-3 bg-grey-lighten-4 border flex-shrink-0">
                                                        <v-img v-if="variant.hinhAnh" :src="variant.hinhAnh" cover />
                                                        <BoxIcon v-else size="20" class="text-grey" />
                                                    </v-avatar>
                                                    <div class="d-flex flex-column" style="gap: 8px !important;">
                                                        <!-- Product Name (Tên biến thể) -->
                                                        <div class="text-slate-700"
                                                            style="font-size: 13.5px !important; line-height: 1.3;">
                                                            {{ variant.tenSanPham }}
                                                        </div>

                                                        <!-- mã sp --- mã sku (với nhãn vuông pastel nhé) -->
                                                        <div class="d-flex align-center gap-1.5 mt-0.5 flex-wrap">
                                                            <span class="sp-badge">Mã Sản phẩm: {{ variant.maSanPham ||
                                                                'SP0001' }}</span>
                                                            <span
                                                                style="margin-left: 15px; margin-right: 15px; font-size: 11px; color: #cbd5e1; opacity: 0.4;">|</span>
                                                            <span class="sku-badge">{{ variant.maChiTietSanPham
                                                            }}</span>
                                                        </div>

                                                        <!-- màu sắc --- size --- số lượng -->
                                                        <div class="d-flex align-center mt-0.5 text-slate-600"
                                                            style="font-size: 12px; flex-wrap: wrap;">
                                                            <span>Màu sắc: <span class="text-slate-500">{{
                                                                variant.tenMauSac || 'Không màu' }}</span></span>
                                                            <span
                                                                style="margin-left: 15px; margin-right: 15px; color: #cbd5e1; opacity: 0.4;">|</span>
                                                            <span>Size: <span class="text-slate-500">{{
                                                                variant.tenKichThuoc || 'N/A' }}</span></span>
                                                            <span
                                                                style="margin-left: 15px; margin-right: 15px; color: #cbd5e1; opacity: 0.4;">|</span>
                                                            <span>Tồn: <span class="text-slate-500">{{
                                                                variant.soLuongTon || 0 }}</span></span>
                                                        </div>
                                                    </div>
                                                </div>

                                                <!-- Right info block -->
                                                <div class="text-right flex-shrink-0 pl-3">
                                                    <template v-if="variant.phanTramGiam > 0">
                                                        <div class="price-text">{{ formatCurrency(variant.giaBan) }}
                                                        </div>
                                                        <span
                                                            style="text-decoration: line-through; text-decoration-color: #94a3b8; -webkit-text-decoration-color: #94a3b8; color: #c92c04 !important; font-size: 11px !important; font-weight: normal; display: block; margin-top: 2px;">
                                                            {{ formatCurrency(variant.giaBan / (1 - variant.phanTramGiam
                                                                / 100)) }}
                                                        </span>
                                                    </template>
                                                    <template v-else>
                                                        <div class="price-text">{{ formatCurrency(variant.giaBan) }}
                                                        </div>
                                                    </template>
                                                </div>
                                            </div>
                                        </v-list-item>
                                    </v-list>
                                </v-card>
                                <v-card v-else-if="showProductAutocomplete && !productSearchLoading"
                                    class="position-absolute w-100 mt-1 shadow-lg border rounded-lg pa-4 text-center text-grey"
                                    style="z-index: 1000">
                                    Không tìm thấy sản phẩm phù hợp.
                                </v-card>
                            </div>

                            <!-- Thương hiệu -->
                            <div style="min-width: 130px; flex: 1 1 130px;">
                                <v-select v-model="filterThuongHieu" :items="filterBrands" item-title="title"
                                    item-value="value" density="compact" hide-details variant="outlined"
                                    bg-color="white" class="compact-select" @update:model-value="onFilterChange" />
                            </div>

                            <!-- Mục đích chạy -->
                            <div style="min-width: 130px; flex: 1 1 130px;">
                                <v-select v-model="filterMucDich" :items="filterPurposes" item-title="title"
                                    item-value="value" density="compact" hide-details variant="outlined"
                                    bg-color="white" class="compact-select" @update:model-value="onFilterChange" />
                            </div>

                            <!-- Khoảng giá -->
                            <div style="min-width: 135px; flex: 1 1 135px;">
                                <v-select v-model="filterKhoangGia" :items="priceRangeOptions" item-title="title"
                                    item-value="value" density="compact" hide-details variant="outlined"
                                    bg-color="white" class="compact-select" @update:model-value="onFilterChange" />
                            </div>

                            <!-- Màu sắc -->
                            <div style="min-width: 110px; flex: 1 1 110px;">
                                <v-select v-model="filterMauSac" :items="filterColors" item-title="title"
                                    item-value="value" density="compact" hide-details variant="outlined"
                                    bg-color="white" class="compact-select" @update:model-value="onFilterChange" />
                            </div>

                            <!-- Kích cỡ -->
                            <div style="min-width: 110px; flex: 1 1 110px;">
                                <v-select v-model="filterKichCo" :items="filterSizes" item-title="title"
                                    item-value="value" density="compact" hide-details variant="outlined"
                                    bg-color="white" class="compact-select" @update:model-value="onFilterChange" />
                            </div>

                            <!-- Scanner Button -->
                            <v-tooltip text="Quét mã vạch" location="top" open-delay="0"
                                content-class="custom-white-tooltip">
                                <template v-slot:activator="{ props }">
                                    <v-btn color="primary" variant="outlined" class="scanner-btn text-none px-1"
                                        style="width: 44px; min-width: 44px; flex-shrink: 0;" @click="startScanner"
                                        v-bind="props">
                                        <v-icon>mdi-barcode-scan</v-icon>
                                    </v-btn>
                                </template>
                            </v-tooltip>

                            <!-- Reset Filters Button -->
                            <v-tooltip text="Làm mới bộ lọc" location="top" open-delay="0"
                                content-class="custom-white-tooltip">
                                <template v-slot:activator="{ props }">
                                    <v-btn color="error" variant="outlined" class="scanner-btn text-none px-1"
                                        style="width: 44px; min-width: 44px; flex-shrink: 0;" @click="resetFilters"
                                        v-bind="props">
                                        <v-icon>mdi-filter-off</v-icon>
                                    </v-btn>
                                </template>
                            </v-tooltip>
                        </div>



                        <!-- Cart list rendering -->
                        <div class="cart-container-box border rounded-lg overflow-hidden"
                            style="height: 420px; background-color: #ffffff !important;">
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
                    <!-- Row of Price Details -->
                    <v-row no-gutters>
                        <!-- Left Block: Pricing Details -->
                        <v-col cols="12" md="6" class="pr-md-3 pr-0 mb-4 mb-md-0">
                            <v-card class="pos-card pa-4 rounded-lg border h-100 d-flex flex-column">
                                <v-row no-gutters class="flex-grow-1">
                                    <!-- Left Column inside the card -->
                                    <v-col cols="12" md="6" class="pr-md-6 d-flex flex-column justify-space-between"
                                        style="gap: 15px;">
                                        <div class="d-flex justify-space-between align-center mb-3">
                                            <h3 class="font-weight-bold text-slate-800 m-0"
                                                style="font-size: 14px !important">
                                                Tổng đơn hàng</h3>
                                        </div>

                                        <v-row no-gutters class="mb-4 border-b pb-3">
                                            <v-col cols="6">
                                                <v-checkbox v-model="isFreeShip" label="Miễn phí giao hàng" hide-details
                                                    density="compact" class="text-slate-700" />
                                            </v-col>
                                            <v-col cols="6">
                                                <v-checkbox v-model="onlyChargeIfReturned" label="Mua hàng tại quầy"
                                                    hide-details density="compact" class="text-slate-700" />
                                            </v-col>
                                        </v-row>

                                        <div class="flex-grow-1 d-flex flex-column" style="gap: 20px;">
                                            <!-- Voucher Selection -->
                                            <div class="d-flex align-center justify-space-between">
                                                <span class="text-slate-600" style="font-size: 13px !important">Phiếu
                                                    giảm
                                                    giá</span>
                                                <v-select :model-value="selectedOrder?.idPhieuGiamGia" :items="vouchers"
                                                    item-title="customTitle" item-value="id" variant="outlined"
                                                    density="compact" hide-details @update:model-value="onApplyVoucher"
                                                    class="compact-select custom-value-input"
                                                    style="width: 200px !important; max-width: 200px !important; min-width: 200px !important; flex: none !important;"
                                                    clearable placeholder="Nhập phiếu giảm giá" persistent-placeholder
                                                    no-data-text="Chưa có phiếu giảm giá" />
                                            </div>

                                            <!-- Shipping Fee -->
                                            <div class="d-flex align-center justify-space-between">
                                                <span class="text-slate-600" style="font-size: 13px !important">Phí vận
                                                    chuyển</span>
                                                <v-menu offset="4">
                                                    <template v-slot:activator="{ props }">
                                                        <v-text-field :model-value="formatNumberWithDots(shippingFee)"
                                                            @input="e => shippingFee = parseNumberFromDots(e.target.value)"
                                                            v-bind="props" variant="outlined" density="compact"
                                                            suffix="đ" hide-details
                                                            style="width: 200px !important; max-width: 200px !important; min-width: 200px !important; flex: none !important;"
                                                            class="text-right-input custom-value-input"
                                                            :disabled="isFreeShip" />
                                                    </template>
                                                    <v-list class="pa-0 border rounded-lg elevation-2 bg-white"
                                                        style="min-width: 170px;">
                                                        <!-- Nội thành -->
                                                        <div>
                                                            <div class="bg-slate-100 text-slate-700 px-3 py-1 font-weight-bold"
                                                                style="font-size: 11px;">Nội thành:</div>
                                                            <v-list-item @click="shippingFee = 30000"
                                                                class="px-3 py-2 cursor-pointer hover-bg-slate-50 text-caption text-slate-800 font-weight-medium">
                                                                30.000 <span class="text-decoration-underline">đ</span>
                                                            </v-list-item>
                                                        </div>
                                                        <!-- Ngoại thành -->
                                                        <div>
                                                            <div class="bg-slate-100 text-slate-700 px-3 py-1 font-weight-bold"
                                                                style="font-size: 11px;">Ngoại thành:</div>
                                                            <v-list-item @click="shippingFee = 30000"
                                                                class="px-3 py-2 cursor-pointer hover-bg-slate-50 text-caption text-slate-800 font-weight-medium">
                                                                30.000 <span class="text-decoration-underline">đ</span>
                                                            </v-list-item>
                                                        </div>
                                                        <!-- Ngoại tỉnh -->
                                                        <div>
                                                            <div class="bg-slate-100 text-slate-700 px-3 py-1 font-weight-bold"
                                                                style="font-size: 11px;">Ngoại tỉnh:</div>
                                                            <v-list-item @click="shippingFee = 30000"
                                                                class="px-3 py-2 cursor-pointer hover-bg-slate-50 text-caption text-slate-800 font-weight-medium">
                                                                30.000 <span class="text-decoration-underline">đ</span>
                                                            </v-list-item>
                                                        </div>
                                                    </v-list>
                                                </v-menu>
                                            </div>


                                            <!-- Total Amount Raw -->
                                            <div class="d-flex align-center justify-space-between">
                                                <span class="text-slate-600" style="font-size: 13px !important">Tổng số
                                                    tiền</span>
                                                <span class="font-weight-bold"
                                                    style="font-size: 13px !important; color: #0c3866;">{{
                                                        formatCurrency(totalRawAmount)
                                                    }}</span>
                                            </div>

                                            <!-- Discount amount applied -->
                                            <div class="d-flex align-center justify-space-between">
                                                <span class="text-slate-600" style="font-size: 13px !important">Giảm
                                                    giá</span>
                                                <span class="font-weight-bold"
                                                    style="font-size: 13px !important; color: #dc2626;">
                                                    {{ discountAmount > 0 ? '-' : '' }}{{ formatCurrency(discountAmount)
                                                    }}
                                                </span>
                                            </div>

                                            <!-- Final net collected amount -->
                                            <div style="border-top: 1px dashed #cbd5e1; margin: 4px 0;"></div>

                                            <div class="d-flex align-center justify-space-between">
                                                <span class="text-slate-600" style="font-size: 13px !important">Tiền cần
                                                    thu</span>
                                                <span class="font-weight-bold"
                                                    style="font-size: 13px !important; color: #0c3866;">{{
                                                        formatCurrency(finalCollectAmount) }}</span>
                                            </div>
                                        </div>
                                    </v-col>

                                    <!-- Right Column: Shipping Fee + Final Collected Amount -->
                                    <v-col cols="12" md="6"
                                        class="d-flex flex-column justify-start pl-md-6 border-s-dashed-md"
                                        style="gap: 15px;">
                                        <!-- Total Discount Amount -->
                                        <div class="d-flex align-center justify-space-between">
                                            <span class="text-slate-600" style="font-size: 13px !important">Tổng tiền
                                                giảm</span>
                                            <span class="font-weight-semibold"
                                                style="font-size: 14px !important; color: #991b1b !important;">
                                                {{ discountAmount > 0 ? '-' : '' }}{{ formatCurrency(discountAmount) }}
                                            </span>
                                        </div>

                                        <!-- Shipping Fee -->
                                        <div class="d-flex flex-column"
                                            style="min-height: 58px; justify-content: flex-start;">
                                            <div class="d-flex align-center justify-space-between">
                                                <span class="text-slate-600 d-flex align-center"
                                                    style="font-size: 13px !important">
                                                    <span>Phí vận chuyển</span>
                                                    <svg width="45" height="15" viewBox="0 0 45 15" fill="none"
                                                        xmlns="http://www.w3.org/2000/svg"
                                                        style="display: inline-block; vertical-align: middle; margin-left: 6px;">
                                                        <path
                                                            d="M1 2.5 L7 2.5 L4.5 6.5 L7 6.5 L3.5 10.5 L1 10.5 L3.5 6.5 L1 6.5 Z"
                                                            fill="#0C2A46" />
                                                        <path
                                                            d="M5.5 2.5 L11.5 2.5 L9 6.5 L11.5 6.5 L8 10.5 L5.5 10.5 L8 6.5 L5.5 6.5 Z"
                                                            fill="#FA6400" />
                                                        <text x="13.5" y="11" fill="#FA6400"
                                                            font-family="'Inter', sans-serif" font-weight="900"
                                                            font-style="italic" font-size="10.5"
                                                            letter-spacing="-0.5px">GHN</text>
                                                    </svg>
                                                </span>
                                                <v-menu offset="4">
                                                    <template v-slot:activator="{ props }">
                                                        <v-text-field :model-value="formatNumberWithDots(shippingFee)"
                                                            @input="e => shippingFee = parseNumberFromDots(e.target.value)"
                                                            v-bind="props" variant="outlined" density="compact"
                                                            suffix="đ" hide-details
                                                            style="width: 240px !important; max-width: 240px !important; min-width: 240px !important; flex: none !important;"
                                                            class="text-right-input custom-value-input"
                                                            :disabled="isFreeShip || !isGiaoHang" />
                                                    </template>
                                                    <v-list class="pa-0 border rounded-lg elevation-2 bg-white"
                                                        style="min-width: 170px;">
                                                        <div>
                                                            <div class="bg-slate-100 text-slate-700 px-3 py-1 font-weight-bold"
                                                                style="font-size: 11px;">Nội thành:</div>
                                                            <v-list-item @click="shippingFee = 30000"
                                                                class="px-3 py-2 cursor-pointer hover-bg-slate-50 text-caption text-slate-800 font-weight-medium">
                                                                30.000 <span class="text-decoration-underline">đ</span>
                                                            </v-list-item>
                                                        </div>
                                                        <div>
                                                            <div class="bg-slate-100 text-slate-700 px-3 py-1 font-weight-bold"
                                                                style="font-size: 11px;">Ngoại thành:</div>
                                                            <v-list-item @click="shippingFee = 30000"
                                                                class="px-3 py-2 cursor-pointer hover-bg-slate-50 text-caption text-slate-800 font-weight-medium">
                                                                30.000 <span class="text-decoration-underline">đ</span>
                                                            </v-list-item>
                                                        </div>
                                                        <div>
                                                            <div class="bg-slate-100 text-slate-700 px-3 py-1 font-weight-bold"
                                                                style="font-size: 11px;">Ngoại tỉnh:</div>
                                                            <v-list-item @click="shippingFee = 30000"
                                                                class="px-3 py-2 cursor-pointer hover-bg-slate-50 text-caption text-slate-800 font-weight-medium">
                                                                30.000 <span class="text-decoration-underline">đ</span>
                                                            </v-list-item>
                                                        </div>
                                                    </v-list>
                                                </v-menu>
                                            </div>
                                        </div>

                                        <div class="d-flex align-center justify-space-between"
                                            style="border-top: 1px dashed #cbd5e1; margin-top: 8px; padding-top: 12px;">
                                            <span class="text-slate-700 font-weight-bold"
                                                style="font-size: 14px !important">Tổng tiền cần
                                                thanh toán</span>
                                            <span class="font-weight-semibold"
                                                style="font-size: 16px !important; color: #0c3866;">
                                                {{ formatCurrency(finalCollectAmount) }}
                                            </span>
                                        </div>
                                    </v-col>
                                </v-row>
                            </v-card>
                        </v-col>

                        <!-- Right Block: Payment & Notes -->
                        <v-col cols="12" md="6" class="pl-md-3 pl-0 d-flex flex-column gap-3">
                            <!-- Payment Card -->
                            <v-card class="pos-card pa-4 rounded-lg border">
                                <div class="d-flex justify-space-between align-center mb-3">
                                    <h3 class="text-slate-800 m-0" style="font-size: 13px !important">Thanh toán</h3>
                                </div>

                                <div class="d-flex align-center justify-space-between mb-4">
                                    <span class="text-slate-600" style="font-size: 13px !important">Hình thức thanh
                                        toán</span>
                                    <div class="d-flex gap-2">
                                        <button type="button" @click="checkoutData.paymentMethod = 'CASH'"
                                            :class="['px-3 d-flex align-center justify-center transition-all',
                                                checkoutData.paymentMethod === 'CASH' ? 'cash-active-btn' : 'payment-inactive-btn']"
                                            style="font-size: 13px !important; border: 1px solid; border-radius: 0px !important; height: 32px; min-width: 90px; cursor: pointer;">
                                            <v-icon class="mr-1" size="16">mdi-cash</v-icon>
                                            Tiền mặt
                                        </button>
                                        <button type="button" @click="checkoutData.paymentMethod = 'VNPAY'"
                                            :class="['px-3 d-flex align-center justify-center transition-all',
                                                checkoutData.paymentMethod === 'VNPAY' ? 'vnpay-active-btn' : 'payment-inactive-btn']"
                                            style="font-size: 13px !important; border: 1px solid; border-radius: 0px !important; height: 32px; min-width: 90px; cursor: pointer;">
                                            <v-icon class="mr-1" size="16">mdi-credit-card-outline</v-icon>
                                            VNPay
                                        </button>
                                    </div>
                                </div>

                                <!-- Money Input -->
                                <div class="d-flex align-center justify-space-between mb-3">
                                    <span class="text-slate-600" style="font-size: 13px !important">
                                        {{ checkoutData.paymentMethod === 'CASH' ? 'Tiền khách đưa' : 'Tiền chuyển
                                        khoản' }}
                                    </span>
                                    <v-text-field :model-value="formatNumberWithDots(checkoutData.receivedAmount)"
                                        @input="e => checkoutData.receivedAmount = parseNumberFromDots(e.target.value)"
                                        variant="outlined" density="compact" suffix="đ" hide-details
                                        style="width: 200px !important; max-width: 200px !important; min-width: 200px !important; flex: none !important;"
                                        class="text-right-input" />
                                </div>

                                <!-- Voucher Selection -->
                                <div class="d-flex flex-column"
                                    style="min-height: 58px; justify-content: flex-start; gap: 4px;">
                                    <div class="d-flex align-center justify-space-between">
                                        <span class="text-slate-600 flex-shrink-0"
                                            style="font-size: 13px !important">Phiếu giảm
                                            giá đang áp dụng tốt nhất</span>
                                        <v-select :model-value="selectedOrder?.idPhieuGiamGia" :items="vouchers"
                                            item-title="customTitle" item-value="id" variant="outlined"
                                            density="compact" hide-details @update:model-value="onApplyVoucher"
                                            class="compact-select custom-value-input"
                                            style="width: 240px !important; max-width: 240px !important; min-width: 240px !important; flex: none !important;"
                                            clearable placeholder="Nhập phiếu giảm giá"
                                            no-data-text="Chưa có phiếu giảm giá">
                                            <template v-slot:selection="{ item }">
                                                <div class="d-flex align-center gap-1">
                                                    <span class="custom-voucher-badge">
                                                        {{ getVoucherDiscountLabel(item.raw) }}
                                                    </span>
                                                    <span class="font-weight-bold text-slate-800"
                                                        style="font-size: 12px !important; letter-spacing: -0.2px;">
                                                        {{ item.raw.ma || item.raw.maPhieu || 'Mã' }}
                                                    </span>
                                                </div>
                                            </template>
                                            <template v-slot:item="{ props, item }">
                                                <v-list-item v-bind="props" class="px-3 py-1">
                                                    <template v-slot:title>
                                                        <div class="d-flex align-center gap-2">
                                                            <span class="custom-voucher-badge">
                                                                {{ getVoucherDiscountLabel(item.raw) }}
                                                            </span>
                                                            <span class="font-weight-bold text-slate-800"
                                                                style="font-size: 12px !important;">
                                                                {{ item.raw.ma || item.raw.maPhieu }}
                                                            </span>
                                                            <span class="text-slate-300"
                                                                style="font-size: 11px;">|</span>
                                                            <span class="text-slate-600 text-truncate"
                                                                style="font-size: 11px !important; max-width: 140px; display: inline-block;">
                                                                {{ item.raw.tenPhieu || item.raw.ten || 'Ưu đãi'
                                                                }}
                                                            </span>
                                                        </div>
                                                    </template>
                                                </v-list-item>
                                            </template>
                                        </v-select>
                                    </div>
                                    <div v-if="nextBetterVoucher || (bestEligibleVoucher && (!appliedVoucher || String(appliedVoucher.id) !== String(bestEligibleVoucher.id))) || (isVoucherAutoApplied[selectedOrder?.id] !== false && appliedVoucher)"
                                        class="d-flex justify-end pr-1 text-right">
                                        <!-- Case 1: Mốc voucher cao hơn chưa đạt điều kiện -->
                                        <span v-if="nextBetterVoucher" class="font-weight-medium"
                                            style="font-size: 11.5px !important; color: #7c2d12 !important; font-style: italic !important;">
                                            Mua thêm <strong style="color: #c2410c; font-size: 12px;">{{
                                                formatCurrency(remainingForSuggestedVoucher) }}</strong> để được nhận
                                            phiếu giảm giá:
                                            <strong style="color: #166534; font-size: 12px;">-{{
                                                getVoucherDiscountLabel(nextBetterVoucher)
                                                }}</strong> ({{ nextBetterVoucher.ma || nextBetterVoucher.maPhieu }})
                                        </span>
                                        <!-- Case 2: Đề xuất phiếu giảm giá tốt hơn có sẵn (Bấm được để áp dụng) -->
                                        <span
                                            v-else-if="bestEligibleVoucher && (!appliedVoucher || String(appliedVoucher.id) !== String(bestEligibleVoucher.id))"
                                            class="font-weight-medium"
                                            style="font-size: 11.5px !important; color: #7c2d12 !important; font-style: italic !important;">
                                            Đề xuất phiếu giảm giá tốt hơn đang có sẵn:
                                            <v-hover v-slot="{ isHovering, props }">
                                                <span v-bind="props"
                                                    @click="onApplyVoucher(bestEligibleVoucher.id, false, false)"
                                                    :class="['ml-1 font-weight-bold cursor-pointer', { 'text-decoration-underline': isHovering }]"
                                                    style="font-size: 11.5px !important; transition: all 0.2s; color: #166534 !important;">
                                                    -{{ getVoucherDiscountLabel(bestEligibleVoucher) }} ({{
                                                    bestEligibleVoucher.ma ||
                                                    bestEligibleVoucher.maPhieu }})
                                                </span>
                                            </v-hover>
                                        </span>
                                        <!-- Case 3: Hệ thống tự động áp dụng mã tốt nhất -->
                                        <span
                                            v-else-if="isVoucherAutoApplied[selectedOrder?.id] !== false && appliedVoucher"
                                            class="font-weight-medium"
                                            style="font-size: 11.5px !important; color: #166534 !important; font-style: italic !important;">
                                            Đã tự động áp dụng phiếu giảm giá tốt nhất:
                                            <strong style="color: #166534; font-size: 12px;">-{{
                                                getVoucherDiscountLabel(appliedVoucher)
                                                }}</strong> ({{ appliedVoucher.ma || appliedVoucher.maPhieu }})
                                        </span>
                                    </div>
                                </div>
                            </v-card>

                            <!-- Notes Card -->
                            <v-card class="pos-card pa-4 rounded-lg border flex-grow-1 d-flex flex-column"
                                style="min-height: 140px">
                                <div class="text-slate-800 mb-2" style="font-size: 13px !important">Ghi chú</div>

                                <v-textarea v-model="checkoutData.note"
                                    placeholder="Viết ghi chú hoặc /shortcut để ghi chú nhanh" variant="outlined"
                                    rows="2" hide-details class="flex-grow-1 note-textarea text-body-2" />

                                <!-- Gợi ý mua thêm - Classic Style -->
                                <div v-if="showProductSuggestions && suggestionData"
                                    class="suggestion-box mt-3 pa-3 rounded-lg border"
                                    style="border-color: #e2e8f0; background-color: transparent; border-style: solid; border-width: 1px;">
                                    <div class="d-flex justify-space-between align-center mb-2">
                                        <span class="font-weight-bold text-slate-800"
                                            style="font-size: 13px !important;">
                                            Gợi ý mua thêm
                                        </span>
                                        <span class="text-caption font-weight-medium"
                                            style="color: #b57a00; font-size: 11px !important;">
                                            1 đề xuất
                                        </span>
                                    </div>
                                    <div class="d-flex flex-column" style="gap: 8px;">
                                        <div class="d-flex justify-space-between align-center">
                                            <span class="text-slate-600" style="font-size: 12px !important;">
                                                Mã sản phẩm:
                                            </span>
                                            <span class="font-weight-bold text-slate-800"
                                                style="font-size: 12px !important;">
                                                {{ suggestionData.productCode }}
                                            </span>
                                        </div>
                                        <div class="d-flex justify-space-between align-center">
                                            <span class="text-slate-600" style="font-size: 12px !important;">
                                                Giảm giá:
                                            </span>
                                            <span class="font-weight-bold text-error"
                                                style="font-size: 12px !important;">
                                                {{ suggestionData.discountPercent }}%
                                            </span>
                                        </div>
                                        <div class="d-flex justify-space-between align-center">
                                            <span class="text-slate-600" style="font-size: 12px !important;">
                                                Cần mua thêm:
                                            </span>
                                            <span class="font-weight-semibold text-slate-800"
                                                style="font-size: 12px !important;">
                                                {{ formatCurrency(suggestionData.needToBuy) }}
                                            </span>
                                        </div>
                                        <div class="d-flex justify-space-between align-center">
                                            <span class="text-slate-600" style="font-size: 12px !important;">
                                                Sẽ được giảm:
                                            </span>
                                            <span class="font-weight-semibold text-success"
                                                style="font-size: 12px !important;">
                                                {{ formatCurrency(suggestionData.willReduce) }}
                                            </span>
                                        </div>
                                    </div>
                                </div>
                            </v-card>
                        </v-col>
                    </v-row>
                </v-col>

                <!-- Right Column (4 cols out of 12) -->
                <v-col cols="12" lg="4" class="d-flex flex-column gap-4 pl-lg-2 mt-4 mt-lg-0">
                    <!-- Khách hàng Card -->
                    <v-card class="pos-card pa-4 rounded-lg border">
                        <div class="d-flex justify-space-between align-center border-b pb-2 mb-3">
                            <div class="d-flex align-center gap-2">
                                <span class="font-weight-bold text-slate-800" style="font-size: 14px !important">Khách
                                    hàng</span>
                                <v-chip v-if="selectedOrder?.idKhachHang" color="blue-lighten-4"
                                    class="text-blue-darken-4 font-weight-bold" size="x-small" variant="flat"
                                    style="font-size: 11px !important; height: 20px !important; border-radius: 6px !important;">
                                    Số lần mua: {{ customerForm.tongDonHang || 0 }}
                                </v-chip>
                                <v-chip
                                    v-else-if="customerForm.ten?.trim() || customerForm.sdt?.trim() || customerForm.email?.trim() || (customerForm.gioiTinh && customerForm.gioiTinh !== 'Giới tính') || customerForm.ngaySinh"
                                    color="green-lighten-4" class="text-green-darken-4 font-weight-bold" size="x-small"
                                    variant="flat"
                                    style="font-size: 11px !important; height: 20px !important; border-radius: 6px !important;">
                                    Thêm mới
                                </v-chip>
                            </div>
                            <div class="d-flex align-center gap-2">
                                <v-select v-model="customerForm.gioiTinh" :items="['Giới tính', 'Nam', 'Nữ', 'Khác']"
                                    density="compact" variant="outlined" hide-details class="dim-select"
                                    style="width: 200px" />
                                <v-btn v-if="selectedOrder?.idKhachHang" icon size="x-small" variant="flat"
                                    color="error" class="bg-red-50"
                                    style="width: 28px !important; height: 28px !important; border-radius: 6px !important;"
                                    @click="onRemoveCustomer">
                                    <v-icon size="14">mdi-close-circle-outline</v-icon>
                                </v-btn>

                            </div>
                        </div>

                        <div class="d-flex flex-column gap-4">
                            <!-- Input tìm kiếm khách hàng -->
                            <div class="position-relative">
                                <v-text-field v-model="customerSearch"
                                    placeholder="Tìm kiếm khách hàng (SĐT, Tên, Email)" variant="outlined"
                                    density="compact" hide-details prepend-inner-icon="mdi-magnify"
                                    class="dim-input-field bg-slate-50" @focus="showCustomerSuggestions = true"
                                    autocomplete="off" />

                                <div v-if="showCustomerSuggestions && customerSearch.length > 0"
                                    class="suggestion-popover overflow-y-auto w-100"
                                    style="max-height: 250px; z-index: 100; top: calc(100% + 4px);"
                                    v-click-outside="() => showCustomerSuggestions = false">
                                    <div v-if="customerResults.length > 0" class="pa-1 d-flex flex-column gap-1">
                                        <div v-for="c in customerResults" :key="c.id"
                                            @click="onSelectSuggestedCustomer(c)"
                                            class="suggestion-item d-flex flex-column px-3 py-2 cursor-pointer transition-all border-b"
                                            style="font-size: 13px;">
                                            <div class="d-flex w-100 justify-space-between mb-1">
                                                <span class="font-weight-bold text-slate-800">{{ c.hoTen || c.ten
                                                }}</span>
                                                <span class="text-blue-darken-3 font-weight-medium">{{ c.sdt }}</span>
                                            </div>
                                            <span class="text-slate-500 text-caption">{{ c.email || 'Không có email'
                                            }}</span>
                                        </div>
                                    </div>
                                    <div v-else class="pa-3 text-center text-slate-500">
                                        <span class="text-caption">Không tìm thấy khách hàng</span>
                                    </div>
                                </div>
                            </div>

                            <div class="d-flex gap-3">
                                <v-text-field v-model="customerForm.ten" placeholder="Tên khách hàng" variant="outlined"
                                    density="compact" hide-details autocomplete="off"
                                    class="dim-input-field flex-grow-1" />
                                <div style="width: 170px; flex: none;">
                                    <v-text-field v-model="customerForm.sdt" placeholder="Số điện thoại"
                                        variant="outlined" density="compact" hide-details autocomplete="off"
                                        class="dim-input-field" />
                                </div>
                            </div>

                            <div class="d-flex gap-3">
                                <v-text-field v-model="customerForm.email" placeholder="Địa chỉ email"
                                    variant="outlined" density="compact" hide-details autocomplete="off"
                                    class="dim-input-field flex-grow-1" />
                                <div style="width: 170px; flex: none;">
                                    <v-text-field v-model="customerForm.ngaySinh" type="date" placeholder="Ngày sinh"
                                        variant="outlined" density="compact" hide-details
                                        append-inner-icon="mdi-calendar-outline"
                                        class="dim-input-field date-input-field" @click:append-inner="openDatePicker"
                                        @click="openDatePicker" />
                                </div>
                            </div>
                        </div>
                    </v-card>

                    <!-- Nhận hàng Card -->
                    <transition name="expand-fade">
                        <v-card v-if="isGiaoHang" class="pos-card pa-4 rounded-lg border">
                            <div class="font-weight-bold text-slate-800 mb-3" style="font-size: 14px !important">Thông
                                tin
                                nhận hàng</div>
                            <div class="d-flex flex-column gap-4">
                                <div class="d-flex gap-3">
                                    <v-text-field v-model="recipientName" placeholder="Tên người nhận"
                                        variant="outlined" density="compact" hide-details autocomplete="off"
                                        class="dim-input-field flex-grow-1" />
                                    <div style="width: 170px; flex: none;">
                                        <v-text-field v-model="recipientPhone" placeholder="Số điện thoại"
                                            variant="outlined" density="compact" hide-details autocomplete="off"
                                            class="dim-input-field" />
                                    </div>
                                </div>

                                <v-text-field v-model="recipientAddressDetail" placeholder="Địa chỉ chi tiết"
                                    variant="outlined" density="compact" hide-details autocomplete="off"
                                    class="dim-input-field w-100" />

                                <div class="d-flex gap-3">
                                    <v-select v-model="recipientProvince" :items="provincesShip" item-title="name"
                                        item-value="code" placeholder="Tỉnh/Thành phố" density="compact"
                                        variant="outlined" hide-details class="dim-select-field flex-grow-1"
                                        style="width: 33.33%;" />
                                    <v-select v-model="recipientDistrict" :items="districtsShip" item-title="name"
                                        item-value="code" placeholder="Quận/Huyện" density="compact" variant="outlined"
                                        hide-details :disabled="!recipientProvince" class="dim-select-field flex-grow-1"
                                        style="width: 33.33%;" />
                                    <v-select v-model="recipientWard" :items="wardsShip" item-title="name"
                                        item-value="code" placeholder="Phường/Xã" density="compact" variant="outlined"
                                        hide-details :disabled="!recipientDistrict" class="dim-select-field flex-grow-1"
                                        style="width: 33.33%;" />
                                </div>
                            </div>
                        </v-card>
                    </transition>

                    <!-- Payment Card -->
                    <v-card class="pos-card pa-4 rounded-lg border">
                        <div class="d-flex justify-space-between align-center mb-3">
                            <h3 class="text-slate-800 m-0" style="font-size: 13px !important">Thanh toán</h3>
                        </div>

                        <div class="d-flex align-center justify-space-between mb-4">
                            <span class="text-slate-600" style="font-size: 13px !important">Hình thức thanh
                                toán</span>
                            <div class="d-flex gap-2">
                                <button type="button" @click="checkoutData.paymentMethod = 'CASH'"
                                    :class="['px-3 d-flex align-center justify-center transition-all',
                                        checkoutData.paymentMethod === 'CASH' ? 'cash-active-btn' : 'payment-inactive-btn']"
                                    style="font-size: 13px !important; border: 1px solid; border-radius: 0px !important; height: 32px; min-width: 90px; cursor: pointer;">
                                    <v-icon class="mr-1" size="16">mdi-cash</v-icon>
                                    Tiền mặt
                                </button>
                                <button type="button" @click="checkoutData.paymentMethod = 'VNPAY'"
                                    :class="['px-3 d-flex align-center justify-center transition-all',
                                        checkoutData.paymentMethod === 'VNPAY' ? 'vnpay-active-btn' : 'payment-inactive-btn']"
                                    style="font-size: 13px !important; border: 1px solid; border-radius: 0px !important; height: 32px; min-width: 90px; cursor: pointer;">
                                    <v-icon class="mr-1" size="16">mdi-credit-card-outline</v-icon>
                                    VNPay
                                </button>
                            </div>
                        </div>

                        <!-- Money Input -->
                        <div class="d-flex align-center justify-space-between mb-3">
                            <span class="text-slate-600" style="font-size: 13px !important">
                                {{ checkoutData.paymentMethod === 'CASH' ? 'Tiền khách đưa' : 'Tiền chuyển khoản' }}
                            </span>
                            <v-text-field :model-value="formatNumberWithDots(checkoutData.receivedAmount)"
                                @input="e => checkoutData.receivedAmount = parseNumberFromDots(e.target.value)"
                                variant="outlined" density="compact" suffix="đ" hide-details
                                style="width: 200px !important; max-width: 200px !important; min-width: 200px !important; flex: none !important;"
                                class="text-right-input" />
                        </div>

                        <!-- Unpaid / Refund Message Alert -->
                        <div v-if="remainingBalance > 0"
                            class="d-flex align-center justify-space-between pa-3 rounded-lg bg-red-50 text-red-800 border-red">
                            <div class="d-flex align-center gap-2">
                                <v-icon color="error" size="18">mdi-alert-circle-outline</v-icon>
                                <span class="text-slate-600" style="font-size: 13px !important">Còn thiếu</span>
                            </div>
                            <span class="font-weight-bold" style="font-size: 13px !important;">{{
                                formatCurrency(remainingBalance)
                                }}</span>
                        </div>
                        <div v-else-if="changeAmount > 0"
                            class="d-flex align-center justify-space-between pa-3 rounded-lg bg-blue-50 text-blue-800 border-blue">
                            <div class="d-flex align-center gap-2">
                                <v-icon color="primary" size="18">mdi-cash-refund</v-icon>
                                <span class="text-slate-600" style="font-size: 13px !important">Tiền thừa trả
                                    khách</span>
                            </div>
                            <span class="font-weight-bold" style="font-size: 13px !important;">{{
                                formatCurrency(changeAmount) }}</span>
                        </div>
                    </v-card>

                    <!-- Checkout / Print Action Buttons at Bottom Right -->
                    <div class="d-flex gap-3 mt-2 w-100">
                        <v-btn color="#107c41" height="60"
                            class="font-weight-bold rounded-lg shadow-md text-white px-4 flex-grow-1"
                            style="font-size: 17px !important;" :disabled="!selectedOrder?.listsHoaDonChiTiet?.length"
                            @click="onPrintInvoice" elevation="0">
                            <v-icon class="mr-1">mdi-printer</v-icon>
                            IN HÓA ĐƠN
                        </v-btn>

                        <v-btn color="primary" height="60"
                            class="font-weight-bold rounded-lg btn-checkout shadow-md text-white px-4 flex-grow-1"
                            style="font-size: 17px !important;" :loading="isProcessing"
                            :disabled="!selectedOrder?.listsHoaDonChiTiet?.length" @click="onCheckout" elevation="0">
                            THANH TOÁN
                        </v-btn>
                    </div>
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
                                <v-img :src="vnpayDialog.qrUrl" width="220" height="220" />
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
                    <v-btn icon variant="text" @click="stopScanner">
                        <XIcon />
                    </v-btn>
                </div>
                <div id="reader" style="width: 100%"></div>
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
        <InvoiceReceiptDialog :show="receiptDialog.show" :receipt="receiptDialog" @close="onCloseReceipt" />

        <!-- Giao Ca Modal --> <!-- Tạm thời ẩn chức năng giao ca
        <GiaoCaModal v-model="showGiaoCaModal" mode="open" @success="handleGiaoCaSuccess" /> 
        -->
    </v-container>
</template>

<style scoped lang="scss">
@import '@/scss/pages/admin/_ban-hang.scss';
</style>

<style lang="scss">
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
</style>
