<script setup>
import { ref, onMounted, computed } from 'vue';
import { PATH } from '@/router/routePaths';
import { useRoute, useRouter } from 'vue-router';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { dichVuBienThe } from '@/services/product/dichVuBienThe';
import { dichVuFile } from '@/services/core/dichVuFile';
import { useNotifications } from '@/services/notificationService';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';
import VariantFormModal from './components/VariantFormModal.vue';
import {
    ArrowLeftIcon, BoxIcon, DeviceFloppyIcon, InfoCircleIcon, PencilIcon,
    PhotoIcon, PlusIcon, SettingsIcon, TrashIcon
} from 'vue-tabler-icons';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import {
    dichVuThuongHieu, dichVuDanhMuc, dichVuXuatXu,
    dichVuChatLieu, dichVuDeGiay, dichVuCoGiay,
    dichVuMucDichChay, dichVuMauSac, dichVuKichThuoc
} from '@/services/product/dichVuThuocTinh';
import logoPlaceholder from '@/assets/images/logos/logo-light.svg';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

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

const variantOptions = computed(() => ({
    mauSacs: colors.value,
    kichThuocs: sizes.value,
    trangThais: [defaultVariantStatus, 'KHONG_HOAT_DONG']
}));

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
    if (status === 'KHONG_HOAT_DONG') return 'Ngừng hoạt động';
    return status || 'Không xác định';
};

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
                    <DeviceFloppyIcon size="18" class="mr-2 text-white" /> Lưu thông tin
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
                <v-col cols="12" lg="7">
                    <v-card class="premium-card create-shell-card mb-6">
                        <v-card-text class="pa-8">
                            <div class="section-header d-flex align-center mb-6">
                                <div class="icon-blob bg-blue-lighten-5 mr-3">
                                    <InfoCircleIcon size="18" class="text-primary" />
                                </div>
                                <div>
                                    <div style="font-size: 16px; color: #1e293b; font-weight: 600; font-family: 'Inter', sans-serif;">
                                        Thêm thông tin sản phẩm
                                    </div>
                                </div>
                            </div>

                <v-row>
                    <v-col cols="12">
                        <div class="field-label">Mã</div>
                        <v-text-field v-model="product.maSanPham" readonly placeholder="(Tự sinh)" variant="outlined"
                            density="comfortable" class="custom-input mono-font bg-slate-50"
                            hide-details></v-text-field>
                    </v-col>
                    <v-col cols="12">
                        <div class="field-label">Sản phẩm <span class="text-red">*</span></div>
                        <v-text-field v-model="product.tenSanPham" placeholder="Ví dụ: Nike Mercurial Vapor 15"
                            :rules="[rules.required]" variant="outlined" density="comfortable"
                            hide-details="auto"></v-text-field>
                    </v-col>
                    <v-col cols="12" md="4">
                        <div class="field-label">Thương hiệu <span class="text-red">*</span></div>
                        <v-combobox v-model="product.idThuongHieu" :items="brands" item-title="ten" item-value="id"
                            :rules="[rules.required]" placeholder="Chọn thương hiệu..." variant="outlined"
                            density="comfortable" :return-object="false"
                            @keyup.enter="(e) => onKeyUpEnter(e, 'idThuongHieu')"></v-combobox>
                    </v-col>
                    <v-col cols="12" md="4">
                        <div class="field-label">Danh mục <span class="text-red">*</span></div>
                        <v-combobox v-model="product.idDanhMuc" :items="categories" item-title="ten" item-value="id"
                            :rules="[rules.required]" placeholder="Chọn danh mục..." variant="outlined"
                            density="comfortable" :return-object="false"
                            @keyup.enter="(e) => onKeyUpEnter(e, 'idDanhMuc')"></v-combobox>
                    </v-col>
                    <v-col cols="12" md="4">
                        <div class="field-label">Xuất xứ <span class="text-red">*</span></div>
                        <v-combobox v-model="product.idXuatXu" :items="origins" item-title="ten" item-value="id"
                            :rules="[rules.required]" placeholder="Chọn xuất xứ..." variant="outlined"
                            density="comfortable" :return-object="false"
                            @keyup.enter="(e) => onKeyUpEnter(e, 'idXuatXu')"></v-combobox>
                    </v-col>
                    <v-col cols="12" md="4">
                        <div class="field-label">Chất liệu <span class="text-red">*</span></div>
                        <v-combobox v-model="product.idChatLieu" :items="materials" item-title="ten" item-value="id"
                            :rules="[rules.required]" placeholder="Chọn chất liệu..." variant="outlined"
                            density="comfortable" :return-object="false"
                            @keyup.enter="(e) => onKeyUpEnter(e, 'idChatLieu')"></v-combobox>
                    </v-col>
                    <v-col cols="12" md="4">
                        <div class="field-label">Loại đế <span class="text-red">*</span></div>
                        <v-combobox v-model="product.idDeGiay" :items="soles" item-title="ten" item-value="id"
                            :rules="[rules.required]" placeholder="Chọn loại đế..." variant="outlined"
                            density="comfortable" :return-object="false"
                            @keyup.enter="(e) => onKeyUpEnter(e, 'idDeGiay')"></v-combobox>
                    </v-col>
                    <v-col cols="12" md="4">
                        <div class="field-label">Loại cổ <span class="text-red">*</span></div>
                        <v-combobox v-model="product.idCoGiay" :items="collars" item-title="ten" item-value="id"
                            :rules="[rules.required]" placeholder="Chọn loại cổ..." variant="outlined"
                            density="comfortable" :return-object="false"
                            @keyup.enter="(e) => onKeyUpEnter(e, 'idCoGiay')"></v-combobox>
                    </v-col>
                    <v-col cols="12" md="6">
                        <div class="field-label">Mục đích sử dụng <span class="text-red">*</span></div>
                        <v-combobox v-model="product.idMucDichChay" :items="purposes" item-title="ten" item-value="id"
                            :rules="[rules.required]" placeholder="Chọn mục đích..." variant="outlined"
                            density="comfortable" :return-object="false"
                            @keyup.enter="(e) => onKeyUpEnter(e, 'idMucDichChay')"></v-combobox>
                    </v-col>
                    <v-col cols="12" md="6">
                        <div class="field-label">Đối tượng sử dụng <span class="text-red">*</span></div>
                        <v-btn-toggle v-model="product.gioiTinhKhachHang" mandatory color="primary" variant="outlined"
                            density="comfortable" class="w-100 rounded-lg custom-toggle">
                            <v-btn value="NAM" class="flex-grow-1 font-weight-bold">Nam</v-btn>
                            <v-btn value="NU" class="flex-grow-1 font-weight-bold">Nữ</v-btn>
                            <v-btn value="UNISEX" class="flex-grow-1 font-weight-bold">Unisex</v-btn>
                        </v-btn-toggle>
                    </v-col>
                    <v-col cols="12">
                        <div class="field-label">Mô tả ngắn</div>
                        <v-textarea v-model="product.moTaNgan" variant="outlined" rows="4"
                            placeholder="Mô tả ngắn cho sản phẩm..." hide-details class="custom-textarea"></v-textarea>
                    </v-col>
                </v-row>
                </v-card-text>
                </v-card>
                </v-col>

                <v-col cols="12" lg="5">
                    <v-card class="premium-card create-shell-card mb-6">
                        <v-card-text class="pa-8">
                            <div class="section-header d-flex align-center mb-6">
                                <div class="icon-blob brand-logo-icon-blob mr-3">
                                    <BoxIcon size="18" class="brand-logo-icon" />
                                </div>
                                <div>
                                    <div style="font-size: 16px; color: #1e293b; font-weight: 600; font-family: 'Inter', sans-serif;">
                                        Biến thể sản phẩm
                                    </div>
                                </div>
                            </div>

                            <v-row>
                                <v-col cols="12">
                                    <div class="d-flex align-center justify-space-between mb-2">
                                        <div class="field-label mb-0">Màu sắc <span class="text-red">*</span></div>
                                        <v-chip size="small" variant="tonal" color="primary" class="font-weight-medium">
                                            {{ selectedColors.length }} màu
                                        </v-chip>
                                    </div>
                                    <v-select v-model="selectedColors" :items="colors" item-title="ten" item-value="id"
                                        multiple chips closable-chips variant="outlined" density="comfortable"
                                        hide-details placeholder="Chọn màu sắc"></v-select>
                                </v-col>
                                <v-col cols="12">
                                    <div class="d-flex align-center justify-space-between mb-2">
                                        <div class="field-label mb-0">Kích thước <span class="text-red">*</span></div>
                                        <v-chip size="small" variant="tonal" color="info" class="font-weight-medium">
                                            {{ selectedSizes.length }} size
                                        </v-chip>
                                    </div>
                                    <v-select v-model="selectedSizes" :items="sizes" item-title="ten" item-value="id"
                                        multiple chips closable-chips variant="outlined" density="comfortable"
                                        hide-details placeholder="Chọn kích thước"></v-select>
                                </v-col>
                            </v-row>

                            <div class="create-config-preview mt-5">
                                <div class="create-config-preview__title">Thông số sẽ áp cho biến thể</div>
                                <div class="create-config-preview__chips">
                                    <v-chip size="small" variant="outlined" class="font-weight-medium">
                                        {{ getOptionLabel(soles, product.idDeGiay) }}
                                    </v-chip>
                                    <v-chip size="small" variant="outlined" class="font-weight-medium">
                                        {{ getOptionLabel(collars, product.idCoGiay) }}
                                    </v-chip>
                                    <v-chip size="small" variant="outlined" class="font-weight-medium">
                                        {{ getOptionLabel(purposes, product.idMucDichChay) }}
                                    </v-chip>
                                </div>
                            </div>

                            <v-btn block class="mt-8 create-generate-btn text-none font-weight-bold" color="primary"
                                size="large" @click="generateVariants">
                                Tạo biến thể tự động
                            </v-btn>
                        </v-card-text>
                    </v-card>
                </v-col>
            </template>

            <template v-else>
                <v-col cols="12">
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
                                <v-col cols="12">
                                    <div class="field-label">Mô tả ngắn</div>
                                    <v-textarea v-model="product.moTaNgan" variant="outlined" rows="3"
                                        placeholder="Tóm tắt đặc điểm nổi bật..." hide-details
                                        class="custom-textarea"></v-textarea>
                                </v-col>
                            </v-row>
                        </v-card-text>
                    </v-card>


                </v-col>
            </template>

            <v-col cols="12">
                 <!-- 2. Detailed Attributes & Classification -->
                <v-card class="premium-card mb-6">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-amber-lighten-5 mr-3">
                                <SettingsIcon size="18" class="text-amber-darken-2" />
                            </div>
                            <div style="font-size: 16px; color: #1e293b; font-weight: 600; font-family: 'Inter', sans-serif;">
                                Thuộc tính & Phân loại
                            </div>
                        </div>

                        <v-row>
                            <v-col cols="12" md="6" lg="4">
                                <div class="field-label">Xuất xứ <span class="text-red">*</span></div>
                                <v-combobox v-model="product.idXuatXu" :items="origins" item-title="ten" item-value="id"
                                    :rules="[rules.required]" placeholder="Chọn xuất xứ..." variant="outlined"
                                    density="comfortable" :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idXuatXu', dichVuXuatXu, 'XUAT_XU', 'xuất xứ')"></v-combobox>
                            </v-col>
                            <v-col cols="12" md="6" lg="4">
                                <div class="field-label">Chất liệu <span class="text-red">*</span></div>
                                <v-combobox v-model="product.idChatLieu" :items="materials" item-title="ten"
                                    item-value="id" :rules="[rules.required]" placeholder="Chọn chất liệu..."
                                    variant="outlined" density="comfortable" :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idChatLieu', dichVuChatLieu, 'CHAT_LIEU', 'chất liệu')"></v-combobox>
                            </v-col>
                            <v-col cols="12" md="6" lg="4">
                                <div class="field-label">Thương hiệu <span class="text-red">*</span></div>
                                <v-combobox v-model="product.idThuongHieu" :items="brands" item-title="ten"
                                    item-value="id" :rules="[rules.required]" placeholder="Tìm thương hiệu..."
                                    variant="outlined" density="comfortable" :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idThuongHieu', dichVuThuongHieu, 'THUONG_HIEU', 'thương hiệu')"></v-combobox>
                            </v-col>
                            <v-col cols="12" md="6" lg="4">
                                <div class="field-label">Danh mục <span class="text-red">*</span></div>
                                <v-combobox v-model="product.idDanhMuc" :items="categories" item-title="ten"
                                    item-value="id" :rules="[rules.required]" placeholder="Chọn danh mục..."
                                    variant="outlined" density="comfortable" :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idDanhMuc', dichVuDanhMuc, 'DANH_MUC', 'danh mục')"></v-combobox>
                            </v-col>
                            <v-col cols="12" md="6" lg="4">
                                <div class="field-label">Loại đế <span class="text-red">*</span></div>
                                <v-combobox v-model="product.idDeGiay" :items="soles" item-title="ten" item-value="id"
                                    :rules="[rules.required]" variant="outlined" density="comfortable"
                                    :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idDeGiay', dichVuDeGiay, 'DE_GIAY', 'đế giày')"></v-combobox>
                            </v-col>
                            <v-col cols="12" md="6" lg="4">
                                <div class="field-label">Loại cổ <span class="text-red">*</span></div>
                                <v-combobox v-model="product.idCoGiay" :items="collars" item-title="ten" item-value="id"
                                    :rules="[rules.required]" variant="outlined" density="comfortable"
                                    :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idCoGiay', dichVuCoGiay, 'CO_GIAY', 'cổ giày')"></v-combobox>
                            </v-col>
                            <v-col cols="12" md="6" lg="6">
                                <div class="field-label">Mục đích sử dụng <span class="text-red">*</span></div>
                                <v-combobox v-model="product.idMucDichChay" :items="purposes" item-title="ten"
                                    item-value="id" :rules="[rules.required]" variant="outlined" density="comfortable"
                                    :return-object="false"
                                    @keyup.enter="(e) => onKeyUpEnter(e, 'idMucDichChay', dichVuMucDichChay, 'MUC_DICH_CHAY', 'mục đích sử dụng')"></v-combobox>
                            </v-col>
                            <v-col cols="12" md="6" lg="6">
                                <div class="field-label">Đối tượng sử dụng <span class="text-red">*</span></div>
                                <v-btn-toggle v-model="product.gioiTinhKhachHang" mandatory color="primary"
                                    variant="outlined" density="comfortable" class="w-100 rounded-lg custom-toggle">
                                    <v-btn value="NAM" class="flex-grow-1">Nam</v-btn>
                                    <v-btn value="NU" class="flex-grow-1">Nữ</v-btn>
                                    <v-btn value="UNISEX" class="flex-grow-1">Unisex</v-btn>
                                </v-btn-toggle>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>
                <v-card class="premium-card">
                    <v-card-text class="pa-8">
                        <template v-if="!isEditMode">
                            <div class="variant-gradient-header">
                                <div class="d-flex align-center">
                                    <v-icon icon="mdi-view-grid-plus-outline" size="18" class="mr-3" />
                                    <div>
                                        <div class="font-weight-bold" style="font-family: 'Inter', 'Outfit', sans-serif; display: flex; align-items: center;">
                                            Danh sách biến thể <span class="text-slate-400 ml-1" style="font-weight: inherit; font-family: inherit;">/ {{ variantItems.length }} biến thể</span>
                                        </div>
                                        <div class="text-caption variant-gradient-header__meta">
                                            Kho {{ totalVariantStock }}
                                        </div>
                                    </div>
                                </div>

                                <div class="d-flex align-center flex-wrap gradient-header-actions">
                                    <v-btn variant="outlined" 
                                        class="text-none rounded-pill action-btn-light" @click="applyBulkAllVariants">
                                        <v-icon icon="mdi-lightning-bolt-outline" size="16" class="mr-1" />
                                        Thêm nhanh tất cả
                                    </v-btn>
                                    <v-btn variant="outlined" 
                                        class="text-none rounded-pill action-btn-light" @click="clearAllDraftVariants">
                                        <v-icon icon="mdi-delete-outline" size="16" class="mr-1" />
                                        Xóa tất cả
                                    </v-btn>
                                </div>
                            </div>

                            <div class="variant-gradient-body">
                                <v-row class="bulk-toolbar" dense>
                                    <v-col cols="12" md="3">
                                        <v-text-field v-model.number="bulkAllForm.soLuong" type="number" min="0"
                                            variant="outlined" density="compact" hide-details
                                            placeholder="Số lượng tất cả"></v-text-field>
                                    </v-col>
                                    <v-col cols="12" md="3">
                                        <v-text-field v-model.number="bulkAllForm.giaNhap" type="number" min="0"
                                            variant="outlined" density="compact" hide-details
                                            placeholder="Giá nhập tất cả"></v-text-field>
                                    </v-col>
                                    <v-col cols="12" md="3">
                                        <v-text-field v-model.number="bulkAllForm.giaBan" type="number" min="0"
                                            variant="outlined" density="compact" hide-details
                                            placeholder="Giá bán tất cả"></v-text-field>
                                    </v-col>
                                    <v-col cols="12" md="3">
                                        <div class="bulk-toolbar__hint">
                                            Chỉ nhập trường cần áp dụng nhanh cho toàn bộ danh sách.
                                        </div>
                                    </v-col>
                                </v-row>

                                <div v-if="variantGroups.length > 0" class="variant-group-stack">
                                    <div v-for="group in variantGroups" :key="group.colorId"
                                        class="variant-color-group">
                                        <div class="variant-color-group__header">
                                            <div class="d-flex align-center">
                                                <span class="color-dot"></span>
                                                <div class="font-weight-bold text-slate-800">
                                                    {{ group.colorLabel }} <span class="text-medium-emphasis">({{
                                                        group.variants.length }} biến thể)</span>
                                                </div>
                                            </div>

                                            <div class="d-flex align-center flex-wrap group-actions">
                                                <v-text-field v-model.number="bulkByColorForms[group.colorId].soLuong"
                                                    type="number" min="0" variant="outlined" density="compact"
                                                    hide-details placeholder="SL nhóm"
                                                    class="group-quick-field"></v-text-field>
                                                <v-text-field v-model.number="bulkByColorForms[group.colorId].giaBan"
                                                    type="number" min="0" variant="outlined" density="compact"
                                                    hide-details placeholder="Giá bán nhóm"
                                                    class="group-quick-field"></v-text-field>
                                                <v-btn size="small" class="text-none rounded-pill group-action-btn"
                                                    color="primary" variant="flat"
                                                    @click="applyBulkColorVariants(group.colorId)">
                                                    <v-icon icon="mdi-lightning-bolt-outline" size="14" class="mr-1" />
                                                    Thêm nhanh
                                                </v-btn>
                                                <v-btn icon size="small" variant="text" color="error"
                                                    @click="removeColorGroup(group.colorId)">
                                                    <TrashIcon size="18" />
                                                </v-btn>
                                            </div>
                                        </div>

                                        <div class="variant-group-table">
                                            <div class="variant-group-table__head">
                                                <div>Kích cỡ</div>
                                                <div>Số lượng</div>
                                                <div>Giá nhập</div>
                                                <div>Giá bán (VNĐ)</div>
                                                <div>Hành động</div>
                                            </div>

                                            <div v-for="variant in group.variants" :key="getVariantKey(variant)"
                                                class="variant-group-row">
                                                <div class="variant-group-row__size">
                                                    <div class="font-weight-bold text-slate-800">
                                                        {{ getVariantSizeLabel(variant.idKichThuoc) }}
                                                    </div>
                                                    <div class="text-caption text-medium-emphasis">
                                                        {{ getVariantDescriptor() || 'Chưa có cấu hình bổ sung' }}
                                                    </div>
                                                </div>
                                                <div>
                                                    <v-text-field v-model.number="variantItems[variant.index].soLuong"
                                                        type="number" min="0" variant="outlined" density="compact"
                                                        hide-details placeholder="Nhập số lượng"></v-text-field>
                                                </div>
                                                <div>
                                                    <v-text-field v-model.number="variantItems[variant.index].giaNhap"
                                                        type="number" min="0" variant="outlined" density="compact"
                                                        hide-details placeholder="Nhập giá nhập"></v-text-field>
                                                </div>
                                                <div>
                                                    <v-text-field v-model.number="variantItems[variant.index].giaBan"
                                                        type="number" min="0" variant="outlined" density="compact"
                                                        hide-details placeholder="Nhập giá bán"></v-text-field>
                                                </div>
                                                <div class="variant-group-row__actions">
                                                    <v-btn icon variant="text" color="error" size="small"
                                                        @click="removeDraftVariantByIndex(variant.index)">
                                                        <TrashIcon size="18" />
                                                    </v-btn>
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    <div class="variant-image-section">
                                        <div class="variant-image-section__title">
                                            <v-icon icon="mdi-image-multiple-outline" size="18" class="mr-2" />
                                            Ảnh theo màu sắc
                                        </div>

                                        <div class="color-image-grid">
                                            <div v-for="group in variantGroups" :key="`${group.colorId}-image`"
                                                class="color-image-card">
                                                <input :ref="(el) => setColorFileInputRef(group.colorId, el)"
                                                    type="file" accept="image/*" class="d-none"
                                                    @change="handleColorImageUpload(group.colorId, $event)" />

                                                <div class="color-image-card__header">
                                                    <div class="d-flex align-center">
                                                        <span class="color-dot"></span>
                                                        <span class="font-weight-bold">{{ group.colorLabel }}</span>
                                                    </div>
                                                    <v-btn icon size="small" variant="text" color="slate-500"
                                                        @click="clearColorImage(group.colorId)">
                                                        <v-icon icon="mdi-close" size="18" />
                                                    </v-btn>
                                                </div>

                                                <div class="color-image-card__preview">
                                                    <v-img v-if="getColorUploadEntry(group.colorId).url"
                                                        :src="getColorUploadEntry(group.colorId).url" cover
                                                        class="fill-height"></v-img>
                                                    <div v-else class="color-image-card__empty">
                                                        <v-icon icon="mdi-image-outline" size="28" />
                                                        <span>Chưa có ảnh</span>
                                                    </div>
                                                </div>

                                                <v-btn block variant="text" class="text-none font-weight-medium"
                                                    :loading="getColorUploadEntry(group.colorId).uploading"
                                                    @click="openColorImagePicker(group.colorId)">
                                                    {{ getColorUploadEntry(group.colorId).url ? `Đổi ảnh
                                                    ${group.colorLabel}` : `Thêm ảnh ${group.colorLabel}` }}
                                                </v-btn>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div v-else class="variant-empty-state">
                                    <div class="variant-empty-state__icon">
                                        <BoxIcon size="28" />
                                    </div>
                                    <div class="text-subtitle-1 font-weight-bold text-slate-700 mt-4">
                                        Chưa có biến thể nào được tạo
                                    </div>
                                    <div class="text-body-2 text-slate-500 mt-2">
                                        Chọn màu sắc và kích thước ở card bên phải, sau đó bấm <span
                                            class="font-weight-bold">Tạo biến thể tự động</span>.
                                    </div>
                                </div>
                            </div>
                        </template>

                        <template v-else>
                            <div class="d-flex align-start justify-space-between flex-wrap section-toolbar">
                                <div class="section-header d-flex align-center">
                                    <div class="icon-blob bg-cyan-lighten-5 mr-3">
                                        <PhotoIcon size="18" class="text-cyan-darken-2" />
                                    </div>
                                    <div>
                                        <div style="font-size: 16px; color: #1e293b; font-weight: 600; font-family: 'Inter', 'Outfit', sans-serif; display: flex; align-items: center;">
                                            Biến thể sản phẩm <span class="text-slate-400 ml-1" style="font-weight: inherit; font-family: inherit;">/ {{ variantItems.length }} biến thể</span>
                                        </div>
                                    </div>
                                </div>

                                <div class="d-flex align-center flex-wrap section-toolbar__actions">
                                    <v-btn color="primary" variant="flat" class="text-none font-weight-bold rounded-lg"
                                        style="color: white !important;"
                                        @click="openCreateVariantModal">
                                        <PlusIcon size="18" class="mr-2" /> Thêm biến thể
                                    </v-btn>
                                </div>
                            </div>

                            <div v-if="variantItems.length > 0" class="mt-6 border rounded-lg overflow-hidden shadow-sm">
                                <table class="native-admin-table">
                                    <thead>
                                        <tr>
                                            <th class="header-cell text-center" style="width: 80px">Ảnh</th>
                                            <th class="header-cell text-left" style="width: 160px">Màu sắc</th>
                                            <th class="header-cell text-left" style="width: 130px">Kích thước</th>
                                            <th class="header-cell text-left" style="width: 160px">Mã biến thể</th>
                                            <th class="header-cell text-center" style="width: 160px">Tồn kho</th>
                                            <th class="header-cell text-left" style="width: 160px">Giá bán</th>
                                            <th class="header-cell text-center" style="width: 160px">Trạng thái</th>
                                            <th class="header-cell text-center" style="width: 120px">Hành động</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr v-for="variant in variantItems" :key="getVariantKey(variant)" class="data-row">
                                            <td class="data-cell text-center">
                                                <v-avatar rounded="lg" size="42" class="border bg-slate-50">
                                                    <v-img :src="getVariantThumbnail(variant)" cover></v-img>
                                                </v-avatar>
                                            </td>
                                            <td class="data-cell text-left text-slate-700">
                                                {{ getOptionLabel(colors, variant.idMauSac) }}
                                            </td>
                                            <td class="data-cell text-left text-slate-700">
                                                {{ getOptionLabel(sizes, variant.idKichThuoc) }}
                                            </td>
                                            <td class="data-cell text-left text-slate-600" style="font-size: 13px;">
                                                {{ getVariantSkuLabel(variant) }}
                                            </td>
                                            <td class="data-cell text-center" >{{ variant.soLuong }}</td>
                                            <td class="data-cell text-left">
                                                <div style="color: #1e257c !important;">
                                                    {{ formatCurrency(variant.giaBan) }}
                                                </div>
                                            </td>
                                            <td class="data-cell text-center">
                                                <v-chip
                                                    :class="['status-chip', variant.trangThai === defaultVariantStatus ? 'status-chip-active' : 'status-chip-inactive']"
                                                    variant="flat"
                                                    size="small"
                                                >
                                                    {{ getStatusLabel(variant.trangThai) }}
                                                </v-chip>
                                            </td>
                                            <td class="data-cell text-center">
                                                <div class="d-flex justify-center gap-1">
                                                    <v-btn icon size="x-small" variant="text" color="primary"
                                                        @click="openEditVariantModal(variant)">
                                                        <PencilIcon size="16" />
                                                    </v-btn>
                                                    <v-btn icon size="x-small" variant="text" color="error"
                                                        @click="handleRemoveVariant(variant)">
                                                        <TrashIcon size="16" />
                                                    </v-btn>
                                                </div>
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>

                            <div v-else class="variant-empty-state mt-6">
                                <div class="variant-empty-state__icon">
                                    <PhotoIcon size="28" />
                                </div>
                                <div class="text-subtitle-1 font-weight-bold text-slate-700 mt-4">
                                    Chưa có biến thể nào
                                </div>
                                <div class="text-body-2 text-slate-500 mt-2">
                                    Bạn có thể thêm biến thể mới ngay tại form này hoặc tiếp tục dùng màn quản lý biến
                                    thể riêng.
                                </div>
                                <v-btn color="primary" variant="flat" class="mt-5 rounded-lg text-none font-weight-bold"
                                    @click="openCreateVariantModal">
                                    <PlusIcon size="18" class="mr-2" /> Thêm biến thể đầu tiên
                                </v-btn>
                            </div>
                        </template>
                    </v-card-text>
                </v-card>

               
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
        top: 0;
    }
}

.header-actions__buttons :deep(.v-btn),
.create-generate-btn,
.action-btn-light,
.variant-actions :deep(.v-btn),
.variant-empty-state :deep(.v-btn) {
    font-family: 'Inter', sans-serif !important;
    font-weight: 500 !important;
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
    background: #1e257c !important;
    color: #fff !important;
    box-shadow: 0 10px 20px rgba(30, 37, 124, 0.2);
}

.field-label {
    font-size: 13.5px;
    font-weight: 600;
    color: #1e293b;
    margin-bottom: 8px;
    font-family: 'Inter', sans-serif;
}

.custom-toggle :deep(.v-btn) {
    font-family: 'Inter', sans-serif !important;
    font-weight: 500 !important;
    font-size: 13.5px !important;
    letter-spacing: -0.01em !important;
    text-transform: none !important;
}

.variant-gradient-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
    padding: 20px 24px;
    border-radius: 22px 22px 0 0;
    background: #f8fafc;
    border: 1px solid rgba(148, 163, 184, 0.16);
    border-bottom: none;
    color: #1e293b;
}

.variant-gradient-header__meta {
    color: #64748b;
}

.gradient-header-actions {
    gap: 10px;
}

.action-btn-light {
    border-color: rgba(148, 163, 184, 0.45) !important;
    color: #1e293b !important;
    font-weight: 600 !important;
}

.variant-gradient-body {
    border: 1px solid rgba(148, 163, 184, 0.16);
    border-top: none;
    border-radius: 0 0 22px 22px;
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
    border: 1px solid rgba(148, 163, 184, 0.18);
    border-radius: 20px;
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

.variant-table-wrap {
    overflow-x: auto;
    border: 1px solid rgba(148, 163, 184, 0.18);
    border-radius: 20px;
}

.variant-table :deep(table) {
    min-width: 920px;
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
    padding: 16px 20px !important;
    border-bottom: 1px solid rgba(148, 163, 184, 0.14);
    font-size: 13.5px !important;
    font-family: 'Inter', sans-serif !important;
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
</style>

