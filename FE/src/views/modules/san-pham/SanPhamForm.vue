<script setup>
/**
 * Module: Form san pham
 * Y nghia: tao/cap nhat san pham cha, chon thuoc tinh, sinh bien the theo mau/size,
 * quan ly ton kho-gia-SKU-QR va dong bo payload bien the ve BE.
 */
import { ref, onMounted, computed, watch, reactive, nextTick } from 'vue';
import debounce from 'lodash/debounce';
import { PATH } from '@/router/routePaths';
import { useRoute, useRouter } from 'vue-router';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { dichVuBienThe } from '@/services/product/dichVuBienThe';
import { dichVuFile } from '@/services/core/dichVuFile';
import { useNotifications } from '@/services/notificationService';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import VariantFormModal from '../bien-the-san-pham/components/VariantFormModal.vue';
import FormattedNumberField from './components/FormattedNumberField.vue';
import SafeProductImage from './components/SafeProductImage.vue';
import {
    ArrowLeftIcon, BoxIcon, DeviceFloppyIcon, InfoCircleIcon, PencilIcon,
    PhotoIcon, PlusIcon, SettingsIcon, TrashIcon
} from 'vue-tabler-icons';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import QrcodeVue from 'qrcode.vue';
import {
    dichVuThuongHieu, dichVuXuatXu,
    dichVuChatLieu, dichVuDeGiay, dichVuCoGiay,
    dichVuMucDichChay, dichVuMauSac, dichVuKichThuoc
} from '@/services/product/dichVuThuocTinh';
import logoPlaceholder from '@/assets/images/logos/logo-light.svg';
import { exportQrImageZip } from '@/utils/qrExcelWorkbook';
import { isActiveStatus, getStatusLabel } from '@/utils/statusUtils';
import { generateRandomCode } from '@/utils/codeGenerator';
import {
    cleanSizeString, formatSizeDisplay,
    normalizeSizeInput, blockNonNumericSizeInput, getDisplayItems,
    getNestedValue, createDraftKey, normalizeUploadedFileUrl
} from '@/utils/productFormHelpers';
import { useVariantTable } from '@/composables/useVariantTable';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const MIN_VARIANT_PRICE = 0;
const DEFAULT_MAX_VARIANT_PRICE = 100000000;

const loading = ref(false);
const saving = ref(false);
const attributeCreateState = ref({});

const confirmDialog = ref({
    show: false,
    title: '',
    message: '',
    color: 'primary',
    action: null,
    loading: false
});

const duplicateAttributeDialog = ref({
    show: false,
    duplicateProduct: null
});

const isEditMode = computed(() => !!route.params.id);
const submitButtonText = computed(() => isEditMode.value ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm');
const defaultVariantStatus = 'DANG_HOAT_DONG';

// Danh muc thuoc tinh dung de tao san pham va sinh bien the.
const brands = ref([]);

const materials = ref([]);
const soles = ref([]);
const collars = ref([]);
const origins = ref([]);
const purposes = ref([]);
const colors = ref([]);
const sizes = ref([]);
const existingProductNames = ref([]);

// Tìm kiếm nhanh cho Chip Group
// Tu khoa tim nhanh trong menu chon mau/size.
const colorSearch = ref('');
const sizeSearch = ref('');

// UI Refs cho Biến thể
// Trang thai UI khi them nhanh mau sac/kich thuoc.
const showColorMenu = ref(false);
const showSizeMenu = ref(false);
const customColorName = ref('');
const customColorHex = ref('#FF5733');
const customSizeName = ref('');

// Bảng màu phổ biến (Việt - Anh) mapping sang HEX
// Tu dien mau pho bien de tu dien ma HEX khi nguoi dung nhap ten mau.
const COLOR_DICTIONARY = {
    // Tiếng Việt
    'đỏ': '#FF0000', 'xanh dương': '#0000FF', 'xanh lá': '#00FF00',
    'vàng': '#FFFF00', 'đen': '#000000', 'trắng': '#FFFFFF',
    'hồng': '#FFC0CB', 'tím': '#800080', 'cam': '#FFA500',
    'nâu': '#8B4513', 'xám': '#808080', 'ghi': '#808080',
    'bạc': '#C0C0C0', 'vàng đồng': '#FFD700', 'xanh ngọc': '#40E0D0',
    'xanh mint': '#98FF98', 'xanh rêu': '#4A5D23', 'xanh navy': '#000080',
    'be': '#F5F5DC', 'kem': '#FFFDD0', 'kem sữa': '#FFFFF0',
    'đỏ đô': '#800000', 'đỏ mận': '#800000', 'xanh coban': '#0047AB',

    // Tiếng Anh
    'red': '#FF0000', 'blue': '#0000FF', 'green': '#00FF00',
    'yellow': '#FFFF00', 'black': '#000000', 'white': '#FFFFFF',
    'pink': '#FFC0CB', 'purple': '#800080', 'orange': '#FFA500',
    'brown': '#8B4513', 'gray': '#808080', 'grey': '#808080',
    'silver': '#C0C0C0', 'gold': '#FFD700', 'turquoise': '#40E0D0',
    'mint': '#98FF98', 'navy': '#000080', 'beige': '#F5F5DC',
    'cream': '#FFFDD0', 'cyan': '#00FFFF', 'magenta': '#FF00FF',
    'maroon': '#800000'
};

// Hàm bỏ dấu tiếng việt
const removeAccents = (str) => str ? str.normalize('NFD').replace(/[\u0300-\u036f]/g, '') : '';

const getColorHexFallback = (colorItem) => {
    if (colorItem?.maMauHex) return colorItem.maMauHex;
    if (!colorItem?.ten) return '#e2e8f0';

    const normalizedName = colorItem.ten.trim().toLowerCase();
    const normalizedNoAccent = removeAccents(normalizedName);

    for (const [key, hex] of Object.entries(COLOR_DICTIONARY)) {
        if (normalizedName === key || normalizedNoAccent === removeAccents(key)) {
            return hex;
        }
    }
    return '#e2e8f0'; // Default fallback
};

watch(customColorName, (newName) => {
    if (!newName) return;
    const normalizedName = newName.trim().toLowerCase();
    const normalizedNoAccent = removeAccents(normalizedName);

    // Tìm kiếm trong từ điển
    for (const [key, hex] of Object.entries(COLOR_DICTIONARY)) {
        if (normalizedName === key || normalizedNoAccent === removeAccents(key)) {
            customColorHex.value = hex;
            break;
        }
    }
});

const toggleColor = (id) => {
    const idx = selectedColors.value.indexOf(id);
    if (idx > -1) selectedColors.value.splice(idx, 1);
    else selectedColors.value.push(id);
};
const toggleSize = (id) => {
    const idx = selectedSizes.value.indexOf(id);
    if (idx > -1) selectedSizes.value.splice(idx, 1);
    else selectedSizes.value.push(id);
};

const handleAddCustomColor = async () => {
    const rawName = customColorName.value.trim();
    const rawHex = customColorHex.value.trim().toUpperCase();

    if (!rawName || !rawHex) return;

    const existingColor = colors.value.find(c => (c.maMauHex || '').toUpperCase() === rawHex);

    if (existingColor) {
        if (!selectedColors.value.includes(existingColor.id)) {
            selectedColors.value.push(existingColor.id);
            addNotification({ title: 'Thành công', subtitle: `Tự động chọn màu "${existingColor.ten}" do trùng mã HEX`, color: 'success' });
        } else {
            addNotification({ title: 'Lỗi', subtitle: 'Màu này đã được chọn', color: 'error' });
        }
        customColorName.value = '';
        customColorHex.value = '#FF5733';
        showColorMenu.value = false;
        return;
    }

    try {
        const newColor = await dichVuMauSac.taoMauSac({ ten: rawName, maMauHex: rawHex });
        colors.value = [newColor, ...colors.value];
        selectedColors.value.push(newColor.id);
        customColorName.value = '';
        customColorHex.value = '#FF5733';
        showColorMenu.value = false;
        addNotification({ title: 'Thành công', subtitle: 'Đã thêm màu mới', color: 'success' });
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể thêm màu', color: 'error' });
    }
};

const updateCustomSizeName = (value) => {
    customSizeName.value = normalizeSizeInput(value);
};

const handleSizePaste = (event) => {
    event.preventDefault();
    customSizeName.value = normalizeSizeInput(event.clipboardData?.getData('text'));
};

// Them/chon size moi tu popover: trung thi chon lai, chua co thi tao thuoc tinh kich thuoc.
const handleAddCustomSize = async () => {
    const rawInput = customSizeName.value;

    // 1. Lỗi rỗng và khoảng trắng: Để trống hoặc chỉ chứa khoảng trắng
    if (!rawInput || !rawInput.trim()) return;

    const normalizedSize = cleanSizeString(rawInput);

    if (!normalizedSize) {
        addNotification({ title: 'Lỗi', subtitle: 'Kích thước không hợp lệ', color: 'error' });
        return;
    }

    // Xử lý dấu phẩy thành dấu chấm cho số thập phân
    const parsedStr = normalizedSize.replace(',', '.');
    const sizeNumber = Number(parsedStr);

    // 1. Phải là số
    if (isNaN(sizeNumber)) {
        addNotification({ title: 'Lỗi', subtitle: 'Kích thước phải là số (VD: 39 hoặc 39.5)', color: 'error' });
        return;
    }

    // 2. Range 40 đến 70
    if (sizeNumber < 35 || sizeNumber > 60) {
        addNotification({ title: 'Loi', subtitle: 'Kich thuoc phai tu 35 den 60', color: 'error' });
        return;
        addNotification({ title: 'Lỗi', subtitle: 'Kích thước phải từ 40 đến 70', color: 'error' });
        return;
    }

    // 3. Phải là số nguyên (không có .5, .0...)
    if (!Number.isInteger(sizeNumber)) {
        addNotification({ title: 'Lỗi', subtitle: 'Kích thước phải là số nguyên (ví dụ: 39, 40)', color: 'error' });
        return;
    }

    // 4. Định dạng lại final (ví dụ 40.0 -> 40)
    const finalNumericSize = sizeNumber.toString();

    // 2. Lỗi trùng lặp: Kiểm tra trùng lặp không phân biệt hoa thường
    const existingSize = sizes.value.find(s => cleanSizeString(s.ten).toLowerCase() === normalizedSize.toLowerCase());

    if (existingSize) {
        // Trùng lặp với dữ liệu đã tồn tại trong hệ thống
        if (selectedSizes.value.includes(existingSize.id)) {
            addNotification({ title: 'Lỗi', subtitle: 'Kích thước này đã được chọn', color: 'error' });
        } else {
            selectedSizes.value.push(existingSize.id);
            addNotification({ title: 'Thành công', subtitle: 'Đã chọn kích thước có sẵn', color: 'success' });
            showSizeMenu.value = false;
        }
        customSizeName.value = '';
        return;
    }

    // 3. Nếu hợp lệ và chưa tồn tại -> Lưu mới
    try {
        const finalSizeName = `Size ${finalNumericSize}`;
        const newSize = await dichVuKichThuoc.taoKichThuoc({ ten: finalSizeName });
        sizes.value = [newSize, ...sizes.value];
        selectedSizes.value.push(newSize.id);
        customSizeName.value = '';
        showSizeMenu.value = false;
        addNotification({ title: 'Thành công', subtitle: 'Đã thêm kích thước mới', color: 'success' });
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể thêm kích thước', color: 'error' });
    }
};

const addAllFilteredSizes = () => {
    filteredSizes.value.forEach(s => {
        if (!selectedSizes.value.includes(s.id)) {
            selectedSizes.value.push(s.id);
        }
    });
    showSizeMenu.value = false;
};

const filteredColors = computed(() => {
    const query = normalizeSearchText(colorSearch.value);
    if (!query) return colors.value;
    return colors.value.filter(c => normalizeSearchText(c.ten).includes(query));
});
const sortedSizes = computed(() => {
    return [...sizes.value].sort((a, b) => {
        // Lấy ra phần số trong chuỗi (ví dụ: 'Size 40' -> 40, '39.5' -> 39.5)
        const numA = parseFloat(a.ten.replace(/[^0-9.]/g, '')) || 0;
        const numB = parseFloat(b.ten.replace(/[^0-9.]/g, '')) || 0;
        return numA - numB;
    });
});

const filteredSizes = computed(() => {
    const query = normalizeSearchText(customSizeName.value);
    if (!query) return sortedSizes.value;
    return sortedSizes.value.filter(s => normalizeSearchText(s.ten).includes(query));
});

// Theo dõi nội dung đang gõ trong các combobox
const searchQueries = reactive({
    idThuongHieu: '',
    idXuatXu: '',
    idChatLieu: '',
    idDeGiay: '',
    idCoGiay: '',
    idMucDichChay: ''
});

const displayBrands = computed(() => getDisplayItems(brands.value, searchQueries.idThuongHieu, 'idThuongHieu'));
const displayOrigins = computed(() => getDisplayItems(origins.value, searchQueries.idXuatXu, 'idXuatXu'));

const displayMaterials = computed(() => getDisplayItems(materials.value, searchQueries.idChatLieu, 'idChatLieu'));
const displaySoles = computed(() => getDisplayItems(soles.value, searchQueries.idDeGiay, 'idDeGiay'));
const displayCollars = computed(() => getDisplayItems(collars.value, searchQueries.idCoGiay, 'idCoGiay'));
const displayPurposes = computed(() => getDisplayItems(purposes.value, searchQueries.idMucDichChay, 'idMucDichChay'));

// Mau/size duoc chon de sinh ma tran bien the san pham.
const selectedColors = ref([]);
const selectedSizes = ref([]);
// Danh sach bien the dang thao tac tren FE truoc khi gui payload ve BE.
const variantItems = ref([]);
const colorImageState = ref({});
const colorFileInputRefs = ref({});
// Gia/ton kho ap dung nhanh cho tat ca bien the hoac theo tung nhom mau.
const bulkAllForm = ref({
    soLuong: '',
    giaNhap: '',
    giaBan: ''
});
const bulkByColorForms = ref({});
// Modal them/sua mot bien the rieng le.
const variantModal = ref({
    open: false,
    mode: 'create',
    submitting: false,
    variant: null
});
const qrExportItems = ref([]);
const qrExportContainer = ref(null);

// Bảng biến thể (lọc/khoảng giá/phân trang/chọn) — tách sang composable useVariantTable.
const {
    variantTableFilters,
    variantPriceBounds,
    variantPage,
    variantPageSize,
    selectedVariantKeys,
    variantPriceStep,
    filteredVariantItems,
    totalVariantElements,
    totalVariantPages,
    paginatedVariantItems,
    visibleVariantKeys,
    selectedVariants,
    allVisibleVariantsSelected,
    someVisibleVariantsSelected,
    updateVariantPriceBounds,
    sanitizeVariantPriceRange,
    updateVariantPriceBoundary,
    handleVariantSliderPriceChange,
    clearVariantSelection,
    syncVariantSelection,
    toggleVariantSelection,
    toggleSelectVisibleVariants,
    resetVariantTableFilters
} = useVariantTable(variantItems, {
    getVariantColorLabel: (id) => getVariantColorLabel(id),
    getVariantSizeLabel: (id) => getVariantSizeLabel(id),
    getVariantKey: (variant) => getVariantKey(variant),
    minPrice: MIN_VARIANT_PRICE,
    maxPrice: DEFAULT_MAX_VARIANT_PRICE
});

// Options truyen sang modal bien the gom mau, size va trang thai hop le.
const variantOptions = computed(() => ({
    mauSacs: colors.value,
    kichThuocs: sizes.value,
    trangThais: [defaultVariantStatus, 'NGUNG_HOAT_DONG']
}));
const variantTableHeaders = [
    { text: 'Chọn', width: '70px' },
    { text: 'Ảnh', width: '80px' },
    { text: 'Màu sắc', width: '140px' },
    { text: 'Kích thước', width: '140px' },
    { text: 'SKU', width: '240px' },
    { text: 'Tồn kho', width: '110px' },
    { text: 'Giá bán', width: '130px' },
    { text: 'Trạng thái', width: '160px' },
    { text: 'Thao tác', width: '120px' }
];
const variantFilterProductLabel = computed(() => (
    [product.value.maSanPham, product.value.tenSanPham].filter(Boolean).join(' • ') || 'Sản phẩm hiện tại'
));

const totalVariantStock = computed(() => variantItems.value.reduce(
    (sum, item) => sum + Number(item.soLuong || 0),
    0
));

// Gom bien the theo mau de hien tab/chinh sua nhanh theo tung mau.
const variantsByColor = computed(() => {
    const groups = {};
    variantItems.value.forEach(item => {
        if (!groups[item.idMauSac]) {
            groups[item.idMauSac] = [];
        }
        groups[item.idMauSac].push(item);
    });
    return groups;
});

const activeColorTab = ref('ALL');

// Form ap dung nhanh gia ban/gia nhap/ton kho cho nhom bien the dang hien.
const quickApplyValues = reactive({
    giaBan: '',
    giaNhap: '',
    soLuong: ''
});

const handleQuickApply = () => {
    const { giaBan, giaNhap, soLuong } = quickApplyValues;
    if (giaBan === '' && giaNhap === '' && soLuong === '') return;

    variantItems.value = variantItems.value.map(item => {
        if (activeColorTab.value === 'ALL' || String(item.idMauSac) === String(activeColorTab.value)) {
            return {
                ...item,
                giaBan: giaBan !== '' ? Number(giaBan) : item.giaBan,
                giaNhap: giaNhap !== '' ? Number(giaNhap) : item.giaNhap,
                soLuong: soLuong !== '' ? Number(soLuong) : item.soLuong
            };
        }
        return item;
    });

    quickApplyValues.giaBan = '';
    quickApplyValues.giaNhap = '';
    quickApplyValues.soLuong = '';
    addNotification({ title: 'Thành công', subtitle: 'Đã áp dụng nhanh giá trị', color: 'success' });
};

const visibleVariantItems = computed(() => {
    if (activeColorTab.value === 'ALL') {
        return variantItems.value;
    }
    return variantItems.value.filter(item => String(item.idMauSac) === String(activeColorTab.value));
});

const createVariantPage = ref(1);
const createVariantPageSize = ref(5);
const createVariantTotalPages = computed(() => Math.max(Math.ceil(visibleVariantItems.value.length / createVariantPageSize.value), 1));
const paginatedVisibleVariantItems = computed(() => {
    const start = (createVariantPage.value - 1) * createVariantPageSize.value;
    return visibleVariantItems.value.slice(start, start + createVariantPageSize.value);
});

watch(activeColorTab, () => {
    createVariantPage.value = 1;
});
watch(createVariantPageSize, () => {
    createVariantPage.value = 1;
});

const bulkEditModal = reactive({
    show: false,
    targetColorId: null,
    form: {
        soLuong: '',
        giaNhap: '',
        giaBan: ''
    }
});

const openBulkEdit = (colorId = null) => {
    bulkEditModal.targetColorId = colorId;
    bulkEditModal.form = { soLuong: '', giaNhap: '', giaBan: '' };
    bulkEditModal.show = true;
};

const applyBulkEdit = () => {
    const { soLuong, giaNhap, giaBan } = bulkEditModal.form;
    variantItems.value = variantItems.value.map(item => {
        if (bulkEditModal.targetColorId === null || String(item.idMauSac) === String(bulkEditModal.targetColorId)) {
            return {
                ...item,
                soLuong: soLuong !== '' ? Number(soLuong) : item.soLuong,
                giaNhap: giaNhap !== '' ? Number(giaNhap) : item.giaNhap,
                giaBan: giaBan !== '' ? Number(giaBan) : item.giaBan
            };
        }
        return item;
    });
    bulkEditModal.show = false;
    addNotification({
        title: 'Thành công',
        subtitle: `Đã cập nhật hàng loạt cho ${bulkEditModal.targetColorId ? 'màu sắc này' : 'tất cả biến thể'}`,
        color: 'success'
    });
};

const variantContextSummary = computed(() => (
    [
        getOptionLabel(soles, product.value.idDeGiay),
        getOptionLabel(collars, product.value.idCoGiay),
        getOptionLabel(purposes, product.value.idMucDichChay)
    ].filter((item) => item && item !== '--')
));

const getAttributeMethodName = (type) => `tao${type.split('_').map(
    word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
).join('')}`;

const getAttributeFetchMethodName = (type) => `lay${type.split('_').map(
    word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
).join('')}`;

const getBackendErrorMessage = (error, fallbackMessage) => (
    error?.response?.data?.message
    || error?.message
    || fallbackMessage
);

const getVariantColorHex = (idMauSac) => {
    const color = colors.value.find(c => String(c.id) === String(idMauSac));
    return color ? getColorHexFallback(color) : '#ffffff';
};

const normalizeSearchText = (value) => String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim();

const comboboxFilter = (itemTitle, queryText, item) => {
    const normalizedQuery = normalizeSearchText(queryText);
    if (!normalizedQuery) {
        return true;
    }

    const normalizedTitle = normalizeSearchText(item?.raw?.ten || itemTitle || item?.value);
    return normalizedTitle.includes(normalizedQuery);
};

const comboboxProps = {
    clearable: true,
    autoSelectFirst: false
};



const removeDraftVariantByObject = (variant) => {
    variantItems.value = variantItems.value.filter(item => item !== variant);
};

const normalizeAttributeText = (value) => {
    if (typeof value === 'string') {
        return value.trim();
    }

    if (value && typeof value === 'object') {
        const textCandidate = [value.ten, value.title, value.label, value.text]
            .find(item => typeof item === 'string' && item.trim());
        if (textCandidate) {
            return textCandidate.trim();
        }

        if (typeof value.value === 'string') {
            return value.value.trim();
        }
    }

    return '';
};

const findAttributeOption = (config, value) => {
    const options = config.options.value || [];

    if (value && typeof value === 'object') {
        if (value.id) {
            const matchedById = options.find(item => item.id === value.id);
            if (matchedById) {
                return matchedById;
            }
        }

        if (typeof value.value === 'string') {
            const matchedByValue = findAttributeOption(config, value.value);
            if (matchedByValue) {
                return matchedByValue;
            }
        }
    }

    if (typeof value !== 'string') {
        return null;
    }

    const normalizedValue = value.trim();
    if (!normalizedValue) {
        return null;
    }

    const normalizedLower = normalizeSearchText(normalizedValue);
    return options.find(item => (
        (item.id === normalizedValue && !String(item.id).startsWith('__new__'))
        || normalizeSearchText(item.ten) === normalizedLower
    )) || null;
};

const upsertAttributeOption = (config, option) => {
    if (!option?.id) {
        return;
    }

    const options = config.options.value || [];
    if (options.some(item => item.id === option.id)) {
        return;
    }

    config.options.value = [option, ...options];
};

const refreshAttributeOptions = async (config) => {
    const fetchMethod = getAttributeFetchMethodName(config.type);
    if (typeof config.service?.[fetchMethod] !== 'function') {
        return;
    }

    const response = await config.service[fetchMethod]({ size: 1000 });
    config.options.value = Array.isArray(response?.content)
        ? response.content
        : Array.isArray(response)
            ? response
            : config.options.value;
};

const resolveAttributeField = async (config, { notifyOnCreate = false } = {}) => {
    let currentValue = product.value[config.field];

    // Nếu là ID tạm thời từ việc click "Thêm nhanh", bóc tách lấy text thực tế
    let isExplicitQuickAdd = false;
    if (typeof currentValue === 'string' && currentValue.startsWith('__new__')) {
        currentValue = currentValue.replace('__new__', '');
        isExplicitQuickAdd = true;
    }

    const existingOption = findAttributeOption(config, currentValue);

    if (existingOption) {
        product.value[config.field] = existingOption.id;
        searchQueries[config.field] = existingOption.ten;
        return existingOption.id;
    }

    if (currentValue && typeof currentValue === 'object' && currentValue.id) {
        product.value[config.field] = currentValue.id;
        return currentValue.id;
    }

    const normalizedText = normalizeAttributeText(currentValue);
    if (!normalizedText) {
        product.value[config.field] = null;
        return null;
    }

    const requestKey = normalizeSearchText(normalizedText);
    const pendingRequest = attributeCreateState.value[config.field];
    if (pendingRequest?.key === requestKey) {
        const resolvedId = await pendingRequest.promise;
        product.value[config.field] = resolvedId;
        return resolvedId;
    }

    let createdOption = null;
    const requestPromise = (async () => {
        try {
            createdOption = await config.service[getAttributeMethodName(config.type)]({
                ten: normalizedText,
                moTa: 'Tự động thêm từ sản phẩm'
            });
            upsertAttributeOption(config, createdOption);
            return createdOption.id;
        } catch (error) {
            await refreshAttributeOptions(config).catch(() => null);
            const matchedAfterRefresh = findAttributeOption(config, normalizedText);
            if (matchedAfterRefresh) {
                return matchedAfterRefresh.id;
            }
            throw error;
        } finally {
            if (attributeCreateState.value[config.field]?.key === requestKey) {
                const nextState = { ...attributeCreateState.value };
                delete nextState[config.field];
                attributeCreateState.value = nextState;
            }
        }
    })();

    attributeCreateState.value = {
        ...attributeCreateState.value,
        [config.field]: {
            key: requestKey,
            promise: requestPromise
        }
    };

    const resolvedId = await requestPromise;
    product.value[config.field] = resolvedId;
    if (createdOption) {
        await nextTick();
        searchQueries[config.field] = createdOption.ten;
    }

    if (createdOption && notifyOnCreate) {
        addNotification({
            title: 'Thành công',
            subtitle: `Đã thêm nhanh ${config.label}: ${normalizedText}`,
            color: 'success'
        });
    }

    return resolvedId;
};

const onKeyUpEnter = async (event, field) => {
    const val = event.target.value?.trim();
    if (!val) return;

    const config = attributeConfig.find(item => item.field === field);
    if (!config) {
        return;
    }

    product.value[field] = val;

    try {
        await resolveAttributeField(config, { notifyOnCreate: true });
    } catch (error) {
        console.error(error);
        addNotification({
            title: 'Lỗi',
            subtitle: getBackendErrorMessage(error, `Không thể thêm nhanh ${config.label}`),
            color: 'error'
        });
    }
};


const formatCurrency = (value) => {
    if (value === null || value === undefined || value === '') return '--';
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND',
        maximumFractionDigits: 0
    }).format(Number(value));
};

const formatNumber = (value) => {
    if (value === null || value === undefined || value === '') return '0';
    return new Intl.NumberFormat('vi-VN').format(Number(value));
};

// getStatusLabel is now imported from @/utils/statusUtils


const normalizeOptionList = (listLike) => {
    if (Array.isArray(listLike)) return listLike;
    if (Array.isArray(listLike?.value)) return listLike.value;
    return [];
};

const getOptionLabel = (listLike, id) => normalizeOptionList(listLike).find(item => item.id === id)?.ten || '--';
const getVariantKey = (variant) => variant.id || variant.clientKey;
const getVariantSkuLabel = (variant) => {
    const sku = variant.maChiTietSanPham;
    if (!sku) return 'Tự sinh sau khi lưu sản phẩm';
    return sku;
};
const getVariantSkuValue = (variant = {}) => (
    variant.maChiTietSanPham
    || variant.sku
    || variant.maSku
    || variant.maBienThe
    || variant.ma
    || ''
);
const getVariantColorIdValue = (variant = {}) => (
    getNestedValue(variant, ['idMauSac', 'mauSacId'])
    || getNestedValue(variant.mauSac, ['id', 'value', 'ma'])
    || getNestedValue(variant.color, ['id', 'value', 'ma'])
    || ''
);
const getVariantColorLabelValue = (variant = {}) => (
    getNestedValue(variant, ['tenMauSac'])
    || getNestedValue(variant.mauSac, ['ten', 'name', 'label', 'title'])
    || getNestedValue(variant.color, ['ten', 'name', 'label', 'title'])
    || ''
);
const getVariantSizeIdValue = (variant = {}) => (
    getNestedValue(variant, ['idKichThuoc', 'kichThuocId', 'sizeId'])
    || getNestedValue(variant.kichThuoc, ['id', 'value', 'ma'])
    || getNestedValue(variant.size, ['id', 'value', 'ma'])
    || ''
);
const getVariantSizeLabelValue = (variant = {}) => (
    getNestedValue(variant, ['tenKichThuoc'])
    || getNestedValue(variant.kichThuoc, ['ten', 'name', 'label', 'title', 'giaTriKichThuoc'])
    || getNestedValue(variant.size, ['ten', 'name', 'label', 'title'])
    || ''
);
const getVariantQrValue = (variant) => variant?.maChiTietSanPham || getVariantKey(variant) || '';
const getVariantThumbnail = (variant) => {
    return normalizeUploadedFileUrl(
        variant.urlAnh
        || variant.images?.find(image => image.hinhAnhDaiDien)?.duongDanAnh
        || variant.images?.[0]?.duongDanAnh
        || variant.hinhAnhs?.find(image => image.hinhAnhDaiDien)?.duongDanAnh
        || variant.hinhAnhs?.[0]?.duongDanAnh
        || variant.anhChiTietSanPhams?.find(image => image.hinhAnhDaiDien)?.duongDanAnh
        || variant.anhChiTietSanPhams?.[0]?.duongDanAnh
        || variant.hinhAnh?.[0]?.duongDanAnh
        || variant.hinhAnh?.[0]?.url
        || variant.hinhAnh
        || variant.duongDanAnh
        || variant.imageUrl
        || variant.anh
    ) || logoPlaceholder;
};
const getVariantCombinationKey = (colorId, sizeId) => `${colorId}-${sizeId}`;
const getColorUploadEntry = (colorId) => colorImageState.value[colorId] || { url: '', uploading: false };
const getColorUploadPreviewUrl = (colorId) => getColorUploadEntry(colorId).url || logoPlaceholder;
const getBulkColorForm = (colorId) => {
    if (!bulkByColorForms.value[colorId]) {
        bulkByColorForms.value[colorId] = { soLuong: '', giaNhap: '', giaBan: '' };
    }
    return bulkByColorForms.value[colorId];
};
const hasAnyBulkValue = (form) => ['soLuong', 'giaNhap', 'giaBan'].some((field) => form[field] !== '' && form[field] !== null && form[field] !== undefined);
const getVariantColorLabel = (colorId) => getOptionLabel(colors, colorId);
const getVariantSizeLabel = (sizeId) => getOptionLabel(sizes, sizeId);
const getVariantDescriptor = () => variantContextSummary.value.join(' • ');

// Chuyen response bien the tu BE ve state FE thong nhat cho form/bang/modal.
const mapVariantToFormState = (variant = {}) => ({
    id: variant.id || null,
    clientKey: variant.clientKey || createDraftKey(),
    maChiTietSanPham: getVariantSkuValue(variant),
    idMauSac: getVariantColorIdValue(variant),
    tenMauSac: getVariantColorLabelValue(variant) || (getOptionLabel(colors, getVariantColorIdValue(variant)) !== '--' ? getOptionLabel(colors, getVariantColorIdValue(variant)) : ''),
    idKichThuoc: getVariantSizeIdValue(variant),
    tenKichThuoc: getVariantSizeLabelValue(variant) || (getOptionLabel(sizes, getVariantSizeIdValue(variant)) !== '--' ? getOptionLabel(sizes, getVariantSizeIdValue(variant)) : ''),
    soLuong: Number(variant.soLuong ?? 0),
    giaNhap: Number(variant.giaNhap ?? 0),
    giaBan: Number(variant.giaBan ?? 0),
    trangThai: variant.trangThai || defaultVariantStatus,
    urlAnh: getVariantThumbnail(variant) === logoPlaceholder ? '' : getVariantThumbnail(variant)
});

// Tao bien the nhap lieu tu cap mau-size; giu lai gia/ton kho neu da co ban nhap cu.
const createGeneratedVariant = (colorId, sizeId, existingVariant = {}, fallbackImageUrl = '') => mapVariantToFormState({
    ...existingVariant,
    clientKey: existingVariant.clientKey || createDraftKey(),
    idMauSac: colorId,
    idKichThuoc: sizeId,
    soLuong: Number(existingVariant.soLuong ?? 0),
    giaNhap: Number(existingVariant.giaNhap ?? 0),
    giaBan: Number(existingVariant.giaBan ?? 0),
    trangThai: existingVariant.trangThai || defaultVariantStatus,
    maChiTietSanPham: existingVariant.maChiTietSanPham || '',
    urlAnh: normalizeUploadedFileUrl(existingVariant.urlAnh || fallbackImageUrl || '')
});

// Dong goi bien the theo schema ProductVariantRequest cua BE.
const buildVariantPayload = (variant, includeImages = true) => {
    const imageUrl = normalizeUploadedFileUrl(variant.urlAnh);
    const payload = {
        maChiTietSanPham: variant.maChiTietSanPham || null,
        idMauSac: variant.idMauSac,
        idKichThuoc: variant.idKichThuoc,
        soLuong: Number(variant.soLuong ?? 0),
        giaNhap: Number(variant.giaNhap ?? 0),
        giaBan: Number(variant.giaBan ?? 0),
        trangThai: variant.trangThai || defaultVariantStatus
    };

    if (includeImages) {
        payload.images = imageUrl
            ? [{ duongDanAnh: imageUrl, hinhAnhDaiDien: true }]
            : [];
    }

    return payload;
};

const handleAttributeChange = async (field, value) => {
    // Chỉ tự động tạo nếu người dùng chọn mục "Thêm nhanh" (có prefix __new__)
    // Hoặc nếu họ nhấn Enter (được xử lý ở onKeyUpEnter)
    if (typeof value === 'string' && value.startsWith('__new__')) {
        const config = attributeConfig.find(item => item.field === field);
        if (!config) return;

        try {
            await resolveAttributeField(config, { notifyOnCreate: true });
        } catch (error) {
            console.error(error);
            addNotification({
                title: 'Lỗi',
                subtitle: getBackendErrorMessage(error, `Không thể thêm nhanh ${config.label}`),
                color: 'error'
            });
        }
    }
};

const renderVariantQrCanvases = async (variants) => {
    qrExportItems.value = variants.map((variant) => ({
        id: getVariantKey(variant),
        value: getVariantQrValue(variant)
    }));

    await nextTick();
    await new Promise((resolve) => window.setTimeout(resolve, 80));

    const canvases = Array.from(qrExportContainer.value?.querySelectorAll('canvas') || []);
    const qrDataUrls = canvases.map((canvas) => canvas.toDataURL('image/png'));
    qrExportItems.value = [];
    return qrDataUrls;
};

const handleExportVariantQrZip = async () => {
    const targetVariants = selectedVariants.value.length ? selectedVariants.value : [];
    if (!targetVariants.length) {
        addNotification({
            title: 'Thông báo',
            subtitle: 'Chọn ít nhất 1 biến thể để xuất ZIP QR',
            color: 'warning'
        });
        return;
    }

    try {
        const fileSuffix = product.value.maSanPham || 'tat_ca';
        const qrDataUrls = await renderVariantQrCanvases(targetVariants);
        if (qrDataUrls.length !== targetVariants.length || qrDataUrls.some((item) => !item)) {
            throw new Error('Không thể tạo đủ dữ liệu QR để xuất ZIP');
        }

        exportQrImageZip({
            fileName: `qrcode_bien_the_da_chon_${fileSuffix}.zip`,
            items: targetVariants.map((variant, index) => ({
                baseName: variant.maChiTietSanPham
                    || [product.value.tenSanPham, getVariantColorLabel(variant.idMauSac), getVariantSizeLabel(variant.idKichThuoc)]
                        .filter(Boolean)
                        .join('_')
                    || `variant_${index + 1}`,
                dataUrl: qrDataUrls[index]
            }))
        });

        addNotification({
            title: 'Thành công',
            subtitle: `Đã xuất ZIP QR của ${targetVariants.length} biến thể`,
            color: 'success'
        });
    } catch (error) {
        console.error('QR export error:', error);
        qrExportItems.value = [];
        addNotification({
            title: 'Lỗi',
            subtitle: 'Không thể xuất ZIP QR của biến thể',
            color: 'error'
        });
    }
};

const updateVariantStatusLocally = (variantKey, nextStatus) => {
    variantItems.value = variantItems.value.map((item) => (
        getVariantKey(item) === variantKey
            ? { ...item, trangThai: nextStatus }
            : item
    ));
};

const persistVariantStatus = async (variant, nextStatus) => {
    if (!variant.id) {
        updateVariantStatusLocally(getVariantKey(variant), nextStatus);
        return;
    }

    await dichVuBienThe.capNhatBienThe(
        variant.id,
        buildVariantPayload({ ...variant, trangThai: nextStatus }, false)
    );
    updateVariantStatusLocally(getVariantKey(variant), nextStatus);
};

const buildProductPayload = ({ includeVariants = false } = {}) => {
    const payload = {
        maSanPham: product.value.maSanPham || null,
        tenSanPham: product.value.tenSanPham || null,
        idThuongHieu: product.value.idThuongHieu,

        idXuatXu: product.value.idXuatXu,
        idChatLieu: product.value.idChatLieu,
        idDeGiay: product.value.idDeGiay,
        idCoGiay: product.value.idCoGiay,
        idMucDichChay: product.value.idMucDichChay,
        gioiTinhKhachHang: product.value.gioiTinhKhachHang,
        trangThai: product.value.trangThai,
        hinhAnh: product.value.hinhAnh || '',

        moTaChiTiet: product.value.moTaChiTiet || ''
    };

    if (includeVariants) {
        payload.variants = variantItems.value.map(item => buildVariantPayload(item, true));
    }

    return payload;
};

const hasDuplicateVariant = (payload, currentKey = null) => variantItems.value.some((item) => (
    String(item.idMauSac) === String(payload.idMauSac)
    && String(item.idKichThuoc) === String(payload.idKichThuoc)
    && getVariantKey(item) !== currentKey
));

const variantGroups = computed(() => {
    const groupedMap = new Map();

    variantItems.value.forEach((variant, index) => {
        const colorId = variant.idMauSac;
        if (!groupedMap.has(colorId)) {
            groupedMap.set(colorId, {
                colorId,
                colorLabel: getVariantColorLabel(colorId),
                variants: []
            });
        }

        groupedMap.get(colorId).variants.push({
            ...variant,
            index
        });
    });

    const orderedColorIds = isEditMode.value
        ? [...groupedMap.keys()]
        : selectedColors.value.filter((colorId) => groupedMap.has(colorId));

    const groups = orderedColorIds.map((colorId) => groupedMap.get(colorId)).filter(Boolean);
    groups.forEach((group) => {
        getBulkColorForm(group.colorId);
    });
    return groups;
});

const syncColorImageStateFromVariants = () => {
    const nextState = {};
    variantItems.value.forEach((variant) => {
        const imageUrl = normalizeUploadedFileUrl(variant.urlAnh);
        if (!variant.idMauSac || !imageUrl || nextState[variant.idMauSac]) return;
        nextState[variant.idMauSac] = {
            url: imageUrl,
            uploading: false
        };
    });
    colorImageState.value = nextState;
};

const applyColorImageToVariants = (colorId, imageUrl) => {
    const normalizedImageUrl = normalizeUploadedFileUrl(imageUrl);
    variantItems.value = variantItems.value.map((variant) => (
        variant.idMauSac === colorId
            ? { ...variant, urlAnh: normalizedImageUrl || '' }
            : variant
    ));

    const nextState = { ...colorImageState.value };
    if (normalizedImageUrl) {
        nextState[colorId] = { url: normalizedImageUrl, uploading: false };
    } else {
        delete nextState[colorId];
    }
    colorImageState.value = nextState;
};

const setColorImageUploading = (colorId, uploading) => {
    colorImageState.value = {
        ...colorImageState.value,
        [colorId]: {
            ...getColorUploadEntry(colorId),
            uploading
        }
    };
};

const setColorFileInputRef = (colorId, el) => {
    if (el) {
        colorFileInputRefs.value[colorId] = el;
        return;
    }
    delete colorFileInputRefs.value[colorId];
};

const openColorImagePicker = (colorId) => {
    colorFileInputRefs.value[colorId]?.click();
};

const handleColorImageUpload = async (colorId, event) => {
    const file = event?.target?.files?.[0];
    if (!file) return;

    setColorImageUploading(colorId, true);
    try {
        const imageUrl = normalizeUploadedFileUrl(await dichVuFile.taiLenFile(file));
        applyColorImageToVariants(colorId, imageUrl);
        addNotification({
            title: 'Thành công',
            subtitle: `Đã cập nhật ảnh cho màu ${getVariantColorLabel(colorId)}`,
            color: 'success'
        });
    } catch (error) {
        console.error(error);
        addNotification({
            title: 'Lỗi',
            subtitle: `Không thể tải ảnh cho màu ${getVariantColorLabel(colorId)}`,
            color: 'error'
        });
    } finally {
        setColorImageUploading(colorId, false);
        if (event?.target) event.target.value = '';
    }
};

const clearColorImage = (colorId) => {
    applyColorImageToVariants(colorId, '');
};

const applyBulkValues = (predicate, form, successMessage) => {
    if (!hasAnyBulkValue(form)) {
        addNotification({
            title: 'Thiếu dữ liệu',
            subtitle: 'Nhập ít nhất một giá trị để áp dụng nhanh',
            color: 'warning'
        });
        return;
    }

    variantItems.value = variantItems.value.map((variant) => {
        if (!predicate(variant)) return variant;

        return {
            ...variant,
            soLuong: form.soLuong !== '' ? Number(form.soLuong) : variant.soLuong,
            giaNhap: form.giaNhap !== '' ? Number(form.giaNhap) : variant.giaNhap,
            giaBan: form.giaBan !== '' ? Number(form.giaBan) : variant.giaBan
        };
    });

    addNotification({
        title: 'Thành công',
        subtitle: successMessage,
        color: 'success'
    });
};

const applyBulkAllVariants = () => {
    applyBulkValues(
        () => true,
        bulkAllForm.value,
        'Đã áp dụng nhanh cho toàn bộ biến thể'
    );
};

const applyBulkColorVariants = (colorId) => {
    applyBulkValues(
        (variant) => variant.idMauSac === colorId,
        getBulkColorForm(colorId),
        `Đã áp dụng nhanh cho nhóm màu ${getVariantColorLabel(colorId)}`
    );
};

const clearAllDraftVariants = () => {
    variantItems.value = [];
    selectedColors.value = [];
    selectedSizes.value = [];
    colorImageState.value = {};
    bulkAllForm.value = { soLuong: '', giaNhap: '', giaBan: '' };
    bulkByColorForms.value = {};
};

const removeColorGroup = (colorId) => {
    variantItems.value = variantItems.value.filter((variant) => variant.idMauSac !== colorId);
    selectedColors.value = selectedColors.value.filter((id) => id !== colorId);

    const nextState = { ...colorImageState.value };
    delete nextState[colorId];
    colorImageState.value = nextState;

    const nextBulkByColor = { ...bulkByColorForms.value };
    delete nextBulkByColor[colorId];
    bulkByColorForms.value = nextBulkByColor;
};

const executeGenerateVariants = () => {
    const existingVariantMap = new Map(
        variantItems.value.map(item => [
            getVariantCombinationKey(item.idMauSac, item.idKichThuoc),
            item
        ])
    );

    const nextVariants = [];
    selectedColors.value.forEach((colorId) => {
        selectedSizes.value.forEach((sizeId) => {
            const combinationKey = getVariantCombinationKey(colorId, sizeId);
            nextVariants.push(createGeneratedVariant(
                colorId,
                sizeId,
                existingVariantMap.get(combinationKey),
                getColorUploadEntry(colorId).url
            ));
        });
    });

    variantItems.value = nextVariants;

    const validColorIds = new Set(selectedColors.value);
    colorImageState.value = Object.fromEntries(
        Object.entries(colorImageState.value).filter(([colorId]) => validColorIds.has(colorId))
    );

    addNotification({
        title: 'Thành công',
        subtitle: `Đã tạo tự động ${nextVariants.length} biến thể`,
        color: 'success'
    });
};

// Sinh/cap nhat danh sach bien the tu tat ca cap mau-size da chon.
const generateVariants = () => {
    if (selectedColors.value.length === 0 || selectedSizes.value.length === 0) {
        addNotification({
            title: 'Cảnh báo',
            subtitle: 'Vui lòng chọn ít nhất 1 màu sắc và 1 kích thước để tạo biến thể',
            color: 'warning'
        });
        return;
    }

    const totalToGenerate = selectedColors.value.length * selectedSizes.value.length;

    confirmDialog.value = {
        show: true,
        title: 'Xác nhận tạo biến thể',
        message: `Bạn có chắc chắn muốn tạo tự động ${totalToGenerate} biến thể dựa trên các thuộc tính đã chọn? Thao tác này sẽ cập nhật lại danh sách biến thể hiện tại.`,
        color: 'primary',
        loading: false,
        action: () => {
            executeGenerateVariants();
            confirmDialog.value.show = false;
        }
    };
};

const removeDraftVariantByIndex = (index) => {
    variantItems.value.splice(index, 1);
};

const fetchFormOptions = async () => {
    try {
        const [opts, productsRes] = await Promise.all([
            dichVuSanPham.layOptionsForm().catch(() => null),
            dichVuSanPham.layDanhSachSanPham({ size: 1000 }).catch(() => null)
        ]);

        if (productsRes) {
            const productList = Array.isArray(productsRes.content) ? productsRes.content : (Array.isArray(productsRes) ? productsRes : []);
            existingProductNames.value = [...new Set(productList.map(p => p.tenSanPham).filter(Boolean))];
        }

        const filterActive = (list) => {
            if (!Array.isArray(list)) return [];
            return list.filter(item => !item.trangThai || item.trangThai === 'DANG_HOAT_DONG');
        };

        if (opts) {
            brands.value = filterActive(opts.thuongHieus);

            origins.value = filterActive(opts.xuatXus);
            purposes.value = filterActive(opts.mucDichChays);
            collars.value = filterActive(opts.coGiays);
            materials.value = filterActive(opts.chatLieus);
            soles.value = filterActive(opts.deGiays);
            colors.value = filterActive(opts.mauSacs);
            sizes.value = filterActive(opts.kichThuocs);
            return;
        }

        const [b, c, o, p, col, m, s, mau, size] = await Promise.all([
            dichVuThuongHieu.layThuongHieu({ size: 1000 }),
            dichVuXuatXu.layXuatXu({ size: 1000 }),
            dichVuMucDichChay.layMucDichChay({ size: 1000 }),
            dichVuCoGiay.layCoGiay({ size: 1000 }),
            dichVuChatLieu.layChatLieu({ size: 1000 }),
            dichVuDeGiay.layDeGiay({ size: 1000 }),
            dichVuMauSac.layMauSac({ size: 1000 }),
            dichVuKichThuoc.layKichThuoc({ size: 1000 })
        ]);

        brands.value = filterActive(b.content || b);

        origins.value = filterActive(o.content || o);
        purposes.value = filterActive(p.content || p);
        collars.value = filterActive(col.content || col);
        materials.value = filterActive(m.content || m);
        soles.value = filterActive(s.content || s);
        colors.value = filterActive(mau.content || mau);
        sizes.value = filterActive(size.content || size);
    } catch (error) {
        console.error('Lỗi khi tải options:', error);
        addNotification({ title: 'Cảnh báo', subtitle: 'Không thể tải đầy đủ thuộc tính', color: 'warning' });
    }
};

// FORM STATE
const tenError = ref('');
const existingProductId = ref(null);

const checkProductName = debounce(async (name) => {
    if (!name || !name.trim()) {
        tenError.value = '';
        existingProductId.value = null;
        return;
    }

    // Only check if creating a new product
    if (isEditMode.value) return;

    try {
        const result = await dichVuSanPham.kiemTraTenSanPham(name);
        if (result && result.exists) {
            tenError.value = 'Tên sản phẩm đã tồn tại trong hệ thống!';
            existingProductId.value = result.productId;
        } else {
            tenError.value = '';
            existingProductId.value = null;
        }
    } catch (error) {
        console.error('Lỗi khi check tên sản phẩm', error);
    }
}, 500);

// Note: watch block moved below product initialization
// Navigate to edit page to add variants
const handleNavigateToUpdate = () => {
    if (existingProductId.value) {
        router.push(`${PATH.SAN_PHAM_FORM}/${existingProductId.value}`);
    }
};

// FORM STATE
const product = ref({
    maSanPham: '',
    tenSanPham: null,
    moTa: '',
    idThuongHieu: null,

    idXuatXu: null,
    idChatLieu: null,
    idDeGiay: null,
    idCoGiay: null,
    idMucDichChay: null,
    gioiTinhKhachHang: null,
    trangThai: defaultVariantStatus,

    moTaChiTiet: '',
    hinhAnh: ''
});

// Watch for product name changes
watch(() => product.value.tenSanPham, (newVal) => {
    if (!isEditMode.value) {
        checkProductName(newVal);
    }
});

const loadProduct = async (id) => {
    const [data, variantsData] = await Promise.all([
        dichVuSanPham.layChiTietSanPham(id),
        dichVuBienThe.layBienTheTheoSanPham(id).catch(() => [])
    ]);
    product.value = {
        maSanPham: data.maSanPham || data.ma || '',
        tenSanPham: data.tenSanPham || null,
        moTa: data.moTa || '',
        idThuongHieu: data.idThuongHieu || null,

        idXuatXu: data.idXuatXu || null,
        idChatLieu: data.idChatLieu || null,
        idDeGiay: data.idDeGiay || null,
        idCoGiay: data.idCoGiay || null,
        idMucDichChay: data.idMucDichChay || null,
        gioiTinhKhachHang: data.gioiTinhKhachHang || null,
        trangThai: data.trangThai || defaultVariantStatus,

        moTaChiTiet: data.moTaChiTiet || '',
        hinhAnh: data.hinhAnh || ''
    };
    variantItems.value = (variantsData || []).map(mapVariantToFormState);
    syncColorImageStateFromVariants();
};

// Các watcher đồng bộ giá/phân trang/lựa chọn của bảng biến thể đã nằm trong useVariantTable.

const loadInitData = async () => {
    loading.value = true;
    try {
        await fetchFormOptions();

        if (route.params.id) {
            await loadProduct(route.params.id);

            // Check if there are variants to merge
            const mergeKey = 'mergeVariants_' + route.params.id;
            const mergeVariantsStr = localStorage.getItem(mergeKey);
            if (mergeVariantsStr) {
                try {
                    const variantsToMerge = JSON.parse(mergeVariantsStr);
                    if (Array.isArray(variantsToMerge) && variantsToMerge.length > 0) {
                        // Append to current variants, avoiding duplicates if possible
                        // Ensure they don't have IDs since they are new to this product
                        const sanitizedToMerge = variantsToMerge.map(v => ({ ...v, id: null, ma: '' }));
                        variantItems.value = [...variantItems.value, ...sanitizedToMerge];
                        addNotification({ title: 'Đã gộp', subtitle: `Đã tự động thêm ${variantsToMerge.length} biến thể từ sản phẩm trùng lặp. Vui lòng bấm Cập nhật để lưu.`, color: 'info' });
                    }
                } catch (e) {
                    console.error('Lỗi khi parse biến thể gộp', e);
                } finally {
                    localStorage.removeItem(mergeKey);
                }
            }
        } else {
            try {
                product.value.maSanPham = await generateRandomCode('SanPham');
            } catch (e) {
                console.error('Lỗi khi lấy mã', e);
            }
        }
    } catch (error) {
        console.error('Error initializing form:', error);
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải dữ liệu khởi tạo', color: 'error' });
    } finally {
        loading.value = false;
    }
};

watch(() => route.params.id, () => {
    if (route.name === 'SanPhamForm') {
        loadInitData();
    }
});

onMounted(() => {
    loadInitData();
});

const rules = {
    required: value => !!value || 'Trường này là bắt buộc',
    noSpecialChar: value => {
        if (!value) return true;
        return /^[\p{L}0-9\s]+$/u.test(value) || 'Không được chứa ký tự đặc biệt';
    },
    uniqueProductName: value => {
        if (!value) return true;
        const valStr = String(value).trim().toLowerCase();
        if (isEditMode.value && originalProductName.value && valStr === String(originalProductName.value).trim().toLowerCase()) {
            return true;
        }
        const exists = existingProductNames.value.some(name => String(name).trim().toLowerCase() === valStr);
        return !exists || 'Tên sản phẩm đã tồn tại. Vui lòng thêm hậu tố (tên đệm) để phân biệt.';
    }
};

const attributeConfig = [
    { field: 'idThuongHieu', service: dichVuThuongHieu, type: 'THUONG_HIEU', label: 'thương hiệu', options: brands },

    { field: 'idXuatXu', service: dichVuXuatXu, type: 'XUAT_XU', label: 'xuất xứ', options: origins },
    { field: 'idChatLieu', service: dichVuChatLieu, type: 'CHAT_LIEU', label: 'chất liệu', options: materials },
    { field: 'idDeGiay', service: dichVuDeGiay, type: 'DE_GIAY', label: 'đế giày', options: soles },
    { field: 'idCoGiay', service: dichVuCoGiay, type: 'CO_GIAY', label: 'cổ giày', options: collars },
    { field: 'idMucDichChay', service: dichVuMucDichChay, type: 'MUC_DICH_CHAY', label: 'mục đích sử dụng', options: purposes }
];

const validateProduct = () => {
    const requiredFields = [
        ['tenSanPham', 'Tên sản phẩm'],
        ['idThuongHieu', 'Thương hiệu'],

        ['idXuatXu', 'Xuất xứ'],
        ['idChatLieu', 'Chất liệu'],
        ['idDeGiay', 'Loại đế giày'],
        ['idCoGiay', 'Loại cổ giày'],
        ['idMucDichChay', 'Mục đích sử dụng']
    ];

    const missing = requiredFields
        .filter(([field]) => !product.value[field])
        .map(([, label]) => label);

    if (tenError.value) {
        addNotification({
            title: 'Lỗi',
            subtitle: tenError.value,
            color: 'error'
        });
        return false;
    }

    if (missing.length > 0) {
        addNotification({
            title: 'Lỗi',
            subtitle: `Vui lòng điền đủ thông tin bắt buộc: ${missing.join(', ')}`,
            color: 'error'
        });
        return false;
    }

    if (product.value.tenSanPham && product.value.tenSanPham.length > 255) {
        addNotification({
            title: 'Lỗi',
            subtitle: 'Tên sản phẩm không được vượt quá 255 ký tự',
            color: 'error'
        });
        return false;
    }

    const valStr = String(product.value.tenSanPham).trim().toLowerCase();
    const isSameAsOriginal = isEditMode.value && originalProductName.value && valStr === String(originalProductName.value).trim().toLowerCase();
    if (!isSameAsOriginal) {
        const exists = existingProductNames.value.some(name => String(name).trim().toLowerCase() === valStr);
        if (exists) {
            addNotification({
                title: 'Lỗi trùng tên',
                subtitle: 'Tên sản phẩm đã tồn tại. Vui lòng nhập thêm tên đệm để phân biệt.',
                color: 'error'
            });
            return false;
        }
    }

    if (!isEditMode.value && variantItems.value.length === 0) {
        addNotification({
            title: 'Thiếu biến thể',
            subtitle: 'Vui lòng tạo ít nhất 1 biến thể trước khi lưu sản phẩm',
            color: 'error'
        });
        return false;
    }

    for (let i = 0; i < variantItems.value.length; i++) {
        const item = variantItems.value[i];
        if (!item.idMauSac || !item.idKichThuoc) {
            addNotification({
                title: 'Lỗi',
                subtitle: 'Danh sách biến thể có dữ liệu chưa hợp lệ. Vui lòng kiểm tra lại màu sắc và kích thước.',
                color: 'error'
            });
            return false;
        }

        const soLuong = Number(item.soLuong);
        const giaNhap = Number(item.giaNhap);
        const giaBan = Number(item.giaBan);

        if (isNaN(soLuong) || soLuong < 0 || !Number.isInteger(soLuong)) {
            addNotification({
                title: 'Lỗi',
                subtitle: 'Số lượng tồn kho của biến thể không hợp lệ (phải là số nguyên lớn hơn hoặc bằng 0).',
                color: 'error'
            });
            return false;
        }

        if (isNaN(giaNhap) || giaNhap < 0) {
            addNotification({
                title: 'Lỗi',
                subtitle: 'Giá nhập của biến thể không hợp lệ.',
                color: 'error'
            });
            return false;
        }

        if (isNaN(giaBan) || giaBan < 0) {
            addNotification({
                title: 'Lỗi',
                subtitle: 'Giá bán của biến thể không hợp lệ.',
                color: 'error'
            });
            return false;
        }

        if (giaBan < giaNhap) {
            addNotification({
                title: 'Lỗi',
                subtitle: `Biến thể (Màu: ${item.tenMauSac || getVariantColorLabel(item.idMauSac)}, Size: ${item.tenKichThuoc || getVariantSizeLabel(item.idKichThuoc)}) có giá bán (${formatCurrency(giaBan)}) thấp hơn giá nhập (${formatCurrency(giaNhap)}). Vui lòng kiểm tra lại.`,
                color: 'error'
            });
            return false;
        }
    }

    return true;
};

const openCreateVariantModal = () => {
    variantModal.value = {
        open: true,
        mode: 'create',
        submitting: false,
        variant: null
    };
};

const openEditVariantModal = (variant) => {
    variantModal.value = {
        open: true,
        mode: 'edit',
        submitting: false,
        variant: { ...variant }
    };
};

const closeVariantModal = () => {
    variantModal.value = {
        open: false,
        mode: 'create',
        submitting: false,
        variant: null
    };
};

const handleToggleVariantStatus = (variant) => {
    const nextStatus = isVariantActiveStatus(variant.trangThai) ? 'NGUNG_HOAT_DONG' : defaultVariantStatus;
    confirmDialog.value = {
        show: true,
        title: 'Đổi trạng thái biến thể',
        message: `Bạn có chắc chắn muốn ${nextStatus === defaultVariantStatus ? 'bật' : 'tắt'} trạng thái cho biến thể này không?`,
        color: 'warning',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                await persistVariantStatus(variant, nextStatus);
                if (variant.id) {
                    await loadProduct(route.params.id);
                }
                addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật trạng thái biến thể', color: 'success' });
                confirmDialog.value.show = false;
            } catch (error) {
                console.error(error);
                addNotification({ title: 'Lỗi', subtitle: 'Không thể cập nhật trạng thái biến thể', color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

const handleVariantSubmit = async (payload) => {
    const existingVariant = variantModal.value.variant || null;
    const currentKey = existingVariant ? getVariantKey(existingVariant) : null;
    const normalizedPayload = {
        ...existingVariant,
        ...payload,
        idMauSac: payload.idMauSac || existingVariant?.idMauSac || '',
        idKichThuoc: payload.idKichThuoc || existingVariant?.idKichThuoc || '',
        maChiTietSanPham: payload.maChiTietSanPham || existingVariant?.maChiTietSanPham || '',
        trangThai: payload.trangThai || existingVariant?.trangThai || defaultVariantStatus
    };

    if (hasDuplicateVariant(normalizedPayload, currentKey)) {
        addNotification({
            title: 'Lỗi',
            subtitle: 'Tổ hợp màu sắc và kích thước này đã tồn tại.',
            color: 'error'
        });
        return;
    }

    if (!isEditMode.value) {
        const nextVariant = mapVariantToFormState({
            ...normalizedPayload,
            clientKey: existingVariant?.clientKey || createDraftKey()
        });

        if (variantModal.value.mode === 'create') {
            variantItems.value = [...variantItems.value, nextVariant];
            addNotification({ title: 'Thành công', subtitle: 'Đã thêm biến thể nháp', color: 'success' });
        } else {
            variantItems.value = variantItems.value.map(item => (
                getVariantKey(item) === currentKey ? nextVariant : item
            ));
            addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật biến thể nháp', color: 'success' });
        }

        closeVariantModal();
        return;
    }

    variantModal.value.submitting = true;
    try {
        if (variantModal.value.mode === 'create') {
            await dichVuBienThe.taoBienThe(route.params.id, buildVariantPayload(normalizedPayload, true));
            addNotification({ title: 'Thành công', subtitle: 'Đã thêm biến thể mới', color: 'success' });
        } else {
            await dichVuBienThe.capNhatBienThe(
                variantModal.value.variant.id,
                buildVariantPayload(normalizedPayload, true)
            );
            addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật biến thể', color: 'success' });
        }

        closeVariantModal();
        await loadProduct(route.params.id);
    } catch (error) {
        console.error(error);
        addNotification({
            title: 'Lỗi',
            subtitle: getBackendErrorMessage(error, 'Không thể lưu biến thể'),
            color: 'error'
        });
    } finally {
        variantModal.value.submitting = false;
    }
};

const handleRemoveVariant = (variant) => {
    const isRemoteVariant = isEditMode.value && variant.id;
    confirmDialog.value = {
        show: true,
        title: isRemoteVariant ? 'Xóa biến thể' : 'Xóa biến thể nháp',
        message: isRemoteVariant
            ? 'Bạn có chắc chắn muốn xóa biến thể này không?'
            : 'Bạn có chắc chắn muốn bỏ biến thể này khỏi danh sách trước khi lưu không?',
        color: 'warning',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                if (isRemoteVariant) {
                    await dichVuBienThe.xoaBienThe(variant.id);
                    await loadProduct(route.params.id);
                    addNotification({ title: 'Thành công', subtitle: 'Đã xóa biến thể', color: 'success' });
                } else {
                    variantItems.value = variantItems.value.filter(item => getVariantKey(item) !== getVariantKey(variant));
                    addNotification({ title: 'Thành công', subtitle: 'Đã xóa biến thể nháp', color: 'success' });
                }
                confirmDialog.value.show = false;
            } catch (error) {
                console.error(error);
                addNotification({ title: 'Lỗi', subtitle: 'Không thể xóa biến thể', color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

const proceedWithSave = (creatingNew, variantCount) => {
    confirmDialog.value = {
        show: true,
        title: creatingNew ? 'Xác nhận thêm mới' : 'Xác nhận cập nhật',
        message: creatingNew
            ? (variantCount > 0
                ? `Bạn có chắc chắn muốn thêm sản phẩm mới cùng ${variantCount} biến thể không?`
                : 'Bạn có chắc chắn muốn thêm sản phẩm mới này không?')
            : 'Bạn có chắc chắn muốn cập nhật thông tin sản phẩm này không?',
        color: 'primary',
        action: async () => {
            confirmDialog.value.loading = true;
            saving.value = true;
            try {
                if (creatingNew) {
                    await dichVuSanPham.taoSanPham(buildProductPayload({ includeVariants: true }));
                    addNotification({
                        title: 'Thành công',
                        subtitle: variantCount > 0
                            ? `Đã thêm sản phẩm mới cùng ${variantCount} biến thể`
                            : 'Đã thêm sản phẩm mới',
                        color: 'success'
                    });
                } else {
                    await dichVuSanPham.capNhatSanPham(route.params.id, buildProductPayload());
                    addNotification({ title: 'Thành công', subtitle: 'Cập nhật sản phẩm hoàn tất', color: 'success' });
                }

                confirmDialog.value.show = false;
                router.push(PATH.SAN_PHAM);
            } catch (error) {
                console.error(error);
                addNotification({ title: 'Lỗi', subtitle: getBackendErrorMessage(error, 'Không thể lưu sản phẩm'), color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
                saving.value = false;
            }
        }
    };
};

const handleMergeDuplicate = () => {
    const dupProdId = duplicateAttributeDialog.value.duplicateProduct.id;
    duplicateAttributeDialog.value.show = false;

    // Store new variants in localStorage to merge after redirect
    const newVariantsToMerge = variantItems.value;
    if (newVariantsToMerge.length > 0) {
        localStorage.setItem('mergeVariants_' + dupProdId, JSON.stringify(newVariantsToMerge));
    }

    router.push({ path: `/admin/san-pham/form/${dupProdId}` });
};

const handleSave = async () => {
    for (const config of attributeConfig) {
        try {
            await resolveAttributeField(config);
        } catch (error) {
            console.error(error);
            addNotification({
                title: 'Lỗi',
                subtitle: getBackendErrorMessage(error, `Không thể tạo tự động ${config.label}`),
                color: 'error'
            });
            return;
        }
    }

    if (!validateProduct()) {
        return;
    }

    const creatingNew = !isEditMode.value;
    const variantCount = variantItems.value.length;

    if (creatingNew) {
        try {
            const payload = {
                idThuongHieu: product.value.idThuongHieu,
                idXuatXu: product.value.idXuatXu,
                idMucDichChay: product.value.idMucDichChay,
                idCoGiay: product.value.idCoGiay,
                idChatLieu: product.value.idChatLieu,
                idDeGiay: product.value.idDeGiay
            };
            const checkRes = await dichVuSanPham.checkDuplicateAttributes(payload);
            if (checkRes && checkRes.exists) {
                duplicateAttributeDialog.value = {
                    show: true,
                    duplicateProduct: checkRes
                };
                return;
            }
        } catch (error) {
            console.error('Lỗi khi kiểm tra thuộc tính trùng lặp', error);
        }
    }

    proceedWithSave(creatingNew, variantCount);
};
</script>

<template>
    <v-container fluid class="pa-6 animate-fade-in overflow-y-auto" style="height: 100vh;">
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý sản phẩm', disabled: false, href: '#' },
            { title: 'Danh sách sản phẩm', disabled: false, to: PATH.SAN_PHAM },
            { title: isEditMode ? 'Cập nhật' : 'Thêm mới', disabled: true }
        ]" />

        <div class="d-flex align-center justify-space-between mb-8 mt-4 header-actions">
            <div class="d-flex align-center gap-4">
                <v-btn icon variant="flat" @click="router.push(PATH.SAN_PHAM)" class="btn-back-header">
                    <ArrowLeftIcon size="20" />
                    <v-tooltip activator="parent" location="top" text="Quay lại danh sách sản phẩm" />
                </v-btn>
            </div>
            <div class="d-flex gap-3 header-actions__buttons">
                <v-btn v-if="isEditMode" variant="outlined" color="primary"
                    class="text-none font-weight-bold px-6 rounded-lg h-11 border-2"
                    @click="router.push({ name: 'BienTheSanPham', query: { productId: route.params.id } })">
                    <BoxIcon size="18" class="mr-2" /> Quản lý biến thể
                </v-btn>
                <v-btn color="primary" variant="flat"
                    class="text-none font-weight-medium px-8 rounded-lg h-11 elevation-4" :loading="saving"
                    @click="handleSave">
                    <DeviceFloppyIcon size="18" class="mr-2" />
                    {{ submitButtonText }}
                </v-btn>
            </div>
        </div>

        <v-row v-if="loading">
            <v-col cols="12" class="text-center py-16">
                <v-progress-circular indeterminate color="primary" size="64" />
                <div class="mt-4 text-subtitle-1 font-weight-bold text-slate-500">Đang tải thông tin sản phẩm...</div>
            </v-col>
        </v-row>

        <v-row v-else class="form-grid">
            <v-col cols="12" :lg="isEditMode ? 12 : 8" class="d-flex flex-column">
                <v-card class="premium-card h-100 d-flex flex-column">
                    <v-card-text class="pa-8 flex-grow-1">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-blue-lighten-5 mr-3">
                                <InfoCircleIcon size="18" class="text-primary" />
                            </div>
                            <span class="text-subtitle-1 text-slate-800">Sản phẩm</span>
                        </div>
                        <v-row>
                            <!-- HÀNG 1: THÔNG TIN CHÍNH -->

                            <v-col cols="12" md="3">
                                <div class="field-label">Mã sản phẩm</div>
                                <v-text-field v-model="product.maSanPham" readonly
                                    :placeholder="isEditMode ? '' : 'Mã tự động tạo...'" variant="outlined"
                                    density="comfortable" class="custom-input mono-font" hide-details>
                                </v-text-field>
                            </v-col>
                            <v-col cols="12" md="3">
                                <div class="field-label">Tên sản phẩm <span class="text-error">*</span></div>
                                <v-combobox v-model="product.tenSanPham" :items="existingProductNames"
                                    placeholder="Ví dụ: Giày Nike Air..."
                                    :rules="[rules.required, rules.noSpecialChar, rules.uniqueProductName]"
                                    variant="outlined" density="comfortable" hide-details="auto" maxlength="250"
                                    :return-object="false"></v-combobox>
                                <v-alert v-if="tenError" type="error" variant="tonal" density="compact"
                                    class="mt-2 text-caption">
                                    {{ tenError }}
                                    <div class="mt-1" v-if="existingProductId">
                                        <a href="#" @click.prevent="handleNavigateToUpdate"
                                            class="text-decoration-underline font-weight-bold text-error">
                                            Chuyển sang Cập nhật sản phẩm này
                                        </a>
                                    </div>
                                </v-alert>
                            </v-col>
                            <v-col cols="12" md="3">
                                <div class="field-label">Thương hiệu <span class="text-error">*</span></div>
                                <v-combobox v-model="product.idThuongHieu" v-model:search="searchQueries.idThuongHieu"
                                    v-bind="comboboxProps" :custom-filter="() => true" :items="displayBrands"
                                    item-title="ten" item-value="id" :rules="[rules.required]"
                                    placeholder="Thương hiệu..." variant="outlined" density="comfortable"
                                    :return-object="false" @keyup.enter="(e) => onKeyUpEnter(e, 'idThuongHieu')"
                                    @update:model-value="(val) => handleAttributeChange('idThuongHieu', val)">
                                    <template #item="{ props, item }">
                                        <v-list-item v-bind="props">
                                            <template #append v-if="item.raw.isNew">
                                                <span class="text-primary ml-2" style="font-size: 13px;">Thêm
                                                    nhanh</span>
                                            </template>
                                        </v-list-item>
                                    </template>
                                </v-combobox>
                            </v-col>


                            <!-- HÀNG 2: THUỘC TÍNH PHÂN LOẠI -->
                            <v-col cols="12" md="3">
                                <div class="field-label">Xuất xứ <span class="text-error">*</span></div>
                                <v-combobox v-model="product.idXuatXu" v-model:search="searchQueries.idXuatXu"
                                    v-bind="comboboxProps" :custom-filter="() => true" :items="displayOrigins"
                                    item-title="ten" item-value="id" :rules="[rules.required]" placeholder="Xuất xứ"
                                    variant="outlined" density="comfortable" :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idXuatXu')"
                                    @update:model-value="(val) => handleAttributeChange('idXuatXu', val)">
                                    <template #item="{ props, item }">
                                        <v-list-item v-bind="props">
                                            <template #append v-if="item.raw.isNew">
                                                <span class="text-primary ml-2" style="font-size: 13px;">Thêm
                                                    nhanh</span>
                                            </template>
                                        </v-list-item>
                                    </template>
                                </v-combobox>
                            </v-col>
                            <v-col cols="12" md="3">
                                <div class="field-label">Chất liệu <span class="text-error">*</span></div>
                                <v-combobox v-model="product.idChatLieu" v-model:search="searchQueries.idChatLieu"
                                    v-bind="comboboxProps" :custom-filter="() => true" :items="displayMaterials"
                                    item-title="ten" item-value="id" :rules="[rules.required]" placeholder="Chất liệu"
                                    variant="outlined" density="comfortable" :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idChatLieu')"
                                    @update:model-value="(val) => handleAttributeChange('idChatLieu', val)">
                                    <template #item="{ props, item }">
                                        <v-list-item v-bind="props">
                                            <template #append v-if="item.raw.isNew">
                                                <span class="text-primary ml-2" style="font-size: 13px;">Thêm
                                                    nhanh</span>
                                            </template>
                                        </v-list-item>
                                    </template>
                                </v-combobox>
                            </v-col>
                            <v-col cols="12" md="3">
                                <div class="field-label">Đối tượng <span class="text-error">*</span></div>
                                <v-select v-model="product.gioiTinhKhachHang"
                                    :items="[{ title: 'Nam', value: 'NAM' }, { title: 'Nữ', value: 'NU' }, { title: 'Unisex', value: 'UNISEX' }]"
                                    :rules="[rules.required]" clearable variant="outlined" density="comfortable"
                                    placeholder="Đối tượng"></v-select>
                            </v-col>
                            <v-col cols="12" md="3">
                                <div class="field-label">Mục đích <span class="text-error">*</span></div>
                                <v-combobox v-model="product.idMucDichChay" v-model:search="searchQueries.idMucDichChay"
                                    v-bind="comboboxProps" :custom-filter="() => true" :items="displayPurposes"
                                    item-title="ten" item-value="id" :rules="[rules.required]" placeholder="Mục đích"
                                    variant="outlined" density="comfortable" :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idMucDichChay')"
                                    @update:model-value="(val) => handleAttributeChange('idMucDichChay', val)">
                                    <template #item="{ props, item }">
                                        <v-list-item v-bind="props">
                                            <template #append v-if="item.raw.isNew">
                                                <span class="text-primary ml-2" style="font-size: 13px;">Thêm
                                                    nhanh</span>
                                            </template>
                                        </v-list-item>
                                    </template>
                                </v-combobox>
                            </v-col>

                            <!-- HÀNG 3: THÔNG SỐ VÀ MÔ TẢ -->
                            <v-col cols="12" md="6">
                                <div class="field-label">Loại đế <span class="text-error">*</span></div>
                                <v-combobox v-model="product.idDeGiay" v-model:search="searchQueries.idDeGiay"
                                    v-bind="comboboxProps" :custom-filter="() => true" :items="displaySoles"
                                    item-title="ten" item-value="id" :rules="[rules.required]" placeholder="Loại đế"
                                    variant="outlined" density="comfortable" :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idDeGiay')"
                                    @update:model-value="(val) => handleAttributeChange('idDeGiay', val)">
                                    <template #item="{ props, item }">
                                        <v-list-item v-bind="props">
                                            <template #append v-if="item.raw.isNew">
                                                <span class="text-primary ml-2" style="font-size: 13px;">Thêm
                                                    nhanh</span>
                                            </template>
                                        </v-list-item>
                                    </template>
                                </v-combobox>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Loại cổ <span class="text-error">*</span></div>
                                <v-combobox v-model="product.idCoGiay" v-model:search="searchQueries.idCoGiay"
                                    v-bind="comboboxProps" :custom-filter="() => true" :items="displayCollars"
                                    item-title="ten" item-value="id" :rules="[rules.required]" placeholder="Loại cổ"
                                    variant="outlined" density="comfortable" :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idCoGiay')"
                                    @update:model-value="(val) => handleAttributeChange('idCoGiay', val)">
                                    <template #item="{ props, item }">
                                        <v-list-item v-bind="props">
                                            <template #append v-if="item.raw.isNew">
                                                <span class="text-primary ml-2" style="font-size: 13px;">Thêm
                                                    nhanh</span>
                                            </template>
                                        </v-list-item>
                                    </template>
                                </v-combobox>
                            </v-col>
                            <v-col cols="12">
                                <div class="field-label">Mô tả chi tiết</div>
                                <v-textarea v-model="product.moTaChiTiet" variant="outlined" rows="3" auto-grow
                                    placeholder="Mô tả chi tiết sản phẩm..." hide-details
                                    class="custom-textarea"></v-textarea>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>
            </v-col>

            <!-- Cấu hình biến thể (Chỉ Thêm Mới) -->
            <v-col cols="12" lg="4" v-if="!isEditMode" class="d-flex flex-column">
                <v-card class="premium-card h-100 d-flex flex-column">
                    <v-card-text class="pa-8 flex-grow-1 d-flex flex-column">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob brand-logo-icon-blob mr-3">
                                <BoxIcon size="18" class="brand-logo-icon" />
                            </div>
                            <span class="text-subtitle-1 text-slate-800">Biến thể sản phẩm</span>
                        </div>

                        <div class="mt-2 flex-grow-1">
                            <!-- MÀU SẮC -->
                            <div class="field-label mb-3">Màu sắc <span class="text-error">*</span></div>
                            <div class="d-flex flex-wrap gap-4 mb-8">
                                <div v-for="c in colors.filter(x => selectedColors.includes(x.id))" :key="c.id"
                                    class="text-center cursor-pointer" @click="toggleColor(c.id)">
                                    <div class="color-circle mx-auto"
                                        :style="{ backgroundColor: getColorHexFallback(c) }"
                                        :class="{ 'selected': selectedColors.includes(c.id) }">
                                        <v-icon v-if="selectedColors.includes(c.id)" color="white" size="18"
                                            class="check-icon">mdi-check</v-icon>
                                    </div>
                                    <div class="text-caption mt-1 text-truncate" style="max-width: 48px;">{{ c.ten }}
                                    </div>
                                </div>
                                <v-menu v-model="showColorMenu" :close-on-content-click="false" location="bottom center"
                                    max-width="320">
                                    <template v-slot:activator="{ props }">
                                        <div class="text-center cursor-pointer" v-bind="props">
                                            <div class="color-circle dashed mx-auto d-flex align-center justify-center">
                                                <v-icon size="20" color="grey-darken-1">mdi-plus</v-icon>
                                            </div>
                                            <div class="text-caption mt-1 text-grey-darken-1">Thêm</div>
                                        </div>
                                    </template>
                                    <v-card class="rounded-xl pa-4 shadow-lg border">
                                        <div class="d-flex justify-space-between align-center mb-4">
                                            <span class="text-subtitle-1 font-weight-bold">Chọn màu sắc</span>
                                            <v-btn icon="mdi-close" variant="text" size="small"
                                                @click="showColorMenu = false"></v-btn>
                                        </div>
                                        <v-text-field v-model="colorSearch" prepend-inner-icon="mdi-magnify"
                                            placeholder="Tìm kiếm màu sắc..." variant="outlined" density="compact"
                                            hide-details class="mb-4 bg-slate-50"></v-text-field>

                                        <div class="text-caption font-weight-bold text-grey mb-3">MÀU PHỔ BIẾN</div>
                                        <div class="d-flex flex-wrap gap-3 mb-6"
                                            style="max-height: 150px; overflow-y: auto;">
                                            <div v-for="c in filteredColors" :key="c.id"
                                                class="text-center cursor-pointer" @click="toggleColor(c.id)">
                                                <div class="color-circle mx-auto mb-1"
                                                    :style="{ backgroundColor: getColorHexFallback(c) }"
                                                    :class="{ 'selected': selectedColors.includes(c.id) }">
                                                    <v-icon v-if="selectedColors.includes(c.id)" color="white" size="18"
                                                        class="check-icon">mdi-check</v-icon>
                                                </div>
                                                <div class="text-caption"
                                                    style="font-size: 10px !important; max-width: 40px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                                                    {{ c.ten }}</div>
                                            </div>
                                        </div>

                                        <div class="text-caption font-weight-bold text-grey mb-3">THÊM NHANH MÀU SẮC
                                        </div>
                                        <div class="d-flex gap-3 mb-4">
                                            <div class="custom-color-preview rounded-lg position-relative overflow-hidden cursor-pointer"
                                                :style="{ backgroundColor: customColorHex }">
                                                <input type="color" v-model="customColorHex"
                                                    class="position-absolute w-100 h-100"
                                                    style="opacity: 0; cursor: pointer; top: 0; left: 0; outline: none; border: none;" />
                                            </div>
                                            <div class="flex-grow-1">
                                                <div class="text-caption mb-1">TÊN MÀU</div>
                                                <v-text-field v-model="customColorName" placeholder="Ví dụ: Xanh Mint"
                                                    variant="outlined" density="compact" hide-details maxlength="250"
                                                    class="mb-3 bg-slate-50"></v-text-field>
                                                <div class="text-caption mb-1">MÃ MÀU (HEX)</div>
                                                <v-text-field v-model="customColorHex" variant="outlined"
                                                    density="compact" hide-details maxlength="250"
                                                    class="bg-slate-50 mb-2"></v-text-field>
                                                <div class="d-flex align-center gap-2 px-3 py-1 bg-slate-50 rounded-lg">
                                                    <div class="color-dot" :style="{ backgroundColor: customColorHex }">
                                                    </div>
                                                    <span class="text-caption font-weight-medium">Màu đang chọn</span>
                                                </div>
                                            </div>
                                        </div>
                                        <v-btn block color="primary" class="rounded-lg font-weight-bold text-none"
                                            @click="handleAddCustomColor">
                                            <v-icon size="18" class="mr-2">mdi-plus-circle-outline</v-icon> Xác nhận
                                            thêm mới màu
                                        </v-btn>
                                    </v-card>
                                </v-menu>
                            </div>

                            <!-- KÍCH THƯỚC -->
                            <div class="field-label mb-3 mt-2">Kích thước <span class="text-error">*</span></div>
                            <div class="d-flex flex-wrap gap-2 mb-4">
                                <v-chip v-for="s in sortedSizes.filter(x => selectedSizes.includes(x.id))" :key="s.id"
                                    variant="flat" color="primary"
                                    class="rounded-lg font-weight-medium px-4 cursor-pointer" @click="toggleSize(s.id)">
                                    {{ formatSizeDisplay(s.ten) }}
                                </v-chip>

                                <v-menu v-model="showSizeMenu" :close-on-content-click="false" location="bottom center"
                                    max-width="320">
                                    <template v-slot:activator="{ props }">
                                        <v-chip v-bind="props" variant="outlined"
                                            style="border-style: dashed; border-width: 1px;" color="grey-darken-1"
                                            class="rounded-lg px-4 bg-transparent cursor-pointer hover-bg-slate-50">
                                            <v-icon start size="16">mdi-plus</v-icon> Thêm mới
                                        </v-chip>
                                    </template>
                                    <v-card class="rounded-xl pa-4 shadow-lg border">
                                        <div class="d-flex justify-space-between align-center mb-4">
                                            <span class="text-subtitle-1 font-weight-bold">Chọn kích thước</span>
                                            <v-btn icon="mdi-close" variant="text" size="small"
                                                @click="showSizeMenu = false"></v-btn>
                                        </div>
                                        <div class="d-flex gap-2 mb-4">
                                            <v-text-field :model-value="customSizeName"
                                                @update:model-value="updateCustomSizeName"
                                                @keydown="blockNonNumericSizeInput" @paste="handleSizePaste"
                                                prepend-inner-icon="mdi-magnify" placeholder="Nhập kích thước"
                                                variant="outlined" density="compact" hide-details class="bg-slate-50"
                                                maxlength="2" type="text" inputmode="numeric"
                                                pattern="[0-9]*"></v-text-field>
                                            <v-btn color="primary" class="rounded-lg text-none font-weight-medium"
                                                height="40" @click="handleAddCustomSize">
                                                <v-icon start size="18">mdi-plus</v-icon> Thêm
                                            </v-btn>
                                        </div>
                                        <div class="text-caption text-grey mb-3">Gợi ý kích thước</div>
                                        <div class="d-flex flex-wrap gap-2 mb-4"
                                            style="max-height: 150px; overflow-y: auto;">
                                            <v-chip v-for="s in filteredSizes" :key="s.id"
                                                :variant="selectedSizes.includes(s.id) ? 'flat' : 'tonal'"
                                                :color="selectedSizes.includes(s.id) ? 'primary' : 'grey-darken-1'"
                                                class="rounded-lg px-4 cursor-pointer font-weight-medium"
                                                @click="toggleSize(s.id)"
                                                :class="{ 'bg-slate-50': !selectedSizes.includes(s.id) }">
                                                {{ formatSizeDisplay(s.ten) }}
                                            </v-chip>
                                        </div>
                                        <v-btn block color="primary" class="rounded-lg font-weight-bold text-none mt-2"
                                            @click="addAllFilteredSizes">
                                            Thêm tất cả vào danh sách
                                        </v-btn>
                                    </v-card>
                                </v-menu>
                            </div>
                        </div>

                        <v-spacer></v-spacer>

                        <v-btn height="44" class="mt-6 text-none font-weight-medium rounded-lg mx-auto d-flex"
                            style="width: fit-content;" color="primary" @click="generateVariants"
                            :disabled="selectedColors.length === 0 || selectedSizes.length === 0">
                            <v-icon icon="mdi-auto-fix" size="18" class="mr-2" />
                            Tạo danh sách biến thể
                        </v-btn>
                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

        <v-row v-if="!loading" class="mt-n2">
            <v-col cols="12">
                <v-card class="premium-card">
                    <v-card-text class="pa-8">
                        <template v-if="!isEditMode">
                            <div class="variant-gradient-header pb-4 mb-6" style="border-bottom: 1px solid #e2e8f0;">
                                <div class="d-flex align-center justify-space-between w-100">
                                    <div class="d-flex align-center">
                                        <div class="icon-blob bg-blue-lighten-5 mr-3">
                                            <v-icon icon="mdi-view-grid-plus-outline" size="18" class="text-primary" />
                                        </div>
                                        <div>
                                            <div class="text-subtitle-1 text-slate-800">Danh sách biến thể</div>
                                        </div>
                                    </div>
                                    <div class="d-flex align-center gap-3">
                                        <v-btn v-if="variantItems.length > 0" color="primary" variant="flat"
                                            class="text-none font-weight-bold rounded-lg px-4"
                                            style="color: white !important;" @click="openBulkEdit(null)">
                                            <v-icon icon="mdi-flash-outline" size="18" class="mr-2" />
                                            Thêm nhanh toàn bộ
                                        </v-btn>
                                        <v-btn v-if="variantItems.length > 0" variant="flat" color="error"
                                            class="text-none font-weight-bold rounded-lg px-4"
                                            style="color: white !important;" @click="clearAllDraftVariants">
                                            <TrashIcon size="18" class="mr-2" />
                                            Xóa tất cả
                                        </v-btn>
                                    </div>
                                </div>
                            </div>

                            <div v-if="variantItems.length > 0" class="variants-tab-container mb-6">
                                <v-row no-gutters class="rounded-lg overflow-hidden"
                                    style="height: 600px; border: 1px solid #cbd5e1 !important;">
                                    <!-- Sidebar Màu sắc -->
                                    <v-col cols="12" md="3" class="bg-slate-50 d-flex flex-column h-100"
                                        style="border-right: 1px solid #cbd5e1;">
                                        <div class="pa-2 flex-shrink-0">
                                            <v-list class="bg-transparent pa-0" lines="one">
                                                <v-list-item
                                                    :class="['rounded-lg transition-all', activeColorTab === 'ALL' ? 'bg-white elevation-2 text-primary font-weight-bold' : 'text-slate-700 hover-bg-slate-100']"
                                                    @click="activeColorTab = 'ALL'">
                                                    <template v-slot:prepend>
                                                        <v-icon icon="mdi-format-list-bulleted"
                                                            :color="activeColorTab === 'ALL' ? 'primary' : 'slate-500'"
                                                            class="mr-3" />
                                                    </template>
                                                    <v-list-item-title
                                                        :class="activeColorTab === 'ALL' ? 'font-weight-bold' : 'font-weight-medium'">Tất
                                                        cả
                                                        màu</v-list-item-title>
                                                </v-list-item>
                                            </v-list>
                                            <v-divider class="mt-4 mb-2 opacity-50" color="slate-300"></v-divider>
                                        </div>

                                        <div class="flex-grow-1 px-2 pb-2" style="overflow-y: auto; min-height: 0;">
                                            <v-list class="bg-transparent pa-0" lines="one">

                                                <v-list-item v-for="(items, colorId) in variantsByColor" :key="colorId"
                                                    :class="['rounded-lg mb-2 transition-all', activeColorTab === String(colorId) ? 'bg-white elevation-2 text-primary' : 'text-slate-700 hover-bg-slate-100']"
                                                    @click="activeColorTab = String(colorId)">
                                                    <template v-slot:prepend>
                                                        <div class="mr-3"
                                                            :style="{ backgroundColor: getVariantColorHex(colorId), width: '16px', height: '16px', borderRadius: '50%', border: '1px solid #94a3b8' }">
                                                        </div>
                                                    </template>
                                                    <v-list-item-title
                                                        :class="activeColorTab === String(colorId) ? 'font-weight-bold' : 'font-weight-medium'">{{
                                                            getVariantColorLabel(colorId) }}</v-list-item-title>
                                                    <template v-slot:append>
                                                        <v-badge :content="String(items.length)"
                                                            :color="activeColorTab === String(colorId) ? 'primary' : 'slate-200'"
                                                            :text-color="activeColorTab === String(colorId) ? 'white' : 'slate-700'"
                                                            inline />
                                                    </template>
                                                </v-list-item>
                                            </v-list>
                                        </div>
                                    </v-col>

                                    <!-- Nội dung bảng biến thể -->
                                    <v-col cols="12" md="9" class="bg-white d-flex flex-column h-100">
                                        <div
                                            class="pa-4 border-b d-flex align-center justify-space-between flex-shrink-0">
                                            <div class="text-subtitle-1 font-weight-bold text-slate-800">
                                                {{ activeColorTab === 'ALL' ? 'Đang xem tất cả biến thể' :
                                                    'Đang xem màu: ' + getVariantColorLabel(activeColorTab) }}
                                            </div>
                                            <div class="d-flex align-center gap-2" v-if="activeColorTab !== 'ALL'">
                                                <span
                                                    class="text-caption font-weight-bold text-slate-500 mr-2 d-none d-sm-block">
                                                    áp dụng nhanh:</span>
                                                <FormattedNumberField v-model="quickApplyValues.giaBan"
                                                    placeholder="Giá bán" variant="outlined" density="compact"
                                                    hide-details class="custom-input-dense" style="width: 120px" />
                                                <FormattedNumberField v-model="quickApplyValues.giaNhap"
                                                    placeholder="Giá nhập" variant="outlined" density="compact"
                                                    hide-details class="custom-input-dense" style="width: 120px" />
                                                <FormattedNumberField v-model="quickApplyValues.soLuong"
                                                    placeholder="Số lượng" variant="outlined" density="compact"
                                                    hide-details class="custom-input-dense" style="width: 120px" />
                                                <v-btn color="primary" variant="flat" size="small"
                                                    class="text-none rounded font-weight-bold"
                                                    style="color: white !important" height="40"
                                                    @click="handleQuickApply">
                                                    <v-icon icon="mdi-flash" size="16" class="mr-1" /> Cập nhật
                                                </v-btn>
                                            </div>
                                        </div>

                                        <div v-if="activeColorTab !== 'ALL'"
                                            class="py-3 bg-slate-50 d-flex align-center justify-center flex-shrink-0"
                                            style="border-bottom: 2px solid #cbd5e1; box-shadow: inset 0 -4px 6px -4px rgba(0,0,0,0.05); z-index: 10;">
                                            <div class="d-flex flex-column align-center">
                                                <div class="text-caption font-weight-bold text-slate-600 mb-2">HÌNH ẢNH
                                                    ĐẠI DIỆN CHO MÀU NÀY</div>
                                                <div class="color-image-uploader elevation-1"
                                                    @click="openColorImagePicker(activeColorTab)"
                                                    style="width: 70px; height: 70px; border-radius: 8px; border: 2px dashed #cbd5e1; cursor: pointer; overflow: hidden; background: white">
                                                    <v-img v-if="getColorUploadEntry(activeColorTab).url"
                                                        :src="getColorUploadEntry(activeColorTab).url" cover
                                                        class="w-100 h-100" />
                                                    <div v-else class="w-100 h-100 d-flex align-center justify-center">
                                                        <v-icon icon="mdi-camera-plus" color="slate-400" size="24" />
                                                    </div>
                                                    <input :ref="(el) => setColorFileInputRef(activeColorTab, el)"
                                                        type="file" accept="image/*" class="d-none"
                                                        @change="handleColorImageUpload(activeColorTab, $event)" />
                                                </div>
                                            </div>
                                        </div>

                                        <div class="flex-grow-1" style="min-height: 0;">
                                            <v-table class="variant-inner-table h-100" fixed-header>
                                                <thead class="bg-white">
                                                    <tr>
                                                        <th class="text-left font-weight-bold text-slate-800 text-caption"
                                                            style="border-bottom: 1px solid #cbd5e1 !important;">Thuộc
                                                            tính
                                                            (Màu/Size)</th>
                                                        <th class="text-left font-weight-bold text-slate-800 text-caption"
                                                            style="width: 140px; border-bottom: 1px solid #cbd5e1 !important;">
                                                            Giá bán (đ)</th>
                                                        <th class="text-left font-weight-bold text-slate-800 text-caption"
                                                            style="width: 140px; border-bottom: 1px solid #cbd5e1 !important;">
                                                            Giá nhập (đ)</th>
                                                        <th class="text-left font-weight-bold text-slate-800 text-caption"
                                                            style="width: 110px; border-bottom: 1px solid #cbd5e1 !important;">
                                                            Số lượng sản phẩm
                                                        </th>
                                                        <th class="text-center font-weight-bold text-slate-800 text-caption"
                                                            style="width: 60px; border-bottom: 1px solid #cbd5e1 !important;">
                                                            Hành động</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <tr v-for="variant in paginatedVisibleVariantItems"
                                                        :key="variant.clientKey || variant.idMauSac + '-' + variant.idKichThuoc">
                                                        <td class="font-weight-medium text-slate-700">
                                                            <div class="d-flex align-center">
                                                                <div class="mr-2"
                                                                    :style="{ backgroundColor: getVariantColorHex(variant.idMauSac), width: '14px', height: '14px', borderRadius: '50%', border: '1px solid #94a3b8' }">
                                                                </div>
                                                                <div>
                                                                    <div class="text-body-2 font-weight-bold">{{
                                                                        getVariantSizeLabel(variant.idKichThuoc) }}
                                                                    </div>
                                                                    <div class="text-caption text-slate-500">{{
                                                                        getVariantColorLabel(variant.idMauSac)
                                                                        }}</div>
                                                                </div>
                                                            </div>
                                                        </td>
                                                        <td>
                                                            <FormattedNumberField v-model="variant.giaBan" hide-details
                                                                variant="outlined" density="compact"
                                                                class="custom-input-dense" />
                                                        </td>
                                                        <td>
                                                            <FormattedNumberField v-model="variant.giaNhap" hide-details
                                                                variant="outlined" density="compact"
                                                                class="custom-input-dense" />
                                                        </td>
                                                        <td>
                                                            <FormattedNumberField v-model="variant.soLuong" hide-details
                                                                variant="outlined" density="compact"
                                                                class="custom-input-dense" />
                                                        </td>
                                                        <td class="text-center">
                                                            <v-btn variant="text" color="error" size="small"
                                                                class="action-icon-btn"
                                                                @click="removeDraftVariantByObject(variant)">
                                                                <TrashIcon size="18" />
                                                            </v-btn>
                                                        </td>
                                                    </tr>
                                                </tbody>
                                            </v-table>
                                        </div>
                                        <div class="flex-shrink-0 bg-white py-3 px-4"
                                            style="border-top: 2px solid #cbd5e1 !important;">
                                            <AdminPagination v-model="createVariantPage"
                                                :page-size="createVariantPageSize"
                                                @update:pageSize="createVariantPageSize = $event"
                                                :total-pages="createVariantTotalPages"
                                                :total-elements="visibleVariantItems.length"
                                                :current-size="paginatedVisibleVariantItems.length" />
                                        </div>
                                    </v-col>
                                </v-row>
                            </div>

                            <div v-else class="variant-empty-state">
                                <div class="variant-empty-state__icon">
                                    <v-icon icon="mdi-layers-outline" size="32" />
                                </div>
                                <div class="text-subtitle-2 font-weight-bold text-slate-700 mt-3">
                                    Chưa có biến thể nào được tạo
                                </div>
                                <div class="text-caption text-slate-500 mt-1">
                                    Chọn màu sắc và kích thước ở card bên phải, sau đó bấm <span
                                        class="font-weight-bold">Tạo danh sách biến
                                        thể</span>.
                                </div>
                            </div>
                        </template>

                        <div v-if="variantItems.length > 0 && isEditMode" class="variant-filter-container mt-6">
                            <AdminFilter title="Bộ lọc nâng cao" @refresh="resetVariantTableFilters" :loading="loading">
                                <v-col cols="12" sm="2">
                                    <div class="variant-filter-label">Sản phẩm</div>
                                    <v-text-field :model-value="variantFilterProductLabel" variant="outlined"
                                        density="compact" hide-details readonly
                                        class="variant-filter-input bg-slate-50" />
                                </v-col>
                                <v-col cols="12" sm="2">
                                    <div class="variant-filter-label">Tìm kiếm nhanh</div>
                                    <v-text-field v-model="variantTableFilters.keyword"
                                        placeholder="Mã SKU, màu, size..." prepend-inner-icon="mdi-magnify"
                                        variant="outlined" density="compact" hide-details clearable
                                        class="variant-filter-input" />
                                </v-col>
                                <v-col cols="12" sm="2">
                                    <div class="variant-filter-label">Màu sắc</div>
                                    <v-select v-model="variantTableFilters.mauSacId"
                                        :items="[{ title: 'Tất cả màu', value: '' }, ...colors.map((item) => ({ title: item.ten, value: item.id }))]"
                                        variant="outlined" density="compact" hide-details
                                        class="variant-filter-input" />
                                </v-col>
                                <v-col cols="12" sm="2">
                                    <div class="variant-filter-label">Kích thước</div>
                                    <v-select v-model="variantTableFilters.kichThuocId"
                                        :items="[{ title: 'Tất cả size', value: '' }, ...sizes.map((item) => ({ title: item.ten, value: item.id }))]"
                                        variant="outlined" density="compact" hide-details
                                        class="variant-filter-input" />
                                </v-col>
                                <v-col cols="12" sm="2">
                                    <div class="variant-filter-label">Trạng thái</div>
                                    <v-select v-model="variantTableFilters.trangThai" :items="[
                                        { title: 'Tất cả trạng thái', value: '' },
                                        { title: 'Đang hoạt động', value: defaultVariantStatus },
                                        { title: 'Ngừng hoạt động', value: 'NGUNG_HOAT_DONG' }
                                    ]" variant="outlined" density="compact" hide-details
                                        class="variant-filter-input" />
                                </v-col>

                                <template #after>
                                    <v-col cols="12" class="mt-4 pa-0">
                                        <div class="d-flex align-center justify-space-between mb-2">
                                            <div class="d-flex align-center gap-2">
                                                <v-icon size="15" color="#3b82f6">mdi-cash-multiple</v-icon>
                                                <span class="text-caption font-weight-bold text-slate-600">Khoảng
                                                    giá</span>
                                            </div>
                                            <span class="text-primary font-weight-bold">{{
                                                formatCurrency(variantTableFilters.khoangGia[0]) }} – {{
                                                    formatCurrency(variantTableFilters.khoangGia[1]) }}</span>
                                        </div>
                                        <v-range-slider :key="`${variantPriceBounds.min}-${variantPriceBounds.max}`"
                                            v-model="variantTableFilters.khoangGia" :min="variantPriceBounds.min"
                                            :max="variantPriceBounds.max" :step="variantPriceStep" hide-details
                                            color="primary" track-color="#e2e8f0" track-size="2" thumb-size="14"
                                            class="blue-range-slider"
                                            @update:model-value="handleVariantSliderPriceChange" />
                                    </v-col>
                                </template>
                            </AdminFilter>
                        </div>
                        <AdminTable v-if="variantItems.length > 0 && isEditMode" title="Danh mục biến thể"
                            :headers="variantTableHeaders" :items="paginatedVariantItems" :loading="loading"
                            :show-add-button="false" class="mt-6 variant-admin-table">
                            <template #headers>
                                <tr>
                                    <th class="header-cell" style="width: 70px;">
                                        <v-checkbox-btn :model-value="allVisibleVariantsSelected"
                                            :indeterminate="someVisibleVariantsSelected" color="primary" hide-details
                                            density="compact" @update:model-value="toggleSelectVisibleVariants" />
                                    </th>
                                    <th class="header-cell" style="width: 60px;">STT</th>
                                    <th class="header-cell" style="width: 80px;">Ảnh</th>
                                    <th class="header-cell" style="width: 140px;">Màu sắc</th>
                                    <th class="header-cell" style="width: 140px;">Kích thước</th>
                                    <th class="header-cell" style="width: 240px;">Mã SKU</th>
                                    <th class="header-cell" style="width: 110px;">Tồn kho</th>
                                    <th class="header-cell" style="width: 130px;">Giá bán</th>
                                    <th class="header-cell" style="width: 160px;">Trạng thái</th>
                                    <th class="header-cell" style="width: 120px;">Thao tác</th>
                                </tr>
                            </template>

                            <template #top>
                                <div
                                    class="px-6 py-3 bg-slate-50 border-b d-flex align-center justify-space-between flex-wrap gap-3">
                                    <div class="d-flex align-center flex-wrap gap-2">
                                        <span class="text-caption font-weight-medium text-slate-500">
                                            Đã chọn {{ selectedVariantKeys.length }} biến thể
                                        </span>
                                    </div>

                                    <div class="d-flex align-center flex-wrap gap-2">
                                        <v-btn v-if="selectedVariants.length > 0" size="small" variant="tonal"
                                            color="primary" class="text-none font-weight-bold"
                                            @click="handleExportVariantQrZip">
                                            Xuất ZIP QR
                                            <v-tooltip activator="parent" location="top"
                                                text="Xuất ảnh QR của các biến thể đã chọn" />
                                        </v-btn>
                                    </div>
                                </div>
                            </template>

                            <template #row="{ item: variant, index }">
                                <tr class="data-row">
                                    <td class="data-cell">
                                        <v-checkbox-btn
                                            :model-value="selectedVariantKeys.includes(getVariantKey(variant))"
                                            color="primary" hide-details density="compact"
                                            @update:model-value="toggleVariantSelection(getVariantKey(variant), $event)" />
                                    </td>
                                    <td class="data-cell text-center text-slate-500 font-weight-medium">
                                        {{ (variantPage - 1) * variantPageSize + index + 1 }}
                                    </td>
                                    <td class="data-cell">
                                        <v-avatar rounded="lg" size="44"
                                            class="border bg-slate-50 shadow-sm avatar-hover">
                                            <SafeProductImage :src="getVariantThumbnail(variant)"
                                                :fallback-src="logoPlaceholder" :alt="getVariantSkuLabel(variant)" />
                                        </v-avatar>
                                    </td>
                                    <td class="data-cell font-weight-bold text-slate-700">
                                        <div class="text-truncate" :title="getOptionLabel(colors, variant.idMauSac)">
                                            {{ getOptionLabel(colors, variant.idMauSac) }}
                                        </div>
                                    </td>
                                    <td class="data-cell font-weight-bold text-slate-700">
                                        <div class="text-truncate" :title="getOptionLabel(sizes, variant.idKichThuoc)">
                                            {{ getOptionLabel(sizes, variant.idKichThuoc) }}
                                        </div>
                                    </td>
                                    <td class="data-cell text-slate-600">
                                        <div class="text-truncate" :title="getVariantSkuLabel(variant)">
                                            {{ getVariantSkuLabel(variant) }}
                                        </div>
                                    </td>
                                    <td class="data-cell">
                                        <span class="text-primary">{{ formatNumber(variant.soLuong) }}</span>
                                    </td>
                                    <td class="data-cell">
                                        <span class="text-primary">{{ formatCurrency(variant.giaBan) }}</span>
                                    </td>
                                    <td class="data-cell text-center">
                                        <v-chip variant="flat"
                                            :class="['status-chip', isActiveStatus(variant.trangThai) ? 'status-chip-active' : 'status-chip-inactive']">
                                            {{ getStatusLabel(variant.trangThai) }}
                                        </v-chip>
                                    </td>
                                    <td class="data-cell text-center">
                                        <div class="d-flex justify-center align-center variant-actions">
                                            <v-btn variant="text" class="action-icon-btn" color="primary"
                                                @click="openEditVariantModal(variant)">
                                                <PencilIcon size="18" />
                                                <v-tooltip activator="parent" location="top"
                                                    text="Chỉnh sửa biến thể" />
                                            </v-btn>
                                            <div class="switch-wrapper">
                                                <v-switch :model-value="isActiveStatus(variant.trangThai)"
                                                    color="primary" hide-details density="compact"
                                                    class="tight-switch action-switch"
                                                    @click.prevent.stop="handleToggleVariantStatus(variant)" />
                                                <v-tooltip activator="parent" location="top"
                                                    text="Chuyển đổi trạng thái" />
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                            </template>

                            <template #pagination>
                                <AdminPagination v-model="variantPage" :page-size="variantPageSize"
                                    @update:pageSize="variantPageSize = $event" :total-pages="totalVariantPages"
                                    :total-elements="totalVariantElements"
                                    :current-size="paginatedVariantItems.length" />
                            </template>
                        </AdminTable>

                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />

        <VariantFormModal
            :key="`${variantModal.mode}-${variantModal.variant?.id || variantModal.variant?.clientKey || 'new'}`"
            :open="variantModal.open" :mode="variantModal.mode" :variant="variantModal.variant"
            :options="variantOptions" :submitting="variantModal.submitting" :productCode="product.maSanPham"
            :lock-attributes-on-edit="isEditMode" :allow-image-upload="true" @close="closeVariantModal"
            @submit="handleVariantSubmit" @options-refreshed="fetchFormOptions" />

        <!-- Modal Thiết lập nhanh hàng loạt -->
        <v-dialog v-model="bulkEditModal.show" max-width="500">
            <v-card class="premium-card rounded-xl">
                <v-card-title class="pa-6 border-b d-flex align-center">
                    <v-icon icon="mdi-flash-circle" color="primary" class="mr-3" />
                    <span class="font-weight-bold text-slate-800">
                        {{ bulkEditModal.targetColorId ? `Thiết lập cho màu
                        ${getVariantColorLabel(bulkEditModal.targetColorId)}` : 'Thiết lập cho tất cả biến thể' }}
                    </span>
                </v-card-title>
                <v-card-text class="pa-8">

                    <v-row>
                        <v-col cols="12">
                            <div class="field-label">Số lượng <span class="text-error">*</span></div>
                            <FormattedNumberField v-model="bulkEditModal.form.soLuong" placeholder="Nhập số lượng..."
                                variant="outlined" density="comfortable" hide-details class="custom-input" />
                        </v-col>
                        <v-col cols="12">
                            <div class="field-label">Giá nhập <span class="text-error">*</span></div>
                            <FormattedNumberField v-model="bulkEditModal.form.giaNhap" placeholder="Nhập giá nhập..."
                                variant="outlined" density="comfortable" hide-details class="custom-input" />
                        </v-col>
                        <v-col cols="12">
                            <div class="field-label">Giá bán (VNĐ) <span class="text-error">*</span></div>
                            <FormattedNumberField v-model="bulkEditModal.form.giaBan" placeholder="Nhập giá bán..."
                                variant="outlined" density="comfortable" hide-details class="custom-input" />
                        </v-col>
                    </v-row>
                </v-card-text>
                <v-card-actions class="pa-6 border-t bg-slate-50">
                    <v-spacer />
                    <v-btn variant="text" color="slate-500" class="text-none font-weight-bold"
                        @click="bulkEditModal.show = false">Hủy</v-btn>
                    <v-btn color="primary" variant="flat" class="text-none font-weight-bold px-6 rounded-lg h-11"
                        @click="applyBulkEdit">Áp dụng</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <div ref="qrExportContainer" class="qr-export-staging" aria-hidden="true">
            <div v-for="item in qrExportItems" :key="item.id" class="qr-export-item">
                <QrcodeVue :value="item.value" :size="120" level="H" render-as="canvas" />
            </div>
        </div>

        <v-dialog v-model="duplicateAttributeDialog.show" max-width="500">
            <v-card class="rounded-xl elevation-10">
                <v-card-title class="d-flex align-center bg-red-lighten-5 text-red-darken-3 pa-4 border-b">
                    <v-icon size="28" class="mr-3">mdi-alert-circle</v-icon>
                    <span class="text-h6 font-weight-bold">Trùng lặp thuộc tính</span>
                </v-card-title>
                <v-card-text class="pa-5 pt-6 text-body-1 text-slate-700 leading-relaxed">
                    Đã tồn tại một sản phẩm khác có chính xác <span class="font-weight-bold text-red">cùng các thuộc
                        tính</span>
                    (Thương hiệu, Danh mục, Xuất xứ, Mục đích chạy, Cổ giày, Chất liệu, Đế giày).<br><br>
                    Tên sản phẩm trùng: <span class="font-weight-bold">[{{
                        duplicateAttributeDialog.duplicateProduct?.ten
                    }}]</span><br><br>
                    Để phân biệt, vui lòng <span class="font-weight-bold text-primary">đặt tên sản phẩm hiện tại (tên
                        đệm) khác
                        đi</span> so với sản phẩm đã tồn tại, hoặc chỉnh sửa các thuộc tính để tránh trùng lặp hoàn
                    toàn.
                </v-card-text>
                <v-card-actions class="pa-4 bg-slate-50 border-t justify-end">
                    <v-btn color="primary" variant="flat" class="text-none px-6 font-weight-bold"
                        @click="duplicateAttributeDialog.show = false">
                        Đã hiểu
                    </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
    </v-container>
</template>

<style scoped lang="scss">
@import '@/scss/pages/admin/_san-pham-form.scss';
</style>
