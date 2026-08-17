<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuLanding } from '@/services/public/dichVuLanding';
import { formatCurrency } from '@/utils/formatters';

const router = useRouter();

const flashSale = ref(null);
const loading = ref(true);
const remainingSeconds = ref(0);
let timerInterval = null;

const hours = computed(() => {
    const h = Math.floor(remainingSeconds.value / 3600);
    return String(h).padStart(2, '0');
});

const minutes = computed(() => {
    const m = Math.floor((remainingSeconds.value % 3600) / 60);
    return String(m).padStart(2, '0');
});

const seconds = computed(() => {
    const s = remainingSeconds.value % 60;
    return String(s).padStart(2, '0');
});

const startCountdown = () => {
    if (timerInterval) clearInterval(timerInterval);
    
    timerInterval = setInterval(() => {
        if (remainingSeconds.value > 0) {
            remainingSeconds.value--;
        } else {
            clearInterval(timerInterval);
            // Refresh flash sale state when timer reaches 0
            loadFlashSale();
        }
    }, 1000);
};

const loadFlashSale = async () => {
    loading.value = true;
    try {
        const data = await dichVuLanding.layFlashSale();
        flashSale.value = data;
        if (data && data.remainingMillis) {
            remainingSeconds.value = Math.floor(data.remainingMillis / 1000);
            startCountdown();
        }
    } catch (e) {
        console.error('Lỗi tải Flash Sale:', e);
    } finally {
        loading.value = false;
    }
};

const goToProduct = (idSanPham) => {
    if (idSanPham) {
        router.push(`/products/${idSanPham}`);
    } else {
        router.push('/products');
    }
};

onMounted(() => {
    loadFlashSale();
});

onBeforeUnmount(() => {
    if (timerInterval) clearInterval(timerInterval);
});
</script>

<template>
    <section v-if="flashSale && flashSale.items && flashSale.items.length > 0" class="flash-sale-section py-12">
        <v-container class="max-w-7xl">
            <!-- Flash Sale Main Container Card -->
            <div class="flash-sale-container rounded-3xl overflow-hidden elevation-8">
                <!-- Header Banner -->
                <div class="flash-sale-banner pa-6 pa-md-8 d-flex flex-column flex-md-row align-center justify-space-between gap-6">
                    <div class="d-flex align-center gap-4 text-center text-md-left">
                        <div class="lightning-badge">
                            <v-icon icon="mdi-flash" size="36" color="amber-accent-2" class="lightning-pulse" />
                        </div>
                        <div>
                            <div class="d-flex align-center flex-wrap justify-center justify-md-start gap-2 mb-1">
                                <h2 class="text-h4 font-weight-black text-white tracking-tight uppercase mb-0">
                                    FLASH SALE GIỜ VÀNG
                                </h2>
                                <v-chip
                                    v-if="flashSale.khungGio"
                                    color="amber-lighten-4"
                                    variant="flat"
                                    class="text-amber-darken-4 font-weight-black px-3"
                                >
                                    ⏰ {{ flashSale.khungGio }}
                                </v-chip>
                            </div>
                            <p class="text-subtitle-1 text-amber-100 font-weight-medium mb-0">
                                {{ flashSale.isHappening ? 'Ưu đãi cực sốc - Số lượng có hạn - Săn ngay kẻo lỡ!' : 'Khung giờ vàng sắp diễn ra, chuẩn bị săn deal!' }}
                            </p>
                        </div>
                    </div>

                    <!-- Countdown Timer Display -->
                    <div class="countdown-wrapper d-flex align-center gap-3">
                        <span class="text-caption font-weight-bold text-amber-200 uppercase letter-spacing-1">
                            {{ flashSale.isHappening ? 'KẾT THÚC SAU:' : 'BẮT ĐẦU TRONG:' }}
                        </span>
                        <div class="d-flex align-center gap-1.5 timer-boxes">
                            <div class="timer-unit">
                                <div class="timer-number">{{ hours }}</div>
                                <div class="timer-label">Giờ</div>
                            </div>
                            <span class="timer-colon">:</span>
                            <div class="timer-unit">
                                <div class="timer-number">{{ minutes }}</div>
                                <div class="timer-label">Phút</div>
                            </div>
                            <span class="timer-colon">:</span>
                            <div class="timer-unit pulse-sec">
                                <div class="timer-number">{{ seconds }}</div>
                                <div class="timer-label">Giây</div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Products Carousel / Grid -->
                <div class="flash-sale-products-body pa-6 pa-md-8">
                    <v-row>
                        <v-col
                            v-for="item in flashSale.items"
                            :key="item.id"
                            cols="12"
                            sm="6"
                            md="4"
                            lg="3"
                        >
                            <v-card
                                class="flash-product-card rounded-2xl overflow-hidden h-100 d-flex flex-column transition-all cursor-pointer"
                                elevation="2"
                                @click="goToProduct(item.idSanPham)"
                            >
                                <!-- Thumbnail Box -->
                                <div class="product-img-wrapper position-relative overflow-hidden">
                                    <v-img
                                        :src="item.hinhAnh || '/placeholder-shoe.png'"
                                        height="220"
                                        cover
                                        class="product-img bg-slate-50"
                                    >
                                        <template #placeholder>
                                            <div class="d-flex align-center justify-center fill-height bg-slate-100">
                                                <v-icon icon="mdi-shoe-sneaker" size="48" color="slate-300" />
                                            </div>
                                        </template>
                                    </v-img>

                                    <!-- Discount Badge -->
                                    <div class="discount-badge">
                                        <span class="font-weight-black text-body-2">-{{ item.phanTramGiam || 30 }}%</span>
                                        <span class="text-xxs uppercase">OFF</span>
                                    </div>

                                    <!-- Hot Flame Tag -->
                                    <div class="hot-tag">
                                        <v-icon icon="mdi-fire" size="14" color="white" />
                                        <span>HOT DEAL</span>
                                    </div>
                                </div>

                                <!-- Card Content -->
                                <div class="pa-4 d-flex flex-column flex-grow-1 justify-space-between bg-white">
                                    <div>
                                        <div class="text-caption font-weight-bold text-slate-400 uppercase tracking-wider mb-1">
                                            {{ item.tenThuongHieu || 'AeroStride' }}
                                        </div>
                                        <h3 class="text-subtitle-1 font-weight-bold text-slate-900 line-clamp-1 mb-1" :title="item.tenSanPham">
                                            {{ item.tenSanPham }}
                                        </h3>
                                        <div class="d-flex align-center gap-2 mb-3">
                                            <v-chip size="x-small" variant="outlined" color="slate-600" class="font-weight-medium">
                                                Size: {{ item.tenKichThuoc }}
                                            </v-chip>
                                            <v-chip size="x-small" variant="outlined" color="slate-600" class="font-weight-medium">
                                                Màu: {{ item.tenMauSac }}
                                            </v-chip>
                                        </div>
                                    </div>

                                    <div>
                                        <!-- Price -->
                                        <div class="d-flex align-baseline gap-2 mb-3">
                                            <span class="text-h6 font-weight-black text-red-darken-2">
                                                {{ formatCurrency(item.giaFlashSale) }}
                                            </span>
                                            <span class="text-caption text-slate-400 text-decoration-line-through">
                                                {{ formatCurrency(item.giaGoc) }}
                                            </span>
                                        </div>

                                        <!-- Progress Bar of Sold Items -->
                                        <div class="stock-progress-box mb-3">
                                            <div class="d-flex justify-space-between text-caption font-weight-bold text-amber-darken-4 mb-1">
                                                <span>🔥 Đã bán {{ item.daBan || 12 }} đôi</span>
                                                <span>Còn {{ item.soLuong || 5 }} đôi</span>
                                            </div>
                                            <div class="progress-bar-track">
                                                <div
                                                    class="progress-bar-fill"
                                                    :style="{ width: `${Math.min(100, Math.max(25, (item.daBan / ((item.daBan || 12) + (item.soLuong || 5))) * 100))}%` }"
                                                ></div>
                                            </div>
                                        </div>

                                        <!-- Action Button -->
                                        <v-btn
                                            block
                                            color="red-darken-2"
                                            variant="flat"
                                            class="rounded-xl font-weight-bold text-white shadow-md buy-now-btn"
                                            prepend-icon="mdi-cart-plus"
                                        >
                                            SĂN DEAL NGAY
                                        </v-btn>
                                    </div>
                                </div>
                            </v-card>
                        </v-col>
                    </v-row>
                </div>
            </div>
        </v-container>
    </section>
</template>

<style scoped>
.flash-sale-container {
    background: #0f172a;
    border: 2px solid rgba(245, 158, 11, 0.4);
    box-shadow: 0 20px 40px -15px rgba(239, 68, 68, 0.35);
}

.flash-sale-banner {
    background: linear-gradient(135deg, #b91c1c 0%, #dc2626 40%, #d97706 100%);
    border-bottom: 2px solid rgba(254, 240, 138, 0.2);
}

.lightning-badge {
    width: 60px;
    height: 60px;
    background: rgba(0, 0, 0, 0.25);
    border-radius: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 2px solid rgba(254, 240, 138, 0.5);
    box-shadow: 0 0 20px rgba(251, 191, 36, 0.4);
}

.lightning-pulse {
    animation: flashGlow 1.2s infinite ease-in-out;
}

@keyframes flashGlow {
    0%, 100% { transform: scale(1); filter: drop-shadow(0 0 2px #fef08a); }
    50% { transform: scale(1.18); filter: drop-shadow(0 0 12px #f59e0b); }
}

.countdown-wrapper {
    background: rgba(15, 23, 42, 0.6);
    padding: 8px 18px;
    border-radius: 9999px;
    border: 1px solid rgba(254, 240, 138, 0.3);
    backdrop-filter: blur(8px);
}

.timer-boxes {
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.timer-unit {
    background: #0f172a;
    color: #ffffff;
    border-radius: 10px;
    padding: 6px 10px;
    text-align: center;
    min-width: 48px;
    border: 1px solid rgba(245, 158, 11, 0.5);
    box-shadow: inset 0 2px 4px rgba(0,0,0,0.5);
}

.timer-number {
    font-size: 1.25rem;
    font-weight: 900;
    line-height: 1.1;
    color: #fbbf24;
}

.timer-label {
    font-size: 0.65rem;
    text-transform: uppercase;
    color: #94a3b8;
    font-weight: 700;
}

.timer-colon {
    font-size: 1.25rem;
    font-weight: 900;
    color: #fbbf24;
}

.flash-sale-products-body {
    background: #0f172a;
}

.flash-product-card {
    border: 1px solid #1e293b;
    transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.flash-product-card:hover {
    transform: translateY(-6px);
    box-shadow: 0 12px 24px -6px rgba(239, 68, 68, 0.35) !important;
}

.flash-product-card:hover .product-img {
    transform: scale(1.06);
}

.product-img-wrapper {
    background: #f8fafc;
}

.product-img {
    transition: transform 0.4s ease;
}

.discount-badge {
    position: absolute;
    top: 12px;
    right: 12px;
    background: #dc2626;
    color: white;
    padding: 4px 8px;
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    line-height: 1;
    box-shadow: 0 4px 10px rgba(220, 38, 38, 0.5);
    border: 1px solid rgba(255, 255, 255, 0.4);
    z-index: 2;
}

.hot-tag {
    position: absolute;
    bottom: 10px;
    left: 10px;
    background: linear-gradient(135deg, #ea580c 0%, #dc2626 100%);
    color: white;
    font-size: 0.65rem;
    font-weight: 900;
    letter-spacing: 0.5px;
    padding: 3px 8px;
    border-radius: 6px;
    display: flex;
    align-items: center;
    gap: 3px;
    box-shadow: 0 2px 6px rgba(0,0,0,0.3);
    z-index: 2;
}

.text-xxs {
    font-size: 0.6rem;
    font-weight: 800;
    opacity: 0.9;
}

.stock-progress-box {
    background: #fffbeb;
    border: 1px solid #fef3c7;
    border-radius: 8px;
    padding: 6px 10px;
}

.progress-bar-track {
    width: 100%;
    height: 7px;
    background: #fde68a;
    border-radius: 9999px;
    overflow: hidden;
}

.progress-bar-fill {
    height: 100%;
    background: linear-gradient(90deg, #f59e0b 0%, #ef4444 100%);
    border-radius: 9999px;
    transition: width 0.5s ease;
}

.buy-now-btn {
    background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%) !important;
    letter-spacing: 0.5px;
    transition: transform 0.2s ease, filter 0.2s ease;
}

.buy-now-btn:hover {
    filter: brightness(1.1);
    transform: scale(1.02);
}

.line-clamp-1 {
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
}
</style>
