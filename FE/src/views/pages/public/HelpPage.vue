<script setup>
import { ref, onMounted } from 'vue';
import MainHeader from '@/components/shared/MainHeader.vue';
import MainFooter from '@/components/shared/MainFooter.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import { useSeoMeta } from '@/composables/useSeoMeta';

const { setSeoMeta } = useSeoMeta();
const searchQuery = ref('');

const categories = [
    { icon: 'mdi-truck-delivery-outline', title: 'Vận chuyển & Giao nhận' },
    { icon: 'mdi-sync', title: 'Đổi trả 30 ngày' },
    { icon: 'mdi-lock-shield-outline', title: 'Chính sách bảo mật' },
    { icon: 'mdi-file-document-check-outline', title: 'Điều khoản dịch vụ' }
];

const faqs = [
    {
        title: 'Chính sách Đổi trả & Hoàn tiền (30 Ngày)',
        items: [
            {
                q: 'Chính sách đổi trả của AeroStride như thế nào?',
                a: 'AeroStride hỗ trợ đổi size, đổi mẫu hoặc hoàn tiền miễn phí trong vòng 30 ngày kể từ ngày nhận hàng với điều kiện sản phẩm còn nguyên tem mác, chưa qua sử dụng ngoài trời và có hộp đi kèm.'
            },
            {
                q: 'Quy trình đổi trả hàng diễn ra như thế nào?',
                a: 'Quý khách vui lòng liên hệ hotline 0986 525 959 hoặc chat trực tiếp qua khung Zalo / Live Chat trên website. Shipper sẽ đến tận nơi nhận lại hàng và giao đôi mới cho quý khách.'
            },
            {
                q: 'Bao lâu thì tôi nhận được tiền hoàn?',
                a: 'Sau khi cửa hàng nhận lại sản phẩm và kiểm tra hợp lệ, khoản tiền sẽ được hoàn về tài khoản ngân hàng hoặc ví điện tử của quý khách trong vòng 24 - 48 giờ làm việc.'
            }
        ]
    },
    {
        title: 'Chính sách Bảo mật Thông tin',
        items: [
            {
                q: 'Thông tin cá nhân của tôi được bảo mật như thế nào?',
                a: 'AeroStride cam kết bảo mật tuyệt đối 100% dữ liệu cá nhân, số điện thoại, địa chỉ nhận hàng và lịch sử giao dịch của khách hàng theo tiêu chuẩn an toàn thông tin quốc tế. Chúng tôi không bao giờ chia sẻ thông tin cho bên thứ ba.'
            },
            {
                q: 'Bảo mật thanh toán trực tuyến qua VNPay ra sao?',
                a: 'Mọi giao dịch thanh toán online được mã hóa chuẩn SSL 256-bit qua cổng thanh toán VNPay được Ngân hàng Nhà nước cấp phép, đảm bảo an toàn tuyệt đối cho tài khoản của quý khách.'
            }
        ]
    },
    {
        title: 'Điều khoản Dịch vụ & Cam kết',
        items: [
            {
                q: 'Cam kết hàng chính hãng tại AeroStride',
                a: '100% sản phẩm giày thể thao tại AeroStride (Nike, Adidas, Puma, Mizuno, Asics...) đều được nhập khẩu chính ngạch với đầy đủ hóa đơn, tem chống hàng giả và bảo hành chính hãng trọn đời đối với keo dán và đế giày.'
            },
            {
                q: 'Quyền lợi khi mua sắm tại AeroStride',
                a: 'Quý khách được đồng kiểm hàng trước khi thanh toán (mở hộp xem giày, thử size), tích điểm thành viên nhận voucher giảm giá và được hỗ trợ đổi trả linh hoạt 30 ngày.'
            }
        ]
    },
    {
        title: 'Vận chuyển & Giao nhận Toàn Quốc',
        items: [
            {
                q: 'Thời gian giao hàng là bao lâu?',
                a: 'Đối với khu vực nội thành Hà Nội và TP.HCM, thời gian giao hàng hỏa tốc trong 2H hoặc 24H. Đối với các tỉnh thành khác trên toàn quốc từ 2 - 3 ngày làm việc.'
            },
            {
                q: 'Phí vận chuyển được tính như thế nào?',
                a: 'AeroStride miễn phí vận chuyển toàn quốc cho mọi đơn hàng có giá trị từ 500.000 VNĐ. Với đơn hàng dưới mức này, phí vận chuyển đồng giá ưu đãi là 30.000 VNĐ.'
            }
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
                        maxlength="100"
                    ></v-text-field>
                </v-container>
            </div>

            <v-container class="py-12">
                <!-- Categories Grid -->
                <v-row class="mb-12" justify="center">
                    <v-col v-for="(cat, i) in categories" :key="i" cols="6" sm="3">
                        <v-card
                            class="text-center pa-6 rounded-xl hover-card cursor-pointer h-100 d-flex flex-column align-center justify-center elevation-2"
                        >
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

        <MainFooter />

        <CustomerChat />
    </div>
</template>

<style scoped>
.main-content {
    padding-top: 60px;
}

.search-input {
    max-width: 600px;
}
.search-input :deep(.v-field__input) {
    padding-top: 16px;
    padding-bottom: 16px;
    font-size: 1.1rem;
}

.hover-card {
    transition:
        transform 0.3s ease,
        box-shadow 0.3s ease;
}
.hover-card:hover {
    transform: translateY(-8px);
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1) !important;
}

.max-w-800 {
    max-width: 800px;
}

.leading-relaxed {
    line-height: 1.7;
}
</style>
