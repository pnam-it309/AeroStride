<script setup>
/**
 * Module: Bán hàng tại quầy (Admin)
 * Component: ProductPicker
 * Chức năng: Tìm kiếm và thêm sản phẩm vào giỏ hàng. Hỗ trợ tìm theo tên, mã, 
 *            và quét mã vạch (Barcode/QR code).
 */
import { ref, onMounted, onUnmounted, watch, computed } from 'vue';
import { Html5QrcodeScanner } from 'html5-qrcode';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';
import { useNotifications } from '@/services/notificationService';
import { isActiveStatus } from '@/utils/statusUtils';
import {
    dichVuDanhMuc,
    dichVuMauSac,
    dichVuKichThuoc,
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
const { addNotification } = useNotifications();

const keyword = ref('');
const results = ref([]);
const loading = ref(false);
const selectedVariant = ref(null);

const filters = ref({
    danhMuc: 'ALL',
    mauSac: 'ALL',
    kichCo: 'ALL',
    thuongHieu: 'ALL'
});

const filterOptions = ref({
    danhMuc: [],
    mauSac: [],
    kichCo: [],
    thuongHieu: []
});

// Tải dữ liệu các tùy chọn bộ lọc từ API cũ
const loadFilterOptions = async () => {
    try {
        const [dm, ms, kc, th] = await Promise.all([
            dichVuDanhMuc.layDanhMuc({ size: 1000 }),
            dichVuMauSac.layMauSac({ size: 1000 }),
            dichVuKichThuoc.layKichThuoc({ size: 1000 }),
            dichVuThuongHieu.layThuongHieu({ size: 1000 })
        ]);

        const pick = (res) => res?.content || res || [];
        filterOptions.value = {
            danhMuc: pick(dm).map((x) => x?.ten).filter(Boolean),
            mauSac: pick(ms).map((x) => x?.ten).filter(Boolean),
            kichCo: pick(kc).map((x) => x?.ten).filter(Boolean),
            thuongHieu: pick(th).map((x) => x?.ten).filter(Boolean)
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

// Chuyển đổi danh sách thuộc tính thành mảng các lựa chọn { title, value } cho <v-select>
const mapFilterItems = (options, label = 'Tất cả') => {
    return [
        { title: `Tất cả ${label.toLowerCase()}`, value: 'ALL' },
        ...Array.from(new Set(options || [])).map((opt) => ({ title: opt, value: opt }))
    ];
};

const getVariantStock = (variant) => Number(variant?.soLuongTon ?? variant?.soLuong ?? 0);

// Gửi yêu cầu tìm kiếm sản phẩm tới server dựa trên keyword.
const onSearch = async () => {
    loading.value = true;
    try {
        const response = await dichVuDonHang.searchSanPham(keyword.value?.trim() || '');
        results.value = Array.isArray(response) ? response : [];
    } catch (e) {
        console.error(e);
    } finally {
        loading.value = false;
    }
};

const filteredResults = computed(() => {
    const rs = results.value || [];
    const f = filters.value;
    return rs.filter((x) => {
        const active = x.trangThai === undefined || isActiveStatus(x.trangThai);
        if (!active) return false;
        
        if (f.danhMuc !== 'ALL' && String(x.tenDanhMuc || '') !== String(f.danhMuc)) return false;
        if (f.thuongHieu !== 'ALL' && String(x.tenThuongHieu || '') !== String(f.thuongHieu)) return false;
        if (f.mauSac !== 'ALL' && String(x.tenMauSac || '') !== String(f.mauSac)) return false;
        if (f.kichCo !== 'ALL' && String(x.tenKichThuoc || '') !== String(f.kichCo)) return false;
        
        return true;
    });
});

const resetFilters = () => {
    filters.value = {
        danhMuc: 'ALL',
        mauSac: 'ALL',
        kichCo: 'ALL',
        thuongHieu: 'ALL'
    };
};

// QR / Barcode Scanner Logic
const showScanner = ref(false);
let html5QrcodeScanner = null;

const startScanner = () => {
    showScanner.value = true;
    setTimeout(() => {
        const el = document.getElementById('reader');
        if (!el || el.clientWidth === 0) {
            startScanner();
            return;
        }

        if (html5QrcodeScanner) {
            html5QrcodeScanner.clear().catch(e => console.error(e));
        }

        html5QrcodeScanner = new Html5QrcodeScanner('reader', { fps: 10, qrbox: { width: 250, height: 250 } }, false);
        html5QrcodeScanner.render(onScanSuccess, onScanFailure);
    }, 150);
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
    const kw = decodedText?.trim();
    if (!kw) return;
    if (!props.activeOrderId) {
        addNotification({ title: 'Chưa có hóa đơn', subtitle: 'Vui lòng tạo hoặc chọn hóa đơn trước khi quét mã.', color: 'warning' });
        return;
    }

    try {
        const variants = await dichVuDonHang.searchSanPham(kw);
        if (variants && variants.length > 0) {
            const exactMatch = variants.find((v) => v.maChiTietSanPham === kw) || variants[0];
            
            if (exactMatch) {
                if (exactMatch.trangThai !== undefined && !isActiveStatus(exactMatch.trangThai)) {
                    addNotification({ title: 'Thất bại', subtitle: 'Sản phẩm đã ngừng bán', color: 'error' });
                    return;
                }
                const currentStock = getVariantStock(exactMatch);
                if (currentStock <= 0) {
                    addNotification({ title: 'Thất bại', subtitle: 'Sản phẩm đã hết hàng', color: 'error' });
                    return;
                }
                
                emit('add-product', { ...exactMatch, _soLuongMuonThem: 1 });
            }
        } else {
            addNotification({ title: 'Không tìm thấy', subtitle: `Không tìm thấy mã sản phẩm ${kw}`, color: 'warning' });
        }
    } catch (e) {
        console.error('Scan error:', e);
    }
};

const onScanFailure = (error) => {};

let barcodeBuffer = '';
let lastKeyTime = 0;

const handleGlobalKeyDown = (e) => {
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
    onSearch(); // Load all products by default
    loadFilterOptions();
});

onUnmounted(() => {
    window.removeEventListener('keydown', handleGlobalKeyDown);
});

// Khi chọn một biến thể từ dropdown
const onSelectVariant = (variant) => {
    if (!variant) return;
    
    // Clear selection so you can select the same again
    setTimeout(() => {
        selectedVariant.value = null;
    }, 50);

    if (variant.trangThai !== undefined && !isActiveStatus(variant.trangThai)) {
        addNotification({ title: 'Thất bại', subtitle: 'Sản phẩm đã ngừng bán', color: 'error' });
        return;
    }
    const currentStock = getVariantStock(variant);
    if (currentStock <= 0) {
        addNotification({ title: 'Thất bại', subtitle: 'Sản phẩm đã hết hàng', color: 'error' });
        return;
    }
    
    emit('add-product', { ...variant, _soLuongMuonThem: 1 });
};

const formatCurrency = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

const imageSrc = (p) => {
    return p?.hinhAnh || null;
};

defineExpose({
    refresh: onSearch
});
</script>

<template>
    <div class="product-picker mb-4">
        <!-- Search bar -->
        <v-autocomplete
            v-model="selectedVariant"
            v-model:search="keyword"
            :items="filteredResults"
            :loading="loading"
            item-title="tenSanPham"
            item-value="id"
            return-object
            placeholder="Nhập mã, tên sản phẩm hoặc Barcode"
            variant="outlined"
            density="compact"
            hide-details
            bg-color="white"
            no-filter
            menu-icon=""
            @update:modelValue="onSelectVariant"
            class="pos-search-bar"
            :menu-props="{ maxHeight: 450, contentClass: 'pos-search-menu' }"
        >
            <template #prepend-inner>
                <v-icon color="grey-darken-1" size="18" class="mr-1">mdi-magnify</v-icon>
            </template>
            <template #append-inner>
                <v-btn icon color="primary" variant="text" size="24" class="mr-n2 ml-2" @click="startScanner">
                    <v-icon size="18">mdi-qrcode-scan</v-icon>
                </v-btn>
            </template>
            
            <!-- Custom Item Slot -->
            <template v-slot:item="{ props, item }">
                <v-list-item v-bind="props" title="" subtitle="" class="py-2 border-b pos-variant-item" :disabled="getVariantStock(item.raw) <= 0">
                    <template v-slot:prepend>
                        <v-avatar rounded="lg" size="50" color="grey-lighten-4" class="mr-3 border">
                            <v-img v-if="imageSrc(item.raw)" :src="imageSrc(item.raw)" cover />
                            <v-icon v-else size="20" color="grey-darken-1">mdi-package-variant</v-icon>
                        </v-avatar>
                    </template>
                    
                    <div class="d-flex flex-column w-100" style="font-size: 13px; line-height: 1.4;">
                        <div class="d-flex justify-space-between align-start w-100 mb-1">
                            <div class="font-weight-normal text-slate-700" style="font-size: 13px;">
                                {{ item.raw.tenSanPham }}
                            </div>
                            <div class="font-weight-bold text-success" style="font-size: 13px;">
                                {{ formatCurrency(item.raw.giaBan) }}
                            </div>
                        </div>
                        
                        <div class="d-flex align-center text-slate-600 flex-wrap gap-2 mb-1">
                            <div class="d-flex align-center mr-2">
                                <span class="product-code-pastel-chip">{{ item.raw.maSanPham }}</span>
                            </div>
                            <div class="d-flex align-center">
                                <span class="font-weight-medium mr-1">Mã SKU:</span>
                                <span>{{ item.raw.maChiTietSanPham || '--' }}</span>
                            </div>
                        </div>
                        
                        <div class="d-flex align-center justify-space-between text-slate-600">
                            <div class="d-flex align-center gap-4">
                                <div><span class="font-weight-medium text-slate-700">Màu:</span> {{ item.raw.tenMauSac || '--' }}</div>
                                <div><span class="font-weight-medium text-slate-700">Cỡ:</span> {{ item.raw.tenKichThuoc || '--' }}</div>
                            </div>
                            <div class="font-weight-medium" :class="getVariantStock(item.raw) > 0 ? 'text-primary' : 'text-error'">
                                Tồn kho: {{ getVariantStock(item.raw) }}
                            </div>
                        </div>
                    </div>
                </v-list-item>
            </template>
            
            <template #no-data>
                <div class="pa-4 text-center text-slate-500">
                    <v-icon size="32" class="mb-2">mdi-package-variant-closed</v-icon>
                    <div>Không tìm thấy sản phẩm phù hợp</div>
                </div>
            </template>
        </v-autocomplete>

        <!-- Filter Row (Below Search Bar) -->
        <div class="d-flex align-center gap-2 mt-3">
            <v-select v-model="filters.thuongHieu" :items="mapFilterItems(filterOptions.thuongHieu, 'Thương hiệu')" item-title="title" item-value="value" density="compact" variant="outlined" placeholder="Thương hiệu" hide-details class="mini-filter" bg-color="white"></v-select>
            <v-select v-model="filters.danhMuc" :items="mapFilterItems(filterOptions.danhMuc, 'Danh mục')" item-title="title" item-value="value" density="compact" variant="outlined" placeholder="Danh mục" hide-details class="mini-filter" bg-color="white"></v-select>
            <v-select v-model="filters.mauSac" :items="mapFilterItems(filterOptions.mauSac, 'Màu sắc')" item-title="title" item-value="value" density="compact" variant="outlined" placeholder="Màu sắc" hide-details class="mini-filter" bg-color="white"></v-select>
            <v-select v-model="filters.kichCo" :items="mapFilterItems(filterOptions.kichCo, 'Kích cỡ')" item-title="title" item-value="value" density="compact" variant="outlined" placeholder="Kích cỡ" hide-details class="mini-filter" bg-color="white"></v-select>
            <v-btn icon variant="text" size="small" @click="resetFilters" class="reset-filter-btn">
                <v-icon size="18">mdi-refresh</v-icon>
            </v-btn>
        </div>

        <!-- QR Scanner Dialog -->
        <v-dialog v-model="showScanner" max-width="500" transition="dialog-bottom-transition">
            <v-card class="rounded-xl pa-4">
                <div class="d-flex justify-space-between align-center mb-4">
                    <span class="text-h6 font-weight-bold">Quét mã sản phẩm</span>
                    <v-btn icon variant="text" @click="stopScanner">
                        <v-icon>mdi-close</v-icon>
                    </v-btn>
                </div>
                <div id="reader" style="width: 100%"></div>
                <div class="mt-4 text-center text-caption text-grey">Đưa mã QR hoặc Barcode của sản phẩm vào khung hình</div>
            </v-card>
        </v-dialog>
    </div>
</template>

<style scoped>
.pos-search-bar :deep(.v-field),
.mini-filter :deep(.v-field) {
    border-radius: 20px !important; /* Pill shape */
    background-color: white !important;
    box-shadow: none !important;
}

.pos-search-bar :deep(.v-field__outline),
.mini-filter :deep(.v-field__outline) {
    color: #cbd5e1 !important; /* Light border */
    --v-field-border-width: 1px !important;
    --v-field-border-opacity: 1 !important;
    transition: color 0.2s ease, opacity 0.2s ease !important;
}

.pos-search-bar :deep(.v-field:hover .v-field__outline),
.mini-filter :deep(.v-field:hover .v-field__outline) {
    color: #1e257c !important; /* Navy hover */
    --v-field-border-opacity: 0.6 !important;
}

.pos-search-bar :deep(.v-field--focused .v-field__outline),
.mini-filter :deep(.v-field--focused .v-field__outline) {
    color: #1e257c !important; /* Navy focus */
    --v-field-border-width: 1px !important;
    --v-field-border-opacity: 1 !important;
}

/* Standardized primary text color and sizing to match invoice filters */
.pos-search-bar :deep(.v-field__input),
.pos-search-bar :deep(.v-field__input input),
.mini-filter :deep(.v-field__input),
.mini-filter :deep(.v-field__input input),
.mini-filter :deep(.v-select__selection-text),
.mini-filter :deep(.v-select__selection) {
    font-size: 13px !important;
    color: #334155 !important; /* Slate 700 */
    font-weight: 400 !important;
    min-height: 32px !important;
    padding-top: 2px !important;
    padding-bottom: 2px !important;
}

.pos-search-bar :deep(input::placeholder),
.pos-search-bar :deep(.v-field__input::placeholder),
.mini-filter :deep(input::placeholder),
.mini-filter :deep(.v-field__input::placeholder),
.mini-filter :deep(.v-field-label) {
    font-size: 13px !important;
    color: #94a3b8 !important;
    opacity: 0.8 !important;
    font-weight: 400 !important;
}

.pos-variant-item {
    margin: 8px 12px !important;
    border-radius: 12px !important;
    border: 1px solid #e2e8f0 !important;
    padding: 12px !important;
    transition: all 0.2s ease !important;
}

.pos-variant-item:hover:not(.v-list-item--disabled) {
    background-color: #f8fafc !important;
    border-color: #cbd5e1 !important;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05) !important;
}

:deep(.pos-search-menu) {
    border-radius: 16px !important;
    box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.05) !important;
    border: 1px solid #e2e8f0 !important;
    background: white !important;
    padding: 8px 0 !important;
}

/* Dropdown arrow scaling */
.mini-filter :deep(.v-field__append-inner .v-icon) {
    font-size: 16px !important;
    color: #94a3b8 !important;
    opacity: 0.8;
}

.reset-filter-btn {
    color: #64748b !important;
}

.border-b {
    border-bottom: 1px solid #f1f5f9 !important;
}

.gap-2 { gap: 8px; }
.gap-4 { gap: 16px; }
.gap-6 { gap: 24px; }

.product-code-pastel-chip {
    display: inline-flex;
    align-items: center;
    background-color: #fcfcfc !important; /* pastel pink bg */
    border: 1px solid #0e18a5 !important; /* pastel pink border */
    color: #323ee6 !important; /* pink text */
    font-size: 13px !important;
    font-weight: 500 !important;
    padding: 2px 8px !important;
    border-radius: 6px !important;
    line-height: 1.2 !important;
}
</style>
