<script setup>
import { onMounted } from 'vue';
import MainHeader from '@/components/shared/MainHeader.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import { useSeoMeta } from '@/composables/useSeoMeta';

const { setSeoMeta } = useSeoMeta();

onMounted(() => {
    window.scrollTo(0, 0);
    setSeoMeta({
        title: 'Giới Thiệu | AeroStride',
        description: 'Khám phá câu chuyện thương hiệu AeroStride, sứ mệnh và hành trình mang đến những đôi giày thể thao đẳng cấp nhất cho người Việt.'
    });
});
</script>

<template>
    <div class="app-container bg-white">
        <MainHeader />
        
        <main class="main-content">
            <!-- Hero Section -->
            <section class="about-hero">
                <div class="hero-overlay"></div>
                <div class="hero-content text-center">
                    <h1 class="text-h2 font-weight-black text-white mb-4 animate-up">BƯỚC CHÂN CỦA SỰ ĐAM MÊ</h1>
                    <p class="text-h6 text-white opacity-80 animate-up delay-1">Hành trình kiến tạo chuẩn mực giày thể thao mới tại Việt Nam.</p>
                </div>
            </section>

            <!-- Mission & Vision -->
            <section class="py-16">
                <v-container>
                    <v-row align="center" justify="center">
                        <v-col cols="12" md="5" class="pr-md-8">
                            <div class="subtitle text-primary font-weight-bold mb-2">VỀ CHÚNG TÔI</div>
                            <h2 class="text-h3 font-weight-black mb-6">AeroStride là ai?</h2>
                            <p class="text-body-1 text-grey-darken-1 mb-4 leading-relaxed">
                                Được thành lập vào năm 2024, AeroStride không chỉ là một thương hiệu bán lẻ giày thể thao, mà còn là nơi hội tụ của những tâm hồn đam mê dịch chuyển. Chúng tôi tin rằng mỗi bước đi đều mang một ý nghĩa riêng, và một đôi giày tốt sẽ là người bạn đồng hành hoàn hảo nhất.
                            </p>
                            <p class="text-body-1 text-grey-darken-1 leading-relaxed">
                                Với cam kết 100% sản phẩm chính hãng, dịch vụ khách hàng vượt trội và không gian mua sắm hiện đại, AeroStride đang từng bước khẳng định vị thế dẫn đầu trong ngành bán lẻ đồ thể thao cao cấp.
                            </p>
                        </v-col>
                        <v-col cols="12" md="7">
                            <v-img 
                                src="https://images.unsplash.com/photo-1556906781-9a412961c28c?auto=format&fit=crop&q=80&w=1200" 
                                class="rounded-xl elevation-10"
                                cover
                                height="400"
                            ></v-img>
                        </v-col>
                    </v-row>
                </v-container>
            </section>

            <!-- Stats -->
            <section class="stats-section py-16 bg-grey-lighten-4">
                <v-container>
                    <v-row class="text-center">
                        <v-col cols="12" sm="4">
                            <div class="text-h2 font-weight-black text-primary mb-2">50+</div>
                            <div class="text-subtitle-1 font-weight-bold text-grey-darken-2 uppercase tracking-wide">Chi Nhánh</div>
                        </v-col>
                        <v-col cols="12" sm="4">
                            <div class="text-h2 font-weight-black text-primary mb-2">1M+</div>
                            <div class="text-subtitle-1 font-weight-bold text-grey-darken-2 uppercase tracking-wide">Khách Hàng</div>
                        </v-col>
                        <v-col cols="12" sm="4">
                            <div class="text-h2 font-weight-black text-primary mb-2">100%</div>
                            <div class="text-subtitle-1 font-weight-bold text-grey-darken-2 uppercase tracking-wide">Chính Hãng</div>
                        </v-col>
                    </v-row>
                </v-container>
            </section>
        </main>
        
        <footer class="footer-landing py-10 text-center text-grey-darken-1 bg-white border-t">
            <LogoClient class="mb-4 d-inline-block" style="max-width: 150px" />
            <p>&copy; 2026 AeroStride All rights reserved.</p>
        </footer>
    </div>
</template>

<style scoped>
.main-content {
    padding-top: 60px; /* Offset for header */
}

.about-hero {
    position: relative;
    height: 60vh;
    min-height: 400px;
    background-image: url('https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&q=80&w=2000');
    background-size: cover;
    background-position: center;
    background-attachment: fixed;
    display: flex;
    align-items: center;
    justify-content: center;
}

.hero-overlay {
    position: absolute;
    inset: 0;
    background: linear-gradient(to right, rgba(0,0,0,0.8), rgba(0,0,0,0.4));
}

.hero-content {
    position: relative;
    z-index: 10;
    max-width: 800px;
    padding: 0 20px;
}

.subtitle {
    letter-spacing: 4px;
    text-transform: uppercase;
    font-size: 0.9rem;
}

.leading-relaxed {
    line-height: 1.8;
}

.uppercase { text-transform: uppercase; }
.tracking-wide { letter-spacing: 2px; }

/* Simple intro animations */
.animate-up {
    animation: fadeInUp 1s ease forwards;
    opacity: 0;
    transform: translateY(30px);
}
.delay-1 { animation-delay: 0.2s; }

@keyframes fadeInUp {
    to {
        opacity: 1;
        transform: translateY(0);
    }
}
</style>
