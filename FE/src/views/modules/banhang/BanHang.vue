<script setup>
/**
 * Module: Ban hang tai quay (Admin/POS)
 * Y nghia: man hinh xu ly hoa don cho, gio hang, khach hang, giao hang,
 * voucher, thanh toan tien mat/VNPay, quet QR/barcode va in hoa don.
 */
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
import { dichVuHoaDon } from '@/services/admin/dichVuHoaDon';
import { dichVuPhieuGiamGia } from '@/services/admin/dichVuPhieuGiamGia';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';
import { dichVuNhanVien } from '@/services/admin/dichVuNhanVien';
import {
    dichVuThuongHieu,
    dichVuXuatXu,
    dichVuMucDichChay,
    dichVuChatLieu
} from '@/services/product/dichVuThuocTinh';
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

const { addNotification } = useNotifications();
const uiStore = useUIStore();
const authStore = useAuthStore();
const MAX_WAITING_ORDERS = 5;
const VNPAY_PENDING_KEY = 'aerostride_pos_vnpay_pending';
const BYPASS_PAYMENT_RECORD_INSERT = false;

// Dia chi cho form them nhanh khach hang.
const { provinces, districts, wards, loadingLocations, fetchProvinces, fetchDistricts, fetchWards, cleanName } = useLocation();
const { mapCodesToNames } = useAddressMapping();

// Bo dia chi rieng cho thong tin nhan hang, tach khoi form them nhanh khach hang.
const {
    provinces: provincesShip,
    districts: districtsShip,
    wards: wardsShip,
    fetchProvinces: fetchProvincesShip,
    fetchDistricts: fetchDistrictsShip,
    fetchWards: fetchWardsShip
} = useLocation();

// Trang thai chung cua man POS va danh sach hoa don cho.
const loading = ref(false);
const orders = ref([]);
const activeOrderIndex = ref(0);
const vouchers = ref([]);
const isProcessing = ref(false);
const isAutoApplyingVoucher = ref(false);
const manualVoucherLocks = ref({});

// Trang thai tim kiem san pham/bien the trong gio hang.
const orderWarehouse = ref('KHO ANSHA BIGSIZE');
const productMode = ref('SP'); // 'SP' or 'COMBO'
const productSearchKeyword = ref('');
const productSearchResults = ref([]);
const productSearchLoading = ref(false);
const showProductAutocomplete = ref(false);
const onlyInStock = ref(false);

// Bo loc san pham POS theo thuoc tinh san pham.
const filterThuongHieu = ref('ALL');
const filterXuatXu = ref('ALL');
const filterMucDich = ref('ALL');
const filterChatLieu = ref('ALL');

const filterBrands = ref([]);
const filterOrigins = ref([]);
const filterPurposes = ref([]);
const filterMaterials = ref([]);

const customerSearch = ref('');
const customerResults = ref([]);
const customerLoading = ref(false);
const showQuickAddDialog = ref(false);
const quickAddLoading = ref(false);
// Form them nhanh khach hang; chi luu vao he thong khi checkout thanh cong.
const quickAddForm = ref({ ten: '', sdt: '', email: '', gioiTinh: true, tinh: null, thanhPho: null, phuongXa: null, diaChiChiTiet: '' });

// Thong tin nhan vien ban hang hien thi cot phai.
const currentEmployeeDetail = ref(null);


// Thong tin khach hang dang gan cho hoa don POS.
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
// Thong tin nhan hang chi bat buoc khi loai don la GIAO_HANG.
const recipientName = ref('');
const recipientPhone = ref('');
const recipientAddressDetail = ref('');
const recipientProvince = ref(null);
const recipientDistrict = ref(null);
const recipientWard = ref(null);

// Cac gia tri phu anh huong tong tien don hang.
const shippingFee = ref(30000);
const surcharge = ref(0);
const isFreeShip = ref(false);
const onlyChargeIfReturned = ref(false);
const chargeTax = ref(false);
const noteType = ref('NOI_BO');

// Du lieu thanh toan: method, so tien thu va ghi chu noi bo cua giao dich.
const checkoutData = ref({
    paymentMethod: 'CASH',
    vnpayMethod: 'QR',
    receivedAmount: null,
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

const isShippingLoaiDon = (loaiDon) => loaiDon === 'GIAO_HANG' || loaiDon === 'ONLINE';

const orderChannel = computed({
    get() {
        if (!selectedOrder.value) return 'Tại quầy';
        return isShippingLoaiDon(selectedOrder.value.loaiDon) ? 'Giao hàng' : 'Tại quầy';
    },
    set(newVal) {
        if (selectedOrder.value) {
            selectedOrder.value.loaiDon = newVal === 'Giao hàng' ? 'GIAO_HANG' : 'TAI_QUAY';
        }
    }
});

const isGiaoHang = computed({
    get() {
        if (!selectedOrder.value) return false;
        return isShippingLoaiDon(selectedOrder.value.loaiDon);
    },
    set(val) {
        if (selectedOrder.value) {
            selectedOrder.value.loaiDon = val ? 'GIAO_HANG' : 'TAI_QUAY';
        }
    }
});

// Order Value Calculations
const totalRawAmount = computed(() => {
    const subtotal = Number(selectedOrder.value?.tongTien || 0);
    return subtotal + Number(shippingFee.value || 0) + Number(surcharge.value || 0);
});

const discountAmount = computed(() => {
    const raw = Number(selectedOrder.value?.tongTien || 0);
    const after = Number(selectedOrder.value?.tongTienSauGiam || 0);
    return Math.max(0, raw - after);
});

const finalCollectAmount = computed(() => {
    return Math.max(0, totalRawAmount.value - discountAmount.value);
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
    checkoutData.value.receivedAmount = null;

    if (id) {
        if (selectedOrder.value.phiVanChuyen !== undefined && selectedOrder.value.phiVanChuyen !== null) {
            shippingFee.value = Number(selectedOrder.value.phiVanChuyen);
            isFreeShip.value = Number(selectedOrder.value.phiVanChuyen) === 0 && isShippingLoaiDon(selectedOrder.value.loaiDon);
        } else {
            const channel = isShippingLoaiDon(selectedOrder.value.loaiDon) ? 'Giao hàng' : 'Tại quầy';
            shippingFee.value = channel === 'Giao hàng' ? 30000 : 0;
            isFreeShip.value = false;
        }

        const channel = isShippingLoaiDon(selectedOrder.value.loaiDon) ? 'Giao hàng' : 'Tại quầy';
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
            shippingFee.value = channel === 'Giao hàng' ? 30000 : 0;
        } else if (channel !== oldChannel && oldChannel !== undefined) {
            shippingFee.value = channel === 'Giao hàng' ? 30000 : 0;
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

// Automatically sync received amount with total payable amount when VNPay is selected
watch(
    () => [checkoutData.value.paymentMethod, finalCollectAmount.value],
    ([method, amount]) => {
        if (method === 'VNPAY') {
            checkoutData.value.receivedAmount = Number(amount);
        } else if (method === 'CASH') {
            if (checkoutData.value.receivedAmount === Number(amount)) {
                checkoutData.value.receivedAmount = null;
            }
        }
    }
);

// Search Products Autocomplete
const searchProducts = async () => {
    const kw = productSearchKeyword.value?.trim() || '';
    productSearchLoading.value = true;
    try {
        const response = await dichVuDonHang.searchSanPham(kw);
        let items = Array.isArray(response) ? response : [];
        if (onlyInStock.value) {
            items = items.filter(v => (v.soLuongTon ?? v.soLuong ?? 0) > 0);
        }
        productSearchResults.value = items;
    } catch (e) {
        console.error(e);
    } finally {
        productSearchLoading.value = false;
    }
};

const loadFilterOptions = async () => {
    try {
        const [th, xx, md, cl] = await Promise.allSettled([
            dichVuThuongHieu.layThuongHieu({ size: 1000 }),
            dichVuXuatXu.layXuatXu({ size: 1000 }),
            dichVuMucDichChay.layMucDichChay({ size: 1000 }),
            dichVuChatLieu.layChatLieu({ size: 1000 })
        ]);

        const pick = (res) => (res.status === 'fulfilled' ? (res.value?.content || res.value || []) : []);

        filterBrands.value = [{ title: 'Tất cả thương hiệu', value: 'ALL' }, ...pick(th).map(x => ({ title: x.ten, value: x.ten }))];
        filterOrigins.value = [{ title: 'Tất cả xuất xứ', value: 'ALL' }, ...pick(xx).map(x => ({ title: x.ten, value: x.ten }))];
        filterPurposes.value = [{ title: 'Tất cả mục đích', value: 'ALL' }, ...pick(md).map(x => ({ title: x.ten, value: x.ten }))];
        filterMaterials.value = [{ title: 'Tất cả chất liệu', value: 'ALL' }, ...pick(cl).map(x => ({ title: x.ten, value: x.ten }))];
    } catch (e) {
        console.error('Lỗi khi tải bộ lọc:', e);
    }
};

const filteredProductSearchResults = computed(() => {
    let list = productSearchResults.value || [];

    if (filterThuongHieu.value && filterThuongHieu.value !== 'ALL') {
        list = list.filter(v => v.tenThuongHieu === filterThuongHieu.value);
    }
    if (filterXuatXu.value && filterXuatXu.value !== 'ALL') {
        list = list.filter(v => v.tenXuatXu === filterXuatXu.value);
    }
    if (filterMucDich.value && filterMucDich.value !== 'ALL') {
        list = list.filter(v => v.tenMucDichChay === filterMucDich.value);
    }
    if (filterChatLieu.value && filterChatLieu.value !== 'ALL') {
        list = list.filter(v => v.tenChatLieu === filterChatLieu.value);
    }

    return list;
});

const groupedProducts = computed(() => {
    const list = filteredProductSearchResults.value || [];
    const grouped = {};
    for (const v of list) {
        if (!grouped[v.maSanPham]) {
            grouped[v.maSanPham] = {
                maSanPham: v.maSanPham,
                tenSanPham: v.tenSanPham,
                hinhAnh: v.hinhAnh || (v.bienTheSanPhams && v.bienTheSanPhams[0]?.anh) || '',
                thuongHieu: v.tenThuongHieu,
                variants: []
            };
        }
        grouped[v.maSanPham].variants.push(v);
    }
    return Object.values(grouped);
});

const onFilterChange = () => {
    showProductAutocomplete.value = true;
};

let searchDebounce = null;
watch(productSearchKeyword, () => {
    if (searchDebounce) clearTimeout(searchDebounce);
    searchDebounce = setTimeout(() => {
        searchProducts();
    }, 300);
});

watch(onlyInStock, () => {
    searchProducts();
});

const onProductSearchFocus = () => {
    showProductAutocomplete.value = true;
    searchProducts();
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

const isAddingProduct = ref(false);

// Modal chon bien the khi mot san pham co nhieu mau/size.
const variantModal = ref({
    show: false,
    product: null,
    variants: [],
    selectedColor: null,
    selectedSize: null,
    quantity: 1
});

// Danh sach mau duy nhat de FE hien chip mau trong modal bien the.
const uniqueColors = computed(() => {
    if (!variantModal.value.variants) return [];
    const colorMap = new Map();
    variantModal.value.variants.forEach(v => {
        const colorName = v.tenMauSac || 'Không màu';
        if (!colorMap.has(colorName)) {
            colorMap.set(colorName, v.hinhAnh || (v.bienTheSanPhams && v.bienTheSanPhams[0]?.anh) || variantModal.value.product?.hinhAnh);
        }
    });
    return Array.from(colorMap.entries()).map(([name, image]) => ({ name, image }));
});

// Danh sach size theo mau dang chon, kem ton kho de khoa size het hang.
const availableSizes = computed(() => {
    if (!variantModal.value.variants || !variantModal.value.selectedColor) return [];
    return variantModal.value.variants
        .filter(v => (v.tenMauSac || 'Không màu') === variantModal.value.selectedColor)
        .map(v => ({
            name: v.tenKichThuoc || 'Free size',
            soLuongTon: v.soLuongTon,
            variant: v
        }))
        .sort((a, b) => a.name.localeCompare(b.name));
});

// Bien the chinh xac sau khi chon mau + size.
const currentSelectedVariant = computed(() => {
    if (!variantModal.value.selectedSize) return null;
    const sizeObj = availableSizes.value.find(s => s.name === variantModal.value.selectedSize);
    return sizeObj ? sizeObj.variant : null;
});

// Bien the dung de hien anh/gia trong modal, ke ca khi chua chon du mau size.
const modalDisplayVariant = computed(() => {
    if (currentSelectedVariant.value) return currentSelectedVariant.value;
    if (!variantModal.value.variants?.length) return null;
    if (!variantModal.value.selectedColor) return variantModal.value.variants[0];
    return variantModal.value.variants.find(v => (v.tenMauSac || 'KhÃ´ng mÃ u') === variantModal.value.selectedColor)
        || variantModal.value.variants[0];
});

// Phan tram giam hien badge tren modal bien the neu co dot giam gia.
const modalDisplayDiscountPercent = computed(() => {
    const percent = Number(modalDisplayVariant.value?.phanTramGiam || 0);
    return Number.isFinite(percent) && percent > 0 ? Math.round(percent) : 0;
});

const selectColor = (colorName) => {
    variantModal.value.selectedColor = colorName;
    variantModal.value.selectedSize = null;
    variantModal.value.quantity = 1;
};

const selectSize = (sizeName, soLuongTon) => {
    if (soLuongTon === 0) return;
    variantModal.value.selectedSize = sizeName;
    variantModal.value.quantity = 1;
};

const validateQuantityInput = (e) => {
    let val = parseInt(e.target.value);
    if (isNaN(val) || val < 1) val = 1;
    const max = currentSelectedVariant.value?.soLuongTon || 1;
    if (val > max) val = max;
    variantModal.value.quantity = val;
};

const openVariantModal = (product) => {
    variantModal.value = {
        show: true,
        product: product,
        variants: product.variants,
        selectedColor: null,
        selectedSize: null,
        quantity: 1
    };

    if (uniqueColors.value.length > 0) {
        selectColor(uniqueColors.value[0].name);
    }

    productSearchKeyword.value = '';
    showProductAutocomplete.value = false;
};

const closeVariantModal = () => {
    variantModal.value.show = false;
};

const confirmAddVariants = async () => {
    const orderId = selectedOrder.value?.id;
    if (!orderId) {
        addNotification({ title: 'Lỗi', subtitle: 'Vui lòng chọn hoặc tạo hóa đơn trước.', color: 'error' });
        return;
    }

    const variant = currentSelectedVariant.value;
    if (!variant) {
        addNotification({ title: 'Chưa chọn', subtitle: 'Vui lòng chọn màu sắc và kích cỡ.', color: 'warning' });
        return;
    }

    const qty = variantModal.value.quantity;
    if (qty <= 0 || qty > variant.soLuongTon) {
        addNotification({ title: 'Lỗi', subtitle: `Số lượng không hợp lệ.`, color: 'error' });
        return;
    }

    isAddingProduct.value = true;
    try {
        const payload = {
            idChiTietSanPham: variant.id,
            soLuong: qty
        };
        const updated = await dichVuDonHang.addSanPham(orderId, payload);
        updateOrderInList(updated);

        addNotification({ title: 'Thành công', subtitle: `Đã thêm sản phẩm vào hóa đơn.`, color: 'success' });
        refreshBestVoucher(selectedOrder.value);
        closeVariantModal();
    } catch (e) {
        console.error(e);
        addNotification({ title: 'Lỗi', subtitle: getErrorMessage(e, 'Thêm sản phẩm thất bại.'), color: 'error' });
    } finally {
        isAddingProduct.value = false;
    }
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
            input.focus();
            input.showPicker();
        } catch (e) {
            // Silently ignore browser restrictions on showPicker without direct gesture
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
        email: email || `${sdt}@khachhang.com`,
        gioiTinh: gt !== null ? gt : true,
        ngaySinh: ns
    };

    const created = await dichVuKhachHang.taoKhachHang(newCustomerPayload);
    if (created?.id) {
        if (recipientProvince.value && recipientDistrict.value && recipientWard.value && recipientAddressDetail.value) {
            try {
                const p = provincesShip.value.find(x => x.code === recipientProvince.value);
                const d = districtsShip.value.find(x => x.code === recipientDistrict.value);
                const w = wardsShip.value.find(x => x.code === recipientWard.value);
                await dichVuKhachHang.taoDiaChi({
                    idKhachHang: created.id,
                    tinh: p ? p.name : '',
                    thanhPho: d ? d.name : '',
                    phuongXa: w ? w.name : '',
                    diaChiChiTiet: recipientAddressDetail.value,
                    tenNguoiNhan: recipientName.value || ten,
                    sdtNguoiNhan: recipientPhone.value || sdt,
                    laMacDinh: true
                });
            } catch (e) {
                console.error('Không thể tự động lưu địa chỉ cho khách hàng mới', e);
            }
        }
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

// QR / Barcode Scanner Logic: doc ma SKU/ma vach de them nhanh bien the vao gio.
const showScanner = ref(false);
let html5QrcodeScanner = null;
let scannerStartAttempts = 0;
const SCANNER_ELEMENT_ID = 'pos-qr-reader';

const startScanner = () => {
    showScanner.value = true;
    scannerStartAttempts = 0;
    setTimeout(initScanner, 150);
};

const initScanner = () => {
    if (!showScanner.value) return;

    const el = document.getElementById(SCANNER_ELEMENT_ID);
    if (!el || el.clientWidth === 0) {
        scannerStartAttempts += 1;
        if (scannerStartAttempts > 20) {
            addNotification({ title: 'Không thể mở camera', subtitle: 'Không tìm thấy khung quét QR. Vui lòng thử lại.', color: 'error' });
            stopScanner();
            return;
        }
        setTimeout(initScanner, 150);
        return;
    }

    if (html5QrcodeScanner) {
        html5QrcodeScanner.clear().catch(e => console.error(e));
    }

    html5QrcodeScanner = new Html5QrcodeScanner(SCANNER_ELEMENT_ID, { fps: 10, qrbox: { width: 250, height: 250 } }, false);
    html5QrcodeScanner.render(onScanSuccess, onScanFailure);
};

// Chuan hoa QR/barcode tu JSON, URL hoac text thuong ve ma bien the.
const normalizeScannedCode = (rawValue) => {
    const raw = String(rawValue || '').trim();
    if (!raw) return '';

    try {
        const parsed = JSON.parse(raw);
        if (parsed && typeof parsed === 'object') {
            const value = parsed.maChiTietSanPham || parsed.sku || parsed.code || parsed.ma || parsed.barcode;
            if (value) return String(value).trim();
        }
    } catch (e) {
        // Not JSON, continue with URL/plain text parsing.
    }

    try {
        const url = new URL(raw);
        const paramKeys = ['maChiTietSanPham', 'sku', 'code', 'ma', 'barcode', 'q'];
        for (const key of paramKeys) {
            const value = url.searchParams.get(key);
            if (value) return value.trim();
        }

        const lastSegment = decodeURIComponent(url.pathname.split('/').filter(Boolean).pop() || '');
        if (lastSegment) return lastSegment.trim();
    } catch (e) {
        // Not a URL, keep parsing as plain text.
    }

    const labeled = raw.match(/(?:maChiTietSanPham|sku|code|barcode|ma)\s*[:=]\s*([A-Za-z0-9_-]+)/i);
    if (labeled?.[1]) return labeled[1].trim();

    return raw;
};

// Uu tien bien the khop chinh xac SKU de tranh them nham khi API tra nhieu ket qua.
const findExactScannedVariant = (variants, keyword) => {
    const normalizedKeyword = String(keyword || '').trim().toUpperCase();
    return variants.find((variant) => {
        const variantCode = String(variant.maChiTietSanPham || '').trim().toUpperCase();
        return variantCode === normalizedKeyword;
    }) || null;
};

const stopScanner = () => {
    if (html5QrcodeScanner) {
        html5QrcodeScanner.clear().catch((error) => console.error('Failed to clear scanner', error));
        html5QrcodeScanner = null;
    }
    showScanner.value = false;
};

// Xu ly sau khi quet thanh cong: tim bien the, check trang thai/ton kho, roi them vao gio.
const onScanSuccess = async (decodedText) => {
    stopScanner();
    const keyword = normalizeScannedCode(decodedText);
    if (!keyword) return;
    if (!selectedOrder.value) {
        addNotification({ title: 'Chưa có hóa đơn', subtitle: 'Vui lòng tạo hoặc chọn hóa đơn trước khi quét mã.', color: 'warning' });
        return;
    }

    try {
        const variants = await dichVuDonHang.searchSanPham(keyword);
        if (variants && variants.length > 0) {
            const exactMatch = findExactScannedVariant(variants, keyword) || variants[0];

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

// Phim tat POS: F9 mo QR, F10 thanh toan, may quet barcode go phim roi Enter.
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
            const res = await dichVuNhanVien.layTatCaNhanVien();
            let list = [];
            if (res && res.data) {
                if (Array.isArray(res.data.content)) {
                    list = res.data.content;
                } else if (Array.isArray(res.data)) {
                    list = res.data;
                }
            } else if (res && Array.isArray(res)) {
                list = res;
            }
            const matched = list.find(e => e.tenTaiKhoan?.toLowerCase() === authStore.user.username?.toLowerCase());
            if (matched) {
                currentEmployeeDetail.value = matched;
            }
        }
    } catch (e) {
        console.error('Error fetching employee detail:', e);
    }
};

const updateShippingFee = (e) => {
    shippingFee.value = parseNumberFromDots(e.target.value);
};

const setShippingFee = (fee) => {
    shippingFee.value = fee;
};

const setPaymentMethod = (method) => {
    checkoutData.value.paymentMethod = method;
};

const updateReceivedAmount = (e) => {
    checkoutData.value.receivedAmount = parseNumberFromDots(e.target.value);
};

const closeCustomerSuggestions = () => {
    showCustomerSuggestions.value = false;
};

const setActiveOrderIndex = (idx) => {
    activeOrderIndex.value = idx;
};

// Lifecycle
onMounted(async () => {
    // Tự động thu gọn sidebar khi vào màn hình Bán hàng
    uiStore.setSidebarCollapsed(true);

    uiStore.setBreadcrumbs([
        { title: 'Bán hàng', disabled: false, href: '/admin/ban-hang' },
        { title: 'Tạo đơn hàng', disabled: true }
    ]);
    window.addEventListener('keydown', handleGlobalKeyDown);
    loading.value = true;
    try {
        fetchProvincesShip();
        await loadCurrentEmployeeDetails();
        await loadFilterOptions();
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

// Watchers for vouchers
watch(() => selectedOrder.value?.id, (id) => {
    if (id) refreshBestVoucher();
});

watch(
    () => [selectedOrder.value?.id, selectedOrder.value?.idKhachHang, selectedOrder.value?.tongTien],
    () => {
        if (selectedOrder.value?.id) refreshBestVoucher();
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

                    recipientName.value = customer.ten || '';
                    recipientPhone.value = customer.sdt || '';

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
            soLuong: product._soLuongMuonThem || 1
        });
        updateOrderInList(updated);
        addNotification({ title: 'Thành công', subtitle: 'Đã thêm sản phẩm vào giỏ hàng', color: 'success' });
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
        customerForm.value = { ten: '', sdt: '', email: '', gioiTinh: true, tinh: null, thanhPho: null, phuongXa: null, diaChiChiTiet: '' };
        recipientName.value = '';
        recipientPhone.value = '';
        recipientProvince.value = null;
        recipientDistrict.value = null;
        recipientWard.value = null;
        recipientAddressDetail.value = '';
        addNotification({ title: 'Thêm khách hàng thành công.', subtitle: 'Đã gỡ khách hàng khỏi hóa đơn.', color: 'success' });
    } catch (e) {
        addNotification({ title: 'Không thêm được khách hàng.', subtitle: getErrorMessage(e, 'Không thể gỡ khách hàng khỏi hóa đơn.'), color: 'error' });
    }
};

// Logic: Voucher
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
    }).map(v => {
        const type = String(v.loaiPhieu || '').toUpperCase();
        const isPercent = type === 'PHAN_TRAM' || type === 'PERCENT';
        const valStr = isPercent ? `${v.phanTramGiamGia}%` : formatCurrency(v.soTienGiam || 0);
        return {
            ...v,
            customTitle: `Giảm ${valStr} - ${v.ten} (${v.ma})`
        };
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

// Làm mới danh sách voucher và tự động áp dụng mã có lợi nhất (Offline)
const refreshBestVoucher = (orderOverride = selectedOrder.value) => {
    if (!orderOverride?.id) return;

    const availableVouchers = loadEligibleVouchers(orderOverride);
    vouchers.value = availableVouchers || [];

    const bestVoucher = pickBestVoucher(vouchers.value, orderOverride.tongTien || 0);
    const bestVoucherId = bestVoucher?.id || null;

    // Gợi ý mã tốt nhất vào state để UI hiển thị
    orderOverride.suggestedVoucherId = bestVoucherId;

    // Luôn tự động áp dụng mã tốt nhất mỗi khi giỏ hàng thay đổi (real-time)
    orderOverride.idPhieuGiamGia = bestVoucherId;

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
let suppressVnPayCloseCancel = false;

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
    suppressVnPayCloseCancel = true;
    vnpayDialog.value.show = false;
    vnpayDialog.value.loading = false;
    vnpayDialog.value.verified = false;
    vnpayPopup = null;
    setTimeout(() => {
        suppressVnPayCloseCancel = false;
    }, 0);
};

const cancelVnPayFlow = () => {
    if (suppressVnPayCloseCancel) return;
    sessionStorage.removeItem(VNPAY_PENDING_KEY);
    closeVnPayFlow();
    addNotification({ title: 'Hủy thanh toán', subtitle: 'Giao dịch VNPay đã được hủy bỏ', color: 'info' });
};

const onVnPayDialogVisibilityChange = (visible) => {
    if (visible || suppressVnPayCloseCancel) return;
    if (vnpayDialog.value.loading || vnpayDialog.value.verified) {
        closeVnPayFlow();
        return;
    }
    cancelVnPayFlow();
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

const getShippingAddressPayload = () => {
    const province = provincesShip.value.find(x => x.code === recipientProvince.value);
    const district = districtsShip.value.find(x => x.code === recipientDistrict.value);
    const ward = wardsShip.value.find(x => x.code === recipientWard.value);
    const tenNguoiNhan = (recipientName.value || customerForm.value.ten || '').trim();
    const sdtNguoiNhan = (recipientPhone.value || customerForm.value.sdt || '').trim();
    const diaChiChiTiet = (recipientAddressDetail.value || '').trim();
    const tinh = province ? province.name : '';
    const thanhPho = district ? district.name : '';
    const phuongXa = ward ? ward.name : '';
    const diaChiNguoiNhan = [diaChiChiTiet, phuongXa, thanhPho, tinh].filter(Boolean).join(', ');

    return {
        tenNguoiNhan,
        sdtNguoiNhan,
        diaChiChiTiet,
        tinh,
        thanhPho,
        phuongXa,
        diaChiNguoiNhan
    };
};

const hasCompleteShippingAddress = () => {
    const shipping = getShippingAddressPayload();
    return Boolean(shipping.tenNguoiNhan && shipping.sdtNguoiNhan && shipping.diaChiChiTiet && shipping.tinh && shipping.thanhPho && shipping.phuongXa);
};

const getPendingCustomerPayload = () => {
    if (selectedOrder.value?.idKhachHang) {
        return {};
    }

    const ten = customerForm.value.ten?.trim() || null;
    const sdt = customerForm.value.sdt?.trim() || null;
    const email = customerForm.value.email?.trim() || null;
    const ngaySinh = customerForm.value.ngaySinh?.trim() || null;
    const gioiTinh = customerForm.value.gioiTinh === 'Nam'
        ? true
        : customerForm.value.gioiTinh === 'Nữ'
            ? false
            : null;

    return {
        tenKhachHang: ten,
        sdtKhachHang: sdt,
        emailKhachHang: email,
        gioiTinhKhachHang: gioiTinh,
        ngaySinhKhachHang: ngaySinh
    };
};

const hasPendingCustomerInput = () => {
    const customer = getPendingCustomerPayload();
    return Boolean(customer.tenKhachHang || customer.sdtKhachHang || customer.emailKhachHang || customer.ngaySinhKhachHang || customer.gioiTinhKhachHang !== null);
};

const validatePendingCustomer = () => {
    if (selectedOrder.value?.idKhachHang || !hasPendingCustomerInput()) {
        return;
    }

    const customer = getPendingCustomerPayload();
    if (!customer.tenKhachHang || !customer.sdtKhachHang) {
        throw new Error('Vui lòng nhập đầy đủ Tên khách hàng và SĐT để thêm khách hàng mới khi thanh toán.');
    }
};

const buildCheckoutPayload = (order, overrides = {}) => {
    let compiledNote = checkoutData.value.note || '';
    const shipping = getShippingAddressPayload();
    if (isGiaoHang.value) {
        const shippingDetails = [
            `Người nhận: ${shipping.tenNguoiNhan}`,
            `SĐT: ${shipping.sdtNguoiNhan}`,
            `Địa chỉ: ${shipping.diaChiNguoiNhan}`
        ].filter(Boolean).join(', ');

        compiledNote = compiledNote
            ? `${compiledNote} | Ship: ${shippingDetails}`
            : `Ship: ${shippingDetails}`;
    }

    return {
        idKhachHang: order?.idKhachHang || null,
        ...getPendingCustomerPayload(),
        idPhieuGiamGia: order?.idPhieuGiamGia || null,
        tongTien: order?.tongTien || 0,
        phiVanChuyen: shippingFee.value,
        tongTienSauGiam: finalCollectAmount.value,
        loaiDon: isGiaoHang.value ? 'GIAO_HANG' : 'TAI_QUAY',
        tenNguoiNhan: isGiaoHang.value ? shipping.tenNguoiNhan : null,
        sdtNguoiNhan: isGiaoHang.value ? shipping.sdtNguoiNhan : null,
        diaChiNguoiNhan: isGiaoHang.value ? shipping.diaChiNguoiNhan : null,
        tinh: isGiaoHang.value ? shipping.tinh : null,
        thanhPho: isGiaoHang.value ? shipping.thanhPho : null,
        phuongXa: isGiaoHang.value ? shipping.phuongXa : null,
        diaChiChiTiet: isGiaoHang.value ? shipping.diaChiChiTiet : null,
        luuDiaChiMacDinh: isGiaoHang.value && hasCompleteShippingAddress(),
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
    checkoutData.value.receivedAmount = null;
    checkoutData.value.note = '';
};

// Dung chung API in hoa don cua man Hoa don de POS co mau in/thong tin thong nhat.
const printInvoiceFromHoaDon = async (orderId, options = {}) => {
    if (!orderId) return false;
    const shouldNotifyPreparing = options.notifyPreparing !== false;
    let printWindow = options.printWindow || null;

    try {
        if (shouldNotifyPreparing) {
            addNotification({ title: 'Đang chuẩn bị', subtitle: 'Đang tạo bản in hóa đơn...', color: 'info' });
        }

        if (!printWindow) {
            printWindow = window.open('', '_blank', 'width=900,height=1000');
        }

        if (!printWindow) {
            addNotification({
                title: 'Không thể mở bản in',
                subtitle: 'Trình duyệt đã chặn cửa sổ bật lên. Vui lòng cho phép popup để in hóa đơn.',
                color: 'warning'
            });
            return false;
        }

        printWindow.document.write('<!doctype html><html><head><title>Đang tạo hóa đơn...</title></head><body style="font-family:Arial,sans-serif;padding:24px">Đang tạo bản in hóa đơn...</body></html>');
        printWindow.document.close();

        const html = await dichVuHoaDon.inHoaDon(orderId);
        printWindow.document.open();
        printWindow.document.write(html);
        printWindow.document.close();
        printWindow.onload = () => {
            setTimeout(() => {
                printWindow.print();
            }, 500);
        };
        return true;
    } catch (error) {
        console.error('Print invoice error:', error);
        if (options.printWindow && !options.printWindow.closed) {
            options.printWindow.close();
        }
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải bản in hóa đơn từ máy chủ.', color: 'error' });
        return false;
    }
};

const onPrintInvoice = async () => {
    if (!selectedOrder.value?.id) return;
    await printInvoiceFromHoaDon(selectedOrder.value.id);
};

// Ham chot thanh toan dung chung cho tien mat va VNPay, co tuy chon in sau thanh toan.
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
    const postCheckoutPrintWindow = showReceiptAfter
        ? window.open('', '_blank', 'width=900,height=1000')
        : null;

    try {
        await dichVuDonHang.checkout(order.id, requestPayload);
        addNotification({ title: 'Thành công', subtitle: successMessage, color: 'success' });

        if (showReceiptAfter) {
            await printInvoiceFromHoaDon(order.id, {
                printWindow: postCheckoutPrintWindow,
                notifyPreparing: false
            });
        }

        // Xóa order khỏi danh sách
        await completePaidOrder(order.id);
    } catch (e) {
        if (postCheckoutPrintWindow && !postCheckoutPrintWindow.closed) {
            postCheckoutPrintWindow.close();
        }
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

// Hoan tat VNPay sau khi xac nhan giao dich va ghi ma doi soat vao hoa don.
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
            amount: finalCollectAmount.value,
            transactionId: orderId,
            checkoutPayload: buildCheckoutPayload(selectedOrder.value, {
                tienChuyenKhoan: finalCollectAmount.value
            })
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
            qrUrl: `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(data.paymentUrl)}`,
            paymentUrl: data.paymentUrl,
            pollInterval: null
        };

        if (checkoutData.value.vnpayMethod === 'QR') {
            return;
        }

        vnpayDialog.value.pollInterval = setInterval(async () => {
            let isClosed = false;
            try {
                isClosed = !vnpayPopup || vnpayPopup.closed;
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
                const currentUrl = vnpayPopup.location.href;
                if (currentUrl && (currentUrl.includes('/vnpay-callback') || currentUrl.includes('vnp_ResponseCode'))) {
                    clearVnPayPolling();

                    vnpayDialog.value.loading = true;
                    vnpayDialog.value.statusText = 'Đang kiểm tra kết quả giao dịch...';

                    const urlObj = new URL(currentUrl);
                    const params = {};
                    urlObj.searchParams.forEach((val, key) => {
                        params[key] = val;
                    });

                    vnpayPopup.close();
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

    isProcessing.value = true;
    try {
        validatePendingCustomer();
        if (isGiaoHang.value && !hasCompleteShippingAddress()) {
            throw new Error('Vui lòng nhập đầy đủ tên, SĐT và địa chỉ nhận hàng.');
        }
    } catch (e) {
        addNotification({ title: 'Cảnh báo', subtitle: e.message || 'Thông tin khách hàng chưa hợp lệ.', color: 'warning' });
        isProcessing.value = false;
        return;
    }
    isProcessing.value = false;

    if (checkoutData.value.paymentMethod === 'VNPAY') {
        startVnPayFlow();
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
                const isCash = checkoutData.value.paymentMethod === 'CASH';
                const payload = buildCheckoutPayload(selectedOrder.value, {
                    tienMat: isCash ? finalCollectAmount.value : 0,
                    tienChuyenKhoan: isCash ? 0 : finalCollectAmount.value
                });
                await submitCheckout({ payload });

                checkoutData.value.receivedAmount = null;
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

const getStoredVnPayPending = () => {
    try {
        const pending = JSON.parse(sessionStorage.getItem(VNPAY_PENDING_KEY) || '{}');
        return pending?.orderId ? pending : null;
    } catch (e) {
        return null;
    }
};

const getStoredVnPayOrder = () => {
    const pending = getStoredVnPayPending();
    if (!pending?.orderId) return null;
    return orders.value.find((order) => order.id === pending.orderId) || null;
};

const handleVnPayCallbackFromUrl = async () => {
    const params = new URLSearchParams(window.location.search);
    if (!params.has('vnp_ResponseCode')) return;

    const callbackParams = {};
    params.forEach((value, key) => {
        callbackParams[key] = value;
    });

    const pending = getStoredVnPayPending();
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
        const checkoutPayload = pending?.checkoutPayload
            ? {
                ...pending.checkoutPayload,
                tienChuyenKhoan: pending.checkoutPayload.tienChuyenKhoan || pending.amount || pendingOrder.tongTienSauGiam,
                maGiaoDich: transactionNo
            }
            : buildCheckoutPayload(pendingOrder, {
                tienChuyenKhoan: pendingOrder.tongTienSauGiam,
                maGiaoDich: transactionNo
            });
        await submitCheckout({
            order: pendingOrder,
            payload: checkoutPayload
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
    const content = allCustomers?.content || allCustomers?.data?.content || allCustomers?.data || allCustomers || [];
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
            email: email || `${phone}@khachhang.com`,
            gioiTinh: quickAddForm.value.gioiTinh !== null ? quickAddForm.value.gioiTinh : true,
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
    if (!val && val !== 0) return null;
    const clean = String(val).replace(/\D/g, '');
    return clean ? Number(clean) : null;
};

const formatDateTime = (dateStr) => {
    if (!dateStr) return '21:58 18/06/2026';
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;
    const pad = (n) => String(n).padStart(2, '0');
    return `${pad(date.getHours())}:${pad(date.getMinutes())} ${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()}`;
};

const formatVNPayAmount = (amount) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);
};

const openVnPayPopup = () => {
    vnpayPopup = window.open(vnpayDialog.value.paymentUrl, 'vnpay', 'width=800,height=600');
};

const closeVnPayChoice = () => {
    vnpayChoiceDialog.value.show = false;
};

const onQuickAddPhoneInput = (event) => {
    quickAddForm.value.sdt = String(event.target.value || '').replace(/[^0-9]/g, '');
};

const onQuickAddTinhChange = (val) => {
    quickAddForm.value.thanhPho = null;
    quickAddForm.value.phuongXa = null;
    if (val) fetchDistricts(val);
};

const onQuickAddHuyenChange = (val) => {
    quickAddForm.value.phuongXa = null;
    if (val) fetchWards(val);
};

const closeQuickAdd = () => {
    showQuickAddDialog.value = false;
};
</script>

<template>
    <v-container fluid class="pos-wrapper pa-0">
        <div class="pos-shell">
            <!-- Header section containing title and tabs -->
            <header class="pos-header-row d-flex align-center justify-space-between">
                <div class="d-flex align-center gap-4">
                    <OrderTabs :orders="orders" :active-index="activeOrderIndex" @select="setActiveOrderIndex"
                        @create="createNewOrder" @close="closeOrder" />
                </div>
            </header>

            <!-- Main Workspace Grid -->
            <v-row v-if="selectedOrder" class="pos-grid">
                <!-- Left Column (8 cols out of 12) -->
                <v-col cols="12" lg="8" class="d-flex flex-column gap-4 pr-lg-2">
                    <!-- Sản phẩm Card -->
                    <v-card class="pos-card pa-4 rounded-lg border"
                        style="overflow: visible !important; z-index: 15 !important;">

                        <!-- Search and Scan Inner Row -->
                        <div class="d-flex align-center gap-2 mb-3 bg-slate-50 pa-1.5 rounded-lg flex-wrap">
                            <span class="font-weight-bold text-slate-800 px-2"
                                style="font-size: 14px !important; width: 94px; display: inline-block; flex-shrink: 0; box-sizing: border-box;">Tìm
                                kiếm</span>

                            <!-- Custom Search Input with floating Dropdown -->
                            <div class="position-relative flex-grow-1 min-w-[200px]">
                                <v-text-field v-model="productSearchKeyword"
                                    placeholder="Nhập mã, tên sản phẩm hoặc Barcode" variant="outlined"
                                    density="compact" hide-details prepend-inner-icon="mdi-magnify"
                                    @focus="onProductSearchFocus" @click="onProductSearchFocus"
                                    @blur="onProductSearchBlur" bg-color="white" class="search-input"
                                    autocomplete="off" />

                                <!-- Search dropdown overlay -->
                                <v-card v-if="showProductAutocomplete && groupedProducts.length > 0"
                                    class="position-absolute mt-1 shadow-lg border rounded-lg overflow-y-auto product-dropdown-card"
                                    style="max-height: 495px !important; z-index: 1000">
                                    <v-list class="pa-0">
                                        <v-list-item v-for="product in groupedProducts" :key="product.maSanPham"
                                            @mousedown="openVariantModal(product)"
                                            class="border-b py-3 hover-autocomplete-item pointer">
                                            <div class="d-flex justify-space-between w-100 align-start">
                                                <div class="d-flex align-start flex-grow-1">
                                                    <v-avatar rounded="lg" size="48"
                                                        class="mr-3 bg-grey-lighten-4 border flex-shrink-0">
                                                        <v-img v-if="product.hinhAnh" :src="product.hinhAnh" cover />
                                                        <BoxIcon v-else size="20" class="text-grey" />
                                                    </v-avatar>
                                                    <div class="d-flex flex-column" style="gap: 8px !important;">
                                                        <div class="text-slate-700"
                                                            style="font-size: 13.5px !important; line-height: 1.3;">
                                                            {{ product.tenSanPham }}
                                                        </div>
                                                        <div class="d-flex align-center gap-1.5 mt-0.5 flex-wrap">
                                                            <span class="sp-badge">Mã Sản phẩm: {{ product.maSanPham ||
                                                                'SP0001' }}</span>
                                                            <span
                                                                style="margin-left: 15px; margin-right: 15px; font-size: 11px; color: #cbd5e1; opacity: 0.4;">|</span>
                                                            <span class="sku-badge">{{ product.variants.length }} biến
                                                                thể</span>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div class="text-right flex-shrink-0 pl-3">
                                                    <div class="price-text text-success font-weight-bold"
                                                        style="font-size: 13.5px;">
                                                        {{ product.variants.length > 0 ?
                                                        formatCurrency(product.variants[0].giaBan) : '' }}
                                                    </div>
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

                            <v-btn color="primary" variant="outlined" class="scanner-btn text-none px-1"
                                style="width: 82px; flex-shrink: 0;" @click="startScanner">
                                <v-icon class="mr-1">mdi-barcode-scan</v-icon>
                            </v-btn>
                        </div>

                        <!-- Filters Row -->
                        <div class="d-flex align-center gap-2 mb-3 bg-slate-50 pa-1.5 rounded-lg flex-wrap">

                            <div class="flex-grow-1"
                                style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; align-items: center;">
                                <!-- Thương hiệu -->
                                <div class="d-flex align-center gap-2">
                                    <span class="text-slate-600 text-caption font-weight-medium flex-shrink-0"
                                        style="font-size: 12px !important; white-space: nowrap;">Thương hiệu</span>
                                    <v-select v-model="filterThuongHieu" :items="filterBrands" item-title="title"
                                        item-value="value" density="compact" hide-details variant="outlined"
                                        bg-color="white" class="compact-select flex-grow-1"
                                        @update:model-value="onFilterChange" />
                                </div>

                                <!-- Xuất xứ -->
                                <div class="d-flex align-center gap-2">
                                    <span class="text-slate-600 text-caption font-weight-medium flex-shrink-0"
                                        style="font-size: 12px !important; white-space: nowrap;">Xuất xứ</span>
                                    <v-select v-model="filterXuatXu" :items="filterOrigins" item-title="title"
                                        item-value="value" density="compact" hide-details variant="outlined"
                                        bg-color="white" class="compact-select flex-grow-1"
                                        @update:model-value="onFilterChange" />
                                </div>

                                <!-- Mục đích chạy -->
                                <div class="d-flex align-center gap-2">
                                    <span class="text-slate-600 text-caption font-weight-medium flex-shrink-0"
                                        style="font-size: 12px !important; white-space: nowrap;">Mục đích</span>
                                    <v-select v-model="filterMucDich" :items="filterPurposes" item-title="title"
                                        item-value="value" density="compact" hide-details variant="outlined"
                                        bg-color="white" class="compact-select flex-grow-1"
                                        @update:model-value="onFilterChange" />
                                </div>

                                <!-- Chất liệu -->
                                <div class="d-flex align-center gap-2">
                                    <span class="text-slate-600 text-caption font-weight-medium flex-shrink-0"
                                        style="font-size: 12px !important; white-space: nowrap;">Chất liệu</span>
                                    <v-select v-model="filterChatLieu" :items="filterMaterials" item-title="title"
                                        item-value="value" density="compact" hide-details variant="outlined"
                                        bg-color="white" class="compact-select flex-grow-1"
                                        @update:model-value="onFilterChange" />
                                </div>
                            </div>
                        </div>



                        <!-- Cart list rendering -->
                        <div class="cart-container-box border rounded-lg overflow-hidden"
                            style="height: 250px; background-color: #ffffff !important;">
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
                                    style="font-size: 13px !important; color: #94a3b8 !important;">Giỏ hàng trống</div>
                            </div>
                        </div>
                    </v-card>
                    <!-- Row of Price & Payment cards -->
                    <v-row no-gutters>
                        <!-- Left Block: Pricing Details -->
                        <v-col cols="12" md="6" class="pr-md-3 pr-0 mb-4 mb-md-0">
                            <v-card class="pos-card pa-4 rounded-lg border h-100 d-flex flex-column">
                                <div class="d-flex justify-space-between align-center mb-3">
                                    <h3 class="font-weight-bold text-slate-800 m-0" style="font-size: 14px !important">
                                        Tổng đơn hàng</h3>
                                </div>

                                <div class="d-flex align-center gap-6 mb-4 border-b pb-3">
                                    <div class="d-flex align-center cursor-pointer" @click="isGiaoHang = false">
                                        <v-icon :color="!isGiaoHang ? 'primary' : 'grey-lighten-1'" class="mr-2"
                                            size="20">
                                            {{ !isGiaoHang ? 'mdi-radiobox-marked' : 'mdi-radiobox-blank' }}
                                        </v-icon>
                                        <span
                                            :class="!isGiaoHang ? 'font-weight-bold text-slate-800' : 'text-slate-600'"
                                            style="font-size: 13px !important">Mua hàng tại quầy</span>
                                    </div>
                                    <div class="d-flex align-center cursor-pointer" @click="isGiaoHang = true">
                                        <v-icon :color="isGiaoHang ? 'primary' : 'grey-lighten-1'" class="mr-2"
                                            size="20">
                                            {{ isGiaoHang ? 'mdi-radiobox-marked' : 'mdi-radiobox-blank' }}
                                        </v-icon>
                                        <span :class="isGiaoHang ? 'font-weight-bold text-slate-800' : 'text-slate-600'"
                                            style="font-size: 13px !important">Giao hàng</span>
                                    </div>
                                </div>

                                <div class="flex-grow-1 d-flex flex-column" style="gap: 20px;">
                                    <!-- Voucher Selection -->
                                    <div class="d-flex align-center justify-space-between">
                                        <span class="text-slate-600" style="font-size: 13px !important">Phiếu giảm
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
                                    <div v-if="isGiaoHang" class="d-flex align-center justify-space-between">
                                        <div class="d-flex flex-column">
                                            <span class="text-slate-600" style="font-size: 13px !important">Phí vận
                                                chuyển</span>
                                            <v-checkbox v-model="isFreeShip" label="Miễn phí vận chuyển" hide-details
                                                density="compact" color="primary"
                                                class="text-caption font-weight-medium"
                                                style="margin-top: -6px; transform: scale(0.9); transform-origin: left top" />
                                        </div>
                                        <v-menu offset="4">
                                            <template v-slot:activator="{ props }">
                                                <v-text-field :model-value="formatNumberWithDots(shippingFee)"
                                                    @input="e => { shippingFee = parseNumberFromDots(e.target.value); e.target.value = formatNumberWithDots(shippingFee); }"
                                                    @keypress="e => { if (!/^[0-9]$/.test(e.key)) e.preventDefault() }"
                                                    v-bind="props" variant="outlined" density="compact" suffix="đ"
                                                    hide-details
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
                                                    <v-list-item @click="setShippingFee(30000)"
                                                        class="px-3 py-2 cursor-pointer hover-bg-slate-50 text-caption text-slate-800 font-weight-medium">
                                                        30.000 <span class="text-decoration-underline">đ</span>
                                                    </v-list-item>
                                                </div>
                                                <!-- Ngoại thành -->
                                                <div>
                                                    <div class="bg-slate-100 text-slate-700 px-3 py-1 font-weight-bold"
                                                        style="font-size: 11px;">Ngoại thành:</div>
                                                    <v-list-item @click="setShippingFee(30000)"
                                                        class="px-3 py-2 cursor-pointer hover-bg-slate-50 text-caption text-slate-800 font-weight-medium">
                                                        30.000 <span class="text-decoration-underline">đ</span>
                                                    </v-list-item>
                                                </div>
                                                <!-- Ngoại tỉnh -->
                                                <div>
                                                    <div class="bg-slate-100 text-slate-700 px-3 py-1 font-weight-bold"
                                                        style="font-size: 11px;">Ngoại tỉnh:</div>
                                                    <v-list-item @click="setShippingFee(30000)"
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
                                        <span class="text-slate-600" style="font-size: 13px !important">Giảm giá</span>
                                        <span class="font-weight-bold"
                                            style="font-size: 13px !important; color: #dc2626;">
                                            {{ discountAmount > 0 ? '-' : '' }}{{ formatCurrency(discountAmount) }}
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
                                        <button type="button" @click="setPaymentMethod('CASH')"
                                            :class="['px-3 d-flex align-center justify-center transition-all',
                                                checkoutData.paymentMethod === 'CASH' ? 'cash-active-btn' : 'payment-inactive-btn']"
                                            style="font-size: 13px !important; border: 1px solid; border-radius: 0px !important; height: 32px; min-width: 90px; cursor: pointer;">
                                            <v-icon class="mr-1" size="16">mdi-cash</v-icon>
                                            Tiền mặt
                                        </button>
                                        <button type="button" @click="setPaymentMethod('VNPAY')"
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
                                        @input="e => { checkoutData.receivedAmount = parseNumberFromDots(e.target.value); e.target.value = formatNumberWithDots(checkoutData.receivedAmount); }"
                                        @keypress="e => { if (!/^[0-9]$/.test(e.key)) e.preventDefault() }"
                                        variant="outlined" density="compact" suffix="đ" hide-details
                                        :disabled="checkoutData.paymentMethod === 'VNPAY'"
                                        style="width: 200px !important; max-width: 200px !important; min-width: 200px !important; flex: none !important;"
                                        :class="{ 'bg-slate-50 opacity-80': checkoutData.paymentMethod === 'VNPAY', 'text-right-input': true }" />
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

                            <!-- Notes Card -->
                            <v-card class="pos-card pa-4 rounded-lg border flex-grow-1 d-flex flex-column"
                                style="min-height: 140px">
                                <div class="text-slate-800 mb-2" style="font-size: 13px !important">Ghi chú</div>

                                <v-textarea v-model="checkoutData.note"
                                    placeholder="Viết ghi chú hoặc /shortcut để ghi chú nhanh" variant="outlined"
                                    rows="2" hide-details class="flex-grow-1 note-textarea text-body-2" />
                            </v-card>
                        </v-col>
                    </v-row>
                </v-col>

                <!-- Right Column (4 cols out of 12) -->
                <v-col cols="12" lg="4" class="d-flex flex-column gap-4 pl-lg-2 mt-4 mt-lg-0">
                    <!-- Thông tin Card -->
                    <v-card class="pos-card pa-4 rounded-lg border">
                        <div class="d-flex justify-space-between align-center border-b pb-2 mb-3">
                            <span class="font-weight-bold text-slate-800" style="font-size: 14px !important">Thông
                                tin</span>
                            <v-icon size="16">mdi-chevron-up</v-icon>
                        </div>

                        <div class="d-flex flex-column gap-2">
                            <div class="d-flex align-center justify-space-between text-body-2 py-1">
                                <span class="text-slate-600 font-weight-medium">Tạo lúc</span>
                                <div class="d-flex align-center border rounded px-2 py-1 bg-slate-50"
                                    style="min-width: 170px; justify-content: space-between">
                                    <span class="font-weight-bold text-slate-800 text-caption">{{
                                        formatDateTime(selectedOrder?.ngayTao)
                                        }}</span>
                                </div>
                            </div>

                            <div class="d-flex align-center justify-space-between text-body-2 py-1">
                                <span class="text-slate-600 font-weight-medium">Mã nhân viên</span>
                                <div class="d-flex align-center border rounded px-2 py-1 bg-slate-50"
                                    style="min-width: 170px; justify-content: space-between">
                                    <span class="font-weight-bold text-slate-800 text-caption">
                                        {{ currentEmployeeDetail?.ma || authStore.user?.username || 'admin1' }}
                                    </span>
                                </div>
                            </div>

                            <div class="d-flex align-center justify-space-between text-body-2 py-1">
                                <span class="text-slate-600 font-weight-medium">Tên nhân viên</span>
                                <div class="d-flex align-center border rounded px-2 py-1 bg-slate-50"
                                    style="min-width: 170px; justify-content: space-between">
                                    <span class="font-weight-bold text-slate-800 text-caption">
                                        {{ currentEmployeeDetail?.ten || authStore.user?.hoTen ||
                                            (authStore.user?.username === 'admin'
                                                ? 'Quản trị viên' : (authStore.user?.username || 'Tường San')) }}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </v-card>

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

                        <div class="d-flex flex-column gap-1">
                            <div class="d-flex gap-1 mb-1">
                                <v-text-field v-model="customerForm.ten" placeholder="Tên khách hàng" variant="outlined"
                                    density="compact" hide-details autocomplete="off"
                                    class="dim-input-field flex-grow-1" @input="onCustomerInput"
                                    @focus="showCustomerSuggestions = true" />
                                <div class="position-relative" style="width: 170px; flex: none;">
                                    <v-text-field v-model="customerForm.sdt" placeholder="Số điện thoại"
                                        variant="outlined" density="compact" hide-details autocomplete="off"
                                        class="dim-input-field" @input="onCustomerInput"
                                        @focus="showCustomerSuggestions = true" />

                                    <!-- Autocomplete Suggestions Dropdown -->
                                    <v-menu v-model="showCustomerSuggestions" activator="parent" location="bottom start"
                                        max-height="200" :close-on-content-click="false">
                                        <v-list v-if="customerResults.length > 0" class="pa-1 d-flex flex-column gap-1">
                                            <div v-for="c in customerResults" :key="c.id"
                                                @click="onSelectSuggestedCustomer(c); showCustomerSuggestions = false;"
                                                class="suggestion-item d-flex justify-end align-center px-3 py-1 cursor-pointer transition-all"
                                                style="font-size: 13px; font-weight: 500;">
                                                <span class="text-slate-800 font-mono">{{ c.sdt }}</span>
                                            </div>
                                        </v-list>
                                    </v-menu>
                                </div>
                            </div>

                            <div class="d-flex gap-1">
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
                    <v-card v-if="isGiaoHang" class="pos-card pa-4 rounded-lg border">
                        <div class="font-weight-bold text-slate-800 mb-3" style="font-size: 14px !important">Thông tin
                            nhận hàng</div>
                        <div class="d-flex flex-column gap-1">
                            <div class="d-flex gap-1 mb-1">
                                <v-text-field v-model="recipientName" placeholder="Tên người nhận" variant="outlined"
                                    density="compact" hide-details autocomplete="off"
                                    class="dim-input-field flex-grow-1" />
                                <div style="width: 170px; flex: none;">
                                    <v-text-field v-model="recipientPhone" placeholder="Số điện thoại"
                                        variant="outlined" density="compact" hide-details autocomplete="off"
                                        class="dim-input-field" />
                                </div>
                            </div>

                            <v-text-field v-model="recipientAddressDetail" placeholder="Địa chỉ chi tiết"
                                variant="outlined" density="compact" hide-details autocomplete="off"
                                class="dim-input-field w-100 mb-1" />

                            <div class="d-flex gap-1">
                                <v-autocomplete v-model="recipientProvince" :items="provincesShip" item-title="name"
                                    item-value="code" placeholder="Tỉnh/Thành phố" density="compact" variant="outlined"
                                    hide-details clearable auto-select-first menu-icon="mdi-chevron-down"
                                    no-data-text="Không tìm thấy tỉnh/thành phố"
                                    class="dim-select-field flex-grow-1" style="width: 33.33%;" />
                                <v-autocomplete v-model="recipientDistrict" :items="districtsShip" item-title="name"
                                    item-value="code" placeholder="Quận/Huyện" density="compact" variant="outlined"
                                    hide-details clearable auto-select-first menu-icon="mdi-chevron-down"
                                    no-data-text="Không tìm thấy quận/huyện" :disabled="!recipientProvince"
                                    class="dim-select-field flex-grow-1" style="width: 33.33%;" />
                                <v-autocomplete v-model="recipientWard" :items="wardsShip" item-title="name"
                                    item-value="code" placeholder="Phường/Xã" density="compact" variant="outlined"
                                    hide-details clearable auto-select-first menu-icon="mdi-chevron-down"
                                    no-data-text="Không tìm thấy phường/xã" :disabled="!recipientDistrict"
                                    class="dim-select-field flex-grow-1" style="width: 33.33%;" />
                            </div>
                        </div>
                    </v-card>

                    <!-- Checkout / Print Action Buttons at Bottom Right -->
                    <div class="d-flex gap-3 mt-2 w-100">
                        <v-btn color="#88c057" height="60"
                            class="font-weight-bold rounded-lg shadow-md text-white px-4 flex-grow-1"
                            style="font-size: 17px !important;" :disabled="!selectedOrder?.listsHoaDonChiTiet?.length"
                            @click="onPrintInvoice" elevation="0">
                            <v-icon class="mr-1">mdi-printer</v-icon>
                            IN HÓA ĐƠN
                        </v-btn>

                        <v-btn color="#4285F4" height="60"
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

        <!-- Product QR / Barcode Scanner Dialog -->
        <v-dialog v-model="showScanner" max-width="520" @update:model-value="val => { if (!val) stopScanner(); }">
            <v-card class="rounded-xl overflow-hidden">
                <v-card-title class="d-flex align-center justify-space-between bg-slate-50 border-b px-5 py-4">
                    <div class="d-flex align-center font-weight-bold text-slate-800">
                        <v-icon size="22" class="mr-2" color="primary">mdi-barcode-scan</v-icon>
                        Quét mã sản phẩm
                    </div>
                    <v-btn icon variant="text" size="small" @click="stopScanner">
                        <v-icon>mdi-close</v-icon>
                    </v-btn>
                </v-card-title>
                <v-card-text class="pa-5">
                    <div :id="SCANNER_ELEMENT_ID" class="scanner-reader"></div>
                    <div class="text-caption text-grey-darken-1 text-center mt-3">
                        Đưa mã QR hoặc Barcode của biến thể sản phẩm vào khung hình.
                    </div>
                </v-card-text>
            </v-card>
        </v-dialog>

        <!-- VNPay QR Dialog -->
        <v-dialog v-model="vnpayDialog.show" max-width="450"
            @update:model-value="onVnPayDialogVisibilityChange">
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
            </v-card>
        </v-dialog>

        <!-- Variant Selection Modal (Shopee style) -->
                    <v-dialog v-model="variantModal.show" max-width="500">
                        <v-card class="rounded-xl overflow-hidden shadow-lg">
                            <!-- Product Summary Header -->
                            <v-card-title
                                class="pa-5 bg-white border-b position-sticky top-0 z-10 d-flex align-start justify-space-between">
                                <div class="d-flex align-start gap-4">
                                    <div class="variant-modal-image-wrap">
                                        <v-avatar rounded="lg" size="80" class="border bg-grey-lighten-4 flex-shrink-0">
                                            <v-img :src="modalDisplayVariant?.hinhAnh || variantModal.product?.hinhAnh"
                                                cover />
                                        </v-avatar>
                                        <div v-if="modalDisplayDiscountPercent > 0" class="variant-modal-discount-badge">
                                            -{{ modalDisplayDiscountPercent }}%
                                        </div>
                                    </div>
                                    <div class="pt-1">
                                        <h3 class="text-subtitle-1 font-weight-bold text-slate-800 mb-1"
                                            style="line-height: 1.3; font-size: 15px !important;">{{
                                            variantModal.product?.tenSanPham }}</h3>
                                        <div class="text-primary font-weight-bold mb-1" style="font-size: 18px;">
                                            {{ modalDisplayVariant ? formatCurrency(modalDisplayVariant.giaBan) :
                                                formatCurrency(variantModal.product?.variants[0]?.giaBan || 0) }}
                                        </div>
                                        <div v-if="modalDisplayVariant?.giaGoc && Number(modalDisplayVariant.giaGoc) > Number(modalDisplayVariant.giaBan)"
                                            class="text-caption text-slate-400 text-decoration-line-through mb-1">
                                            {{ formatCurrency(modalDisplayVariant.giaGoc) }}
                                        </div>
                                        <div class="text-body-2 text-slate-500">Kho: {{currentSelectedVariant ?
                                            currentSelectedVariant.soLuongTon :
                                            variantModal.product?.variants.reduce((sum, v) => sum + (v.soLuongTon||0), 0) }}</div>
                                    </div>
                                </div>
                                <v-btn icon="mdi-close" variant="text" size="small" color="slate-400"
                                    @click="closeVariantModal" class="ml-2" />
                            </v-card-title>

                            <v-card-text class="pa-5 bg-white" style="max-height: 60vh; overflow-y: auto;">
                                <!-- Colors -->
                                <div class="mb-6">
                                    <div class="text-subtitle-2 font-weight-bold text-slate-800 mb-3">Màu sắc</div>
                                    <div class="d-flex flex-wrap gap-2">
                                        <div v-for="color in uniqueColors" :key="color.name"
                                            class="cursor-pointer border rounded-lg px-3 py-1.5 d-flex align-center gap-2 transition-all"
                                            :class="variantModal.selectedColor === color.name ? 'border-primary bg-primary-lighten-5 text-primary font-weight-bold' : 'border-slate-300 text-slate-700 hover-bg-slate-50'"
                                            @click="selectColor(color.name)">
                                            <v-avatar size="24" rounded="sm" v-if="color.image"><v-img
                                                    :src="color.image" cover /></v-avatar>
                                            <span style="font-size: 14px;">{{ color.name }}</span>
                                        </div>
                                    </div>
                                </div>

                                <!-- Sizes -->
                                <div class="mb-6" v-if="variantModal.selectedColor">
                                    <div
                                        class="text-subtitle-2 font-weight-bold text-slate-800 mb-3 d-flex justify-space-between align-center">
                                        <span>Kích cỡ</span>
                                        <span
                                            class="text-caption text-primary font-weight-medium cursor-pointer"><v-icon
                                                size="14" left>mdi-ruler</v-icon> Hướng dẫn chọn size</span>
                                    </div>
                                    <div class="d-flex flex-wrap gap-2">
                                        <div v-for="size in availableSizes" :key="size.name"
                                            class="cursor-pointer border rounded-lg px-5 py-2 transition-all text-center"
                                            style="min-width: 60px;" :class="[
                                                size.soLuongTon === 0 ? 'bg-slate-50 border-slate-200 text-slate-400 opacity-60' :
                                                    variantModal.selectedSize === size.name ? 'border-primary bg-primary text-white font-weight-bold shadow-sm' : 'border-slate-300 text-slate-700 hover-bg-slate-50'
                                            ]" @click="selectSize(size.name, size.soLuongTon)">
                                            <div style="font-size: 14px;">{{ size.name }}</div>
                                        </div>
                                    </div>
                                </div>

                                <!-- Quantity -->
                                <div class="d-flex align-center justify-space-between mt-6 pt-5 border-t"
                                    v-if="variantModal.selectedSize">
                                    <div class="text-subtitle-2 font-weight-bold text-slate-800">Số lượng</div>
                                    <div class="d-flex align-center border border-slate-300 rounded-lg overflow-hidden"
                                        style="height: 36px; width: 120px;">
                                        <v-btn icon="mdi-minus" size="small" variant="text" color="slate-600"
                                            class="rounded-0 h-100 flex-shrink-0" style="width: 36px; min-width: 36px;"
                                            :disabled="variantModal.quantity <= 1"
                                            @click="variantModal.quantity--"></v-btn>
                                        <input type="number" v-model.number="variantModal.quantity"
                                            @blur="validateQuantityInput"
                                            class="text-center font-weight-bold outline-none flex-grow-1 bg-white h-100 hide-arrows"
                                            style="width: 100%; font-size: 15px;" />
                                        <v-btn icon="mdi-plus" size="small" variant="text" color="slate-600"
                                            class="rounded-0 h-100 flex-shrink-0" style="width: 36px; min-width: 36px;"
                                            :disabled="variantModal.quantity >= (currentSelectedVariant?.soLuongTon || 1)"
                                            @click="variantModal.quantity++"></v-btn>
                                    </div>
                                </div>
                            </v-card-text>

                            <v-card-actions class="pa-4 bg-white border-t">
                                <v-btn color="primary" variant="flat" block size="x-large"
                                    class="rounded-xl text-none font-weight-bold shadow-sm" style="font-size: 16px;"
                                    :disabled="!currentSelectedVariant" :loading="isAddingProduct"
                                    @click="confirmAddVariants">
                                    <v-icon left class="mr-2">mdi-cart-plus</v-icon>
                                    Thêm vào giỏ hàng
                                </v-btn>
                            </v-card-actions>
                        </v-card>
                    </v-dialog>

        <!-- Confirm Dialog -->
        <AdminConfirm v-model:show="confirmDialog.show" v-bind="confirmDialog" @confirm="confirmDialog.action" />
    </v-container>
</template>
<style scoped>

            .hide-arrows::-webkit-outer-spin-button,
            .hide-arrows::-webkit-inner-spin-button {
                -webkit-appearance: none;
                margin: 0;
            }

            .hide-arrows {
                -moz-appearance: textfield;
            }

            .pos-wrapper {
                background: #eef2f6;
                min-height: calc(100vh - 64px);
                overflow-y: auto;
            }

            .pos-wrapper :deep(.v-btn) {
                text-transform: none;
                letter-spacing: normal;
            }

            .pos-shell {
                display: flex;
                flex-direction: column;
                padding: 16px;
            }

            .pos-header-row {
                min-height: 48px;
            }

            .pos-card {
                background: #ffffff;
                border: 1px solid #dfe5ee !important;
                box-shadow: 0 4px 12px rgba(15, 23, 42, 0.03) !important;
                border-radius: 8px !important;
            }

            .compact-select :deep(.v-field__input) {
                padding-top: 2px !important;
                padding-bottom: 2px !important;
                min-height: 32px !important;
                font-size: 13px !important;
            }

            .compact-select :deep(.v-field) {
                border-radius: 8px !important;
                --v-input-control-height: 32px !important;
                font-size: 13px !important;
                background-color: #ffffff !important;
                border: 1px solid #cbd5e1 !important;
                transition: border-color 0.2s ease, box-shadow 0.2s ease;
            }

            .compact-select :deep(.v-field--focused) {
                border-color: #4285F4 !important;
                box-shadow: 0 0 0 2px rgba(66, 133, 244, 0.15) !important;
            }

            .compact-select :deep(.v-field__outline) {
                display: none !important;
            }

            .compact-btn {
                height: 32px !important;
                border-radius: 8px !important;
                font-size: 13px !important;
                border: 1px solid #dfe5ee !important;
                background: #ffffff !important;
                color: #334155 !important;
            }

            .custom-customer-btn {
                border-color: #dfe5ee !important;
                background-color: #ffffff !important;
            }

            .product-mode-toggle {
                height: 32px !important;
            }

            .product-mode-toggle .v-btn {
                height: 30px !important;
                font-size: 12px !important;
                text-transform: none !important;
                border-radius: 6px !important;
            }

            .search-input :deep(.v-field) {
                border-radius: 8px !important;
                font-size: 13px !important;
                border: 1px solid #cbd5e1 !important;
                background-color: #ffffff !important;
                transition: border-color 0.2s ease, box-shadow 0.2s ease;
            }

            .search-input :deep(.v-field--focused) {
                border-color: #4285F4 !important;
                box-shadow: 0 0 0 2px rgba(66, 133, 244, 0.15) !important;
            }

            .search-input :deep(.v-field__outline) {
                display: none !important;
            }

            .scanner-btn {
                border-radius: 8px !important;
                height: 40px !important;
            }

            .scanner-reader {
                width: 100%;
                min-height: 320px;
                overflow: hidden;
                border: 1px solid #dbe3ef;
                border-radius: 12px;
                background: #ffffff;
            }

            .scanner-reader :deep(video) {
                border-radius: 10px;
            }

            .hover-autocomplete-item {
                border-bottom: 1px solid #e2e8f0 !important;
                transition: background-color 0.2s ease;
            }

            .hover-autocomplete-item:hover {
                background-color: #f8fafc !important;
            }

            .hover-autocomplete-item:last-child {
                border-bottom: none !important;
            }

            .cart-container-box {
                background-color: #ffffff;
                min-height: 200px;
                border: 1px solid #cbd5e1 !important;
                border-radius: 8px !important;
            }

            .text-right-input :deep(input) {
                text-align: right !important;
            }

            .text-right-input :deep(.v-field) {
                border-radius: 8px !important;
                font-size: 13px !important;
                border: 1px solid #cbd5e1 !important;
                background-color: #ffffff !important;
                transition: border-color 0.2s ease, box-shadow 0.2s ease;
            }

            .text-right-input :deep(.v-field--focused) {
                border-color: #4285F4 !important;
                box-shadow: 0 0 0 2px rgba(66, 133, 244, 0.15) !important;
            }

            .text-right-input :deep(.v-field__outline) {
                display: none !important;
            }

            .custom-value-input :deep(.v-field) {
                background-color: #ffffff !important;
                border: 1px solid #cbd5e1 !important;
                box-shadow: none !important;
                border-radius: 8px !important;
                transition: border-color 0.2s ease, box-shadow 0.2s ease;
            }

            .custom-value-input :deep(.v-field--focused) {
                border-color: #4285F4 !important;
                box-shadow: 0 0 0 2px rgba(66, 133, 244, 0.15) !important;
            }

            .custom-value-input :deep(.v-field__outline) {
                display: none !important;
            }

            .custom-value-input :deep(input) {
                color: #334155 !important;
                font-weight: 400 !important;
                padding-right: 8px !important;
            }

            .custom-value-input :deep(.v-text-field__suffix) {
                font-size: 13px !important;
                color: #475569 !important;
                font-weight: 400 !important;
                opacity: 1 !important;
            }

            .pos-card :deep(.v-checkbox .v-label) {
                font-size: 13px !important;
                font-weight: 400 !important;
            }

            .bg-emerald-50 {
                background-color: #ecfdf5 !important;
                border: 1px solid #a7f3d0 !important;
            }

            .text-emerald-800 {
                color: #065f46 !important;
            }

            .text-emerald-600 {
                color: #059669 !important;
            }

            .border-emerald {
                border: 1px solid #a7f3d0 !important;
            }

            .bg-red-50 {
                background-color: #fef2f2 !important;
                border: 1px solid #fecaca !important;
            }

            .text-red-800 {
                color: #991b1b !important;
            }

            .border-red {
                border: 1px solid #fecaca !important;
            }

            .bg-blue-50 {
                background-color: #eff6ff !important;
                border: 1px solid #bfdbfe !important;
            }

            .text-blue-800 {
                color: #1e40af !important;
            }

            .border-blue {
                border: 1px solid #bfdbfe !important;
            }

            .payment-method-toggle {
                height: 36px !important;
            }

            .payment-method-toggle .v-btn {
                height: 34px !important;
                font-size: 12px !important;
                text-transform: none !important;
            }

            .note-type-toggle {
                height: 32px !important;
            }

            .note-type-toggle .v-btn {
                height: 30px !important;
                font-size: 12px !important;
                text-transform: none !important;
            }

            .note-textarea :deep(.v-field) {
                border-radius: 8px !important;
                border: 1px solid #cbd5e1 !important;
                background-color: #ffffff !important;
                transition: border-color 0.2s ease, box-shadow 0.2s ease;
            }

            .note-textarea :deep(.v-field--focused) {
                border-color: #4285F4 !important;
                box-shadow: 0 0 0 2px rgba(66, 133, 244, 0.15) !important;
            }

            .note-textarea :deep(.v-field__outline) {
                display: none !important;
            }

            .btn-checkout {
                text-transform: none !important;
                letter-spacing: 0 !important;
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

            .gap-2 {
                gap: 6px;
            }

            .gap-3 {
                gap: 8px;
            }

            .gap-4 {
                gap: 8px;
            }

            .pointer {
                cursor: pointer;
            }

            .bg-slate-50 {
                background-color: #f8fafc !important;
            }

            .product-dropdown-card {
                box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1) !important;
            }

            /* Right Column Styles */
            .compact-select-right :deep(.v-field__input) {
                padding-top: 2px !important;
                padding-bottom: 2px !important;
                min-height: 28px !important;
                font-size: 13px !important;
            }

            .compact-select-right :deep(.v-field) {
                border-radius: 8px !important;
                --v-input-control-height: 28px !important;
                font-size: 13px !important;
                background-color: #ffffff !important;
                border: 1px solid #cbd5e1 !important;
                transition: border-color 0.2s ease, box-shadow 0.2s ease;
            }

            .compact-select-right :deep(.v-field--focused) {
                border-color: #4285F4 !important;
                box-shadow: 0 0 0 2px rgba(66, 133, 244, 0.15) !important;
            }

            .compact-select-right :deep(.v-field__outline) {
                display: none !important;
            }

            .right-input-field :deep(.v-field__input) {
                padding-top: 2px !important;
                padding-bottom: 2px !important;
                min-height: 32px !important;
                font-size: 13px !important;
            }

            .right-input-field :deep(.v-field) {
                border-radius: 8px !important;
                --v-input-control-height: 32px !important;
                font-size: 13px !important;
                background-color: #ffffff !important;
                border: 1px solid #cbd5e1 !important;
                transition: border-color 0.2s ease, box-shadow 0.2s ease;
            }

            .right-input-field :deep(.v-field--focused) {
                border-color: #4285F4 !important;
                box-shadow: 0 0 0 2px rgba(66, 133, 244, 0.15) !important;
            }

            .right-input-field :deep(.v-field__outline) {
                display: none !important;
            }

            /* Base font-size rules for inputs, textareas, buttons, placeholders and body text */
            .pos-wrapper :deep(.v-field),
            .pos-wrapper :deep(.v-field__input),
            .pos-wrapper :deep(.v-btn),
            .pos-wrapper :deep(.text-body-2),
            .pos-wrapper :deep(input),
            .pos-wrapper :deep(textarea),
            .pos-wrapper :deep(input::placeholder),
            .pos-wrapper :deep(textarea::placeholder),
            .pos-wrapper :deep(.v-field__placeholder),
            .pos-wrapper :deep(.v-label),
            .pos-wrapper :deep(.v-list-item-title),
            .pos-wrapper :deep(.v-select__selection-text) {
                font-size: 13px !important;
            }

            /* Custom Payment Method Buttons */
            .cash-active-btn {
                background-color: #fdf2f8 !important;
                color: #db2777 !important;
                border-color: #fbcfe8 !important;
            }

            .vnpay-active-btn {
                background-color: #eff6ff !important;
                color: #1d4ed8 !important;
                border-color: #bfdbfe !important;
            }

            .payment-inactive-btn {
                background-color: #f8fafc !important;
                color: #64748b !important;
                border-color: #e2e8f0 !important;
            }

            /* Custom Note Tabs */
            .custom-note-tabs {
                background-color: #f1f5f9 !important;
            }

            .note-tab-active {
                background-color: #ffffff !important;
                color: #0c3866 !important;
                box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06) !important;
                border: 1px solid #e2e8f0 !important;
            }

            .note-tab-inactive {
                background-color: transparent !important;
                color: #64748b !important;
                border: 1px solid transparent !important;
            }

            /* Styled input fields for customer card */
            .dim-input-field :deep(.v-field) {
                background-color: #ffffff !important;
                border-radius: 8px !important;
                --v-input-control-height: 32px !important;
                font-size: 13px !important;
                border: 1px solid #cbd5e1 !important;
                transition: border-color 0.2s ease, box-shadow 0.2s ease;
            }

            .dim-input-field :deep(.v-field--focused) {
                border-color: #4285F4 !important;
                box-shadow: 0 0 0 2px rgba(66, 133, 244, 0.15) !important;
            }

            .dim-input-field :deep(.v-field__outline) {
                display: none !important;
            }

            .dim-input-field :deep(.v-field__input) {
                padding-top: 2px !important;
                padding-bottom: 2px !important;
                min-height: 32px !important;
                font-size: 13px !important;
            }

            .dim-select :deep(.v-field) {
                background-color: #ffffff !important;
                border-radius: 8px !important;
                --v-input-control-height: 28px !important;
                font-size: 13px !important;
                border: 1px solid #cbd5e1 !important;
                transition: border-color 0.2s ease, box-shadow 0.2s ease;
            }

            .dim-select :deep(.v-field--focused) {
                border-color: #4285F4 !important;
                box-shadow: 0 0 0 2px rgba(66, 133, 244, 0.15) !important;
            }

            .dim-select :deep(.v-field__outline) {
                display: none !important;
            }

            .dim-select :deep(.v-field__input) {
                padding-top: 2px !important;
                padding-bottom: 2px !important;
                min-height: 28px !important;
                font-size: 13px !important;
            }

            /* Suggestion Popover - Right Aligned under SĐT */
            .suggestion-popover {
                position: absolute;
                top: 100%;
                right: 0;
                left: auto;
                width: 170px;
                z-index: 9999;
                background-color: #ffffff;
                border: 1px solid #e2e8f0;
                box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.05), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
                border-radius: 20px !important;
                margin-top: 4px;
            }

            .suggestion-item {
                border-radius: 16px;
                height: 32px;
            }

            .suggestion-item:hover {
                background-color: #f1f5f9;
            }

            /* Styled select field for delivery card */
            .dim-select-field :deep(.v-field) {
                background-color: #ffffff !important;
                border-radius: 8px !important;
                --v-input-control-height: 32px !important;
                font-size: 13px !important;
                border: 1px solid #cbd5e1 !important;
                transition: border-color 0.2s ease, box-shadow 0.2s ease;
            }

            .dim-select-field :deep(.v-field--focused) {
                border-color: #4285F4 !important;
                box-shadow: 0 0 0 2px rgba(66, 133, 244, 0.15) !important;
            }

            .dim-select-field :deep(.v-field__outline) {
                display: none !important;
            }

            .dim-select-field :deep(.v-field__input) {
                padding-top: 2px !important;
                padding-bottom: 2px !important;
                min-height: 32px !important;
                font-size: 13px !important;
            }

            .date-input-field :deep(input[type="date"]) {
                position: relative;
                cursor: pointer;
            }

            .date-input-field :deep(input[type="date"]::-webkit-calendar-picker-indicator) {
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                width: 100%;
                height: 100%;
                opacity: 0;
                cursor: pointer;
            }

            .product-dropdown-card {
                width: 100% !important;
                min-width: 650px !important;
                left: 0;
            }

            .sku-badge {
                background-color: #ffe4e6 !important;
                color: #9f1239 !important;
                font-size: 11px !important;
                font-weight: 500;
                padding: 1px 6px;
                border-radius: 4px;
                display: inline-flex;
                align-items: center;
            }

            .sp-badge {
                background-color: #e0f2fe !important;
                color: #0369a1 !important;
                font-size: 11px !important;
                font-weight: 500;
                padding: 1px 6px;
                border-radius: 4px;
                display: inline-flex;
                align-items: center;
            }

            .size-badge {
                border: 1px solid #cbd5e1 !important;
                color: #64748b !important;
                font-size: 11px !important;
                font-weight: 500;
                padding: 0px 6px;
                border-radius: 4px;
                display: inline-flex;
                align-items: center;
            }

            .bullet-details {
                font-size: 11.5px !important;
                color: #64748b;
                line-height: 1.4;
            }

            .bullet-item {
                margin-bottom: 1px;
            }

            .price-text {
                font-size: 14px !important;
                font-weight: 500;
                color: #88c057 !important;
            }

            .speed-text {
                font-size: 11px !important;
                color: #64748b;
            }

            .variant-modal-image-wrap {
                position: relative;
                flex-shrink: 0;
                width: 80px;
                height: 80px;
            }

            .variant-modal-discount-badge {
                position: absolute;
                top: -6px;
                right: -8px;
                min-width: 38px;
                height: 22px;
                padding: 0 7px;
                border-radius: 999px;
                background: #ef4444;
                color: #ffffff;
                font-size: 11px;
                font-weight: 800;
                line-height: 22px;
                text-align: center;
                box-shadow: 0 6px 14px rgba(239, 68, 68, 0.28);
                border: 2px solid #ffffff;
                z-index: 2;
            }


        </style>
