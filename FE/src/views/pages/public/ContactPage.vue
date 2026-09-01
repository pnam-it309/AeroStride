<script setup>
import { ref, onMounted } from 'vue';
import MainHeader from '@/components/shared/MainHeader.vue';
import MainFooter from '@/components/shared/MainFooter.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import { useSeoMeta } from '@/composables/useSeoMeta';

import { dichVuLienHe } from '@/services/public/dichVuLienHe';

const { setSeoMeta } = useSeoMeta();

const form = ref({
    name: '',
    email: '',
    phone: '',
    topic: 'Tư vấn sản phẩm',
    message: ''
});

const topicOptions = [
    'Tư vấn sản phẩm & chọn size',
    'Kiểm tra đơn hàng',
    'Hỗ trợ đổi trả & bảo hành',
    'Hợp tác kinh doanh',
    'Ý kiến đóng góp khác'
];

const isSubmitting = ref(false);
const showSuccess = ref(false);
const successMessage = ref('Cảm ơn bạn! Yêu cầu hỗ trợ đã được gửi thành công. AeroStride sẽ sớm phản hồi.');
const contactFormRef = ref(null);

const faqs = ref([
    {
        q: 'AeroStride có cam kết sản phẩm 100% chính hãng không?',
        a: 'Tất cả sản phẩm tại AeroStride được nhập khẩu trực tiếp từ các thương hiệu Nike, Adidas, Puma, Mizuno... Cam kết đền bù 200% nếu phát hiện sản phẩm không chính hãng.'
    },
    {
        q: 'Thời gian giao hàng toàn quốc mất bao lâu?',
        a: 'Đơn hàng tại Hà Nội & TP.HCM nhận hàng trong 1-2 ngày. Các tỉnh thành khác thời gian giao hàng từ 2-4 ngày làm việc.'
    },
    {
        q: 'Chính sách đổi trả hàng áp dụng như thế nào?',
        a: 'AeroStride hỗ trợ đổi size hoặc đổi mẫu hoàn toàn miễn phí trong 30 ngày kể từ ngày nhận hàng (sản phẩm còn nguyên tem mác, nguyên hộp và chưa qua sử dụng).'
    },
    {
        q: 'Tôi có thể đến xem và thử trực tiếp tại showroom không?',
        a: 'Bạn hoàn toàn có thể ghé bất kỳ showroom nào trong hệ thống 50+ chi nhánh AeroStride trên toàn quốc để xem và mang thử sản phẩm.'
    }
]);

const submitForm = async () => {
    if (contactFormRef.value) {
        const { valid } = await contactFormRef.value.validate();
        if (!valid) return;
    }
    isSubmitting.value = true;
    try {
        const res = await dichVuLienHe.guiLienHe({
            ten: form.value.name,
            sdt: form.value.phone,
            email: form.value.email,
            chuDe: form.value.topic,
            noiDung: form.value.message
        });
        if (res?.message) {
            successMessage.value = res.message;
        }
        showSuccess.value = true;
        form.value = { name: '', email: '', phone: '', topic: 'Tư vấn sản phẩm', message: '' };
        setTimeout(() => (showSuccess.value = false), 6000);
    } catch (error) {
        console.error('Lỗi gửi liên hệ:', error);
    } finally {
        isSubmitting.value = false;
    }
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
    <div class="app-container bg-white font-body">
        <MainHeader />

        <main class="main-content">
            <!-- Header Section: Logo Blue Gradient + Crisp White Text -->
            <div class="page-header py-14 mb-10 text-white">
                <v-container>
                    <div class="d-flex justify-center mb-3">
                        <v-chip
                            variant="flat"
                            size="small"
                            class="font-weight-black px-4 shadow-sm"
                            style="background-color: rgba(255, 255, 255, 0.15) !important; color: #ffffff !important; border: 1px solid rgba(255, 255, 255, 0.25) !important;"
                        >
                            <v-icon start size="16" color="white">mdi-phone-in-talk-outline</v-icon>
                            AEROSTRIDE SUPPORT
                        </v-chip>
                    </div>
                    <h1 class="page-header-title font-weight-black text-center mb-3">LIÊN HỆ VỚI AEROSTRIDE</h1>
                    <p class="page-header-subtitle text-center text-subtitle-1 max-w-600 mx-auto">
                        Chúng tôi luôn sẵn sàng lắng nghe, giải đáp thắc mắc và hỗ trợ bạn 24/7.
                    </p>
                </v-container>
            </div>

            <v-container class="py-16">
                <!-- Direct Info Cards -->
                <v-row class="mb-12">
                    <v-col cols="12" sm="6" md="3">
                        <v-card class="h-100 pa-6 rounded-xl border elevation-2 hover-lift text-center">
                            <div class="icon-wrap mx-auto mb-4 bg-blue-lighten">
                                <v-icon size="32" color="primary">mdi-map-marker</v-icon>
                            </div>
                            <h3 class="text-h6 font-weight-bold text-slate-900 mb-2">Trụ Sở Chính</h3>
                            <p class="text-body-2 text-slate-600 mb-0">21A Lê Đức Thọ, Mỹ Đình 2<br />Nam Từ Liêm, Hà Nội</p>
                        </v-card>
                    </v-col>
                    <v-col cols="12" sm="6" md="3">
                        <v-card class="h-100 pa-6 rounded-xl border elevation-2 hover-lift text-center">
                            <div class="icon-wrap mx-auto mb-4 bg-green-lighten">
                                <v-icon size="32" color="success">mdi-phone</v-icon>
                            </div>
                            <h3 class="text-h6 font-weight-bold text-slate-900 mb-2">Hotline CSKH</h3>
                            <p class="text-body-2 text-slate-600 mb-2"><strong>1900 6868</strong> (08:00 - 22:00 Hàng ngày)</p>
                            <v-btn color="success" variant="text" size="small" href="tel:19006868" class="font-weight-bold">
                                Gọi Ngay
                            </v-btn>
                        </v-card>
                    </v-col>
                    <v-col cols="12" sm="6" md="3">
                        <v-card class="h-100 pa-6 rounded-xl border elevation-2 hover-lift text-center style-zalo-card">
                            <div class="icon-wrap mx-auto mb-4 bg-zalo-lighten">
                                <v-icon size="32" color="#0068ff">mdi-message-processing</v-icon>
                            </div>
                            <h3 class="text-h6 font-weight-bold text-slate-900 mb-2">Zalo CSKH 24/7</h3>
                            <p class="text-body-2 text-slate-600 mb-2"><strong>0987.654.321</strong> (Phản hồi tức thì)</p>
                            <v-btn
                                color="#0068ff"
                                variant="flat"
                                size="small"
                                href="https://zalo.me/0987654321"
                                target="_blank"
                                class="font-weight-bold text-white rounded-pill px-4"
                            >
                                <v-icon size="16" class="mr-1">mdi-open-in-new</v-icon> Chat Qua Zalo
                            </v-btn>
                        </v-card>
                    </v-col>
                    <v-col cols="12" sm="6" md="3">
                        <v-card class="h-100 pa-6 rounded-xl border elevation-2 hover-lift text-center">
                            <div class="icon-wrap mx-auto mb-4 bg-purple-lighten">
                                <v-icon size="32" color="purple">mdi-email</v-icon>
                            </div>
                            <h3 class="text-h6 font-weight-bold text-slate-900 mb-2">Email Hỗ Trợ</h3>
                            <p class="text-body-2 text-slate-600 mb-2">support@aerostride.vn<br />cskh@aerostride.vn</p>
                            <v-btn color="purple" variant="text" size="small" href="mailto:support@aerostride.vn" class="font-weight-bold">
                                Gửi Email
                            </v-btn>
                        </v-card>
                    </v-col>
                </v-row>

                <v-row>
                    <!-- Form Send Request -->
                    <v-col cols="12" md="7" class="pr-md-6 mb-8 mb-md-0">
                        <v-card class="rounded-xl pa-8 elevation-3 border">
                            <h3 class="text-h5 font-weight-black text-slate-900 mb-2">Gửi Yêu Cầu Hỗ Trợ</h3>
                            <p class="text-slate-600 text-body-2 mb-6">Hãy điền thông tin bên dưới, chuyên viên CSKH sẽ phản hồi trong vòng 30 phút.</p>

                            <v-alert v-if="showSuccess" type="success" variant="tonal" class="mb-6 rounded-lg" closable>
                                {{ successMessage }}
                            </v-alert>

                            <v-form ref="contactFormRef" @submit.prevent="submitForm">
                                <v-row dense>
                                    <v-col cols="12" sm="6" class="mb-3">
                                        <div class="field-label text-caption font-weight-bold text-slate-700 mb-1">Họ và tên *</div>
                                        <v-text-field
                                            v-model="form.name"
                                            placeholder="Nguyễn Văn A"
                                            variant="outlined"
                                            density="comfortable"
                                            hide-details="auto"
                                            class="modern-input"
                                            :rules="[(v) => !!v?.trim() || 'Vui lòng nhập họ và tên']"
                                        ></v-text-field>
                                    </v-col>
                                    <v-col cols="12" sm="6" class="mb-3">
                                        <div class="field-label text-caption font-weight-bold text-slate-700 mb-1">Số điện thoại *</div>
                                        <v-text-field
                                            v-model="form.phone"
                                            placeholder="0987654321"
                                            variant="outlined"
                                            density="comfortable"
                                            hide-details="auto"
                                            class="modern-input"
                                            :rules="[(v) => !!v?.trim() || 'Vui lòng nhập số điện thoại', (v) => /^0[3|5|7|8|9][0-9]{8}$/.test(v?.trim() || '') || 'Số điện thoại không hợp lệ']"
                                        ></v-text-field>
                                    </v-col>
                                    <v-col cols="12" class="mb-3">
                                        <div class="field-label text-caption font-weight-bold text-slate-700 mb-1">Email</div>
                                        <v-text-field
                                            v-model="form.email"
                                            placeholder="email@example.com"
                                            variant="outlined"
                                            type="email"
                                            density="comfortable"
                                            hide-details="auto"
                                            class="modern-input"
                                            :rules="[(v) => !v || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v?.trim() || '') || 'Email không hợp lệ']"
                                        ></v-text-field>
                                    </v-col>
                                    <v-col cols="12" class="mb-3">
                                        <div class="field-label text-caption font-weight-bold text-slate-700 mb-1">Chủ đề cần hỗ trợ</div>
                                        <v-select
                                            v-model="form.topic"
                                            :items="topicOptions"
                                            variant="outlined"
                                            density="comfortable"
                                            hide-details
                                            class="modern-input"
                                        ></v-select>
                                    </v-col>
                                    <v-col cols="12" class="mb-4">
                                        <div class="field-label text-caption font-weight-bold text-slate-700 mb-1">Nội dung chi tiết *</div>
                                        <v-textarea
                                            v-model="form.message"
                                            placeholder="Vui lòng mô tả chi tiết thắc mắc hoặc yêu cầu của bạn..."
                                            variant="outlined"
                                            rows="4"
                                            density="comfortable"
                                            hide-details="auto"
                                            class="modern-input"
                                            :rules="[(v) => !!v?.trim() || 'Vui lòng nhập nội dung cần hỗ trợ']"
                                        ></v-textarea>
                                    </v-col>
                                    <v-col cols="12">
                                        <v-btn
                                            type="submit"
                                            color="primary"
                                            size="x-large"
                                            rounded="pill"
                                            :loading="isSubmitting"
                                            class="font-weight-bold text-none shadow-blue"
                                            block
                                        >
                                            GỬI YÊU CẦU HỖ TRỢ
                                        </v-btn>
                                    </v-col>
                                </v-row>
                            </v-form>
                        </v-card>
                    </v-col>

                    <!-- FAQ Accordion -->
                    <v-col cols="12" md="5">
                        <div class="pa-2">
                            <h3 class="text-h5 font-weight-black text-slate-900 mb-2">Câu Hỏi Thường Gặp</h3>
                            <p class="text-slate-600 text-body-2 mb-6">Giải đáp nhanh các câu hỏi phổ biến từ khách hàng.</p>

                            <v-expansion-panels variant="accordion" class="rounded-xl overflow-hidden elevation-1">
                                <v-expansion-panel
                                    v-for="(item, i) in faqs"
                                    :key="i"
                                    class="border-b"
                                >
                                    <v-expansion-panel-title class="font-weight-bold text-slate-800 text-body-2 pa-4">
                                        {{ item.q }}
                                    </v-expansion-panel-title>
                                    <v-expansion-panel-text class="text-slate-600 text-body-2 leading-relaxed pa-4">
                                        {{ item.a }}
                                    </v-expansion-panel-text>
                                </v-expansion-panel>
                            </v-expansion-panels>
                        </div>
                    </v-col>
                </v-row>

                <!-- Embedded Interactive Google Map -->
                <v-row class="mt-12">
                    <v-col cols="12">
                        <h3 class="text-h5 font-weight-black text-slate-900 mb-4 text-center">Bản Đồ Cửa Hàng Flagship AeroStride</h3>
                        <div class="map-container rounded-xl overflow-hidden elevation-3 border">
                            <iframe
                                src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3723.8638558814234!2d105.77252237599026!3d21.034131487455806!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x313454b6163c392f%3A0x103f3de963ac6693!2zMjMgTMOqIMSQ4bupYyBUaOG7jSwgTOG7uWMgTmdhLCBOYW0gVOG7qyBMacOqbSwgSMOgIE7hu5lpLCBWaWV0bmFt!5e0!3m2!1sen!2s!4v1700000000000!5m2!1sen!2s"
                                width="100%"
                                height="420"
                                style="border: 0"
                                allowfullscreen=""
                                loading="lazy"
                                referrerpolicy="no-referrer-when-downgrade"
                            ></iframe>
                        </div>
                    </v-col>
                </v-row>
            </v-container>
        </main>

        <MainFooter />

        <CustomerChat />
    </div>
</template>

<style scoped lang="scss">
.main-content {
    padding-top: 60px;
}

.max-w-600 {
    max-width: 600px;
}

.icon-wrap {
    width: 60px;
    height: 60px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
}

.bg-blue-lighten {
    background: #eff6ff;
}

.bg-green-lighten {
    background: #f0fdf4;
}

.bg-zalo-lighten {
    background: #e6f0ff;
}

.bg-purple-lighten {
    background: #faf5ff;
}

.hover-lift {
    transition: transform 0.3s ease, box-shadow 0.3s ease;
    &:hover {
        transform: translateY(-5px);
        box-shadow: 0 10px 20px rgba(0, 0, 0, 0.08) !important;
    }
}

.modern-input :deep(.v-field) {
    border-radius: 12px;
}

.shadow-blue {
    box-shadow: 0 8px 20px rgba(37, 99, 235, 0.3) !important;
}

.map-container iframe {
    display: block;
}

.page-header {
    background: linear-gradient(135deg, #1e257c 0%, #23318c 50%, #1d4ed8 100%) !important;
    position: relative;
    box-shadow: 0 4px 20px rgba(30, 37, 124, 0.25);
    color: #ffffff !important;
}

.page-header-title {
    color: #ffffff !important;
    letter-spacing: -0.5px;
    font-size: clamp(1.4rem, 2.8vw, 2.2rem) !important;
    line-height: 1.25 !important;
    white-space: nowrap;
}

@media (max-width: 600px) {
    .page-header-title {
        white-space: normal;
    }
}

.page-header-subtitle {
    color: rgba(255, 255, 255, 0.85) !important;
    font-weight: 500;
}
</style>
