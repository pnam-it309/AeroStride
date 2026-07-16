<script setup>
/**
 * Module: Biến thể sản phẩm (Admin)
 * View: BienTheSanPham
 * Chức năng: Quản lý danh sách chi tiết các biến thể của sản phẩm. Cho phép tìm kiếm, lọc
 *            quét mã QR, tải xuống mã QR, chỉnh sửa giá/số lượng, thêm/cập nhật biến thể.
 */
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeftIcon, QrcodeIcon } from 'vue-tabler-icons';
import { ADMIN_ICONS } from '@/constants/adminIcons';
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';
import { useServerPagination } from '@/composables/useServerPagination';
import { PATH } from '@/router/routePaths';
import { useNotifications } from '@/services/notificationService';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { dichVuBienThe } from '@/services/product/dichVuBienThe';
import { dichVuMauSac, dichVuKichThuoc } from '@/services/product/dichVuThuocTinh';
import logoPlaceholder from '@/assets/images/logos/logo-light.svg';
import VariantFormModal from './components/VariantFormModal.vue';
import VariantManagementDrawer from './components/VariantManagementDrawer.vue';
import QrScanner from './components/QrScanner.vue';
import SafeProductImage from '../san-pham/components/SafeProductImage.vue';
import QrcodeVue from 'qrcode.vue';
import { exportQrImageZip } from '@/utils/qrExcelWorkbook';
import { formatCurrency, formatNumber, toNumber } from '@/utils/formatters';
import { isActiveStatus, getStatusLabel } from '@/utils/statusUtils';

const MIN_VARIANT_PRICE = 0;
const MAX_VARIANT_PRICE = 6500000;
const VARIANT_PRICE_STEP = 50000;

const router = useRouter();
const route = useRoute();
const { addNotification } = useNotifications();

const productOptions = ref([]);
const formOptions = ref({
    mauSacs: [],
    kichThuocs: [],
    trangThais: []
});
const selectedProductId = ref('');
const selectedProduct = ref(null);
const selectedVariantIds = ref([]);
const qrExportItems = ref([]);
const qrExportContainer = ref(null);
const dynamicMaxPrice = ref(MAX_VARIANT_PRICE);

const filters = reactive({
    keyword: '',
    mauSacId: '',
    kichThuocId: '',
    trangThai: '',
    khoangGia: [MIN_VARIANT_PRICE, MAX_VARIANT_PRICE]
});

// Phân trang + tải dữ liệu server-side (composable dùng chung với SanPham)
const {
    items: variants,
    loading,
    pagination,
    totalElements,
    totalPages,
    load: fetchSelectedProduct,
    reload: reloadVariants
} = useServerPagination(
    (pageable) => dichVuBienThe.layBienThePhanTrang({ ...pageable, ...buildVariantFilterParams() }),
    {
        pageSize: 5,
        onLoaded: () => updateSelectedProductMeta(),
        onError: () => {
            clearVariantSelection();
            addNotification({ title: 'Lỗi', subtitle: 'Không thể tải danh sách biến thể', color: 'error' });
        }
    }
);

const variantModal = reactive({ open: false, mode: 'create', submitting: false, variant: null });
const variantDrawer = reactive({ open: false, variant: null, initialTab: 0 });
const qrDialog = reactive({ open: false, value: '', variant: null });
const showQrScanner = ref(false);
const qrCodeWrapper = ref(null);

const confirmDialog = ref({
    show: false,
    title: '',
    message: '',
    color: 'primary',
    action: null,
    loading: false
});

// Trích xuất nội dung lỗi từ backend để hiển thị thông báo
const getBackendErrorMessage = (error, fallbackMessage) =>
    error?.response?.data?.message || error?.response?.data?.errors?.[0]?.defaultMessage || error?.message || fallbackMessage;

const selectedProductSummary = computed(() => {
    if (!selectedProductId.value || selectedProductId.value === 'ALL') {
        return {
            title: 'Tất cả sản phẩm',
            subtitle: `Tổng số ${formatNumber(totalElements.value)} biến thể`,
            maSanPham: ''
        };
    }

    if (!selectedProduct.value) {
        return { title: 'Đang tải...', subtitle: 'Vui lòng đợi', maSanPham: '' };
    }

    const maSP = selectedProduct.value.maSanPham || selectedProduct.value.ma || '';

    return {
        title: selectedProduct.value.tenSanPham,
        subtitle: `${maSP} • ${formatNumber(totalElements.value)} biến thể`,
        maSanPham: maSP
    };
});

// variants / totalElements / totalPages / pagination / loading do useServerPagination cung cấp (khai báo phía trên).
const filteredVariantIds = computed(() => variants.value.map((item) => item.id));
const selectedVariants = computed(() => variants.value.filter((item) => selectedVariantIds.value.includes(item.id)));
const allVariantsSelected = computed(
    () => filteredVariantIds.value.length > 0 && filteredVariantIds.value.every((id) => selectedVariantIds.value.includes(id))
);
const someVariantsSelected = computed(
    () => filteredVariantIds.value.some((id) => selectedVariantIds.value.includes(id)) && !allVariantsSelected.value
);

// Bỏ chọn tất cả các biến thể
const clearVariantSelection = () => {
    selectedVariantIds.value = [];
};

// Đưa các form filter về trạng thái ban đầu
const resetFilters = () => {
    filters.keyword = '';
    filters.mauSacId = '';
    filters.kichThuocId = '';
    filters.trangThai = '';
    filters.khoangGia = [MIN_VARIANT_PRICE, dynamicMaxPrice.value];
    pagination.page = 1;
    clearVariantSelection();
};

// Tải các option bộ lọc (màu sắc, kích thước, trạng thái) từ API
const fetchFormOptions = async () => {
    try {
        const response = await dichVuSanPham.layOptionsForm().catch(() => null)
        if (response) {
            formOptions.value = {
                mauSacs: response.mauSacs || [],
                kichThuocs: response.kichThuocs || [],
                trangThais: response.trangThais || [],
            }
            return
        }

        const [mauSacResponse, kichThuocResponse] = await Promise.all([
            dichVuMauSac.layMauSac({ size: 1000 }),
            dichVuKichThuoc.layKichThuoc({ size: 1000 })
        ])

        // layMauSac/layKichThuoc luôn trả PageResponse ({ content: [...] })
        formOptions.value = {
            mauSacs: mauSacResponse.content || [],
            kichThuocs: kichThuocResponse.content || [],
            trangThais: ['DANG_HOAT_DONG', 'NGUNG_HOAT_DONG']
        }
    } catch (error) {
        console.error('Lỗi khi tải options:', error)
    }
}

// Tải danh sách tên sản phẩm để hiển thị trong select box bộ lọc
const fetchProductOptions = async () => {
    try {
        const response = await dichVuSanPham.layDanhSachSanPham({ page: 0, size: 100 });
        productOptions.value = response.content || [];
    } catch (error) {
        console.error('Lỗi khi tải danh sách sản phẩm:', error);
    }
};

// Lấy mức giá biến thể cao nhất từ API để thiết lập giới hạn cho thanh trượt
const loadMaxPrice = async () => {
    try {
        const maxPrice = await dichVuSanPham.layGiaLonNhat();
        if (maxPrice !== undefined && maxPrice !== null) {
            const nextMax = Math.max(maxPrice, VARIANT_PRICE_STEP);
            const oldMax = dynamicMaxPrice.value;

            // Cập nhật giá trị Max mới
            dynamicMaxPrice.value = nextMax;

            // Nếu người dùng đang để thanh trượt ở mức Max cũ, tự động đẩy lên Max mới
            if (filters.khoangGia[1] === oldMax || filters.khoangGia[1] > nextMax) {
                filters.khoangGia = [filters.khoangGia[0], nextMax];
            }
        }
    } catch (error) {
        console.error('Error loading max price:', error);
    }
};

// Build tham số gửi lên API tìm kiếm biến thể (phân trang + lọc do BE xử lý).
// Không có sanPhamId hoặc 'ALL' => lấy toàn bộ; có => giới hạn theo 1 sản phẩm.
// page/size do composable useServerPagination tự thêm; hàm này chỉ dựng phần LỌC.
const buildVariantFilterParams = () => ({
    sortBy: 'ngayTao',
    sortDirection: 'desc',
    sanPhamId: selectedProductId.value && selectedProductId.value !== 'ALL' ? selectedProductId.value : undefined,
    keyword: filters.keyword?.trim() || undefined,
    mauSacId: filters.mauSacId || undefined,
    kichThuocId: filters.kichThuocId || undefined,
    trangThai: filters.trangThai || undefined,
    minGia: Number(filters.khoangGia[0]) > MIN_VARIANT_PRICE ? filters.khoangGia[0] : undefined,
    maxGia: Number(filters.khoangGia[1]) < dynamicMaxPrice.value ? filters.khoangGia[1] : undefined
});

// Cập nhật thông tin hiển thị (tên/mã) cho sản phẩm đang chọn ở header
const updateSelectedProductMeta = () => {
    if (!selectedProductId.value || selectedProductId.value === 'ALL') {
        selectedProduct.value = { tenSanPham: 'Tất cả sản phẩm' };
        return;
    }
    const productOption = productOptions.value.find((item) => item.id === selectedProductId.value) || {};
    const firstVariant = variants.value[0] || {};
    selectedProduct.value = {
        ...productOption,
        id: selectedProductId.value,
        tenSanPham: productOption.tenSanPham || productOption.ten || firstVariant.tenSanPham || '',
        maSanPham: productOption.maSanPham || productOption.ma || firstVariant.maSanPham || ''
    };
};

// Lấy toàn bộ biến thể khớp bộ lọc hiện tại (mọi trang) — phục vụ "chọn tất cả" và xuất QR toàn bộ
const fetchAllFilteredVariants = async () => {
    const params = { ...buildVariantFilterParams(), page: 0, size: Math.max(totalElements.value, 1) };
    const result = await dichVuBienThe.layBienThePhanTrang(params);
    return Array.isArray(result?.content) ? result.content : [];
};

// Lấy Tên option (ví dụ tên màu sắc) thông qua ID option
const getOptionLabel = (items, id) => items.find((item) => item.id === id)?.ten || '';

// Lấy ID option thông qua tên option, so sánh không phân biệt hoa thường và dấu
const getOptionIdByLabel = (items, label) => {
    const normalizedLabel = String(label ?? '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase()
        .trim();
    if (!normalizedLabel) return '';

    return (
        (items || []).find(
            (item) =>
                String(item.ten ?? '')
                    .normalize('NFD')
                    .replace(/[\u0300-\u036f]/g, '')
                    .toLowerCase()
                    .trim() === normalizedLabel
        )?.id || ''
    );
};

// Tìm kiếm url hình ảnh đại diện của một biến thể
const getVariantPrimaryImageUrl = (variant) => {
    const mainImage = variant?.images?.find((image) => image?.hinhAnhDaiDien);
    const fallbackImage = variant?.images?.[0] || (Array.isArray(variant?.hinhAnh) ? variant?.hinhAnh[0] : variant?.hinhAnh);

    return normalizeImageUrl(variant?.urlAnh) || normalizeImageUrl(mainImage) || normalizeImageUrl(fallbackImage);
};

// Build object thông tin biến thể để lưu trong state frontend (không gọi API)
const buildLocalVariantViewModel = (payload, existingVariant = {}) => ({
    ...existingVariant,
    ...payload,
    tenSanPham: existingVariant.tenSanPham || selectedProduct.value?.tenSanPham || '',
    maSanPham: existingVariant.maSanPham || selectedProduct.value?.maSanPham || '',
    tenMauSac: getOptionLabel(formOptions.value.mauSacs || [], payload.idMauSac) || existingVariant.tenMauSac || '--',
    tenKichThuoc: getOptionLabel(formOptions.value.kichThuocs || [], payload.idKichThuoc) || existingVariant.tenKichThuoc || '--',
    images:
        Array.isArray(payload.images) && payload.images.length
            ? payload.images
            : getVariantPrimaryImageUrl(payload)
                ? [{ duongDanAnh: getVariantPrimaryImageUrl(payload), hinhAnhDaiDien: true }]
                : existingVariant.images || [],
    urlAnh: getVariantPrimaryImageUrl(payload) || getVariantPrimaryImageUrl(existingVariant) || ''
});

// Cập nhật lại UI dựa theo biến thể sau khi đã cập nhật/thêm thành công
const syncVariantToLocalState = (nextVariant) => {
    if (!selectedProduct.value) return;

    const currentVariants = Array.isArray(selectedProduct.value.variants) ? [...selectedProduct.value.variants] : [];
    const variantIndex = currentVariants.findIndex((item) => item.id === nextVariant.id);

    if (variantIndex >= 0) {
        currentVariants.splice(variantIndex, 1, {
            ...currentVariants[variantIndex],
            ...nextVariant
        });
    } else {
        currentVariants.unshift(nextVariant);
    }

    selectedProduct.value = {
        ...selectedProduct.value,
        variants: currentVariants
    };
};

// Đóng Modal thêm/sửa biến thể
const closeVariantModal = () => {
    variantModal.open = false;
    variantModal.mode = 'create';
    variantModal.submitting = false;
    variantModal.variant = null;
};

// Xử lý nộp form Thêm mới hoặc Cập nhật biến thể
const handleVariantSubmit = (payload) => {
    confirmDialog.value = {
        show: true,
        title: variantModal.mode === 'create' ? 'Xác nhận thêm' : 'Xác nhận cập nhật',
        message: `Bạn có chắc chắn muốn ${variantModal.mode === 'create' ? 'thêm mới' : 'cập nhật'} biến thể này không?`,
        color: 'primary',
        action: async () => {
            confirmDialog.value.loading = true;
            variantModal.submitting = true;
            try {
                if (variantModal.mode === 'create') {
                    const createdVariant = await dichVuBienThe.taoBienThe(selectedProductId.value, buildVariantRequestPayload(payload));
                    syncVariantToLocalState(buildLocalVariantViewModel({ ...payload, ...(createdVariant || {}) }, createdVariant || {}));
                    addNotification({ title: 'Thành công', subtitle: 'Đã thêm biến thể mới', color: 'success' });
                } else {
                    const updatedVariant = await dichVuBienThe.capNhatBienThe(
                        variantModal.variant.id,
                        buildVariantRequestPayload(payload, variantModal.variant)
                    );
                    syncVariantToLocalState(
                        buildLocalVariantViewModel(
                            { ...variantModal.variant, ...payload, ...(updatedVariant || {}), id: variantModal.variant.id },
                            updatedVariant || variantModal.variant
                        )
                    );
                    addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật biến thể', color: 'success' });
                }

                closeVariantModal();
                confirmDialog.value.show = false;
                await Promise.all([fetchSelectedProduct(), loadMaxPrice()]);
            } catch (error) {
                console.error(error);
                addNotification({
                    title: 'Lỗi',
                    subtitle: getBackendErrorMessage(error, 'Thao tác thất bại'),
                    color: 'error'
                });
            } finally {
                variantModal.submitting = false;
                confirmDialog.value.loading = false;
            }
        }
    };
};

// Mở Modal chỉnh sửa nhiều ảnh biến thể
const openImageModal = (item) => {
    variantDrawer.variant = item;
    variantDrawer.initialTab = 1;
    variantDrawer.open = true;
};

// Tính toán mã SKU tự động kế tiếp cho biến thể (SP01-1, SP01-2)
const getNextSku = () => {
    const maSp = selectedProductSummary.value?.maSanPham
    if (!maSp) return ''
    let maxSuffix = 0
    const prefix = maSp + '-'
    const variants = selectedProduct.value?.variants || []
    variants.forEach(item => {
        const sku = item.maChiTietSanPham || item.sku || item.maSku || item.maBienThe || item.ma || ''
        if (sku.startsWith(prefix)) {
            const parts = sku.split('-')
            const num = parseInt(parts[parts.length - 1], 10)
            if (!isNaN(num) && num > maxSuffix) {
                maxSuffix = num
            }
        }
    })
    return `${maSp}-${maxSuffix + 1}`
}

// Mở Modal để thêm mới 1 biến thể
const openCreateVariantModal = () => {
    variantModal.variant = { maChiTietSanPham: getNextSku() }
    variantModal.mode = 'create'
    variantModal.open = true
}

// Mở Modal để chỉnh sửa 1 biến thể đã chọn
const openEditVariantModal = (item) => {
    variantModal.variant = {
        ...item,
        maChiTietSanPham: item.maChiTietSanPham || item.sku || item.maSku || item.maBienThe || item.ma || '',
        idMauSac: item.idMauSac || getOptionIdByLabel(formOptions.value.mauSacs, item.tenMauSac),
        idKichThuoc: item.idKichThuoc || getOptionIdByLabel(formOptions.value.kichThuocs, item.tenKichThuoc),
        tenMauSac: item.tenMauSac || getOptionLabel(formOptions.value.mauSacs, item.idMauSac),
        tenKichThuoc: item.tenKichThuoc || getOptionLabel(formOptions.value.kichThuocs, item.idKichThuoc),
        urlAnh: getVariantPrimaryImageUrl(item)
    };
    variantModal.mode = 'edit';
    variantModal.open = true;
};

// Chuyển hướng kết quả sau khi dùng Camera quét được mã QR
const handleQrScan = (code) => {
    if (!code) return;
    if (selectedProductId.value !== 'ALL') {
        selectedProductId.value = 'ALL';
    }
    filters.keyword = code;
    addNotification({ title: 'Quét mã thành công', subtitle: `Mã: ${code}`, color: 'success' });
};

// normalizeImageUrl đã được chuyển lên trên để dùng chung

// Chuẩn hóa một Object chứa hình ảnh hoặc chuỗi thành URL dạng Text
const normalizeImageUrl = (value) => {
    if (!value) return '';
    if (typeof value === 'string') return value;
    return value.duongDanAnh || value.url || value.fileUrl || value.secure_url || value.data || '';
};

// Lấy URL ảnh thu nhỏ của biến thể, dự phòng nếu không có
const getVariantThumbnail = (item) => {
    return getVariantPrimaryImageUrl(item) || logoPlaceholder;
};
// Lấy mã Sản phẩm gốc của một biến thể
const getVariantProductCode = (item) => {
    if (item?.maSanPham) return item.maSanPham;
    if (selectedProductSummary.value.maSanPham) return selectedProductSummary.value.maSanPham;

    const sku = String(item?.maChiTietSanPham || '').trim();
    if (sku.includes('-')) {
        return sku.split('-')[0] || '--';
    }

    return sku || '--';
};

// Lấy Text hiển thị khi quét QR (ưu tiên mã biến thể/SKU)
const getVariantQrValue = (item) => item?.maChiTietSanPham || item?.maSanPham || String(item?.id || '');

// Mở Popup xem QR code phóng to của 1 biến thể
const openQrDialog = (item) => {
    qrDialog.variant = item;
    qrDialog.value = getVariantQrValue(item);
    qrDialog.open = true;
};

// Vẽ Canvas mã QR ẩn trên UI để lấy base64 cho việc xuất Excel/ZIP
const renderVariantQrCanvases = async (variants) => {
    qrExportItems.value = variants.map((variant) => ({
        id: variant.id,
        value: getVariantQrValue(variant)
    }));

    await nextTick();
    await new Promise((resolve) => window.setTimeout(resolve, 80));

    const canvases = Array.from(qrExportContainer.value?.querySelectorAll('canvas') || []);
    const qrDataUrls = canvases.map((canvas) => canvas.toDataURL('image/png'));
    qrExportItems.value = [];
    return qrDataUrls;
};

// Gom các QR code của biến thể đang chọn thành 1 file ZIP và kích hoạt tải về
const handleExportVariantQrZip = async () => {
    if (!selectedVariantIds.value.length) {
        addNotification({
            title: 'Thông báo',
            subtitle: 'Chọn ít nhất 1 biến thể để tải mã QR',
            color: 'warning'
        });
        return;
    }

    // Lấy đầy đủ object của các biến thể đã chọn (có thể nằm ở nhiều trang khác nhau)
    const selectedIdSet = new Set(selectedVariantIds.value);
    let targetVariants = variants.value.filter((item) => selectedIdSet.has(item.id));
    if (targetVariants.length !== selectedVariantIds.value.length) {
        const all = await fetchAllFilteredVariants();
        targetVariants = all.filter((item) => selectedIdSet.has(item.id));
    }
    if (!targetVariants.length) {
        addNotification({ title: 'Thông báo', subtitle: 'Không tìm thấy biến thể đã chọn để xuất.', color: 'warning' });
        return;
    }

    try {
        const fileSuffix = selectedProductSummary.value.maSanPham || 'tat_ca';
        const qrDataUrls = await renderVariantQrCanvases(targetVariants);
        if (qrDataUrls.length !== targetVariants.length || qrDataUrls.some((item) => !item)) {
            throw new Error('Không thể tạo đủ dữ liệu QR để xuất ZIP');
        }

        exportQrImageZip({
            fileName: `qrcode_bien_the_da_chon_${fileSuffix}.zip`,
            items: targetVariants.map((variant, index) => ({
                baseName:
                    variant.maChiTietSanPham ||
                    [variant.tenSanPham, variant.tenMauSac, variant.tenKichThuoc].filter(Boolean).join('_') ||
                    `variant_${index + 1}`,
                dataUrl: qrDataUrls[index]
            }))
        });

        addNotification({
            title: 'Thành công',
            subtitle: `Đã xuất ZIP QR của ${targetVariants.length} biến thể`,
            color: 'success'
        });
    } catch (error) {
        console.error('QR ZIP export error:', error);
        qrExportItems.value = [];
        addNotification({
            title: 'Lỗi',
            subtitle: 'Không thể xuất ZIP QR của biến thể',
            color: 'error'
        });
    }
};

// Build object JSON gửi lên API thêm/sửa biến thể
const buildVariantRequestPayload = (payload, existingVariant = null) => {
    const currentImageUrl = getVariantPrimaryImageUrl(existingVariant);
    const nextImageUrl = normalizeImageUrl(payload?.urlAnh);

    const requestPayload = {
        idSanPham: selectedProductId.value !== 'ALL' ? selectedProductId.value : payload.idSanPham || null,
        maChiTietSanPham: payload.maChiTietSanPham || null,
        idMauSac: payload.idMauSac,
        idKichThuoc: payload.idKichThuoc,
        soLuong: Number(payload.soLuong ?? 0),
        giaNhap: Number(payload.giaNhap ?? 0),
        giaBan: Number(payload.giaBan ?? 0),
        trangThai: payload.trangThai || 'DANG_HOAT_DONG'
    };

    if (nextImageUrl && (!existingVariant || nextImageUrl !== currentImageUrl)) {
        requestPayload.images = [
            {
                duongDanAnh: nextImageUrl,
                moTa: `Ảnh của ${payload.maChiTietSanPham || existingVariant?.maChiTietSanPham || 'biến thể'}`,
                hinhAnhDaiDien: true
            }
        ];
    } else if (!existingVariant) {
        // Luôn gửi mảng images nếu là tạo mới để tránh lỗi backend
        requestPayload.images = [];
    }

    return requestPayload;
};

// Build object JSON cho API chỉ cập nhật trạng thái biến thể
const buildVariantStatusPayload = (variant, nextStatus) => ({
    maChiTietSanPham: variant.maChiTietSanPham || null,
    idMauSac: variant.idMauSac,
    idKichThuoc: variant.idKichThuoc,
    soLuong: Number(variant.soLuong ?? 0),
    giaNhap: Number(variant.giaNhap ?? 0),
    giaBan: Number(variant.giaGoc ?? variant.giaBan ?? 0),
    trangThai: nextStatus
});

// Thay đổi trạng thái biến thể phía Frontend (sau khi API báo thành công)
const updateVariantStatusLocally = (variantId, nextStatus) => {
    if (!selectedProduct.value?.variants?.length) return;

    selectedProduct.value = {
        ...selectedProduct.value,
        variants: selectedProduct.value.variants.map((item) => (item.id === variantId ? { ...item, trangThai: nextStatus } : item))
    };
};

// Xác nhận thay đổi bật/tắt trạng thái 1 biến thể
const handleToggleVariantStatus = (variant) => {
    const nextStatus = isActiveStatus(variant.trangThai) ? 'NGUNG_HOAT_DONG' : 'DANG_HOAT_DONG';
    confirmDialog.value = {
        show: true,
        title: 'Đổi trạng thái biến thể',
        message: `Bạn có chắc chắn muốn ${nextStatus === 'DANG_HOAT_DONG' ? 'bật' : 'tắt'} trạng thái cho biến thể này không?`,
        color: 'warning',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                await dichVuBienThe.capNhatBienThe(variant.id, buildVariantStatusPayload(variant, nextStatus));
                updateVariantStatusLocally(variant.id, nextStatus);
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

// Chọn/Bỏ chọn 1 checkbox dòng biến thể
const toggleVariantSelection = (variantId, checked) => {
    if (checked) {
        if (!selectedVariantIds.value.includes(variantId)) {
            selectedVariantIds.value = [...selectedVariantIds.value, variantId];
        }
        return;
    }

    selectedVariantIds.value = selectedVariantIds.value.filter((id) => id !== variantId);
};

// Chọn tất cả biến thể khớp bộ lọc (mọi trang, không chỉ trang hiện tại) để hỗ trợ xuất QR toàn bộ.
const toggleSelectAllVariants = async (checked) => {
    if (!checked) {
        selectedVariantIds.value = [];
        return;
    }
    try {
        const all = await fetchAllFilteredVariants();
        selectedVariantIds.value = all.map((item) => item.id);
    } catch (error) {
        // Nếu không tải được toàn bộ, tối thiểu chọn các biến thể ở trang hiện tại
        selectedVariantIds.value = filteredVariantIds.value.slice();
    }
};

// Lưu ảnh QR đang hiển thị trên Modal to xuống máy
const downloadCurrentQrCode = () => {
    const canvas = qrCodeWrapper.value?.querySelector('canvas');
    if (!canvas || !qrDialog.variant) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tạo mã QR để tải xuống', color: 'error' });
        return;
    }

    const link = document.createElement('a');
    link.href = canvas.toDataURL('image/png');
    link.download = `${getVariantQrValue(qrDialog.variant)}.png`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    addNotification({ title: 'Thành công', subtitle: 'Đã tải mã QR của biến thể', color: 'success' });
};

// Debounce reload khi đổi bộ lọc (gọi lại API server-side, về trang 1, bỏ chọn)
let variantSearchTimer = null;
const scheduleVariantReload = () => {
    if (variantSearchTimer) window.clearTimeout(variantSearchTimer);
    variantSearchTimer = window.setTimeout(() => {
        clearVariantSelection();
        reloadVariants();
    }, 300);
};

watch(
    () => [filters.keyword, filters.mauSacId, filters.kichThuocId, filters.trangThai, filters.khoangGia[0], filters.khoangGia[1]],
    scheduleVariantReload
);

watch(
    () => selectedProductId.value,
    async () => {
        clearVariantSelection();
        await reloadVariants();
    }
);

// Lắng nghe sự kiện WebSocket khi tồn kho thay đổi từ server
const handleRealtimeStockUpdate = (event) => {
    const { id, maChiTietSanPham, soLuongTon } = event.detail || {};
    if (variants.value && Array.isArray(variants.value)) {
        const item = variants.value.find(v => v.id === id || v.maChiTietSanPham === maChiTietSanPham);
        if (item) {
            item.soLuong = Number(soLuongTon ?? 0);
        }
    }
    if (selectedProduct.value && Array.isArray(selectedProduct.value.variants)) {
        const item = selectedProduct.value.variants.find(v => v.id === id || v.maChiTietSanPham === maChiTietSanPham);
        if (item) {
            item.soLuong = Number(soLuongTon ?? 0);
        }
    }
};

onMounted(async () => {
    window.addEventListener('product-stock-update', handleRealtimeStockUpdate);
    await Promise.all([loadMaxPrice(), fetchFormOptions(), fetchProductOptions()])
    const routeProductId = route.query.productId?.toString()
    selectedProductId.value = routeProductId || 'ALL'

    if (route.query.keyword) {
        filters.keyword = route.query.keyword.toString()
    }
})

onBeforeUnmount(() => {
    window.removeEventListener('product-stock-update', handleRealtimeStockUpdate);
})
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body admin-module-page"
        style="height: 100% !important; display: flex; flex-direction: column; overflow: hidden !important">
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý sản phẩm', disabled: false, href: '#' },
            { title: 'Biến thể', disabled: false, to: '/san-pham' },
            { title: selectedProductSummary.title, disabled: true }
        ]" />

        <div class="d-flex align-center justify-space-between mb-4 mt-2">
            <div class="d-flex align-center gap-4">
                <v-btn icon variant="flat" @click="router.push(PATH.SAN_PHAM)"
                    style="background-color: transparent !important; box-shadow: none !important;">
                    <ArrowLeftIcon size="20" />
                    <v-tooltip activator="parent" location="top" text="Quay lại danh sách sản phẩm" />
                </v-btn>
                <div class="text-h6 font-weight-bold text-slate-800">{{ selectedProductSummary.title }}</div>
            </div>

            <div class="d-flex gap-3">
                <v-btn color="primary" variant="flat" class="text-none rounded-lg px-4 font-weight-bold shadow"
                    @click="openCreateVariantModal" v-if="selectedProductId !== 'ALL'">
                    <v-icon icon="mdi-plus" class="mr-2" size="20" />
                    Thêm biến thể mới
                </v-btn>
            </div>
        </div>

        <div class="filter-shell">
            <AdminFilter title="Bộ lọc nâng cao" @refresh="resetFilters" :loading="loading">
                <v-col cols="12" md="3">
                    <div class="filter-field-label">Tìm kiếm nhanh</div>
                    <v-text-field v-model="filters.keyword" placeholder="Mã SKU, màu, size..."
                        prepend-inner-icon="mdi-magnify" variant="outlined" density="compact" hide-details clearable
                        class="compact-input" />
                </v-col>
                <v-col cols="12" md="2">
                    <div class="filter-field-label">Sản phẩm</div>
                    <v-autocomplete v-model="selectedProductId"
                        :items="[{ tenSanPham: 'Tất cả sản phẩm', id: 'ALL' }, ...productOptions]"
                        item-title="tenSanPham" item-value="id" variant="outlined" density="compact" hide-details
                        class="compact-input" :menu-props="{ contentClass: 'product-select-menu' }" />
                </v-col>
                <v-col cols="12" md="2">
                    <div class="filter-field-label">Màu sắc</div>
                    <v-select v-model="filters.mauSacId" :items="[
                        { title: 'Tất cả màu', value: '' },
                        ...formOptions.mauSacs.map((mauSac) => ({ title: mauSac.ten, value: mauSac.id }))
                    ]" variant="outlined" density="compact" hide-details class="compact-input"
                        :menu-props="{ contentClass: 'product-select-menu' }" />
                </v-col>
                <v-col cols="12" md="2">
                    <div class="filter-field-label">Kích thước</div>
                    <v-select v-model="filters.kichThuocId" :items="[
                        { title: 'Tất cả size', value: '' },
                        ...formOptions.kichThuocs.map((kichThuoc) => ({ title: kichThuoc.ten, value: kichThuoc.id }))
                    ]" variant="outlined" density="compact" hide-details class="compact-input"
                        :menu-props="{ contentClass: 'product-select-menu' }" />
                </v-col>
                <v-col cols="12" md="2">
                    <div class="filter-field-label">Trạng thái</div>
                    <v-select v-model="filters.trangThai" :items="[
                        { title: 'Tất cả trạng thái', value: '' },
                        ...formOptions.trangThais.map((trangThai) => ({ title: getStatusLabel(trangThai), value: trangThai }))
                    ]" variant="outlined" density="compact" hide-details class="compact-input"
                        :menu-props="{ contentClass: 'product-select-menu' }" />
                </v-col>

                <template #after>
                    <v-col cols="12" class="mt-4 pa-0">
                        <div class="d-flex align-center justify-space-between mb-2">
                            <div class="d-flex align-center gap-2">
                                <v-icon size="15" color="#3b82f6">mdi-cash-multiple</v-icon>
                                <span class="filter-range-label">Khoảng giá</span>
                            </div>
                            <span class="filter-range-value">
                                {{ formatCurrency(filters.khoangGia[0]) }} – {{ formatCurrency(filters.khoangGia[1]) }}
                            </span>
                        </div>
                        <v-range-slider :key="`${MIN_VARIANT_PRICE}-${dynamicMaxPrice}`" v-model="filters.khoangGia"
                            :max="dynamicMaxPrice" :min="MIN_VARIANT_PRICE" :step="VARIANT_PRICE_STEP" hide-details
                            color="primary" track-color="#e2e8f0" track-size="3" thumb-size="14"
                            class="blue-range-slider" />
                    </v-col>
                </template>
            </AdminFilter>
        </div>

        <AdminTable title="Danh mục biến thể" :headers="[
            { text: 'Chọn', width: '40px' },
            { text: 'STT', width: '70px' },
            { text: 'Mã sản phẩm', width: '100px' },
            { text: 'Hình ảnh', width: '70px' },
            { text: 'Mã SKU', width: '100px' },
            { text: 'Màu sắc', width: '100px' },
            { text: 'Kích thước', width: '100px' },
            { text: 'Số lượng', width: '80px' },
            { text: 'Giá bán', width: '120px' },
            { text: 'Trạng thái', width: '120px' },
            { text: 'Hành động', width: '160px' }
        ]" :items="variants" :loading="loading"
            :showAddButton="!!selectedProductId && selectedProductId !== 'ALL'" addButtonText="Tạo mới"
            @add="openCreateVariantModal" class="all-center-table">

            <template #extra-actions>
                <v-btn prepend-icon="mdi-qrcode" variant="flat" class="admin-btn-secondary"
                    @click="handleExportVariantQrZip">
                    Tải mã QR
                    <v-tooltip activator="parent" location="top" text="Tải mã QR" />
                </v-btn>

            </template>

            <template #headers>
                <tr>
                    <th class="header-cell px-0" style="width: 50px; text-align: center;">
                        <v-checkbox-btn :model-value="allVariantsSelected" :indeterminate="someVariantsSelected"
                            color="primary" hide-details density="compact"
                            style="margin: auto; display: inline-flex; width: auto;"
                            @update:model-value="toggleSelectAllVariants" />
                    </th>
                    <th class="header-cell" style="width: 70px;">STT</th>
                    <th class="header-cell" style="width: 100px;">Mã sản phẩm</th>
                    <th class="header-cell" style="width: 100px;">Hình ảnh</th>
                    <th class="header-cell" style="width: 100px;">Mã SKU</th>
                    <th class="header-cell" style="width: 100px;">Màu sắc</th>
                    <th class="header-cell" style="width: 100px;">Kích thước</th>
                    <th class="header-cell" style="width: 80px;">Số lượng</th>
                    <th class="header-cell" style="width: 120px;">Giá bán</th>
                    <th class="header-cell" style="width: 120px;">Trạng thái</th>
                    <th class="header-cell" style="width: 120px;">Hành động</th>
                </tr>
            </template>

            <template #top>
                <div
                    class="px-6 py-3 bg-slate-50 border-b d-flex align-center justify-space-between flex-wrap gap-3">
                    <div class="d-flex align-center flex-wrap gap-2">
                        <span class="text-caption font-weight-medium text-slate-500">
                            Đã chọn {{ selectedVariantIds.length }} biến thể
                        </span>
                    </div>
                </div>
            </template>

            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell px-0" style="width: 50px; text-align: center;">
                        <v-checkbox-btn :model-value="selectedVariantIds.includes(item.id)" color="primary"
                            hide-details density="compact"
                            style="margin: auto; display: inline-flex; width: auto;"
                            @update:model-value="toggleVariantSelection(item.id, $event)" />
                    </td>
                    <td class="data-cell text-center">
                        {{ (pagination.page - 1) * pagination.size + index + 1 }}
                    </td>
                    <td class="data-cell text-center">
                        <div class="text-truncate" :title="getVariantProductCode(item)">{{
                            getVariantProductCode(item) }}</div>
                    </td>
                    <td class="data-cell text-center">
                        <div class="product-image-container d-inline-block position-relative">
                            <v-avatar rounded="lg" size="44" class="border bg-slate-50 shadow-sm avatar-hover">
                                <SafeProductImage :src="getVariantThumbnail(item)" :fallback-src="logoPlaceholder"
                                    :alt="item.maChiTietSanPham || 'variant-image'" />
                            </v-avatar>
                            <div v-if="item.phanTramGiam > 0" class="discount-badge">
                                -{{ Math.round(item.phanTramGiam) }}%
                            </div>
                        </div>
                    </td>
                    <td class="data-cell text-center">
                        <div class="text-truncate" :title="item.maChiTietSanPham">
                            <span class="text-truncate">{{ item.maChiTietSanPham ?
                                item.maChiTietSanPham.split('-')[0] : '--' }}</span>
                        </div>
                    </td>
                    <td class="data-cell text-center">
                        <div class="text-truncate" :title="item.tenMauSac">
                            <span class="text-truncate">{{ item.tenMauSac }}</span>
                        </div>
                    </td>
                    <td class="data-cell text-center">
                        <div class="text-truncate" :title="item.tenKichThuoc">
                            <span class="text-truncate">{{ item.tenKichThuoc }}</span>
                        </div>
                    </td>
                    <td class="data-cell text-center">
                        <div class="text-truncate" :title="formatNumber(item.soLuong)">
                            <span class="text-truncate font-weight-medium">{{ formatNumber(item.soLuong) }}</span>
                        </div>
                    </td>
                    <td class="data-cell text-center">
                        <div class="text-primary text-truncate" :title="formatCurrency(item.giaBan)">{{
                            formatCurrency(item.giaBan) }}</div>
                    </td>
                    <td class="data-cell">
                        <v-chip variant="flat"
                            :class="['status-chip', isActiveStatus(item.trangThai) ? 'status-chip-active' : 'status-chip-inactive']">
                            {{ getStatusLabel(item.trangThai) }}
                        </v-chip>
                    </td>
                    <td class="data-cell">
                        <div class="d-flex align-center justify-center action-controls">
                            <v-btn variant="text" class="action-icon-btn action-icon-btn--edit"
                                @click="openEditVariantModal(item)">
                                <component :is="ADMIN_ICONS.ACTION.EDIT" size="15" />
                                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                            </v-btn>
                            <v-btn variant="text" class="action-icon-btn" @click="openImageModal(item)">
                                <component :is="ADMIN_ICONS.ACTION.VIEW" size="15" />
                                <v-tooltip activator="parent" location="top">Thư viện ảnh</v-tooltip>
                            </v-btn>
                            <v-btn variant="text" color="primary" class="action-icon-btn qr-action-btn"
                                @click="openQrDialog(item)">
                                <QrcodeIcon />
                                <v-tooltip activator="parent" location="top">QR code</v-tooltip>
                            </v-btn>
                            <div class="switch-wrapper">
                                <v-switch :model-value="isActiveStatus(item.trangThai)" color="primary" hide-details
                                    density="compact" class="tight-switch action-switch"
                                    @click.prevent.stop="handleToggleVariantStatus(item)" />
                                <v-tooltip activator="parent" location="top" text="Chuyển đổi trạng thái" />
                            </div>
                        </div>
                    </td>
                </tr>
            </template>

            <template #pagination>
                <AdminPagination v-model="pagination.page" :page-size="pagination.size"
                    @update:pageSize="pagination.size = $event" @change="fetchSelectedProduct"
                    :total-pages="totalPages" :total-elements="totalElements" :current-size="variants.length" />
            </template>
        </AdminTable>

        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />

        <VariantFormModal :key="`${variantModal.mode}-${variantModal.variant?.id || 'new'}`" :open="variantModal.open"
            :mode="variantModal.mode" :variant="variantModal.variant" :options="formOptions"
            :submitting="variantModal.submitting" :productCode="selectedProductSummary.maSanPham"
            @close="closeVariantModal" @submit="handleVariantSubmit" @options-refreshed="fetchFormOptions" />
        <VariantManagementDrawer v-model:show="variantDrawer.open" :variant="variantDrawer.variant"
            :initialTab="variantDrawer.initialTab" @saved="fetchSelectedProduct" />
        <QrScanner v-model:show="showQrScanner" @scan="handleQrScan" />

        <v-dialog v-model="qrDialog.open" max-width="420" transition="variant-modal-transition">
            <v-card class="rounded-xl shadow-lg border-0 overflow-hidden">
                <div class="px-6 pt-6 pb-4 d-flex justify-space-between align-start">
                    <div>
                        <div class="text-caption font-weight-bold text-slate-500 mb-1"
                            style="text-transform: none; letter-spacing: 0.5px">
                            Mã QR biến thể
                        </div>
                        <h3 class="text-h6 font-weight-bold text-slate-900 mb-1">{{
                            qrDialog.variant?.maChiTietSanPham
                            || '--' }}</h3>
                        <p class="text-body-2 text-slate-600 mb-0 text-truncate" style="max-width: 280px">
                            {{ qrDialog.variant?.tenSanPham || selectedProduct.value?.tenSanPham || 'Sản phẩm'
                            }}
                        </p>
                    </div>
                    <v-btn icon variant="tonal" size="small" color="slate-400" class="rounded-lg"
                        @click="qrDialog.open = false">
                        <v-icon size="18">mdi-close</v-icon>
                    </v-btn>
                </div>

                <div class="px-6 pb-6 bg-slate-50">
                    <div class="pt-6">
                        <div ref="qrCodeWrapper"
                            class="d-flex flex-column align-center pa-6 rounded-xl bg-white border shadow-sm">
                            <QrcodeVue :value="qrDialog.value" :size="240" level="H" render-as="canvas"
                                class="qr-canvas-display" />

                            <div
                                class="mt-5 px-4 py-2 rounded-lg bg-slate-100 border text-slate-800 font-weight-bold text-body-2 tracking-wide monospace">
                                {{ qrDialog.value }}
                            </div>
                        </div>
                    </div>

                    <div class="d-flex gap-3 mt-6">
                        <v-btn variant="flat" color="white"
                            class="text-none rounded-lg px-4 font-weight-bold border flex-grow-1"
                            @click="qrDialog.open = false">
                            Đóng
                        </v-btn>
                        <v-btn color="primary" variant="flat"
                            class="text-none rounded-lg px-4 font-weight-bold shadow flex-grow-1"
                            @click="downloadCurrentQrCode">
                            <v-icon size="18" class="mr-2">mdi-download</v-icon>
                            Tải mã QR
                        </v-btn>
                    </div>
                </div>
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
/* Vuetify cung cấp sẵn .flex-0-0, .flex-grow-1, .min-h-0 — đã loại bỏ các class thừa */

.qr-action-btn {
    border-radius: 10px;
}

.qr-action-btn:hover {
    background: rgba(var(--v-theme-primary), 0.08);
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

:deep(.v-slider-track__background) {
    height: 2px !important;
}

:deep(.v-slider-track__fill) {
    height: 2px !important;
}

:deep(.v-slider-thumb__surface) {
    width: 12px !important;
    height: 12px !important;
}

:deep(.v-slider-thumb__ripple) {
    width: 24px !important;
    height: 24px !important;
}

.product-image-container {
    position: relative;
    padding: 2px;
}

.discount-badge {
    position: absolute;
    top: -8px;
    right: -8px;
    background: #ef4444;
    color: white;
    font-size: 10px;
    font-weight: 700;
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
    z-index: 2;
    white-space: nowrap;
}

/* Style input text, placeholders and selections */
:deep(.compact-input) .v-field__input,
:deep(.compact-input) input,
:deep(.compact-input) input::placeholder,
:deep(.compact-input) .v-select__selection-text {
    font-size: 13px !important;
}

/* Style select dropdown popover option list */
:global(.product-select-menu .v-list-item-title),
:global(.product-select-menu .v-list-item) {
    font-size: 13px !important;
}
</style>
