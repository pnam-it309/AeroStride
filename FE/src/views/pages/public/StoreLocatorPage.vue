<script setup>
import { ref, onMounted, computed } from 'vue';
import MainHeader from '@/components/shared/MainHeader.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import { useSeoMeta } from '@/composables/useSeoMeta';

const { setSeoMeta } = useSeoMeta();

const selectedCity = ref('Tất cả');
const cities = ['Tất cả', 'Hà Nội', 'TP. Hồ Chí Minh', 'Đà Nẵng'];

const stores = [
    { id: 1, name: 'AeroStride Cầu Giấy', city: 'Hà Nội', address: '123 Đường Cầu Giấy, Phường Quan Hoa, Cầu Giấy, Hà Nội', phone: '024 1234 5678', time: '08:00 - 22:00' },
    { id: 2, name: 'AeroStride Hai Bà Trưng', city: 'Hà Nội', address: '456 Phố Huế, Phường Phạm Đình Hổ, Hai Bà Trưng, Hà Nội', phone: '024 8765 4321', time: '08:00 - 22:00' },
    { id: 3, name: 'AeroStride Quận 1', city: 'TP. Hồ Chí Minh', address: '789 Nguyễn Trãi, Phường Bến Thành, Quận 1, TP. HCM', phone: '028 9999 8888', time: '08:30 - 22:30' },
    { id: 4, name: 'AeroStride Gò Vấp', city: 'TP. Hồ Chí Minh', address: '101 Phan Văn Trị, Phường 10, Gò Vấp, TP. HCM', phone: '028 7777 6666', time: '08:30 - 22:30' },
    { id: 5, name: 'AeroStride Hải Châu', city: 'Đà Nẵng', address: '202 Lê Duẩn, Phường Hải Châu 1, Hải Châu, Đà Nẵng', phone: '023 5555 4444', time: '08:00 - 21:30' },
];

const filteredStores = computed(() => {
    if (selectedCity.value === 'Tất cả') return stores;
    return stores.filter(s => s.city === selectedCity.value);
});

onMounted(() => {
    window.scrollTo(0, 0);
    setSeoMeta({
        title: 'Hệ Thống Cửa Hàng | AeroStride',
        description: 'Tìm kiếm cửa hàng giày thể thao AeroStride gần bạn nhất. Hệ thống 50+ cửa hàng trên toàn quốc.'
    });
});
</script>

<template>
    <div class="app-container bg-grey-lighten-4">
        <MainHeader />
        
        <main class="main-content">
            <!-- Header Section -->
            <div class="page-header py-12 bg-white border-b">
                <v-container>
                    <h1 class="text-h3 font-weight-black text-center text-grey-darken-4 mb-4">HỆ THỐNG CỬA HÀNG</h1>
                    <p class="text-center text-grey-darken-1 text-subtitle-1">Trải nghiệm trực tiếp các mẫu giày mới nhất tại 50+ chi nhánh trên toàn quốc.</p>
                </v-container>
            </div>

            <v-container class="py-12">
                <v-row>
                    <!-- Sidebar List -->
                    <v-col cols="12" md="4" class="d-flex flex-column h-md-100">
                        <v-card class="rounded-xl elevation-2 flex-grow-1 overflow-hidden d-flex flex-column">
                            <div class="pa-4 bg-primary text-white">
                                <h3 class="text-h6 font-weight-bold mb-4">Tìm cửa hàng</h3>
                                <v-select
                                    v-model="selectedCity"
                                    :items="cities"
                                    variant="solo"
                                    density="comfortable"
                                    hide-details
                                    prepend-inner-icon="mdi-map-marker"
                                    class="bg-white rounded-lg"
                                ></v-select>
                            </div>
                            
                            <v-list class="overflow-y-auto flex-grow-1 store-list" max-height="600">
                                <template v-for="(store, index) in filteredStores" :key="store.id">
                                    <v-list-item class="pa-4 store-item border-b">
                                        <h4 class="text-subtitle-1 font-weight-bold text-primary mb-2">{{ store.name }}</h4>
                                        <div class="d-flex align-start mb-1 text-caption text-grey-darken-2">
                                            <v-icon size="14" class="mr-2 mt-1">mdi-map-marker-outline</v-icon>
                                            {{ store.address }}
                                        </div>
                                        <div class="d-flex align-center mb-1 text-caption text-grey-darken-2">
                                            <v-icon size="14" class="mr-2">mdi-phone-outline</v-icon>
                                            {{ store.phone }}
                                        </div>
                                        <div class="d-flex align-center mb-2 text-caption text-grey-darken-2">
                                            <v-icon size="14" class="mr-2">mdi-clock-outline</v-icon>
                                            {{ store.time }}
                                        </div>
                                        <v-btn variant="outlined" color="primary" size="small" class="mt-2 text-none" rounded="lg">
                                            Chỉ đường
                                        </v-btn>
                                    </v-list-item>
                                </template>
                                <div v-if="filteredStores.length === 0" class="pa-6 text-center text-grey">
                                    Không tìm thấy cửa hàng nào tại khu vực này.
                                </div>
                            </v-list>
                        </v-card>
                    </v-col>

                    <!-- Map / Image Banner -->
                    <v-col cols="12" md="8">
                        <v-card class="rounded-xl elevation-2 h-100 overflow-hidden d-flex flex-column align-center justify-center bg-white min-h-400">
                            <!-- In a real app, this would be a Google Map component -->
                            <v-img src="https://images.unsplash.com/photo-1555529771-835f59fc5efe?auto=format&fit=crop&q=80&w=1200" cover class="w-100 h-100">
                                <div class="fill-height d-flex flex-column align-center justify-center" style="background: rgba(0,0,0,0.5)">
                                    <v-icon size="64" color="white" class="mb-4">mdi-store-search-outline</v-icon>
                                    <h2 class="text-h4 font-weight-black text-white text-center px-4">CHỌN CỬA HÀNG ĐỂ XEM TRÊN BẢN ĐỒ</h2>
                                </div>
                            </v-img>
                        </v-card>
                    </v-col>
                </v-row>
            </v-container>
        </main>
        
        <footer class="footer-landing py-10 text-center text-grey-darken-1 bg-white border-t">
            <LogoClient class="mb-4 d-inline-block" style="max-width: 150px" />
            <p>&copy; 2026 AeroStride All rights reserved.</p>
        </footer>
    </div>
</template>

<style scoped>
.main-content { padding-top: 60px; }

.store-list::-webkit-scrollbar {
    width: 6px;
}
.store-list::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 4px;
}

.store-item {
    transition: background 0.2s ease;
    cursor: pointer;
}
.store-item:hover {
    background: #f1f5f9;
}

.min-h-400 {
    min-height: 400px;
}
</style>
