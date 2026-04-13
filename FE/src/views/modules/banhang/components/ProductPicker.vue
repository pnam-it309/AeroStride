<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { SearchIcon, ShoppingCartIcon, BoxIcon, QrcodeIcon, XIcon } from 'vue-tabler-icons';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';

const props = defineProps(['activeOrderId']);
const emit = defineEmits(['add-product']);

const search = ref('');
const results = ref([]);
const loading = ref(false);

const onSearch = async () => {
    if (!search.value || search.value.length < 2) {
        results.value = [];
        return;
    }
    loading.value = true;
    try {
        const data = await dichVuDonHang.searchSanPham(search.value);
        results.value = data || [];
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
});

onUnmounted(() => {
    window.removeEventListener('keydown', handleGlobalKeyDown);
});

const selectProduct = (p) => {
    emit('add-product', p);
    search.value = '';
    results.value = [];
};

const formatCurrency = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);
</script>

<template>
    <div class="product-picker position-relative">
        <div class="d-flex gap-2">
            <v-text-field
                v-model="search"
                placeholder="Tìm sản phẩm bằng tên, mã, barcode..."
                variant="outlined"
                prepend-inner-icon="mdi-magnify"
                hide-details
                bg-color="white"
                class="rounded-lg shadow-sm flex-grow-1"
                @input="onSearch"
            ></v-text-field>
            <v-btn icon color="#2E4E8E" variant="flat" size="56" class="rounded-lg" @click="startScanner">
                <QrcodeIcon size="24" />
            </v-btn>
        </div>

        <!-- QR Scanner Dialog -->
        <v-dialog v-model="showScanner" max-width="500">
            <v-card class="rounded-xl pa-4">
                <div class="d-flex justify-space-between align-center mb-4">
                    <span class="text-h6 font-weight-bold">Quét mã sản phẩm</span>
                    <v-btn icon variant="text" @click="stopScanner"><XIcon /></v-btn>
                </div>
                <div id="reader" style="width: 100%"></div>
                <div class="mt-4 text-center text-caption text-grey">Đưa mã QR hoặc Barcode của sản phẩm vào khung hình</div>
            </v-card>
        </v-dialog>

        <!-- Search Results Dropdown -->
        <v-card v-if="results.length > 0" class="search-overlay mt-2 shadow-xl border overflow-hidden">
            <v-list class="pa-0">
                <v-list-item v-for="p in results" :key="p.id" class="pa-4 border-b hover-active" @click="selectProduct(p)">
                    <template v-slot:prepend>
                        <v-avatar color="grey-lighten-4" rounded="lg" size="48">
                            <BoxIcon size="24" class="text-grey-darken-1" />
                        </v-avatar>
                    </template>

                    <v-list-item-title class="font-weight-bold text-subtitle-1">
                        {{ p.tenSanPham }}
                    </v-list-item-title>

                    <v-list-item-subtitle class="mt-1 d-flex gap-2">
                        <v-chip size="x-small" label color="primary" variant="tonal">
                            {{ p.tenMauSac }}
                        </v-chip>
                        <v-chip size="x-small" label color="secondary" variant="tonal"> Size {{ p.tenKichThuoc }} </v-chip>
                        <span class="text-caption ml-2"
                            >Tồn: <b>{{ p.soLuongTon }}</b></span
                        >
                    </v-list-item-subtitle>

                    <template v-slot:append>
                        <div class="text-right">
                            <div class="text-h6 font-weight-bold text-primary">{{ formatCurrency(p.giaBan) }}</div>
                            <div class="text-caption text-grey">{{ p.maChiTietSanPham }}</div>
                        </div>
                    </template>
                </v-list-item>
            </v-list>
        </v-card>

        <v-card v-else-if="search && !loading && search.length >= 2" class="search-overlay mt-2 pa-6 text-center text-grey">
            Không tìm thấy sản phẩm nào khớp với "{{ search }}"
        </v-card>
    </div>
</template>

<style scoped>
.search-overlay {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    z-index: 999;
    max-height: 500px;
    overflow-y: auto;
}

.hover-active:hover {
    background-color: var(--v-secondary-lighten-5);
    cursor: pointer;
}

.gap-2 {
    gap: 8px;
}
</style>
