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
    if (!price) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

// Giả lập một số "highlight" dựa trên dữ liệu sản phẩm để tăng sự hứng thú
const highlights = computed(() => {
    const list = [];
    if (props.product.phanTramGiam > 0) {
        list.push({ icon: 'mdi-sale', text: `Tiết kiệm ${props.product.phanTramGiam}%`, color: 'error' });
    }
    if (props.product.soLuong > 0 && props.product.soLuong < 5) {
        list.push({ icon: 'mdi-fire', text: 'Sắp hết hàng!', color: 'orange-darken-2' });
    }
    list.push({ icon: 'mdi-check-decagram', text: 'Chính hãng AeroStride', color: 'success' });
    return list;
});
</script>

<template>
    <div class="product-showcase-card shadow-lg">
        <div class="card-inner">
            <!-- Visual Section -->
            <div class="visual-section">
                <v-img :src="product.hinhAnh || '/placeholder.jpg'" cover class="product-image">
                    <template v-slot:placeholder>
                        <div class="d-flex align-center justify-center fill-height bg-grey-lighten-4">
                            <v-icon size="48" color="grey-lighten-2">mdi-image-outline</v-icon>
                        </div>
                    </template>
                </v-img>
                <div class="brand-tag">{{ product.tenThuongHieu }}</div>
                <div v-if="product.phanTramGiam > 0" class="discount-badge">-{{ product.phanTramGiam }}%</div>
            </div>

            <!-- Info Section -->
            <div class="info-section pa-4">
                <h3 class="product-title mb-2">{{ product.tenSanPham }}</h3>

                <div class="price-wrap mb-4">
                    <span class="current-price">{{ formatPrice(product.giaBan) }}</span>
                    <span v-if="product.phanTramGiam > 0" class="old-price ml-2">
                        {{ formatPrice(product.giaBan * (1 + product.phanTramGiam / 100)) }}
                    </span>
                </div>

                <!-- Interactive Highlights -->
                <div class="highlights-list mb-4">
                    <div v-for="(hl, i) in highlights" :key="i" class="hl-item d-flex align-center mb-1">
                        <v-icon :icon="hl.icon" :color="hl.color" size="14" class="mr-2"></v-icon>
                        <span class="hl-text">{{ hl.text }}</span>
                    </div>
                </div>

                <!-- Feature Badges (Demo style) -->
                <div class="d-flex gap-2 mb-4 flex-wrap">
                    <v-chip size="x-small" variant="tonal" color="black" class="font-weight-bold">Lightweight</v-chip>
                    <v-chip size="x-small" variant="tonal" color="black" class="font-weight-bold">Breathable</v-chip>
                    <v-chip size="x-small" variant="tonal" color="black" class="font-weight-bold">Pro-Grip</v-chip>
                </div>

                <!-- Action Button -->
                <v-btn block color="black" rounded="pill" class="view-btn py-6" @click="emit('view-detail', product.idSanPham)">
                    <span class="mr-2">Xem chi tiết</span>
                    <v-icon size="small">mdi-arrow-right</v-icon>
                </v-btn>
            </div>
        </div>
    </div>
</template>

<style scoped lang="scss">
.product-showcase-card {
    background: #fff;
    border-radius: 20px;
    overflow: hidden;
    width: 100%;
    max-width: 320px;
    flex-shrink: 0;
    border: 1px solid #f0f0f0;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
        transform: translateY(-8px);
        box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1) !important;
        border-color: #000;

        .product-image {
            transform: scale(1.05);
        }
    }
}

.visual-section {
    position: relative;
    height: 200px;
    overflow: hidden;
    background: #f8f9fa;

    .product-image {
        height: 100%;
        transition: transform 0.5s ease;
    }

    .brand-tag {
        position: absolute;
        top: 12px;
        left: 12px;
        background: rgba(0, 0, 0, 0.8);
        color: #fff;
        padding: 4px 10px;
        border-radius: 6px;
        font-size: 0.65rem;
        font-weight: 900;
        letter-spacing: 1px;
        text-transform: uppercase;
    }

    .discount-badge {
        position: absolute;
        top: 12px;
        right: 12px;
        background: #ff1744;
        color: #fff;
        padding: 4px 8px;
        border-radius: 6px;
        font-size: 0.75rem;
        font-weight: 900;
    }
}

.category-text {
    font-size: 0.7rem;
    font-weight: 700;
    color: #999;
    text-transform: uppercase;
    letter-spacing: 0.5px;
}

.product-title {
    font-size: 1.1rem;
    font-weight: 800;
    color: #000;
    line-height: 1.2;
    height: 2.4rem;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.price-wrap {
    .current-price {
        font-size: 1.1rem;
        font-weight: 900;
        color: #000;
    }
    .old-price {
        font-size: 0.85rem;
        color: #999;
        text-decoration: line-through;
        font-weight: 500;
    }
}

.hl-text {
    font-size: 0.75rem;
    font-weight: 600;
    color: #444;
}

.view-btn {
    font-weight: 800;
    letter-spacing: 0.5px;
    transition: all 0.3s ease;
    &:hover {
        background: #333 !important;
    }
}
</style>
