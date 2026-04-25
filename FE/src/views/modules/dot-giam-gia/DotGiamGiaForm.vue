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
const selectedVariantsIds = ref([]); 
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
    khoangGia: [0, 5000000] // Giá từ 0đ đến 5tr
});

const isEditMode = computed(() => !!route.params.id && !route.path.includes('/detail'));
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

const filteredProductsToSelect = computed(() => {
    let result = products.value;
    if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase();
        result = result.filter(p => 
            p.tenSanPhamDayDu?.toLowerCase().includes(query)
        );
    }
    if (searchCode.value) {
        const code = searchCode.value.toLowerCase();
        result = result.filter(p => 
            p.ma?.toLowerCase().includes(code)
        );
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

const selectedProductsDetails = computed(() => {
    return products.value.filter(p => selectedVariantsIds.value.includes(p.id));
});

// --- PHẦN THÊM MỚI: Logic lọc cho bảng chi tiết phía dưới ---
const filteredSelectedDetails = computed(() => {
    return selectedProductsDetails.value.filter(p => {
        const giaSauGiam = calculateDiscountedPrice(p.giaGoc);
        
        const matchPrice = giaSauGiam >= detailFilters.value.khoangGia[0] && 
                          giaSauGiam <= detailFilters.value.khoangGia[1];
        
        const matchBrand = !detailFilters.value.thuongHieu || p.thuongHieu === detailFilters.value.thuongHieu;
        const matchSize = !detailFilters.value.kichCo || p.kichCo === detailFilters.value.kichCo;
        
        return matchPrice && matchBrand && matchSize;
    });
});

const calculateDiscountedPrice = (originalPrice) => {
    return originalPrice * (1 - (form.value.soTienGiam || 0) / 100);
};

const formatCurrency = (value) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
};

const toggleProduct = (id) => {
    const index = selectedVariantsIds.value.indexOf(id);
    if (index > -1) {
        selectedVariantsIds.value.splice(index, 1);
    } else {
        selectedVariantsIds.value.push(id);
    }
};

const removeSelectedProduct = (id) => {
    const index = selectedVariantsIds.value.indexOf(id);
    if (index > -1) {
        selectedVariantsIds.value.splice(index, 1);
    }
};

const removeAllSelected = () => {
    selectedVariantsIds.value = [];
};

const init = async () => {
    try {
        const data = await dichVuDotGiamGia.layDanhSachSanPhamApDung();
        products.value = (data || []).map(p => ({
            ...p,
            anhMauc: p.anhMauc || 'https://via.placeholder.com/40',
            color: p.color || 'Xanh dương',
            kichCo: p.kichCo || 41,
            loaiSan: p.loaiSan || 'Sân cỏ nhân tạo (TF)',
            giaGoc: p.giaBan || 2000000
        }));
    } catch (e) {
        console.error('Error loading products:', e);
    }

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
        } catch (e) {
            addNotification({ title: 'Lỗi', subtitle: 'Không thể tải thông tin', color: 'error' });
        } finally {
            loading.value = false;
        }
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
                    {{ isEditMode ? 'Cập nhật đợt giảm giá' : 'Kích hoạt đợt giảm giá' }}
                </v-btn>
            </div>
        </div>

        <v-row v-if="loading">
            <v-col cols="12" class="text-center py-16">
                <v-progress-circular indeterminate color="primary" size="64" />
                <div class="mt-4 text-subtitle-1 font-weight-bold text-slate-500">Đang tải cấu hình...</div>
            </v-col>
        </v-row>

        <v-row v-else class="match-height-row pb-16">
            <v-col cols="12" md="4" class="d-flex flex-column">
                <v-card class="premium-card mb-6 flex-grow-1">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-blue-lighten-5 mr-3">
                                <v-icon color="primary" size="18">mdi-information-variant</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-black text-slate-800">Thông tin cơ bản</span>
                        </div>

                        <div class="mb-4">
                            <div class="field-label">Mã đợt (Tự động sinh)</div>
                            <v-text-field v-model="form.ma" readonly placeholder="Mã tự sinh..." variant="outlined"
                                density="comfortable" class="bg-slate-50 mono-font" hide-details></v-text-field>
                        </div>

                        <div class="mb-4">
                            <div class="field-label">Tên đợt giảm giá *</div>
                            <v-text-field v-model="form.ten" :readonly="isDetailView"
                                placeholder="Nhập tên đợt giảm giá" variant="outlined" density="comfortable"
                                hide-details></v-text-field>
                        </div>

                        <div class="mb-4">
                            <div class="field-label">Giá trị giảm (%) *</div>
                            <v-text-field v-model.number="form.soTienGiam" :readonly="isDetailView" type="number"
                                suffix="%" placeholder="0" variant="outlined" density="comfortable"
                                hide-details></v-text-field>
                        </div>

                        <div class="mb-4">
                            <div class="field-label">Ngày bắt đầu *</div>
                            <v-text-field v-model="form.ngayBatDau" :readonly="isDetailView" type="datetime-local"
                                variant="outlined" density="comfortable" hide-details></v-text-field>
                        </div>

                        <div class="mb-6">
                            <div class="field-label">Ngày kết thúc *</div>
                            <v-text-field v-model="form.ngayKetThuc" :readonly="isDetailView" type="datetime-local"
                                variant="outlined" density="comfortable" hide-details></v-text-field>
                        </div>
                    </v-card-text>
                </v-card>
            </v-col>

            <v-col cols="12" md="8" class="d-flex flex-column">
                <v-card class="premium-card mb-6 flex-grow-1">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-amber-lighten-5 mr-3">
                                <v-icon color="amber-darken-3" size="18">mdi-package-variant-closed</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-black text-slate-800">Chọn sản phẩm áp dụng</span>
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
                                <v-btn icon variant="tonal" color="primary" class="rounded-lg h-10 w-10 font-weight-bold reset-btn" @click="searchQuery = ''; searchCode = ''; selectionPage = 1">
                                    <v-icon size="20">mdi-refresh</v-icon>
                                    <v-tooltip activator="parent" location="top">Làm mới bộ lọc</v-tooltip>
                                </v-btn>
                            </v-col>
                        </v-row>

                        <v-table class="modern-table border rounded-lg overflow-hidden" height="300px" fixed-header>
                            <thead>
                                <tr>
                                    <th class="text-center" style="width: 50px">
                                        <v-checkbox-btn density="compact" color="primary" hide-details
                                            @change="e => { if (e.target.checked) selectedVariantsIds = [...new Set([...selectedVariantsIds, ...paginatedProductsToSelect.map(p => p.id)])]; else selectedVariantsIds = selectedVariantsIds.filter(id => !paginatedProductsToSelect.map(p => p.id).includes(id)) }"></v-checkbox-btn>
                                    </th>
                                    <th style="width: 120px">Mã SP</th>
                                    <th>Tên sản phẩm</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="(item, index) in paginatedProductsToSelect" :key="item.id">
                                    <td class="text-center py-2" style="vertical-align: middle;">
                                        <v-checkbox-btn :model-value="selectedVariantsIds.includes(item.id)"
                                            @update:model-value="toggleProduct(item.id)" :readonly="isDetailView"
                                            color="primary" hide-details density="compact" class="d-inline-flex"></v-checkbox-btn>
                                    </td>
                                    <td class="font-weight-bold text-slate-400 text-caption py-2" style="vertical-align: middle;">{{ item.ma || 'SP' +
                                        String(index + 1).padStart(5, '0') }}</td>
                                    <td class="font-weight-bold py-2" style="vertical-align: middle;">{{ item.tenSanPhamDayDu }}</td>
                                </tr>
                            </tbody>
                        </v-table>

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
                            <span class="text-subtitle-1 font-weight-black text-slate-800">Danh sách sản phẩm được
                                chọn</span>
                            <v-chip color="primary" size="small" class="ml-3 font-weight-black px-4">{{
                                selectedVariantsIds.length }} sản phẩm</v-chip>
                            <v-spacer></v-spacer>
                            <v-btn variant="flat" class="btn-danger-outline"
                                prepend-icon="mdi-trash-can-outline" @click="removeAllSelected">
                                Xóa tất cả
                            </v-btn>
                        </div>

                        <v-row class="mb-6 bg-slate-50 pa-6 rounded-lg border border-dashed">
                            <v-col cols="12" md="3">
                                <div class="field-label-small">Thương hiệu</div>
                                <v-select v-model="detailFilters.thuongHieu" :items="['Adidas', 'Nike']"
                                    density="comfortable" variant="outlined" hide-details clearable
                                    placeholder="Tất cả"></v-select>
                            </v-col>
                            <v-col cols="12" md="3">
                                <div class="field-label-small">Chất liệu</div>
                                <v-select v-model="detailFilters.chatLieu" :items="['Da', 'Vải']"
                                    density="comfortable" variant="outlined" hide-details clearable
                                    placeholder="Tất cả"></v-select>
                            </v-col>
                            <v-col cols="12" md="3">
                                <div class="field-label-small">Kích cỡ</div>
                                <v-select v-model="detailFilters.kichCo" :items="[39, 40, 41, 42, 43]"
                                    density="comfortable" variant="outlined" hide-details clearable
                                    placeholder="Tất cả"></v-select>
                            </v-col>
                            <v-col cols="12" md="3">
                                <div class="field-label-small">Màu sắc</div>
                                <v-select v-model="detailFilters.mauSac" :items="['Đen', 'Trắng', 'Xám', 'Xanh']"
                                    density="comfortable" variant="outlined" hide-details clearable
                                    placeholder="Tất cả"></v-select>
                            </v-col>
                            <v-col cols="12" class="pt-2">
                                <div class="d-flex align-center bg-white pa-4 rounded-lg border">
                                    <v-icon size="20" class="mr-4 text-primary">mdi-cash-multiple</v-icon>
                                    <div class="flex-grow-1">
                                        <div class="d-flex justify-space-between mb-2">
                                            <span class="text-caption font-weight-black text-slate-600">Lọc theo giá sau giảm</span>
                                            <span class="text-caption font-weight-black text-primary">{{
                                                formatCurrency(detailFilters.khoangGia[0]) }} - {{
                                                formatCurrency(detailFilters.khoangGia[1]) }}</span>
                                        </div>
                                        <v-range-slider v-model="detailFilters.khoangGia" :max="5000000" :min="0"
                                            :step="100000" hide-details color="primary" track-color="slate-200"
                                            size="small"></v-range-slider>
                                    </div>
                                </div>
                            </v-col>
                        </v-row>

                        <v-table class="modern-table border rounded-lg overflow-hidden" fixed-header height="400px">
                            <thead>
                                <tr>
                                    <th class="text-center" style="width: 48px">#</th>
                                    <th class="text-center" style="width: 80px">Ảnh</th>
                                    <th>Thông tin sản phẩm</th>
                                    <th class="text-right">Giá bán</th>
                                    <th class="text-center">Thuộc tính</th>
                                    <th class="text-center" style="width: 80px">Xóa</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="(item, index) in filteredSelectedDetails" :key="item.id">
                                    <td class="text-center text-slate-400 font-weight-bold">{{ index + 1 }}</td>
                                    <td class="text-center py-2">
                                        <div class="product-image-container d-inline-block position-relative">
                                            <v-avatar rounded="lg" size="48" class="border">
                                                <v-img :src="item.anhMauc" cover></v-img>
                                            </v-avatar>
                                            <div v-if="form.soTienGiam > 0" class="discount-badge">
                                                -{{ form.soTienGiam }}%
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <div class="font-weight-black text-slate-800">{{ item.tenSanPhamDayDu }}</div>
                                        <div class="text-caption font-weight-bold text-primary">{{ item.ma }}</div>
                                    </td>
                                    <td class="text-right">
                                        <div class="text-caption text-slate-400 text-decoration-line-through">{{
                                            formatCurrency(item.giaGoc) }}</div>
                                        <div class="font-weight-black text-error text-subtitle-2">{{
                                            formatCurrency(calculateDiscountedPrice(item.giaGoc)) }}</div>
                                    </td>
                                    <td>
                                        <div class="d-flex align-center justify-center gap-2">
                                            <v-chip size="x-small" variant="tonal" class="font-weight-bold">Size {{
                                                item.kichCo }}</v-chip>
                                            <v-chip size="x-small" variant="tonal" class="font-weight-bold">{{ item.color
                                                }}</v-chip>
                                        </div>
                                    </td>
                                    <td class="text-center">
                                        <v-btn icon variant="text" color="error" size="small"
                                            @click="removeSelectedProduct(item.id)" class="action-icon-btn">
                                            <TrashIcon size="18" />
                                        </v-btn>
                                    </td>
                                </tr>
                                <tr v-if="filteredSelectedDetails.length === 0">
                                    <td colspan="6" class="text-center py-12 text-slate-400 bg-slate-50">
                                        <v-icon size="40" class="mb-2 opacity-50">mdi-package-variant</v-icon>
                                        <div>Không tìm thấy sản phẩm nào phù hợp.</div>
                                    </td>
                                </tr>
                            </tbody>
                        </v-table>
                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

        <AdminConfirm v-model:show="confirmDialog.show" v-bind="confirmDialog" @confirm="confirmDialog.action" />
    </v-container>
</template>

<style scoped>
/* Local style overrides - using global premium design system */
:deep(.v-container.font-body),
:deep(.v-card),
:deep(.v-table),
:deep(.v-table__wrapper table),
:deep(.v-table__wrapper table tbody tr td),
:deep(.v-table__wrapper table thead tr th),
:deep(.v-label),
.field-label,
.field-label-small {
    font-size: 13px !important;
    text-transform: none !important;
    letter-spacing: normal !important;
}

/* Làm nhạt màu chữ trong các ô input */
:deep(.v-field__input), 
:deep(.v-select__selection), 
:deep(.v-text-field input),
:deep(.v-textarea textarea),
:deep(input),
:deep(textarea) {
    color: #475569 !important;
    font-size: 13px !important;
}

:deep(.v-table__wrapper table tbody tr td *) {
    font-size: 13px !important;
    font-weight: 500 !important;
    color: #1d2025 !important;
}

:deep(.v-table__wrapper table thead tr th *) {
    color: #64748b !important;
    font-weight: 700 !important;
}

.modern-table :deep(td) {
    vertical-align: middle !important;
    padding-top: 8px !important;
    padding-bottom: 8px !important;
}

.modern-table :deep(.v-checkbox-btn) {
    min-height: auto !important;
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
    font-weight: 700 !important;
}
:deep(.v-field__input), 
:deep(.v-select__selection), 
:deep(.v-text-field input),
:deep(.v-textarea textarea),
:deep(.modern-table td),
:deep(.modern-table th),
.field-label,
.field-label-small {
    font-size: 13px !important;
}

.field-label {
    font-weight: 700;
    color: #1d2025 !important;
    margin-bottom: 6px;
}

.field-label-small {
    font-weight: 700;
    color: #1d2025 !important;
    text-transform: none !important;
    letter-spacing: normal !important;
    margin-bottom: 4px;
}

.font-weight-black {
    font-weight: 700 !important;
}

.font-weight-bold {
    font-weight: 600 !important;
}

.text-slate-400 {
    color: #64748b !important;
}
</style>
