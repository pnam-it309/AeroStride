<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue';
import { BoxIcon, QrcodeIcon, XIcon } from 'vue-tabler-icons';
import { Html5QrcodeScanner } from 'html5-qrcode';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminFilter from '@/components/common/AdminFilter.vue';
import {
    dichVuDanhMuc,
    dichVuMauSac,
    dichVuChatLieu,
    dichVuKichThuoc,
    dichVuDeGiay,
    dichVuThuongHieu
} from '@/services/product/dichVuThuocTinh';

const props = defineProps({
    activeOrderId: String,
    loadingExternal: Boolean,
    orders: {
        type: Array,
        default: () => []
    }
});
const emit = defineEmits(['add-product']);

const keyword = ref('');
const results = ref([]);
const selectedSelections = ref({});
const loading = ref(false);
const hasSearched = ref(false);

const page = ref(1);
const pageSize = ref(10);

const filters = ref({
    danhMuc: 'ALL',
    mauSac: 'ALL',
    chatLieu: 'ALL',
    kichCo: 'ALL',
    deGiay: 'ALL',
    thuongHieu: 'ALL'
});

const filterOptions = ref({
    danhMuc: [],
    mauSac: [],
    chatLieu: [],
    kichCo: [],
    deGiay: [],
    thuongHieu: []
});

const loadFilterOptions = async () => {
    try {
        const [dm, ms, cl, kc, dg, th] = await Promise.all([
            dichVuDanhMuc.layDanhMuc({ size: 1000 }),
            dichVuMauSac.layMauSac({ size: 1000 }),
            dichVuChatLieu.layChatLieu({ size: 1000 }),
            dichVuKichThuoc.layKichThuoc({ size: 1000 }),
            dichVuDeGiay.layDeGiay({ size: 1000 }),
            dichVuThuongHieu.layThuongHieu({ size: 1000 })
        ]);

        const pick = (res) => res?.content || res || [];
        const uniqueNames = (items) => Array.from(new Set(pick(items).map((x) => x?.ten).filter(Boolean)));
        filterOptions.value = {
            danhMuc: uniqueNames(dm),
            mauSac: uniqueNames(ms),
            chatLieu: uniqueNames(cl),
            kichCo: uniqueNames(kc),
            deGiay: uniqueNames(dg),
            thuongHieu: uniqueNames(th)
        };
    } catch (e) {
        console.error('Load filter options failed:', e);
    }
};

let debounceTimer = null;
watch(keyword, (newVal) => {
    if (debounceTimer) clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
        onSearch();
    }, 400);
});

const mapFilterItems = (options) => {
    return [
        { title: 'Tất cả', value: 'ALL' },
        ...Array.from(new Set(options || [])).map((opt) => ({ title: opt, value: opt }))
    ];
};

const onSearch = async () => {
    if (debounceTimer) clearTimeout(debounceTimer);
    hasSearched.value = true;
    loading.value = true;
    try {
        const data = await dichVuDonHang.searchSanPham(keyword.value || '');
        results.value = data || [];
        selectedSelections.value = {};
        page.value = 1;
    } catch (e) {
        console.error(e);
    } finally {
        loading.value = false;
    }
};

// QR / Barcode Scanner Logic
const showScanner = ref(false);
let html5QrcodeScanner = null;

const startScanner = () => {
    showScanner.value = true;
    setTimeout(() => {
        html5QrcodeScanner = new Html5QrcodeScanner('reader', { fps: 10, qrbox: { width: 250, height: 250 } }, /* verbose= */ false);
        html5QrcodeScanner.render(onScanSuccess, onScanFailure);
    }, 300);
};

const stopScanner = () => {
    if (html5QrcodeScanner) {
        html5QrcodeScanner.clear().catch((error) => console.error('Failed to clear scanner', error));
        html5QrcodeScanner = null;
    }
    showScanner.value = false;
};

const onScanSuccess = async (decodedText) => {
    stopScanner();
    // Giả định mã quét được là mã chi tiết sản phẩm
    try {
        const results = await dichVuDonHang.searchSanPham(decodedText);
        if (results && results.length > 0) {
            // Nếu tìm thấy đúng 1 sản phẩm khớp mã, thêm luôn
            const exactMatch = results.find((p) => p.maChiTietSanPham === decodedText) || results[0];
            emit('add-product', exactMatch);
        }
    } catch (e) {
        console.error('Scan error:', e);
    }
};

const onScanFailure = (error) => {
    // console.warn(`Code scan error = ${error}`);
};

// Hỗ trợ súng quét mã (Barcode Scanner giả lập bàn phím)
let barcodeBuffer = '';
let lastKeyTime = 0;

const handleGlobalKeyDown = (e) => {
    // Nếu đang tập trung vào input thì không xử lý tự động
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;

    const currentTime = new Date().getTime();
    if (currentTime - lastKeyTime > 100) {
        barcodeBuffer = '';
    }

    if (e.key === 'Enter') {
        if (barcodeBuffer.length > 3) {
            onScanSuccess(barcodeBuffer);
            barcodeBuffer = '';
        }
    } else if (e.key.length === 1) {
        barcodeBuffer += e.key;
    }
    lastKeyTime = currentTime;
};

onMounted(() => {
    window.addEventListener('keydown', handleGlobalKeyDown);
    loadFilterOptions();
    onSearch(); // Load all products by default
});

onUnmounted(() => {
    window.removeEventListener('keydown', handleGlobalKeyDown);
});

const selectProduct = (p) => {
    emit('add-product', p);
    // keep keyword/results for fast adding multiple items like the sample UX
};

const getAdjustedStock = (p) => {
    if (!props.orders) return p.soLuongTon || 0;
    let sumInCarts = 0;
    props.orders.forEach(order => {
        if (order.listsHoaDonChiTiet) {
            order.listsHoaDonChiTiet.forEach(item => {
                if (item.idChiTietSanPham === p.id) {
                    sumInCarts += item.soLuong || 0;
                }
            });
        }
    });
    return Math.max(0, (p.soLuongTon || 0) - sumInCarts);
};

const formatCurrency = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

const filteredResults = computed(() => {
    const rs = results.value || [];
    const f = filters.value;
    const match = (key, val) => {
        if (val === 'ALL') return true;
        return String(key || '') === String(val);
    };

    return rs.filter((x) => {
        return (
            match(x.tenDanhMuc, f.danhMuc) &&
            match(x.tenMauSac, f.mauSac) &&
            match(x.tenChatLieu, f.chatLieu) &&
            match(x.tenKichThuoc, f.kichCo) &&
            match(x.tenDeGiay, f.deGiay) &&
            match(x.tenThuongHieu, f.thuongHieu)
        );
    });
});

const groupedProducts = computed(() => {
    const list = filteredResults.value || [];
    const groups = {};
    list.forEach(item => {
        const key = item.maSanPham || item.tenSanPham;
        if (!groups[key]) {
            groups[key] = {
                maSanPham: item.maSanPham,
                tenSanPham: item.tenSanPham,
                tenDanhMuc: item.tenDanhMuc,
                tenThuongHieu: item.tenThuongHieu,
                tenChatLieu: item.tenChatLieu,
                tenDeGiay: item.tenDeGiay,
                hinhAnh: item.hinhAnh,
                variants: []
            };
        }
        groups[key].variants.push(item);
    });
    return Object.values(groups);
});

const getProductState = (p) => {
    const variants = p.variants || [];
    const colors = Array.from(new Set(variants.map(v => v.tenMauSac).filter(Boolean)));
    
    let color = selectedSelections.value[p.maSanPham]?.color;
    if (!color || !colors.includes(color)) {
        color = colors[0] || '';
    }
    
    const colorVariants = variants.filter(v => v.tenMauSac === color);
    const sizes = Array.from(new Set(colorVariants.map(v => v.tenKichThuoc).filter(Boolean)));
    
    let size = selectedSelections.value[p.maSanPham]?.size;
    if (!size || !sizes.includes(size)) {
        size = sizes[0] || '';
    }
    
    const activeVariant = variants.find(v => v.tenMauSac === color && v.tenKichThuoc === size) || colorVariants[0] || variants[0] || null;
    
    return {
        colors,
        color,
        sizes,
        size,
        activeVariant
    };
};

const selectColor = (maSanPham, color) => {
    if (!selectedSelections.value[maSanPham]) {
        selectedSelections.value[maSanPham] = {};
    }
    selectedSelections.value[maSanPham].color = color;
    
    const p = groupedProducts.value.find(x => x.maSanPham === maSanPham);
    if (p) {
        const variants = p.variants || [];
        const colorVariants = variants.filter(v => v.tenMauSac === color);
        const sizes = Array.from(new Set(colorVariants.map(v => v.tenKichThuoc).filter(Boolean)));
        selectedSelections.value[maSanPham].size = sizes[0] || '';
    }
};

const selectSize = (maSanPham, size) => {
    if (!selectedSelections.value[maSanPham]) {
        selectedSelections.value[maSanPham] = {};
    }
    selectedSelections.value[maSanPham].size = size;
};

const totalElements = computed(() => groupedProducts.value.length);
const totalPages = computed(() => Math.max(1, Math.ceil(totalElements.value / pageSize.value)));
const pagedResults = computed(() => {
    const start = (page.value - 1) * pageSize.value;
    return groupedProducts.value.slice(start, start + pageSize.value);
});

const imageSrc = (p) => {
    const src = p?.hinhAnh;
    if (!src) return null;
    return src;
};

const tableHeaders = [
    { text: 'Ảnh', width: '70px' },
    { text: 'Tên' },
    { text: 'Mã', width: '130px' },
    { text: 'Danh mục', width: '100px' },
    { text: 'Thương hiệu', width: '110px' },
    { text: 'Màu sắc', width: '130px' },
    { text: 'Chất liệu', width: '90px' },
    { text: 'Size', width: '95px' },
    { text: 'Giá', width: '110px' },
    { text: 'Thao tác', width: '100px' }
];
</script>

<template>
    <div class="product-picker">
        <!-- 1. Bộ lọc đồng bộ -->
        <AdminFilter title="Tìm kiếm & Lọc sản phẩm" :loading="loading" @refresh="onSearch">
            <!-- Tìm kiếm -->
            <v-col cols="12" md="4">
                <div class="filter-field-label">Tìm kiếm</div>
                <v-text-field
                    v-model="keyword"
                    placeholder="Nhập tên hoặc mã sản phẩm..."
                    variant="outlined"
                    density="compact"
                    hide-details
                    bg-color="white"
                    prepend-inner-icon="mdi-magnify"
                    @keydown.enter="onSearch"
                    class="compact-input"
                >
                    <template #append-inner>
                        <v-btn icon color="primary" variant="text" size="small" class="mr-n2" @click="startScanner">
                            <QrcodeIcon size="20" />
                        </v-btn>
                    </template>
                </v-text-field>
            </v-col>

            <!-- Danh mục -->
            <v-col cols="12" md="2">
                <div class="filter-field-label">Danh mục</div>
                <v-select
                    v-model="filters.danhMuc"
                    :items="mapFilterItems(filterOptions.danhMuc)"
                    item-title="title"
                    item-value="value"
                    density="compact"
                    hide-details
                    variant="outlined"
                    class="compact-input"
                ></v-select>
            </v-col>

            <!-- Thương hiệu -->
            <v-col cols="12" md="2">
                <div class="filter-field-label">Thương hiệu</div>
                <v-select
                    v-model="filters.thuongHieu"
                    :items="mapFilterItems(filterOptions.thuongHieu)"
                    item-title="title"
                    item-value="value"
                    density="compact"
                    hide-details
                    variant="outlined"
                    class="compact-input"
                ></v-select>
            </v-col>

            <!-- Màu sắc -->
            <v-col cols="12" md="2">
                <div class="filter-field-label">Màu sắc</div>
                <v-select
                    v-model="filters.mauSac"
                    :items="mapFilterItems(filterOptions.mauSac)"
                    item-title="title"
                    item-value="value"
                    density="compact"
                    hide-details
                    variant="outlined"
                    class="compact-input"
                ></v-select>
            </v-col>

            <!-- Chất liệu -->
            <v-col cols="12" md="2">
                <div class="filter-field-label">Chất liệu</div>
                <v-select
                    v-model="filters.chatLieu"
                    :items="mapFilterItems(filterOptions.chatLieu)"
                    item-title="title"
                    item-value="value"
                    density="compact"
                    hide-details
                    variant="outlined"
                    class="compact-input"
                ></v-select>
            </v-col>

            <!-- Kích cỡ -->
            <v-col cols="12" md="2">
                <div class="filter-field-label">Kích cỡ</div>
                <v-select
                    v-model="filters.kichCo"
                    :items="mapFilterItems(filterOptions.kichCo)"
                    item-title="title"
                    item-value="value"
                    density="compact"
                    hide-details
                    variant="outlined"
                    class="compact-input"
                ></v-select>
            </v-col>

            <!-- Đế giày -->
            <v-col cols="12" md="2">
                <div class="filter-field-label">Đế giày</div>
                <v-select
                    v-model="filters.deGiay"
                    :items="mapFilterItems(filterOptions.deGiay)"
                    item-title="title"
                    item-value="value"
                    density="compact"
                    hide-details
                    variant="outlined"
                    class="compact-input"
                ></v-select>
            </v-col>
        </AdminFilter>

        <!-- QR Scanner Dialog -->
        <v-dialog v-model="showScanner" max-width="500" transition="dialog-bottom-transition">
            <v-card class="rounded-xl pa-4">
                <div class="d-flex justify-space-between align-center mb-4">
                    <span class="text-h6 font-weight-bold">Quét mã sản phẩm</span>
                    <v-btn icon variant="text" @click="stopScanner"><XIcon /></v-btn>
                </div>
                <div id="reader" style="width: 100%"></div>
                <div class="mt-4 text-center text-caption text-grey">Đưa mã QR hoặc Barcode của sản phẩm vào khung hình</div>
            </v-card>
        </v-dialog>

        <div class="mt-2 table-wrapper-picker">
            <AdminTable
                title="Kết quả tìm kiếm"
                :headers="tableHeaders"
                :items="pagedResults"
                :loading="loading"
                :showAddButton="false"
                :emptyText="hasSearched ? 'Không tìm thấy sản phẩm phù hợp' : 'Nhập từ khóa hoặc bấm TÌM KIẾM'"
                emptyIcon="mdi-magnify"
            >
                <template #row="{ item: p }">
                    <tr>
                        <td>
                            <v-avatar rounded="lg" size="56" color="grey-lighten-4">
                                <v-img v-if="imageSrc(getProductState(p).activeVariant) || imageSrc(p)" :src="imageSrc(getProductState(p).activeVariant) || imageSrc(p)" cover />
                                <BoxIcon v-else size="22" class="text-grey-darken-1" />
                            </v-avatar>
                        </td>
                        <td>
                            <div class="font-weight-bold text-truncate" :title="p.tenSanPham">{{ p.tenSanPham }}</div>
                            <div class="text-caption text-slate-500">Tồn: <b>{{ getProductState(p).activeVariant ? getAdjustedStock(getProductState(p).activeVariant) : 0 }}</b></div>
                        </td>
                        <td class="text-caption font-weight-bold">
                            <div class="text-truncate" :title="getProductState(p).activeVariant?.maChiTietSanPham || p.maSanPham">{{ getProductState(p).activeVariant?.maChiTietSanPham || p.maSanPham }}</div>
                        </td>
                        <td class="text-caption">
                            <div class="text-truncate" :title="p.tenDanhMuc">{{ p.tenDanhMuc || '—' }}</div>
                        </td>
                        <td class="text-caption">
                            <div class="text-truncate" :title="p.tenThuongHieu">{{ p.tenThuongHieu || '—' }}</div>
                        </td>
                        <td>
                            <v-select
                                :model-value="getProductState(p).color"
                                @update:model-value="selectColor(p.maSanPham, $event)"
                                :items="getProductState(p).colors"
                                density="compact"
                                hide-details
                                variant="outlined"
                                class="mini-select"
                            />
                        </td>
                        <td class="text-caption">
                            <div class="text-truncate" :title="p.tenChatLieu">{{ p.tenChatLieu || '—' }}</div>
                        </td>
                        <td>
                            <v-select
                                :model-value="getProductState(p).size"
                                @update:model-value="selectSize(p.maSanPham, $event)"
                                :items="getProductState(p).sizes"
                                density="compact"
                                hide-details
                                variant="outlined"
                                class="mini-select"
                            />
                        </td>
                        <td class="text-right font-weight-bold text-error">{{ formatCurrency(getProductState(p).activeVariant?.giaBan || p.variants[0]?.giaBan) }}</td>
                        <td class="text-center">
                            <v-btn
                                color="black"
                                size="small"
                                variant="flat"
                                class="font-weight-black"
                                :disabled="!getProductState(p).activeVariant || getAdjustedStock(getProductState(p).activeVariant) <= 0 || loadingExternal"
                                :loading="loadingExternal"
                                @click="selectProduct(getProductState(p).activeVariant)"
                            >
                                CHỌN
                            </v-btn>
                        </td>
                    </tr>
                </template>

                <template #pagination>
                    <AdminPagination
                        v-model="page"
                        :page-size="pageSize"
                        :total-pages="totalPages"
                        :total-elements="totalElements"
                        :current-size="pagedResults.length"
                        @update:pageSize="pageSize = $event"
                    />
                </template>
            </AdminTable>
        </div>
    </div>
</template>

<style scoped>
.table-wrapper-picker {
    max-height: calc(100vh - 360px);
    min-height: 360px;
    overflow-y: auto;
    overflow-x: hidden;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
}

.filter-field-label {
    font-size: 13px;
    font-weight: 700;
    color: #475569;
    margin-bottom: 4px;
}

.compact-input :deep(.v-field) {
    border-radius: 8px !important;
}

.product-picker :deep(.filter-card) {
    margin-bottom: 16px !important;
    background-color: #f8fafc !important;
}

@media (max-width: 960px) {
    .table-wrapper-picker {
        max-height: 350px;
        min-height: 280px;
    }
}

.mini-select {
    max-width: 120px;
}
.mini-select :deep(.v-field__input) {
    padding-top: 2px !important;
    padding-bottom: 2px !important;
    min-height: 28px !important;
    font-size: 12px !important;
}
.mini-select :deep(.v-field) {
    border-radius: 6px !important;
    --v-input-control-height: 28px !important;
    font-size: 12px !important;
}
</style>




