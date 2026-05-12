<script setup>
import { ref } from 'vue';
import MainHeader from '@/components/shared/MainHeader.vue';
import PromotionBar from '@/components/shared/PromotionBar.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';

const selectedSize = ref(null);
const sizes = ['EU 38.5', 'EU 39', 'EU 40', 'EU 40.5', 'EU 41', 'EU 42', 'EU 42.5', 'EU 43', 'EU 44', 'EU 44.5', 'EU 45', 'EU 46'];

const product = {
    name: 'AeroStride X1 "Infinite"',
    category: 'Men\'s Road Running Shoes',
    price: '4,250,000₫',
    description: 'Trải nghiệm đỉnh cao công nghệ với AeroStride X1. Được thiết kế để tối ưu hóa từng bước chạy, X1 mang lại sự êm ái tuyệt đối và khả năng phản hồi lực vượt trội nhờ đế giữa Carbon-Nano thế hệ mới.'
};

const mainImageIndex = ref(0);
const thumbnails = Array(6).fill(null);
</script>

<template>
    <div class="product-detail-page bg-white min-vh-100">
        <MainHeader />
        
        <div class="header-spacing" style="height: 104px;"></div>
        <PromotionBar />

        <v-container class="mt-12">
            <v-row>
                <!-- Left: Image Gallery (Placeholders) -->
                <v-col cols="12" md="7" class="image-gallery">
                    <v-row>
                        <v-col v-for="(thumb, i) in thumbnails" :key="i" cols="6" class="mb-4">
                            <div class="image-placeholder-large">
                                <v-icon size="64" color="grey-lighten-2">mdi-image-outline</v-icon>
                                <div class="index-label">VIEW {{ i + 1 }}</div>
                            </div>
                        </v-col>
                    </v-row>
                </v-col>

                <!-- Right: Product Info (Sticky) -->
                <v-col cols="12" md="5">
                    <div class="sticky-info-panel px-md-8">
                        <div class="header-info mb-8">
                            <h1 class="product-title text-h4 font-weight-black mb-1">{{ product.name }}</h1>
                            <p class="product-cat text-subtitle-1 font-weight-bold grey--text">{{ product.category }}</p>
                            <div class="product-price mt-4 text-h6 font-weight-black">{{ product.price }}</div>
                        </div>

                        <!-- Size Selection -->
                        <div class="size-selection-section mb-10">
                            <div class="d-flex justify-space-between align-center mb-4">
                                <span class="font-weight-black">Select Size</span>
                                <a href="#" class="size-guide">Size Guide</a>
                            </div>
                            <v-row g-2>
                                <v-col v-for="size in sizes" :key="size" cols="4">
                                    <div class="size-box" 
                                         :class="{ 'active': selectedSize === size }"
                                         @click="selectedSize = size">
                                        {{ size }}
                                    </div>
                                </v-col>
                            </v-row>
                        </div>

                        <!-- Action Buttons -->
                        <div class="action-buttons d-flex flex-column gap-4">
                            <v-btn block size="x-large" color="black" rounded="pill" class="font-weight-black py-6">
                                Add to Bag
                            </v-btn>
                            <v-btn block size="x-large" variant="outlined" rounded="pill" class="font-weight-black py-6">
                                Favourite <v-icon class="ml-2">mdi-heart-outline</v-icon>
                            </v-btn>
                        </div>

                        <!-- Description -->
                        <div class="product-description mt-12 pt-12 border-t">
                            <p class="desc-text">{{ product.description }}</p>
                            <ul class="mt-6 pl-4 spec-list">
                                <li>Colour Shown: White/Platinum/Black</li>
                                <li>Style: AS-9920-101</li>
                            </ul>
                            
                            <v-btn variant="text" class="px-0 mt-6 font-weight-black text-decoration-underline">
                                View Product Details
                            </v-btn>
                        </div>

                        <!-- Expansion Panels for Shipping/Reviews -->
                        <div class="mt-8">
                            <v-expansion-panels flat variant="accordion">
                                <v-expansion-panel title="Delivery & Returns" class="border-t">
                                    <v-expansion-panel-text class="text-caption">
                                        Free standard delivery on orders over 5,000,000₫. Returns are free within 30 days.
                                    </v-expansion-panel-text>
                                </v-expansion-panel>
                                <v-expansion-panel title="Reviews (128)" class="border-t">
                                    <v-expansion-panel-text>
                                        <div class="d-flex align-center mb-2">
                                            <v-rating model-value="4.8" density="compact" color="black" half-increments readonly></v-rating>
                                            <span class="ml-2 font-weight-bold">4.8 Stars</span>
                                        </div>
                                    </v-expansion-panel-text>
                                </v-expansion-panel>
                            </v-expansion-panels>
                        </div>
                    </div>
                </v-col>
            </v-row>
        </v-container>

        <!-- Global Chat System -->
        <CustomerChat />
    </div>
</template>

<style scoped lang="scss">
.product-detail-page {
    padding-top: 64px;
}

.image-placeholder-large {
    width: 100%; aspect-ratio: 1; background: #f6f6f6;
    display: flex; align-items: center; justify-content: center;
    position: relative; border-radius: 4px;
    .index-label { position: absolute; bottom: 20px; left: 20px; font-size: 0.65rem; font-weight: 900; color: #ccc; letter-spacing: 2px; }
}

.sticky-info-panel {
    position: sticky; top: 140px;
}

.product-cat { color: #111; }
.product-price { color: #111; }

.size-guide { font-size: 0.9rem; color: #757575; text-decoration: none; font-weight: 600; border-bottom: 1px solid transparent; &:hover { border-bottom-color: #757575; } }

.size-box {
    border: 1px solid #e5e5e5; border-radius: 4px; padding: 12px; text-align: center;
    font-size: 0.95rem; font-weight: 700; cursor: pointer; transition: all 0.2s ease;
    &:hover { border-color: #000; }
    &.active { border-color: #000; background: #000; color: #fff; }
}

.desc-text { font-size: 1.05rem; line-height: 1.8; color: #111; }
.spec-list {
    list-style: disc;
    li { font-size: 0.9rem; margin-bottom: 8px; color: #111; font-weight: 500; }
}

:deep(.v-expansion-panel-title) { font-weight: 800; font-size: 1.1rem; padding: 24px 0; }
:deep(.v-expansion-panel-text__wrapper) { padding: 0 0 24px 0; }

.gap-4 { gap: 16px; }

@media (max-width: 960px) {
    .sticky-info-panel { position: relative; top: 0; padding: 0; margin-top: 40px; }
}
</style>
