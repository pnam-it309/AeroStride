<script setup>
import { ref, onMounted } from 'vue';
import ClientLoginForm from '@/components/auth/client/ClientLoginForm.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import { PATH } from '@/router/routePaths';

const currentShoeIndex = ref(0);

onMounted(() => {
    setInterval(() => {
        currentShoeIndex.value = (currentShoeIndex.value + 1) % shoes.length;
    }, 3000);
});

</script>
<template>
    <div class="client-auth-wrapper bg-white min-h-screen d-flex align-center justify-center">
        <!-- Main Container -->
        <v-container fluid class="pa-0 h-100vh">
            <v-row no-gutters class="h-100vh">

                <!-- Left Side: Dynamic Visual / Branding -->
                <v-col cols="12" md="6" lg="7"
                    class="d-none d-md-flex position-relative visual-section align-center justify-center overflow-hidden bg-white">
                    <!-- Overlay gradient -->
                    <div class="overlay-gradient-blue"></div>

                    <!-- Decorative Elements -->
                    <div class="geometric-shape shape-1"></div>
                    <div class="geometric-shape shape-2"></div>

                    <div class="content-wrapper text-center z-index-2 w-100 px-10">
                        <div class="mb-12 logo-container">
                            <LogoClient class="mega-logo subtle-logo" dark />
                        </div>

                        <h1 class="text-h2 font-weight-black text-black mb-6 tracking-tight hero-text text-uppercase"
                            style="letter-spacing: -1px;">
                            BƯỚC ĐI<br /><span class="text-blue-accent-4">ĐỘT PHÁ</span>
                        </h1>
                        <p
                            class="text-body-1 text-black font-weight-medium max-w-md mx-auto mb-10 leading-relaxed opacity-80">
                            Khám phá bộ sưu tập giày thể thao đẳng cấp. Trải nghiệm phong cách và hiệu suất đỉnh cao
                            cùng AeroStride.
                        </p>

                        <!-- Animated Shoe Slider -->
                        <div class="shoe-showcase mt-8">
                            <div class="slider-wrapper">
                                <transition-group name="fade-slide">
                                    <v-img v-for="(shoe, index) in shoes" v-show="currentShoeIndex === index"
                                        :key="shoe" :src="shoe"
                                        class="mx-auto floating-shoe-premium drop-shadow-2xl rounded-xl absolute-shoe"
                                        width="100%" max-width="500" cover></v-img>
                                </transition-group>
                            </div>
                        </div>
                    </div>
                </v-col>

                <!-- Right Side: Login Form -->
                <v-col cols="12" md="6" lg="5" class="d-flex align-center justify-center form-section bg-white">
                    <div class="auth-form-container w-100 px-sm-12 px-6 py-10" style="max-width: 500px;">
                        <!-- Mobile Logo -->
                        <div class="text-center mb-10 d-md-none transform-zoom">
                            <LogoClient class="mega-logo" />
                        </div>

                        <!-- Header -->
                        <div class="mb-10 text-center">
                            <h2 class="text-h4 font-weight-black mb-3 text-black">Chào mừng trở lại</h2>
                            <p class="text-body-1 text-grey-darken-1">Đăng nhập để tiếp tục mua sắm và quản lý đơn hàng
                                của bạn.</p>
                        </div>

                        <!-- Form Component -->
                        <ClientLoginForm />

                        <!-- Footer -->
                        <div class="mt-12 text-center">
                            <p class="text-body-1 text-grey-darken-2">
                                Bạn chưa có tài khoản?
                                <RouterLink :to="PATH.REGISTER"
                                    class="text-blue-darken-3 text-decoration-none font-weight-bold ml-2 premium-link border-b-2 border-blue-darken-3 pb-1 hover-effect">
                                    Đăng ký ngay
                                </RouterLink>
                            </p>
                        </div>
                    </div>
                </v-col>

            </v-row>
        </v-container>
    </div>
</template>

<style scoped>
.h-100vh {
    height: 100vh;
}

.min-h-screen {
    min-height: 100vh;
}

.z-index-2 {
    z-index: 2;
}

.max-w-md {
    max-width: 600px;
}

.leading-relaxed {
    line-height: 1.6 !important;
}

.text-primary-accent {
    color: #2962FF;
    /* Blue Accent 4 */
    background: linear-gradient(135deg, #0091EA 0%, #2962FF 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
}

.hero-text {
    line-height: 1.1;
    text-shadow: 0 10px 30px rgba(0, 145, 234, 0.2);
}

.visual-section {
    position: relative;
    background-color: #f8fafc;
    /* subtle pattern */
    background-image: radial-gradient(#e2e8f0 1px, transparent 1px);
    background-size: 40px 40px;
}

.overlay-gradient-blue {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: radial-gradient(circle at top left, rgba(255, 255, 255, 0.9) 0%, rgba(224, 242, 254, 0.7) 100%);
    z-index: 1;
}

.geometric-shape {
    position: absolute;
    background: linear-gradient(45deg, rgba(0, 145, 234, 0.05), rgba(41, 98, 255, 0.1));
    backdrop-filter: blur(10px);
    border-radius: 50%;
    z-index: 1;
}

.shape-1 {
    width: 600px;
    height: 600px;
    top: -150px;
    left: -150px;
    animation: float-slow 20s infinite alternate ease-in-out;
}

.shape-2 {
    width: 400px;
    height: 400px;
    bottom: -100px;
    right: -100px;
    animation: float-slow 15s infinite alternate-reverse ease-in-out;
}

@keyframes float-slow {
    0% {
        transform: translate(0, 0) rotate(0deg);
    }

    100% {
        transform: translate(50px, 50px) rotate(15deg);
    }
}

.floating-shoe-premium {
    animation: float 6s ease-in-out infinite;
    transform-origin: center center;
    box-shadow: 0 30px 60px -12px rgba(0, 0, 0, 0.8) !important;
}

@keyframes float {
    0% {
        transform: translateY(0px) rotate(-2deg);
    }

    50% {
        transform: translateY(-20px) rotate(2deg);
    }

    100% {
        transform: translateY(0px) rotate(-2deg);
    }
}

.form-section {
    position: relative;
    z-index: 10;
    box-shadow: -20px 0 40px rgba(0, 0, 0, 0.05);
}

.hover-effect {
    transition: all 0.3s ease;
}

.hover-effect:hover {
    color: #0D47A1 !important;
    border-color: #0D47A1 !important;
}

/* New Styles for Slider and Logo */
.subtle-logo {
    opacity: 1;
    transition: all 0.5s ease;
    mix-blend-mode: darken;
    /* Darken hoạt động tốt hơn để triệt tiêu nền trắng trên nền sáng */
}

.logo-container:hover .subtle-logo {
    opacity: 1;
    filter: brightness(1) contrast(1);
    transform: scale(1.05);
}

.slider-wrapper {
    position: relative;
    height: 420px;
    /* Điều chỉnh chiều cao hợp lý */
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-top: -10px;
    /* Kéo lên trên một chút so với văn bản */
    margin-bottom: 50px;
    /* Thêm khoảng trống ở dưới để không dính đáy */
}

.absolute-shoe {
    position: absolute !important;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
}

/* Fade Slide Transition */
.fade-slide-enter-active,
.fade-slide-leave-active {
    transition: all 1s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-slide-enter-from {
    opacity: 0;
    transform: translate(-50%, -40%) rotate(5deg) scale(0.8);
}

.fade-slide-leave-to {
    opacity: 0;
    transform: translate(-50%, -60%) rotate(-5deg) scale(1.2);
}

.shoe-showcase {
    perspective: 1000px;
}
</style>
