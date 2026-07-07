<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useToastStore } from '@/stores/toastStore';
import { dichVuSanPhamPublic } from '@/services/public/dichVuSanPhamPublic';
import MainHeader from '@/components/shared/MainHeader.vue';

const router = useRouter();
const toastStore = useToastStore();
const favoriteProducts = ref([]);
const loading = ref(true);

const fetchFavorites = async () => {
    loading.value = true;
    try {
        const favoriteIds = JSON.parse(localStorage.getItem('aerostride_favorites') || '[]');
        const promises = favoriteIds.map(id => dichVuSanPhamPublic.layChiTietSanPham(id).catch(() => null));
        const results = await Promise.all(promises);
        
        const validProducts = [];
        const validIds = [];
        
        results.forEach((p, index) => {
            if (p !== null) {
                validProducts.push(p);
                validIds.push(favoriteIds[index]);
            }
        });
        
        favoriteProducts.value = validProducts;
        
        if (validIds.length !== favoriteIds.length) {
            localStorage.setItem('aerostride_favorites', JSON.stringify(validIds));
            window.dispatchEvent(new Event('favorites-updated'));
        }
    } catch (error) {
        console.error('Error fetching favorites:', error);
    } finally {
        loading.value = false;
    }
};

onMounted(() => {
    fetchFavorites();
    window.addEventListener('favorites-updated', fetchFavorites);
});

onUnmounted(() => {
    window.removeEventListener('favorites-updated', fetchFavorites);
});

const formatPrice = (price) => {
    if (!price) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const goToDetail = (id) => {
    router.push(`/product/${id}`);
};

const removeFavorite = (id, event) => {
    event.stopPropagation();
    let favorites = JSON.parse(localStorage.getItem('aerostride_favorites') || '[]');
    favorites = favorites.filter(favId => favId !== id);
    localStorage.setItem('aerostride_favorites', JSON.stringify(favorites));
    window.dispatchEvent(new Event('favorites-updated'));
    toastStore.showToast('Đã xoá khỏi danh sách yêu thích', 'info');
    fetchFavorites();
};
</script>

<template>
    <div class="favorites-page bg-grey-lighten-4 min-vh-100 d-flex flex-column">
        <MainHeader />
        <div class="header-spacing" style="height: 104px"></div>

        <v-container class="py-12 flex-grow-1">
            <h1 class="text-h4 font-weight-black mb-2">Danh Sách Yêu Thích</h1>
            <p class="text-body-1 text-grey-darken-1 mb-8">Những mẫu giày bạn đã thả tim sẽ được lưu tại đây.</p>

            <v-row v-if="loading">
                <v-col v-for="i in 4" :key="i" cols="12" sm="6" md="4" lg="3">
                    <v-skeleton-loader type="image, article"></v-skeleton-loader>
                </v-col>
            </v-row>

            <v-row v-else-if="favoriteProducts.length > 0">
                <v-col v-for="p in favoriteProducts" :key="p.id" cols="12" sm="6" md="4" lg="3">
                        <div class="cursor-pointer position-relative product-card" @click="goToDetail(p.id)">
                            <!-- Nút Xoá Yêu Thích -->
                            <v-btn icon="mdi-heart" color="red" variant="flat" size="small" class="favorite-btn"
                                @click="(e) => removeFavorite(p.id, e)"></v-btn>
                                
                            <div class="rounded-xl bg-grey-lighten-4 mb-4 overflow-hidden d-flex align-center justify-center" style="aspect-ratio: 1">
                                <v-img :src="p.images?.[0]?.duongDanAnh || p.hinhAnh || ''" cover class="fill-height"></v-img>
                            </div>
                            
                            <div class="px-1 text-left">
                                <div class="text-caption font-weight-bold text-uppercase text-grey-darken-2 mb-1">{{ p.tenThuongHieu || 'NIKE' }}</div>
                                <h3 class="text-subtitle-1 font-weight-black text-black mb-1 text-truncate">{{ p.tenSanPham }}</h3>
                                <div class="text-subtitle-1 font-weight-black text-black">{{ formatPrice(p.giaBanThapNhat) }}</div>
                            </div>
                        </div>
                </v-col>
            </v-row>

            <div v-else class="text-center py-16 bg-white rounded-xl elevation-1">
                <v-icon size="80" color="grey-lighten-2" class="mb-4">mdi-heart-broken-outline</v-icon>
                <h2 class="text-h5 font-weight-bold mb-2">Chưa có sản phẩm nào</h2>
                <p class="text-body-1 text-grey-darken-1 mb-6">Bạn chưa thả tim mẫu giày nào. Hãy khám phá và thêm vào
                    danh sách yêu thích nhé!</p>
                <v-btn color="black" size="large" rounded="pill" class="px-8 font-weight-bold"
                    @click="router.push('/shoes')">
                    Khám phá ngay
                </v-btn>
            </div>
        </v-container>

        <MainFooter class="mt-auto" />
    </div>
</template>

<style scoped>
.favorite-btn {
    position: absolute;
    top: 12px;
    right: 12px;
    z-index: 2;
}

.product-card {
    cursor: pointer;
    transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.product-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1) !important;
}
</style>
