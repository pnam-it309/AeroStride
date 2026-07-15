<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { dichVuDotGiamGia } from '@/services/admin/dichVuDotGiamGia';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { generateRandomCode } from '@/utils/codeGenerator';
import {
    dichVuThuongHieu,
    dichVuMauSac,
    dichVuKichThuoc,
    dichVuChatLieu
} from '@/services/product/dichVuThuocTinh';
import { useNotifications } from '@/services/notificationService';
import { MESSAGES } from '@/constants/messages';
import { STATUS } from '@/utils/statusUtils';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import TableEmptyState from '@/components/common/TableEmptyState.vue';
import { CalendarIcon, GiftIcon, InfoCircleIcon, TagIcon, BoxIcon, SearchIcon, TrashIcon } from 'vue-tabler-icons';
import { PATH } from '@/router/routePaths';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

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

const loading = ref(false);
const saving = ref(false);
const products = ref([]);
const searchQuery = ref('');

// Attribute Options
const brands = ref([]);
const colors = ref([]);
const sizes = ref([]);
const materials = ref([]);

// Pagination for Selection Table
const selectionPage = ref(1);
const selectionPageSize = ref(5);

// --- PHẦN THÊM MỚI: Khởi tạo các biến lọc cho bảng dưới ---
const dynamicMaxPrice = ref(6500000); // Mặc định 6.5 triệu, sẽ cập nhật sau khi load data
const loadMaxPrice = async () => {
    try {
        const maxPrice = await dichVuSanPham.layGiaLonNhat();
        if (maxPrice !== undefined && maxPrice !== null) {
            dynamicMaxPrice.value = Math.max(maxPrice, 50000);
            detailFilters.value.khoangGia = [0, dynamicMaxPrice.value];
        }
    } catch (error) {
        console.error('Error loading max price:', error);
    }
};

const detailFilters = ref({
    timKiem: '',
    thuongHieu: null,
    chatLieu: null,
    kichCo: null,
    mauSac: null,
    loaiSan: null,
    khoangGia: [0, dynamicMaxPrice.value]
});

// Pagination for Bottom Table (Selected Details)
const bottomPage = ref(1);
const bottomPageSize = ref(5);

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

const isEditMode = computed(() => !!route.params.id && !route.path.includes('/detail'));
const submitButtonText = computed(() => (isEditMode.value ? 'Cập nhật đợt giảm giá' : 'Thêm đợt giảm giá'));
const isDetailView = computed(() => route.path.includes('/detail'));
const primaryColor = '#2E4E8E';

const form = ref({
    ma: '',
    ten: '',
    moTa: '',
    loaiGiamGia: 'PHAN_TRAM',
    soTienGiam: 0,
    dieuKienGiamGia: 0,
    mucUuTien: 0,
    ngayBatDau: '',
    ngayKetThuc: '',
    trangThai: STATUS.ACTIVE
});

const expandedProductIds = ref([]);
const selectedVariantsIds = ref([]); // Các biến thể thực sự được chọn để giảm giá

const baseProducts = computed(() => {
    const map = new Map();
    products.value.forEach((p) => {
        const prodId = p.idSanPham || p.id;
        const prodMa = p.maSanPham || p.sanPhamMa || p.maSp || p.maChiTietSanPham || p.ma;
        if (!map.has(prodId)) {
            map.set(prodId, {
                id: prodId,
                ma: prodMa,
                ten: p.tenSanPham || p.tenSanPhamDayDu,
                variants: []
            });
        }
        map.get(prodId).variants.push(p);
    });
    return Array.from(map.values());
});

const filteredProductsToSelect = computed(() => {
    let result = baseProducts.value;
    if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase();
        result = result.filter((p) => (p.ten && p.ten.toLowerCase().includes(query)) || (p.ma && p.ma.toLowerCase().includes(query)));
    }
    return result;
});

const paginatedProductsToSelect = computed(() => {
    const start = (selectionPage.value - 1) * selectionPageSize.value;
    const end = start + selectionPageSize.value;
    return filteredProductsToSelect.value.slice(start, end);
});

const totalSelectionPages = computed(() => {
    return Math.ceil(filteredProductsToSelect.value.length / selectionPageSize.value) || 1;
});

// Reset page when search changes
watch(searchQuery, () => {
    selectionPage.value = 1;
});

const isProductSelected = (productId) => {
    const product = baseProducts.value.find((p) => p.id === productId);
    if (!product || product.variants.length === 0) return false;
    return product.variants.every((v) => selectedVariantsIds.value.includes(v.id));
};

const isProductIndeterminate = (productId) => {
    const product = baseProducts.value.find((p) => p.id === productId);
    if (!product || product.variants.length === 0) return false;
    const selectedCount = product.variants.filter((v) => selectedVariantsIds.value.includes(v.id)).length;
    return selectedCount > 0 && selectedCount < product.variants.length;
};

const toggleProductSelection = (productId) => {
    const product = baseProducts.value.find((p) => p.id === productId);
    if (!product) return;

    const allSelected = isProductSelected(productId);
    const variantIds = product.variants.map((v) => v.id);

    if (allSelected) {
        // Unselect all variants
        selectedVariantsIds.value = selectedVariantsIds.value.filter((vid) => !variantIds.includes(vid));
    } else {
        // Select all variants
        variantIds.forEach((vid) => {
            if (!selectedVariantsIds.value.includes(vid)) {
                selectedVariantsIds.value.push(vid);
            }
        });
    }
};

const isAllProductsSelected = computed(() => {
    return filteredProductsToSelect.value.length > 0 && filteredProductsToSelect.value.every((p) => isProductSelected(p.id));
});

const toggleAllProductsSelection = () => {
    if (isAllProductsSelected.value) {
        filteredProductsToSelect.value.forEach((p) => {
            const variantIds = p.variants.map((v) => v.id);
            selectedVariantsIds.value = selectedVariantsIds.value.filter((vid) => !variantIds.includes(vid));
        });
    } else {
        filteredProductsToSelect.value.forEach((p) => {
            p.variants.forEach((v) => {
                if (!selectedVariantsIds.value.includes(v.id)) {
                    selectedVariantsIds.value.push(v.id);
                }
            });
        });
    }
};

const toggleExpand = (productId) => {
    const index = expandedProductIds.value.indexOf(productId);
    if (index > -1) {
        expandedProductIds.value.splice(index, 1);
    } else {
        expandedProductIds.value.push(productId);
    }
};

// Biến thể hiển thị ở bảng dưới = (Các biến thể đã được chọn) + (Các biến thể của sản phẩm đang 'mở')
const bottomTableVariants = computed(() => {
    return products.value.filter((p) => selectedVariantsIds.value.includes(p.id));
});

const filteredSelectedDetails = computed(() => {
    let result = [...bottomTableVariants.value];

    const filters = detailFilters.value;

    // Áp dụng bộ lọc an toàn và linh hoạt
    if (filters.timKiem) {
        const query = filters.timKiem.toLowerCase();
        result = result.filter(
            (p) => (p.tenSanPham && p.tenSanPham.toLowerCase().includes(query)) || (p.ma && p.ma.toLowerCase().includes(query))
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

    // Lọc theo giá sau giảm (Chỉ lọc nếu dải giá bị thay đổi đáng kể)
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

// Reset bottom page when filters change
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
        // Nếu trang hiện tại không còn dữ liệu sau khi xóa, quay về trang trước
        if (bottomPage.value > totalBottomPages.value) {
            bottomPage.value = Math.max(1, totalBottomPages.value);
        }
    },
    { deep: true }
);

const calculateDiscountedPrice = (originalPrice) => {
    return originalPrice * (1 - (form.value.soTienGiam || 0) / 100);
};

const formatCurrency = (value) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
};

const isVariantSelected = (id) => selectedVariantsIds.value.includes(id);

const toggleVariantSelection = (id) => {
    const index = selectedVariantsIds.value.indexOf(id);
    if (index > -1) {
        selectedVariantsIds.value.splice(index, 1);
    } else {
        selectedVariantsIds.value.push(id);
    }
};

const bottomTableSelection = ref([]); // Chỉ dùng cho bulk action XÓA (UI)

const toggleBottomSelection = (id) => {
    const index = bottomTableSelection.value.indexOf(id);
    if (index === -1) bottomTableSelection.value.push(id);
    else bottomTableSelection.value.splice(index, 1);
};

const toggleAllBottomSelection = () => {
    const allIds = filteredSelectedDetails.value.map((p) => p.id);
    const allSelected = allIds.every((id) => bottomTableSelection.value.includes(id));
    if (allSelected) {
        bottomTableSelection.value = bottomTableSelection.value.filter((id) => !allIds.includes(id));
    } else {
        const newIds = allIds.filter((id) => !bottomTableSelection.value.includes(id));
        bottomTableSelection.value.push(...newIds);
    }
};

const removeBulkSelected = () => {
    selectedVariantsIds.value = selectedVariantsIds.value.filter((id) => !bottomTableSelection.value.includes(id));
    bottomTableSelection.value = [];
};

const removeAllSelected = () => {
    selectedVariantsIds.value = [];
    bottomTableSelection.value = [];
};

// Hàm đồng bộ mở trình chọn ngày giờ khi bấm vào icon (giống màn NV/KH)
const openDatePicker = (event) => {
    const container = event.target.closest('.v-input');
    const input = container ? container.querySelector('input[type="datetime-local"]') : null;
    if (input) {
        if (typeof input.showPicker === 'function') input.showPicker();
        else input.click();
    }
};

const init = async () => {
    try {
        await loadMaxPrice();
        // Load attributes for filters
        const [brandData, colorData, sizeData, materialData] = await Promise.all([
            dichVuThuongHieu.layThuongHieu({ trangThai: STATUS.ACTIVE }),
            dichVuMauSac.layMauSac({ trangThai: STATUS.ACTIVE }),
            dichVuKichThuoc.layKichThuoc({ trangThai: STATUS.ACTIVE }),
            dichVuChatLieu.layChatLieu({ trangThai: STATUS.ACTIVE })
        ]);

        brands.value = (brandData?.content || brandData || []).map((b) => b.ten);
        colors.value = (colorData?.content || colorData || []).map((c) => c.ten);
        sizes.value = (sizeData?.content || sizeData || []).map((s) => s.ten);
        materials.value = (materialData?.content || materialData || []).map((m) => m.ten);

        const data = await dichVuDotGiamGia.layDanhSachSanPhamApDung();
        products.value = (data || []).map((p, i) => ({
            ...p,
            ma: p.maChiTietSanPham || p.ma || p.maSanPham || p.sanPhamMa || p.maSp,
            maSanPham: p.maSanPham || p.sanPhamMa || p.maSp || p.maChiTietSanPham || p.ma,
            tenSanPham: p.tenSanPham || p.tenSanPhamDayDu || p.ten,
            tenSanPhamDayDu: p.tenSanPhamDayDu || p.tenSanPham || p.ten,
            anhMauc: p.hinhAnh || p.urlAnh || p.anhMauc || (p.images?.length > 0 ? p.images[0].duongDanAnh : null) || p.anh || p.duongDanAnh || p.imageUrl || 'https://via.placeholder.com/40',
            color: p.tenMauSac || p.color || '--',
            kichCo: p.tenKichThuoc || p.kichCo || '--',
            thuongHieu: p.sanPham?.thuongHieu?.ten || p.tenThuongHieu || p.thuongHieu || '--',
            chatLieu: p.sanPham?.chatLieu?.ten || p.tenChatLieu || p.chatLieu || '--',
            giaGoc: p.giaBan || 0
        }));

        // Cập nhật giá lớn nhất thực tế từ danh sách sản phẩm đã load
        if (products.value.length > 0) {
            const actualMax = products.value.reduce((max, p) => Math.max(max, p.giaGoc || 0), 0);
            if (actualMax > 0) {
                dynamicMaxPrice.value = actualMax;
                // Nếu giá trị lọc hiện tại đang là mặc định hoặc lớn hơn thực tế quá nhiều, cập nhật lại
                if (detailFilters.value.khoangGia[1] >= 6500000 || detailFilters.value.khoangGia[1] === 0) {
                    detailFilters.value.khoangGia = [0, actualMax];
                }
            }
        }
    } catch (e) {
        console.error('Error loading products:', e);
    }

    if (isEditMode.value || isDetailView.value) {
        loading.value = true;
        try {
            const data = await dichVuDotGiamGia.layChiTietDotGiamGia(route.params.id);
            form.value = {
                ...data,
                ngayBatDau: data.ngayBatDau ? toLocalDatetimeString(data.ngayBatDau) : '',
                ngayKetThuc: data.ngayKetThuc ? toLocalDatetimeString(data.ngayKetThuc) : ''
            };
            const applied = await dichVuDotGiamGia.layDanhSachBienTheApDung(route.params.id);
            selectedVariantsIds.value = applied.map((v) => v.id);

            // KHÔNG tự động 'active' ở trên để bảng dưới chỉ hiện những cái đã chọn ban đầu
            expandedProductIds.value = [];
        } catch (e) {
            addNotification({ title: 'Lỗi', subtitle: MESSAGES.ERROR.LOAD_DATA, color: 'error' });
        } finally { loading.value = false; }
    } else {
        try {
            form.value.ma = await generateRandomCode('DotGiamGia');
        } catch (e) {
            console.error('Lỗi khi lấy mã', e);
        }
    }
};

const confirmDialog = ref({ show: false, title: '', message: '', color: 'primary', action: null, loading: false });

const handleSave = () => {
    const rawName = form.value.ten;
    if (!rawName || !String(rawName).trim()) {
        addNotification({ title: 'Lỗi', subtitle: 'Vui lòng nhập tên đợt giảm giá', color: 'error' });
        return;
    }
    if (String(rawName).trim().length > 255) {
        addNotification({ title: 'Lỗi', subtitle: 'Tên đợt giảm giá không được vượt quá 255 ký tự', color: 'error' });
        return;
    }
    if (!/^[\p{L}0-9\s]+$/u.test(rawName)) {
        addNotification({ title: 'Lỗi', subtitle: 'Tên đợt giảm giá không được chứa ký tự đặc biệt', color: 'error' });
        return;
    }
    if (rawName.trim() !== rawName) {
        addNotification({ title: 'Lỗi', subtitle: 'Tên đợt giảm giá không được chứa khoảng trắng ở 2 đầu', color: 'error' });
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
                const payload = {
                    ...form.value,
                    ngayBatDau: new Date(form.value.ngayBatDau).getTime(),
                    ngayKetThuc: new Date(form.value.ngayKetThuc).getTime(),
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
                addNotification({ title: 'Lỗi', subtitle: MESSAGES.ERROR.SAVE_DATA, color: 'error' });
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

const handleRefreshSearch = () => {
    searchQuery.value = '';
    selectionPage.value = 1;
};

const getExpandIcon = (id) => {
    return expandedProductIds.value.includes(id) ? 'mdi-minus' : 'mdi-plus';
};

const updateSelectionPageSize = (size) => {
    selectionPageSize.value = size;
};

onMounted(init);
</script>

<template>
    <v-container fluid class="pa-6 animate-fade-in overflow-y-auto" style="height: 100vh">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý đợt giảm giá', disabled: false, href: '#' },
            { title: 'Đợt giảm giá', disabled: false, to: PATH.DOT_GIAM_GIA },
            { title: isEditMode ? 'Cập nhật' : isDetailView ? 'Chi tiết' : 'Thêm mới', disabled: true }
        ]" />

        <!-- Action Header -->
        <div class="d-flex align-center justify-space-between mb-8 mt-4">
            <div class="d-flex align-center gap-4">
                <v-btn icon variant="flat" @click="goBack" class="btn-back-header">
                    <v-icon>mdi-arrow-left</v-icon>
                </v-btn>
            </div>
            <div class="d-flex gap-3">
                <v-btn v-if="!isDetailView" color="primary" variant="flat"
                    class="text-none px-8 campaign-submit-btn elevation-4" @click="handleSave" :loading="saving">
                    <v-icon size="18" class="mr-2">mdi-check-all</v-icon>
                    {{ submitButtonText }}
                </v-btn>
            </div>
        </div>

        <v-row v-if="loading">
            <v-col cols="12" class="text-center py-16">
                <v-progress-circular indeterminate color="primary" size="64" />
                <div class="mt-4 text-subtitle-1 font-weight-medium text-slate-500">Đang tải cấu hình...</div>
            </v-col>
        </v-row>

        <v-row v-else class="match-height-row pb-16">
            <v-col cols="12" md="5" class="d-flex flex-column">
                <v-card class="premium-card elevation-0 border border-slate-200 mb-6 flex-grow-1"
                    style="min-height: 580px">
                    <v-card-text class="pa-8 d-flex flex-column h-100">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-blue-lighten-5 mr-3">
                                <v-icon color="primary" size="18">mdi-ticket-percent</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Thông tin đợt giảm giá</span>
                        </div>

                        <div class="form-fields-container flex-grow-1 d-flex flex-column justify-center">
                            <div class="mb-5">
                                <div class="field-label">Mã đợt giảm giá</div>
                                <v-text-field v-model="form.ma" readonly placeholder="Mã tự sinh..." variant="outlined"
                                    density="compact" class="bg-slate-50 mono-font" hide-details></v-text-field>
                            </div>

                            <div class="mb-5">
                                <div class="field-label">Tên đợt giảm giá <span class="text-error">*</span></div>
                                <v-text-field v-model="form.ten" :readonly="isDetailView"
                                    :rules="[
                                        (v) => !!v || 'Vui lòng nhập tên đợt giảm giá',
                                        (v) => (v && v.trim() === v) || 'Không được chứa khoảng trắng ở 2 đầu',
                                        (v) => (v && /^[\p{L}0-9\s]+$/u.test(v)) || 'Không được chứa ký tự đặc biệt',
                                        (v) => (v && v.length <= 255) || 'Không vượt quá 255 ký tự'
                                    ]"
                                    placeholder="Nhập tên đợt giảm giá" variant="outlined" density="compact"
                                    hide-details="auto"></v-text-field>
                            </div>

                            <div class="mb-5">
                                <div class="field-label">Mức giảm giá (%) <span class="text-error">*</span></div>
                                <v-text-field :model-value="form.soTienGiam" 
                                    @update:model-value="val => { let v = Number(val); form.soTienGiam = isNaN(v) ? null : (v < 0 ? 0 : (v > 100 ? 100 : v)) }" 
                                    :readonly="isDetailView" type="number"
                                    suffix="%" placeholder="0" variant="outlined" density="compact"
                                    hide-details></v-text-field>
                            </div>

                            <div class="mb-5">
                                <div class="field-label">Ngày bắt đầu <span class="text-error">*</span></div>
                                <AppDatePicker v-model="form.ngayBatDau" :disabled="isDetailView" enable-time-picker
                                    placeholder="Chọn ngày bắt đầu" />
                            </div>

                            <div class="mb-6">
                                <div class="field-label">Ngày kết thúc <span class="text-error">*</span></div>
                                <AppDatePicker v-model="form.ngayKetThuc" :disabled="isDetailView" enable-time-picker
                                    placeholder="Chọn ngày kết thúc" />
                            </div>
                        </div>
                    </v-card-text>
                </v-card>
            </v-col>

            <v-col cols="12" md="7" class="d-flex flex-column">
                <v-card class="premium-card elevation-0 border border-slate-200 mb-6 flex-grow-1"
                    style="min-height: 580px">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-amber-lighten-5 mr-3">
                                <v-icon color="amber-darken-3" size="18">mdi-package-variant</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Danh sách sản phẩm</span>
                        </div>

                        <div class="d-flex align-center gap-3 mb-4 mt-2">
                            <span class="text-slate-800 text-no-wrap" style="font-size: 14px;">Tìm kiếm sản phẩm</span>
                            <v-text-field v-model="searchQuery" prepend-inner-icon="mdi-magnify"
                                placeholder="Tìm theo tên hoặc mã SKU..." variant="outlined" density="compact"
                                hide-details class="compact-input flex-grow-1"></v-text-field>
                            <v-btn variant="outlined" color="primary" class="reset-btn" @click="handleRefreshSearch">
                                <v-icon size="18">mdi-refresh</v-icon>
                                <v-tooltip activator="parent" location="top">Làm mới bộ lọc</v-tooltip>
                            </v-btn>
                        </div>

                        <div class="table-wrapper border rounded-lg overflow-y-auto mt-4" style="height: 380px">
                            <table class="native-admin-table">
                                <thead>
                                    <tr>
                                        <th class="header-cell text-center text-no-wrap" style="width: 40px"></th>
                                        <th class="header-cell text-center text-no-wrap" style="width: 60px">
                                            <div class="d-flex justify-center align-center">
                                                <v-checkbox-btn density="compact" color="primary" hide-details
                                                    :model-value="isAllProductsSelected"
                                                    @change="toggleAllProductsSelection"></v-checkbox-btn>
                                            </div>
                                        </th>
                                        <th class="header-cell text-center text-no-wrap">Mã sản phẩm</th>
                                        <th class="header-cell text-center text-no-wrap">Tên sản phẩm</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <template v-for="(item, index) in paginatedProductsToSelect" :key="item.ma">
                                        <tr class="data-row">
                                            <td class="data-cell text-center">
                                                <div class="d-flex align-center justify-center" style="height: 32px">
                                                    <v-btn icon variant="text" size="small" density="compact"
                                                        @click="toggleExpand(item.id)">
                                                        <v-icon>{{ getExpandIcon(item.id) }}</v-icon>
                                                    </v-btn>
                                                </div>
                                            </td>
                                            <td class="data-cell text-center">
                                                <div class="d-flex align-center justify-center" style="height: 32px">
                                                    <v-checkbox-btn :model-value="isProductSelected(item.id)"
                                                        :indeterminate="isProductIndeterminate(item.id)"
                                                        @update:model-value="toggleProductSelection(item.id)"
                                                        :readonly="isDetailView" color="primary" hide-details
                                                        density="compact" class="d-inline-flex m-0 p-0"></v-checkbox-btn>
                                                </div>
                                            </td>
                                            <td
                                                class="data-cell text-center text-primary font-weight-bold text-slate-600">
                                                {{ item.ma }}
                                            </td>
                                            <td class="data-cell text-center font-weight-medium">
                                                {{ item.ten }}
                                            </td>
                                        </tr>
                                        <!-- Variant rows -->
                                        <tr v-if="expandedProductIds.includes(item.id)" v-for="variant in item.variants"
                                            :key="variant.id" class="variant-row bg-slate-50/50">
                                            <td class="data-cell text-right pr-3"></td>
                                            <td class="data-cell text-center">
                                                <div class="d-flex align-center justify-center" style="height: 32px">
                                                    <v-checkbox-btn :model-value="selectedVariantsIds.includes(variant.id)"
                                                        @update:model-value="toggleVariantSelection(variant.id)"
                                                        :readonly="isDetailView" color="primary" hide-details
                                                        density="compact" class="d-inline-flex m-0 p-0"></v-checkbox-btn>
                                                </div>
                                            </td>
                                            <td class="data-cell text-center text-slate-500 font-weight-medium">
                                                {{ variant.ma }}
                                            </td>
                                            <td class="data-cell text-center text-slate-500">
                                                {{ variant.color }} - {{ variant.kichCo }} - {{ variant.chatLieu }}
                                            </td>
                                        </tr>
                                    </template>
                                </tbody>
                            </table>

                            <div v-if="filteredProductsToSelect.length === 0" class="d-flex flex-column align-center justify-center py-12 bg-slate-50/30 rounded-lg mx-4 my-2 border-t">
                                <v-icon icon="mdi-package-variant" size="48" style="color: #94a3b8 !important; opacity: 0.6;" class="mb-3" />
                                <span class="text-slate-500 text-center" style="font-size: 14px !important; font-weight: 400 !important; width: 100%; display: block;">Không tìm thấy sản phẩm nào.</span>
                            </div>
                        </div>

                        <AdminPagination v-model="selectionPage" :page-size="selectionPageSize"
                            @update:pageSize="updateSelectionPageSize" :total-pages="totalSelectionPages"
                            :total-elements="filteredProductsToSelect.length"
                            :current-size="paginatedProductsToSelect.length" class="mt-4" />
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
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Danh sách chi tiết sản phẩm
                                được chọn
                                <span class="text-primary ml-1">({{ selectedVariantsIds.length }})</span>
                            </span>
                            <v-spacer></v-spacer>
                            <v-btn v-if="bottomTableSelection.length > 0" variant="flat" color="primary" class="mr-2"
                                prepend-icon="mdi-trash-can-outline" size="small" @click="removeBulkSelected"
                                style="height: 36px; text-transform: none">
                                Xóa đã chọn ({{ bottomTableSelection.length }})
                            </v-btn>
                            <v-btn variant="outlined" color="primary" prepend-icon="mdi-trash-can-outline"
                                @click="removeAllSelected"
                                style="height: 36px; text-transform: none; border-width: 1px">
                                Xóa tất cả
                            </v-btn>
                        </div>

                        <!-- Bộ lọc sản phẩm chi tiết sử dụng AdminFilter chuẩn -->
                        <AdminFilter title="Bộ lọc chi tiết" class="mx-n4 bg-transparent" @refresh="handleRefreshDetailSearch">
                            <v-col cols="12" sm="2">
                                <div class="field-label-small mb-1">Tìm kiếm sản phẩm</div>
                                <v-text-field v-model="detailFilters.timKiem" prepend-inner-icon="mdi-magnify"
                                    placeholder="Tìm theo mã, tên" variant="outlined" density="compact" hide-details
                                    class="compact-input"></v-text-field>
                            </v-col>
                            <v-col cols="12" sm="2">
                                <div class="field-label-small mb-1">Thương hiệu</div>
                                <v-select v-model="detailFilters.thuongHieu" :items="brands" density="compact"
                                    variant="outlined" hide-details clearable placeholder="Thương hiệu"
                                    :menu-props="{ contentClass: 'campaign-select-menu' }"
                                    class="compact-input"></v-select>
                            </v-col>
                            <v-col cols="12" sm="2">
                                <div class="field-label-small mb-1">Chất liệu</div>
                                <v-select v-model="detailFilters.chatLieu" :items="materials" density="compact"
                                    variant="outlined" hide-details clearable placeholder="Chất liệu"
                                    :menu-props="{ contentClass: 'campaign-select-menu' }"
                                    class="compact-input"></v-select>
                            </v-col>
                            <v-col cols="12" sm="2">
                                <div class="field-label-small mb-1">Kích cỡ</div>
                                <v-select v-model="detailFilters.kichCo" :items="sizes" density="compact"
                                    variant="outlined" hide-details clearable placeholder="Kích cỡ"
                                    :menu-props="{ contentClass: 'campaign-select-menu' }"
                                    class="compact-input"></v-select>
                            </v-col>
                            <v-col cols="12" sm="2">
                                <div class="field-label-small mb-1">Màu sắc</div>
                                <v-select v-model="detailFilters.mauSac" :items="colors" density="compact"
                                    variant="outlined" hide-details clearable placeholder="Màu sắc"
                                    :menu-props="{ contentClass: 'campaign-select-menu' }"
                                    class="compact-input"></v-select>
                            </v-col>

                            <template #after>
                                <v-col cols="12" class="mt-4 pa-0">
                                    <div class="d-flex align-center justify-space-between mb-2">
                                        <div class="d-flex align-center gap-2">
                                            <v-icon size="15" color="#3b82f6">mdi-cash-multiple</v-icon>
                                            <span class="text-caption font-weight-bold text-slate-600">Lọc theo giá sau
                                                giảm</span>
                                        </div>
                                        <span class="price-range-value text-primary font-weight-bold">{{
                                            formatCurrency(detailFilters.khoangGia[0]) }} –
                                            {{ formatCurrency(detailFilters.khoangGia[1]) }}</span>
                                    </div>
                                    <v-range-slider :key="`0-${dynamicMaxPrice}`" v-model="detailFilters.khoangGia"
                                        :max="dynamicMaxPrice" :min="0" :step="10000" hide-details color="primary"
                                        track-color="#e2e8f0" track-size="2" thumb-size="14"
                                        class="blue-range-slider"></v-range-slider>
                                </v-col>
                            </template>
                        </AdminFilter>

                        <div class="table-wrapper border rounded-lg overflow-y-auto mt-4" style="max-height: 400px">
                            <table class="native-admin-table">
                                <thead>
                                    <tr>
                                        <th class="header-cell text-center text-no-wrap" style="width: 50px">
                                            <div class="d-flex justify-center align-center">
                                                <v-checkbox-btn density="compact" color="primary" hide-details :model-value="filteredSelectedDetails.length > 0 &&
                                                    filteredSelectedDetails.every((p) => bottomTableSelection.includes(p.id))
                                                    " @change="toggleAllBottomSelection"></v-checkbox-btn>
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
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr v-for="(item, index) in paginatedSelectedDetails"
                                        :key="item.id + '-' + item.ma + '-' + index" class="data-row">
                                        <td class="data-cell text-center">
                                            <div class="d-flex align-center justify-center" style="height: 32px">
                                                <v-checkbox-btn color="primary" hide-details density="compact"
                                                    :model-value="isVariantSelected(item.id)"
                                                    @update:model-value="toggleVariantSelection(item.id)"></v-checkbox-btn>
                                            </div>
                                        </td>
                                        <td class="data-cell text-center text-slate-500 font-weight-medium">
                                            {{ (bottomPage - 1) * bottomPageSize + index + 1 }}
                                        </td>
                                        <td class="data-cell text-center py-2">
                                            <div class="product-image-container d-inline-block position-relative">
                                                <v-avatar rounded="lg" size="44" class="border">
                                                    <v-img :src="item.anhMauc" cover></v-img>
                                                </v-avatar>
                                                <div v-if="form.soTienGiam > 0" class="discount-badge">-{{
                                                    form.soTienGiam }}%</div>
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
                                            <div class="d-flex align-center justify-center gap-2">
                                                <div class="color-dot" :style="{
                                                    backgroundColor:
                                                        item.color === 'Xanh dương'
                                                            ? '#3b82f6'
                                                            : item.color === 'Xanh lá'
                                                                ? '#22c55e'
                                                                : item.color === 'Đen'
                                                                    ? '#000'
                                                                    : '#ccc'
                                                }"></div>
                                                <span>{{ item.color }}</span>
                                            </div>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>

                            <div v-if="filteredSelectedDetails.length === 0" class="d-flex flex-column align-center justify-center py-12 bg-slate-50/30 rounded-lg mx-4 my-2 border-t">
                                <v-icon icon="mdi-package-variant" size="48" style="color: #94a3b8 !important; opacity: 0.6;" class="mb-3" />
                                <span class="text-slate-500 text-center" style="font-size: 14px !important; font-weight: 400 !important; width: 100%; display: block;">Không tìm thấy sản phẩm nào phù hợp.</span>
                            </div>
                        </div>

                        <AdminPagination v-model="bottomPage" :page-size="bottomPageSize"
                            @update:pageSize="bottomPageSize = $event" :total-pages="totalBottomPages"
                            :total-elements="filteredSelectedDetails.length"
                            :current-size="paginatedSelectedDetails.length" class="mt-4" />
                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

        <AdminConfirm v-model:show="confirmDialog.show" v-bind="confirmDialog" @confirm="confirmDialog.action" />
    </v-container>
</template>

<style scoped>
/* All styles migrated to _admin-common.scss under .admin-module-page */

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

