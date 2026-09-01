<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { dichVuDotGiamGia } from '@/services/admin/dichVuDotGiamGia';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { dichVuBienThe } from '@/services/product/dichVuBienThe';
import { generateRandomCode } from '@/utils/codeGenerator';
import { dichVuThuongHieu, dichVuMauSac, dichVuKichThuoc, dichVuChatLieu } from '@/services/product/dichVuThuocTinh';
import { useNotifications } from '@/services/notificationService';
import { formatCurrency } from '@/utils/formatters';
import {
    AdminConfirm,
    AdminBreadcrumbs,
    AdminFilter,
    AdminPagination,
    TableEmptyState,
    FormattedPercentField,
    FormattedNumberField
} from '@/components/common';
import { useUIStore } from '@/stores/ui';
import { getColorHexByName } from '@/utils/colorDictionary';
import { CalendarIcon, GiftIcon, InfoCircleIcon, TagIcon, BoxIcon, SearchIcon, TrashIcon } from 'vue-tabler-icons';
import { PATH } from '@/router/routePaths';
import { getNameRules, lengthBetween3And255, noSpecialChar } from '@/utils/validators';
import { SYSTEM_STATUS } from '@/constants/statusConstants';
import { MESSAGES } from '@/constants/messages';
import SafeProductImage from '@/views/modules/san-pham/components/SafeProductImage.vue';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();
const uiStore = useUIStore();

const toLocalDatetimeString = (timestamp) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    const pad = (num) => String(num).padStart(2, '0');
    const year = date.getFullYear();
    const month = pad(date.getMonth() + 1);
    const day = pad(date.getDate());
    const hours = pad(date.getHours());
    const minutes = pad(date.getMinutes());
    return `${year}-${month}-${day}T${hours}:${minutes}`;
};

const saving = ref(false);

// Attribute Options
const brands = ref([]);
const colors = ref([]);
const sizes = ref([]);
const materials = ref([]);

// --- PHẦN 1: QUẢN LÝ DANH SÁCH SẢN PHẨM Ở BẢNG CHỌN (LAZY LOAD / ON-DEMAND) ---
const selectionLoading = ref(false);
const productsList = ref([]); // Danh sách sản phẩm trên trang hiện tại
const selectionPage = ref(1);
const selectionPageSize = ref(10);
const selectionTotalElements = ref(0);
const selectionTotalPages = ref(1);
const searchQuery = ref('');

// Cache biến thể theo từng sản phẩm: productId -> Array<Variant>
const variantsCache = ref(new Map());
// Trạng thái đang tải biến thể cho từng sản phẩm: { [productId]: boolean }
const loadingProductVariants = ref({});
const expandedProductIds = ref([]);

// Map các biến thể thực sự đã được chọn: variantId -> variantObject
const selectedVariantsMap = ref(new Map());
const selectedVariantsIds = computed(() => Array.from(selectedVariantsMap.value.keys()));

// Hàm lấy mã màu Hex chuẩn từ dữ liệu hoặc thư viện màu sắc
const getVariantColorHex = (variant) => {
    if (!variant) return '#cbd5e1';
    if (variant.maMauHex && /^#[0-9A-Fa-f]{3,8}$/.test(variant.maMauHex)) {
        return variant.maMauHex;
    }
    const colorName = variant.color || variant.tenMauSac || (variant.mauSac ? variant.mauSac.ten : '');
    return getColorHexByName(colorName) || '#cbd5e1';
};

// Hàm chuẩn hóa dữ liệu biến thể
const normalizeVariant = (v, fallbackProduct = {}) => {
    const maSp = fallbackProduct.maSanPham || fallbackProduct.ma || v.maSanPham || v.sanPhamMa || v.maSp || '';
    const tenSp = fallbackProduct.tenSanPham || fallbackProduct.ten || v.tenSanPham || v.tenSanPhamDayDu || v.ten || '';
    const variantMa = v.maChiTietSanPham || v.ma || (maSp ? `${maSp}-${v.id?.substring(0, 4)}` : '');
    const color = v.tenMauSac || v.color || (v.mauSac ? v.mauSac.ten : '') || '--';
    const kichCo = v.tenKichThuoc || v.kichCo || (v.kichThuoc ? v.kichThuoc.ten : '') || '--';
    const thuongHieu = v.tenThuongHieu || v.thuongHieu || fallbackProduct.tenThuongHieu || fallbackProduct.thuongHieu || '--';
    const chatLieu = v.tenChatLieu || v.chatLieu || fallbackProduct.tenChatLieu || fallbackProduct.chatLieu || '--';
    const loaiSan = v.loaiSan || fallbackProduct.loaiSan || '--';
    const giaGoc = v.giaGoc !== null && v.giaGoc !== undefined ? Number(v.giaGoc) : (v.giaBan !== null && v.giaBan !== undefined ? Number(v.giaBan) : 0);
    const anhMauc =
        v.hinhAnh ||
        v.urlAnh ||
        v.anhMauc ||
        (v.images?.length > 0 ? v.images[0].duongDanAnh : null) ||
        fallbackProduct.hinhAnh ||
        '';
    const maMauHex = v.maMauHex || v.maMau || (v.mauSac ? (v.mauSac.maMauHex || v.mauSac.maMau) : '') || getColorHexByName(color) || '#cbd5e1';

    return {
        ...v,
        id: v.id,
        idSanPham: v.idSanPham || fallbackProduct.id,
        ma: variantMa,
        maChiTietSanPham: variantMa,
        maSanPham: maSp,
        tenSanPham: tenSp,
        tenSanPhamDayDu: v.tenSanPhamDayDu || `${tenSp} [${color} - ${kichCo}]`,
        color,
        maMauHex,
        kichCo,
        thuongHieu,
        chatLieu,
        loaiSan,
        giaGoc,
        anhMauc
    };
};

// Tải danh sách sản phẩm theo trang (nhẹ, nhanh, phân trang server-side)
const loadProductsToSelect = async (page = 1) => {
    selectionLoading.value = true;
    try {
        selectionPage.value = page;
        const res = await dichVuSanPham.layDanhSachSanPham({
            page: Math.max(0, page - 1),
            size: selectionPageSize.value,
            keyword: searchQuery.value?.trim() || undefined,
            trangThai: SYSTEM_STATUS.ACTIVE
        });

        const items = res?.content || res?.data || (Array.isArray(res) ? res : []);
        selectionTotalElements.value = res?.totalElements ?? items.length;
        selectionTotalPages.value = res?.totalPages ?? 1;

        productsList.value = items.map((p) => {
            const cached = variantsCache.value.get(p.id);
            return {
                id: p.id,
                ma: p.maSanPham || p.ma,
                ten: p.tenSanPham || p.ten,
                hinhAnh: p.hinhAnh,
                thuongHieu: p.tenThuongHieu || p.thuongHieu,
                chatLieu: p.tenChatLieu || p.chatLieu,
                variants: cached || []
            };
        });
        // Biến thể chỉ tải khi user expand sản phẩm (on-demand) để tránh N+1 API calls
    } catch (e) {
        console.error('Lỗi khi tải danh sách sản phẩm:', e);
        productsList.value = [];
    } finally {
        selectionLoading.value = false;
    }
};

// Tải biến thể theo yêu cầu (Lazy Load / On-Demand) cho 1 sản phẩm cụ thể
const loadProductVariants = async (product) => {
    if (variantsCache.value.has(product.id)) {
        return variantsCache.value.get(product.id);
    }
    loadingProductVariants.value[product.id] = true;
    try {
        const rawVariants = await dichVuBienThe.layBienTheTheoSanPham(product.id);
        const normalized = (rawVariants || []).map((v) => normalizeVariant(v, product));
        variantsCache.value.set(product.id, normalized);
        product.variants = normalized;
        return normalized;
    } catch (e) {
        console.error(`Lỗi khi tải biến thể sản phẩm ${product.id}:`, e);
        variantsCache.value.set(product.id, []);
        product.variants = [];
        return [];
    } finally {
        loadingProductVariants.value[product.id] = false;
    }
};

// Đóng / mở danh sách biến thể của sản phẩm
const toggleExpand = async (productId) => {
    const index = expandedProductIds.value.indexOf(productId);
    if (index > -1) {
        expandedProductIds.value.splice(index, 1);
    } else {
        expandedProductIds.value.push(productId);
        const product = productsList.value.find((p) => p.id === productId);
        if (product && !variantsCache.value.has(productId)) {
            await loadProductVariants(product);
        }
    }
};

// Kiểm tra trạng thái chọn của sản phẩm
const isProductSelected = (product) => {
    const variants = variantsCache.value.get(product.id) || product.variants;
    if (!variants || variants.length === 0) return false;
    return variants.every((v) => selectedVariantsMap.value.has(v.id));
};

const isProductIndeterminate = (product) => {
    const variants = variantsCache.value.get(product.id) || product.variants;
    if (!variants || variants.length === 0) {
        return Array.from(selectedVariantsMap.value.values()).some(
            (v) => (v.idSanPham || v.sanPhamId) === product.id
        );
    }
    const selectedCount = variants.filter((v) => selectedVariantsMap.value.has(v.id)).length;
    return selectedCount > 0 && selectedCount < variants.length;
};

// Chọn / Bỏ chọn toàn bộ biến thể của 1 sản phẩm
const toggleProductSelection = async (product) => {
    let variants = variantsCache.value.get(product.id);
    if (!variants) {
        variants = await loadProductVariants(product);
    }
    if (!variants || variants.length === 0) return;

    const allSelected = variants.every((v) => selectedVariantsMap.value.has(v.id));
    const newMap = new Map(selectedVariantsMap.value);

    if (allSelected) {
        variants.forEach((v) => newMap.delete(v.id));
    } else {
        variants.forEach((v) => newMap.set(v.id, v));
    }
    selectedVariantsMap.value = newMap;
};

// Chọn / Bỏ chọn 1 biến thể lẻ
const toggleVariantSelection = (variant) => {
    const newMap = new Map(selectedVariantsMap.value);
    if (newMap.has(variant.id)) {
        newMap.delete(variant.id);
    } else {
        newMap.set(variant.id, normalizeVariant(variant));
    }
    selectedVariantsMap.value = newMap;
};

const isVariantSelected = (id) => {
    return selectedVariantsMap.value.has(id);
};

// Checkbox chọn tất cả trên trang hiện tại
const isAllCurrentPageSelected = computed(() => {
    if (productsList.value.length === 0) return false;
    return productsList.value.every((p) => isProductSelected(p));
});

const toggleAllProductsSelection = () => {
    const allSelected = isAllCurrentPageSelected.value;
    const newMap = new Map(selectedVariantsMap.value);

    // 1 phát ăn ngay: Chọn/Bỏ chọn ngay lập tức toàn bộ các sản phẩm và biến thể đã nạp trong cache
    for (const product of productsList.value) {
        const variants = variantsCache.value.get(product.id) || product.variants || [];
        if (variants && variants.length > 0) {
            if (allSelected) {
                variants.forEach((v) => newMap.delete(v.id));
            } else {
                variants.forEach((v) => newMap.set(v.id, v));
            }
        }
    }
    selectedVariantsMap.value = newMap;

    // Tải ngầm các biến thể chưa có trong cache mà không làm đơ giao diện hay hiện thanh load
    const unloadedProducts = productsList.value.filter((p) => !variantsCache.value.has(p.id));
    if (!allSelected && unloadedProducts.length > 0) {
        Promise.all(
            unloadedProducts.map(async (p) => {
                const variants = await loadProductVariants(p);
                p.variants = variants;
                if (variants && variants.length > 0) {
                    variants.forEach((v) => selectedVariantsMap.value.set(v.id, v));
                }
            })
        ).then(() => {
            selectedVariantsMap.value = new Map(selectedVariantsMap.value);
        });
    }
};

// Tìm kiếm sản phẩm
let searchTimer = null;
watch(searchQuery, () => {
    if (searchTimer) clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
        loadProductsToSelect(1);
    }, 300);
});

const handleRefreshSearch = () => {
    if (searchTimer) clearTimeout(searchTimer);
    searchQuery.value = '';
    loadProductsToSelect(1);
};

const updateSelectionPage = (page) => {
    loadProductsToSelect(page);
};

const updateSelectionPageSize = (size) => {
    selectionPageSize.value = size;
    loadProductsToSelect(1);
};

const getExpandIcon = (id) => {
    return expandedProductIds.value.includes(id) ? 'mdi-minus' : 'mdi-plus';
};

// --- PHẦN 2: QUẢN LÝ BẢNG CHI TIẾT SẢN PHẨM ĐÃ CHỌN (BẢNG DƯỚI) ---
const dynamicMaxPrice = ref(6500000);

const detailFilters = ref({
    timKiem: '',
    thuongHieu: null,
    chatLieu: null,
    kichCo: null,
    mauSac: null,
    loaiSan: null,
    khoangGia: [0, dynamicMaxPrice.value]
});

const bottomPage = ref(1);
const bottomPageSize = ref(10);
const bottomTableSelection = ref([]); // Bulk action xóa ở bảng dưới

const resetDetailFilters = () => {
    detailFilters.value = {
        timKiem: '',
        thuongHieu: null,
        chatLieu: null,
        kichCo: null,
        mauSac: null,
        loaiSan: null,
        khoangGia: [0, dynamicMaxPrice.value]
    };
};

const handleRefreshDetailSearch = () => {
    resetDetailFilters();
};

const bottomTableVariants = computed(() => {
    return Array.from(selectedVariantsMap.value.values());
});

const filteredSelectedDetails = computed(() => {
    let result = bottomTableVariants.value;
    const filters = detailFilters.value;

    if (filters.timKiem) {
        const query = filters.timKiem.toLowerCase().trim();
        result = result.filter(
            (p) =>
                (p.tenSanPham && p.tenSanPham.toLowerCase().includes(query)) ||
                (p.tenSanPhamDayDu && p.tenSanPhamDayDu.toLowerCase().includes(query)) ||
                (p.maSanPham && p.maSanPham.toLowerCase().includes(query)) ||
                (p.ma && p.ma.toLowerCase().includes(query)) ||
                (p.maChiTietSanPham && p.maChiTietSanPham.toLowerCase().includes(query)) ||
                (p.ten && p.ten.toLowerCase().includes(query))
        );
    }
    if (filters.thuongHieu) result = result.filter((p) => p.thuongHieu === filters.thuongHieu);
    if (filters.chatLieu) result = result.filter((p) => p.chatLieu === filters.chatLieu);
    if (filters.kichCo)
        result = result.filter((p) =>
            String(p.kichCo || '')
                .toLowerCase()
                .includes(String(filters.kichCo).toLowerCase())
        );
    if (filters.mauSac) result = result.filter((p) => p.color === filters.mauSac);
    if (filters.loaiSan) result = result.filter((p) => p.loaiSan === filters.loaiSan);

    const minPrice = filters.khoangGia[0] || 0;
    const maxPrice = filters.khoangGia[1] || dynamicMaxPrice.value;

    result = result.filter((p) => {
        const giaSauGiam = calculateDiscountedPrice(p.giaGoc || 0);
        return giaSauGiam >= minPrice && giaSauGiam <= maxPrice;
    });

    return result;
});

const paginatedSelectedDetails = computed(() => {
    const start = (bottomPage.value - 1) * bottomPageSize.value;
    const end = start + bottomPageSize.value;
    return filteredSelectedDetails.value.slice(start, end);
});

const totalBottomPages = computed(() => {
    return Math.ceil(filteredSelectedDetails.value.length / bottomPageSize.value) || 1;
});

// Reset bottom page khi bộ lọc thay đổi
watch(
    detailFilters,
    () => {
        bottomPage.value = 1;
    },
    { deep: true }
);

watch(
    selectedVariantsIds,
    () => {
        if (bottomPage.value > totalBottomPages.value) {
            bottomPage.value = Math.max(1, totalBottomPages.value);
        }
    },
    { deep: true }
);

const calculateDiscountedPrice = (originalPrice) => {
    return originalPrice * (1 - (form.value.soTienGiam || 0) / 100);
};

const toggleAllBottomSelection = () => {
    const allSelected = filteredSelectedDetails.value.length > 0 && filteredSelectedDetails.value.every((p) => isVariantSelected(p.id));
    const newMap = new Map(selectedVariantsMap.value);
    if (allSelected) {
        filteredSelectedDetails.value.forEach((v) => newMap.delete(v.id));
    } else {
        filteredSelectedDetails.value.forEach((v) => newMap.set(v.id, normalizeVariant(v)));
    }
    selectedVariantsMap.value = newMap;
};

const removeAllSelected = () => {
    selectedVariantsMap.value = new Map();
};

// --- PHẦN 3: FORM CONFIG VÀ LƯU DỮ LIỆU ---
const isEditMode = computed(() => !!route.params.id && !route.path.includes('/detail'));
const submitButtonText = computed(() => (isEditMode.value ? 'Cập nhật đợt giảm giá' : 'Thêm đợt giảm giá'));
const isDetailView = computed(() => route.path.includes('/detail'));

const tenDotGiamGiaRules = getNameRules('Tên đợt giảm giá');

const form = ref({
    ma: '',
    ten: '',
    moTa: '',
    loaiGiamGia: 'PHAN_TRAM',
    soTienGiam: 0,
    giamToiDa: null,
    mucUuTien: 0,
    ngayBatDau: '',
    ngayKetThuc: '',
    trangThai: SYSTEM_STATUS.ACTIVE
});

const init = async () => {
    uiStore.startProgress();
    try {
        // Chạy TẤT CẢ promise song song tối đa (không chờ tuần tự)
        const promises = [
            loadProductsToSelect(1),
            dichVuSanPham.layGiaLonNhat().catch(() => 6500000),
            dichVuThuongHieu.layThuongHieu({ trangThai: SYSTEM_STATUS.ACTIVE }).catch(() => []),
            dichVuMauSac.layMauSac({ trangThai: SYSTEM_STATUS.ACTIVE }).catch(() => []),
            dichVuKichThuoc.layKichThuoc({ trangThai: SYSTEM_STATUS.ACTIVE }).catch(() => []),
            dichVuChatLieu.layChatLieu({ trangThai: SYSTEM_STATUS.ACTIVE }).catch(() => [])
        ];

        // Nếu mode Edit/Detail: thêm 2 promise vào batch
        if (isEditMode.value || isDetailView.value) {
            promises.push(
                dichVuDotGiamGia.layChiTietDotGiamGia(route.params.id),
                dichVuDotGiamGia.layDanhSachBienTheApDung(route.params.id).catch(() => [])
            );
        } else {
            promises.push(
                generateRandomCode('DotGiamGia').catch(() => 'DGG' + Date.now().toString().slice(-6))
            );
        }

        const results = await Promise.all(promises);

        // Phân tích kết quả: [0]=productLoad(void), [1]=maxPrice, [2-5]=attributes
        const maxPrice = results[1];
        const brandData = results[2];
        const colorData = results[3];
        const sizeData = results[4];
        const materialData = results[5];

        if (maxPrice !== undefined && maxPrice !== null) {
            dynamicMaxPrice.value = Math.max(maxPrice, 50000);
            detailFilters.value.khoangGia = [0, dynamicMaxPrice.value];
        }

        brands.value = (brandData?.content || brandData || []).map((b) => b.ten);
        colors.value = (colorData?.content || colorData || []).map((c) => c.ten);
        sizes.value = (sizeData?.content || sizeData || []).map((s) => s.ten);
        materials.value = (materialData?.content || materialData || []).map((m) => m.ten);

        if (isEditMode.value || isDetailView.value) {
            const data = results[6];
            const applied = results[7];

            form.value = {
                ...data,
                giamToiDa: data?.giamToiDa !== undefined && data?.giamToiDa !== null ? data.giamToiDa : null,
                ngayBatDau: data.ngayBatDau ? toLocalDatetimeString(data.ngayBatDau) : '',
                ngayKetThuc: data.ngayKetThuc ? toLocalDatetimeString(data.ngayKetThuc) : ''
            };

            const newMap = new Map();
            (applied || []).forEach((v) => {
                const normalized = normalizeVariant(v);
                newMap.set(v.id, normalized);
            });
            selectedVariantsMap.value = newMap;
        } else {
            form.value.ma = results[6] || ('DGG' + Date.now().toString().slice(-6));
        }
    } catch (e) {
        console.error('Error during init:', e);
        addNotification({ title: 'Lỗi', subtitle: MESSAGES.ERROR.LOAD_DATA, color: 'error' });
    } finally {
        uiStore.stopProgress();
    }
};

const confirmDialog = ref({ show: false, title: '', message: '', color: 'primary', action: null, loading: false });

const handleSave = () => {
    const rawName = form.value.ten;
    if (!rawName || !String(rawName).trim()) {
        addNotification({ title: 'Lỗi', subtitle: 'Vui lòng nhập tên đợt giảm giá', color: 'error' });
        return;
    }
    const lenCheck = lengthBetween3And255(rawName);
    if (lenCheck !== true) {
        addNotification({ title: 'Lỗi', subtitle: `Tên đợt giảm giá: ${lenCheck}`, color: 'error' });
        return;
    }
    const specialCharCheck = noSpecialChar(rawName);
    if (specialCharCheck !== true) {
        addNotification({ title: 'Lỗi', subtitle: `Tên đợt giảm giá: ${specialCharCheck}`, color: 'error' });
        return;
    }

    const discountAmount = Number(form.value.soTienGiam);
    if (!discountAmount || discountAmount <= 0) {
        addNotification({ title: 'Lỗi', subtitle: 'Mức giảm giá phải lớn hơn 0', color: 'error' });
        return;
    }
    if (discountAmount > 100) {
        addNotification({ title: 'Lỗi', subtitle: 'Mức giảm giá không được vượt quá 100%', color: 'error' });
        return;
    }
    if (!Number.isInteger(discountAmount)) {
        addNotification({ title: 'Lỗi', subtitle: 'Mức giảm giá phải là số nguyên', color: 'error' });
        return;
    }

    if (!form.value.ngayBatDau) {
        addNotification({ title: 'Lỗi', subtitle: 'Vui lòng chọn ngày bắt đầu', color: 'error' });
        return;
    }
    if (!form.value.ngayKetThuc) {
        addNotification({ title: 'Lỗi', subtitle: 'Vui lòng chọn ngày kết thúc', color: 'error' });
        return;
    }

    const startDate = new Date(form.value.ngayBatDau).getTime();
    const endDate = new Date(form.value.ngayKetThuc).getTime();

    if (startDate >= endDate) {
        addNotification({ title: 'Lỗi', subtitle: 'Ngày kết thúc phải sau ngày bắt đầu', color: 'error' });
        return;
    }

    if (selectedVariantsIds.value.length === 0) {
        addNotification({ title: 'Lỗi', subtitle: 'Vui lòng chọn ít nhất 1 sản phẩm áp dụng', color: 'error' });
        return;
    }

    confirmDialog.value = {
        show: true,
        title: isEditMode.value ? 'Xác nhận cập nhật' : 'Xác nhận thêm mới',
        message: isEditMode.value
            ? `Bạn có chắc muốn cập nhật chiến dịch [${form.value.ten}]?`
            : `Bạn có chắc muốn tạo chiến dịch mới [${form.value.ten}]?`,
        color: 'primary',
        action: async () => {
            confirmDialog.value.loading = true;
            saving.value = true;
            try {
                const rawGiamToiDa = form.value.giamToiDa;
                let giamToiDaValue = null;
                if (rawGiamToiDa !== null && rawGiamToiDa !== undefined && rawGiamToiDa !== '') {
                    const parsed = Number(String(rawGiamToiDa).replace(/[^\d.]/g, ''));
                    if (!isNaN(parsed) && parsed > 0) {
                        giamToiDaValue = parsed;
                    }
                }

                if (!form.value.ma || !String(form.value.ma).trim()) {
                    try {
                        form.value.ma = await generateRandomCode('DotGiamGia');
                    } catch (codeErr) {
                        form.value.ma = 'DGG' + Date.now().toString().slice(-6);
                    }
                }

                const parseTimestamp = (val) => {
                    if (!val) return null;
                    if (val instanceof Date) return val.getTime();
                    if (typeof val === 'number') return val;
                    const d = new Date(val);
                    return isNaN(d.getTime()) ? null : d.getTime();
                };

                const payload = {
                    ma: String(form.value.ma).trim(),
                    ten: String(form.value.ten).trim(),
                    moTa: form.value.moTa ? String(form.value.moTa).trim() : '',
                    loaiGiamGia: form.value.loaiGiamGia || 'PHAN_TRAM',
                    soTienGiam: Number(form.value.soTienGiam),
                    dieuKienGiamGia: form.value.dieuKienGiamGia ? Number(form.value.dieuKienGiamGia) : 0,
                    giamToiDa: giamToiDaValue,
                    mucUuTien: form.value.mucUuTien !== undefined && form.value.mucUuTien !== null ? Number(form.value.mucUuTien) : 0,
                    ngayBatDau: parseTimestamp(form.value.ngayBatDau),
                    ngayKetThuc: parseTimestamp(form.value.ngayKetThuc),
                    trangThai: form.value.trangThai || 'DANG_HOAT_DONG',
                    isFlashSale: !!form.value.isFlashSale,
                    khungGio: form.value.khungGio || null,
                    listIdChiTietSanPham: selectedVariantsIds.value
                };

                if (isEditMode.value) {
                    await dichVuDotGiamGia.capNhatDotGiamGia(route.params.id, payload);
                    addNotification({ title: 'Thành công', subtitle: MESSAGES.SUCCESS.UPDATE, color: 'success' });
                } else {
                    await dichVuDotGiamGia.taoDotGiamGia(payload);
                    addNotification({ title: 'Thành công', subtitle: MESSAGES.SUCCESS.ADD, color: 'success' });
                }
                confirmDialog.value.show = false;
                router.push(PATH.DOT_GIAM_GIA);
            } catch (e) {
                const errMsg = e?.response?.data?.message || e?.userMessage || MESSAGES.ERROR.SAVE_DATA;
                addNotification({ title: 'Lỗi', subtitle: errMsg, color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
                saving.value = false;
            }
        }
    };
};

const goBack = () => {
    router.back();
};

onMounted(init);
</script>

<template>
    <v-container fluid class="pa-6 animate-fade-in overflow-y-auto" style="height: 100vh">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs
            :items="[
                { title: 'Quản lý đợt giảm giá', disabled: false, href: '#' },
                { title: 'Đợt giảm giá', disabled: false, to: PATH.DOT_GIAM_GIA },
                { title: isEditMode ? 'Cập nhật' : isDetailView ? 'Chi tiết' : 'Thêm mới', disabled: true }
            ]"
        />

        <!-- Action Header -->
        <div class="d-flex align-center justify-space-between mb-8 mt-4">
            <div class="d-flex align-center ga-4">
                <v-btn
                    icon
                    variant="flat"
                    color="white"
                    class="mr-3 border elevation-1 rounded-lg"
                    size="36"
                    style="height: 36px !important; width: 36px !important; min-height: 36px !important"
                    @click="goBack"
                >
                    <v-icon size="18" color="slate-700">mdi-arrow-left</v-icon>
                </v-btn>
            </div>
            <div class="d-flex ga-3">
                <v-btn
                    v-if="!isDetailView"
                    color="primary"
                    variant="flat"
                    class="text-none px-8 campaign-submit-btn elevation-4"
                    @click="handleSave"
                    :loading="saving"
                >
                    <v-icon size="18" class="mr-2">mdi-check-all</v-icon>
                    {{ submitButtonText }}
                </v-btn>
            </div>
        </div>


        <v-row class="campaign-equal-row pb-16" align="stretch">
            <v-col cols="12" md="5" class="d-flex">
                <v-card class="premium-card elevation-0 border border-slate-200 mb-6 campaign-side-card">
                    <v-card-text class="pa-8 d-flex flex-column h-100">
                        <div class="section-header d-flex align-center mb-4">
                            <div class="icon-blob bg-blue-lighten-5 mr-3">
                                <v-icon color="primary" size="18">mdi-ticket-percent</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Thông tin đợt giảm giá</span>
                        </div>

                        <div class="form-fields-container flex-grow-1 d-flex flex-column">
                            <div class="mb-3">
                                <div class="field-label">Mã đợt giảm giá</div>
                                <v-text-field
                                    v-model="form.ma"
                                    readonly
                                    placeholder="Mã tự sinh..."
                                    variant="outlined"
                                    density="compact"
                                    class="bg-slate-50 mono-font"
                                    hide-details
                                ></v-text-field>
                            </div>

                            <div class="mb-3">
                                <div class="field-label">Tên đợt giảm giá <span class="text-error">*</span></div>
                                <v-text-field
                                    v-model="form.ten"
                                    :readonly="isDetailView"
                                    :rules="tenDotGiamGiaRules"
                                    placeholder="Nhập tên đợt giảm giá"
                                    variant="outlined"
                                    density="compact"
                                    maxlength="255"
                                    hide-details
                                ></v-text-field>
                            </div>

                            <div class="mb-3">
                                <div class="field-label">Mức giảm giá (%) <span class="text-error">*</span></div>
                                <FormattedPercentField
                                    v-model="form.soTienGiam"
                                    :readonly="isDetailView"
                                    placeholder="0"
                                    variant="outlined"
                                    density="compact"
                                    hide-details
                                />
                            </div>

                            <div class="mb-3">
                                <div class="field-label">Giảm tối đa (đ)</div>
                                <FormattedNumberField
                                    v-model="form.giamToiDa"
                                    :readonly="isDetailView"
                                    placeholder="Nhập mức giảm tối đa (để trống nếu không giới hạn)"
                                    variant="outlined"
                                    density="compact"
                                    hide-details
                                />
                            </div>

                            <div class="mb-3">
                                <div class="field-label">Ngày bắt đầu <span class="text-error">*</span></div>
                                <AppDatePicker
                                    v-model="form.ngayBatDau"
                                    :disabled="isDetailView"
                                    disable-past
                                    enable-time-picker
                                    placeholder="Chọn ngày bắt đầu"
                                />
                            </div>

                            <div class="mb-3">
                                <div class="field-label">Ngày kết thúc <span class="text-error">*</span></div>
                                <AppDatePicker
                                    v-model="form.ngayKetThuc"
                                    :disabled="isDetailView"
                                    disable-past
                                    :min-date="form.ngayBatDau"
                                    enable-time-picker
                                    placeholder="Chọn ngày kết thúc"
                                />
                            </div>

                            <div class="mo-ta-field flex-grow-1 d-flex flex-column">
                                <div class="field-label">Mô tả</div>
                                <v-textarea
                                    v-model="form.moTa"
                                    :readonly="isDetailView"
                                    placeholder="Nhập mô tả đợt giảm giá (không bắt buộc)..."
                                    variant="outlined"
                                    density="compact"
                                    rows="3"
                                    maxlength="1000"
                                    hide-details
                                    class="mo-ta-textarea flex-grow-1"
                                ></v-textarea>
                            </div>
                        </div>
                    </v-card-text>
                </v-card>
            </v-col>

            <v-col cols="12" md="7" class="d-flex">
                <v-card class="premium-card elevation-0 border border-slate-200 mb-6 campaign-side-card">
                    <v-card-text class="pa-8 d-flex flex-column h-100">
                        <div class="section-header d-flex align-center mb-4">
                            <div class="icon-blob bg-amber-lighten-5 mr-3">
                                <v-icon color="amber-darken-3" size="18">mdi-package-variant</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Danh sách sản phẩm</span>
                        </div>

                        <div class="d-flex align-center ga-3 mb-3">
                            <span class="text-slate-800 text-no-wrap" style="font-size: 14px">Tìm kiếm sản phẩm</span>
                            <v-text-field
                                v-model="searchQuery"
                                prepend-inner-icon="mdi-magnify"
                                placeholder="Tìm kiếm theo mã sản phẩm, tên sản phẩm..."
                                variant="outlined"
                                density="compact"
                                maxlength="255"
                                hide-details
                                clearable
                                class="compact-input flex-grow-1"
                            ></v-text-field>
                            <v-btn variant="outlined" color="primary" class="reset-btn" @click="handleRefreshSearch">
                                <v-icon size="18">mdi-refresh</v-icon>
                                <v-tooltip activator="parent" location="top">Làm mới bộ lọc</v-tooltip>
                            </v-btn>
                        </div>

                        <div class="table-wrapper campaign-product-table border rounded-lg overflow-y-auto custom-scrollbar">
                            <div v-if="selectionLoading" class="d-flex flex-column align-center justify-center h-100 py-12">
                                <v-progress-circular indeterminate color="primary" size="36" width="3" />
                                <span class="text-caption text-slate-500 mt-2">Đang tải danh sách sản phẩm...</span>
                            </div>

                            <template v-else>
                                <table class="native-admin-table">
                                    <thead>
                                        <tr>
                                            <th class="header-cell text-center text-no-wrap" style="width: 40px"></th>
                                            <th class="header-cell text-center text-no-wrap" style="width: 60px">
                                                <div class="d-flex justify-center align-center">
                                                    <v-checkbox-btn
                                                        density="compact"
                                                        color="primary"
                                                        hide-details
                                                        :model-value="isAllCurrentPageSelected"
                                                        @change="toggleAllProductsSelection"
                                                    ></v-checkbox-btn>
                                                </div>
                                            </th>
                                            <th class="header-cell text-center text-no-wrap">Mã sản phẩm</th>
                                            <th class="header-cell text-center text-no-wrap">Tên sản phẩm</th>
                                        </tr>
                                    </thead>
                                    <tbody v-if="productsList.length > 0">
                                        <template v-for="item in productsList" :key="item.id">
                                            <tr class="data-row">
                                                <td class="data-cell text-center">
                                                    <div class="d-flex align-center justify-center" style="height: 32px">
                                                        <v-progress-circular
                                                            v-if="loadingProductVariants[item.id]"
                                                            indeterminate
                                                            size="18"
                                                            width="2"
                                                            color="primary"
                                                        />
                                                        <v-btn
                                                            v-else
                                                            icon
                                                            variant="text"
                                                            size="small"
                                                            density="compact"
                                                            @click="toggleExpand(item.id)"
                                                        >
                                                            <v-icon>{{ getExpandIcon(item.id) }}</v-icon>
                                                        </v-btn>
                                                    </div>
                                                </td>
                                                <td class="data-cell text-center">
                                                    <div class="d-flex align-center justify-center" style="height: 32px">
                                                        <v-checkbox-btn
                                                            :model-value="isProductSelected(item)"
                                                            :indeterminate="isProductIndeterminate(item)"
                                                            @update:model-value="toggleProductSelection(item)"
                                                            :readonly="isDetailView"
                                                            color="primary"
                                                            hide-details
                                                            density="compact"
                                                            class="d-inline-flex ma-0 pa-0"
                                                        ></v-checkbox-btn>
                                                    </div>
                                                </td>
                                                <td class="data-cell text-center text-primary font-weight-bold text-slate-600">
                                                    {{ item.ma }}
                                                </td>
                                                <td class="data-cell text-center font-weight-medium">
                                                    {{ item.ten }}
                                                </td>
                                            </tr>
                                            <!-- Variant rows (Lazy Loaded) -->
                                            <template v-if="expandedProductIds.includes(item.id)">
                                                <tr v-if="loadingProductVariants[item.id]" class="variant-row bg-slate-50">
                                                    <td colspan="4" class="text-center py-3 text-slate-500 text-caption">
                                                        <v-progress-circular indeterminate size="16" width="2" color="primary" class="mr-2" />
                                                        Đang tải biến thể của sản phẩm...
                                                    </td>
                                                </tr>
                                                <tr v-else-if="!item.variants || item.variants.length === 0" class="variant-row bg-slate-50">
                                                    <td colspan="4" class="text-center py-3 text-slate-400 text-caption">
                                                        Sản phẩm chưa có biến thể hoạt động nào.
                                                    </td>
                                                </tr>
                                                <tr v-else v-for="variant in item.variants" :key="variant.id" class="variant-row bg-slate-50">
                                                    <td class="data-cell text-right pr-3"></td>
                                                    <td class="data-cell text-center">
                                                        <div class="d-flex align-center justify-center" style="height: 32px">
                                                            <v-checkbox-btn
                                                                :model-value="isVariantSelected(variant.id)"
                                                                @update:model-value="toggleVariantSelection(variant)"
                                                                :readonly="isDetailView"
                                                                color="primary"
                                                                hide-details
                                                                density="compact"
                                                                class="d-inline-flex ma-0 pa-0"
                                                            ></v-checkbox-btn>
                                                        </div>
                                                    </td>
                                                    <td class="data-cell text-center text-slate-600 font-weight-medium">
                                                        {{ variant.ma }}
                                                    </td>
                                                    <td class="data-cell text-center text-slate-500">
                                                        <div class="d-inline-flex align-center justify-center ga-2">
                                                            <div
                                                                class="color-dot"
                                                                :style="{
                                                                    backgroundColor: getVariantColorHex(variant),
                                                                    border: '1px solid rgba(0, 0, 0, 0.15)'
                                                                }"
                                                            ></div>
                                                            <span>{{ variant.color }} - {{ variant.kichCo }} - {{ variant.chatLieu }}</span>
                                                        </div>
                                                    </td>
                                                </tr>
                                            </template>
                                        </template>
                                    </tbody>
                                </table>

                                <div
                                    v-if="productsList.length === 0"
                                    class="empty-state-wrapper py-12 w-100 d-flex flex-column align-center justify-center border-t"
                                >
                                    <div
                                        class="empty-state-icon-box d-flex align-center justify-center rounded-circle mb-3"
                                        style="width: 68px; height: 68px; background: rgba(241, 245, 249, 0.8); border: 1.5px dashed #cbd5e1"
                                    >
                                        <v-icon icon="mdi-database-search-outline" size="32" style="color: #94a3b8 !important" />
                                    </div>
                                    <span class="text-slate-600 text-center font-weight-medium" style="font-size: 14px !important; line-height: 1.5; max-width: 420px">
                                        Không tìm thấy sản phẩm nào.
                                    </span>
                                </div>
                            </template>
                        </div>

                        <AdminPagination
                            v-model="selectionPage"
                            :page-size="selectionPageSize"
                            @update:pageSize="updateSelectionPageSize"
                            @update:model-value="updateSelectionPage"
                            :total-pages="selectionTotalPages"
                            :total-elements="selectionTotalElements"
                            :current-size="productsList.length"
                            class="mt-4"
                        />
                    </v-card-text>
                </v-card>
            </v-col>

            <v-col cols="12">
                <v-card class="premium-card elevation-0 border border-slate-200">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-8">
                            <div class="icon-blob bg-emerald-lighten-5 mr-3">
                                <v-icon color="emerald-darken-2" size="18">mdi-format-list-checks</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800"
                                >Danh sách chi tiết sản phẩm được chọn
                                <span class="text-primary ml-1">({{ selectedVariantsIds.length }})</span>
                            </span>
                            <v-spacer></v-spacer>
                            <v-btn
                                v-if="!isDetailView && selectedVariantsIds.length > 0"
                                variant="outlined"
                                color="error"
                                prepend-icon="mdi-trash-can-outline"
                                @click="removeAllSelected"
                                style="height: 36px; text-transform: none; border-width: 1px"
                            >
                                Xóa tất cả
                            </v-btn>
                        </div>

                        <!-- Bộ lọc sản phẩm chi tiết sử dụng AdminFilter chuẩn -->
                        <AdminFilter title="Bộ lọc chi tiết" class="mx-n4 bg-transparent" @refresh="handleRefreshDetailSearch">
                            <v-col cols="12" sm="2">
                                <div class="field-label-small mb-1">Tìm kiếm sản phẩm</div>
                                <v-text-field
                                    v-model="detailFilters.timKiem"
                                    prepend-inner-icon="mdi-magnify"
                                    placeholder="Tìm theo mã SP, tên SP..."
                                    variant="outlined"
                                    density="compact"
                                    hide-details
                                    clearable
                                    class="compact-input"
                                ></v-text-field>
                            </v-col>
                            <v-col cols="12" sm="2">
                                <div class="field-label-small mb-1">Thương hiệu</div>
                                <v-select
                                    v-model="detailFilters.thuongHieu"
                                    :items="brands"
                                    density="compact"
                                    variant="outlined"
                                    hide-details
                                    clearable
                                    placeholder="Thương hiệu"
                                    :menu-props="{ contentClass: 'campaign-select-menu' }"
                                    class="compact-input"
                                ></v-select>
                            </v-col>
                            <v-col cols="12" sm="2">
                                <div class="field-label-small mb-1">Chất liệu</div>
                                <v-select
                                    v-model="detailFilters.chatLieu"
                                    :items="materials"
                                    density="compact"
                                    variant="outlined"
                                    hide-details
                                    clearable
                                    placeholder="Chất liệu"
                                    :menu-props="{ contentClass: 'campaign-select-menu' }"
                                    class="compact-input"
                                ></v-select>
                            </v-col>
                            <v-col cols="12" sm="2">
                                <div class="field-label-small mb-1">Kích cỡ</div>
                                <v-select
                                    v-model="detailFilters.kichCo"
                                    :items="sizes"
                                    density="compact"
                                    variant="outlined"
                                    hide-details
                                    clearable
                                    placeholder="Kích cỡ"
                                    :menu-props="{ contentClass: 'campaign-select-menu' }"
                                    class="compact-input"
                                ></v-select>
                            </v-col>
                            <v-col cols="12" sm="2">
                                <div class="field-label-small mb-1">Màu sắc</div>
                                <v-select
                                    v-model="detailFilters.mauSac"
                                    :items="colors"
                                    density="compact"
                                    variant="outlined"
                                    hide-details
                                    clearable
                                    placeholder="Màu sắc"
                                    :menu-props="{ contentClass: 'campaign-select-menu' }"
                                    class="compact-input"
                                ></v-select>
                            </v-col>

                            <template #after>
                                <v-col cols="12" class="mt-4 pa-0">
                                    <div class="d-flex align-center justify-space-between mb-2">
                                        <div class="d-flex align-center ga-2">
                                            <v-icon size="15" color="#3b82f6">mdi-cash-multiple</v-icon>
                                            <span class="text-caption font-weight-bold text-slate-600">Lọc theo giá sau giảm</span>
                                        </div>
                                        <span class="price-range-value text-primary font-weight-bold"
                                            >{{ formatCurrency(detailFilters.khoangGia[0]) }} –
                                            {{ formatCurrency(detailFilters.khoangGia[1]) }}</span
                                        >
                                    </div>
                                    <v-range-slider
                                        :key="`0-${dynamicMaxPrice}`"
                                        v-model="detailFilters.khoangGia"
                                        :max="dynamicMaxPrice"
                                        :min="0"
                                        :step="10000"
                                        hide-details
                                        color="primary"
                                        track-color="#e2e8f0"
                                        track-size="2"
                                        thumb-size="14"
                                        class="blue-range-slider"
                                    ></v-range-slider>
                                </v-col>
                            </template>
                        </AdminFilter>

                        <div class="table-wrapper border rounded-lg overflow-y-auto mt-4 custom-scrollbar" style="max-height: 400px">
                            <table class="native-admin-table">
                                <thead>
                                    <tr>
                                        <th class="header-cell text-center text-no-wrap" style="width: 50px">
                                            <div class="d-flex justify-center align-center">
                                                <v-checkbox-btn
                                                    density="compact"
                                                    color="primary"
                                                    hide-details
                                                    :model-value="
                                                        filteredSelectedDetails.length > 0 &&
                                                        filteredSelectedDetails.every((p) => isVariantSelected(p.id))
                                                    "
                                                    @change="toggleAllBottomSelection"
                                                    :readonly="isDetailView"
                                                ></v-checkbox-btn>
                                            </div>
                                        </th>
                                        <th class="header-cell text-center text-no-wrap" style="width: 50px">STT</th>
                                        <th class="header-cell text-center text-no-wrap" style="width: 80px">Ảnh</th>
                                        <th class="header-cell text-center text-no-wrap">Mã biến thể</th>
                                        <th class="header-cell text-center text-no-wrap">Tên sản phẩm</th>
                                        <th class="header-cell text-center text-no-wrap">Giá bán</th>
                                        <th class="header-cell text-center text-no-wrap">Thương hiệu</th>
                                        <th class="header-cell text-center text-no-wrap">Chất liệu</th>
                                        <th class="header-cell text-center text-no-wrap">Kích cỡ</th>
                                        <th class="header-cell text-center text-no-wrap">Màu sắc</th>
                                        <th v-if="!isDetailView" class="header-cell text-center text-no-wrap" style="width: 70px">Thao tác</th>
                                    </tr>
                                </thead>
                                <tbody v-if="filteredSelectedDetails.length > 0">
                                    <tr
                                        v-for="(item, index) in paginatedSelectedDetails"
                                        :key="item.id + '-' + item.ma + '-' + index"
                                        class="data-row"
                                    >
                                        <td class="data-cell text-center">
                                            <div class="d-flex align-center justify-center" style="height: 32px">
                                                <v-checkbox-btn
                                                    color="primary"
                                                    hide-details
                                                    density="compact"
                                                    :model-value="isVariantSelected(item.id)"
                                                    @update:model-value="toggleVariantSelection(item)"
                                                    :readonly="isDetailView"
                                                ></v-checkbox-btn>
                                            </div>
                                        </td>
                                        <td class="data-cell text-center text-slate-500 font-weight-medium">
                                            {{ (bottomPage - 1) * bottomPageSize + index + 1 }}
                                        </td>
                                        <td class="data-cell text-center py-2">
                                            <div class="product-image-container d-inline-block position-relative">
                                                <v-avatar rounded="lg" size="44" class="border overflow-hidden">
                                                    <SafeProductImage :src="item.anhMauc || item.hinhAnh" :alt="item.tenSanPham" :iconSize="22" />
                                                </v-avatar>
                                                <div v-if="form.soTienGiam > 0" class="discount-badge">-{{ form.soTienGiam }}%</div>
                                            </div>
                                        </td>
                                        <td class="data-cell text-center text-primary font-weight-medium">
                                            {{ item.ma }}
                                        </td>
                                        <td class="data-cell text-center font-weight-medium">
                                            {{ item.tenSanPham }}
                                        </td>
                                        <td class="data-cell text-center">
                                            <div class="text-caption text-slate-400 text-decoration-line-through">
                                                {{ formatCurrency(item.giaGoc) }}
                                            </div>
                                            <div class="discounted-price font-weight-bold">
                                                {{ formatCurrency(calculateDiscountedPrice(item.giaGoc)) }}
                                            </div>
                                        </td>
                                        <td class="data-cell text-center">
                                            {{ item.thuongHieu }}
                                        </td>
                                        <td class="data-cell text-center">
                                            {{ item.chatLieu }}
                                        </td>
                                        <td class="data-cell text-center font-weight-medium">
                                            {{ item.kichCo }}
                                        </td>
                                        <td class="data-cell text-center">
                                            <div class="d-flex align-center justify-center ga-2">
                                                <div
                                                    class="color-dot"
                                                    :style="{
                                                        backgroundColor: getVariantColorHex(item),
                                                        border: '1px solid rgba(0, 0, 0, 0.15)'
                                                    }"
                                                ></div>
                                                <span>{{ item.color }}</span>
                                            </div>
                                        </td>
                                        <td v-if="!isDetailView" class="data-cell text-center">
                                            <v-btn
                                                icon
                                                variant="text"
                                                size="small"
                                                color="error"
                                                density="compact"
                                                class="action-icon-btn"
                                                @click="toggleVariantSelection(item)"
                                            >
                                                <v-icon size="18">mdi-trash-can-outline</v-icon>
                                                <v-tooltip activator="parent" location="top" text="Bỏ chọn biến thể này" />
                                            </v-btn>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>

                            <div
                                v-if="filteredSelectedDetails.length === 0"
                                class="empty-state-wrapper py-12 w-100 d-flex flex-column align-center justify-center border-t"
                            >
                                <div
                                    class="empty-state-icon-box d-flex align-center justify-center rounded-circle mb-3"
                                    style="width: 68px; height: 68px; background: rgba(241, 245, 249, 0.8); border: 1.5px dashed #cbd5e1"
                                >
                                    <v-icon icon="mdi-database-search-outline" size="32" style="color: #94a3b8 !important" />
                                </div>
                                <span class="text-slate-600 text-center font-weight-medium" style="font-size: 14px !important; line-height: 1.5; max-width: 420px">
                                    Không tìm thấy sản phẩm nào phù hợp.
                                </span>
                            </div>
                        </div>

                        <AdminPagination
                            v-model="bottomPage"
                            :page-size="bottomPageSize"
                            @update:pageSize="bottomPageSize = $event"
                            :total-pages="totalBottomPages"
                            :total-elements="filteredSelectedDetails.length"
                            :current-size="paginatedSelectedDetails.length"
                            class="mt-4"
                        />
                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

        <AdminConfirm v-model:show="confirmDialog.show" v-bind="confirmDialog" @confirm="confirmDialog.action" />
    </v-container>
</template>

<style scoped>
.campaign-equal-row {
    align-items: stretch;
}

.campaign-side-card {
    width: 100%;
    display: flex;
    flex-direction: column;
    flex: 1 1 auto;
}

.campaign-side-card :deep(.v-card-text) {
    flex: 1 1 auto;
}

.campaign-product-table {
    flex: 1 1 auto;
    height: 440px;
    max-height: 440px;
    overflow-y: auto;
}

.campaign-product-table :deep(thead th) {
    position: sticky;
    top: 0;
    z-index: 2;
    background-color: #f8fafc !important;
}

:deep(.v-field),
:deep(.v-field__outline) {
    border-radius: 12px !important;
}

:deep(.v-field__input),
:deep(input),
:deep(input::placeholder),
:deep(textarea),
:deep(.v-select__selection-text),
:deep(.v-label) {
    font-size: 13px !important;
}

:deep(.v-field__input::placeholder) {
    font-size: 13px !important;
}

.field-label,
.field-label-small {
    font-size: 13px !important;
}

:global(.campaign-select-menu .v-list-item-title),
:global(.campaign-select-menu .v-list-item) {
    font-size: 13px !important;
}

.campaign-submit-btn {
    border-radius: 16px !important;
    font-size: 13px !important;
    height: 44px !important;
}

.discount-badge {
    font-size: 9px !important;
    font-weight: 500 !important;
    padding: 1px 4px !important;
    border-radius: 4px !important;
    top: -4px !important;
    right: -4px !important;
    left: auto !important;
}
</style>
