<script setup>
import { ref, onMounted, computed, watch, reactive, nextTick } from 'vue';
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
    dichVuThuongHieu, dichVuDanhMuc, dichVuXuatXu,
    dichVuChatLieu, dichVuDeGiay, dichVuCoGiay,
    dichVuMucDichChay, dichVuMauSac, dichVuKichThuoc
} from '@/services/product/dichVuThuocTinh';
import logoPlaceholder from '@/assets/images/logos/logo-light.svg';
import { exportQrImageZip } from '@/utils/qrExcelWorkbook';
import { isActiveStatus, getStatusLabel } from '@/utils/statusUtils';
import { generateRandomCode } from '@/utils/codeGenerator';

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

const isEditMode = computed(() => !!route.params.id);
const submitButtonText = computed(() => isEditMode.value ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm');
const defaultVariantStatus = 'DANG_HOAT_DONG';

// DATA OPTIONS
const brands = ref([]);
const categories = ref([]);
const materials = ref([]);
const soles = ref([]);
const collars = ref([]);
const origins = ref([]);
const purposes = ref([]);
const colors = ref([]);
const sizes = ref([]);

// Tìm kiếm nhanh cho Chip Group
const colorSearch = ref('');
const sizeSearch = ref('');

// UI Refs cho Biến thể
const showColorMenu = ref(false);
const showSizeMenu = ref(false);
const customColorName = ref('');
const customColorHex = ref('#FF5733');
const customSizeName = ref('');

// Bảng màu phổ biến (Việt - Anh) mapping sang HEX
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

watch(customColorName, (newName) => {
    if (!newName) return;
    const normalizedName = newName.trim().toLowerCase();

    // Hàm bỏ dấu tiếng việt
    const removeAccents = (str) => str.normalize('NFD').replace(/[\u0300-\u036f]/g, '');
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

const cleanSizeString = (str) => {
    let s = (str || '').toString();
    // 5. Bảo mật: Loại bỏ HTML tags để chống XSS
    s = s.replace(/<[^>]*>?/gm, '');

    // 3. Định dạng: Chỉ giữ lại chữ cái, số, khoảng trắng và dấu .,- (Loại bỏ @, #, $, %...)
    s = s.replace(/[^a-zA-Z0-9.,\-\s]/g, '');

    // 1. Chuẩn hóa: Loại bỏ các từ khóa thừa "Kích thước", "size", "sz"
    s = s.replace(/(?:^|\s)(?:kích thước|size|sz)\s*/gi, '');

    // Loại bỏ chữ "s" nếu nó đứng ngay trước một con số (để biến s43, s 43 thành 43)
    s = s.replace(/(?:^|\s)s\s*(?=\d)/gi, '');

    // 1. Loại bỏ khoảng trắng thừa ở giữa và 2 đầu
    return s.replace(/\s+/g, ' ').trim();
};

const formatSizeDisplay = (name) => {
    const norm = cleanSizeString(name).toUpperCase();
    return norm ? `Size ${norm}` : (name || '');
};

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

    // 2. Range > 0 và < 99
    if (sizeNumber <= 0 || sizeNumber >= 99) {
        addNotification({ title: 'Lỗi', subtitle: 'Kích thước phải lớn hơn 0 và nhỏ hơn 99', color: 'error' });
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
    idDanhMuc: '',
    idChatLieu: '',
    idDeGiay: '',
    idCoGiay: '',
    idMucDichChay: ''
});

// Hàm tạo danh sách hiển thị với mục thêm nhanh ở đầu
const getDisplayItems = (originalItems, query) => {
    const trimmedQuery = query?.trim();
    const normalizedQuery = normalizeSearchText(trimmedQuery);

    // Lọc danh sách gốc trước
    let filtered = originalItems;
    if (normalizedQuery) {
        filtered = originalItems.filter(item =>
            normalizeSearchText(item.ten).includes(normalizedQuery)
        );
    }

    if (!trimmedQuery) return filtered;

    // Kiểm tra xem đã có item nào trùng hoàn toàn chưa
    const existsExact = originalItems.some(item =>
        normalizeSearchText(item.ten) === normalizedQuery
    );

    if (existsExact) return filtered;

    // Chèn mục mới vào đầu danh sách đã lọc với ID đặc biệt để nhận biết click trực tiếp
    return [
        { id: `__new__${trimmedQuery}`, ten: trimmedQuery, isNew: true },
        ...filtered
    ];
};

const displayBrands = computed(() => getDisplayItems(brands.value, searchQueries.idThuongHieu, 'idThuongHieu'));
const displayOrigins = computed(() => getDisplayItems(origins.value, searchQueries.idXuatXu, 'idXuatXu'));
const displayCategories = computed(() => getDisplayItems(categories.value, searchQueries.idDanhMuc, 'idDanhMuc'));
const displayMaterials = computed(() => getDisplayItems(materials.value, searchQueries.idChatLieu, 'idChatLieu'));
const displaySoles = computed(() => getDisplayItems(soles.value, searchQueries.idDeGiay, 'idDeGiay'));
const displayCollars = computed(() => getDisplayItems(collars.value, searchQueries.idCoGiay, 'idCoGiay'));
const displayPurposes = computed(() => getDisplayItems(purposes.value, searchQueries.idMucDichChay, 'idMucDichChay'));

const selectedColors = ref([]);
const selectedSizes = ref([]);
const variantItems = ref([]);
const colorImageState = ref({});
const colorFileInputRefs = ref({});
const bulkAllForm = ref({
    soLuong: '',
    giaNhap: '',
    giaBan: ''
});
const bulkByColorForms = ref({});
const variantModal = ref({
    open: false,
    mode: 'create',
    submitting: false,
    variant: null
});
const selectedVariantKeys = ref([]);
const qrExportItems = ref([]);
const qrExportContainer = ref(null);
const variantTableFilters = reactive({
    keyword: '',
    mauSacId: '',
    kichThuocId: '',
    trangThai: '',
    khoangGia: [MIN_VARIANT_PRICE, DEFAULT_MAX_VARIANT_PRICE]
});
const variantPriceBounds = ref({
    min: MIN_VARIANT_PRICE,
    max: DEFAULT_MAX_VARIANT_PRICE
});

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
const variantPage = ref(1);
const variantPageSize = ref(5);
const variantFilterProductLabel = computed(() => (
    [product.value.maSanPham, product.value.tenSanPham].filter(Boolean).join(' • ') || 'Sản phẩm hiện tại'
));
const variantPriceStep = computed(() => (
    variantPriceBounds.value.max > 1000000 ? 50000 : 1000
));
const filteredVariantItems = computed(() => variantItems.value.filter((item) => {
    const keyword = variantTableFilters.keyword.trim().toLowerCase();
    const colorLabel = getVariantColorLabel(item.idMauSac).toLowerCase();
    const sizeLabel = getVariantSizeLabel(item.idKichThuoc).toLowerCase();
    const matchesKeyword = !keyword
        || String(item.maChiTietSanPham || '').toLowerCase().includes(keyword)
        || colorLabel.includes(keyword)
        || sizeLabel.includes(keyword);
    const variantPrice = Number(item.giaBan ?? 0);
    const matchesPrice = variantPrice >= variantTableFilters.khoangGia[0]
        && variantPrice <= variantTableFilters.khoangGia[1];

    return matchesKeyword
        && (!variantTableFilters.mauSacId || item.idMauSac === variantTableFilters.mauSacId)
        && (!variantTableFilters.kichThuocId || item.idKichThuoc === variantTableFilters.kichThuocId)
        && (!variantTableFilters.trangThai || item.trangThai === variantTableFilters.trangThai)
        && matchesPrice;
}));
const totalVariantElements = computed(() => filteredVariantItems.value.length);
const totalVariantPages = computed(() => Math.max(Math.ceil(totalVariantElements.value / variantPageSize.value), 1));
const paginatedVariantItems = computed(() => {
    const start = (variantPage.value - 1) * variantPageSize.value;
    return filteredVariantItems.value.slice(start, start + variantPageSize.value);
});
const visibleVariantKeys = computed(() => paginatedVariantItems.value.map((item) => getVariantKey(item)));
const selectedVariants = computed(() => filteredVariantItems.value.filter((item) => selectedVariantKeys.value.includes(getVariantKey(item))));
const allVisibleVariantsSelected = computed(() => visibleVariantKeys.value.length > 0
    && visibleVariantKeys.value.every((key) => selectedVariantKeys.value.includes(key)));
const someVisibleVariantsSelected = computed(() => visibleVariantKeys.value.some((key) => selectedVariantKeys.value.includes(key))
    && !allVisibleVariantsSelected.value);

const totalVariantStock = computed(() => variantItems.value.reduce(
    (sum, item) => sum + Number(item.soLuong || 0),
    0
));

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

const getVariantColorHex = (colorId) => {
    const color = colors.value.find(c => c.id === colorId);
    return color?.maMauHex || '#ffffff';
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

const createDraftKey = () => `draft-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

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

const normalizeUploadedFileUrl = (value) => {
    if (!value) return '';
    if (typeof value === 'string') return value;
    return value.fileUrl
        || value.url
        || value.secure_url
        || value.duongDanAnh
        || value.duongDan
        || value.path
        || value.data
        || value.hinhAnh
        || value.anh
        || '';
};

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
const getNestedValue = (source, keys) => {
    for (const key of keys) {
        const value = source?.[key];
        if (value !== null && value !== undefined && value !== '') {
            return value;
        }
    }
    return '';
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

const updateVariantPriceBounds = () => {
    const maxPrice = variantItems.value.reduce((maxValue, item) => Math.max(maxValue, Number(item.giaBan ?? 0)), MIN_VARIANT_PRICE);
    const safeMax = Math.max(maxPrice, variantPriceStep.value);
    variantPriceBounds.value = {
        min: MIN_VARIANT_PRICE,
        max: safeMax
    };

    variantTableFilters.khoangGia = [
        Math.min(variantTableFilters.khoangGia[0], safeMax),
        Math.min(variantTableFilters.khoangGia[1], safeMax)
    ];

    if (variantTableFilters.khoangGia[0] > variantTableFilters.khoangGia[1]) {
        variantTableFilters.khoangGia = [MIN_VARIANT_PRICE, safeMax];
    }
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

const sanitizeVariantPriceRange = (range, maxPrice = variantPriceBounds.value.max) => {
    const safeMaxPrice = Math.max(MIN_VARIANT_PRICE, Number(maxPrice || DEFAULT_MAX_VARIANT_PRICE));
    const rawMin = Math.max(MIN_VARIANT_PRICE, Number(range?.[0] ?? MIN_VARIANT_PRICE));
    const rawMax = Math.max(MIN_VARIANT_PRICE, Number(range?.[1] ?? safeMaxPrice));
    const nextMin = Math.min(rawMin, safeMaxPrice);
    const nextMax = Math.min(Math.max(rawMax, nextMin), safeMaxPrice);
    return [nextMin, nextMax];
};

const updateVariantPriceBoundary = (boundary, value) => {
    const nextRange = [...variantTableFilters.khoangGia];
    if (boundary === 'min') {
        nextRange[0] = value === '' ? MIN_VARIANT_PRICE : value;
    } else {
        nextRange[1] = value === '' ? variantPriceBounds.value.max : value;
    }

    variantTableFilters.khoangGia = sanitizeVariantPriceRange(nextRange);
};

const handleVariantSliderPriceChange = (value) => {
    variantTableFilters.khoangGia = sanitizeVariantPriceRange(value);
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

const resetVariantTableFilters = () => {
    variantTableFilters.keyword = '';
    variantTableFilters.mauSacId = '';
    variantTableFilters.kichThuocId = '';
    variantTableFilters.trangThai = '';
    variantTableFilters.khoangGia = [MIN_VARIANT_PRICE, variantPriceBounds.value.max];
    variantPage.value = 1;
    clearVariantSelection();
};

const clearVariantSelection = () => {
    selectedVariantKeys.value = [];
};

const syncVariantSelection = () => {
    const validKeys = new Set(filteredVariantItems.value.map((item) => getVariantKey(item)));
    selectedVariantKeys.value = selectedVariantKeys.value.filter((key) => validKeys.has(key));
};

const toggleVariantSelection = (variantKey, checked) => {
    if (checked) {
        if (!selectedVariantKeys.value.includes(variantKey)) {
            selectedVariantKeys.value = [...selectedVariantKeys.value, variantKey];
        }
        return;
    }

    selectedVariantKeys.value = selectedVariantKeys.value.filter((key) => key !== variantKey);
};

const toggleSelectVisibleVariants = (checked) => {
    if (checked) {
        const mergedKeys = new Set([...selectedVariantKeys.value, ...visibleVariantKeys.value]);
        selectedVariantKeys.value = Array.from(mergedKeys);
        return;
    }

    const visibleKeySet = new Set(visibleVariantKeys.value);
    selectedVariantKeys.value = selectedVariantKeys.value.filter((key) => !visibleKeySet.has(key));
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
        tenSanPham: product.value.tenSanPham,
        idThuongHieu: product.value.idThuongHieu,
        idDanhMuc: product.value.idDanhMuc,
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
    item.idMauSac === payload.idMauSac
    && item.idKichThuoc === payload.idKichThuoc
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
        const opts = await dichVuSanPham.layOptionsForm().catch(() => null);
        if (opts) {
            brands.value = opts.thuongHieus || [];
            categories.value = opts.danhMucs || [];
            origins.value = opts.xuatXus || [];
            purposes.value = opts.mucDichChays || [];
            collars.value = opts.coGiays || [];
            materials.value = opts.chatLieus || [];
            soles.value = opts.deGiays || [];
            colors.value = opts.mauSacs || [];
            sizes.value = opts.kichThuocs || [];
            return;
        }

        const [b, c, o, p, col, m, s, mau, size] = await Promise.all([
            dichVuThuongHieu.layThuongHieu({ size: 1000 }),
            dichVuDanhMuc.layDanhMuc({ size: 1000 }),
            dichVuXuatXu.layXuatXu({ size: 1000 }),
            dichVuMucDichChay.layMucDichChay({ size: 1000 }),
            dichVuCoGiay.layCoGiay({ size: 1000 }),
            dichVuChatLieu.layChatLieu({ size: 1000 }),
            dichVuDeGiay.layDeGiay({ size: 1000 }),
            dichVuMauSac.layMauSac({ size: 1000 }),
            dichVuKichThuoc.layKichThuoc({ size: 1000 })
        ]);

        brands.value = b.content || b || [];
        categories.value = c.content || c || [];
        origins.value = o.content || o || [];
        purposes.value = p.content || p || [];
        collars.value = col.content || col || [];
        materials.value = m.content || m || [];
        soles.value = s.content || s || [];
        colors.value = mau.content || mau || [];
        sizes.value = size.content || size || [];
    } catch (error) {
        console.error('Lỗi khi tải options:', error);
        addNotification({ title: 'Cảnh báo', subtitle: 'Không thể tải đầy đủ thuộc tính', color: 'warning' });
    }
};

// FORM STATE
const product = ref({
    maSanPham: '',
    tenSanPham: '',
    moTa: '',
    idThuongHieu: null,
    idDanhMuc: null,
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

const loadProduct = async (id) => {
    const data = await dichVuSanPham.layChiTietSanPham(id);
    product.value = {
        maSanPham: data.maSanPham || data.ma || '',
        tenSanPham: data.tenSanPham || '',
        moTa: data.moTa || '',
        idThuongHieu: data.idThuongHieu || null,
        idDanhMuc: data.idDanhMuc || null,
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
    variantItems.value = (data.variants || []).map(mapVariantToFormState);
    syncColorImageStateFromVariants();
};

watch(variantItems, () => {
    updateVariantPriceBounds();
    syncVariantSelection();
    if (variantPage.value > totalVariantPages.value) {
        variantPage.value = totalVariantPages.value;
    }
}, { deep: true });

watch(variantPageSize, () => {
    variantPage.value = 1;
});

watch(
    () => [
        variantTableFilters.keyword,
        variantTableFilters.mauSacId,
        variantTableFilters.kichThuocId,
        variantTableFilters.trangThai,
        variantTableFilters.khoangGia[0],
        variantTableFilters.khoangGia[1]
    ],
    () => {
        variantPage.value = 1;
        syncVariantSelection();
    }
);

onMounted(async () => {
    loading.value = true;
    try {
        await fetchFormOptions();

        if (route.params.id) {
            await loadProduct(route.params.id);
        }
    } catch (error) {
        console.error('Error initializing form:', error);
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải dữ liệu khởi tạo', color: 'error' });
    } finally {
        loading.value = false;
    }
});

const rules = {
    required: value => !!value || 'Trường này là bắt buộc'
};

const attributeConfig = [
    { field: 'idThuongHieu', service: dichVuThuongHieu, type: 'THUONG_HIEU', label: 'thương hiệu', options: brands },
    { field: 'idDanhMuc', service: dichVuDanhMuc, type: 'DANH_MUC', label: 'danh mục', options: categories },
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
        ['idDanhMuc', 'Danh mục'],
        ['idXuatXu', 'Xuất xứ'],
        ['idChatLieu', 'Chất liệu'],
        ['idDeGiay', 'Loại đế giày'],
        ['idCoGiay', 'Loại cổ giày'],
        ['idMucDichChay', 'Mục đích sử dụng']
    ];

    const missing = requiredFields
        .filter(([field]) => !product.value[field])
        .map(([, label]) => label);

    if (missing.length > 0) {
        addNotification({
            title: 'Lỗi',
            subtitle: `Vui lòng điền đủ thông tin bắt buộc: ${missing.join(', ')}`,
            color: 'error'
        });
        return false;
    }

    if (!isEditMode.value && variantItems.value.length === 0) {
        addNotification({
            title: 'Thiếu biến thể',
            subtitle: 'Vui lòng tạo ít nhất 1 biến thể trước khi lưu sản phẩm',
            color: 'error'
        });
        return false;
    }

    const invalidVariant = variantItems.value.find(item => (
        !item.idMauSac
        || !item.idKichThuoc
        || item.soLuong < 0
        || item.giaNhap < 0
        || item.giaBan < 0
    ));

    if (invalidVariant) {
        addNotification({
            title: 'Lỗi',
            subtitle: 'Danh sách biến thể có dữ liệu chưa hợp lệ. Vui lòng kiểm tra lại màu sắc, kích thước, tồn kho và giá.',
            color: 'error'
        });
        return false;
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

    confirmDialog.value = {
        show: true,
        title: creatingNew ? 'Xác nhận thêm mới' : 'Xác nhận cập nhật',
        message: creatingNew
            ? (variantCount > 0
                ? `Bạn có chắc chắn muốn thêm sản phẩm mới cùng ${variantCount} biến thể không?`
                : 'Bạn có chắc chắn muốn thêm sản phẩm mới này không?')
            : 'Bạn có chắc chắn muốn cập nhật thông tin sản phẩm này không?',
        color: 'success',
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
                addNotification({ title: 'Lỗi', subtitle: 'Không thể lưu sản phẩm', color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
                saving.value = false;
            }
        }
    };
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

                            <v-col cols="12" md="4">
                                <div class="field-label">Tên sản phẩm</div>
                                <v-text-field v-model="product.tenSanPham" placeholder="Ví dụ: Giày Nike Air..."
                                    :rules="[rules.required]" variant="outlined" density="comfortable"
                                    hide-details="auto" maxlength="250"></v-text-field>
                            </v-col>
                            <v-col cols="12" md="4">
                                <div class="field-label">Thương hiệu</div>
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
                            <v-col cols="12" md="4">
                                <div class="field-label">Danh mục</div>
                                <v-combobox v-model="product.idDanhMuc" v-model:search="searchQueries.idDanhMuc"
                                    v-bind="comboboxProps" :custom-filter="() => true" :items="displayCategories"
                                    item-title="ten" item-value="id" :rules="[rules.required]" placeholder="Danh mục..."
                                    variant="outlined" density="comfortable" :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idDanhMuc')"
                                    @update:model-value="(val) => handleAttributeChange('idDanhMuc', val)">
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
                                <div class="field-label">Xuất xứ</div>
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
                                <div class="field-label">Chất liệu</div>
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
                                <div class="field-label">Đối tượng</div>
                                <v-select v-model="product.gioiTinhKhachHang"
                                    :items="[{ title: 'Nam', value: 'NAM' }, { title: 'Nữ', value: 'NU' }, { title: 'Unisex', value: 'UNISEX' }]"
                                    :rules="[rules.required]" clearable
                                    variant="outlined" density="comfortable"
                                    placeholder="Đối tượng"></v-select>
                            </v-col>
                            <v-col cols="12" md="3">
                                <div class="field-label">Mục đích</div>
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
                                <div class="field-label">Loại đế</div>
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
                                <div class="field-label">Loại cổ</div>
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
                            <div class="field-label mb-3">Màu sắc</div>
                            <div class="d-flex flex-wrap gap-4 mb-8">
                                <div v-for="c in colors.filter(x => selectedColors.includes(x.id))" :key="c.id"
                                    class="text-center cursor-pointer" @click="toggleColor(c.id)">
                                    <div class="color-circle mx-auto"
                                        :style="{ backgroundColor: c.maMauHex || '#e2e8f0' }"
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
                                                    :style="{ backgroundColor: c.maMauHex || '#e2e8f0' }"
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
                            <div class="field-label mb-3 mt-2">Kích thước</div>
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
                                            <v-text-field :model-value="customSizeName" @update:model-value="customSizeName = String($event || '').replace(/[^0-9]/g, '')" prepend-inner-icon="mdi-magnify"
                                                placeholder="Nhập kích thước" variant="outlined" density="compact"
                                                hide-details class="bg-slate-50" maxlength="250" type="number" min="0"></v-text-field>
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
                                <v-row no-gutters class="rounded-lg overflow-hidden" style="height: 600px; border: 1px solid #cbd5e1 !important;">
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
                                                <FormattedNumberField v-model="quickApplyValues.giaBan" placeholder="Giá bán"
                                                    variant="outlined" density="compact" hide-details
                                                    class="custom-input-dense" style="width: 120px" />
                                                <FormattedNumberField v-model="quickApplyValues.giaNhap" placeholder="Giá nhập"
                                                    variant="outlined" density="compact" hide-details
                                                    class="custom-input-dense" style="width: 120px" />
                                                <FormattedNumberField v-model="quickApplyValues.soLuong" placeholder="Số lượng"
                                                    variant="outlined" density="compact" hide-details
                                                    class="custom-input-dense" style="width: 120px" />
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
                                                <div class="text-caption font-weight-bold text-slate-600 mb-2">HÌNH ẢNH ĐẠI DIỆN CHO MÀU NÀY</div>
                                                <div class="color-image-uploader elevation-1"
                                                    @click="openColorImagePicker(activeColorTab)"
                                                    style="width: 70px; height: 70px; border-radius: 8px; border: 2px dashed #cbd5e1; cursor: pointer; overflow: hidden; background: white">
                                                    <v-img v-if="getColorUploadEntry(activeColorTab).url"
                                                        :src="getColorUploadEntry(activeColorTab).url" cover
                                                        class="w-100 h-100" />
                                                    <div v-else
                                                        class="w-100 h-100 d-flex align-center justify-center">
                                                        <v-icon icon="mdi-camera-plus" color="slate-400" size="24" />
                                                    </div>
                                                    <input :ref="(el) => setColorFileInputRef(activeColorTab, el)"
                                                        type="file" accept="image/*" class="d-none"
                                                        @change="handleColorImageUpload(activeColorTab, $event)" />
                                                </div>
                                            </div>
                                        </div>

                                        <div class="flex-grow-1" style="min-height: 0;">
                                            <v-table class="variant-inner-table h-100"
                                                fixed-header>
                                                <thead class="bg-white">
                                                    <tr>
                                                        <th class="text-left font-weight-bold text-slate-800 text-caption" style="border-bottom: 1px solid #cbd5e1 !important;">Thuộc tính
                                                            (Màu/Size)</th>
                                                        <th class="text-left font-weight-bold text-slate-800 text-caption"
                                                            style="width: 140px; border-bottom: 1px solid #cbd5e1 !important;">Giá bán (đ)</th>
                                                        <th class="text-left font-weight-bold text-slate-800 text-caption"
                                                            style="width: 140px; border-bottom: 1px solid #cbd5e1 !important;">Giá nhập (đ)</th>
                                                        <th class="text-left font-weight-bold text-slate-800 text-caption"
                                                            style="width: 110px; border-bottom: 1px solid #cbd5e1 !important;">Số lượng sản phẩm
                                                        </th>
                                                        <th class="text-center font-weight-bold text-slate-800 text-caption"
                                                            style="width: 60px; border-bottom: 1px solid #cbd5e1 !important;">Hành động</th>
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
                                                            <FormattedNumberField v-model="variant.soLuong"
                                                                hide-details variant="outlined" density="compact"
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
                                        <div class="flex-shrink-0 bg-white py-3 px-4" style="border-top: 2px solid #cbd5e1 !important;">
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
                                            color="primary" track-color="#e2e8f0" track-size="3" thumb-size="14"
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
                            <div class="field-label">Số lượng</div>
                            <FormattedNumberField v-model="bulkEditModal.form.soLuong"
                                placeholder="Nhập số lượng..." variant="outlined" density="comfortable" hide-details
                                class="custom-input" />
                        </v-col>
                        <v-col cols="12">
                            <div class="field-label">Giá nhập</div>
                            <FormattedNumberField v-model="bulkEditModal.form.giaNhap" placeholder="Nhập giá nhập..."
                                variant="outlined" density="comfortable" hide-details class="custom-input" />
                        </v-col>
                        <v-col cols="12">
                            <div class="field-label">Giá bán (VNĐ)</div>
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
    </v-container>
</template>

<style scoped>
/* Giảm cỡ chữ tiêu đề ô input và dữ liệu xuống 13px theo yêu cầu */
.field-label,
.create-config-preview__title,
:deep(.v-label),
:deep(.v-field__input),
:deep(.v-field__input input),
:deep(.v-textarea textarea),
:deep(.v-select__selection-text),
:deep(.v-btn-toggle .v-btn),
:deep(.v-list-item-title),
:deep(.data-cell),
:deep(.data-cell span) {
    font-size: 13px !important;
}

:deep(.v-field__input::placeholder) {
    font-size: 13px !important;
}

.field-label {
    font-weight: 600 !important;
    margin-bottom: 6px !important;
    color: #334155 !important;
}

.variant-chip-group :deep(.v-chip),
.variant-chip-group :deep(.v-chip__content) {
    font-size: 13px !important;
}

.color-group-card {
    background: #ffffff;
    border-radius: 12px !important;
    overflow: hidden;
    border: 1px solid #f1f5f9 !important;
    box-shadow: none !important;
}

.premium-card {
    box-shadow: none !important;
    border: 1px solid #cbd5e1 !important;
}

.color-group-header {
    background: #f8fafc;
    border-bottom: 1px solid #f1f5f9 !important;
}

.action-icon-btn {
    opacity: 0.6;
    transition: all 0.2s ease;
}

.action-icon-btn:hover {
    opacity: 1;
    transform: scale(1.1);
    background-color: rgba(239, 68, 68, 0.08) !important;
}

.color-image-uploader {
    width: 180px;
    height: 180px;
    border: 2px dashed #e2e8f0;
    border-radius: 12px;
    cursor: pointer;
    overflow: hidden;
    position: relative;
    transition: all 0.2s ease;
    background: #ffffff;
}

.color-image-uploader:hover {
    border-color: #3b82f6;
    background: #f1f7ff;
}

.uploader-placeholder {
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
}

.variant-inner-table {
    border-left: 1px solid #f1f5f9;
}

.variant-inner-table :deep(th) {
    font-size: 13px !important;
    text-transform: none !important;
    letter-spacing: normal !important;
}

.variant-inner-table :deep(td) {
    font-size: 13px !important;
    padding-top: 8px !important;
    padding-bottom: 8px !important;
}

.custom-input-dense :deep(.v-field__input) {
    font-size: 13px !important;
    min-height: 36px !important;
    padding-top: 0 !important;
    padding-bottom: 0 !important;
}

.attribute-list-wrapper {
    min-height: 200px;
    height: 0;
    /* Let flex-grow handle it */
    overflow-y: auto;
    border: 1px solid #f1f5f9;
    border-radius: 8px;
    background: #f8fafc;
    padding: 8px;
}

.attribute-list-wrapper::-webkit-scrollbar {
    width: 4px;
}

.attribute-list-wrapper::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 4px;
}

.attribute-checkbox :deep(.v-label) {
    font-size: 13px !important;
    color: #334155 !important;
    opacity: 1 !important;
}

.attribute-checkbox :deep(.v-selection-control) {
    min-height: 32px !important;
}

@media (min-width: 1280px) {
    .sticky-sidebar {
        position: sticky;
        top: 0;
    }
}

.header-actions {
    gap: 16px;
}

.header-actions__buttons {
    flex-wrap: wrap;
}

.section-toolbar {
    gap: 16px;
}

.section-toolbar__actions {
    gap: 12px;
    justify-content: flex-end;
}

.section-description {
    max-width: 680px;
    line-height: 1.6;
}

.create-shell-card {
    height: 100%;
}

.create-config-preview {
    border: 1px solid rgba(148, 163, 184, 0.18);
    border-radius: 18px;
    background: #fff;
    padding: 16px;
}

.create-config-preview__title {
    font-size: 13px;
    font-weight: 700;
    color: #475569;
    margin-bottom: 12px;
}

.create-config-preview__chips {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.create-generate-btn {
    min-height: 50px;
    border-radius: 3px !important;
    font-weight: 500 !important;
    letter-spacing: 0.5px;
    box-shadow: 0 4px 12px rgba(var(--v-theme-primary), 0.15) !important;
    text-transform: none !important;
}

.variant-gradient-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
    padding: 0 0 24px 0;
    border: none;
    background: transparent;
    color: #1e293b;
}

.variant-gradient-header__meta {
    color: #64748b;
}

.gradient-header-actions {
    gap: 10px;
}

.action-btn-light {
    border-color: rgba(148, 163, 184, 0.16) !important;
    color: #1e293b !important;
}

.variant-gradient-body {
    border: none;
    border-radius: 0 0 12px 12px;
    padding: 24px;
    background: #fff;
}

.bulk-toolbar {
    align-items: center;
    margin-bottom: 20px;
}

.bulk-toolbar__hint {
    height: 100%;
    min-height: 40px;
    display: flex;
    align-items: center;
    font-size: 13px;
    line-height: 1.5;
    color: #64748b;
}

.variant-group-stack {
    display: flex;
    flex-direction: column;
    gap: 18px;
}

.variant-color-group {
    border: 1px solid rgba(148, 163, 184, 0.18);
    border-radius: 20px;
    padding: 18px;
    background: linear-gradient(180deg, rgba(255, 255, 255, 1), rgba(248, 250, 252, 0.78));
}

.variant-color-group__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
    margin-bottom: 14px;
}

.group-actions {
    gap: 8px;
}

.group-quick-field {
    width: 140px;
}

.group-action-btn {
    min-width: 116px;
}

.variant-group-table {
    border: 1px solid rgba(148, 163, 184, 0.14);
    border-radius: 18px;
    overflow: hidden;
    background: #fff;
    font-family: 'Inter', sans-serif !important;
}

.variant-group-table__head,
.variant-group-row {
    display: grid;
    grid-template-columns: minmax(220px, 1.4fr) repeat(3, minmax(120px, 1fr)) 72px;
    gap: 12px;
    align-items: center;
    padding: 14px 18px;
}

.variant-group-table__head {
    background: #f8fafc;
    color: #334155;
    font-size: 13px;
    font-weight: 600;
    letter-spacing: 0.01em;
    text-transform: none;
}

.variant-group-row+.variant-group-row {
    border-top: 1px solid rgba(148, 163, 184, 0.12);
}

.variant-group-row__size {
    min-width: 0;
}

.variant-group-row__actions {
    display: flex;
    justify-content: center;
}

.variant-image-section {
    border: 1px solid rgba(148, 163, 184, 0.18);
    border-radius: 20px;
    padding: 18px;
    background: #fff;
}

.variant-image-section__title {
    display: flex;
    align-items: center;
    font-weight: 800;
    color: #1f2937;
    margin-bottom: 16px;
}

.color-image-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 16px;
}

.color-image-card {
    border: 1px solid rgba(148, 163, 184, 0.18);
    border-radius: 18px;
    overflow: hidden;
    background: #fff;
}

.color-image-card__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 14px;
    border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}

.color-image-card__preview {
    height: 230px;
    background: #f8fafc;
}

.color-image-card__empty {
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: #94a3b8;
}

.color-dot {
    width: 10px;
    height: 10px;
    border-radius: 999px;
    display: inline-block;
    margin-right: 10px;
    background: #0085DB;
    box-shadow: 0 0 0 4px rgba(0, 133, 219, 0.12);
}

.brand-logo-icon-blob {
    background: rgba(90, 200, 250, 0.16);
}

.brand-logo-icon {
    color: #0085DB;
}

.variant-edit-filter-wrap {
    position: relative;
}

.variant-filter-label {
    font-size: 13px;
    font-weight: 600;
    color: #0f172a;
    margin-bottom: 8px;
}

.variant-filter-input {
    background: #ffffff;
}

.variant-edit-filter-wrap :deep(.v-field) {
    border-radius: 12px !important;
}

.variant-edit-filter-wrap :deep(.v-slider-track__background) {
    opacity: 1;
}

.variant-edit-filter-wrap :deep(.v-slider-thumb__surface),
.variant-edit-filter-wrap :deep(.v-slider-track__fill) {
    color: rgb(var(--v-theme-primary)) !important;
}

.variant-table-wrap {
    overflow-x: auto;
    border: 1px solid rgba(148, 163, 184, 0.18);
    border-radius: 20px;
}

.variant-table-toolbar {
    border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}

.variant-table :deep(table) {
    min-width: 1020px;
}

.variant-table :deep(thead th) {
    background: #f8fafc;
    color: #475569;
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 0.01em;
    text-transform: none;
    white-space: nowrap;
}

.variant-table :deep(tbody td) {
    padding-top: 12px;
    padding-bottom: 12px;
    border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}

.color-dropzone-premium {
    border: 1px dashed rgba(148, 163, 184, 0.55);
    background: linear-gradient(180deg, rgba(248, 250, 252, 0.92), rgba(255, 255, 255, 0.98));
}

.qr-export-staging {
    position: fixed;
    left: -9999px;
    top: -9999px;
    opacity: 0;
    pointer-events: none;
}

.qr-export-item {
    display: inline-block;
    padding: 8px;
    background: #ffffff;
}

.variant-actions {
    gap: 6px;
}

.variant-pills {
    gap: 8px;
}

.variant-empty-state {
    border: 1px dashed rgba(148, 163, 184, 0.45);
    border-radius: 24px;
    padding: 40px 24px;
    text-align: center;
    background: linear-gradient(180deg, rgba(248, 250, 252, 0.9), rgba(255, 255, 255, 0.96));
}

.variant-empty-state__icon {
    width: 64px;
    height: 64px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 20px;
    background: #e0f2fe;
    color: #0891b2;
}

@media (max-width: 960px) {
    .variant-group-table__head {
        display: none;
    }

    .variant-group-row {
        grid-template-columns: 1fr;
    }

    .group-actions {
        width: 100%;
    }

    .group-quick-field {
        width: calc(50% - 4px);
        min-width: 140px;
    }
}

.color-circle {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    position: relative;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    transition: transform 0.2s;
    border: 1px solid rgba(0, 0, 0, 0.05);
}

.color-circle:hover {
    transform: scale(1.1);
}

.color-circle.dashed {
    background: transparent;
    border: 2px dashed #94a3b8;
    box-shadow: none;
}

.color-circle .check-icon {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.4);
}

.custom-color-preview {
    width: 64px;
    height: 64px;
    box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

.color-dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    border: 1px solid rgba(0, 0, 0, 0.1);
}

.hover-bg-slate-50:hover {
    background-color: #f8fafc !important;
}
</style>

<style>
/* Cưỡng chế kích cỡ chữ 13px cho danh sách dropdown được render qua cơ chế Overlay (ngoài Scope) */
.v-overlay-container .v-list-item-title {
    font-size: 13px !important;
}

.v-overlay-container .v-list-item__prepend .v-icon {
    font-size: 16px !important;
}
</style>