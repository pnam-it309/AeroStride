<script setup>
import { ref, onMounted } from 'vue';
import MainHeader from '@/components/shared/MainHeader.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import { useSeoMeta } from '@/composables/useSeoMeta';

const { setSeoMeta } = useSeoMeta();
const searchQuery = ref('');

const categories = [
    { icon: 'mdi-truck-delivery-outline', title: 'Vận chuyển & Giao nhận' },
    { icon: 'mdi-credit-card-outline', title: 'Thanh toán' },
    { icon: 'mdi-sync', title: 'Đổi trả & Hoàn tiền' },
    { icon: 'mdi-shield-check-outline', title: 'Bảo hành' },
];

const faqs = [
    {
        title: 'Vận chuyển & Giao nhận',
        items: [
            { q: 'Thời gian giao hàng là bao lâu?', a: 'Đối với khu vực nội thành Hà Nội và TP.HCM, thời gian giao hàng thường từ 1-2 ngày làm việc. Với các tỉnh thành khác, thời gian từ 3-5 ngày làm việc tuỳ khu vực.' },
            { q: 'Tôi có thể kiểm tra hàng trước khi thanh toán không?', a: 'Có. AeroStride luôn khuyến khích khách hàng đồng kiểm cùng bưu tá trước khi thanh toán để đảm bảo sản phẩm đúng màu, đúng size và không bị lỗi.' },
            { q: 'Phí vận chuyển được tính như thế nào?', a: 'AeroStride miễn phí vận chuyển toàn quốc cho mọi đơn hàng có giá trị từ 1.000.000 VNĐ. Với đơn hàng dưới mức này, phí ship đồng giá là 30.000 VNĐ.' },
        ]
    },
    {
        title: 'Đổi trả & Hoàn tiền',
        items: [
            { q: 'Chính sách đổi trả của AeroStride như thế nào?', a: 'Bạn có thể đổi trả sản phẩm trong vòng 30 ngày kể từ ngày nhận hàng với điều kiện sản phẩm còn nguyên tem mác, chưa qua sử dụng và có đầy đủ hộp, phụ kiện đi kèm.' },
            { q: 'Làm thế nào để yêu cầu đổi trả?', a: 'Bạn vui lòng đăng nhập vào tài khoản, vào mục "Đơn hàng của tôi", chọn đơn hàng cần đổi trả và bấm nút "Yêu cầu đổi/trả". Hệ thống sẽ hướng dẫn chi tiết.' },
        ]
    }
];

onMounted(() => {
    window.scrollTo(0, 0);
    setSeoMeta({
        title: 'Trợ Giúp & FAQ | AeroStride',
        description: 'Trung tâm trợ giúp AeroStride. Tìm câu trả lời cho mọi thắc mắc về vận chuyển, thanh toán, đổi trả và bảo hành.'
    });
});
</script>

<template>
    <div class="app-container bg-grey-lighten-4">
        <MainHeader />
        
        <main class="main-content">
            <!-- Search Hero Section -->
            <div class="help-hero py-16 bg-primary text-white text-center">
                <v-container maxWidth="800">
                    <h1 class="text-h3 font-weight-black mb-6">XIN CHÀO, CHÚNG TÔI CÓ THỂ GIÚP GÌ CHO BẠN?</h1>
                    <v-text-field
                        v-model="searchQuery"
                        placeholder="Nhập câu hỏi của bạn (VD: đổi trả, vận chuyển...)"
                        variant="solo"
                        rounded="xl"
                        prepend-inner-icon="mdi-magnify"
                        hide-details
                        class="search-input mx-auto"
                        bg-color="white"
                        color="primary"
                    ></v-text-field>
                </v-container>
            </div>

            <v-container class="py-12">
                <!-- Categories Grid -->
                <v-row class="mb-12" justify="center">
                    <v-col v-for="(cat, i) in categories" :key="i" cols="6" sm="3">
                        <v-card class="text-center pa-6 rounded-xl hover-card cursor-pointer h-100 d-flex flex-column align-center justify-center elevation-2">
                            <v-icon size="48" color="primary" class="mb-4">{{ cat.icon }}</v-icon>
                            <h3 class="text-subtitle-1 font-weight-bold text-grey-darken-3">{{ cat.title }}</h3>
                        </v-card>
                    </v-col>
                </v-row>

                <!-- FAQ Section -->
                <div class="max-w-800 mx-auto">
                    <h2 class="text-h4 font-weight-black text-center mb-10 text-grey-darken-4">CÂU HỎI THƯỜNG GẶP</h2>
                    
                    <div v-for="(section, idx) in faqs" :key="idx" class="mb-10">
                        <h3 class="text-h5 font-weight-bold mb-6 text-primary">{{ section.title }}</h3>
                        <v-expansion-panels variant="accordion" class="rounded-xl overflow-hidden elevation-2">
                            <v-expansion-panel v-for="(item, itemIdx) in section.items" :key="itemIdx" class="mb-1 border-b">
                                <v-expansion-panel-title class="font-weight-bold text-body-1 py-4">
                                    {{ item.q }}
                                </v-expansion-panel-title>
                                <v-expansion-panel-text class="text-body-1 text-grey-darken-2 bg-grey-lighten-5 pt-4 pb-4 leading-relaxed">
                                    {{ item.a }}
                                </v-expansion-panel-text>
                            </v-expansion-panel>
                        </v-expansion-panels>
                    </div>
                    
                    <!-- Contact Banner -->
                    <v-card class="bg-blue-grey-darken-4 text-white rounded-xl pa-8 text-center mt-12">
                        <v-icon size="48" class="mb-4">mdi-headset</v-icon>
                        <h3 class="text-h5 font-weight-bold mb-2">Vẫn cần hỗ trợ?</h3>
                        <p class="text-body-1 mb-6 opacity-80">Đội ngũ chăm sóc khách hàng của chúng tôi luôn sẵn sàng 24/7</p>
                        <v-btn color="white" variant="flat" size="large" rounded="xl" class="text-primary font-weight-bold" to="/lien-he">
                            Liên hệ ngay
                        </v-btn>
                    </v-card>
                </div>
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

.search-input {
    max-width: 600px;
}
.search-input :deep(.v-field__input) {
    padding-top: 16px;
    padding-bottom: 16px;
    font-size: 1.1rem;
}

.hover-card {
    transition: transform 0.3s ease, box-shadow 0.3s ease;
}
.hover-card:hover {
    transform: translateY(-8px);
    box-shadow: 0 12px 24px rgba(0,0,0,0.1) !important;
}

.max-w-800 {
    max-width: 800px;
}

.leading-relaxed {
    line-height: 1.7;
}
</style>
