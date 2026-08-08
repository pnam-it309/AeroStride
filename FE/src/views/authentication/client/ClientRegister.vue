<script setup>
import { ref, onMounted } from 'vue';
import ClientRegisterForm from '@/components/auth/client/ClientRegisterForm.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import { PATH } from '@/router/routePaths';

const currentShoeIndex = ref(0);

import s4 from '@/assets/images/products/s4.jpg';
import s5 from '@/assets/images/products/s5.jpg';
import s7 from '@/assets/images/products/s7.jpg';
import s11 from '@/assets/images/products/s11.jpg';

const shoes = [s4, s5, s7, s11];

onMounted(() => {
    setInterval(() => {
        currentShoeIndex.value = (currentShoeIndex.value + 1) % shoes.length;
    }, 3000);
});
</script>

<template>
    <div class="client-auth-wrapper bg-white min-h-screen d-flex align-center justify-center">
        <v-container fluid class="pa-0 h-100vh">
            <v-row no-gutters class="h-100vh">
                <!-- Left Side: Dynamic Visual / Branding -->
                <v-col
                    cols="12"
                    md="6"
                    lg="7"
                    class="d-none d-md-flex position-relative visual-section align-center justify-center overflow-hidden bg-white"
                >
                    <!-- Overlay gradient -->
                    <div class="overlay-gradient-blue"></div>

                    <!-- Decorative Elements -->
                    <div class="geometric-shape shape-1"></div>
                    <div class="geometric-shape shape-2"></div>

                    <div class="content-wrapper text-center z-index-2 w-100 px-10">
                        <div class="mb-10 logo-container">
                            <LogoClient class="mega-logo subtle-logo" dark />
                        </div>

                        <h1
                            class="text-h2 font-weight-black text-black mb-6 tracking-tight hero-text text-uppercase"
                            style="letter-spacing: -1px"
                        >
                            HÀNH TRÌNH<br /><span class="text-blue-accent-4">BẮT ĐẦU ĐÂY</span>
                        </h1>
                        <p class="text-body-1 text-black font-weight-medium max-w-md mx-auto mb-8 leading-relaxed opacity-80">
                            Tạo tài khoản miễn phí và khám phá bộ sưu tập giày thể thao đẳng cấp của AeroStride. Trải nghiệm mua sắm cá nhân
                            hóa dành riêng cho bạn.
                        </p>

                        <!-- Benefits list -->
                        <div class="benefits-list d-flex flex-column align-center ga-3">
                            <div
                                v-for="(item, i) in [
                                    { icon: 'mdi-truck-fast-outline', text: 'Giao hàng siêu tốc toàn quốc' },
                                    { icon: 'mdi-tag-heart-outline', text: 'Ưu đãi độc quyền cho thành viên' },
                                    { icon: 'mdi-history', text: 'Theo dõi đơn hàng dễ dàng' }
                                ]"
                                :key="i"
                                class="benefit-item d-flex align-center ga-3"
                            >
                                <v-icon :icon="item.icon" color="blue-darken-3" size="22" />
                                <span class="text-body-2 font-weight-medium text-grey-darken-2">{{ item.text }}</span>
                            </div>
                        </div>

                        <!-- Animated Shoe Slider -->
                        <div class="shoe-showcase mt-6">
                            <div class="slider-wrapper">
                                <transition-group name="fade-slide">
                                    <v-img
                                        v-for="(shoe, index) in shoes"
                                        v-show="currentShoeIndex === index"
                                        :key="shoe"
                                        :src="shoe"
                                        class="mx-auto floating-shoe-premium drop-elevation-12 rounded-xl absolute-shoe"
                                        width="100%"
                                        max-width="420"
                                        cover
                                    />
                                </transition-group>
                            </div>
                        </div>
                    </div>
                </v-col>

                <!-- Right Side: Register Form -->
                <v-col cols="12" md="6" lg="5" class="d-flex align-center justify-center form-section bg-white">
                    <div class="auth-form-container w-100 px-sm-12 px-6 py-8" style="max-width: 520px">
                        <!-- Mobile Logo -->
                        <div class="text-center mb-8 d-md-none">
                            <LogoClient class="mega-logo" />
                        </div>

                        <!-- Header -->
                        <div class="mb-6 text-center">
                            <div class="register-icon-wrapper mx-auto mb-4">
                                <v-icon icon="mdi-account-plus-outline" size="32" color="blue-darken-3" />
                            </div>
                            <h2 class="text-h5 font-weight-black mb-2 text-black">Tạo tài khoản mới</h2>
                            <p class="text-body-2 text-grey-darken-1">Điền thông tin bên dưới để bắt đầu hành trình của bạn.</p>
                        </div>

                        <!-- Register Form Component -->
                        <ClientRegisterForm />

                        <!-- Footer: link back to login -->
                        <div class="mt-8 text-center">
                            <p class="text-body-2 text-grey-darken-2">
                                Đã có tài khoản?
                                <RouterLink
                                    :to="PATH.LOGIN"
                                    class="text-blue-darken-3 text-decoration-none font-weight-bold ml-1 premium-link border-b border-blue-darken-3 pb-px hover-effect"
                                >
                                    Đăng nhập ngay
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
    max-width: 580px;
}

.leading-relaxed {
    line-height: 1.6 !important;
}

.hero-text {
    line-height: 1.1;
    text-shadow: 0 10px 30px rgba(0, 145, 234, 0.2);
}

.visual-section {
    position: relative;
    background-color: #f8fafc;
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
    overflow-y: auto;
}

.register-icon-wrapper {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 15px rgba(13, 71, 161, 0.12);
}

.benefit-item {
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(8px);
    border: 1px solid rgba(25, 118, 210, 0.1);
    border-radius: 10px;
    padding: 8px 16px;
    width: fit-content;
    transition: all 0.3s ease;
}

.benefit-item:hover {
    transform: translateX(4px);
    border-color: rgba(25, 118, 210, 0.25);
}

.hover-effect {
    transition: all 0.3s ease;
}

.hover-effect:hover {
    color: #0d47a1 !important;
    border-color: #0d47a1 !important;
}

.subtle-logo {
    opacity: 1;
    transition: all 0.5s ease;
    mix-blend-mode: darken;
}

.logo-container:hover .subtle-logo {
    opacity: 1;
    filter: brightness(1) contrast(1);
    transform: scale(1.05);
}

.slider-wrapper {
    position: relative;
    height: 300px;
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 30px;
}

.absolute-shoe {
    position: absolute !important;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
}

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
