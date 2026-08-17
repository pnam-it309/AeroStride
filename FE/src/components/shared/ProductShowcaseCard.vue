<script setup>
import { computed } from 'vue';

const props = defineProps({
    product: {
        type: Object,
        required: true
    }
});

const emit = defineEmits(['view-detail']);

const formatPrice = (price) => {
    if (!price && price !== 0) return 'Liên hệ';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const originalPrice = computed(() => {
    if (!props.product.giaBan) return null;
    if (props.product.phanTramGiam && props.product.phanTramGiam > 0) {
        return props.product.giaBan / (1 - props.product.phanTramGiam / 100);
    }
    return null;
});
</script>

<template>
    <div class="product-showcase-card elevation-2" @click="emit('view-detail', product.idSanPham)">
        <div class="card-inner">
            <!-- Visual Section -->
            <div class="visual-section">
                <v-img
                    :src="product.hinhAnh || 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600'"
                    cover
                    class="product-image"
                >
                    <template v-slot:placeholder>
                        <div class="d-flex align-center justify-center fill-height bg-grey-lighten-4">
                            <v-progress-circular indeterminate size="20" width="2" color="primary" />
                        </div>
                    </template>
                </v-img>

                <div class="brand-tag">
                    {{ product.tenThuongHieu || 'AeroStride' }}
                </div>

                <div v-if="product.phanTramGiam > 0" class="discount-badge">
                    -{{ product.phanTramGiam }}%
                </div>
            </div>

            <!-- Info Section -->
            <div class="info-section pa-3">
                <h4 class="product-title mb-1" :title="product.tenSanPham">
                    {{ product.tenSanPham }}
                </h4>

                <div class="price-wrap mb-2">
                    <span class="current-price">{{ formatPrice(product.giaBan) }}</span>
                    <span v-if="originalPrice" class="old-price ml-2">
                        {{ formatPrice(originalPrice) }}
                    </span>
                </div>

                <!-- Attributes Info -->
                <div class="d-flex ga-1 mb-3 flex-wrap">
                    <span v-if="product.kichThuoc" class="attr-chip">Size {{ product.kichThuoc }}</span>
                    <span v-if="product.chatLieu" class="attr-chip">{{ product.chatLieu }}</span>
                    <span v-if="product.soLuong > 0" class="attr-chip in-stock">Còn hàng</span>
                </div>

                <!-- Action Button -->
                <v-btn
                    block
                    color="primary"
                    variant="flat"
                    rounded="lg"
                    size="small"
                    class="view-btn font-weight-bold text-none"
                    @click.stop="emit('view-detail', product.idSanPham)"
                >
                    <span class="mr-1">Xem chi tiết</span>
                    <v-icon size="14">mdi-arrow-right</v-icon>
                </v-btn>
            </div>
        </div>
    </div>
</template>

<style scoped lang="scss">
.product-showcase-card {
    background: #ffffff;
    border-radius: 16px;
    overflow: hidden;
    width: 220px;
    min-width: 220px;
    max-width: 220px;
    flex-shrink: 0;
    border: 1px solid #e2e8f0;
    cursor: pointer;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);

    &:hover {
        transform: translateY(-4px);
        box-shadow: 0 12px 24px rgba(30, 37, 124, 0.12) !important;
        border-color: #2563eb;

        .product-image {
            transform: scale(1.06);
        }

        .view-btn {
            background: #1d4ed8 !important;
        }
    }
}

.visual-section {
    position: relative;
    height: 130px;
    overflow: hidden;
    background: #f8fafc;

    .product-image {
        height: 100%;
        width: 100%;
        transition: transform 0.4s ease;
    }

    .brand-tag {
        position: absolute;
        top: 8px;
        left: 8px;
        background: rgba(15, 23, 42, 0.85);
        backdrop-filter: blur(4px);
        color: #ffffff;
        padding: 2px 8px;
        border-radius: 6px;
        font-size: 0.65rem;
        font-weight: 700;
        letter-spacing: 0.5px;
        text-transform: uppercase;
    }

    .discount-badge {
        position: absolute;
        top: 8px;
        right: 8px;
        background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
        color: #ffffff;
        padding: 2px 6px;
        border-radius: 6px;
        font-size: 0.65rem;
        font-weight: 800;
        box-shadow: 0 2px 6px rgba(239, 68, 68, 0.4);
    }
}

.product-title {
    font-size: 0.85rem;
    font-weight: 700;
    color: #0f172a;
    line-height: 1.3;
    height: 2.2rem;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.price-wrap {
    display: flex;
    align-items: baseline;
    .current-price {
        font-size: 0.95rem;
        font-weight: 800;
        color: #2563eb;
    }
    .old-price {
        font-size: 0.72rem;
        color: #94a3b8;
        text-decoration: line-through;
        font-weight: 500;
    }
}

.attr-chip {
    font-size: 0.62rem;
    background: #f1f5f9;
    color: #475569;
    padding: 1px 6px;
    border-radius: 4px;
    font-weight: 600;

    &.in-stock {
        background: #ecfdf5;
        color: #059669;
    }
}

.view-btn {
    height: 32px !important;
    font-size: 0.75rem !important;
    letter-spacing: 0.2px;
    background: linear-gradient(135deg, #1e257c 0%, #2563eb 100%) !important;
}
</style>
