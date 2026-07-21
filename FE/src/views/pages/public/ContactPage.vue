<script setup>
import { ref, onMounted } from 'vue';
import MainHeader from '@/components/shared/MainHeader.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import { useSeoMeta } from '@/composables/useSeoMeta';

const { setSeoMeta } = useSeoMeta();

const form = ref({
    name: '',
    email: '',
    phone: '',
    message: ''
});

const isSubmitting = ref(false);
const showSuccess = ref(false);

const submitForm = () => {
    isSubmitting.value = true;
    // Mock API call
    setTimeout(() => {
        isSubmitting.value = false;
        showSuccess.value = true;
        form.value = { name: '', email: '', phone: '', message: '' };
        setTimeout(() => showSuccess.value = false, 5000);
    }, 1500);
};

onMounted(() => {
    window.scrollTo(0, 0);
    setSeoMeta({
        title: 'Liên Hệ | AeroStride',
        description: 'Đội ngũ chăm sóc khách hàng của AeroStride luôn sẵn sàng hỗ trợ bạn 24/7. Liên hệ với chúng tôi ngay.'
    });
});
</script>

<template>
    <div class="app-container bg-white">
        <MainHeader />
        
        <main class="main-content">
            <!-- Header Section -->
            <div class="page-header py-12 bg-grey-lighten-4 border-b">
                <v-container>
                    <h1 class="text-h3 font-weight-black text-center text-grey-darken-4 mb-4">LIÊN HỆ VỚI CHÚNG TÔI</h1>
                    <p class="text-center text-grey-darken-1 text-subtitle-1">Chúng tôi luôn sẵn sàng lắng nghe và hỗ trợ bạn bất cứ lúc nào.</p>
                </v-container>
            </div>

            <v-container class="py-16">
                <v-row>
                    <!-- Contact Info -->
                    <v-col cols="12" md="4" class="pr-md-8 mb-8 mb-md-0">
                        <div class="d-flex align-start mb-8">
                            <v-icon size="36" color="primary" class="mr-4 mt-1">mdi-map-marker-outline</v-icon>
                            <div>
                                <h3 class="text-h6 font-weight-bold mb-1">Trụ sở chính</h3>
                                <p class="text-body-2 text-grey-darken-1">123 Đường Cầu Giấy, Quận Cầu Giấy<br>Thủ đô Hà Nội, Việt Nam</p>
                            </div>
                        </div>

                        <div class="d-flex align-start mb-8">
                            <v-icon size="36" color="primary" class="mr-4 mt-1">mdi-phone-outline</v-icon>
                            <div>
                                <h3 class="text-h6 font-weight-bold mb-1">Hotline CSKH</h3>
                                <p class="text-body-2 text-grey-darken-1">1900 6789<br>(Từ 8:00 - 22:00 tất cả các ngày)</p>
                            </div>
                        </div>

                        <div class="d-flex align-start">
                            <v-icon size="36" color="primary" class="mr-4 mt-1">mdi-email-outline</v-icon>
                            <div>
                                <h3 class="text-h6 font-weight-bold mb-1">Email hỗ trợ</h3>
                                <p class="text-body-2 text-grey-darken-1">support@aerostride.vn<br>cskh@aerostride.vn</p>
                            </div>
                        </div>
                    </v-col>

                    <!-- Contact Form -->
                    <v-col cols="12" md="8">
                        <v-card class="rounded-xl pa-8 elevation-4 border">
                            <h3 class="text-h5 font-weight-bold mb-6">Gửi tin nhắn cho chúng tôi</h3>
                            
                            <v-alert v-if="showSuccess" type="success" variant="tonal" class="mb-6 rounded-lg" closable>
                                Cảm ơn bạn! Tin nhắn đã được gửi thành công. Chúng tôi sẽ phản hồi sớm nhất có thể.
                            </v-alert>

                            <v-form @submit.prevent="submitForm">
                                <v-row>
                                    <v-col cols="12" sm="6">
                                        <v-text-field
                                            v-model="form.name"
                                            label="Họ và tên"
                                            variant="outlined"
                                            required
                                            hide-details="auto"
                                            class="modern-input"
                                        ></v-text-field>
                                    </v-col>
                                    <v-col cols="12" sm="6">
                                        <v-text-field
                                            v-model="form.phone"
                                            label="Số điện thoại"
                                            variant="outlined"
                                            required
                                            hide-details="auto"
                                            class="modern-input"
                                        ></v-text-field>
                                    </v-col>
                                    <v-col cols="12">
                                        <v-text-field
                                            v-model="form.email"
                                            label="Email (Không bắt buộc)"
                                            variant="outlined"
                                            type="email"
                                            hide-details="auto"
                                            class="modern-input"
                                        ></v-text-field>
                                    </v-col>
                                    <v-col cols="12">
                                        <v-textarea
                                            v-model="form.message"
                                            label="Nội dung cần hỗ trợ..."
                                            variant="outlined"
                                            required
                                            rows="4"
                                            hide-details="auto"
                                            class="modern-input"
                                        ></v-textarea>
                                    </v-col>
                                    <v-col cols="12">
                                        <v-btn
                                            type="submit"
                                            color="primary"
                                            size="x-large"
                                            rounded="lg"
                                            :loading="isSubmitting"
                                            class="font-weight-bold text-none mt-2"
                                            block
                                        >
                                            GỬI TIN NHẮN
                                        </v-btn>
                                    </v-col>
                                </v-row>
                            </v-form>
                        </v-card>
                    </v-col>
                </v-row>

                <!-- Map -->
                <v-row class="mt-12">
                    <v-col cols="12">
                        <div class="map-container rounded-xl overflow-hidden elevation-2">
                            <!-- Dummy Map Image to simulate Google Maps -->
                            <v-img src="https://images.unsplash.com/photo-1524661135-423995f22d0b?auto=format&fit=crop&q=80&w=1200" height="400" cover>
                                <div class="fill-height d-flex align-center justify-center" style="background: rgba(0,0,0,0.3)">
                                    <v-btn color="white" variant="elevated" rounded="xl" prepend-icon="mdi-map-marker">
                                        Mở trên Google Maps
                                    </v-btn>
                                </div>
                            </v-img>
                        </div>
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

.modern-input :deep(.v-field) {
    border-radius: 10px;
}
.modern-input :deep(.v-field--focused) {
    background: #f8fafc;
}

.map-container {
    position: relative;
}
</style>
