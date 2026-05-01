<script setup>
import { ref, onMounted, computed, watch, reactive } from 'vue';
import { PATH } from '@/router/routePaths';
import { useRoute, useRouter } from 'vue-router';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { dichVuBienThe } from '@/services/product/dichVuBienThe';
import { dichVuFile } from '@/services/core/dichVuFile';
import { useNotifications } from '@/services/notificationService';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import VariantFormModal from './components/VariantFormModal.vue';
import {
    ArrowLeftIcon, BoxIcon, DeviceFloppyIcon, InfoCircleIcon, PencilIcon,
    PhotoIcon, PlusIcon, SettingsIcon, TrashIcon
} from 'vue-tabler-icons';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import AdminEmptyState from '@/components/common/AdminEmptyState.vue';
import {
    dichVuThuongHieu, dichVuDanhMuc, dichVuXuatXu,
    dichVuChatLieu, dichVuDeGiay, dichVuCoGiay,
    dichVuMucDichChay, dichVuMauSac, dichVuKichThuoc
} from '@/services/product/dichVuThuocTinh';
import logoPlaceholder from '@/assets/images/logos/logo-light.svg';

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
    trangThais: [defaultVariantStatus, 'KHONG_HOAT_DONG']
}));
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
const hasSelectedActiveVariants = computed(() => selectedVariants.value.some((item) => isVariantActiveStatus(item.trangThai)));
const hasSelectedInactiveVariants = computed(() => selectedVariants.value.some((item) => !isVariantActiveStatus(item.trangThai)));
const canBulkActivateVariants = computed(() => selectedVariants.value.length > 0 && hasSelectedInactiveVariants.value);
const canBulkDeactivateVariants = computed(() => selectedVariants.value.length > 0 && hasSelectedActiveVariants.value);

const totalVariantStock = computed(() => variantItems.value.reduce(
    (sum, item) => sum + Number(item.soLuong || 0),
    0
));

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

    const normalizedLower = normalizedValue.toLowerCase();
    return options.find(item => (
        item.id === normalizedValue
        || item.ten?.trim().toLowerCase() === normalizedLower
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
    const currentValue = product.value[config.field];
    const existingOption = findAttributeOption(config, currentValue);

    if (existingOption) {
        product.value[config.field] = existingOption.id;
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

    const requestKey = normalizedText.toLowerCase();
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

const getStatusLabel = (status) => {
    if (status === defaultVariantStatus) return 'Đang hoạt động';
    if (status === 'KHONG_HOAT_DONG') return 'Ngừng bán';
    return status || 'Không xác định';
};
const isVariantActiveStatus = (status) => status === defaultVariantStatus;

const normalizeUploadedFileUrl = (value) => {
    if (!value) return '';
    if (typeof value === 'string') return value;
    return value.fileUrl || value.url || value.secure_url || value.duongDanAnh || value.data || '';
};

const normalizeOptionList = (listLike) => {
    if (Array.isArray(listLike)) return listLike;
    if (Array.isArray(listLike?.value)) return listLike.value;
    return [];
};

const getOptionLabel = (listLike, id) => normalizeOptionList(listLike).find(item => item.id === id)?.ten || '--';
const getVariantKey = (variant) => variant.id || variant.clientKey;
const getVariantSkuLabel = (variant) => variant.maChiTietSanPham || 'Tự sinh sau khi lưu sản phẩm';
const getCleanVariantSku = (variant) => {
    const sku = variant.maChiTietSanPham || '';
    if (!sku) return '--';
    
    // Pattern: PRODUCTCODE-COLORCODE-SIZE
    // We want to keep PRODUCTCODE-COLORCODE and hide the SIZE part if possible
    const parts = sku.split('-');
    if (parts.length >= 3) {
        // Return everything except the last part (usually size)
        return parts.slice(0, 2).join('-');
    }
    return sku;
};
const getVariantThumbnail = (variant) => normalizeUploadedFileUrl(
    variant.urlAnh
    || variant.images?.find(image => image.hinhAnhDaiDien)?.duongDanAnh
    || variant.images?.[0]?.duongDanAnh
    || variant.hinhAnh?.[0]?.duongDanAnh
    || variant.hinhAnh?.[0]?.url
    || variant.hinhAnh
) || logoPlaceholder;
const getVariantCombinationKey = (colorId, sizeId) => `${colorId}-${sizeId}`;
const getColorUploadEntry = (colorId) => colorImageState.value[colorId] || { url: '', uploading: false };
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
    maChiTietSanPham: variant.maChiTietSanPham || '',
    idMauSac: variant.idMauSac || '',
    idKichThuoc: variant.idKichThuoc || '',
    soLuong: Number(variant.soLuong ?? 0),
    giaNhap: Number(variant.giaNhap ?? 0),
    giaBan: Number(variant.giaBan ?? 0),
    trangThai: variant.trangThai || defaultVariantStatus,
    urlAnh: normalizeUploadedFileUrl(
        variant.urlAnh
        || variant.images?.find(image => image.hinhAnhDaiDien)?.duongDanAnh
        || variant.images?.[0]?.duongDanAnh
        || variant.hinhAnh?.[0]?.duongDanAnh
        || variant.hinhAnh?.[0]?.url
        || variant.hinhAnh
    )
        || ''
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
        moTaNgan: product.value.moTaNgan || '',
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

const generateVariants = () => {
    if (selectedColors.value.length === 0 || selectedSizes.value.length === 0) {
        addNotification({
            title: 'Cảnh báo',
            subtitle: 'Vui lòng chọn ít nhất 1 màu sắc và 1 kích thước để tạo biến thể',
            color: 'warning'
        });
        return;
    }

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
    gioiTinhKhachHang: 'UNISEX',
    trangThai: defaultVariantStatus,
    moTaNgan: '',
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
        gioiTinhKhachHang: data.gioiTinhKhachHang || 'UNISEX',
        trangThai: data.trangThai || defaultVariantStatus,
        moTaNgan: data.moTaNgan || '',
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
});

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
        ['idDeGiay', 'Loại đế'],
        ['idCoGiay', 'Loại cổ'],
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
    variantModal.value.open = false;
};

const handleToggleVariantStatus = (variant) => {
    const nextStatus = isVariantActiveStatus(variant.trangThai) ? 'KHONG_HOAT_DONG' : defaultVariantStatus;
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

const handleBulkVariantStatus = (nextStatus) => {
    if (!selectedVariants.value.length) {
        addNotification({
            title: 'Thông báo',
            subtitle: 'Bạn chưa chọn biến thể nào',
            color: 'warning'
        });
        return;
    }

    const canApplyStatus = nextStatus === defaultVariantStatus
        ? canBulkActivateVariants.value
        : canBulkDeactivateVariants.value;
    if (!canApplyStatus) {
        addNotification({
            title: 'Thông báo',
            subtitle: nextStatus === defaultVariantStatus
                ? 'Các biến thể đã ở trạng thái bật'
                : 'Các biến thể đã ở trạng thái tắt',
            color: 'warning'
        });
        return;
    }

    const selectedCount = selectedVariants.value.length;
    confirmDialog.value = {
        show: true,
        title: 'Cập nhật trạng thái biến thể',
        message: `Bạn có chắc chắn muốn ${nextStatus === defaultVariantStatus ? 'bật' : 'tắt'} ${selectedCount} biến thể đã chọn không?`,
        color: 'warning',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                await Promise.all(selectedVariants.value.map((variant) => persistVariantStatus(variant, nextStatus)));
                clearVariantSelection();
                if (isEditMode.value) {
                    await loadProduct(route.params.id);
                }
                addNotification({
                    title: 'Thành công',
                    subtitle: `Đã cập nhật trạng thái ${selectedCount} biến thể`,
                    color: 'success'
                });
                confirmDialog.value.show = false;
            } catch (error) {
                console.error(error);
                addNotification({ title: 'Lỗi', subtitle: 'Không thể cập nhật trạng thái hàng loạt', color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

const handleVariantSubmit = async (payload) => {
    const currentKey = variantModal.value.variant ? getVariantKey(variantModal.value.variant) : null;
    if (hasDuplicateVariant(payload, currentKey)) {
        addNotification({
            title: 'Lỗi',
            subtitle: 'Tổ hợp màu sắc và kích thước này đã tồn tại.',
            color: 'error'
        });
        return;
    }

    if (!isEditMode.value) {
        const nextVariant = mapVariantToFormState({
            ...variantModal.value.variant,
            ...payload,
            clientKey: variantModal.value.variant?.clientKey || createDraftKey()
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
            await dichVuBienThe.taoBienThe(route.params.id, buildVariantPayload(payload, true));
            addNotification({ title: 'Thành công', subtitle: 'Đã thêm biến thể mới', color: 'success' });
        } else {
            await dichVuBienThe.capNhatBienThe(
                variantModal.value.variant.id,
                buildVariantPayload(payload, false)
            );
            addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật biến thể', color: 'success' });
        }

        closeVariantModal();
        await loadProduct(route.params.id);
    } catch (error) {
        console.error(error);
        addNotification({ title: 'Lỗi', subtitle: 'Không thể lưu biến thể', color: 'error' });
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
                </v-btn>
            </div>
            <div class="d-flex gap-3 header-actions__buttons">
                <v-btn v-if="isEditMode" variant="outlined" color="primary"
                    class="text-none font-weight-bold px-6 rounded-lg h-11 border-2"
                    @click="router.push({ name: 'BienTheSanPham', query: { productId: route.params.id } })">
                    <BoxIcon size="18" class="mr-2" /> Quản lý biến thể
                </v-btn>
                <v-btn color="primary" variant="flat"
                    class="text-none font-weight-bold text-white px-8 rounded-lg h-11 elevation-4" :loading="saving"
                    @click="handleSave">
                    <DeviceFloppyIcon size="18" class="mr-2 text-white" />
                    <span>{{ isEditMode ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm' }}</span>
                </v-btn>
            </div>
        </div>

        <v-row v-if="loading">
            <v-col cols="12" class="text-center py-16">
                <v-progress-circular indeterminate color="primary" size="64" />
                <div class="mt-4 text-subtitle-1 font-weight-medium text-slate-500">Đang tải thông tin sản phẩm...</div>
            </v-col>
        </v-row>

        <v-row v-else class="form-grid pb-16">
            <template v-if="!isEditMode">
                <v-col cols="12">
                    <v-card class="premium-card mb-6">
                        <v-card-text class="pa-8">
                            <div class="section-header d-flex align-center mb-6">
                                <div class="icon-blob bg-blue-lighten-5 mr-3">
                                    <InfoCircleIcon size="18" class="text-primary" />
                                </div>
                                <div class="text-h5 font-weight-black text-gradient-to-r from-blue-500 to-purple-600">
                                    Thông tin cơ bản
                                </div>
                            </div>

                            <v-row>
                                <v-col cols="12" md="4">
                                    <div class="field-label">Mã sản phẩm</div>
                                    <v-text-field v-model="product.maSanPham" readonly placeholder="(Hệ thống tự sinh)"
                                        variant="outlined" density="comfortable"
                                        class="custom-input mono-font bg-slate-50" hide-details></v-text-field>
                                </v-col>
                                <v-col cols="12" md="8">
                                    <div class="field-label">Tên sản phẩm <span class="text-red">*</span></div>
                                    <v-text-field v-model="product.tenSanPham"
                                        placeholder="Ví dụ: Nike Mercurial Vapor 15" :rules="[rules.required]"
                                        variant="outlined" density="comfortable" hide-details="auto"></v-text-field>
                                </v-col>
                            </v-row>
                        </v-card-text>
                    </v-card>
                </v-col>

                <v-col cols="12">
                    <v-row class="fill-height">
                        <v-col cols="12" lg="7">
                            <v-card class="premium-card mb-6 h-100">
                                <v-card-text class="pa-8">
                                    <div class="section-header d-flex align-center mb-6">
                                        <div class="icon-blob bg-amber-lighten-5 mr-3">
                                            <SettingsIcon size="18" class="text-amber-darken-2" />
                                        </div>
                                        <span class="text-subtitle-1 font-weight-black text-slate-800">Thuộc tính & Phân
                                            loại</span>
                                    </div>

                                    <v-row>
                                        <v-col cols="12" md="6">
                                            <div class="field-label">Thương hiệu <span class="text-red">*</span></div>
                                            <v-combobox v-model="product.idThuongHieu" :items="brands" item-title="ten"
                                                item-value="id" :rules="[rules.required]" placeholder="Chọn..."
                                                variant="outlined" density="comfortable" :return-object="false"
                                                @keyup.enter="(e) => onKeyUpEnter(e, 'idThuongHieu')"></v-combobox>
                                        </v-col>
                                        <v-col cols="12" md="6">
                                            <div class="field-label">Danh mục <span class="text-red">*</span></div>
                                            <v-combobox v-model="product.idDanhMuc" :items="categories" item-title="ten"
                                                item-value="id" :rules="[rules.required]" placeholder="Chọn..."
                                                variant="outlined" density="comfortable" :return-object="false"
                                                @keyup.enter="(e) => onKeyUpEnter(e, 'idDanhMuc')"></v-combobox>
                                        </v-col>
                                        <v-col cols="12" md="6">
                                            <div class="field-label">Xuất xứ <span class="text-red">*</span></div>
                                            <v-combobox v-model="product.idXuatXu" :items="origins" item-title="ten"
                                                item-value="id" :rules="[rules.required]" placeholder="Chọn..."
                                                variant="outlined" density="comfortable" :return-object="false"
                                                @keyup.enter="(e) => onKeyUpEnter(e, 'idXuatXu')"></v-combobox>
                                        </v-col>
                                        <v-col cols="12" md="6">
                                            <div class="field-label">Chất liệu <span class="text-red">*</span></div>
                                            <v-combobox v-model="product.idChatLieu" :items="materials" item-title="ten"
                                                item-value="id" :rules="[rules.required]" placeholder="Chọn..."
                                                variant="outlined" density="comfortable" :return-object="false"
                                                @keyup.enter="(e) => onKeyUpEnter(e, 'idChatLieu')"></v-combobox>
                                        </v-col>
                                        <v-col cols="12" md="6">
                                            <div class="field-label">Loại đế <span class="text-red">*</span></div>
                                            <v-combobox v-model="product.idDeGiay" :items="soles" item-title="ten"
                                                item-value="id" :rules="[rules.required]" placeholder="Chọn..."
                                                variant="outlined" density="comfortable" :return-object="false"
                                                @keyup.enter="(e) => onKeyUpEnter(e, 'idDeGiay')"></v-combobox>
                                        </v-col>
                                        <v-col cols="12" md="6">
                                            <div class="field-label">Loại cổ <span class="text-red">*</span></div>
                                            <v-combobox v-model="product.idCoGiay" :items="collars" item-title="ten"
                                                item-value="id" :rules="[rules.required]" placeholder="Chọn..."
                                                variant="outlined" density="comfortable" :return-object="false"
                                                @keyup.enter="(e) => onKeyUpEnter(e, 'idCoGiay')"></v-combobox>
                                        </v-col>
                                        <v-col cols="12" md="6">
                                            <div class="field-label">Mục đích <span class="text-red">*</span></div>
                                            <v-combobox v-model="product.idMucDichChay" :items="purposes"
                                                item-title="ten" item-value="id" :rules="[rules.required]"
                                                placeholder="Chọn..." variant="outlined" density="comfortable"
                                                :return-object="false"
                                                @keyup.enter="(e) => onKeyUpEnter(e, 'idMucDichChay')"></v-combobox>
                                        </v-col>
                                        <v-col cols="12" md="6">
                                            <div class="field-label">Đối tượng <span class="text-red">*</span></div>
                                            <v-btn-toggle v-model="product.gioiTinhKhachHang" mandatory color="primary"
                                                variant="outlined" density="comfortable"
                                                class="w-100 rounded-lg custom-toggle">
                                                <v-btn value="NAM" class="flex-grow-1 font-weight-bold">Nam</v-btn>
                                                <v-btn value="NU" class="flex-grow-1 font-weight-bold">Nữ</v-btn>
                                                <v-btn value="UNISEX"
                                                    class="flex-grow-1 font-weight-bold">Unisex</v-btn>
                                            </v-btn-toggle>
                                        </v-col>
                                    </v-row>
                                </v-card-text>
                            </v-card>
                        </v-col>

                        <v-col cols="12" lg="5">
                            <v-card class="premium-card mb-6 h-100">
                                <v-card-text class="pa-8">
                                    <div class="section-header d-flex align-center mb-6">
                                        <div class="icon-blob brand-logo-icon-blob mr-3">
                                            <BoxIcon size="18" class="brand-logo-icon" />
                                        </div>
                                        <span class="text-subtitle-1 font-weight-black text-slate-800">Thiết lập biến
                                            thể</span>
                                    </div>

                                    <v-row>
                                        <v-col cols="12">
                                            <div class="field-label">Màu sắc <span class="text-red">*</span></div>
                                            <v-select v-model="selectedColors" :items="colors" item-title="ten"
                                                item-value="id" multiple chips closable-chips variant="outlined"
                                                density="comfortable" hide-details
                                                placeholder="Chọn màu sắc"></v-select>
                                        </v-col>
                                        <v-col cols="12">
                                            <div class="field-label">Kích thước <span class="text-red">*</span></div>
                                            <v-select v-model="selectedSizes" :items="sizes" item-title="ten"
                                                item-value="id" multiple chips closable-chips variant="outlined"
                                                density="comfortable" hide-details
                                                placeholder="Chọn kích thước"></v-select>
                                        </v-col>
                                        <v-col cols="12">
                                            <div class="create-config-preview">
                                                <div class="create-config-preview__title">Thông số áp dụng nhanh</div>
                                                <div class="create-config-preview__chips">
                                                    <v-chip size="x-small" variant="outlined">De: {{
                                                        getOptionLabel(soles,
                                                        product.idDeGiay) || '--' }}</v-chip>
                                                    <v-chip size="x-small" variant="outlined">Co: {{
                                                        getOptionLabel(collars,
                                                        product.idCoGiay) || '--' }}</v-chip>
                                                    <v-chip size="x-small" variant="outlined">Purpose: {{
                                                        getOptionLabel(purposes, product.idMucDichChay) || '--'
                                                        }}</v-chip>
                                                </div>
                                            </div>
                                        </v-col>
                                        <v-col cols="12" class="mt-auto">
                                            <v-btn block class="create-generate-btn text-none font-weight-bold"
                                                color="primary" size="large" @click="generateVariants">
                                                <PlusIcon size="20" class="mr-2" /> Tạo danh sách biến thể
                                            </v-btn>
                                        </v-col>
                                    </v-row>
                                </v-card-text>
                            </v-card>
                        </v-col>
                    </v-row>
                </v-col>

            </template>

            <template v-else>
                <!-- Edit mode: Left Column (Basic Info & Description) and Right Column (Attributes) -->
                <v-col cols="12" lg="6">
                    <!-- Basic Info -->
                    <v-card class="premium-card mb-6">
                        <v-card-text class="pa-8">
                            <div class="section-header d-flex align-center mb-6">
                                <div class="icon-blob bg-blue-lighten-5 mr-3">
                                    <InfoCircleIcon size="18" class="text-primary" />
                                </div>
                                <span class="text-subtitle-1 font-weight-black text-slate-800">Thông tin cơ bản</span>
                            </div>

                            <v-row>
                                <v-col cols="12" md="4">
                                    <div class="field-label">Mã sản phẩm</div>
                                    <v-text-field v-model="product.maSanPham" readonly
                                        :placeholder="isEditMode ? 'SP-XXXX' : 'Hệ thống tự sinh khi lưu'"
                                        variant="outlined" density="comfortable"
                                        class="custom-input mono-font bg-slate-50" hide-details></v-text-field>
                                </v-col>
                                <v-col cols="12" md="8">
                                    <div class="field-label">Tên sản phẩm <span class="text-red">*</span></div>
                                    <v-text-field v-model="product.tenSanPham" placeholder="Ví dụ: Giày Nike Air..."
                                        :rules="[rules.required]" variant="outlined" density="comfortable"
                                        hide-details="auto"></v-text-field>
                                </v-col>
                            </v-row>
                        </v-card-text>
                    </v-card>

                    <!-- Description Section (Moved inside left column) -->
                    <v-card class="premium-card mb-6">
                        <v-card-text class="pa-8">
                            <div class="section-header d-flex align-center mb-4">
                                <div class="icon-blob bg-cyan-lighten-5 mr-3">
                                    <v-icon icon="mdi-text-box-outline" size="18" class="text-cyan-darken-2" />
                                </div>
                                <span class="text-subtitle-1 font-weight-black text-slate-800">Mô tả sản phẩm</span>
                            </div>
                            <v-textarea v-model="product.moTaNgan" variant="outlined" rows="12"
                                placeholder="Nhập mô tả tóm tắt về sản phẩm..." hide-details
                                class="custom-textarea"></v-textarea>
                        </v-card-text>
                    </v-card>
                </v-col>

                <v-col cols="12" lg="6">
                    <!-- Attributes Section -->
                    <v-card class="premium-card mb-6 h-100">
                        <v-card-text class="pa-8">
                            <div class="section-header d-flex align-center mb-6">
                                <div class="icon-blob bg-amber-lighten-5 mr-3">
                                    <SettingsIcon size="18" class="text-amber-darken-2" />
                                </div>
                                <span class="text-subtitle-1 font-weight-black text-slate-800">Thuộc tính & Phân
                                    loại</span>
                            </div>

                            <v-row>
                                <v-col cols="12" md="6">
                                    <div class="field-label">Xuất xứ <span class="text-red">*</span></div>
                                    <v-combobox v-model="product.idXuatXu" :items="origins" item-title="ten"
                                        item-value="id" :rules="[rules.required]" placeholder="Chọn xuất xứ..."
                                        variant="outlined" density="comfortable" :return-object="false"
                                        @keyup.enter="(e) => onKeyUpEnter(e, 'idXuatXu')"></v-combobox>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="field-label">Chất liệu <span class="text-red">*</span></div>
                                    <v-combobox v-model="product.idChatLieu" :items="materials" item-title="ten"
                                        item-value="id" :rules="[rules.required]" placeholder="Chọn chất liệu..."
                                        variant="outlined" density="comfortable" :return-object="false"
                                        @keyup.enter="(e) => onKeyUpEnter(e, 'idChatLieu')"></v-combobox>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="field-label">Thương hiệu <span class="text-red">*</span></div>
                                    <v-combobox v-model="product.idThuongHieu" :items="brands" item-title="ten"
                                        item-value="id" :rules="[rules.required]" placeholder="Tìm thương hiệu..."
                                        variant="outlined" density="comfortable" :return-object="false"
                                        @keyup.enter="(e) => onKeyUpEnter(e, 'idThuongHieu')"></v-combobox>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="field-label">Danh mục <span class="text-red">*</span></div>
                                    <v-combobox v-model="product.idDanhMuc" :items="categories" item-title="ten"
                                        item-value="id" :rules="[rules.required]" placeholder="Chọn danh mục..."
                                        variant="outlined" density="comfortable" :return-object="false"
                                        @keyup.enter="(e) => onKeyUpEnter(e, 'idDanhMuc')"></v-combobox>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="field-label">Loại đế <span class="text-red">*</span></div>
                                    <v-combobox v-model="product.idDeGiay" :items="soles" item-title="ten"
                                        item-value="id" :rules="[rules.required]" variant="outlined"
                                        density="comfortable" :return-object="false"
                                        @keyup.enter="(e) => onKeyUpEnter(e, 'idDeGiay')"></v-combobox>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="field-label">Loại cổ <span class="text-red">*</span></div>
                                    <v-combobox v-model="product.idCoGiay" :items="collars" item-title="ten"
                                        item-value="id" :rules="[rules.required]" variant="outlined"
                                        density="comfortable" :return-object="false"
                                        @keyup.enter="(e) => onKeyUpEnter(e, 'idCoGiay')"></v-combobox>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="field-label">Mục đích sử dụng <span class="text-red">*</span></div>
                                    <v-combobox v-model="product.idMucDichChay" :items="purposes" item-title="ten"
                                        item-value="id" :rules="[rules.required]" variant="outlined"
                                        density="comfortable" :return-object="false"
                                        @keyup.enter="(e) => onKeyUpEnter(e, 'idMucDichChay')"></v-combobox>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="field-label">Đối tượng sử dụng <span class="text-red">*</span></div>
                                    <v-btn-toggle v-model="product.gioiTinhKhachHang" mandatory color="primary"
                                        variant="outlined" density="comfortable" class="w-100 rounded-lg custom-toggle">
                                        <v-btn value="NAM" class="flex-grow-1 font-weight-bold">Nam</v-btn>
                                        <v-btn value="NU" class="flex-grow-1 font-weight-bold">Nữ</v-btn>
                                        <v-btn value="UNISEX" class="flex-grow-1 font-weight-bold">Unisex</v-btn>
                                    </v-btn-toggle>
                                </v-col>
                            </v-row>
                        </v-card-text>
                    </v-card>
                </v-col>
            </template>

            <!-- Shared Description Section (Create mode only) -->
            <v-col cols="12" v-if="!isEditMode">
                <v-card class="premium-card mb-6">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-4">
                            <div class="icon-blob bg-cyan-lighten-5 mr-3">
                                <v-icon icon="mdi-text-box-outline" size="18" class="text-cyan-darken-2" />
                            </div>
                            <span class="text-subtitle-1 font-weight-black text-slate-800">Mô tả sản phẩm</span>
                        </div>
                        <v-textarea v-model="product.moTaNgan" variant="outlined" rows="3"
                            placeholder="Nhập mô tả tóm tắt về sản phẩm..." hide-details
                            class="custom-textarea"></v-textarea>
                    </v-card-text>
                </v-card>
            </v-col>

            <v-col cols="12" class="mt-8">
                <template v-if="!isEditMode">
                    <v-row>
                        <!-- Full Width: Variants Management -->
                        <v-col cols="12">
                            <div class="premium-card overflow-hidden">
                                <div class="variant-gradient-header">
                                    <div class="d-flex align-center">
                                        <div class="icon-blob bg-white-transparent mr-3">
                                            <v-icon icon="mdi-view-grid-plus-outline" size="18" class="text-white" />
                                        </div>
                                        <div>
                                            <div class="font-weight-bold text-white">Danh sách biến thể</div>
                                            <div class="text-caption text-blue-50">
                                                {{ variantItems.length }} biến thể • Kho {{ totalVariantStock }}
                                            </div>
                                        </div>
                                    </div>

                                    <div class="d-flex align-center flex-wrap gradient-header-actions">
                                        <v-btn variant="flat" color="white"
                                            class="text-none rounded-lg action-btn-light text-primary font-weight-black px-5 h-10 shadow-sm"
                                            @click="applyBulkAllVariants">
                                            <v-icon icon="mdi-lightning-bolt" size="16" class="mr-1" />
                                            Thêm nhanh tất cả
                                        </v-btn>
                                        <v-btn variant="outlined" color="white"
                                            class="text-none rounded-lg ml-2 px-5 h-10 border-2 font-weight-black"
                                            @click="clearAllDraftVariants">
                                            <v-icon icon="mdi-delete-outline" size="16" class="mr-1" />
                                            Xóa tất cả
                                        </v-btn>
                                    </div>
                                </div>

                                <div class="variant-gradient-body pa-6">
                                    <v-row class="bulk-toolbar mb-6 pa-4 bg-slate-50 rounded-lg border" dense>
                                        <v-col cols="12" md="3">
                                            <div class="text-caption font-weight-bold text-slate-500 mb-1 ml-1">Số lượng
                                            </div>
                                            <v-text-field v-model.number="bulkAllForm.soLuong" type="number" min="0"
                                                variant="outlined" density="compact" hide-details
                                                placeholder="Số lượng tất cả" class="bg-white"></v-text-field>
                                        </v-col>
                                        <v-col cols="12" md="3">
                                            <div class="text-caption font-weight-bold text-slate-500 mb-1 ml-1">Giá nhập
                                            </div>
                                            <v-text-field v-model.number="bulkAllForm.giaNhap" type="number" min="0"
                                                variant="outlined" density="compact" hide-details
                                                placeholder="Giá nhập tất cả" class="bg-white"></v-text-field>
                                        </v-col>
                                        <v-col cols="12" md="3">
                                            <div class="text-caption font-weight-bold text-slate-500 mb-1 ml-1">Giá bán
                                            </div>
                                            <v-text-field v-model.number="bulkAllForm.giaBan" type="number" min="0"
                                                variant="outlined" density="compact" hide-details
                                                placeholder="Giá bán tất cả" class="bg-white"></v-text-field>
                                        </v-col>
                                        <v-col cols="12" md="3" class="d-flex align-end">
                                            <div class="bulk-toolbar__hint text-caption text-slate-400 italic">
                                                * Nhập giá trị để áp dụng nhanh cho toàn bộ danh sách bên dưới.
                                            </div>
                                        </v-col>
                                    </v-row>

                                    <div v-if="variantGroups.length > 0" class="variant-group-stack">
                                        <div v-for="group in variantGroups" :key="group.colorId"
                                            class="variant-color-group border rounded-xl mb-10 overflow-hidden bg-white">
                                            <v-row no-gutters class="fill-height align-stretch">
                                                <!-- Side Column: Color Image -->
                                                <v-col cols="12" md="3" class="border-r bg-slate-50/50 d-flex flex-column">
                                                    <div class="pa-4 border-b bg-white">
                                                        <div class="d-flex align-center justify-space-between">
                                                            <div class="d-flex align-center">
                                                                <span class="color-dot mr-2"></span>
                                                                <span class="font-weight-black text-slate-800">{{ group.colorLabel }}</span>
                                                            </div>
                                                            <v-btn v-if="getColorUploadEntry(group.colorId).url" icon size="x-small" variant="text" color="slate-400" @click="clearColorImage(group.colorId)">
                                                                <v-icon icon="mdi-close" size="14" />
                                                            </v-btn>
                                                        </div>
                                                    </div>

                                                    <div class="flex-grow-1 pa-4 d-flex flex-column">
                                                        <input :ref="(el) => setColorFileInputRef(group.colorId, el)" type="file"
                                                            accept="image/*" class="d-none"
                                                            @change="handleColorImageUpload(group.colorId, $event)" />
                                                            
                                                        <div class="color-dropzone-premium flex-grow-1 rounded-xl border-dashed d-flex flex-column align-center justify-center cursor-pointer transition-all"
                                                            style="min-height: 240px; position: relative; overflow: hidden;"
                                                            @click="openColorImagePicker(group.colorId)">
                                                            
                                                            <v-img v-if="getColorUploadEntry(group.colorId).url"
                                                                :src="getColorUploadEntry(group.colorId).url" cover
                                                                class="fill-height w-100"></v-img>
                                                                
                                                            <template v-else>
                                                                <div class="text-center pa-6" v-if="!getColorUploadEntry(group.colorId).uploading">
                                                                    <div class="icon-blob bg-blue-lighten-5 mb-4 mx-auto" style="width: 52px; height: 52px;">
                                                                        <v-icon icon="mdi-image-plus" color="primary" size="24" />
                                                                    </div>
                                                                    <div class="text-subtitle-2 font-weight-black text-slate-700">Tải ảnh màu</div>
                                                                    <div class="text-caption text-slate-400 mt-2 px-2">Đại diện cho dòng sản phẩm màu {{ group.colorLabel }}</div>
                                                                </div>
                                                                <div v-else class="text-center">
                                                                    <v-progress-circular indeterminate color="primary" size="32" width="3" />
                                                                    <div class="text-caption text-primary mt-3 font-weight-bold">Đang tải...</div>
                                                                </div>
                                                            </template>
                                                        </div>
                                                    </div>
                                                </v-col>

                                                <!-- Main Column: Variant Table -->
                                                <v-col cols="12" md="9" class="d-flex flex-column">
                                                    <div class="variant-color-group__header pa-4 bg-white border-b">
                                                        <div class="d-flex align-center">
                                                            <v-icon icon="mdi-layers-triple-outline" size="18" class="mr-2 text-slate-300" />
                                                            <span class="font-weight-bold text-slate-500 uppercase tracking-wider" style="font-size: 11px;">Chi tiết biến thể</span>
                                                        </div>

                                                        <div class="d-flex align-center flex-wrap group-actions">
                                                            <v-text-field
                                                                v-model.number="bulkByColorForms[group.colorId].soLuong"
                                                                type="number" min="0" variant="outlined" density="compact"
                                                                hide-details placeholder="SL"
                                                                class="group-quick-field mr-2 bg-white"
                                                                style="width: 80px;"></v-text-field>
                                                            <v-text-field
                                                                v-model.number="bulkByColorForms[group.colorId].giaBan"
                                                                type="number" min="0" variant="outlined" density="compact"
                                                                hide-details placeholder="Giá"
                                                                class="group-quick-field mr-2 bg-white"
                                                                style="width: 120px;"></v-text-field>
                                                            <v-btn color="primary" variant="flat"
                                                                class="text-none font-weight-black px-6 rounded-lg h-10 elevation-2 text-white"
                                                                @click="applyBulkColorVariants(group.colorId)">
                                                                Áp dụng
                                                            </v-btn>
                                                            <v-btn icon size="small" variant="text" color="error" class="ml-2"
                                                                @click="removeColorGroup(group.colorId)">
                                                                <TrashIcon size="18" />
                                                            </v-btn>
                                                        </div>
                                                    </div>
                                                <div
                                                    class="variant-group-table__head px-4 py-2 bg-slate-100 text-overline">
                                                    <div>Kích cỡ</div>
                                                    <div>Số lượng</div>
                                                    <div>Giá nhập</div>
                                                    <div>Giá bán (VNĐ)</div>
                                                    <div class="text-center">Xóa</div>
                                                </div>

                                                <div v-for="variant in group.variants" :key="getVariantKey(variant)"
                                                    class="variant-group-row px-4 py-3 border-b hover-bg-slate-50">
                                                    <div class="variant-group-row__size">
                                                        <div class="font-weight-bold text-slate-800">
                                                            Size {{ getVariantSizeLabel(variant.idKichThuoc) }}
                                                        </div>
                                                    </div>
                                                    <div>
                                                        <v-text-field
                                                            v-model.number="variantItems[variant.index].soLuong"
                                                            type="number" min="0" variant="outlined" density="compact"
                                                            hide-details class="bg-white"></v-text-field>
                                                    </div>
                                                    <div>
                                                        <v-text-field
                                                            v-model.number="variantItems[variant.index].giaNhap"
                                                            type="number" min="0" variant="outlined" density="compact"
                                                            hide-details class="bg-white"></v-text-field>
                                                    </div>
                                                    <div>
                                                        <v-text-field
                                                            v-model.number="variantItems[variant.index].giaBan"
                                                            type="number" min="0" variant="outlined" density="compact"
                                                            hide-details class="bg-white"></v-text-field>
                                                    </div>
                                                    <div class="d-flex justify-center">
                                                        <v-btn icon variant="text" color="error" size="small"
                                                            @click="removeDraftVariantByIndex(variant.index)">
                                                            <TrashIcon size="18" />
                                                        </v-btn>
                                                    </div>
                                                    </div>
                                                </v-col>
                                            </v-row>
                                        </div>
                                    </div>

                                    <AdminEmptyState v-else :icon="BoxIcon" title="Chưa có biến thể nào được tạo">
                                        <template #subtitle>
                                            Chọn màu sắc và kích thước ở card phía trên, sau đó bấm <span
                                                class="font-weight-bold text-primary">Thêm biến thể</span>.
                                        </template>
                                    </AdminEmptyState>
                                </div>
                            </div>
                        </v-col>
                    </v-row>
                </template>

                <template v-else>
                    <div class="premium-card pa-8">
                        <div class="d-flex align-start justify-space-between flex-wrap section-toolbar">
                            <div class="section-header d-flex align-center">
                                <div class="icon-blob bg-cyan-lighten-5 mr-3">
                                    <PhotoIcon size="18" class="text-cyan-darken-2" />
                                </div>
                                <div>
                                    <div class="text-subtitle-1 font-weight-black text-slate-800">Biến thể sản phẩm
                                    </div>
                                </div>
                            </div>

                            <div class="d-flex align-center flex-wrap section-toolbar__actions">
                                <div class="d-flex align-center mr-4 bg-slate-50 px-4 py-1 rounded-pill border status-toggle-wrap">
                                    <span class="text-caption font-weight-black text-slate-500 mr-2">Trạng thái bán:</span>
                                    <v-switch v-model="product.trangThai" 
                                        :true-value="'DANG_HOAT_DONG'" 
                                        :false-value="'KHONG_HOAT_DONG'"
                                        color="success" hide-details density="compact" />
                                </div>
                                <v-chip color="primary" variant="tonal" class="font-weight-bold">
                                    {{ variantItems.length }} biến thể
                                </v-chip>
                                <v-btn color="primary" variant="flat" class="text-none font-weight-bold rounded-lg text-white px-6 h-11"
                                    @click="openCreateVariantModal">
                                    <PlusIcon size="18" class="mr-2 text-white" /> <span class="text-white">Thêm biến thể</span>
                                </v-btn>
                            </div>
                        </div>

                        <div v-if="variantItems.length > 0" class="variant-edit-filter-wrap mt-5">
                            <AdminFilter title="Bộ lọc nâng cao" @refresh="resetVariantTableFilters" :loading="loading">
                                <v-col cols="12" md="3">
                                    <div class="variant-filter-label">Sản phẩm</div>
                                    <v-text-field :model-value="variantFilterProductLabel" variant="outlined"
                                        density="compact" hide-details readonly
                                        class="variant-filter-input bg-slate-50" />
                                </v-col>
                                <v-col cols="12" md="3">
                                    <div class="variant-filter-label">Tìm kiếm nhanh</div>
                                    <v-text-field v-model="variantTableFilters.keyword"
                                        placeholder="Mã SKU, màu, size..." prepend-inner-icon="mdi-magnify"
                                        variant="outlined" density="compact" hide-details clearable
                                        class="variant-filter-input" />
                                </v-col>
                                <v-col cols="12" md="2">
                                    <div class="variant-filter-label">Màu sắc</div>
                                    <v-select v-model="variantTableFilters.mauSacId"
                                        :items="[{ title: 'Tất cả màu', value: '' }, ...colors.map((item) => ({ title: item.ten, value: item.id }))]"
                                        variant="outlined" density="compact" hide-details
                                        class="variant-filter-input" />
                                </v-col>
                                <v-col cols="12" md="2">
                                    <div class="variant-filter-label">Kích thước</div>
                                    <v-select v-model="variantTableFilters.kichThuocId"
                                        :items="[{ title: 'Tất cả size', value: '' }, ...sizes.map((item) => ({ title: item.ten, value: item.id }))]"
                                        variant="outlined" density="compact" hide-details
                                        class="variant-filter-input" />
                                </v-col>
                                <v-col cols="12" md="2">
                                    <div class="variant-filter-label">Trạng thái</div>
                                    <v-select v-model="variantTableFilters.trangThai" :items="[
                                        { title: 'Tất cả trạng thái', value: '' },
                                        { title: 'Đang hoạt động', value: defaultVariantStatus },
                                        { title: 'Ngừng bán', value: 'KHONG_HOAT_DONG' }
                                    ]" variant="outlined" density="compact" hide-details
                                        class="variant-filter-input" />
                                </v-col>
                                <v-col cols="12" md="10" lg="11">
                                    <div class="variant-filter-label">Khoảng giá</div>
                                    <div class="d-flex align-center bg-white pa-4 rounded-lg ">
                                        <v-icon size="20" class="mr-4 text-primary">mdi-cash-multiple</v-icon>
                                        <div class="flex-grow-1">
                                            <div class="d-flex justify-space-between mb-2">
                                                <span class="text-caption font-weight-black text-slate-600">Lọc theo giá
                                                    bán biến thể</span>
                                                <span class="text-caption font-weight-black text-primary">
                                                    {{ formatCurrency(variantTableFilters.khoangGia[0]) }} - {{
                                                        formatCurrency(variantTableFilters.khoangGia[1]) }}
                                                </span>
                                            </div>
                                            <v-range-slider v-model="variantTableFilters.khoangGia"
                                                :min="variantPriceBounds.min" :max="variantPriceBounds.max"
                                                :step="variantPriceStep" hide-details color="primary"
                                                track-color="slate-200" />
                                        </div>
                                    </div>
                                </v-col>
                            </AdminFilter>
                        </div>
                        <div v-if="variantItems.length > 0" class="variant-table-wrap mt-6">
                            <div
                                class="variant-table-toolbar px-5 py-3 bg-slate-50 border-b d-flex align-center justify-space-between flex-wrap gap-3">
                                <div class="d-flex align-center flex-wrap gap-2">
                                    <v-checkbox-btn :model-value="allVisibleVariantsSelected"
                                        :indeterminate="someVisibleVariantsSelected" color="primary" hide-details
                                        density="compact" @update:model-value="toggleSelectVisibleVariants" />
                                    <span class="text-caption font-weight-black text-slate-500">
                                        Đã chọn {{ selectedVariantKeys.length }} biến thể • Tìm thấy {{
                                            filteredVariantItems.length }} biến thể
                                    </span>
                                </div>

                                <div class="d-flex align-center flex-wrap gap-2">
                                    <v-btn size="small" variant="tonal" color="success"
                                        class="bulk-status-btn text-none" :disabled="!canBulkActivateVariants"
                                        @click="handleBulkVariantStatus(defaultVariantStatus)">
                                        Bật trạng thái
                                    </v-btn>
                                    <v-btn size="small" variant="tonal" color="warning"
                                        class="bulk-status-btn text-none" :disabled="!canBulkDeactivateVariants"
                                        @click="handleBulkVariantStatus('KHONG_HOAT_DONG')">
                                        Tắt trạng thái
                                    </v-btn>
                                </div>
                            </div>

                            <v-table class="variant-table">
                                <thead>
                                    <tr>
                                        <th class="text-center">Chọn</th>
                                        <th class="text-center">Ảnh</th>
                                        <th class="text-center">Màu sắc</th>
                                        <th class="text-center">Kích thước</th>
                                        <th class="text-center">SKU</th>
                                        <th class="text-center">Số lượng</th>
                                        <th class="text-center">Giá bán</th>
                                        <th class="text-center">Trạng thái</th>
                                        <th class="text-center">Thao tác</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr v-for="variant in paginatedVariantItems" :key="getVariantKey(variant)">
                                        <td class="text-center">
                                            <v-checkbox-btn
                                                :model-value="selectedVariantKeys.includes(getVariantKey(variant))"
                                                color="primary" hide-details density="compact"
                                                @update:model-value="toggleVariantSelection(getVariantKey(variant), $event)" />
                                        </td>
                                        <td class="text-center">
                                            <v-avatar rounded="lg" size="48" class="border bg-slate-50 shadow-sm mx-auto">
                                                <v-img :src="getVariantThumbnail(variant)" cover></v-img>
                                            </v-avatar>
                                        </td>
                                        <td class="text-center font-weight-bold text-slate-700">
                                            {{ getOptionLabel(colors, variant.idMauSac) }}
                                        </td>
                                        <td class="text-center font-weight-bold text-slate-700">
                                            {{ getOptionLabel(sizes, variant.idKichThuoc) }}
                                        </td>
                                        <td class="text-center mono-font text-slate-600 font-weight-bold">
                                            {{ getCleanVariantSku(variant) }}
                                        </td>
                                        <td class="text-center font-weight-black text-slate-700">
                                            {{ variant.soLuong ?? 0 }}
                                        </td>
                                        <td class="text-center font-weight-black text-primary">
                                            {{ formatCurrency(variant.giaBan) }}
                                        </td>
                                        <td class="text-center">
                                            <v-chip size="small"
                                                :color="isVariantActiveStatus(variant.trangThai) ? 'success' : 'warning'"
                                                variant="flat" class="font-weight-bold text-white">
                                                {{ getStatusLabel(variant.trangThai) }}
                                            </v-chip>
                                        </td>
                                        <td>
                                            <div class="d-flex justify-end align-center variant-actions gap-2">
                                                <v-btn icon size="small" variant="text" color="primary"
                                                    @click="openEditVariantModal(variant)">
                                                    <PencilIcon size="20" stroke-width="2.5" />
                                                    <v-tooltip activator="parent" location="top">Chỉnh sửa biến
                                                        thể</v-tooltip>
                                                </v-btn>
                                                <div class="switch-wrapper d-flex align-center justify-center">
                                                    <v-switch :model-value="isVariantActiveStatus(variant.trangThai)"
                                                        color="primary" hide-details density="compact"
                                                        class="tight-switch action-switch"
                                                        @click.prevent.stop="handleToggleVariantStatus(variant)" />
                                                    <v-tooltip activator="parent" location="top">Trạng thái
                                                        bán</v-tooltip>
                                                </div>
                                            </div>
                                        </td>
                                    </tr>
                                </tbody>
                            </v-table>

                            <div class="px-5 py-4 border-t bg-white">
                                <AdminPagination v-model="variantPage" :page-size="variantPageSize"
                                    @update:pageSize="variantPageSize = $event" :total-pages="totalVariantPages"
                                    :total-elements="totalVariantElements"
                                    :current-size="paginatedVariantItems.length" />
                            </div>
                        </div>

                        <AdminEmptyState v-else :icon="PhotoIcon" title="Chưa có biến thể nào" 
                            subtitle="Bạn có thể thêm biến thể mới ngay tại form này hoặc tiếp tục dùng màn quản lý biến thể riêng.">
                            <template #actions>
                                <v-btn color="primary" variant="flat" class="rounded-lg text-none font-weight-bold px-8 h-11 elevation-4"
                                    @click="openCreateVariantModal">
                                    <PlusIcon size="18" class="mr-2" /> Thêm biến thể đầu tiên
                                </v-btn>
                            </template>
                        </AdminEmptyState>
                    </div>
                </template>
            </v-col>
        </v-row>

        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />

        <VariantFormModal :open="variantModal.open" :mode="variantModal.mode" :variant="variantModal.variant"
            :options="variantOptions" :submitting="variantModal.submitting" :productCode="product.maSanPham"
            :lock-attributes-on-edit="isEditMode" :allow-image-upload="!isEditMode || variantModal.mode === 'create'"
            @close="closeVariantModal" @submit="handleVariantSubmit" @options-refreshed="fetchFormOptions" />
    </v-container>
</template>

<style scoped>
@media (min-width: 1280px) {
    .sticky-sidebar {
        position: sticky;
        top: 24px;
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

.status-toggle-wrap {
    height: 44px;
    border-color: #e2e8f0 !important;
    display: flex;
    align-items: center;
}

.status-toggle-wrap :deep(.v-selection-control) {
    min-height: unset;
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
    background: linear-gradient(180deg, rgba(248, 250, 252, 0.95), rgba(255, 255, 255, 1));
    padding: 16px;
}

.create-config-preview__title {
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    color: #64748b;
    margin-bottom: 12px;
}

.create-config-preview__chips {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.create-generate-btn {
    min-height: 50px;
    border-radius: 16px;
    background-color: rgb(var(--v-theme-primary)) !important;
    color: #fff !important;
    box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.premium-card {
    border: 1px solid rgba(148, 163, 184, 0.16) !important;
    border-radius: 16px !important;
    background: #fff !important;
    box-shadow: 0 4px 10px rgba(15, 23, 42, 0.03) !important;
    overflow: hidden;
}

.variant-gradient-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
    padding: 16px 24px;
    border-radius: 16px 16px 0 0;
    background: linear-gradient(90deg, #5AC8FA 0%, #0085DB 48%, #111C2D 100%);
    color: #fff;
}

.variant-gradient-body {
    border: 1px solid rgba(148, 163, 184, 0.14);
    border-top: none;
    border-radius: 0 0 16px 16px;
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
    border: 1px solid rgba(148, 163, 184, 0.16);
    border-radius: 16px;
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
    border-radius: 12px;
    overflow: hidden;
    background: #fff;
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
    color: #64748b;
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 0.04em;
    text-transform: uppercase;
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
    border: 1px solid rgba(148, 163, 184, 0.16);
    border-radius: 16px;
    padding: 18px;
    background: #fff;
}

.variant-image-section__title {
    display: flex;
    align-items: center;
    font-weight: 600;
    color: #1f2937;
    margin-bottom: 16px;
}

.color-image-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 16px;
}

.color-dropzone-premium {
    border: 2px dashed rgba(148, 163, 184, 0.25);
    background: #f8fafc;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.color-dropzone-premium:hover {
    border-color: rgb(var(--v-theme-primary));
    background: #fff;
    box-shadow: 0 8px 24px rgba(var(--v-theme-primary), 0.08);
}

.image-overlay {
    position: absolute;
    inset: 0;
    background: rgba(15, 23, 42, 0.4);
    backdrop-filter: blur(2px);
    opacity: 0;
    transition: opacity 0.3s ease;
}

.color-dropzone-premium:hover .image-overlay {
    opacity: 1;
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
    font-weight: 800;
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

.bulk-status-btn {
    font-weight: 800;
    letter-spacing: 0.01em;
}

.bulk-status-btn:deep(.v-btn__content) {
    font-weight: 800;
}

.bulk-status-btn.v-btn--disabled {
    opacity: 0.62 !important;
}

.variant-table :deep(table) {
    min-width: 1020px;
}

.variant-table :deep(thead th) {
    background: #f8fafc;
    color: #475569;
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    white-space: nowrap;
}

.variant-table :deep(tbody td) {
    padding-top: 16px;
    padding-bottom: 16px;
    border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}

.variant-actions {
    gap: 6px;
}

.variant-actions .switch-wrapper {
    min-width: 52px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.variant-actions :deep(.tight-switch .v-selection-control) {
    min-height: 28px;
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
</style>
