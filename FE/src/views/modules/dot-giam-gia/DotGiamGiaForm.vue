<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { dichVuDotGiamGia } from '@/services/admin/dichVuDotGiamGia';
import { useNotifications } from '@/services/notificationService';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import { CalendarIcon, GiftIcon, InfoCircleIcon, TagIcon, BoxIcon, SearchIcon, TrashIcon } from 'vue-tabler-icons';
import { PATH } from '@/router/routePaths';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const loading = ref(false);
const saving = ref(false);
const products = ref([]); 
const searchQuery = ref('');
const searchCode = ref('');

// Pagination for Selection Table
const selectionPage = ref(1);
const selectionPageSize = ref(5);

// --- PHẦN THÊM MỚI: Khởi tạo các biến lọc cho bảng dưới ---
const detailFilters = ref({
    thuongHieu: null,
    chatLieu: null,
    kichCo: null,
    mauSac: null,
    loaiSan: null,
    khoangGia: [0, 100000000] 
});

// Pagination for Bottom Table (Selected Details)
const bottomPage = ref(1);
const bottomPageSize = ref(5);


const isEditMode = computed(() => !!route.params.id && !route.path.includes('/detail'));
const submitButtonText = computed(() => isEditMode.value ? 'Cập nhật đợt giảm giá' : 'Thêm đợt giảm giá');
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
    trangThai: 'DANG_HOAT_DONG'
});

const expandedProductIds = ref([]);
const selectedVariantsIds = ref([]); // Các biến thể thực sự được chọn để giảm giá

const baseProducts = computed(() => {
    const map = new Map();
    products.value.forEach(p => {
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
        result = result.filter(p => p.ten?.toLowerCase().includes(query));
    }
    if (searchCode.value) {
        const code = searchCode.value.toLowerCase();
        result = result.filter(p => p.ma?.toLowerCase().includes(code));
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
watch(searchQuery, () => { selectionPage.value = 1; });

const isProductSelected = (productId) => {
    const product = baseProducts.value.find(p => p.id === productId);
    if (!product || product.variants.length === 0) return false;
    return product.variants.every(v => selectedVariantsIds.value.includes(v.id));
};

const isProductIndeterminate = (productId) => {
    const product = baseProducts.value.find(p => p.id === productId);
    if (!product || product.variants.length === 0) return false;
    const selectedCount = product.variants.filter(v => selectedVariantsIds.value.includes(v.id)).length;
    return selectedCount > 0 && selectedCount < product.variants.length;
};

const toggleProductSelection = (productId) => {
    const product = baseProducts.value.find(p => p.id === productId);
    if (!product) return;
    
    const allSelected = isProductSelected(productId);
    const variantIds = product.variants.map(v => v.id);
    
    if (allSelected) {
        // Unselect all variants
        selectedVariantsIds.value = selectedVariantsIds.value.filter(vid => !variantIds.includes(vid));
    } else {
        // Select all variants
        variantIds.forEach(vid => {
            if (!selectedVariantsIds.value.includes(vid)) {
                selectedVariantsIds.value.push(vid);
            }
        });
    }
};

const isAllProductsSelected = computed(() => {
    return paginatedProductsToSelect.value.length > 0 && 
           paginatedProductsToSelect.value.every(p => isProductSelected(p.id));
});

const toggleAllProductsSelection = () => {
    if (isAllProductsSelected.value) {
        paginatedProductsToSelect.value.forEach(p => {
            const variantIds = p.variants.map(v => v.id);
            selectedVariantsIds.value = selectedVariantsIds.value.filter(vid => !variantIds.includes(vid));
        });
    } else {
        paginatedProductsToSelect.value.forEach(p => {
            p.variants.forEach(v => {
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
    return products.value.filter(p => selectedVariantsIds.value.includes(p.id));
});

const filteredSelectedDetails = computed(() => {
    let result = [...bottomTableVariants.value];
    
    const filters = detailFilters.value;
    
    // Áp dụng bộ lọc an toàn và linh hoạt
    if (filters.thuongHieu) result = result.filter(p => p.thuongHieu === filters.thuongHieu);
    if (filters.chatLieu) result = result.filter(p => p.chatLieu === filters.chatLieu);
    if (filters.kichCo) result = result.filter(p => String(p.kichCo || '').toLowerCase().includes(String(filters.kichCo).toLowerCase()));
    if (filters.mauSac) result = result.filter(p => p.color === filters.mauSac);
    if (filters.loaiSan) result = result.filter(p => p.loaiSan === filters.loaiSan);
    
    // Lọc theo giá sau giảm (Chỉ lọc nếu dải giá bị thay đổi đáng kể)
    const minPrice = filters.khoangGia[0] || 0;
    const maxPrice = filters.khoangGia[1] || 100000000;
    
    result = result.filter(p => {
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
watch(detailFilters, () => { bottomPage.value = 1; }, { deep: true });
watch(selectedVariantsIds, () => { 
    // Nếu trang hiện tại không còn dữ liệu sau khi xóa, quay về trang trước
    if (bottomPage.value > totalBottomPages.value) {
        bottomPage.value = Math.max(1, totalBottomPages.value);
    }
}, { deep: true });


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
    const allIds = filteredSelectedDetails.value.map(p => p.id);
    const allSelected = allIds.every(id => bottomTableSelection.value.includes(id));
    if (allSelected) {
        bottomTableSelection.value = bottomTableSelection.value.filter(id => !allIds.includes(id));
    } else {
        const newIds = allIds.filter(id => !bottomTableSelection.value.includes(id));
        bottomTableSelection.value.push(...newIds);
    }
};

const removeBulkSelected = () => {
    selectedVariantsIds.value = selectedVariantsIds.value.filter(id => !bottomTableSelection.value.includes(id));
    bottomTableSelection.value = [];
};

const removeAllSelected = () => {
    selectedVariantsIds.value = [];
    bottomTableSelection.value = [];
};

const init = async () => {
    try {
        const data = await dichVuDotGiamGia.layDanhSachSanPhamApDung();
        products.value = (data || []).map((p, i) => ({
            ...p,
            ma: p.maChiTietSanPham || p.ma || p.maSanPham || p.sanPhamMa || p.maSp,
            maSanPham: p.maSanPham || p.sanPhamMa || p.maSp || p.maChiTietSanPham || p.ma,
            tenSanPham: p.tenSanPham || p.tenSanPhamDayDu || p.ten,
            tenSanPhamDayDu: p.tenSanPhamDayDu || p.tenSanPham || p.ten,
            anhMauc: p.urlAnh || (p.hinhAnh?.length > 0 ? p.hinhAnh[0].url : (p.anhMauc || 'https://via.placeholder.com/40')),
            color: p.tenMauSac || p.color || '--',
            kichCo: p.tenKichThuoc || p.kichCo || '--',
            thuongHieu: p.sanPham?.thuongHieu?.ten || p.tenThuongHieu || p.thuongHieu || '--',
            chatLieu: p.sanPham?.chatLieu?.ten || p.tenChatLieu || p.chatLieu || '--',
            giaGoc: p.giaBan || 0
        }));
    } catch (e) { console.error('Error loading products:', e); }

    if (isEditMode.value || isDetailView.value) {
        loading.value = true;
        try {
            const data = await dichVuDotGiamGia.layChiTietDotGiamGia(route.params.id);
            form.value = {
                ...data,
                ngayBatDau: data.ngayBatDau ? new Date(data.ngayBatDau).toISOString().slice(0, 16) : '',
                ngayKetThuc: data.ngayKetThuc ? new Date(data.ngayKetThuc).toISOString().slice(0, 16) : ''
            };
            const applied = await dichVuDotGiamGia.layDanhSachBienTheApDung(route.params.id);
            selectedVariantsIds.value = applied.map((v) => v.id);
            
            // KHÔNG tự động 'active' ở trên để bảng dưới chỉ hiện những cái đã chọn ban đầu
            expandedProductIds.value = [];
        } catch (e) {
            addNotification({ title: 'Lỗi', subtitle: 'Không thể tải thông tin', color: 'error' });
        } finally { loading.value = false; }
    }
};

const confirmDialog = ref({ show: false, title: '', message: '', color: 'primary', action: null, loading: false });

const handleSave = () => {
    if (!form.value.ten || !form.value.soTienGiam || !form.value.ngayBatDau || !form.value.ngayKetThuc || selectedVariantsIds.value.length === 0) {
        addNotification({ title: 'Thiếu thông tin', subtitle: 'Vui lòng điền đủ các trường và chọn ít nhất 1 sản phẩm', color: 'warning' });
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
                    addNotification({ title: 'Thành công', subtitle: 'Cập nhật hoàn tất', color: 'success' });
                } else {
                    await dichVuDotGiamGia.taoDotGiamGia(payload);
                    addNotification({ title: 'Thành công', subtitle: 'Đã tạo chiến dịch mới', color: 'success' });
                }
                confirmDialog.value.show = false;
                router.push(PATH.DOT_GIAM_GIA);
            } catch (e) {
                addNotification({ title: 'Lỗi', subtitle: 'Lỗi khi lưu dữ liệu', color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
                saving.value = false;
            }
        }
    };
};

onMounted(init);
</script>

<template>
    <v-container fluid class="pa-6 animate-fade-in overflow-y-auto" style="height: 100vh;">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý đợt giảm giá', disabled: false, href: '#' },
            { title: 'Đợt giảm giá', disabled: false, to: '/dot-giam-gia' },
            { title: isDetailView ? 'Chi tiết' : isEditMode ? 'Cập nhật' : 'Thêm mới', disabled: true }
        ]" />

        <!-- Action Header -->
        <div class="d-flex align-center justify-space-between mb-8 mt-4">
            <div class="d-flex align-center gap-4">
                <v-btn icon variant="flat" @click="router.push(PATH.DOT_GIAM_GIA)" class="btn-back-header">
                    <ArrowLeftIcon size="20" />
                </v-btn>
            </div>
            <div class="d-flex gap-3">
                <v-btn v-if="!isDetailView" color="primary" variant="flat"
                    class="add-btn-primary text-white" 
                    prepend-icon="mdi-check-all"
                    @click="handleSave"
                    :loading="saving">
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
                <v-card class="premium-card mb-6 flex-grow-1" style="min-height: 580px;">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-blue-lighten-5 mr-3">
                                <v-icon color="primary" size="18">mdi-ticket-percent</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Thông tin đợt giảm giá</span>
                        </div>

                        <div class="mb-5">
                            <div class="field-label">Mã đợt giảm giá</div>
                            <v-text-field v-model="form.ma" readonly placeholder="Mã tự sinh..." variant="outlined"
                                density="comfortable" class="bg-slate-50 mono-font" hide-details></v-text-field>
                        </div>

                        <div class="mb-5">
                            <div class="field-label">Tên đợt giảm giá *</div>
                            <v-text-field v-model="form.ten" :readonly="isDetailView"
                                placeholder="Nhập tên đợt giảm giá" variant="outlined" density="comfortable"
                                hide-details></v-text-field>
                        </div>

                        <div class="mb-5">
                            <div class="field-label">Mức giảm giá (%) <span class="text-red">*</span></div>
                            <v-text-field v-model.number="form.soTienGiam" :readonly="isDetailView" type="number"
                                suffix="%" placeholder="0" variant="outlined" density="comfortable"
                                hide-details></v-text-field>
                        </div>

                        <div class="mb-5">
                            <div class="field-label">Ngày bắt đầu <span class="text-red">*</span></div>
                            <v-text-field v-model="form.ngayBatDau" :readonly="isDetailView" type="datetime-local"
                                variant="outlined" density="comfortable" hide-details></v-text-field>
                        </div>

                        <div class="mb-6">
                            <div class="field-label">Ngày kết thúc <span class="text-red">*</span></div>
                            <v-text-field v-model="form.ngayKetThuc" :readonly="isDetailView" type="datetime-local"
                                variant="outlined" density="comfortable" hide-details></v-text-field>
                        </div>
                    </v-card-text>
                </v-card>
            </v-col>

            <v-col cols="12" md="7" class="d-flex flex-column">
                <v-card class="premium-card mb-6 flex-grow-1" style="min-height: 580px;">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-amber-lighten-5 mr-3">
                                <v-icon color="amber-darken-3" size="18">mdi-package-variant</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Danh sách sản phẩm</span>
                        </div>

                        <v-row class="mb-4 d-flex align-end" no-gutters>
                            <v-col cols="12" sm="6" class="pr-sm-2">
                                <div class="field-label-small mb-1">Tên sản phẩm</div>
                                <v-text-field v-model="searchQuery" prepend-inner-icon="mdi-magnify"
                                    placeholder="Tìm tên..." variant="outlined" density="compact" hide-details
                                    class="compact-input"></v-text-field>
                            </v-col>
                            <v-col cols="12" sm="5" class="px-sm-2">
                                <div class="field-label-small mb-1">Mã sản phẩm</div>
                                <v-text-field v-model="searchCode" prepend-inner-icon="mdi-barcode-scan"
                                    placeholder="Tìm mã..." variant="outlined" density="compact" hide-details
                                    class="compact-input"></v-text-field>
                            </v-col>
                            <v-col cols="12" sm="1" class="pl-sm-1 d-flex align-center justify-end">
                                <v-btn icon variant="tonal" color="error" class="rounded-lg h-10 w-10 reset-btn" @click="searchQuery = ''; searchCode = ''; selectionPage = 1">
                                    <v-icon size="20">mdi-refresh</v-icon>
                                    <v-tooltip activator="parent" location="top">Làm mới bộ lọc</v-tooltip>
                                </v-btn>
                            </v-col>
                        </v-row>

                        <div class="table-wrapper border rounded-lg overflow-y-auto mt-4" style="height: 380px;">
                            <table class="native-admin-table">
                                <thead>
                                    <tr>
                                        <th class="header-cell text-center" style="width: 40px"></th>
                                        <th class="header-cell text-center" style="width: 60px">
                                            <v-checkbox-btn density="compact" color="primary" hide-details
                                                :model-value="isAllProductsSelected"
                                                @change="toggleAllProductsSelection"></v-checkbox-btn>
                                        </th>
                                        <th class="header-cell text-center">Mã sản phẩm</th>
                                        <th class="header-cell text-center">Tên sản phẩm</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <template v-for="(item, index) in paginatedProductsToSelect" :key="item.ma">
                                        <tr class="data-row">
                                            <td class="data-cell text-center">
                                                <v-btn icon variant="text" size="small" density="compact" @click="toggleExpand(item.id)">
                                                    <v-icon>{{ expandedProductIds.includes(item.id) ? 'mdi-minus' : 'mdi-plus' }}</v-icon>
                                                </v-btn>
                                            </td>
                                            <td class="data-cell text-center">
                                                <v-checkbox-btn :model-value="isProductSelected(item.id)"
                                                    :indeterminate="isProductIndeterminate(item.id)"
                                                    @update:model-value="toggleProductSelection(item.id)" :readonly="isDetailView"
                                                    color="primary" hide-details density="compact" class="d-inline-flex"></v-checkbox-btn>
                                            </td>
                                            <td class="data-cell text-center text-primary font-weight-bold text-slate-600">
                                                {{ item.ma }}
                                            </td>
                                            <td class="data-cell text-center font-weight-medium">
                                                {{ item.ten }}
                                            </td>
                                        </tr>
                                        <!-- Variant rows -->
                                        <tr v-if="expandedProductIds.includes(item.id)" v-for="variant in item.variants" :key="variant.id" class="variant-row bg-slate-50">
                                            <td class="data-cell text-center"></td>
                                            <td class="data-cell text-center">
                                                <v-checkbox-btn :model-value="selectedVariantsIds.includes(variant.id)"
                                                    @update:model-value="toggleVariantSelection(variant.id)" :readonly="isDetailView"
                                                    color="primary" hide-details density="compact" class="d-inline-flex"></v-checkbox-btn>
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
                        </div>

                        <AdminPagination
                            v-model="selectionPage"
                            :page-size="selectionPageSize"
                            @update:pageSize="selectionPageSize = $event"
                            :total-pages="totalSelectionPages"
                            :total-elements="filteredProductsToSelect.length"
                            :current-size="paginatedProductsToSelect.length"
                            class="mt-4"
                        />
                    </v-card-text>
                </v-card>
            </v-col>

            <v-col cols="12">
                <v-card class="premium-card">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-8">
                            <div class="icon-blob bg-emerald-lighten-5 mr-3">
                                <v-icon color="emerald-darken-2" size="18">mdi-format-list-checks</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Danh sách chi tiết sản phẩm được chọn 
                                <span class="text-primary ml-1">({{ selectedVariantsIds.length }})</span>
                            </span>
                            <v-spacer></v-spacer>
                            <v-btn v-if="bottomTableSelection.length > 0" variant="flat" color="error" class="mr-2"
                                prepend-icon="mdi-trash-can-outline" size="small" @click="removeBulkSelected" style="height: 36px; text-transform: none;">
                                Xóa đã chọn ({{ bottomTableSelection.length }})
                            </v-btn>
                            <v-btn variant="flat" class="btn-danger-outline"
                                prepend-icon="mdi-trash-can-outline" @click="removeAllSelected">
                                Xóa tất cả
                            </v-btn>
                        </div>

                        <!-- Bộ lọc sản phẩm chi tiết -->
                        <div class="detail-filter-shell mb-6">
                            <div class="detail-filter-header">
                                <v-icon size="16" class="mr-2" color="#64748b">mdi-filter-outline</v-icon>
                                <span class="filter-title-label">Bộ lọc</span>
                            </div>
                            <div class="detail-filter-body">
                                <div class="d-flex align-center" style="gap: 4px;">
                                    <div class="flex-grow-1">
                                        <div class="filter-field-label">Thương hiệu</div>
                                        <v-select v-model="detailFilters.thuongHieu" :items="['Adidas', 'Nike']"
                                            density="compact" variant="outlined" hide-details clearable
                                            placeholder="Thương hiệu" class="compact-input"></v-select>
                                    </div>
                                    <div class="flex-grow-1">
                                        <div class="filter-field-label">Chất liệu</div>
                                        <v-select v-model="detailFilters.chatLieu" :items="['Da', 'Vải', 'Vải dệt']"
                                            density="compact" variant="outlined" hide-details clearable
                                            placeholder="Chất liệu" class="compact-input"></v-select>
                                    </div>
                                    <div class="flex-grow-1">
                                        <div class="filter-field-label">Kích cỡ</div>
                                        <v-select v-model="detailFilters.kichCo" :items="[39, 40, 41, 42, 43]"
                                            density="compact" variant="outlined" hide-details clearable
                                            placeholder="Kích cỡ" class="compact-input"></v-select>
                                    </div>
                                    <div class="flex-grow-1">
                                        <div class="filter-field-label">Màu sắc</div>
                                        <v-select v-model="detailFilters.mauSac" :items="['Đen', 'Trắng', 'Xám', 'Xanh dương', 'Xanh lá']"
                                            density="compact" variant="outlined" hide-details clearable
                                            placeholder="Màu sắc" class="compact-input"></v-select>
                                    </div>
                                </div>

                                <div class="price-range-row">
                                    <div class="d-flex align-center justify-space-between mb-2">
                                        <div class="d-flex align-center gap-2">
                                            <v-icon size="15" color="#3b82f6">mdi-cash-multiple</v-icon>
                                            <span class="filter-field-label mb-0" style="margin-bottom: 0 !important;">Lọc theo giá sau giảm</span>
                                        </div>
                                        <span class="price-range-value">{{ formatCurrency(detailFilters.khoangGia[0]) }} – {{ formatCurrency(detailFilters.khoangGia[1]) }}</span>
                                    </div>
                                    <v-range-slider v-model="detailFilters.khoangGia" :max="5000000" :min="0"
                                        :step="100000" hide-details color="primary" track-color="#e2e8f0"
                                        thumb-size="18" class="blue-range-slider"></v-range-slider>
                                </div>
                            </div>
                        </div>

                        <div class="table-wrapper border rounded-lg overflow-y-auto mt-4" style="max-height: 400px;">
                            <table class="native-admin-table">
                                <thead>
                                    <tr>
                                        <th class="header-cell text-center" style="width: 50px">
                                            <v-checkbox-btn density="compact" color="primary" hide-details
                                                :model-value="filteredSelectedDetails.length > 0 && filteredSelectedDetails.every(p => bottomTableSelection.includes(p.id))"
                                                @change="toggleAllBottomSelection"></v-checkbox-btn>
                                        </th>
                                        <th class="header-cell text-center" style="width: 50px">STT</th>
                                        <th class="header-cell text-center" style="width: 80px">Ảnh</th>
                                        <th class="header-cell text-center">Mã biến thể</th>
                                        <th class="header-cell text-center">Tên sản phẩm</th>
                                        <th class="header-cell text-center">Giá bán</th>
                                        <th class="header-cell text-center">Thương hiệu</th>
                                        <th class="header-cell text-center">Chất liệu</th>
                                        <th class="header-cell text-center">Kích cỡ</th>
                                        <th class="header-cell text-center">Màu sắc</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr v-for="(item, index) in paginatedSelectedDetails" :key="item.id + '-' + item.ma + '-' + index" class="data-row">
                                        <td class="data-cell text-center">
                                            <v-checkbox-btn color="primary" hide-details density="compact" 
                                                :model-value="isVariantSelected(item.id)" @update:model-value="toggleVariantSelection(item.id)"></v-checkbox-btn>
                                        </td>
                                        <td class="data-cell text-center text-slate-500 font-weight-medium">
                                            {{ (bottomPage - 1) * bottomPageSize + index + 1 }}
                                        </td>
                                        <td class="data-cell text-center py-2">
                                            <div class="product-image-container d-inline-block position-relative">
                                                <v-avatar rounded="lg" size="44" class="border">
                                                    <v-img :src="item.anhMauc" cover></v-img>
                                                </v-avatar>
                                                <div v-if="form.soTienGiam > 0" class="discount-badge">
                                                    -{{ form.soTienGiam }}%
                                                </div>
                                            </div>
                                        </td>
                                        <td class="data-cell text-center text-primary font-weight-medium">
                                            {{ item.ma }}
                                        </td>
                                        <td class="data-cell text-center font-weight-medium">
                                            {{ item.tenSanPham }}
                                        </td>
                                        <td class="data-cell text-center">
                                            <div class="text-caption text-slate-400 text-decoration-line-through">{{
                                                formatCurrency(item.giaGoc) }}</div>
                                            <div class="text-error font-weight-bold">{{
                                                formatCurrency(calculateDiscountedPrice(item.giaGoc)) }}</div>
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
                                                <div class="color-dot" :style="{ backgroundColor: item.color === 'Xanh dương' ? '#3b82f6' : item.color === 'Xanh lá' ? '#22c55e' : item.color === 'Đen' ? '#000' : '#ccc' }"></div>
                                                <span>{{ item.color }}</span>
                                            </div>
                                        </td>
                                    </tr>
                                    <tr v-if="filteredSelectedDetails.length === 0">
                                        <td colspan="10" class="data-cell py-0">
                                            <div class="empty-state-wrapper py-16">
                                               
                                                <div class="text-body-2 text-slate-400">Không tìm thấy sản phẩm nào phù hợp.</div>
                                            </div>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
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
.color-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
}

.native-admin-table th,
.native-admin-table td {
    text-align: center !important;
    vertical-align: middle !important;
}

.native-admin-table .d-flex {
    justify-content: center !important;
}

.selection-pagination :deep(.v-pagination__list) {
    justify-content: flex-end;
}

.custom-toggle {
    height: 44px !important;
    border: 1px solid #e2e8f0 !important;
    background: #f8fafc !important;
}

.custom-toggle .v-btn {
    border-radius: 8px !important;
    border: none !important;
    height: 36px !important;
    margin: 4px !important;
    text-transform: none !important;
    font-weight: 500 !important;
}

.field-label-small {
    font-size: 12px;
    font-weight: 500;
    color: #475569;
    letter-spacing: 0.01em;
    margin-bottom: 6px;
    display: block;
}

/* --- Bộ lọc chi tiết sản phẩm --- */
.detail-filter-shell {
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    overflow: hidden;
    background: #ffffff;
}

.detail-filter-header {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid #f1f5f9;
    background: #f8fafc;
}

.filter-title-label {
    font-size: 13px;
    font-weight: 600;
    color: #475569;
    letter-spacing: 0.01em;
}

.detail-filter-body {
    padding: 16px;
}

.filter-row-inner {
    gap: 0;
    margin-bottom: 14px;

    .filter-cell {
        padding: 0 8px 0 0;

        &:last-child {
            padding-right: 0;
        }
    }
}

.price-range-row {
    border-top: 1px solid #f1f5f9;
    padding: 14px 0 0;
}

/* Blue range slider */
:deep(.blue-range-slider .v-slider-thumb .v-slider-thumb__surface) {
    background: #ffffff !important;
    border: 2.5px solid #3b82f6 !important;
    box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3) !important;
    transition: transform 0.15s ease, box-shadow 0.15s ease !important;
}

:deep(.blue-range-slider .v-slider-thumb:hover .v-slider-thumb__surface),
:deep(.blue-range-slider .v-slider-thumb--focused .v-slider-thumb__surface) {
    border-color: #2563eb !important;
    box-shadow: 0 0 0 6px rgba(59, 130, 246, 0.15) !important;
    transform: scale(1.15) !important;
}

.price-range-value {
    font-size: 12px;
    font-weight: 600;
    color: #3b82f6;
    letter-spacing: 0.01em;
    white-space: nowrap;
}

/* --- Native Admin Table Premium --- */
.native-admin-table {
    width: 100%;
    border-collapse: separate;
    border-spacing: 0;

    thead {
        position: sticky;
        top: 0;
        z-index: 10;
        background: #f8fafc;
    }

    .header-cell {
        padding: 14px 12px;
        font-size: 11px;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: #64748b;
        border-bottom: 2px solid #e2e8f0;
        white-space: nowrap;
    }

    .data-row {
        transition: background-color 0.2s ease;
        
        &:hover {
            background-color: #f1f5f9;
        }

        &:last-child .data-cell {
            border-bottom: none;
        }
    }

    .data-cell {
        padding: 12px;
        font-size: 13px;
        border-bottom: 1px solid #f1f5f9;
        vertical-align: middle;
        color: #334155;
    }
}

.empty-state-wrapper {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
}

.empty-icon-container {
    background: #f8fafc;
    width: 100px;
    height: 100px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    border: 1px dashed #cbd5e1;
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
</style>
