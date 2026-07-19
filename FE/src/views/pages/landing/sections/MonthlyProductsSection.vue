<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useCartStore } from '@/stores/cartStore';
import { dichVuFile } from '@/services/core/dichVuFile';

const props = defineProps({
    active: Boolean,
    warm: Boolean,
    variants: {
        type: Array,
        default: () => []
    },
    title: {
        type: String,
        default: 'SẢN PHẨM NỔI BẬT'
    },
    subtitle: {
        type: String,
        default: 'Được yêu thích nhất tháng này'
    }
});

const router = useRouter();
let cartStore;
try { cartStore = useCartStore(); } catch { cartStore = null; }

const isAbsoluteUrl = (v) => /^(https?:)?\/\//i.test(v) || v?.startsWith('data:') || v?.startsWith('blob:');
const resolveImg = (v) => {
    if (!v) return '';
    if (isAbsoluteUrl(v)) return v;
    return dichVuFile.layUrlFile(v.replace(/^\/+/, ''));
};

const formatPrice = (v) => {
    if (!v && v !== 0) return '';
    return new Intl.NumberFormat('vi-VN').format(v) + ' đ';
};

// Hiển thị từng biến thể riêng lẻ (mỗi card = 1 biến thể cụ thể)
const displayProducts = computed(() => {
    if (!props.variants || props.variants.length === 0) return [];
    return props.variants.slice(0, 4).map((v) => ({
        id: v.id,
        idSanPham: v.idSanPham,
        maSanPham: v.maSanPham,
        tenSanPham: v.tenSanPham,
        tenThuongHieu: v.tenThuongHieu,
        maMauHex: v.maMauHex,
        hinhAnh: resolveImg(v.hinhAnh),
        giaBan: v.giaBan,
        phanTramGiam: Number(v.phanTramGiam) || 0,
        soLuong: v.soLuong || 0,
        sizes: v.tenKichThuoc ? [v.tenKichThuoc] : [],
        tenMauSac: v.tenMauSac
    }));
});

const goToProduct = (product) => {
    router.push(`/product/${product.idSanPham}`);
};

const addToCart = (e, product) => {
    e.stopPropagation();
    if (cartStore) {
        cartStore.addItem({ ...product, soLuongMua: 1 });
    } else {
        router.push(`/product/${product.idSanPham}`);
    }
};
</script>

<template>
    <section class="snap-section monthly-section d-flex align-center">
        <template v-if="props.active || props.warm">
            <v-container fluid class="fill-height pa-6">
                <v-row no-gutters class="fill-height align-center justify-center">
                    <v-col cols="12" class="px-4">
                        <!-- Header -->
                        <div class="section-header reveal-container mb-6" :class="{ active: props.active }">
                            <div class="d-flex align-center justify-space-between">
                                <div class="d-flex align-center" style="gap: 12px;">
                                    <div class="header-bar"></div>
                                    <div>
                                        <h2 class="section-title reveal-item delay-1">{{ props.title }}</h2>
                                        <p class="section-subtitle reveal-item delay-2">{{ props.subtitle }}</p>
                                    </div>
                                </div>
                                <v-btn variant="text" color="red-darken-2" class="reveal-item delay-2 see-all-btn"
                                    to="/shoes">
                                    Xem tất cả <v-icon>mdi-arrow-right</v-icon>
                                </v-btn>
                            </div>
                        </div>

                        <!-- Product Grid -->
                        <div class="product-grid reveal-container" :class="{ active: props.active }">
                            <v-row v-if="displayProducts.length > 0">
                                <v-col v-for="(product, index) in displayProducts" :key="product.id" cols="6" md="3"
                                    class="product-col">
                                    <div class="premium-product-card" :style="{ transitionDelay: `${0.05 * index}s` }"
                                        @click="goToProduct(product)">
                                        <div class="card-image-wrap">
                                            <!-- Badges overlay -->
                                            <div class="premium-badges">
                                                <div class="badge-new">NEW</div>
                                                <div v-if="product.phanTramGiam > 0" class="badge-sale">
                                                    -{{ product.phanTramGiam }}%
                                                </div>
                                            </div>

                                            <!-- Main Image -->
                                            <img v-if="product.hinhAnh" :src="product.hinhAnh" :alt="product.tenSanPham"
                                                class="premium-img" />
                                            <div v-else class="premium-placeholder"
                                                :style="{ backgroundColor: product.maMauHex || '#e2e8f0' }"></div>

                                            <!-- Hover Actions -->
                                            <div class="premium-hover-overlay">
                                                <div class="action-buttons">
                                                    <v-btn icon class="hover-btn eye-btn" size="40"
                                                        @click.stop="goToProduct(product)">
                                                        <v-icon size="20">mdi-eye-outline</v-icon>
                                                    </v-btn>
                                                    <v-btn icon class="hover-btn cart-btn" size="40"
                                                        @click.stop="(e) => addToCart(e, product)">
                                                        <v-icon size="20">mdi-cart-outline</v-icon>
                                                    </v-btn>
                                                </div>
                                            </div>
                                        </div>

                                        <!-- Info Section -->
                                        <div class="card-info-wrap">
                                            <div class="d-flex align-center justify-space-between mb-2">
                                                <span class="premium-brand">{{ product.tenThuongHieu || 'AeroStride'
                                                    }}</span>
                                                <span class="premium-stock"
                                                    :class="product.soLuong > 0 ? 'in-stock' : 'out-stock'">
                                                    {{ product.soLuong > 0 ? `Còn ${product.soLuong}` : 'Hết' }}
                                                </span>
                                            </div>

                                            <h3 class="premium-title">{{ product.tenSanPham }}</h3>

                                            <div class="premium-price-row mt-2 mb-3">
                                                <span v-if="product.giaBan != null" class="price-main">{{
                                                    formatPrice(product.giaBan) }}</span>
                                                <span v-else class="price-contact">Liên hệ</span>
                                                <span v-if="product.phanTramGiam > 0" class="price-old">
                                                    {{ formatPrice(product.giaBan / (1 - product.phanTramGiam / 100)) }}
                                                </span>
                                            </div>

                                            <div class="premium-attributes mt-auto">
                                                <div v-if="product.tenMauSac" class="attr-pill">
                                                    <span class="color-dot"
                                                        :style="{ backgroundColor: product.maMauHex || '#000' }"></span>
                                                    {{ product.tenMauSac }}
                                                </div>
                                                <div v-if="product.sizes[0]" class="attr-pill">
                                                    <v-icon size="12" class="mr-1"
                                                        color="grey-darken-1">mdi-ruler</v-icon>
                                                    {{ product.sizes[0] }}
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </v-col>
                            </v-row>
                            <v-row v-else>
                                <v-col v-for="n in 4" :key="n" cols="6" md="3">
                                    <v-skeleton-loader type="card, article"
                                        class="rounded-xl elevation-2"></v-skeleton-loader>
                                </v-col>
                            </v-row>
                        </div>
                    </v-col>
                </v-row>
            </v-container>
        </template>
    </section>
</template>

<style scoped lang="scss">
.header-bar {
    width: 4px;
    height: 36px;
    background: #d32f2f;
    border-radius: 4px;
    flex-shrink: 0;
}

.section-title {
    font-size: 1.3rem;
    font-weight: 900;
    color: #1e293b;
    letter-spacing: 0.5px;
    margin: 0;
    text-transform: uppercase;
}

.section-subtitle {
    font-size: 0.8rem;
    color: #94a3b8;
    margin: 2px 0 0;
}

.see-all-btn {
    font-weight: 700;
    font-size: 0.85rem;
}

.premium-product-card {
    background: #ffffff;
    border-radius: 20px;
    border: 1px solid rgba(226, 232, 240, 0.8);
    overflow: hidden;
    cursor: pointer;
    opacity: 0;
    transform: translateY(30px);
    transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
    display: flex;
    flex-direction: column;
    height: 100%;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);

    .active & {
        opacity: 1;
        transform: translateY(0);
    }

    &:hover {
        transform: translateY(-8px);
        box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
        border-color: rgba(41, 98, 255, 0.3);

        .premium-img {
            transform: scale(1.08) rotate(-2deg);
        }

        .premium-hover-overlay {
            opacity: 1;
        }

        .action-buttons {
            transform: translateY(0);
        }

        .premium-title {
            color: #2962ff;
        }
    }
}

.card-image-wrap {
    position: relative;
    height: 250px;
    background: #f8fafc;
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;
}

.premium-img {
    width: 85%;
    height: 85%;
    object-fit: contain;
    transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
    filter: drop-shadow(0 15px 25px rgba(0, 0, 0, 0.15));
}

.premium-placeholder {
    width: 100%;
    height: 100%;
    opacity: 0.9;
}

.premium-badges {
    position: absolute;
    top: 16px;
    left: 16px;
    z-index: 2;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.badge-new,
.badge-sale {
    padding: 4px 12px;
    border-radius: 20px;
    font-size: 0.7rem;
    font-weight: 800;
    letter-spacing: 0.5px;
    text-transform: uppercase;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
    backdrop-filter: blur(8px);
}

.badge-new {
    background: rgba(46, 125, 50, 0.9);
    color: #fff;
}

.badge-sale {
    background: rgba(211, 47, 47, 0.9);
    color: #fff;
}

.premium-hover-overlay {
    position: absolute;
    inset: 0;
    background: rgba(255, 255, 255, 0.2);
    backdrop-filter: blur(4px);
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: opacity 0.4s ease;
    z-index: 3;
}

.action-buttons {
    display: flex;
    gap: 12px;
    transform: translateY(20px);
    transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.hover-btn {
    background: #ffffff !important;
    color: #1e293b !important;
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1) !important;
    transition: all 0.3s ease !important;

    &:hover {
        background: #2962ff !important;
        color: #ffffff !important;
        transform: scale(1.1);
    }

    &.cart-btn:hover {
        background: #d32f2f !important;
    }
}

.card-info-wrap {
    padding: 20px;
    display: flex;
    flex-direction: column;
    flex-grow: 1;
}

.premium-brand {
    font-size: 0.75rem;
    font-weight: 800;
    color: #2962ff;
    letter-spacing: 1px;
    text-transform: uppercase;
}

.premium-stock {
    font-size: 0.7rem;
    font-weight: 700;
    padding: 4px 10px;
    border-radius: 12px;

    &.in-stock {
        background: rgba(46, 125, 50, 0.1);
        color: #2e7d32;
    }

    &.out-stock {
        background: rgba(211, 47, 47, 0.1);
        color: #d32f2f;
    }
}

.premium-title {
    font-size: 1.05rem;
    font-weight: 700;
    color: #0f172a;
    line-height: 1.4;
    margin: 4px 0 0;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    transition: color 0.3s ease;
}

.premium-price-row {
    display: flex;
    align-items: baseline;
    gap: 10px;
}

.price-main {
    font-size: 1.25rem;
    font-weight: 900;
    color: #1e293b;
}

.price-old {
    font-size: 0.85rem;
    font-weight: 600;
    color: #94a3b8;
    text-decoration: line-through;
}

.premium-attributes {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.attr-pill {
    display: flex;
    align-items: center;
    background: #f1f5f9;
    padding: 6px 12px;
    border-radius: 8px;
    font-size: 0.75rem;
    font-weight: 600;
    color: #475569;
    border: 1px solid #e2e8f0;
}

.color-dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    margin-right: 6px;
    box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
    border: 1px solid rgba(0, 0, 0, 0.05);
}

@media (max-width: 960px) {
    .section-title {
        font-size: 1.1rem;
    }

    .card-image-wrap {
        height: 180px;
    }

    .premium-title {
        font-size: 0.9rem;
    }

    .price-main {
        font-size: 1.1rem;
    }
}
</style>
