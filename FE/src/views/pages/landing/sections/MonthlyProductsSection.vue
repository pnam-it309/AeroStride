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
    return props.variants.slice(0, 5).map((v) => ({
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
    <section class="snap-section monthly-section">
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
                                <v-btn variant="text" color="red-darken-2" class="reveal-item delay-2 see-all-btn" to="/shoes">
                                    Xem tất cả <v-icon>mdi-arrow-right</v-icon>
                                </v-btn>
                            </div>
                        </div>

                        <!-- Product Grid -->
                        <div class="product-grid reveal-container" :class="{ active: props.active }">
                            <v-row v-if="displayProducts.length > 0">
                                <v-col
                                    v-for="(product, index) in displayProducts"
                                    :key="product.id"
                                    cols="6"
                                    md="3"
                                    class="product-col"
                                >
                                    <div
                                        class="product-card"
                                        :style="{ transitionDelay: `${0.08 * index}s` }"
                                        @click="goToProduct(product)"
                                    >
                                        <!-- Badges -->
                                        <div class="card-badges">
                                            <span class="badge-new">Mới</span>
                                            <span v-if="product.phanTramGiam > 0" class="badge-discount">
                                                -{{ product.phanTramGiam }}%
                                            </span>
                                        </div>

                                        <!-- Image -->
                                        <div class="card-image">
                                            <img
                                                v-if="product.hinhAnh"
                                                :src="product.hinhAnh"
                                                :alt="product.tenSanPham"
                                                class="product-img"
                                            />
                                            <div
                                                v-else
                                                class="image-placeholder"
                                                :style="{ backgroundColor: product.maMauHex || '#2962ff' }"
                                            ></div>
                                            <!-- Hover Actions -->
                                            <div class="card-actions">
                                                <v-btn icon size="small" color="grey-darken-4" class="action-btn" @click.stop="goToProduct(product)">
                                                    <v-icon size="18">mdi-eye-outline</v-icon>
                                                </v-btn>
                                                <v-btn icon size="small" color="red-darken-2" class="action-btn" @click="(e) => addToCart(e, product)">
                                                    <v-icon size="18">mdi-cart-plus</v-icon>
                                                </v-btn>
                                            </div>
                                        </div>

                                        <!-- Info -->
                                        <div class="card-info">
                                            <div class="brand-name d-flex align-center justify-space-between">
                                                <span>{{ product.tenThuongHieu || 'AeroStride' }}</span>
                                                <span class="px-2 py-0.5 rounded-pill text-caption font-weight-bold"
                                                      :style="product.soLuong > 0 ? 'background: rgba(46, 125, 50, 0.15); color: #2e7d32; font-size: 0.7rem;' : 'background: rgba(198, 40, 40, 0.15); color: #c62828; font-size: 0.7rem;'">
                                                    {{ product.soLuong > 0 ? `Còn ${product.soLuong}` : 'Hết' }}
                                                </span>
                                            </div>
                                            <div class="price-row">
                                                <span v-if="product.giaBan != null" class="current-price">{{ formatPrice(product.giaBan) }}</span>
                                                <span v-else class="out-of-stock">Liên hệ</span>
                                            </div>
                                            <div class="size-list">
                                                <span v-if="product.tenMauSac" class="size-chip color-chip">{{ product.tenMauSac }}</span>
                                                <span v-if="product.sizes[0]" class="size-chip">Size {{ product.sizes[0] }}</span>
                                            </div>
                                            <div class="product-name">{{ product.tenSanPham }}</div>
                                        </div>
                                    </div>
                                </v-col>
                            </v-row>
                            <v-row v-else>
                                <v-col v-for="n in 4" :key="n" cols="6" md="3">
                                    <v-skeleton-loader type="card, article" class="rounded-xl elevation-2"></v-skeleton-loader>
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

.product-card {
    background: #ffffff;
    border-radius: 12px;
    border: 1px solid #e2e8f0;
    overflow: hidden;
    cursor: pointer;
    opacity: 0;
    transform: translateY(20px);
    transition: all 0.5s cubic-bezier(0.2, 1, 0.3, 1);

    .active & {
        opacity: 1;
        transform: translateY(0);
    }

    &:hover {
        box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
        border-color: rgba(211, 47, 47, 0.2);
        transform: translateY(-4px);
        .card-actions { opacity: 1; }
    }
}

.card-badges {
    display: flex;
    gap: 4px;
    padding: 8px 8px 0;
    min-height: 28px;
}

.badge-new {
    background: #2e7d32;
    color: #fff;
    font-size: 0.65rem;
    font-weight: 800;
    padding: 2px 8px;
    border-radius: 10px;
}

.badge-discount {
    background: #d32f2f;
    color: #fff;
    font-size: 0.65rem;
    font-weight: 800;
    padding: 2px 8px;
    border-radius: 10px;
}

.card-image {
    position: relative;
    height: 180px;
    background: #f8fafc;
    overflow: hidden;
}

.product-img {
    width: 100%;
    height: 100%;
    object-fit: contain;
    padding: 8px;
    transition: transform 0.4s ease;
    .product-card:hover & { transform: scale(1.05); }
}

.image-placeholder {
    width: 100%;
    height: 100%;
    opacity: 0.8;
}

.card-actions {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    background: rgba(0, 0, 0, 0.1);
    opacity: 0;
    transition: opacity 0.3s ease;
}

.action-btn { box-shadow: 0 2px 8px rgba(0,0,0,0.2) !important; }

.card-info { padding: 10px 12px 12px; }

.product-code {
    font-size: 0.72rem;
    font-weight: 700;
    color: #d32f2f;
    margin-bottom: 4px;
    display: flex;
    align-items: center;
    gap: 4px;
    &::before {
        content: '';
        display: inline-block;
        width: 6px;
        height: 6px;
        background: #d32f2f;
        border-radius: 50%;
    }
}

.price-row {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 4px;
}

.price-current {
    font-size: 1rem;
    font-weight: 900;
    color: #d32f2f;
}

.stock-status {
    margin-bottom: 6px;
    .in-stock {
        font-size: 0.7rem;
        font-weight: 700;
        color: #2e7d32;
        border: 1px solid #c8e6c9;
        background: #f1f8e9;
        padding: 2px 8px;
        border-radius: 8px;
    }
    .out-of-stock {
        font-size: 0.7rem;
        font-weight: 700;
        color: #9e9e9e;
        border: 1px solid #e0e0e0;
        background: #fafafa;
        padding: 2px 8px;
        border-radius: 8px;
    }
}



.product-name {
    font-size: 0.85rem;
    font-weight: 700;
    color: #1e293b;
    line-height: 1.3;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

@media (max-width: 960px) {
    .section-title { font-size: 1rem; }
    .card-image { height: 140px; }
    .product-name { font-size: 0.75rem; }
}
</style>
