<script setup>
import { ref, computed, onMounted } from 'vue';
import MainHeader from '@/components/shared/MainHeader.vue';
import MainFooter from '@/components/shared/MainFooter.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import { dichVuVoucher } from '@/services/public/dichVuVoucher';
import { useToastStore } from '@/stores/toastStore';
import { useSeoMeta } from '@/composables/useSeoMeta';

const SNAPSHOT_KEY = 'aerostride-voucher-snapshot';
const toast = useToastStore();
const vouchers = ref([]);
const loading = ref(false);
const changedVoucherIds = ref(new Set());

// Các field dùng để so sánh thay đổi
const COMPARE_FIELDS = ['donHangToiThieu', 'soTienGiam', 'phanTramGiamGia', 'giamToiDa', 'loaiPhieu', 'ngayKetThuc'];

const hasChangedVouchers = computed(() => changedVoucherIds.value.size > 0);

const loadSnapshot = () => {
    try {
        const raw = localStorage.getItem(SNAPSHOT_KEY);
        return raw ? JSON.parse(raw) : null;
    } catch {
        return null;
    }
};

const saveSnapshot = (voucherList) => {
    try {
        const snapshot = {};
        voucherList.forEach((v) => {
            snapshot[v.id] = {};
            COMPARE_FIELDS.forEach((f) => { snapshot[v.id][f] = v[f]; });
        });
        localStorage.setItem(SNAPSHOT_KEY, JSON.stringify(snapshot));
    } catch { /* ignore quota errors */ }
};

const detectChanges = (voucherList, snapshot) => {
    const changed = new Set();
    if (!snapshot) return changed; // lần đầu vào — chưa có gì để so sánh
    voucherList.forEach((v) => {
        const prev = snapshot[v.id];
        if (!prev) return; // voucher mới thêm — không coi là "đã thay đổi"
        const isChanged = COMPARE_FIELDS.some((f) => String(v[f] ?? '') !== String(prev[f] ?? ''));
        if (isChanged) changed.add(v.id);
    });
    return changed;
};

const fetchVouchers = async () => {
    loading.value = true;
    try {
        const data = await dichVuVoucher.layDanhSachVoucher();
        const snapshot = loadSnapshot();
        changedVoucherIds.value = detectChanges(data || [], snapshot);
        vouchers.value = data || [];
        // Cập nhật snapshot sau khi detect
        saveSnapshot(vouchers.value);
    } catch (error) {
        console.error('Error fetching vouchers:', error);
        toast.error('Không thể tải danh sách mã giảm giá.');
    } finally {
        loading.value = false;
    }
};

const copyToClipboard = (code) => {
    navigator.clipboard
        .writeText(code)
        .then(() => { toast.success(`Đã sao chép mã: ${code}`); })
        .catch((err) => { console.error('Could not copy text: ', err); });
};

const formatPrice = (price) => {
    if (!price) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const formatDate = (timestamp) => {
    if (!timestamp) return 'Không thời hạn';
    return new Date(timestamp).toLocaleDateString('vi-VN');
};

// SEO
const { setSeoMeta } = useSeoMeta();

onMounted(() => {
    fetchVouchers();
    setSeoMeta({
        title: 'Kho Voucher Ưu Đãi - Mã Giảm Giá Giày',
        description:
            'Săn mã giảm giá cực hời tại AeroStride. Voucher giảm giá giày thể thao chính hãng, cập nhật mới mỗi ngày. Chốt ngay đôi giày ưng ý với giá tốt nhất.',
        url: window.location.origin + '/vouchers'
    });
});
</script>


<template>
    <div class="voucher-listing-page bg-grey-lighten-4 min-vh-100">
        <MainHeader />

        <div class="header-spacing" style="height: 104px"></div>

        <v-container class="py-12">
            <div class="text-center mb-12">
                <h1 class="text-h3 font-weight-black mb-4">KHO VOUCHER ƯU ĐÃI</h1>
                <p class="text-h6 text-grey-darken-1">Săn mã giảm giá cực hời, chốt ngay đôi giày ưng ý tại AeroStride.</p>
            </div>

            <!-- Banner cảnh báo khi có voucher bị thay đổi -->
            <v-alert
                v-if="hasChangedVouchers && !loading"
                type="warning"
                variant="tonal"
                class="mb-6 rounded-xl"
                border="start"
                icon="mdi-bell-alert-outline"
            >
                <strong>Một số phiếu giảm giá vừa được cập nhật điều kiện.</strong>
                Vui lòng kiểm tra lại các mã được đánh dấu
                <span class="font-weight-bold" style="color:#d97706">"Đã cập nhật"</span>
                trước khi sử dụng để tránh nhầm lẫn.
            </v-alert>

            <v-row v-if="loading">
                <v-col v-for="n in 6" :key="n" cols="12" md="6" lg="4">
                    <v-skeleton-loader type="card" height="200"></v-skeleton-loader>
                </v-col>
            </v-row>

            <v-row v-else-if="vouchers.length > 0">
                <v-col v-for="v in vouchers" :key="v.id" cols="12" md="6" lg="4">
                    <div
                        class="voucher-card"
                        :class="{ 'voucher-card--updated': changedVoucherIds.has(v.id) }"
                    >
                        <!-- Badge "Đã cập nhật" -->
                        <div v-if="changedVoucherIds.has(v.id)" class="updated-badge">
                            <v-icon size="11" class="mr-1">mdi-refresh</v-icon>
                            Đã cập nhật
                        </div>

                        <div class="voucher-left" :style="{ background: v.phanTramGiamGia ? '#FF1744' : '#2962FF' }">
                            <v-icon color="white" size="40">mdi-ticket-percent</v-icon>
                            <span class="discount-val">
                                {{ v.phanTramGiamGia ? v.phanTramGiamGia + '%' : formatPrice(v.soTienGiam) }}
                            </span>
                        </div>
                        <div class="voucher-right">
                            <div class="voucher-info">
                                <h3 class="voucher-title">{{ v.ten }}</h3>
                                <p class="min-order">
                                    Đơn tối thiểu: <strong>{{ formatPrice(v.donHangToiThieu) }}</strong>
                                </p>
                                <p class="expiry">HSD: {{ formatDate(v.ngayKetThuc) }}</p>
                            </div>
                            <div class="voucher-action">
                                <div class="code-box">
                                    <span class="code">{{ v.ma }}</span>
                                    <v-btn icon="mdi-content-copy" variant="text" size="small" @click="copyToClipboard(v.ma)"></v-btn>
                                </div>
                                <v-btn color="black" block rounded="pill" class="mt-2 font-weight-bold" @click="copyToClipboard(v.ma)"
                                    >SAO CHÉP MÃ</v-btn
                                >
                            </div>
                        </div>
                        <div class="cut-out top"></div>
                        <div class="cut-out bottom"></div>
                    </div>
                </v-col>
            </v-row>

            <div v-else class="text-center py-16">
                <v-icon size="80" color="grey-lighten-1">mdi-ticket-outline</v-icon>
                <p class="text-h5 text-grey-darken-1 mt-6">Hiện tại chưa có mã giảm giá nào khả dụng.</p>
                <v-btn color="black" class="mt-8 px-8" rounded="pill" to="/shoes">TIẾP TỤC MUA SẮM</v-btn>
            </div>
        </v-container>

        <!-- Main Footer -->
        <MainFooter />

        <CustomerChat />
    </div>
</template>

<style scoped lang="scss">
.voucher-listing-page {
    padding-top: 64px;
}

.voucher-card {
    background: #fff;
    display: flex;
    height: 200px;
    border-radius: 16px;
    overflow: hidden;
    position: relative;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
    transition: transform 0.3s ease;

    &:hover {
        transform: translateY(-5px);
        box-shadow: 0 15px 40px rgba(0, 0, 0, 0.1);
    }
}

.voucher-left {
    width: 120px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 10px;
    color: white;
    padding: 20px;

    .discount-val {
        font-size: 1.5rem;
        font-weight: 900;
        text-align: center;
    }
}

.voucher-right {
    flex: 1;
    padding: 20px 25px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
}

.voucher-title {
    font-size: 1.1rem;
    font-weight: 800;
    color: #111;
    margin-bottom: 8px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.min-order,
.expiry {
    font-size: 0.85rem;
    color: #666;
    margin-bottom: 4px;
}

.code-box {
    background: #f5f5f5;
    border: 1px dashed #ccc;
    border-radius: 8px;
    padding: 4px 12px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;

    .code {
        font-family: monospace;
        font-weight: 900;
        font-size: 1.1rem;
        color: #000;
    }
}

.cut-out {
    position: absolute;
    left: 110px;
    width: 20px;
    height: 20px;
    background: #f5f5f5;
    border-radius: 50%;
    z-index: 2;

    &.top {
        top: -10px;
    }
    &.bottom {
        bottom: -10px;
    }
}

.min-vh-100 {
    min-height: 100vh;
}

/* Voucher đã bị thay đổi – viền cam nổi bật */
.voucher-card--updated {
    border: 2px solid #f59e0b;
    box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.15), 0 10px 30px rgba(0, 0, 0, 0.05);
}

/* Badge "Đã cập nhật" floating góc trên phải */
.updated-badge {
    position: absolute;
    top: 10px;
    right: 10px;
    z-index: 5;
    background: #f59e0b;
    color: #fff;
    font-size: 0.68rem;
    font-weight: 700;
    padding: 2px 8px;
    border-radius: 20px;
    display: flex;
    align-items: center;
    letter-spacing: 0.02em;
    box-shadow: 0 2px 6px rgba(0,0,0,0.15);
    white-space: nowrap;
}
</style>
