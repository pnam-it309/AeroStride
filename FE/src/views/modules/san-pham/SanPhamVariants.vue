<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { useNotifications } from '@/services/notificationService';
import { resolveMediaUrl } from '@/utils/mediaUrl';
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import VariantFormDialog from './components/VariantFormDialog.vue';
import VariantImageDialog from './components/VariantImageDialog.vue';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const loading = ref(false);
const productsLoading = ref(false);
const variantDialogLoading = ref(false);
const imageDialogLoading = ref(false);

const products = ref([]);
const selectedProductId = ref(route.query.productId?.toString() || '');
const selectedProduct = ref(null);

const filters = ref({ keyword: '', mauSacId: null, kichThuocId: null, trangThai: null });
const pagination = ref({ page: 1, size: 10 });
const formOptions = ref({ mauSacs: [], kichThuocs: [], trangThais: ['DANG_HOAT_DONG', 'KHONG_HOAT_DONG'] });

const variantDialog = ref({ show: false, mode: 'create', data: null });
const imageDialog = ref({ show: false, mode: 'create', data: null, variantId: null });
const confirmDialog = ref({ show: false, title: '', message: '', color: 'primary', action: null, loading: false });

const placeholderImage = 'https://placehold.co/120x120/f8fafc/94a3b8?text=SKU';
const getApiErrorMessage = (error, fallback) => error?.response?.data?.message || error?.response?.data?.error || fallback;

const formatNumber = (value) => new Intl.NumberFormat('vi-VN').format(Number(value || 0));
const formatCurrency = (value) => {
    if (value === null || value === undefined || value === '') return '--';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(Number(value));
};
const formatDateTime = (value) => {
    if (!value) return '--';
    const date = typeof value === 'number' ? new Date(value) : new Date(String(value));
    if (Number.isNaN(date.getTime())) return '--';
    return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(date);
};
const getStatusLabel = (status) => {
    if (status === 'DANG_HOAT_DONG') return 'Hoạt động';
    if (status === 'KHONG_HOAT_DONG') return 'Ngừng bán';
    if (status === 'DA_XOA') return 'Đã xóa';
    return status || '--';
};
const getStatusChipClass = (status) => (status === 'DANG_HOAT_DONG' ? 'variant-status-active' : status === 'KHONG_HOAT_DONG' ? 'variant-status-inactive' : 'variant-status-neutral');
const getVariantThumbnail = (variant) => {
    const images = Array.isArray(variant?.images) ? variant.images : [];
    const imageUrl = images.find((image) => image.hinhAnhDaiDien)?.duongDanAnh || images[0]?.duongDanAnh || '';
    return resolveMediaUrl(imageUrl, 'aerostride/products/variants') || placeholderImage;
};

const filteredVariants = computed(() => {
    const variants = Array.isArray(selectedProduct.value?.variants) ? selectedProduct.value.variants : [];
    const keyword = filters.value.keyword.trim().toLowerCase();
    return variants.filter((variant) => {
        const matchesKeyword = !keyword
            || String(variant.maChiTietSanPham || '').toLowerCase().includes(keyword)
            || String(variant.tenMauSac || '').toLowerCase().includes(keyword)
            || String(variant.tenKichThuoc || '').toLowerCase().includes(keyword);
        return matchesKeyword
            && (!filters.value.mauSacId || variant.idMauSac === filters.value.mauSacId)
            && (!filters.value.kichThuocId || variant.idKichThuoc === filters.value.kichThuocId)
            && (!filters.value.trangThai || variant.trangThai === filters.value.trangThai);
    });
});

const totalPages = computed(() => Math.max(Math.ceil(filteredVariants.value.length / pagination.value.size), 1));
const paginatedVariants = computed(() => {
    const start = (pagination.value.page - 1) * pagination.value.size;
    return filteredVariants.value.slice(start, start + pagination.value.size);
});

const selectedProductSummary = computed(() => {
    if (!selectedProduct.value) return 'Chọn sản phẩm để hiển thị danh sách biến thể.';
    const totalStock = (selectedProduct.value.variants || []).reduce((sum, item) => sum + Number(item.soLuong || 0), 0);
    return `${selectedProduct.value.maSanPham} • ${selectedProduct.value.tenSanPham} • ${formatNumber(selectedProduct.value.variants?.length || 0)} biến thể • ${formatNumber(totalStock)} đôi`;
});

const loadFormOptions = async () => {
    const options = await dichVuSanPham.layOptionsForm();
    formOptions.value = {
        mauSacs: options?.mauSacs || [],
        kichThuocs: options?.kichThuocs || [],
        trangThais: options?.trangThais || ['DANG_HOAT_DONG', 'KHONG_HOAT_DONG']
    };
};

const loadProducts = async () => {
    productsLoading.value = true;
    try {
        const response = await dichVuSanPham.layDanhSachSanPham({ page: 0, size: 100, sortBy: 'tenSanPham', sortDirection: 'asc' });
        products.value = response?.content || [];
        if (!selectedProductId.value && products.value.length > 0) selectedProductId.value = products.value[0].id;
    } finally {
        productsLoading.value = false;
    }
};

const loadSelectedProduct = async () => {
    if (!selectedProductId.value) {
        selectedProduct.value = null;
        return;
    }

    loading.value = true;
    try {
        selectedProduct.value = await dichVuSanPham.layChiTietSanPham(selectedProductId.value);
        pagination.value.page = 1;
    } catch (error) {
        selectedProduct.value = null;
        addNotification({ title: 'Lỗi tải biến thể', subtitle: 'Không thể tải thông tin sản phẩm đã chọn', color: 'error' });
    } finally {
        loading.value = false;
    }
};

const syncRouteQuery = async () => {
    const nextQuery = selectedProductId.value ? { productId: selectedProductId.value } : {};
    if (route.query.productId === selectedProductId.value) return;
    await router.replace({ query: nextQuery });
};

const resetFilters = async () => {
    filters.value = { keyword: '', mauSacId: null, kichThuocId: null, trangThai: null };
    pagination.value.page = 1;
};

const openCreateVariantDialog = () => {
    if (!selectedProductId.value) {
        addNotification({ title: 'Chưa chọn sản phẩm', subtitle: 'Vui lòng chọn sản phẩm trước khi thêm biến thể', color: 'warning' });
        return;
    }
    variantDialog.value = { show: true, mode: 'create', data: null };
};

const openEditVariantDialog = (variant) => {
    variantDialog.value = { show: true, mode: 'edit', data: { ...variant } };
};

const submitVariant = async (payload) => {
    variantDialogLoading.value = true;
    try {
        if (variantDialog.value.mode === 'edit' && variantDialog.value.data?.id) {
            await dichVuSanPham.capNhatBienTheSanPham(variantDialog.value.data.id, payload);
            addNotification({ title: 'Cập nhật thành công', subtitle: 'Đã lưu thay đổi cho biến thể', color: 'success' });
        } else {
            await dichVuSanPham.themBienTheSanPham(selectedProductId.value, payload);
            addNotification({ title: 'Thêm biến thể thành công', subtitle: 'Biến thể mới đã được tạo', color: 'success' });
        }
        variantDialog.value.show = false;
        await loadSelectedProduct();
    } catch (error) {
        addNotification({
            title: 'Lỗi lưu biến thể',
            subtitle: getApiErrorMessage(error, 'Không thể lưu dữ liệu biến thể'),
            color: 'error'
        });
    } finally {
        variantDialogLoading.value = false;
    }
};

const requestDeleteVariant = (variant) => {
    confirmDialog.value = {
        show: true,
        title: 'Xóa biến thể',
        message: `Bạn có chắc muốn xóa mềm biến thể [${variant.maChiTietSanPham}] không?`,
        color: 'error',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                await dichVuSanPham.xoaBienTheSanPham(variant.id);
                addNotification({ title: 'Đã xóa biến thể', subtitle: 'Biến thể đã được xóa mềm', color: 'success' });
                confirmDialog.value.show = false;
                await loadSelectedProduct();
            } catch (error) {
                addNotification({
                    title: 'Lỗi xóa biến thể',
                    subtitle: getApiErrorMessage(error, 'Không thể xóa biến thể này'),
                    color: 'error'
                });
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

const openImageDialog = (variant) => {
    const preferredImage = variant?.images?.find((image) => image.hinhAnhDaiDien) || variant?.images?.[0] || null;
    imageDialog.value = {
        show: true,
        mode: preferredImage ? 'edit' : 'create',
        data: preferredImage,
        variantId: variant.id
    };
};

const submitImage = async (payload) => {
    imageDialogLoading.value = true;
    try {
        if (imageDialog.value.mode === 'edit' && imageDialog.value.data?.id) {
            await dichVuSanPham.capNhatAnhBienThe(imageDialog.value.data.id, payload);
            addNotification({ title: 'Cập nhật ảnh thành công', subtitle: 'Đã lưu thay đổi ảnh biến thể', color: 'success' });
        } else {
            await dichVuSanPham.themAnhBienThe(imageDialog.value.variantId, payload);
            addNotification({ title: 'Thêm ảnh thành công', subtitle: 'Đã thêm ảnh mới cho biến thể', color: 'success' });
        }
        imageDialog.value.show = false;
        await loadSelectedProduct();
    } catch (error) {
        addNotification({
            title: 'Lỗi lưu ảnh',
            subtitle: getApiErrorMessage(error, 'Không thể lưu ảnh biến thể'),
            color: 'error'
        });
    } finally {
        imageDialogLoading.value = false;
    }
};

watch(() => selectedProductId.value, async () => {
    await syncRouteQuery();
    await loadSelectedProduct();
});

watch(() => filteredVariants.value.length, () => {
    if (pagination.value.page > totalPages.value) pagination.value.page = totalPages.value;
});

onMounted(async () => {
    await Promise.all([loadFormOptions(), loadProducts()]);
    if (selectedProductId.value) await loadSelectedProduct();
});
</script>

<template>
    <v-container fluid class="pa-4 gray-bg min-h-screen font-body">
        <div class="mb-4">
            <div class="d-flex align-center justify-space-between flex-wrap gap-3">
                <div>
                    <h1 class="page-title text-h5 font-weight-bold text-slate-900 mb-1">Quản lí biến thể sản phẩm</h1>
                    <div class="text-body-2 text-medium-emphasis">
                        Tách riêng khỏi màn sản phẩm để dễ quản lý SKU, giá, tồn kho và ảnh biến thể.
                    </div>
                </div>

                <div class="d-flex align-center gap-2 flex-wrap">
                    <v-btn variant="outlined" color="primary" prepend-icon="mdi-arrow-left" class="text-none" @click="router.push('/san-pham')">
                        Quay lại sản phẩm
                    </v-btn>
                    <v-btn color="primary" prepend-icon="mdi-plus" class="text-none font-weight-bold" @click="openCreateVariantDialog">
                        Thêm biến thể
                    </v-btn>
                </div>
            </div>
        </div>

        <div class="filter-top">
            <AdminFilter title="Bộ lọc biến thể" :loading="loading || productsLoading" @refresh="resetFilters">
                <v-col cols="12" md="2">
                    <div class="filter-field-label">Sản phẩm</div>
                    <v-select
                        v-model="selectedProductId"
                        :items="products"
                        item-title="tenSanPham"
                        item-value="id"
                        :loading="productsLoading"
                        placeholder="Chọn sản phẩm"
                        variant="outlined"
                        density="compact"
                        hide-details
                    >
                        <template #item="{ props, item }">
                            <v-list-item v-bind="props" :title="item.raw.tenSanPham" :subtitle="item.raw.maSanPham" />
                        </template>
                    </v-select>
                </v-col>

                <v-col cols="12" md="3">
                    <div class="filter-field-label">Tìm kiếm biến thể</div>
                    <v-text-field
                        v-model="filters.keyword"
                        placeholder="Mã biến thể, màu sắc, kích thước..."
                        prepend-inner-icon="mdi-magnify"
                        variant="outlined"
                        density="compact"
                        hide-details
                    />
                </v-col>

                <v-col cols="12" md="2">
                    <div class="filter-field-label">Màu sắc</div>
                    <v-select
                        v-model="filters.mauSacId"
                        :items="[{ ten: 'Tất cả màu sắc', id: null }, ...formOptions.mauSacs]"
                        item-title="ten"
                        item-value="id"
                        variant="outlined"
                        density="compact"
                        hide-details
                    />
                </v-col>

                <v-col cols="12" md="2">
                    <div class="filter-field-label">Kích thước</div>
                    <v-select
                        v-model="filters.kichThuocId"
                        :items="[{ ten: 'Tất cả kích thước', id: null }, ...formOptions.kichThuocs]"
                        item-title="ten"
                        item-value="id"
                        variant="outlined"
                        density="compact"
                        hide-details
                    />
                </v-col>

                <v-col cols="12" md="2">
                    <div class="filter-field-label">Trạng thái</div>
                    <v-select
                        v-model="filters.trangThai"
                        :items="[{ title: 'Tất cả trạng thái', value: null }, ...formOptions.trangThais.map((item) => ({ title: getStatusLabel(item), value: item }))]"
                        item-title="title"
                        item-value="value"
                        variant="outlined"
                        density="compact"
                        hide-details
                    />
                </v-col>
            </AdminFilter>
        </div>

        <div class="product-summary-card mb-4">
            <div class="d-flex align-center justify-space-between flex-wrap gap-3">
                <div>
                    <div class="text-subtitle-1 font-weight-bold text-dark">Sản phẩm đang chọn</div>
                    <div class="text-body-2 text-medium-emphasis mt-1">{{ selectedProductSummary }}</div>
                </div>
            </div>
        </div>

        <AdminTable
            title="Danh sách biến thể"
            :headers="[
                { text: 'Biến thể', align: 'left', width: '24%' },
                { text: 'Sản phẩm', align: 'left', width: '18%' },
                { text: 'Thuộc tính', align: 'left', width: '14%' },
                { text: 'Tồn kho', align: 'center', width: '10%' },
                { text: 'Giá bán', align: 'center', width: '12%' },
                { text: 'Giá nhập', align: 'center', width: '12%' },
                { text: 'Trạng thái', align: 'center', width: '10%' },
                { text: 'Cập nhật', align: 'center', width: '12%' },
                { text: 'Hành động', align: 'center', width: '12%' }
            ]"
            :items="paginatedVariants"
            :loading="loading"
            :show-add-button="false"
            empty-text="Chọn sản phẩm hoặc điều chỉnh bộ lọc để xem biến thể"
        >
            <template #row="{ item }">
                <tr class="data-row">
                    <td class="data-cell text-left">
                        <div class="variant-cell">
                            <v-avatar size="48" rounded="lg" class="variant-avatar">
                                <v-img :src="getVariantThumbnail(item)" cover />
                            </v-avatar>
                            <div class="min-w-0">
                                <div class="font-weight-bold text-dark">{{ item.maChiTietSanPham }}</div>
                                <div class="text-caption text-medium-emphasis mt-1">{{ formatNumber(item.images?.length || 0) }} ảnh</div>
                            </div>
                        </div>
                    </td>

                    <td class="data-cell text-left">
                        <div class="font-weight-bold text-dark">{{ selectedProduct?.tenSanPham || '--' }}</div>
                        <div class="text-caption text-medium-emphasis mt-1">{{ selectedProduct?.maSanPham || '--' }}</div>
                    </td>

                    <td class="data-cell text-left">
                        <div class="font-weight-medium text-dark">{{ item.tenMauSac }}</div>
                        <div class="text-caption text-medium-emphasis mt-1">Size {{ item.tenKichThuoc }}</div>
                    </td>

                    <td class="data-cell text-center font-weight-bold text-dark">{{ formatNumber(item.soLuong) }}</td>
                    <td class="data-cell text-center price-primary">{{ formatCurrency(item.giaBan) }}</td>
                    <td class="data-cell text-center">{{ formatCurrency(item.giaNhap) }}</td>

                    <td class="data-cell text-center">
                        <v-chip variant="flat" class="font-weight-bold px-3 status-chip" :class="getStatusChipClass(item.trangThai)">
                            {{ getStatusLabel(item.trangThai) }}
                        </v-chip>
                    </td>

                    <td class="data-cell text-center">{{ formatDateTime(item.ngayCapNhat || item.ngayTao) }}</td>

                    <td class="data-cell text-center action-cell">
                        <div class="d-flex align-center justify-center action-controls">
                            <v-btn icon variant="text" size="30" color="#2aa6a1" class="action-icon-btn" @click="openImageDialog(item)">
                                <v-icon size="18">mdi-image-multiple-outline</v-icon>
                            </v-btn>
                            <v-btn icon variant="text" size="30" color="#5f6f82" class="action-icon-btn" @click="openEditVariantDialog(item)">
                                <v-icon size="18">mdi-pencil-outline</v-icon>
                            </v-btn>
                            <v-btn icon variant="text" size="30" color="#dc2626" class="action-icon-btn" @click="requestDeleteVariant(item)">
                                <v-icon size="18">mdi-delete-outline</v-icon>
                            </v-btn>
                        </div>
                    </td>
                </tr>
            </template>

            <template #pagination>
                <AdminPagination
                    v-model="pagination.page"
                    :page-size="pagination.size"
                    :total-pages="totalPages"
                    :total-elements="filteredVariants.length"
                    :current-size="paginatedVariants.length"
                    @update:pageSize="pagination.size = $event"
                    @update:page-size="pagination.size = $event"
                />
            </template>
        </AdminTable>

        <VariantFormDialog
            v-model:show="variantDialog.show"
            :mode="variantDialog.mode"
            :variant="variantDialog.data"
            :color-options="formOptions.mauSacs"
            :size-options="formOptions.kichThuocs"
            :status-options="formOptions.trangThais"
            :loading="variantDialogLoading"
            @submit="submitVariant"
        />

        <VariantImageDialog
            v-model:show="imageDialog.show"
            :mode="imageDialog.mode"
            :image="imageDialog.data"
            :status-options="formOptions.trangThais"
            :loading="imageDialogLoading"
            @submit="submitImage"
        />

        <AdminConfirm
            v-model:show="confirmDialog.show"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            :color="confirmDialog.color"
            :loading="confirmDialog.loading"
            @confirm="confirmDialog.action"
        />
    </v-container>
</template>

<style scoped>
.gray-bg { background-color: #f5f7fb; }
.font-body { font-family: 'Inter', sans-serif; }
.page-title { line-height: 1.1; }
.filter-field-label { font-size: 13px; font-weight: 700; color: #334155; margin-bottom: 4px; }

.product-summary-card {
    background: white;
    border: 1px solid #dbe4ef;
    border-radius: 12px;
    padding: 16px 18px;
}

.variant-cell {
    display: flex;
    align-items: center;
    gap: 12px;
}

.variant-avatar {
    border: 1px solid #dbe4ef;
    background: #ffffff;
}

.text-dark { color: #0f172a !important; }
.price-primary { color: #1e3a8a; font-weight: 700; }
.action-controls { gap: 6px; }

:deep(.action-icon-btn) {
    min-width: 30px !important;
    width: 30px !important;
    height: 30px !important;
    padding: 0 !important;
    border-radius: 10px !important;
}

:deep(.status-chip) {
    border-radius: 999px !important;
    border: 1px solid transparent !important;
    font-size: 12px !important;
    min-height: 26px !important;
}

:deep(.variant-status-active) {
    background: #edf7f4 !important;
    color: #15803d !important;
    border-color: #cce8d7 !important;
}

:deep(.variant-status-inactive) {
    background: #fff4e6 !important;
    color: #b45309 !important;
    border-color: #f4dcc0 !important;
}

:deep(.variant-status-neutral) {
    background: #eef2f7 !important;
    color: #475569 !important;
    border-color: #d8e0ea !important;
}
</style>
